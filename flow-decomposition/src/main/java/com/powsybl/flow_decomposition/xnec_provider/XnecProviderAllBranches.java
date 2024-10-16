/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.flow_decomposition.xnec_provider;

import com.powsybl.contingency.Contingency;
import com.powsybl.flow_decomposition.XnecProvider;
import com.powsybl.iidm.network.Identifiable;
import com.powsybl.iidm.network.Network;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 */
public class XnecProviderAllBranches implements XnecProvider {
    @Override
    public Set<Identifiable<?>> getNetworkElements(Network network) {
        return network.getBranchStream()
                .map(e -> (Identifiable<?>) e)
                .collect(Collectors.toSet());
    }

    @Override
    public Set<Identifiable<?>> getNetworkElements(String contingencyId, Network network) {
        return Collections.emptySet();
    }

    @Override
    public Map<String, Set<Identifiable<?>>> getNetworkElementsPerContingency(Network network) {
        return Collections.emptyMap();
    }

    @Override
    public List<Contingency> getContingencies(Network network) {
        return Collections.emptyList();
    }
}
