/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.flow_decomposition;

import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.sensitivity.SensitivityAnalysis;
import com.powsybl.sensitivity.SensitivityAnalysisResult;
import com.powsybl.sensitivity.SensitivityFactorReader;
import com.powsybl.sensitivity.SensitivityResultWriter;
import com.powsybl.sensitivity.SensitivityVariableSet;
import com.powsybl.sensitivity.SensitivityVariableType;
import org.jgrapht.alg.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
class SensitivityAnalyser extends AbstractSensitivityAnalyser {
    private static final Logger LOGGER = LoggerFactory.getLogger(SensitivityAnalyser.class);
    private static final boolean SENSITIVITY_VARIABLE_SET = false;
    public static final List<SensitivityVariableSet> EMPTY_SENSITIVITY_VARIABLE_SETS = Collections.emptyList();
    private final Network network;
    private final List<Branch> functionList;
    private final Map<String, Integer> functionIndex;
    private final FlowDecompositionParameters parameters;

    SensitivityAnalyser(LoadFlowParameters loadFlowParameters,
                        FlowDecompositionParameters parameters,
                        SensitivityAnalysis.Runner runner,
                        Network network,
                        List<Branch> functionList,
                        Map<String, Integer> functionIndex) {
        super(loadFlowParameters, runner);
        this.parameters = parameters;
        this.network = network;
        this.functionList = functionList;
        this.functionIndex = functionIndex;
    }

    SensitivityAnalyser(LoadFlowParameters loadFlowParameters, FlowDecompositionParameters parameters, SensitivityAnalysis.Runner runner, Network network, NetworkMatrixIndexes networkMatrixIndexes) {
        this(loadFlowParameters, parameters, runner, network, networkMatrixIndexes.getXnecList(), networkMatrixIndexes.getXnecIndex());
    }

    SparseMatrixWithIndexesTriplet run(List<String> variableList,
                                       Map<String, Integer> variableIndex,
                                       SensitivityVariableType sensitivityVariableType) {
        SparseMatrixWithIndexesTriplet sensiMatrixTriplet = initSensitivityMatrixTriplet(variableIndex);
        for (int i = 0; i < variableList.size(); i += parameters.getSensitivityVariableBatchSize()) {
            List<String> localVariableList = variableList.subList(i, Math.min(variableList.size(), i + parameters.getSensitivityVariableBatchSize()));
            partialFillSensitivityMatrix(sensitivityVariableType, sensiMatrixTriplet, localVariableList);
        }
        return sensiMatrixTriplet;
    }

    private SparseMatrixWithIndexesTriplet initSensitivityMatrixTriplet(Map<String, Integer> variableIndex) {
        LOGGER.debug("Filtering Sensitivity values with epsilon = {}", parameters.getSensitivityEpsilon());
        return new SparseMatrixWithIndexesTriplet(functionIndex,
            variableIndex,
            functionIndex.size() * variableIndex.size(),
            parameters.getSensitivityEpsilon());
    }

    private void partialFillSensitivityMatrix(SensitivityVariableType sensitivityVariableType,
                                              SparseMatrixWithIndexesTriplet sensitivityMatrixTriplet,
                                              List<String> variableList) {
        List<Pair<String, String>> factors = getFunctionVariableFactors(variableList, functionList);
        fillSensitivityAnalysisResult(factors, sensitivityMatrixTriplet, sensitivityVariableType);
    }

    private void fillSensitivityAnalysisResult(List<Pair<String, String>> factors, SparseMatrixWithIndexesTriplet sensitivityMatrixTriplet, SensitivityVariableType sensitivityVariableType) {
        SensitivityFactorReader factorReader = getSensitivityFactorReader(factors, sensitivityVariableType, SENSITIVITY_VARIABLE_SET);
        SensitivityResultWriter valueWriter = getSensitivityResultWriter(factors, sensitivityMatrixTriplet);
        runSensitivityAnalysis(network, factorReader, valueWriter, EMPTY_SENSITIVITY_VARIABLE_SETS);
    }

    public static double respectFlowSignConvention(double ptdfValue, double referenceFlow) {
        return referenceFlow < 0 ? -ptdfValue : ptdfValue;
    }

    private static SensitivityResultWriter getSensitivityResultWriter(List<Pair<String, String>> factors, SparseMatrixWithIndexesTriplet sensitivityMatrixTriplet) {
        return new SensitivityResultWriter() {
            @Override
            public void writeSensitivityValue(int factorIndex, int contingencyIndex, double value, double functionReference) {
                Pair<String, String> factor = factors.get(factorIndex);
                sensitivityMatrixTriplet.addItem(factor.getFirst(), factor.getSecond(), respectFlowSignConvention(value, functionReference));
            }

            @Override
            public void writeContingencyStatus(int contingencyIndex, SensitivityAnalysisResult.Status status) {
                // We do not manage contingency yet
            }
        };
    }
}
