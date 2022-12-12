package com.powsybl.flow_decomposition.xnec_provider;

import com.powsybl.commons.PowsyblException;
import com.powsybl.contingency.Contingency;
import com.powsybl.contingency.ContingencyElement;
import com.powsybl.flow_decomposition.XnecProvider;
import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Network;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class XnecProviderUnion implements XnecProvider {
    Set<String> validContingenciesCache = new HashSet<>();
    private final List<XnecProvider> xnecProviders;

    public XnecProviderUnion(List<XnecProvider> xnecProviders) {
        this.xnecProviders = xnecProviders;
    }

    private void assertContingencyDefinitionIsUniqueAcrossXnecProviders(String contingencyId, Network network) {
        if (xnecProviders.size() < 2 || validContingenciesCache.contains(contingencyId)) {
            return;
        }
        boolean hasSameDefinitionForContingencies = xnecProviders.stream()
            .mapToInt(getHashOfContingencyElementsFromSelectedContingency(contingencyId, network))
            .distinct().count() == 1;
        if (!hasSameDefinitionForContingencies) {
            throw new PowsyblException(String.format("Contingency '%s' definition is not unique across different providers", contingencyId));
        }
        validContingenciesCache.add(contingencyId);
    }

    private static ToIntFunction<XnecProvider> getHashOfContingencyElementsFromSelectedContingency(String contingencyId, Network network) {
        return xnecProvider -> getContingencyElementsFromSelectedContingency(contingencyId, network, xnecProvider)
            .map(ContingencyElement::getId)
            .collect(Collectors.toList())
            .hashCode();
    }

    private static Stream<ContingencyElement> getContingencyElementsFromSelectedContingency(String contingencyId, Network network, XnecProvider xnecProvider) {
        return getSelectedContingency(contingencyId, network, xnecProvider)
            .map(Contingency::getElements)
            .flatMap(Collection::stream);
    }

    private static Stream<Contingency> getSelectedContingency(String contingencyId, Network network, XnecProvider xnecProvider) {
        return xnecProvider
            .getContingencies(network)
            .stream()
            .filter(contingency -> Objects.equals(contingency.getId(), contingencyId));
    }

    @Override
    public Set<Branch> getNetworkElements(Network network) {
        return xnecProviders.stream()
            .flatMap(xnecProvider -> xnecProvider.getNetworkElements(network).stream())
            .collect(Collectors.toSet());
    }

    @Override
    public Set<Branch> getNetworkElements(@NonNull String contingencyId, Network network) {
        assertContingencyDefinitionIsUniqueAcrossXnecProviders(contingencyId, network);
        return xnecProviders.stream()
            .flatMap(xnecProvider -> xnecProvider.getNetworkElements(contingencyId, network).stream())
            .collect(Collectors.toSet());
    }

    @Override
    public Map<String, Set<Branch>> getNetworkElementsPerContingency(Network network) {
        return null;
    }

    @Override
    public List<Contingency> getContingencies(Network network) {
        Set<Contingency> contingencies = new HashSet<>();
        xnecProviders.forEach(xnecProvider -> {
            xnecProvider.getContingencies(network).forEach(
                contingency -> {
                    assertContingencyDefinitionIsUniqueAcrossXnecProviders(contingency.getId(), network);
                    contingencies.add(contingency);
                });
        });
        return new ArrayList<>(contingencies);
    }
}
