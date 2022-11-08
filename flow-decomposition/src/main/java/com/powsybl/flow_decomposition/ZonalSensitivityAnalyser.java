/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.flow_decomposition;

import com.powsybl.commons.reporter.Reporter;
import com.powsybl.computation.local.LocalComputationManager;
import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Identifiable;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.sensitivity.*;

import java.util.EnumMap;
import java.util.HashMap;
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

    public ZonalSensitivityAnalyser(LoadFlowParameters loadFlowParameters, SensitivityAnalysis.Runner runner) {
        super(loadFlowParameters, runner);
    }

    public Map<String, Map<Country, Double>> run(Network network,
                                                 Map<Country, Map<String, Double>> glsks,
                                                 SensitivityVariableType sensitivityVariableType) {
        List<Branch> functionList = NetworkUtil.getAllValidBranches(network);
        List<String> variableList = getVariableList(glsks);
        List<SensitivityVariableSet> sensitivityVariableSets = getSensitivityVariableSets(glsks);
        List<SensitivityFactor> factors = getFactors(variableList, functionList,
            sensitivityVariableType, SENSITIVITY_VARIABLE_SET);
        return getSensitivityAnalysisResult(network,
            factors, sensitivityVariableSets);
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

    private Map<String, Map<Country, Double>> getSensitivityAnalysisResult(Network network, List<SensitivityFactor> factors, List<SensitivityVariableSet> sensitivityVariableSets) {
        Map<String, Map<Country, Double>> zonalPtdf = new HashMap<>();
        SensitivityFactorReader factorReader = handler -> factors.forEach(sensitivityFactor -> handler.onFactor(sensitivityFactor.getFunctionType(),
            sensitivityFactor.getFunctionId(),
            sensitivityFactor.getVariableType(),
            sensitivityFactor.getVariableId(),
            sensitivityFactor.isVariableSet(),
            sensitivityFactor.getContingencyContext()));
        SensitivityResultWriter valueWriter = new SensitivityResultWriter() {
            @Override
            public void writeSensitivityValue(int factorIndex, int contingencyIndex, double value, double functionReference) {
                SensitivityFactor factor = factors.get(factorIndex);
                String branchId = factor.getFunctionId();
                zonalPtdf.putIfAbsent(branchId, new EnumMap<>(Country.class));
                Country country = Country.valueOf(factor.getVariableId());
                zonalPtdf.get(branchId).put(country, value);
            }

            @Override
            public void writeContingencyStatus(int contingencyIndex, SensitivityAnalysisResult.Status status) {
                // We do not manage contingency yet
            }
        };
        runner.run(network,
            network.getVariantManager().getWorkingVariantId(),
            factorReader,
            valueWriter,
            CONTINGENCIES,
            sensitivityVariableSets,
            sensitivityAnalysisParameters,
            LocalComputationManager.getDefault(),
            Reporter.NO_OP
        );
        return zonalPtdf;
    }
}
