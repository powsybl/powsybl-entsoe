/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.flow_decomposition.xnec_provider;

import com.powsybl.contingency.Contingency;
import com.powsybl.flow_decomposition.NetworkUtil;
import com.powsybl.flow_decomposition.XnecProvider;
import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 */
public class XnecProviderInterconnection implements XnecProvider {
    static boolean isAnInterconnection(Branch<?> branch) {
        Country country1 = NetworkUtil.getTerminalCountry(branch.getTerminal1());
        Country country2 = NetworkUtil.getTerminalCountry(branch.getTerminal2());
        return !country1.equals(country2);
    }

    @Override
    public Set<Branch<?>> getNetworkElements(Network network) {
        return NetworkUtil.getAllValidBranches(network)
            .stream()
            .filter(XnecProviderInterconnection::isAnInterconnection)
            .collect(Collectors.toSet());
    }

    @Override
    public Set<Branch<?>> getNetworkElements(String contingencyId, Network network) {
        return Collections.emptySet();
    }

    @Override
    public Map<String, Set<Branch<?>>> getNetworkElementsPerContingency(Network network) {
        return Collections.emptyMap();
    }

    @Override
    public List<Contingency> getContingencies(Network network) {
        return Collections.emptyList();
    }
}
