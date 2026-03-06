package com.powsybl.flow_decomposition.partitioners;

import com.google.common.collect.Streams;
import com.powsybl.commons.PowsyblException;
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
import static com.powsybl.flow_decomposition.partitioners.SensitivityAnalyser.EMPTY_SENSITIVITY_VARIABLE_SETS;

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

    public Map<String, Map<String, Double>> run() {
        List<String> flowPartNameList = new ArrayList<>(List.of(PST_COLUMN_NAME, ALLOCATED_COLUMN_NAME, XNODE_COLUMN_NAME));
        network.getCountries().forEach(country -> flowPartNameList.add(NetworkUtil.getLoopFlowIdFromCountry(country)));
        Map<String, Integer> flowPartIndex = NetworkUtil.getIndex(flowPartNameList);
        int nFlowPart = flowPartIndex.size();
        Map<String, Integer> xnecIndex = NetworkUtil.getIndex(xnecIds);
        int nXnec = xnecIndex.size();

        Map<String, double[]> exchangePerFlowPart = new HashMap<>();
        Streams.stream(pexMatrix.createCoordinateIterator())
            .forEach(coordinateRealValue -> {
                int sourceIndex = coordinateRealValue.row;
                int sinkIndex = coordinateRealValue.col;
                double exchangeBetweenFromAndTo = coordinateRealValue.value;
                String sourceInjId = Optional.ofNullable(injByVertexId[sourceIndex]).orElse(vertexIds[sourceIndex]);
                String sinkInjId = Optional.ofNullable(injByVertexId[sinkIndex]).orElse(vertexIds[sinkIndex]);
                String flowPartName = computeFlowPartName(sourceIndex, sinkIndex);
                double[] exchangesSource = exchangePerFlowPart.computeIfAbsent(sourceInjId, s -> new double[nFlowPart]);
                exchangesSource[flowPartIndex.get(flowPartName)] += exchangeBetweenFromAndTo;
                double[] exchangesSink = exchangePerFlowPart.computeIfAbsent(sinkInjId, s -> new double[nFlowPart]);
                exchangesSink[flowPartIndex.get(flowPartName)] += -exchangeBetweenFromAndTo;
            });
        List<String> variableIds = exchangePerFlowPart.keySet().stream().toList();

        double[][] results = new double[nXnec][nFlowPart];
        LOGGER.debug("Running sensitivity analysis for PST flow for variables");
        List<FunctionVariableFactor> sensitivityFactorsPst = new ArrayList<>();
        List<String> pstIdList = NetworkUtil.getPstIdList(network);
        Map<String, PhaseTapChanger> phaseTapChangerMap = pstIdList.stream().collect(Collectors.toMap(pstId -> pstId, pstId -> network.getTwoWindingsTransformer(pstId).getPhaseTapChanger()));
        SensitivityFactorReader factorReaderPst = new FastFLDPstSensitivityFactorReader(sensitivityFactorsPst, xnecIds, pstIdList);
        SensitivityResultWriter valueWriterPst = new FastFLDPstSensitivityResultWriter(sensitivityFactorsPst, results, flowPartIndex.get(PST_COLUMN_NAME), xnecIndex, phaseTapChangerMap);
        runSensitivityAnalysis(network, factorReaderPst, valueWriterPst, Collections.emptyList());
        int batchSize2 = 5000;
        for (int iVariable = 0; iVariable < variableIds.size(); iVariable += batchSize2) {
            int upperbound = Math.min(variableIds.size(), iVariable + batchSize2);
            LOGGER.debug("Running sensitivity analysis for decomposed flow for variables {}-{}/{}", iVariable, upperbound, variableIds.size());
            List<String> subVariableIds = variableIds.subList(iVariable, upperbound);
            FunctionVariableFactor[] sensitivityFactors = new FunctionVariableFactor[subVariableIds.size() * xnecIds.size()];
            SensitivityFactorReader factorReader = new FastFLDSensitivityFactorReader(subVariableIds, sensitivityFactors, xnecIds);
            SensitivityResultWriter valueWriter = new FastFLDSensitivityResultWriter(sensitivityFactors, results, xnecIndex, exchangePerFlowPart);
            runSensitivityAnalysis(network, factorReader, valueWriter, EMPTY_SENSITIVITY_VARIABLE_SETS);
        }

        return xnecIds.stream()
            .collect(Collectors.toMap(
                xnecId -> xnecId,
                xnecId -> flowPartNameList.stream().collect(Collectors.toMap(
                    flowPartName -> flowPartName,
                    flowPartName -> results[xnecIndex.get(xnecId)][flowPartIndex.get(flowPartName)]
                ))));
    }

    private String computeFlowPartName(int sourceIndex, int sinkIndex) {
        if ((isBusByVertexIndex[sourceIndex] && isBusByVertexIndex[sinkIndex])) {
            Country sourceCountry = countriesByVertexPos[sourceIndex];
            Country sinkCountry = countriesByVertexPos[sinkIndex];
            if (sourceCountry == null || sinkCountry == null) {
                String sourceVertexId = vertexIds[sourceIndex];
                String sinkVertexId = vertexIds[sinkIndex];
                throw new PowsyblException(String.format("Cannot compute loop flow for bus %s and %s because of invalid country", sourceVertexId, sinkVertexId));
            }
            if (sourceCountry.equals(sinkCountry)) {
                return NetworkUtil.getLoopFlowIdFromCountry(sourceCountry);
            } else {
                return ALLOCATED_COLUMN_NAME;
            }
        } else {
            return XNODE_COLUMN_NAME;
        }
    }

    private static class FastFLDSensitivityResultWriter implements SensitivityResultWriter {

        private final FunctionVariableFactor[] factors;
        private final double[][] results;
        private final Map<String, Integer> xnecIndex;
        private final Map<String, double[]> exchangePerFlowPart;

        public FastFLDSensitivityResultWriter(FunctionVariableFactor[] factors, double[][] results, Map<String, Integer> xnecIndex, Map<String, double[]> exchangePerFlowPart) {
            this.factors = factors;
            this.results = results;
            this.xnecIndex = xnecIndex;
            this.exchangePerFlowPart = exchangePerFlowPart;
        }

        @Override
        public void writeSensitivityValue(int factorIndex, int contingencyIndex, double value, double functionReference) {
            if (Double.isNaN(value)) {
                return;
            }
            if (value == 0.0) {
                return;
            }
            FunctionVariableFactor factor = factors[factorIndex];
            Integer i = xnecIndex.get(factor.functionId());
            double[] flowDecomposition = results[i];
            String variableId = factor.variableId();

            double[] exchanges = exchangePerFlowPart.get(variableId);
            for (int flowPartIndex = 0; flowPartIndex < exchanges.length; flowPartIndex++) {
                double increase = exchanges[flowPartIndex] * value;
                double increaseWithSign = respectFlowSignConvention(increase, functionReference);
                flowDecomposition[flowPartIndex] += increaseWithSign;
            }
        }

        @Override
        public void writeContingencyStatus(int contingencyIndex, SensitivityAnalysisResult.Status status) {
            // We do not manage contingency yet
        }
    }

    private static class FastFLDSensitivityFactorReader implements SensitivityFactorReader {
        private final List<String> variableIds;
        private final FunctionVariableFactor[] factors;
        private final List<String> xnecs;

        public FastFLDSensitivityFactorReader(List<String> variableIds, FunctionVariableFactor[] factors, List<String> xnecs) {
            this.variableIds = variableIds;
            this.factors = factors;
            this.xnecs = xnecs;
        }

        @Override
        public void read(Handler handler) {
            int i = 0;
            for (String xnecId : xnecs) {
                for (String variableId : variableIds) {
                    factors[i] = new FunctionVariableFactor(xnecId, variableId);
                    handler.onFactor(SENSITIVITY_FUNCTION_TYPE, xnecId, SensitivityVariableType.INJECTION_ACTIVE_POWER, variableId, false, ContingencyContext.none());
                    i++;
                }
            }
        }
    }

    private static class FastFLDPstSensitivityResultWriter implements SensitivityResultWriter {

        private final List<FunctionVariableFactor> factors;
        private final double[][] results;
        private final int pstIndex;
        private final Map<String, Integer> xnecIndex;
        private final Map<String, PhaseTapChanger> phaseTapChangerMap;

        public FastFLDPstSensitivityResultWriter(List<FunctionVariableFactor> factors, double[][] results, int pstIndexPos, Map<String, Integer> xnecIndex, Map<String, PhaseTapChanger> phaseTapChangerMap) {
            this.factors = factors;
            this.results = results;
            this.pstIndex = pstIndexPos;
            this.xnecIndex = xnecIndex;
            this.phaseTapChangerMap = phaseTapChangerMap;
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

            String pstId = factor.variableId();
            PhaseTapChanger phaseTapChanger = phaseTapChangerMap.get(pstId);
            Optional<PhaseTapChangerStep> neutralStep = phaseTapChanger.getNeutralStep();
            double deltaTap = 0.0;
            if (neutralStep.isPresent()) {
                deltaTap = phaseTapChanger.getCurrentStep().getAlpha() - neutralStep.get().getAlpha();
            }
            double increase = respectFlowSignConvention(deltaTap * value, functionReference);
            results[xnecIndex.get(factor.functionId())][pstIndex] += increase;
        }

        @Override
        public void writeContingencyStatus(int contingencyIndex, SensitivityAnalysisResult.Status status) {
            // We do not manage contingency yet
        }
    }

    private static class FastFLDPstSensitivityFactorReader implements SensitivityFactorReader {
        private final List<FunctionVariableFactor> factors;
        private final List<String> xnecs;
        private final List<String> pstIdList;

        public FastFLDPstSensitivityFactorReader(List<FunctionVariableFactor> factors, List<String> xnecs, List<String> pstIdList) {
            this.factors = factors;
            this.xnecs = xnecs;
            this.pstIdList = pstIdList;
        }

        @Override
        public void read(Handler handler) {
            for (String xnecId : xnecs) {
                for (String pst : pstIdList) {
                    factors.add(new FunctionVariableFactor(xnecId, pst));
                    handler.onFactor(SENSITIVITY_FUNCTION_TYPE, xnecId, SensitivityVariableType.TRANSFORMER_PHASE, pst, false, ContingencyContext.none());
                }
            }
        }
    }
}
