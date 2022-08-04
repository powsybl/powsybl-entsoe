/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.flow_decomposition;

import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.sensitivity.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
class SensitivityAnalyser extends AbstractSensitivityAnalyser {
    private static final int SENSITIVITY_VARIABLE_BATCH_SIZE = 15000;
    private static final Logger LOGGER = LoggerFactory.getLogger(SensitivityAnalyser.class);
    private static final boolean SENSITIVITY_VARIABLE_SET = false;
    private final Network network;
    private final List<Xnec> functionList;
    private final Map<String, Integer> functionIndex;
    private final FlowDecompositionParameters parameters;

    SensitivityAnalyser(LoadFlowParameters loadFlowParameters,
                        FlowDecompositionParameters parameters,
                        Network network,
                        List<Xnec> functionList,
                        Map<String, Integer> functionIndex) {
        super(loadFlowParameters);
        this.parameters = parameters;
        this.network = network;
        this.functionList = functionList;
        this.functionIndex = functionIndex;
    }

    SensitivityAnalyser(LoadFlowParameters loadFlowParameters, FlowDecompositionParameters parameters, Network network, NetworkMatrixIndexes networkMatrixIndexes) {
        this(loadFlowParameters, parameters, network, networkMatrixIndexes.getXnecList(), networkMatrixIndexes.getXnecIndex());
    }

    SparseMatrixWithIndexesTriplet run(List<String> variableList,
                                       Map<String, Integer> variableIndex,
                                       SensitivityVariableType sensitivityVariableType) {
        SparseMatrixWithIndexesTriplet sensiMatrixTriplet = initSensitivityMatrixTriplet(variableIndex);
        for (int i = 0; i < variableList.size(); i += SENSITIVITY_VARIABLE_BATCH_SIZE) {
            List<String> localVariableList = variableList.subList(i, Math.min(variableList.size(), i + SENSITIVITY_VARIABLE_BATCH_SIZE));
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
        List<SensitivityFactor> factors = getFactors(variableList, sensitivityVariableType);
        SensitivityAnalysisResult sensitivityResult = getSensitivityAnalysisResult(factors);
        fillSensibilityMatrixTriplet(sensitivityMatrixTriplet, factors, sensitivityResult);
    }

    private List<SensitivityFactor> getFactors(List<String> variableList,
                                               SensitivityVariableType sensitivityVariableType) {
        return getFactors(variableList, functionList, sensitivityVariableType, SENSITIVITY_VARIABLE_SET);
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
        String functionId = Xnec.createId(factor.getFunctionId(), network.getVariantManager().getWorkingVariantId());
        sensitivityMatrixTriplet.addItem(functionId, factor.getVariableId(), referenceOrientedSensitivity);
    }
}
