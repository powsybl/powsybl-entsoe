/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.flow_decomposition.xnec_provider;

import com.powsybl.commons.PowsyblException;
import com.powsybl.contingency.Contingency;
import com.powsybl.flow_decomposition.TestUtils;
import com.powsybl.flow_decomposition.XnecProvider;
import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Network;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 */
class XnecProviderByIdsTests {
    @Test
    void testXnecProvider() {
        String networkFileName = "NETWORK_SINGLE_LOAD_TWO_GENERATORS_WITH_COUNTRIES.uct";
        Network network = TestUtils.importNetwork(networkFileName);
        String xnecFrBe = "FGEN1 11 BLOAD 11 1";
        Set<String> xnecSet = Set.of(xnecFrBe);
        XnecProvider xnecProvider = XnecProviderByIds.builder()
            .addNetworkElementsOnBasecase(xnecSet)
            .build();
        Set<Branch<?>> branchSet = xnecProvider.getNetworkElements(network);
        assertTrue(branchSet.contains(network.getBranch(xnecFrBe)));
        assertEquals(1, branchSet.size());

        Map<String, Set<Branch<?>>> networkElementsPerContingency = xnecProvider.getNetworkElementsPerContingency(network);
        assertEquals(0, networkElementsPerContingency.size());
    }

    @Test
    void testXnecProviderNonExistingId() {
        String networkFileName = "NETWORK_SINGLE_LOAD_TWO_GENERATORS_WITH_COUNTRIES.uct";
        Network network = TestUtils.importNetwork(networkFileName);
        String xnecFrBe = "FGEN1 11 BLOAD 11 1";
        Set<String> xnecSet = Set.of(xnecFrBe, "NON EXISTING ID");
        XnecProvider xnecProvider = XnecProviderByIds.builder()
            .addNetworkElementsOnBasecase(xnecSet)
            .build();
        Set<Branch<?>> branchSet = xnecProvider.getNetworkElements(network);
        assertTrue(branchSet.contains(network.getBranch(xnecFrBe)));
        assertEquals(1, branchSet.size());
    }

    @Test
    void testXnecProviderMultipleIds() {
        String networkFileName = "NETWORK_SINGLE_LOAD_TWO_GENERATORS_WITH_COUNTRIES.uct";
        Network network = TestUtils.importNetwork(networkFileName);
        String xnecFrBe = "FGEN1 11 BLOAD 11 1";
        String xnecBeBe = "BLOAD 11 BGEN2 11 1";
        Set<String> xnecSet = Set.of(xnecFrBe, xnecBeBe);
        XnecProvider xnecProvider = XnecProviderByIds.builder()
            .addNetworkElementsOnBasecase(xnecSet)
            .build();
        Set<Branch<?>> branchSet = xnecProvider.getNetworkElements(network);
        assertTrue(branchSet.contains(network.getBranch(xnecFrBe)));
        assertTrue(branchSet.contains(network.getBranch(xnecBeBe)));
        assertEquals(2, branchSet.size());
    }

    @Test
    void testXnecProviderMultipleDuplicateIds() {
        String networkFileName = "NETWORK_SINGLE_LOAD_TWO_GENERATORS_WITH_COUNTRIES.uct";
        Network network = TestUtils.importNetwork(networkFileName);
        String xnecFrBe = "FGEN1 11 BLOAD 11 1";
        String xnecBeBe = "BLOAD 11 BGEN2 11 1";
        XnecProvider xnecProvider = XnecProviderByIds.builder()
            .addNetworkElementsOnBasecase(Set.of(xnecFrBe, xnecBeBe))
            .addNetworkElementsOnBasecase(Set.of(xnecFrBe))
            .build();
        Set<Branch<?>> branchSet = xnecProvider.getNetworkElements(network);
        assertTrue(branchSet.contains(network.getBranch(xnecFrBe)));
        assertTrue(branchSet.contains(network.getBranch(xnecBeBe)));
        assertEquals(2, branchSet.size());
    }

