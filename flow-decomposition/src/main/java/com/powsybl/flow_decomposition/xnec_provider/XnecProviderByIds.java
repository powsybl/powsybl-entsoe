/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.flow_decomposition.xnec_provider;

import com.powsybl.commons.PowsyblException;
import com.powsybl.contingency.BranchContingency;
import com.powsybl.contingency.Contingency;
import com.powsybl.contingency.ContingencyBuilder;
import com.powsybl.flow_decomposition.XnecProvider;
import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Network;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
public final class XnecProviderByIds implements XnecProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(XnecProviderByIds.class);
    private final Map<String, Contingency> contingencyIdToContingencyMap;
    private final Map<Contingency, Set<String>> contingencyToXnecMap;
    private final Set<String> bestCaseBranches;

    private XnecProviderByIds(Map<String, Contingency> contingencyIdToContingencyMap, Map<Contingency, Set<String>> contingencyToXnecMap, Set<String> bestCaseBranches) {
        this.contingencyIdToContingencyMap = contingencyIdToContingencyMap;
        this.contingencyToXnecMap = contingencyToXnecMap;
        this.bestCaseBranches = bestCaseBranches;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final Map<String, Contingency> contingencyIdToContingencyMap = new HashMap<>();
        private final Map<Contingency, Set<String>> contingencyToXnecMap = new HashMap<>();
        private final Set<String> bestCaseBranches = new HashSet<>();

        public Builder addContingencies(Map<String, Set<String>> contingencies) {
            contingencies.forEach(this::addContingency);
            return this;
        }

        public Builder addContingency(String contingencyId, Set<String> contingencyElementIdSet) {
            Contingency contingency = createContingency(contingencyId, contingencyElementIdSet);
            contingencyIdToContingencyMap.put(contingencyId, contingency);
            contingencyToXnecMap.put(contingency, new HashSet<>());
            return this;
        }

        private Contingency createContingency(String contingencyId, Set<String> contingencyElementIdSet) {
            ContingencyBuilder contingencyBuilder = Contingency.builder(contingencyId);
            contingencyElementIdSet.forEach(contingencyBuilder::addBranch);
            return contingencyBuilder.build();
        }

        public Builder addNetworkElementsAfterContingencies(Set<String> branchIds, Set<String> contingencyIds) {
            contingencyIds.forEach(contingencyId -> {
                if (contingencyIdToContingencyMap.containsKey(contingencyId)) {
                    Contingency contingency = contingencyIdToContingencyMap.get(contingencyId);
                    branchIds.forEach(branchId -> {
                        if (!contingency.getElements().contains(new BranchContingency(branchId))) {
                            contingencyToXnecMap.get(contingency).add(branchId);
                        }
                    });
                } else {
                    throw new PowsyblException(String.format("Contingency Id '%s' have not been defined. See addContingency and/or addContingencies", contingencyId));
                }
            });
            return this;
        }

        public Builder addNetworkElementsOnBasecase(Set<String> branchIds) {
            bestCaseBranches.addAll(branchIds);
            return this;
        }

        public XnecProviderByIds build() {
            return new XnecProviderByIds(contingencyIdToContingencyMap, contingencyToXnecMap, bestCaseBranches);
        }
    }

    private List<Branch> mapBranchSetToList(Set<String> branchSet, Network network) {
        return branchSet.stream()
            .map(xnecId -> {
                Branch branch = network.getBranch(xnecId);
                if (Objects.isNull(branch)) {
                    LOGGER.warn("Branch {} without contingency was not found in network {}", xnecId, network.getId());
                }
                return branch;
            })
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    @Override
    public List<Branch> getNetworkElements(Network network) {
        return mapBranchSetToList(bestCaseBranches, network);
    }

    @Override
    public List<Branch> getNetworkElements(@NonNull String contingencyId, Network network) {
        if (!contingencyIdToContingencyMap.containsKey(contingencyId)) {
            return Collections.emptyList();
        }
        return mapBranchSetToList(contingencyToXnecMap.get(contingencyIdToContingencyMap.get(contingencyId)), network);
    }

    @Override
    public Map<String, List<Branch>> getNetworkElementsPerContingency(Network network) {
        Map<String, List<Branch>> contingencyIdToXnec = new HashMap<>(); //TODO Cache ?
        contingencyIdToContingencyMap.forEach((contingencyId, contingency) -> contingencyIdToXnec.put(contingencyId, mapBranchSetToList(contingencyToXnecMap.get(contingency), network)));
        return contingencyIdToXnec;
    }

    @Override
    public List<Contingency> getContingencies(Network network) {
        return contingencyToXnecMap.keySet().stream()
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }
}
