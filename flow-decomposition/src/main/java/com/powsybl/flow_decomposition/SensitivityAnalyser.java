/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.flow_decomposition;

import com.powsybl.contingency.ContingencyContext;
import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.sensitivity.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
class SensitivityAnalyser {
    private static final int SENSITIVITY_VARIABLE_BATCH_SIZE = 15000;
    private static final Logger LOGGER = LoggerFactory.getLogger(SensitivityAnalyser.class);
    private static final boolean SENSITIVITY_VARIABLE_SET = false;
    private final SensitivityAnalysisParameters sensitivityAnalysisParameters;
    private final Network network;
    private final List<Branch> functionList;
    private final Map<String, Integer> functionIndex;
    private final FlowDecompositionParameters parameters;

    SensitivityAnalyser(LoadFlowParameters loadFlowParameters,
                        FlowDecompositionParameters parameters,
                        Network network,
                        List<Branch> functionList,
                        Map<String, Integer> functionIndex) {
        this.sensitivityAnalysisParameters = initSensitivityAnalysisParameters(loadFlowParameters);
        this.parameters = parameters;
        this.network = network;
        this.functionList = functionList;
        this.functionIndex = functionIndex;
    }

    SensitivityAnalyser(LoadFlowParameters loadFlowParameters, FlowDecompositionParameters parameters, Network network, NetworkMatrixIndexes networkMatrixIndexes) {
        this(loadFlowParameters, parameters, network, networkMatrixIndexes.getXnecList(), networkMatrixIndexes.getXnecIndex());
    }

    private static SensitivityAnalysisParameters initSensitivityAnalysisParameters(LoadFlowParameters loadFlowParameters) {
        SensitivityAnalysisParameters parameters = SensitivityAnalysisParameters.load();
        parameters.setLoadFlowParameters(loadFlowParameters);
        LOGGER.debug("Using following sensitivity analysis parameters: {}", parameters);
        return parameters;
    }

    SparseMatrixWithIndexesTriplet run(List<String> variableList,
                                       Map<String, Integer> variableIndex,
                                       SensitivityVariableType sensitivityVariableType) {
        SparseMatrixWithIndexesTriplet sensiMatrixTriplet = initSensitivityMatrixTriplet(variableIndex);
        for (int i = 0; i < variableList.size(); i += SENSITIVITY_VARIABLE_BATCH_SIZE) {
            List<String> localNodeList = variableList.subList(i, Math.min(variableList.size(), i + SENSITIVITY_VARIABLE_BATCH_SIZE));
            partialFillSensitivityMatrix(sensitivityVariableType, sensiMatrixTriplet, localNodeList);
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
                                              List<String> localNodeList) {
        List<SensitivityFactor> factors = getFactors(localNodeList, sensitivityVariableType);
        SensitivityAnalysisResult sensitivityResult = getSensitivityAnalysisResult(factors);
        fillSensibilityMatrixTriplet(sensitivityMatrixTriplet, factors, sensitivityResult);
    }

    private List<SensitivityFactor> getFactors(List<String> variableList,
                                               SensitivityVariableType sensitivityVariableType) {
        List<SensitivityFactor> factors = new ArrayList<>();
        variableList.forEach(
            variable -> functionList.forEach(
                function -> factors.add(getSensitivityFactor(variable, function, sensitivityVariableType))));
        return factors;
    }

    private SensitivityFactor getSensitivityFactor(String variable,
                                                   Branch<?> function,
                                                   SensitivityVariableType sensitivityVariableType) {
        return new SensitivityFactor(
            SensitivityFunctionType.BRANCH_ACTIVE_POWER_1, function.getId(),
            sensitivityVariableType, variable,
            SENSITIVITY_VARIABLE_SET,
            ContingencyContext.none()
        );
    }

    private SensitivityAnalysisResult getSensitivityAnalysisResult(List<SensitivityFactor> factors) {
        return SensitivityAnalysis.run(network, factors, sensitivityAnalysisParameters);
    }

    private void fillSensibilityMatrixTriplet(
        SparseMatrixWithIndexesTriplet sensitivityMatrixTriplet,
        List<SensitivityFactor> factors,
        SensitivityAnalysisResult sensiResult) {
        for (SensitivityValue sensitivityValue : sensiResult.getValues()) {
            fillSensitivityMatrixCell(sensitivityMatrixTriplet, factors, sensitivityValue);
        }
    }

    private void fillSensitivityMatrixCell(SparseMatrixWithIndexesTriplet sensitivityMatrixTriplet,
                                           List<SensitivityFactor> factors, SensitivityValue sensitivityValue) {
        SensitivityFactor factor = factors.get(sensitivityValue.getFactorIndex());
        double sensitivity = sensitivityValue.getValue();
        double referenceOrientedSensitivity = sensitivityValue.getFunctionReference() < 0 ?
            -sensitivity : sensitivity;
        sensitivityMatrixTriplet.addItem(factor.getFunctionId(), factor.getVariableId(), referenceOrientedSensitivity);
    }
}
