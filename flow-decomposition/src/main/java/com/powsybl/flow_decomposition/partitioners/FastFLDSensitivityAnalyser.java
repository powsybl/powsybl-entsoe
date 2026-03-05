package com.powsybl.flow_decomposition.partitioners;

import com.google.common.collect.Streams;
import com.powsybl.contingency.ContingencyContext;
import com.powsybl.flow_decomposition.AbstractSensitivityAnalyser;
import com.powsybl.flow_decomposition.FunctionVariableFactor;
import com.powsybl.flow_decomposition.NetworkUtil;
import com.powsybl.iidm.network.*;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.sensitivity.*;
import org.ejml.data.DMatrixSparseCSC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

import static com.powsybl.flow_decomposition.DecomposedFlow.*;

/**
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 */
public class FastFLDSensitivityAnalyser extends AbstractSensitivityAnalyser {
    public static final String SPLIT_CHARACTER = "_";
    private static final Logger LOGGER = LoggerFactory.getLogger(FastFLDSensitivityAnalyser.class);
    private final Network network;
    private final List<String> xnecIds;
    private final DMatrixSparseCSC pexMatrix;
    private final String[] vertexIds;
    private final boolean[] isBusByVertexIndex;
    private final Country[] countriesByVertexPos;
    private final String[] injByVertexId;

    public FastFLDSensitivityAnalyser(LoadFlowParameters loadFlowParameters, SensitivityAnalysis.Runner runner, Network network, Set<Branch<?>> xnecs, Map<String, Integer> vertexIdMapping, DMatrixSparseCSC pexMatrix, List<Bus> busesInMainSynchronousComponent) {
        super(loadFlowParameters, runner);
        this.network = network;
        this.xnecIds = xnecs.stream().map(Identifiable::getId).toList();
        this.pexMatrix = pexMatrix;

        Map<String, String> anyInjectionOnBus = busesInMainSynchronousComponent.stream().collect(Collectors.toMap(Identifiable::getId, bus -> NetworkUtil.getInjectionStream(bus).findAny().orElseThrow().getId()));
        Map<String, Bus> idToBus = new HashMap<>();
        busesInMainSynchronousComponent.forEach(bus -> idToBus.put(bus.getId(), bus));

        int nVertex = vertexIdMapping.size();
        this.vertexIds = new String[nVertex];
        this.isBusByVertexIndex = new boolean[nVertex];
        this.countriesByVertexPos = new Country[nVertex];
        this.injByVertexId = new String[nVertex];

        vertexIdMapping.forEach((id, index) -> {
            this.vertexIds[index] = id;
            this.injByVertexId[index] = anyInjectionOnBus.get(id);
            Bus bus = idToBus.get(id);
            if (bus != null) {
                this.isBusByVertexIndex[index] = true;
                Country country = bus.getVoltageLevel()
                    .getSubstation().orElseThrow()
                    .getCountry().orElse(null);
                this.countriesByVertexPos[index] = country;
            }
        });
    }

    private static double respectFlowSignConvention(double ptdfValue, double referenceFlow) {
        return referenceFlow < 0 ? -ptdfValue : ptdfValue;
    }

    private static String encodeFlowPartNameVertex(String flowPartName, int sourceIndex, int sinkIndex, String vertex_type) {
        return String.format("%s%s%s%s%s%s%s", flowPartName, SPLIT_CHARACTER, sourceIndex, SPLIT_CHARACTER, sinkIndex, SPLIT_CHARACTER, vertex_type);
    }

    private static String encodePst(String pst) {
        return String.format("%s%s%s", PST_COLUMN_NAME, SPLIT_CHARACTER, pst);
    }

    private static boolean isPstCode(String code) {
        return code.startsWith(PST_COLUMN_NAME);
    }

    private static String decodeFlowPart(String code) {
        return code.split(SPLIT_CHARACTER)[0];
    }

    private static Integer decodeSource(String code) {
        return Integer.valueOf(code.split(SPLIT_CHARACTER)[1]);
    }

    private static Integer decodeSink(String code) {
        return Integer.valueOf(code.split(SPLIT_CHARACTER)[2]);
    }

    private static int decodeVertexType(String code) {
        String vertexType = code.split(SPLIT_CHARACTER)[3];
        if (vertexType.equals("source")) {
            return 1;
        } else if (vertexType.equals("sink")) {
            return -1;
        } else {
            throw new IllegalArgumentException("Invalid vertex type: " + vertexType);
        }
    }

    private String decodePst(String code) {
        return code.split(SPLIT_CHARACTER)[1];
    }

