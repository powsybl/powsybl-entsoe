/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.flow_decomposition;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.computation.local.LocalComputationManager;
import com.powsybl.contingency.Contingency;
import com.powsybl.contingency.ContingencyContext;
import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.sensitivity.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 */
public abstract class AbstractSensitivityAnalyser {
    public static final SensitivityFunctionType SENSITIVITY_FUNCTION_TYPE = SensitivityFunctionType.BRANCH_ACTIVE_POWER_1;
    public static final List<Contingency> CONTINGENCIES = Collections.emptyList();
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractSensitivityAnalyser.class);
    private static final boolean DC_LOAD_FLOW = true;
    protected final SensitivityAnalysisParameters sensitivityAnalysisParameters;
    protected final SensitivityAnalysis.Runner runner;

    protected AbstractSensitivityAnalyser(LoadFlowParameters loadFlowParameters, SensitivityAnalysis.Runner runner) {
        this.sensitivityAnalysisParameters = initSensitivityAnalysisParameters(loadFlowParameters);
        this.runner = runner;
    }

    protected static SensitivityAnalysisParameters initSensitivityAnalysisParameters(LoadFlowParameters loadFlowParameters) {
        SensitivityAnalysisParameters parameters = SensitivityAnalysisParameters.load();
        parameters.setLoadFlowParameters(enforceDcLoadFlowCalculation(loadFlowParameters));
        LOGGER.debug("Using following sensitivity analysis parameters: {}", parameters);
        return parameters;
    }

    private static LoadFlowParameters enforceDcLoadFlowCalculation(LoadFlowParameters initialLoadFlowParameters) {
        LoadFlowParameters dcEnforcedParameters = initialLoadFlowParameters.copy();
        dcEnforcedParameters.setDc(DC_LOAD_FLOW);
        return dcEnforcedParameters;
    }

    protected List<FunctionVariableFactor> getFunctionVariableFactors(List<String> variableList,
                                                                      List<Branch<?>> functionList) {
        List<FunctionVariableFactor> factors = new ArrayList<>();
        variableList.forEach(
                variable -> functionList.forEach(
                        function -> factors.add(new FunctionVariableFactor(function.getId(), variable))));
        return factors;
    }

    protected static SensitivityFactorReader getSensitivityFactorReader(List<FunctionVariableFactor> factors, SensitivityVariableType sensitivityVariableType, boolean sensitivityVariableSet) {
        return handler -> factors.forEach(
                pair -> handler.onFactor(SENSITIVITY_FUNCTION_TYPE,
                        pair.functionId(),
                        sensitivityVariableType,
                        pair.variableId(),
                        sensitivityVariableSet,
                        ContingencyContext.none()));
    }

    protected void runSensitivityAnalysis(Network network, SensitivityFactorReader factorReader, SensitivityResultWriter valueWriter, List<SensitivityVariableSet> sensitivityVariableSets) {
        runner.run(network,
                network.getVariantManager().getWorkingVariantId(),
                factorReader,
                valueWriter,
                CONTINGENCIES,
                sensitivityVariableSets,
                sensitivityAnalysisParameters,
                LocalComputationManager.getDefault(),
                ReportNode.NO_OP
        );
    }
}
