/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.flow_decomposition.xnec_provider;

import com.powsybl.commons.PowsyblException;
import com.powsybl.contingency.Contingency;
import com.powsybl.flow_decomposition.XnecProvider;
import com.powsybl.iidm.network.Identifiable;
import com.powsybl.iidm.network.Network;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 */
public class XnecProviderUnion implements XnecProvider {
    private final List<XnecProvider> xnecProviders;
    private ContingencyConsistencyCache contingencyConsistencyCache = null;

    public XnecProviderUnion(List<XnecProvider> xnecProviders) {
        this.xnecProviders = xnecProviders;
    }

    private class ContingencyConsistencyCache {
        private final Set<String> validContingencyIds = new HashSet<>();
        private final Map<String, Set<Identifiable<?>>> contingencyIdToXnec = new HashMap<>();
        private final Set<Contingency> contingencies = new HashSet<>();
        private final int networkHash;

        public ContingencyConsistencyCache(Network network) {
            networkHash = network.hashCode();
            xnecProviders.forEach(xnecProvider -> xnecProvider.getContingencies(network).forEach(
                contingency -> {
                    String contingencyId = contingency.getId();
                    if (!validContingencyIds.contains(contingencyId)) {
                        assertContingencyDefinitionIsUniqueAcrossXnecProviders(contingencyId, network);
                        if (contingencies.add(contingency)) {
                            contingencyIdToXnec.put(contingencyId, getBranchSetForAllProviders(contingencyId, network));
                        }
                        validContingencyIds.add(contingencyId);
                    }
                }));
        }

        public boolean isValid(Network network) {
            return network.hashCode() == networkHash;
        }

        private void assertContingencyDefinitionIsUniqueAcrossXnecProviders(String contingencyId, Network network) {
            boolean hasUniqueDefinitionForSelectedContingency = xnecProviders.stream()
                .mapToInt(xnecProvider -> getSelectedContingencyHashCode(contingencyId, network, xnecProvider))
                .distinct()
                .filter(i -> i != Collections.emptyList().hashCode())
                .count() == 1;
            if (!hasUniqueDefinitionForSelectedContingency) {
                throw new PowsyblException(String.format("Contingency '%s' definition is not unique across different providers", contingencyId));
            }
        }

        private int getSelectedContingencyHashCode(String contingencyId, Network network, XnecProvider xnecProvider) {
            return xnecProvider
                .getContingencies(network)
                .stream()
                .filter(contingency -> Objects.equals(contingency.getId(), contingencyId))
                .collect(Collectors.toList())
                .hashCode();
        }

        private Set<Identifiable<?>> getBranchSetForAllProviders(String contingencyId, Network network) {
            return xnecProviders.stream()
                .flatMap(xnecProvider -> xnecProvider.getNetworkElements(contingencyId, network).stream())
                .collect(Collectors.toSet());
        }

        public Map<String, Set<Identifiable<?>>> getContingencyIdToXnec() {
            return contingencyIdToXnec;
        }

        public List<Contingency> getContingencies() {
            return new ArrayList<>(contingencies);
        }
    }

    private void initizializeContingencyConsistencyCache(Network network) {
        if (contingencyConsistencyCache == null || !contingencyConsistencyCache.isValid(network)) {
            contingencyConsistencyCache = new ContingencyConsistencyCache(network);
        }
    }

    @Override
    public Set<Identifiable<?>> getNetworkElements(Network network) {
        return xnecProviders.stream()
            .flatMap(xnecProvider -> xnecProvider.getNetworkElements(network).stream())
            .collect(Collectors.toSet());
    }

    @Override
    public Set<Identifiable<?>> getNetworkElements(String contingencyId, Network network) {
        return getNetworkElementsPerContingency(network).get(contingencyId);
    }

    @Override
    public Map<String, Set<Identifiable<?>>> getNetworkElementsPerContingency(Network network) {
        initizializeContingencyConsistencyCache(network);
        return contingencyConsistencyCache.getContingencyIdToXnec();
    }

    @Override
    public List<Contingency> getContingencies(Network network) {
        initizializeContingencyConsistencyCache(network);
        return contingencyConsistencyCache.getContingencies();
    }
}
