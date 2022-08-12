/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.flow_decomposition;

import com.powsybl.contingency.Contingency;
import com.powsybl.contingency.ContingencyContext;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.sensitivity.SensitivityAnalysisParameters;
import com.powsybl.sensitivity.SensitivityFactor;
import com.powsybl.sensitivity.SensitivityFunctionType;
import com.powsybl.sensitivity.SensitivityVariableType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 */
abstract class AbstractSensitivityAnalyser {
    public static final SensitivityFunctionType SENSITIVITY_FUNCTION_TYPE = SensitivityFunctionType.BRANCH_ACTIVE_POWER_1;
    public static final List<Contingency> CONTINGENCIES = Collections.emptyList();
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractSensitivityAnalyser.class);
    protected final SensitivityAnalysisParameters sensitivityAnalysisParameters;

    AbstractSensitivityAnalyser(LoadFlowParameters loadFlowParameters) {
        this.sensitivityAnalysisParameters = initSensitivityAnalysisParameters(loadFlowParameters);
    }

    protected static SensitivityAnalysisParameters initSensitivityAnalysisParameters(LoadFlowParameters loadFlowParameters) {
        SensitivityAnalysisParameters parameters = SensitivityAnalysisParameters.load();
        parameters.setLoadFlowParameters(loadFlowParameters);
        LOGGER.debug("Using following sensitivity analysis parameters: {}", parameters);
        return parameters;
    }

    protected List<SensitivityFactor> getFactors(List<String> variableList,
                                                 List<XnecWithDecomposition> functionList,
                                                 SensitivityVariableType sensitivityVariableType,
                                                 boolean sensitivityVariableSet) {
        List<SensitivityFactor> factors = new ArrayList<>();
        variableList.forEach(
            variable -> functionList.forEach(
                function -> factors.add(getSensitivityFactor(function, variable,
                    sensitivityVariableType, sensitivityVariableSet))));
        return factors;
    }

    protected SensitivityFactor getSensitivityFactor(Xnec function,
                                                     String variable,
                                                     SensitivityVariableType sensitivityVariableType,
                                                     boolean sensitivityVariableSet) {
        return new SensitivityFactor(
            SensitivityFunctionType.BRANCH_ACTIVE_POWER_1, function.getBranch().getId(),
            sensitivityVariableType, variable,
            sensitivityVariableSet,
            ContingencyContext.none()
        );
    }
}