    public Map<String, Map<String, Double>> run() {
        List<SensitivityVariableSet> sensitivityVariableSets = new ArrayList<>();
        List<String> variableIds = new ArrayList<>();
        Streams.stream(pexMatrix.createCoordinateIterator())
            .forEach(coordinateRealValue -> {
                int sourceIndex = coordinateRealValue.row;
                int sinkIndex = coordinateRealValue.col;
                String sourceInjId = Optional.ofNullable(injByVertexId[sourceIndex]).orElse(vertexIds[sourceIndex]);
                String sinkInjId = Optional.ofNullable(injByVertexId[sinkIndex]).orElse(vertexIds[sinkIndex]);
                WeightedSensitivityVariable sourceVariable = new WeightedSensitivityVariable(sourceInjId, 1);
                WeightedSensitivityVariable sinkVariable = new WeightedSensitivityVariable(sinkInjId, 1);
                Optional<String> optionalFlowPartName = computeFlowPartName(sourceIndex, sinkIndex);
                if (optionalFlowPartName.isEmpty()) {
                    return;
                }
                String flowPartName = optionalFlowPartName.get();
                String sourceVariableId = encodeFlowPartNameVertex(flowPartName, sourceIndex, sinkIndex, "source");
                SensitivityVariableSet sourceVariableSet = new SensitivityVariableSet(sourceVariableId, List.of(sourceVariable));
                sensitivityVariableSets.add(sourceVariableSet);
                variableIds.add(sourceVariableId);
                String sinkVariableId = encodeFlowPartNameVertex(flowPartName, sourceIndex, sinkIndex, "sink");
                SensitivityVariableSet sinkVariableSet = new SensitivityVariableSet(sinkVariableId, List.of(sinkVariable));
                sensitivityVariableSets.add(sinkVariableSet);
                variableIds.add(sinkVariableId);
            });


        Map<String, Map<String, Double>> results = new HashMap<>();
        LOGGER.debug("Running sensitivity analysis for PST flow for variables");
        List<FunctionVariableFactor> sensitivityFactorsPst = new ArrayList<>();
        SensitivityFactorReader factorReaderPst = new FastFLDPstSensitivityFactorReader(sensitivityFactorsPst, xnecIds);
        SensitivityResultWriter valueWriterPst = new FastFLDPstSensitivityResultWriter(sensitivityFactorsPst, results);
        runSensitivityAnalysis(network, factorReaderPst, valueWriterPst, Collections.emptyList());
        int batchSize2 = 1500;
        for (int iVariable = 0; iVariable < variableIds.size(); iVariable += batchSize2) {
            LOGGER.debug("Running sensitivity analysis for decomposed flow for variables {}/{}", iVariable, variableIds.size());
            int upperbound = Math.min(variableIds.size(), iVariable + batchSize2);
            List<String> localFlowPart = variableIds.subList(iVariable, upperbound);
            List<SensitivityVariableSet> localSensitivityVariableSets = sensitivityVariableSets.subList(iVariable, upperbound);
            List<FunctionVariableFactor> sensitivityFactors = new ArrayList<>();
            SensitivityFactorReader factorReader = new FastFLDSensitivityFactorReader(localFlowPart, sensitivityFactors, xnecIds);
            SensitivityResultWriter valueWriter = new FastFLDSensitivityResultWriter(sensitivityFactors, results);
            runSensitivityAnalysis(network, factorReader, valueWriter, localSensitivityVariableSets);
        }
        return results;
    }

    private Optional<String> computeFlowPartName(int sourceIndex, int sinkIndex) {
        if ((isBusByVertexIndex[sourceIndex] && isBusByVertexIndex[sinkIndex])) {
            Country sourceCountry = countriesByVertexPos[sourceIndex];
            Country sinkCountry = countriesByVertexPos[sinkIndex];
            if (sourceCountry == null || sinkCountry == null) {
                String sourceVertexId = vertexIds[sourceIndex];
                String sinkVertexId = vertexIds[sinkIndex];
                LOGGER.warn("Cannot compute loop flow for bus {} and {} because of invalid country", sourceVertexId, sinkVertexId);
                return Optional.empty();
            }
            if (sourceCountry.equals(sinkCountry)) {
                return Optional.of(NetworkUtil.getLoopFlowIdFromCountry(sourceCountry));
            } else {
                return Optional.of(ALLOCATED_COLUMN_NAME);
            }
        } else {
            return Optional.of(XNODE_COLUMN_NAME);
        }
    }

    private class FastFLDSensitivityResultWriter implements SensitivityResultWriter {

        private final List<FunctionVariableFactor> factors;
        private final Map<String, Map<String, Double>> results;

        public FastFLDSensitivityResultWriter(List<FunctionVariableFactor> factors, Map<String, Map<String, Double>> results) {
            this.factors = factors;
            this.results = results;
        }

