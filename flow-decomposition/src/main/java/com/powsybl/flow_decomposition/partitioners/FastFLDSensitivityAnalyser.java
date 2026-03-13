/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
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
import org.jspecify.annotations.NonNull;
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

    private static List<WeightedSensitivityVariable> normalizeVariables(List<WeightedSensitivityVariable> variables, double total) {
        return variables.stream()
            .map(variable -> new WeightedSensitivityVariable(variable.getId(), variable.getWeight() / total))
            .toList();
    }

    public Map<String, Map<String, Double>> run() {
        List<String> flowPartNameList = new ArrayList<>(List.of(PST_COLUMN_NAME, ALLOCATED_COLUMN_NAME, XNODE_COLUMN_NAME));
        network.getCountries().forEach(country -> flowPartNameList.add(NetworkUtil.getLoopFlowIdFromCountry(country)));
        Map<String, Integer> flowPartIndex = NetworkUtil.getIndex(flowPartNameList);
        int nFlowPart = flowPartIndex.size();
        Map<String, Integer> xnecIndex = NetworkUtil.getIndex(xnecIds);
        int nXnec = xnecIndex.size();

        Map<String, double[]> exchangePerFlowPart = buildExchangePerfFlowPart(nFlowPart, flowPartIndex);

        double[][] results = new double[nXnec][nFlowPart];

        runSensitivityAnalysisAndFillResultsForPstFlow(flowPartIndex, xnecIndex, results);
        runSensitivityAnalysisAndFillResultsForPex(exchangePerFlowPart, flowPartNameList, results);

        return xnecIds.stream()
            .collect(Collectors.toMap(
                xnecId -> xnecId,
                xnecId -> flowPartNameList.stream().collect(Collectors.toMap(
                    flowPartName -> flowPartName,
                    flowPartName -> results[xnecIndex.get(xnecId)][flowPartIndex.get(flowPartName)]
                ))));
    }

    private void runSensitivityAnalysisAndFillResultsForPex(Map<String, double[]> exchangePerFlowPart, List<String> flowPartNameList, double[][] results) {
        LOGGER.debug("Running sensitivity analysis for decomposed flow");
        VariableSetBuildResult variableSetBuildResult = buildVariableSets(exchangePerFlowPart, flowPartNameList);
        GroupedFLDFactor[] groupedFactors = new GroupedFLDFactor[variableSetBuildResult.factorCount()];
        SensitivityFactorReader factorReader = new FastFLDGroupedSensitivityFactorReader(xnecIds, variableSetBuildResult.aggregations(), groupedFactors);
        SensitivityResultWriter valueWriter = new FastFLDGroupedSensitivityResultWriter(groupedFactors, results, variableSetBuildResult.aggregations());
        runSensitivityAnalysis(network, factorReader, valueWriter, variableSetBuildResult.variableSets());
    }

    private void runSensitivityAnalysisAndFillResultsForPstFlow(Map<String, Integer> flowPartIndex, Map<String, Integer> xnecIndex, double[][] results) {
        LOGGER.debug("Running sensitivity analysis for PST flow for variables");
        List<FunctionVariableFactor> sensitivityFactorsPst = new ArrayList<>();
        List<String> pstIdList = NetworkUtil.getPstIdList(network);
        Map<String, PhaseTapChanger> phaseTapChangerMap = pstIdList.stream().collect(Collectors.toMap(pstId -> pstId, pstId -> network.getTwoWindingsTransformer(pstId).getPhaseTapChanger()));
        SensitivityFactorReader factorReaderPst = new FastFLDPstSensitivityFactorReader(sensitivityFactorsPst, xnecIds, pstIdList);
        SensitivityResultWriter valueWriterPst = new FastFLDPstSensitivityResultWriter(sensitivityFactorsPst, results, flowPartIndex.get(PST_COLUMN_NAME), xnecIndex, phaseTapChangerMap);
        runSensitivityAnalysis(network, factorReaderPst, valueWriterPst, Collections.emptyList());
    }

    private @NonNull Map<String, double[]> buildExchangePerfFlowPart(int nFlowPart, Map<String, Integer> flowPartIndex) {
        Map<String, double[]> exchangePerFlowPart = new HashMap<>();
        Streams.stream(pexMatrix.createCoordinateIterator())
            .forEach(coordinateRealValue -> {
                int sourceIndex = coordinateRealValue.row;
                int sinkIndex = coordinateRealValue.col;
                double exchangeBetweenFromAndTo = coordinateRealValue.value;
                String sourceInjId = Optional.ofNullable(injByVertexId[sourceIndex]).orElse(vertexIds[sourceIndex]);
                String sinkInjId = Optional.ofNullable(injByVertexId[sinkIndex]).orElse(vertexIds[sinkIndex]);
                String flowPartName = computeFlowPartName(sourceIndex, sinkIndex);
                exchangePerFlowPart.computeIfAbsent(sourceInjId, s1 -> new double[nFlowPart])[flowPartIndex.get(flowPartName)] += exchangeBetweenFromAndTo;
                exchangePerFlowPart.computeIfAbsent(sinkInjId, s -> new double[nFlowPart])[flowPartIndex.get(flowPartName)] -= exchangeBetweenFromAndTo;
            });
        return exchangePerFlowPart;
    }

    private String computeFlowPartName(int sourceIndex, int sinkIndex) {
        if (isBusByVertexIndex[sourceIndex] && isBusByVertexIndex[sinkIndex]) {
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

    private VariableSetBuildResult buildVariableSets(Map<String, double[]> exchangePerFlowPart, List<String> flowPartNameList) {
        List<SensitivityVariableSet> variableSets = new ArrayList<>();
        FlowPartAggregation[] aggregations = new FlowPartAggregation[flowPartNameList.size()];
        int factorPerXnec = 0;

        for (int flowPartIndex = 0; flowPartIndex < flowPartNameList.size(); flowPartIndex++) {
            String flowPartName = flowPartNameList.get(flowPartIndex);

            List<WeightedSensitivityVariable> positiveVariables = new ArrayList<>();
            List<WeightedSensitivityVariable> negativeVariables = new ArrayList<>();
            double positiveTotal = 0.0;
            double negativeTotal = 0.0;

            for (Map.Entry<String, double[]> entry : exchangePerFlowPart.entrySet()) {
                double exchange = entry.getValue()[flowPartIndex];
                if (exchange > 0.0) {
                    positiveVariables.add(new WeightedSensitivityVariable(entry.getKey(), exchange));
                    positiveTotal += exchange;
                } else if (exchange < 0.0) {
                    double absExchange = -exchange;
                    negativeVariables.add(new WeightedSensitivityVariable(entry.getKey(), absExchange));
                    negativeTotal += absExchange;
                }
            }

            String positiveSetId = null;
            String negativeSetId = null;

            if (positiveTotal > 0.0) {
                positiveSetId = flowPartName + "_POS";
                variableSets.add(new SensitivityVariableSet(positiveSetId, normalizeVariables(positiveVariables, positiveTotal)));
                factorPerXnec++;
            }
            if (negativeTotal > 0.0) {
                negativeSetId = flowPartName + "_NEG";
                variableSets.add(new SensitivityVariableSet(negativeSetId, normalizeVariables(negativeVariables, negativeTotal)));
                factorPerXnec++;
            }

            aggregations[flowPartIndex] = new FlowPartAggregation(flowPartIndex, positiveSetId, negativeSetId, positiveTotal, negativeTotal);
        }

        return new VariableSetBuildResult(variableSets, aggregations, factorPerXnec * xnecIds.size());
    }

    @SuppressWarnings("java:S6218") // We do not want to generate code that will not be used just to please Sonar...
    private record FastFLDGroupedSensitivityResultWriter(GroupedFLDFactor[] factors, double[][] results,
                                                         FlowPartAggregation[] aggregations) implements SensitivityResultWriter {

        @Override
        public void writeSensitivityValue(int factorIndex, int contingencyIndex, double value, double functionReference) {
            if (Double.isNaN(value) || value == 0.0) {
                return;
            }

            GroupedFLDFactor factor = factors[factorIndex];
            FlowPartAggregation aggregation = aggregations[factor.flowPartIndex()];

            double increase = factor.positive()
                ? aggregation.positiveTotal() * value
                : -aggregation.negativeTotal() * value;

            double increaseWithSign = respectFlowSignConvention(increase, functionReference);
            results[factor.iXnec()][factor.flowPartIndex()] += increaseWithSign;
        }

        @Override
        public void writeContingencyStatus(int contingencyIndex, SensitivityAnalysisResult.Status status) {
            // We do not manage contingency yet
        }
    }

    @SuppressWarnings("java:S6218") // We do not want to generate code that will not be used just to please Sonar...
    private record FastFLDGroupedSensitivityFactorReader(List<String> xnecs, FlowPartAggregation[] aggregations,
                                                         GroupedFLDFactor[] factors) implements SensitivityFactorReader {

        @Override
        public void read(Handler handler) {
            int i = 0;
            int iXnec = 0;
            for (String xnecId : xnecs) {
                for (FlowPartAggregation aggregation : aggregations) {
                    if (aggregation.positiveSetId() != null) {
                        factors[i] = new GroupedFLDFactor(iXnec, aggregation.flowPartIndex(), true);
                        handler.onFactor(SENSITIVITY_FUNCTION_TYPE, xnecId, SensitivityVariableType.INJECTION_ACTIVE_POWER, aggregation.positiveSetId(), true, ContingencyContext.none());
                        i++;
                    }
                    if (aggregation.negativeSetId() != null) {
                        factors[i] = new GroupedFLDFactor(iXnec, aggregation.flowPartIndex(), false);
                        handler.onFactor(SENSITIVITY_FUNCTION_TYPE, xnecId, SensitivityVariableType.INJECTION_ACTIVE_POWER, aggregation.negativeSetId(), true, ContingencyContext.none());
                        i++;
                    }
                }
                iXnec++;
            }
        }
    }

    private record GroupedFLDFactor(int iXnec, int flowPartIndex, boolean positive) {
    }

    private record FlowPartAggregation(int flowPartIndex, String positiveSetId, String negativeSetId,
                                       double positiveTotal, double negativeTotal) {
    }

    @SuppressWarnings("java:S6218") // We do not want to generate code that will not be used just to please Sonar...
    private record VariableSetBuildResult(List<SensitivityVariableSet> variableSets, FlowPartAggregation[] aggregations,
                                          int factorCount) {
    }

    @SuppressWarnings("java:S6218") // We do not want to generate code that will not be used just to please Sonar...
    private record FastFLDPstSensitivityResultWriter(List<FunctionVariableFactor> factors, double[][] results,
                                                     int pstIndex, Map<String, Integer> xnecIndex,
                                                     Map<String, PhaseTapChanger> phaseTapChangerMap) implements SensitivityResultWriter {

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

    private record FastFLDPstSensitivityFactorReader(List<FunctionVariableFactor> factors, List<String> xnecs,
                                                     List<String> pstIdList) implements SensitivityFactorReader {

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
