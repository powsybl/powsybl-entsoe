/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.flow_decomposition;

import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.sensitivity.*;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * This class performs a Sensitivity Analysis to get zonal PTDFs.
 *
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 */
public class ZonalSensitivityAnalyser extends AbstractSensitivityAnalyser {
    private static final boolean SENSITIVITY_VARIABLE_SET = true;

    public ZonalSensitivityAnalyser(LoadFlowParameters loadFlowParameters, SensitivityAnalysis.Runner runner) {
        super(loadFlowParameters, runner);
    }

    public Map<String, Map<Country, Double>> run(Network network,
                                                 Map<Country, Map<String, Double>> glsks,
                                                 SensitivityVariableType sensitivityVariableType) {
        List<Branch<?>> functionList = NetworkUtil.getAllValidBranches(network);
        List<String> variableList = getVariableList(glsks);
        List<SensitivityVariableSet> sensitivityVariableSets = getSensitivityVariableSets(glsks);
        List<FunctionVariableFactor> factors = getFunctionVariableFactors(variableList, functionList);
        return getSensitivityAnalysisResult(network,
            factors, sensitivityVariableSets, sensitivityVariableType);
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
            getWeightedSensitivityVariables(countryMapEntry.getValue()));
    }

    private List<WeightedSensitivityVariable> getWeightedSensitivityVariables(Map<String, Double> singleCountryGlsks) {
        return singleCountryGlsks.entrySet().stream().map(this::getWeightedSensitivityVariable).collect(Collectors.toList());
    }

    private WeightedSensitivityVariable getWeightedSensitivityVariable(Map.Entry<String, Double> stringDoubleEntry) {
        return new WeightedSensitivityVariable(stringDoubleEntry.getKey(), stringDoubleEntry.getValue());
    }

    private Map<String, Map<Country, Double>> getSensitivityAnalysisResult(Network network, List<FunctionVariableFactor> factors, List<SensitivityVariableSet> sensitivityVariableSets, SensitivityVariableType sensitivityVariableType) {
        Map<String, Map<Country, Double>> zonalPtdf = new HashMap<>();
        SensitivityFactorReader factorReader = getSensitivityFactorReader(factors, sensitivityVariableType, SENSITIVITY_VARIABLE_SET);
        SensitivityResultWriter valueWriter = getSensitivityResultWriter(factors, zonalPtdf);
        runSensitivityAnalysis(network, factorReader, valueWriter, sensitivityVariableSets);
        return zonalPtdf;
    }

    private static SensitivityResultWriter getSensitivityResultWriter(List<FunctionVariableFactor> factors, Map<String, Map<Country, Double>> zonalPtdf) {
        return new SensitivityResultWriter() {
            @Override
            public void writeSensitivityValue(int factorIndex, int contingencyIndex, double value, double functionReference) {
                FunctionVariableFactor factor = factors.get(factorIndex);
                String branchId = factor.functionId();
                zonalPtdf.putIfAbsent(branchId, new EnumMap<>(Country.class));
                Country country = Country.valueOf(factor.variableId());
                zonalPtdf.get(branchId).put(country, value);
            }

            @Override
            public void writeContingencyStatus(int contingencyIndex, SensitivityAnalysisResult.Status status) {
                // We do not manage contingency yet
            }
        };
    }
}
