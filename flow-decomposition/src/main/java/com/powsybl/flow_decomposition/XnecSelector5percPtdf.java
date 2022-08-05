/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.flow_decomposition;

import com.powsybl.contingency.Contingency;
import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;

import java.util.*;

/**
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 */
class XnecSelector5percPtdf implements XnecSelector {
    public static final double MAX_ZONE_TO_ZONE_PTDF_THRESHOLD = 0.05;
    private final Map<String, Map<Country, Double>> zonalPtdf;

    public XnecSelector5percPtdf(Map<String, Map<Country, Double>> zonalPtdf) {
        this.zonalPtdf = zonalPtdf;
    }

    public List<XnecWithDecomposition> run(Network network, Map<String, Contingency> variantContingenciesMap) {
        List<Branch> branchList = XnecSelector.getBranches(network, this::isAXnec);
        return XnecSelector.getXnecList(network, branchList, variantContingenciesMap);
    }

    private boolean isAXnec(Branch branch) {
        return XnecSelector.isAnInterconnection(branch) || hasMoreThan5PercentPtdf(getZonalPtdf(branch));
    }

    private Collection<Double> getZonalPtdf(Branch branch) {
        return zonalPtdf.getOrDefault(branch.getId(), Collections.emptyMap()).values();
    }

    private static boolean hasMoreThan5PercentPtdf(Collection<Double> countryPtdfList) {
        return (!countryPtdfList.isEmpty())
            && (Collections.max(countryPtdfList) - Collections.min(countryPtdfList)) > MAX_ZONE_TO_ZONE_PTDF_THRESHOLD;
    }
}
