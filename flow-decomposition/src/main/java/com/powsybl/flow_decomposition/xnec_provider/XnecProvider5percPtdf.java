/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.flow_decomposition.xnec_provider;

import com.powsybl.contingency.Contingency;
import com.powsybl.flow_decomposition.GlskComputer;
import com.powsybl.flow_decomposition.NetworkUtil;
import com.powsybl.flow_decomposition.XnecProvider;
import com.powsybl.flow_decomposition.ZonalSensitivityAnalyser;
import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.sensitivity.SensitivityAnalysis;
import com.powsybl.sensitivity.SensitivityVariableType;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * This class will select branches if they have any zone to zone PTDF greater thon the 5% or if they are an interconnection.
 *
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 */
public class XnecProvider5percPtdf implements XnecProvider {
    public static final double MAX_ZONE_TO_ZONE_PTDF_THRESHOLD = 0.05;

    private boolean isAXnec(Branch branch, Map<String, Map<Country, Double>> zonalPtdf) {
        return XnecProviderInterconnection.isAnInterconnection(branch) || hasMoreThan5PercentPtdf(getZonalPtdf(branch, zonalPtdf));
    }

    private Collection<Double> getZonalPtdf(Branch branch, Map<String, Map<Country, Double>> zonalPtdf) {
        return zonalPtdf.getOrDefault(branch.getId(), Collections.emptyMap()).values();
    }

    private static boolean hasMoreThan5PercentPtdf(Collection<Double> countryPtdfList) {
        return (!countryPtdfList.isEmpty())
            && (Collections.max(countryPtdfList) - Collections.min(countryPtdfList)) >= MAX_ZONE_TO_ZONE_PTDF_THRESHOLD;
    }

    @Override
    public List<Branch> getNetworkElements(Network network) {
        GlskComputer glskComputer = new GlskComputer();
        Map<Country, Map<String, Double>> glsks = glskComputer.run(network);
        ZonalSensitivityAnalyser zonalSensitivityAnalyser = new ZonalSensitivityAnalyser(LoadFlowParameters.load(), SensitivityAnalysis.find());
        Map<String, Map<Country, Double>> zonalPtdf = zonalSensitivityAnalyser.run(network, glsks, SensitivityVariableType.INJECTION_ACTIVE_POWER);
        return NetworkUtil.getAllValidBranches(network)
            .stream()
            .filter(branch -> isAXnec(branch, zonalPtdf))
            .collect(Collectors.toList());
    }

    @Override
    public List<Branch> getNetworkElements(@NonNull String contingencyId, Network network) {
        return Collections.emptyList();
    }

    @Override
    public Map<String, List<Branch>> getNetworkElementsPerContingency(Network network) {
        return Collections.emptyMap();
    }

    @Override
    public List<Contingency> getContingencies(Network network) {
        return Collections.emptyList();
    }
}
