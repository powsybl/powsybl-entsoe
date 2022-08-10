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

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * This class will select branches based on the 5% zonal PTDF criteria.
 *
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 */
class XnecSelector5percPtdf implements XnecSelector {
    public static final double MAX_ZONE_TO_ZONE_PTDF_THRESHOLD = 0.05;
    private final Map<String, Map<Country, Double>> zonalPtdf;

    public XnecSelector5percPtdf(Map<String, Map<Country, Double>> zonalPtdf) {
        this.zonalPtdf = zonalPtdf;
    }

    public List<Branch> run(Network network) {
        return NetworkUtil.getAllValidBranches(network)
            .stream()
            .filter(this::isAXnec)
            .collect(Collectors.toList());
    }

    private boolean isAXnec(Branch branch) {
        return XnecSelectorInterconnection.isAnInterconnection(branch) || hasMoreThan5PercentPtdf(getZonalPtdf(branch));
    }

    private Collection<Double> getZonalPtdf(Branch branch) {
        return zonalPtdf.getOrDefault(branch.getId(), Collections.emptyMap()).values();
    }

    private static boolean hasMoreThan5PercentPtdf(Collection<Double> countryPtdfList) {
        return (!countryPtdfList.isEmpty())
            && (Collections.max(countryPtdfList) - Collections.min(countryPtdfList)) >= MAX_ZONE_TO_ZONE_PTDF_THRESHOLD;
    }
}