    @Test
    void testXnecProviderDisconnectedBranch() {
        String networkFileName = "NETWORK_SINGLE_LOAD_TWO_GENERATORS_WITH_COUNTRIES.uct";
        Network network = TestUtils.importNetwork(networkFileName);
        String xnecFrBe = "FGEN1 11 BLOAD 11 1";
        String xnecBeBe = "BLOAD 11 BGEN2 11 1";
        network.getBranch(xnecBeBe).getTerminal1().disconnect();
        Set<String> xnecSet = Set.of(xnecFrBe, xnecBeBe);
        XnecProvider xnecProvider = XnecProviderByIds.builder()
            .addNetworkElementsOnBasecase(xnecSet)
            .build();
        Set<Branch<?>> branchSet = xnecProvider.getNetworkElements(network);
        assertFalse(network.getBranch(xnecBeBe).getTerminal1().isConnected());
        assertTrue(branchSet.contains(network.getBranch(xnecFrBe)));
        assertTrue(branchSet.contains(network.getBranch(xnecBeBe)));
        assertEquals(2, branchSet.size());
    }

    @Test
    void testXnecProviderWithValidContingencies() {
        String networkFileName = "NETWORK_PST_FLOW_WITH_COUNTRIES_NON_NEUTRAL.uct";
        String x1 = "FGEN  11 BLOAD 11 1";
        String x2 = "FGEN  11 BLOAD 12 1";

        Network network = TestUtils.importNetwork(networkFileName);
        XnecProvider xnecProvider = XnecProviderByIds.builder()
            .addContingency(x2, Set.of(x2))
            .addNetworkElementsAfterContingencies(Set.of(x1), Set.of(x2))
            .build();

        List<Contingency> contingencies = xnecProvider.getContingencies(network);
        assertEquals(1, contingencies.size());
        assertTrue(contingencies.contains(Contingency.builder(x2).addBranch(x2).build()));

        Set<Branch<?>> xnecSet = xnecProvider.getNetworkElements(network);
        assertTrue(xnecSet.isEmpty());

        Set<Branch<?>> xnecSetX2 = xnecProvider.getNetworkElements(x2, network);
        assertEquals(1, xnecSetX2.size());
        assertTrue(xnecSetX2.contains(network.getBranch(x1)));

        Set<Branch<?>> xnecSetX1 = xnecProvider.getNetworkElements(x1, network);
        assertTrue(xnecSetX1.isEmpty());

        Map<String, Set<Branch<?>>> networkElementsPerContingency = xnecProvider.getNetworkElementsPerContingency(network);
        assertEquals(1, networkElementsPerContingency.size());
        assertTrue(networkElementsPerContingency.containsKey(x2));
        assertEquals(1, networkElementsPerContingency.get(x2).size());
        assertTrue(networkElementsPerContingency.get(x2).contains(network.getBranch(x1)));
    }

    @Test
    void testXnecProviderWithValidMultipleContingencies() {
        String networkFileName = "NETWORK_PST_FLOW_WITH_COUNTRIES_NON_NEUTRAL.uct";
        String x1 = "FGEN  11 BLOAD 11 1";
        String x2 = "FGEN  11 BLOAD 12 1";
        String g1 = "BLOAD 11_generator";

        Network network = TestUtils.importNetwork(networkFileName);
        Contingency branchContingency = Contingency.builder(x2).addBranch(x2).build();
        Contingency genContingency = Contingency.builder(g1).addGenerator(g1).build();

        XnecProvider xnecProvider = XnecProviderByIds.builder()
            .addContingency(branchContingency)
            .addContingency(genContingency)
            .addNetworkElementsAfterContingencies(Set.of(x1), Set.of(x2))
            .addNetworkElementsAfterContingencies(Set.of(x1), Set.of(g1))
            .build();

        List<Contingency> contingencies = xnecProvider.getContingencies(network);
        assertEquals(2, contingencies.size());
        assertTrue(contingencies.contains(Contingency.builder(x2).addBranch(x2).build()));
        assertTrue(contingencies.contains(Contingency.builder(g1).addGenerator(g1).build()));

        Set<Branch<?>> xnecSet = xnecProvider.getNetworkElements(network);
        assertTrue(xnecSet.isEmpty());

        Set<Branch<?>> xnecSetX2 = xnecProvider.getNetworkElements(x2, network);
        assertEquals(1, xnecSetX2.size());
        assertTrue(xnecSetX2.contains(network.getBranch(x1)));

        Set<Branch<?>> xnecSetG1 = xnecProvider.getNetworkElements(g1, network);
        assertEquals(1, xnecSetG1.size());
        assertTrue(xnecSetG1.contains(network.getBranch(x1)));

        Set<Branch<?>> xnecSetX1 = xnecProvider.getNetworkElements(x1, network);
        assertTrue(xnecSetX1.isEmpty());

        Map<String, Set<Branch<?>>> networkElementsPerContingency = xnecProvider.getNetworkElementsPerContingency(network);
        assertEquals(2, networkElementsPerContingency.size());
        assertTrue(networkElementsPerContingency.containsKey(x2));
        assertTrue(networkElementsPerContingency.containsKey(g1));
        assertEquals(1, networkElementsPerContingency.get(x2).size());
        assertEquals(1, networkElementsPerContingency.get(g1).size());
        assertTrue(networkElementsPerContingency.get(x2).contains(network.getBranch(x1)));
        assertTrue(networkElementsPerContingency.get(g1).contains(network.getBranch(x1)));
    }

