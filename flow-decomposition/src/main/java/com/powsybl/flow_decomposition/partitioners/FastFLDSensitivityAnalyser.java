package com.powsybl.flow_decomposition.partitioners;

import com.powsybl.contingency.ContingencyContext;
import com.powsybl.flow_decomposition.AbstractSensitivityAnalyser;
import com.powsybl.flow_decomposition.FunctionVariableFactor;
import com.powsybl.flow_decomposition.NetworkUtil;
import com.powsybl.iidm.network.*;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.sensitivity.*;
import org.ejml.data.DMatrixSparse;
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
    private static final Logger LOGGER = LoggerFactory.getLogger(FastFLDSensitivityAnalyser.class);
    private final Network network;
    private final Set<Branch<?>> xnecs;
    private final Map<String, Integer> vertexIdMapping;
    private final DMatrixSparseCSC pexMatrix;
    private final Set<String> flowParts;
    private final String[] vertexIds;
    private final boolean[] isBusByVertexIndex;
    private final Country[] countriesByVertexPos;
    private final Map<Country, Integer> countryIndex;
    private final String[] injByVertexId;

    public FastFLDSensitivityAnalyser(LoadFlowParameters loadFlowParameters, SensitivityAnalysis.Runner runner, Network network, Set<Branch<?>> xnecs, Map<String, Integer> vertexIdMapping, DMatrixSparseCSC pexMatrix, List<Bus> busesInMainSynchronousComponent) {
        super(loadFlowParameters, runner);
        this.network = network;
        this.xnecs = xnecs;
        this.vertexIdMapping = vertexIdMapping;
        this.pexMatrix = pexMatrix;
        this.flowParts = network.getCountries().stream()
            .map(NetworkUtil::getLoopFlowIdFromCountry)
            .collect(Collectors.toSet());
        flowParts.addAll(Set.of(ALLOCATED_COLUMN_NAME, XNODE_COLUMN_NAME));

        Map<String, String> anyInjectionOnBus = busesInMainSynchronousComponent.stream().collect(Collectors.toMap(Identifiable::getId, bus -> NetworkUtil.getInjectionStream(bus).findAny().orElseThrow().getId()));
        Map<String, Bus> idToBus = new HashMap<>();
        busesInMainSynchronousComponent.forEach(bus -> idToBus.put(bus.getId(), bus));

        int nVertex = vertexIdMapping.size();
        this.vertexIds = new String[nVertex];
        this.isBusByVertexIndex = new boolean[nVertex];
        this.countriesByVertexPos = new Country[nVertex];
        this.injByVertexId = new String[nVertex];

        this.countryIndex = NetworkUtil.getIndex(busesInMainSynchronousComponent.stream().map(bus -> bus.getVoltageLevel().getSubstation().orElseThrow().getCountry().orElse(null)).distinct().toList());
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

    private static String getNegativeFlowPartName(String flowPart) {
        return "Negative " + flowPart;
    }

    private static String getPositiveFlowPartName(String flowPart) {
        return "Positive " + flowPart;
    }

    private static double respectFlowSignConvention(double ptdfValue, double referenceFlow) {
        return referenceFlow < 0 ? -ptdfValue : ptdfValue;
    }

    public Map<String, Map<String, Double>> run() {
        Map<String, Map<String, Double>> sensitivityVarMap = new HashMap<>();
        Map<String, List<WeightedSensitivityVariable>> sensitivityVariablesMap = new HashMap<>();
        Map<String, Double> pexSumByFlowPart = new HashMap<>();
        for (String flowPart : flowParts) {
            sensitivityVariablesMap.put(getPositiveFlowPartName(flowPart), new ArrayList<>());
            sensitivityVariablesMap.put(getNegativeFlowPartName(flowPart), new ArrayList<>());
            sensitivityVarMap.put(getPositiveFlowPartName(flowPart), new HashMap<>());
            sensitivityVarMap.put(getNegativeFlowPartName(flowPart), new HashMap<>());
        }
        Iterator<DMatrixSparse.CoordinateRealValue> coordinateRealValueIterator = pexMatrix.createCoordinateIterator();
        while (coordinateRealValueIterator.hasNext()) {
            DMatrixSparse.CoordinateRealValue e = coordinateRealValueIterator.next();
            int sourceIndex = e.row;
            int sinkIndex = e.col;
            double exchangeBetweenFromAndTo = e.value;
            String sourceInjId = injByVertexId[sourceIndex];
            String sinkInjId = injByVertexId[sinkIndex];
            WeightedSensitivityVariable sourceVariable = new WeightedSensitivityVariable(sourceInjId, exchangeBetweenFromAndTo);
            WeightedSensitivityVariable sinkVariable = new WeightedSensitivityVariable(sinkInjId, -exchangeBetweenFromAndTo);
            if ((isBusByVertexIndex[sourceIndex] && isBusByVertexIndex[sinkIndex])) {
                Country sourceCountry = countriesByVertexPos[sourceIndex];
                Country sinkCountry = countriesByVertexPos[sinkIndex];
                if (sourceCountry == null || sinkCountry == null) {
                    String sourceVertexId = vertexIds[sourceIndex];
                    String sinkVertexId = vertexIds[sinkIndex];
                    LOGGER.warn("Cannot compute loop flow for bus {} and {} because of invalid country", sourceVertexId, sinkVertexId);
                    continue;
                }
                if (sourceCountry.equals(sinkCountry)) {
                    String loopFlowIdFromCountry = NetworkUtil.getLoopFlowIdFromCountry(sourceCountry);
                    sensitivityVarMap.get(getPositiveFlowPartName(loopFlowIdFromCountry)).put(sourceInjId, exchangeBetweenFromAndTo);
                    sensitivityVarMap.get(getNegativeFlowPartName(loopFlowIdFromCountry)).put(sinkInjId, -exchangeBetweenFromAndTo);
                } else {
                    sensitivityVarMap.get(getPositiveFlowPartName(ALLOCATED_COLUMN_NAME)).put(sourceInjId, exchangeBetweenFromAndTo);
                    sensitivityVarMap.get(getNegativeFlowPartName(ALLOCATED_COLUMN_NAME)).put(sinkInjId, -exchangeBetweenFromAndTo);
                }
            } else {
                sensitivityVarMap.get(getPositiveFlowPartName(XNODE_COLUMN_NAME)).put(sourceInjId, exchangeBetweenFromAndTo);
                sensitivityVarMap.get(getNegativeFlowPartName(XNODE_COLUMN_NAME)).put(sinkInjId, -exchangeBetweenFromAndTo);
            }
        }
        List<SensitivityVariableSet> sensitivityVariableSets = sensitivityVarMap.entrySet().stream()
            .map(entry -> {
                double sum = entry.getValue().values().stream().mapToDouble(Double::doubleValue).sum();
                String flowPart = entry.getKey();
                pexSumByFlowPart.put(flowPart, sum);
                List<WeightedSensitivityVariable> weightedSensitivityVariables = entry.getValue().entrySet().stream()
                    .map(e -> new WeightedSensitivityVariable(e.getKey(), e.getValue() / sum))
                    .toList();
                return new SensitivityVariableSet(flowPart, weightedSensitivityVariables);
            })
            .toList();

        List<FunctionVariableFactor> sensitivityFactors = new ArrayList<>();
        SensitivityFactorReader factorReader = new FastFLDSensitivityFactorReader(flowParts, sensitivityFactors);
        Map<String, Map<String, Double>> results = new HashMap<>();
        SensitivityResultWriter valueWriter = new FastFLDSensitivityResultWriter(sensitivityFactors, results, flowParts, pexSumByFlowPart);
        runSensitivityAnalysis(network, factorReader, valueWriter, sensitivityVariableSets);
        return results;
    }

    private class FastFLDSensitivityResultWriter implements SensitivityResultWriter {

        private final List<FunctionVariableFactor> factors;
        private final Map<String, Map<String, Double>> results;
        private final Set<String> flowParts;
        private final Map<String, Double> pexSumByFlowPart;

        public FastFLDSensitivityResultWriter(List<FunctionVariableFactor> factors, Map<String, Map<String, Double>> results, Set<String> flowParts, Map<String, Double> pexSumByFlowPart) {
            this.factors = factors;
            this.results = results;
            this.flowParts = flowParts;
            this.pexSumByFlowPart = pexSumByFlowPart;
        }

        @Override
        public void writeSensitivityValue(int factorIndex, int contingencyIndex, double value, double functionReference) {
            if (Double.isNaN(value)) {
                return;
            }
            FunctionVariableFactor factor = factors.get(factorIndex);
            Map<String, Double> flowDecomposition = results.computeIfAbsent(factor.functionId(), s -> new HashMap<>());
            for (String flowPart : flowParts) {
                if (factor.variableId().equals(getPositiveFlowPartName(flowPart))) {
                    double partialFlowPartValue = flowDecomposition.getOrDefault(flowPart, 0.0);
                    Double pexSum = pexSumByFlowPart.get(getPositiveFlowPartName(flowPart));
                    double increase = respectFlowSignConvention(value * pexSum, functionReference);
                    flowDecomposition.put(flowPart, partialFlowPartValue + increase);
                    return;
                } else if (factor.variableId().equals(getNegativeFlowPartName(flowPart))) {
                    double partialFlowPartValue = flowDecomposition.getOrDefault(flowPart, 0.0);
                    Double pexSum = pexSumByFlowPart.get(getNegativeFlowPartName(flowPart));
                    double increase = respectFlowSignConvention(value * pexSum, functionReference);
                    flowDecomposition.put(flowPart, partialFlowPartValue + increase);
                    return;
                }
            }

            PhaseTapChanger phaseTapChanger = network.getTwoWindingsTransformer(factor.variableId()).getPhaseTapChanger();
            Optional<PhaseTapChangerStep> neutralStep = phaseTapChanger.getNeutralStep();
            double deltaTap = 0.0;
            if (neutralStep.isPresent()) {
                deltaTap = phaseTapChanger.getCurrentStep().getAlpha() - neutralStep.get().getAlpha();
            }
            double pstFlow = flowDecomposition.getOrDefault(PST_COLUMN_NAME, 0.0);
            flowDecomposition.put(PST_COLUMN_NAME, pstFlow + respectFlowSignConvention(deltaTap * value, functionReference));
        }

        @Override
        public void writeContingencyStatus(int contingencyIndex, SensitivityAnalysisResult.Status status) {
            // We do not manage contingency yet
        }
    }

    private class FastFLDSensitivityFactorReader implements SensitivityFactorReader {
        private final Set<String> flowParts;
        private final List<FunctionVariableFactor> factors;

        public FastFLDSensitivityFactorReader(Set<String> flowParts, List<FunctionVariableFactor> factors) {
            this.flowParts = flowParts;
            this.factors = factors;
        }

        @Override
        public void read(Handler handler) {
            for (Branch<?> xnec : xnecs) {
                for (String flowPart : flowParts) {
                    factors.add(new FunctionVariableFactor(xnec.getId(), getPositiveFlowPartName(flowPart)));
                    handler.onFactor(SENSITIVITY_FUNCTION_TYPE, xnec.getId(), SensitivityVariableType.INJECTION_ACTIVE_POWER, getPositiveFlowPartName(flowPart), true, ContingencyContext.none());
                    factors.add(new FunctionVariableFactor(xnec.getId(), getNegativeFlowPartName(flowPart)));
                    handler.onFactor(SENSITIVITY_FUNCTION_TYPE, xnec.getId(), SensitivityVariableType.INJECTION_ACTIVE_POWER, getNegativeFlowPartName(flowPart), true, ContingencyContext.none());
                }

                for (String pst : NetworkUtil.getPstIdList(network)) {
                    factors.add(new FunctionVariableFactor(xnec.getId(), pst));
                    handler.onFactor(SENSITIVITY_FUNCTION_TYPE, xnec.getId(), SensitivityVariableType.TRANSFORMER_PHASE, pst, false, ContingencyContext.none());
                }
            }
        }
    }
}
