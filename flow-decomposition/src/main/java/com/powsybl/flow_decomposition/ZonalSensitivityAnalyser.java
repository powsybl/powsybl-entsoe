/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.flow_decomposition;

import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.sensitivity.*;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * This class performs a Sensitivity Analysis to get zonal PTDFs.
 *
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 */
class ZonalSensitivityAnalyser extends AbstractSensitivityAnalyser {
    private static final boolean SENSITIVITY_VARIABLE_SET = true;

    public ZonalSensitivityAnalyser(LoadFlowParameters loadFlowParameters) {
        super(loadFlowParameters);
    }

    public Map<String, Map<Country, Double>> run(Network network,
                                                 Map<Country, Map<String, Double>> glsks,
                                                 SensitivityVariableType sensitivityVariableType) {
        XnecFactory xnecFactory = new XnecFactory();
        List<XnecWithDecomposition> functionList = xnecFactory.run(network, NetworkUtil.getAllValidBranches(network));
        List<String> variableList = getVariableList(glsks);
        List<SensitivityVariableSet> sensitivityVariableSets = getSensitivityVariableSets(glsks);
        List<SensitivityFactor> factors = getFactors(variableList, functionList,
            sensitivityVariableType, SENSITIVITY_VARIABLE_SET);
        SensitivityAnalysisResult sensitivityResult = getSensitivityAnalysisResult(network,
            factors, sensitivityVariableSets);
        return getZonalPtdfMap(variableList, functionList, sensitivityResult);
    }

    private List<String> getVariableList(Map<Country, Map<String, Double>> glsks) {
        return glsks.keySet().stream().map(Country::toString).collect(Collectors.toList());
    }

    private List<SensitivityVariableSet> getSensitivityVariableSets(Map<Country, Map<String, Double>> glsks) {
        return glsks.entrySet().stream().map(
            this::getSensitivityVariableSet).collect(Collectors.toList());
    }

    private SensitivityVariableSet getSensitivityVariableSet(Map.Entry<Country, Map<String, Double>> countryMapEntry) {
        return new SensitivityVariableSet(countryMapEntry.getKey().toString(),
            getWeighteitedSensitivityVariables(countryMapEntry.getValue()));
    }

    private List<WeightedSensitivityVariable> getWeighteitedSensitivityVariables(Map<String, Double> singleCountryGlsks) {
        return singleCountryGlsks.entrySet().stream().map(this::getWeightedSensitivityVariable).collect(Collectors.toList());
    }

    private WeightedSensitivityVariable getWeightedSensitivityVariable(Map.Entry<String, Double> stringDoubleEntry) {
        return new WeightedSensitivityVariable(stringDoubleEntry.getKey(), stringDoubleEntry.getValue());
    }

    private SensitivityAnalysisResult getSensitivityAnalysisResult(Network network, List<SensitivityFactor> factors, List<SensitivityVariableSet> sensitivityVariableSets) {
        return SensitivityAnalysis.run(network, factors, CONTINGENCIES, sensitivityVariableSets, sensitivityAnalysisParameters);
    }

    private Map<String, Map<Country, Double>> getZonalPtdfMap(List<String> variableList,
                                                              List<XnecWithDecomposition> functionList,
                                                              SensitivityAnalysisResult sensitivityResult) {
        return functionList.stream().map(xnec -> xnec.getBranch().getId()).collect(Collectors.toMap(
            Function.identity(),
            branch -> variableList.stream().collect(Collectors.toMap(
                Country::valueOf,
                variable -> getPtdfValue(branch, variable, sensitivityResult)))));
    }

    private double getPtdfValue(String branch, String variable, SensitivityAnalysisResult sensitivityResult) {
        return sensitivityResult.getSensitivityValue(variable, branch, SENSITIVITY_FUNCTION_TYPE);
    }
}