    @Test
    void testXnecProviderWithValidContingenciesMixedManually() {
        String networkFileName = "NETWORK_PST_FLOW_WITH_COUNTRIES_NON_NEUTRAL.uct";
        String x1 = "FGEN  11 BLOAD 11 1";
        String x2 = "FGEN  11 BLOAD 12 1";

        Network network = TestUtils.importNetwork(networkFileName);
        XnecProvider xnecProvider = XnecProviderByIds.builder()
            .addContingency(x2, Set.of(x2))
            .addNetworkElementsAfterContingencies(Set.of(x1), Set.of(x2))
            .addNetworkElementsOnBasecase(Set.of(x1, x2))
            .build();

        List<Contingency> contingencies = xnecProvider.getContingencies(network);
        assertEquals(1, contingencies.size());
        assertTrue(contingencies.contains(Contingency.builder(x2).addBranch(x2).build()));

        Set<Branch<?>> xneSet = xnecProvider.getNetworkElements(network);
        assertEquals(2, xneSet.size());
        assertTrue(xneSet.contains(network.getBranch(x1)));
        assertTrue(xneSet.contains(network.getBranch(x2)));

        Set<Branch<?>> xnecSetX2 = xnecProvider.getNetworkElements(x2, network);
        assertEquals(1, xnecSetX2.size());
        assertTrue(xnecSetX2.contains(network.getBranch(x1)));

        Set<Branch<?>> xnecSetX1 = xnecProvider.getNetworkElements(x1, network);
        assertTrue(xnecSetX1.isEmpty());

        Map<String, Set<Branch<?>>> networkElementsPerContingency = xnecProvider.getNetworkElementsPerContingency(network);
        assertEquals(1, networkElementsPerContingency.size());
        assertTrue(networkElementsPerContingency.containsKey(x2));
        assertEquals(1, networkElementsPerContingency.get(x2).size());
        assertTrue(networkElementsPerContingency.get(x2).contains(network.getBranch(x1)));
    }

