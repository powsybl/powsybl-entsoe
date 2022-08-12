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
import com.powsybl.sensitivity.SensitivityVariableType;

import java.util.*;

/**
 * This class will select branches if they have any zone to zone PTDF greater thon the 5% or if they are an interconnection.
 *
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 */
class BranchSelector5PercPtdf implements BranchSelector {
    public static final double MAX_ZONE_TO_ZONE_PTDF_THRESHOLD = 0.05;
    private final Map<String, Map<Country, Double>> zonalPtdf;

    public BranchSelector5PercPtdf(Map<String, Map<Country, Double>> zonalPtdf) {
        this.zonalPtdf = zonalPtdf;
    }

    public List<Branch> run(Network network) {
        return BranchSelector.getBranches(network, this::isAXnec);
    }

    private boolean isAXnec(Branch branch) {
        return BranchSelector.isAnInterconnection(branch) || hasMoreThan5PercentPtdf(getZonalPtdf(branch));
    }

    private Collection<Double> getZonalPtdf(Branch branch) {
        return zonalPtdf.getOrDefault(branch.getId(), Collections.emptyMap()).values();
    }

    private static boolean hasMoreThan5PercentPtdf(Collection<Double> countryPtdfList) {
        return (!countryPtdfList.isEmpty())
            && (Collections.max(countryPtdfList) - Collections.min(countryPtdfList)) >= MAX_ZONE_TO_ZONE_PTDF_THRESHOLD;
    }

    static Map<String, Map<Country, Double>> getZonalPtdf(Network network,
                                                          Map<Country, Map<String, Double>> glsks,
                                                          LoadFlowParameters loadFlowParameters) {
        ZonalSensitivityAnalyser zonalSensitivityAnalyser = new ZonalSensitivityAnalyser(loadFlowParameters);
        return zonalSensitivityAnalyser.run(network,
            glsks, SensitivityVariableType.INJECTION_ACTIVE_POWER);
    }

}
