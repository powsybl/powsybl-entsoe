package com.powsybl.flow_decomposition.xnec_provider;

import com.powsybl.commons.PowsyblException;
import com.powsybl.contingency.Contingency;
import com.powsybl.flow_decomposition.XnecProvider;
import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Network;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class XnecProviderUnion implements XnecProvider {
    private final List<XnecProvider> xnecProviders;
    private Cache cache = null;

    public XnecProviderUnion(List<XnecProvider> xnecProviders) {
        this.xnecProviders = xnecProviders;
    }

    private class Cache {
        private final Set<String> validContingencyIds = new HashSet<>();
        private final Map<String, Set<Branch>> contingencyIdToXnec = new HashMap<>();
        private final Set<Contingency> contingencies = new HashSet<>();

        public Cache(Network network) {
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

        private Set<Branch> getBranchSetForAllProviders(String contingencyId, Network network) {
            return xnecProviders.stream()
                .flatMap(xnecProvider -> xnecProvider.getNetworkElements(contingencyId, network).stream())
                .collect(Collectors.toSet());
        }

        public Map<String, Set<Branch>> getContingencyIdToXnec() {
            return contingencyIdToXnec;
        }

        public List<Contingency> getContingencies() {
            return new ArrayList<>(contingencies);
        }
    }

    private void initCache(Network network) {
        cache = cache == null ? new Cache(network) : cache;
    }

    @Override
    public Set<Branch> getNetworkElements(Network network) {
        return xnecProviders.stream()
            .flatMap(xnecProvider -> xnecProvider.getNetworkElements(network).stream())
            .collect(Collectors.toSet());
    }

    @Override
    public Set<Branch> getNetworkElements(@NonNull String contingencyId, Network network) {
        return getNetworkElementsPerContingency(network).get(contingencyId);
    }

    @Override
    public Map<String, Set<Branch>> getNetworkElementsPerContingency(Network network) {
        initCache(network);
        return cache.getContingencyIdToXnec();
    }

    @Override
    public List<Contingency> getContingencies(Network network) {
        initCache(network);
        return cache.getContingencies();
    }
}