    @Test
    void testXnecProviderWithValidContingenciesMixedAutomatically() {
        String networkFileName = "NETWORK_PST_FLOW_WITH_COUNTRIES_NON_NEUTRAL.uct";
        String x1 = "FGEN  11 BLOAD 11 1";
        String x2 = "FGEN  11 BLOAD 12 1";
        String x3 = "BLOAD 11 BLOAD 12 2";

        Network network = TestUtils.importNetwork(networkFileName);
        XnecProvider xnecProvider = XnecProviderByIds.builder()
            .addContingencies(Map.of(x1, Set.of(x1), x2, Set.of(x2), x3, Set.of(x3)))
            .addNetworkElementsAfterContingencies(Set.of(x1, x2, x3), Set.of(x1, x2, x3))
            .addNetworkElementsOnBasecase(Set.of(x1, x2, x3))
            .build();

        List<Contingency> contingencies = xnecProvider.getContingencies(network);
        assertEquals(3, contingencies.size());
        assertTrue(contingencies.contains(Contingency.builder(x1).addBranch(x1).build()));
        assertTrue(contingencies.contains(Contingency.builder(x2).addBranch(x2).build()));
        assertTrue(contingencies.contains(Contingency.builder(x3).addBranch(x3).build()));

        Set<Branch<?>> xneSet = xnecProvider.getNetworkElements(network);
        assertEquals(3, xneSet.size());
        assertTrue(xneSet.contains(network.getBranch(x1)));
        assertTrue(xneSet.contains(network.getBranch(x2)));
        assertTrue(xneSet.contains(network.getBranch(x3)));

        Set<Branch<?>> xnecSetX1 = xnecProvider.getNetworkElements(x1, network);
        assertEquals(2, xnecSetX1.size());
        assertTrue(xnecSetX1.contains(network.getBranch(x2)));
        assertTrue(xnecSetX1.contains(network.getBranch(x3)));

        Set<Branch<?>> xnecSetX2 = xnecProvider.getNetworkElements(x2, network);
        assertEquals(2, xnecSetX2.size());
        assertTrue(xnecSetX2.contains(network.getBranch(x1)));
        assertTrue(xnecSetX2.contains(network.getBranch(x3)));

        Set<Branch<?>> xnecSetX3 = xnecProvider.getNetworkElements(x3, network);
        assertEquals(2, xnecSetX3.size());
        assertTrue(xnecSetX3.contains(network.getBranch(x1)));
        assertTrue(xnecSetX3.contains(network.getBranch(x2)));

        Map<String, Set<Branch<?>>> networkElementsPerContingency = xnecProvider.getNetworkElementsPerContingency(network);
        assertEquals(3, networkElementsPerContingency.size());
        assertTrue(networkElementsPerContingency.containsKey(x1));
        assertTrue(networkElementsPerContingency.containsKey(x2));
        assertTrue(networkElementsPerContingency.containsKey(x3));
        assertEquals(2, networkElementsPerContingency.get(x1).size());
        assertEquals(2, networkElementsPerContingency.get(x2).size());
        assertEquals(2, networkElementsPerContingency.get(x3).size());
        assertFalse(networkElementsPerContingency.get(x1).contains(network.getBranch(x1)));
        assertTrue(networkElementsPerContingency.get(x1).contains(network.getBranch(x2)));
        assertTrue(networkElementsPerContingency.get(x1).contains(network.getBranch(x3)));
        assertTrue(networkElementsPerContingency.get(x2).contains(network.getBranch(x1)));
        assertFalse(networkElementsPerContingency.get(x2).contains(network.getBranch(x2)));
        assertTrue(networkElementsPerContingency.get(x2).contains(network.getBranch(x3)));
        assertTrue(networkElementsPerContingency.get(x3).contains(network.getBranch(x1)));
        assertTrue(networkElementsPerContingency.get(x3).contains(network.getBranch(x2)));
        assertFalse(networkElementsPerContingency.get(x3).contains(network.getBranch(x3)));
    }

    @Test
    void testContingencyIdNotDefined() {
        Exception exception = assertThrows(PowsyblException.class, () -> XnecProviderByIds.builder().addNetworkElementsAfterContingencies(Collections.emptySet(), Collections.singleton("NON EXISTING CONTINGENCY ID")));
        assertEquals("Contingency Id 'NON EXISTING CONTINGENCY ID' have not been defined. See addContingency and/or addContingencies", exception.getMessage());
    }

    @Test
    void testContingencyNotProvided() {
        String networkFileName = "NETWORK_PST_FLOW_WITH_COUNTRIES_NON_NEUTRAL.uct";
        Network network = TestUtils.importNetwork(networkFileName);
        assertTrue(XnecProviderByIds.builder().build().getNetworkElements("NON PRESENT CONTINGENCY ID", network).isEmpty());
    }

    @Test
    void testContingencyIdNotNull() {
        String networkFileName = "NETWORK_PST_FLOW_WITH_COUNTRIES_NON_NEUTRAL.uct";
        String x1 = "FGEN  11 BLOAD 11 1";
        String x2 = "FGEN  11 BLOAD 12 1";

        Network network = TestUtils.importNetwork(networkFileName);
        XnecProvider xnecProvider = XnecProviderByIds.builder()
            .addContingency(x2, Set.of(x2))
            .addNetworkElementsAfterContingencies(Set.of(x1), Set.of(x2))
            .build();
        Exception exception = assertThrows(NullPointerException.class, () -> xnecProvider.getNetworkElements(null, network));
        assertEquals("Contingency Id must be specified", exception.getMessage());
    }
}
