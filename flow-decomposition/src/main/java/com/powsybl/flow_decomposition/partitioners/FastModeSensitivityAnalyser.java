/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.flow_decomposition.partitioners;

import com.powsybl.contingency.ContingencyContext;
import com.powsybl.flow_decomposition.AbstractSensitivityAnalyser;
import com.powsybl.flow_decomposition.FunctionVariableFactor;
import com.powsybl.flow_decomposition.NetworkUtil;
import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.PhaseTapChanger;
import com.powsybl.iidm.network.PhaseTapChangerStep;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.sensitivity.*;

import java.util.*;

import static com.powsybl.flow_decomposition.DecomposedFlow.PST_COLUMN_NAME;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
public class FastModeSensitivityAnalyser extends AbstractSensitivityAnalyser {
    private final Network network;
    private final Set<Branch> xnecs;
    private final Set<String> flowParts;
    private final Map<String, Map<String, Double>> nodalInjectionPartitions;

    FastModeSensitivityAnalyser(LoadFlowParameters loadFlowParameters,
                                SensitivityAnalysis.Runner runner,
                                Network network,
                                Set<Branch> xnecs,
                                SparseMatrixWithIndexesTriplet nodalInjectionsMatrix) {
        super(loadFlowParameters, runner);
        this.network = network;
        this.xnecs = xnecs;
        this.flowParts = nodalInjectionsMatrix.colIndex.keySet();
        this.nodalInjectionPartitions = nodalInjectionsMatrix.toMap();
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
        List<SensitivityVariableSet> sensitivityVariableSets = new ArrayList<>();
        List<FunctionVariableFactor> sensitivityFactors = new ArrayList<>();
        Map<String, Double> nodalInjectionsPartitionSumByFlowPart = new HashMap<>();
        for (String flowPart : flowParts) {
            String positiveFlowPartName = getPositiveFlowPartName(flowPart);
            double positiveFlowPartSum = nodalInjectionPartitions.values().stream().filter(stringDoubleMap -> stringDoubleMap.containsKey(flowPart) && stringDoubleMap.get(flowPart) > 0).mapToDouble(stringDoubleMap -> stringDoubleMap.get(flowPart)).sum();
            String negativeFlowPartName = getNegativeFlowPartName(flowPart);
            double negativeFlowPartSum = nodalInjectionPartitions.values().stream().filter(stringDoubleMap -> stringDoubleMap.containsKey(flowPart) && stringDoubleMap.get(flowPart) < 0).mapToDouble(stringDoubleMap -> stringDoubleMap.get(flowPart)).sum();
            sensitivityVariableSets.add(new SensitivityVariableSet(
                    positiveFlowPartName,
                    nodalInjectionPartitions.entrySet().stream().filter(entry -> entry.getValue().containsKey(flowPart) && entry.getValue().get(flowPart) > 0).map(entry -> new WeightedSensitivityVariable(entry.getKey(), entry.getValue().get(flowPart) / positiveFlowPartSum)).toList()));
            nodalInjectionsPartitionSumByFlowPart.put(positiveFlowPartName, positiveFlowPartSum);
            sensitivityVariableSets.add(new SensitivityVariableSet(
                    negativeFlowPartName,
                    nodalInjectionPartitions.entrySet().stream().filter(entry -> entry.getValue().containsKey(flowPart) && entry.getValue().get(flowPart) < 0).map(entry -> new WeightedSensitivityVariable(entry.getKey(), entry.getValue().get(flowPart) / negativeFlowPartSum)).toList()));
            nodalInjectionsPartitionSumByFlowPart.put(negativeFlowPartName, negativeFlowPartSum);
        }

        SensitivityFactorReader factorReader = new FastModeSensitivityFactorReader(flowParts, sensitivityFactors);
        Map<String, Map<String, Double>> results = new HashMap<>();
        SensitivityResultWriter valueWriter = new FastModeSensitivityResultWriter(sensitivityFactors, results, flowParts, nodalInjectionsPartitionSumByFlowPart);
        runSensitivityAnalysis(network, factorReader, valueWriter, sensitivityVariableSets);
        return results;
    }

    private class FastModeSensitivityResultWriter implements SensitivityResultWriter {

        private final List<FunctionVariableFactor> factors;
        private final Map<String, Map<String, Double>> results;
        private final Set<String> flowParts;
        private final Map<String, Double> nodalInjectionsPartitionSumByFlowPart;

        public FastModeSensitivityResultWriter(List<FunctionVariableFactor> factors, Map<String, Map<String, Double>> results, Set<String> flowParts, Map<String, Double> nodalInjectionsPartitionSumByFlowPart) {
            this.factors = factors;
            this.results = results;
            this.flowParts = flowParts;
            this.nodalInjectionsPartitionSumByFlowPart = nodalInjectionsPartitionSumByFlowPart;
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
                    flowDecomposition.put(flowPart, partialFlowPartValue + respectFlowSignConvention(value * nodalInjectionsPartitionSumByFlowPart.get(getPositiveFlowPartName(flowPart)), functionReference));
                    return;
                } else if (factor.variableId().equals(getNegativeFlowPartName(flowPart))) {
                    double partialFlowPartValue = flowDecomposition.getOrDefault(flowPart, 0.0);
                    flowDecomposition.put(flowPart, partialFlowPartValue + respectFlowSignConvention(value * nodalInjectionsPartitionSumByFlowPart.get(getNegativeFlowPartName(flowPart)), functionReference));
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

    private class FastModeSensitivityFactorReader implements SensitivityFactorReader {
        private final Set<String> flowParts;
        private final List<FunctionVariableFactor> factors;

        public FastModeSensitivityFactorReader(Set<String> flowParts, List<FunctionVariableFactor> factors) {
            this.flowParts = flowParts;
            this.factors = factors;
        }

        @Override
        public void read(Handler handler) {
            for (Branch xnec : xnecs) {
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