        @Override
        public void writeSensitivityValue(int factorIndex, int contingencyIndex, double value, double functionReference) {
            if (Double.isNaN(value)) {
                return;
            }
            if (value == 0.0) {
                return;
            }
            FunctionVariableFactor factor = factors.get(factorIndex);
            Map<String, Double> flowDecomposition = results.computeIfAbsent(factor.functionId(), s -> new HashMap<>());
            String codedVariableId = factor.variableId();

            String flowPart = decodeFlowPart(codedVariableId);
            int sourceIndex = decodeSource(codedVariableId);
            int sinkIndex = decodeSink(codedVariableId);
            int vertexTypeSign = decodeVertexType(codedVariableId);
            double partialFlowPartValue = flowDecomposition.getOrDefault(flowPart, 0.0);
            double exchange = pexMatrix.get(sourceIndex, sinkIndex);
            double increaseFlow = value * exchange;
            double increaseWithSign = respectFlowSignConvention(increaseFlow, functionReference) * vertexTypeSign;
            flowDecomposition.put(flowPart, partialFlowPartValue + increaseWithSign);
        }

        @Override
        public void writeContingencyStatus(int contingencyIndex, SensitivityAnalysisResult.Status status) {
            // We do not manage contingency yet
        }
    }

    private class FastFLDSensitivityFactorReader implements SensitivityFactorReader {
        private final List<String> flowParts;
        private final List<FunctionVariableFactor> factors;
        private final List<String> subsetXnecs;

        public FastFLDSensitivityFactorReader(List<String> flowParts, List<FunctionVariableFactor> factors, List<String> xnecs) {
            this.flowParts = flowParts;
            this.factors = factors;
            this.subsetXnecs = xnecs;
        }

        @Override
        public void read(Handler handler) {
            for (String xnecId : subsetXnecs) {
                for (String flowPart : flowParts) {
                    factors.add(new FunctionVariableFactor(xnecId, flowPart));
                    handler.onFactor(SENSITIVITY_FUNCTION_TYPE, xnecId, SensitivityVariableType.INJECTION_ACTIVE_POWER, flowPart, true, ContingencyContext.none());
                }
            }
        }
    }

    private class FastFLDPstSensitivityResultWriter implements SensitivityResultWriter {

        private final List<FunctionVariableFactor> factors;
        private final Map<String, Map<String, Double>> results;

        public FastFLDPstSensitivityResultWriter(List<FunctionVariableFactor> factors, Map<String, Map<String, Double>> results) {
            this.factors = factors;
            this.results = results;
        }

        @Override
        public void writeSensitivityValue(int factorIndex, int contingencyIndex, double value, double functionReference) {
            if (Double.isNaN(value)) {
                return;
            }
            if (value == 0.0) {
                return;
            }
            FunctionVariableFactor factor = factors.get(factorIndex);
            Map<String, Double> flowDecomposition = results.computeIfAbsent(factor.functionId(), s -> new HashMap<>());

            String pstId = factor.variableId();
            PhaseTapChanger phaseTapChanger = network.getTwoWindingsTransformer(pstId).getPhaseTapChanger();
            Optional<PhaseTapChangerStep> neutralStep = phaseTapChanger.getNeutralStep();
            double deltaTap = 0.0;
            if (neutralStep.isPresent()) {
                deltaTap = phaseTapChanger.getCurrentStep().getAlpha() - neutralStep.get().getAlpha();
            }
            double pstFlow = flowDecomposition.getOrDefault(PST_COLUMN_NAME, 0.0);
            double increase = respectFlowSignConvention(deltaTap * value, functionReference);
            flowDecomposition.put(PST_COLUMN_NAME, pstFlow + increase);
        }

        @Override
        public void writeContingencyStatus(int contingencyIndex, SensitivityAnalysisResult.Status status) {
            // We do not manage contingency yet
        }
    }

    private class FastFLDPstSensitivityFactorReader implements SensitivityFactorReader {
        private final List<FunctionVariableFactor> factors;
        private final List<String> subsetXnecs;

        public FastFLDPstSensitivityFactorReader(List<FunctionVariableFactor> factors, List<String> xnecs) {
            this.factors = factors;
            this.subsetXnecs = xnecs;
        }

        @Override
        public void read(Handler handler) {
            for (String xnecId : subsetXnecs) {
                for (String pst : NetworkUtil.getPstIdList(network)) {
                    factors.add(new FunctionVariableFactor(xnecId, pst));
                    handler.onFactor(SENSITIVITY_FUNCTION_TYPE, xnecId, SensitivityVariableType.TRANSFORMER_PHASE, pst, false, ContingencyContext.none());
                }
            }
        }
    }
}
