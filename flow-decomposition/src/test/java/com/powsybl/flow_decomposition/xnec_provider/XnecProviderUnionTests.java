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
import com.powsybl.flow_decomposition.TestUtils;
import com.powsybl.flow_decomposition.XnecProvider;
import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Network;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 */
class XnecProviderUnionTests {
    @Test
    void testUnionSingleProviderOnBasecase() {
        String networkFileName = "NETWORK_PARALLEL_LINES_PTDF.uct";
        Network network = TestUtils.importNetwork(networkFileName);
        String lineFrBe = "FLOAD 11 BLOAD 11 1";
        String lineBeFr = "FGEN  11 BLOAD 11 1";
        String line1 = "FGEN  11 FLOAD 11 1";
        String line2 = "FGEN  11 FLOAD 11 2";
        String line3 = "FGEN  11 FLOAD 11 3";
        String line4 = "FGEN  11 FLOAD 11 4";
        String line5 = "FGEN  11 FLOAD 11 5";
        String line6 = "FGEN  11 FLOAD 11 6";
        String line7 = "FGEN  11 FLOAD 11 7";
        String line8 = "FGEN  11 FLOAD 11 8";
        String line9 = "FGEN  11 FLOAD 11 9";
        XnecProvider xnecProvider5percPtdf = new XnecProvider5percPtdf();
        XnecProvider xnecProvider = new XnecProviderUnion(List.of(xnecProvider5percPtdf));
        Set<Branch<?>> branchSet = xnecProvider.getNetworkElements(network);
        assertTrue(branchSet.contains(network.getBranch(lineFrBe)));
        assertTrue(branchSet.contains(network.getBranch(lineBeFr)));
        assertTrue(branchSet.contains(network.getBranch(line1)));
        assertTrue(branchSet.contains(network.getBranch(line2)));
        assertTrue(branchSet.contains(network.getBranch(line3)));
        assertTrue(branchSet.contains(network.getBranch(line4)));
        assertTrue(branchSet.contains(network.getBranch(line5)));
        assertTrue(branchSet.contains(network.getBranch(line6)));
        assertTrue(branchSet.contains(network.getBranch(line7)));
        assertTrue(branchSet.contains(network.getBranch(line8)));
        assertTrue(branchSet.contains(network.getBranch(line9)));
        assertEquals(11, branchSet.size());
    }

    @Test
    void testUnionMultipleProvidersOnBasecase() {
        String networkFileName = "NETWORK_PARALLEL_LINES_PTDF.uct";
        Network network = TestUtils.importNetwork(networkFileName);
        String lineFrBe = "FLOAD 11 BLOAD 11 1";
        String lineBeFr = "FGEN  11 BLOAD 11 1";
        String line1 = "FGEN  11 FLOAD 11 1";
        String line8 = "FGEN  11 FLOAD 11 8";
        XnecProvider xnecProviderInterCo = new XnecProviderInterconnection();
        XnecProvider xnecProviderId = XnecProviderByIds.builder()
            .addNetworkElementsOnBasecase(Set.of(line8, line1))
            .build();
        XnecProvider xnecProvider = new XnecProviderUnion(List.of(xnecProviderInterCo, xnecProviderId));
        Set<Branch<?>> branchSet = xnecProvider.getNetworkElements(network);
        assertTrue(branchSet.contains(network.getBranch(lineFrBe)));
        assertTrue(branchSet.contains(network.getBranch(lineBeFr)));
        assertTrue(branchSet.contains(network.getBranch(line1)));
        assertTrue(branchSet.contains(network.getBranch(line8)));
        assertEquals(4, branchSet.size());
    }

    @Test
    void testUnionMultipleProvidersWithDuplicateBranchIdOnBasecase() {
        String networkFileName = "NETWORK_PARALLEL_LINES_PTDF.uct";
        Network network = TestUtils.importNetwork(networkFileName);
        String lineFrBe = "FLOAD 11 BLOAD 11 1";
        String lineBeFr = "FGEN  11 BLOAD 11 1";
        String line1 = "FGEN  11 FLOAD 11 1";
        String line8 = "FGEN  11 FLOAD 11 8";
        XnecProvider xnecProviderInterCo = new XnecProviderInterconnection();
        XnecProvider xnecProviderId = XnecProviderByIds.builder()
            .addNetworkElementsOnBasecase(Set.of(line8, line1, lineFrBe))
            .build();
        XnecProvider xnecProvider = new XnecProviderUnion(List.of(xnecProviderInterCo, xnecProviderId));
        Set<Branch<?>> branchSet = xnecProvider.getNetworkElements(network);
        assertTrue(branchSet.contains(network.getBranch(lineFrBe)));
        assertTrue(branchSet.contains(network.getBranch(lineBeFr)));
        assertTrue(branchSet.contains(network.getBranch(line1)));
        assertTrue(branchSet.contains(network.getBranch(line8)));
        assertEquals(4, branchSet.size());
    }

    @Test
    void testUnionSingleProviderOnContingencyState() {
        String networkFileName = "NETWORK_PARALLEL_LINES_PTDF.uct";
        Network network = TestUtils.importNetwork(networkFileName);
        String lineFrBe = "FLOAD 11 BLOAD 11 1";
        String line1 = "FGEN  11 FLOAD 11 1";
        String line8 = "FGEN  11 FLOAD 11 8";
        XnecProvider xnecProviderId = XnecProviderByIds.builder()
            .addContingency(lineFrBe, Set.of(lineFrBe))
            .addNetworkElementsAfterContingencies(Set.of(line8, line1, lineFrBe), Set.of(lineFrBe))
            .build();
        XnecProvider xnecProvider = new XnecProviderUnion(List.of(xnecProviderId));

        List<Contingency> contingencies = xnecProvider.getContingencies(network);
        assertEquals(1, contingencies.size());
        assertTrue(contingencies.contains(Contingency.builder(lineFrBe).addBranch(lineFrBe).build()));

        Set<Branch<?>> branchSet = xnecProvider.getNetworkElements(network);
        assertTrue(branchSet.isEmpty());

        Set<Branch<?>> branchSetFrBe = xnecProvider.getNetworkElements(lineFrBe, network);
        assertTrue(branchSetFrBe.contains(network.getBranch(line1)));
        assertTrue(branchSetFrBe.contains(network.getBranch(line8)));
        assertEquals(2, branchSetFrBe.size());

        Map<String, Set<Branch<?>>> networkElementsPerContingency = xnecProvider.getNetworkElementsPerContingency(network);
        assertEquals(1, networkElementsPerContingency.size());
        assertTrue(networkElementsPerContingency.containsKey(lineFrBe));
        assertEquals(2, networkElementsPerContingency.get(lineFrBe).size());
        assertTrue(networkElementsPerContingency.get(lineFrBe).contains(network.getBranch(line1)));
        assertTrue(networkElementsPerContingency.get(lineFrBe).contains(network.getBranch(line8)));
    }

    @Test
    void testUnionMultipleProvidersOnContingencyStateSameContingency() {
        String networkFileName = "NETWORK_PARALLEL_LINES_PTDF.uct";
        Network network = TestUtils.importNetwork(networkFileName);
        String lineFrBe = "FLOAD 11 BLOAD 11 1";
        String lineBeFr = "FGEN  11 BLOAD 11 1";
        String line1 = "FGEN  11 FLOAD 11 1";
        String line2 = "FGEN  11 FLOAD 11 2";
        String line8 = "FGEN  11 FLOAD 11 8";
        XnecProvider xnecProviderId1 = XnecProviderByIds.builder()
            .addContingency(lineFrBe, Set.of(lineFrBe))
            .addNetworkElementsAfterContingencies(Set.of(line8, line1, lineBeFr), Set.of(lineFrBe))
            .build();
        XnecProvider xnecProviderId2 = XnecProviderByIds.builder()
            .addContingency(lineFrBe, Set.of(lineFrBe))
            .addNetworkElementsAfterContingencies(Set.of(line2, lineBeFr), Set.of(lineFrBe))
            .build();
        XnecProvider xnecProvider = new XnecProviderUnion(List.of(xnecProviderId1, xnecProviderId2));

        List<Contingency> contingencies = xnecProvider.getContingencies(network);
        assertEquals(1, contingencies.size());
        assertTrue(contingencies.contains(Contingency.builder(lineFrBe).addBranch(lineFrBe).build()));

        Set<Branch<?>> branchSet = xnecProvider.getNetworkElements(network);
        assertTrue(branchSet.isEmpty());

        Set<Branch<?>> branchSetFrBe = xnecProvider.getNetworkElements(lineFrBe, network);
        assertTrue(branchSetFrBe.contains(network.getBranch(line1)));
        assertTrue(branchSetFrBe.contains(network.getBranch(line2)));
        assertTrue(branchSetFrBe.contains(network.getBranch(line8)));
        assertTrue(branchSetFrBe.contains(network.getBranch(lineBeFr)));
        assertEquals(4, branchSetFrBe.size());

        Map<String, Set<Branch<?>>> networkElementsPerContingency = xnecProvider.getNetworkElementsPerContingency(network);
        assertEquals(1, networkElementsPerContingency.size());
        assertTrue(networkElementsPerContingency.containsKey(lineFrBe));
        assertEquals(4, networkElementsPerContingency.get(lineFrBe).size());
        assertTrue(networkElementsPerContingency.get(lineFrBe).contains(network.getBranch(line1)));
        assertTrue(networkElementsPerContingency.get(lineFrBe).contains(network.getBranch(line2)));
        assertTrue(networkElementsPerContingency.get(lineFrBe).contains(network.getBranch(line8)));
        assertTrue(networkElementsPerContingency.get(lineFrBe).contains(network.getBranch(lineBeFr)));
    }

    @Test
    void testUnionMultipleProvidersOnContingencyStateNotUniqueContingencyDefinition() {
        String networkFileName = "NETWORK_PARALLEL_LINES_PTDF.uct";
        Network network = TestUtils.importNetwork(networkFileName);
        String lineFrBe = "FLOAD 11 BLOAD 11 1";
        String lineBeFr = "FGEN  11 BLOAD 11 1";
        String line1 = "FGEN  11 FLOAD 11 1";
        String line2 = "FGEN  11 FLOAD 11 2";
        String line8 = "FGEN  11 FLOAD 11 8";
        XnecProvider xnecProviderId1 = XnecProviderByIds.builder()
            .addContingency(lineFrBe, Set.of(lineFrBe))
            .addNetworkElementsAfterContingencies(Set.of(line8, line1, lineFrBe), Set.of(lineFrBe))
            .build();
        XnecProvider xnecProviderId2 = XnecProviderByIds.builder()
            .addContingency(lineFrBe, Set.of(line8))
            .addNetworkElementsAfterContingencies(Set.of(line2, lineBeFr, lineFrBe), Set.of(lineFrBe))
            .build();
        XnecProvider xnecProvider = new XnecProviderUnion(List.of(xnecProviderId1, xnecProviderId2, xnecProviderId1));

        PowsyblException exception1 = assertThrows(PowsyblException.class, () -> xnecProvider.getContingencies(network));
        assertEquals("Contingency 'FLOAD 11 BLOAD 11 1' definition is not unique across different providers", exception1.getMessage());

        PowsyblException exception2 = assertThrows(PowsyblException.class, () -> xnecProvider.getNetworkElements(lineFrBe, network));
        assertEquals("Contingency 'FLOAD 11 BLOAD 11 1' definition is not unique across different providers", exception2.getMessage());

        PowsyblException exception3 = assertThrows(PowsyblException.class, () -> xnecProvider.getNetworkElementsPerContingency(network));
        assertEquals("Contingency 'FLOAD 11 BLOAD 11 1' definition is not unique across different providers", exception3.getMessage());
    }

    @Test
    void testUnionMultipleProvidersOnContingencyStateDifferentContingencies() {
        String networkFileName = "NETWORK_PARALLEL_LINES_PTDF.uct";
        Network network = TestUtils.importNetwork(networkFileName);
        String lineFrBe = "FLOAD 11 BLOAD 11 1";
        String lineBeFr = "FGEN  11 BLOAD 11 1";
        String line1 = "FGEN  11 FLOAD 11 1";
        String line2 = "FGEN  11 FLOAD 11 2";
        String line8 = "FGEN  11 FLOAD 11 8";
        XnecProvider xnecProviderId1 = XnecProviderByIds.builder()
            .addContingency(lineFrBe, Set.of(lineFrBe))
            .addNetworkElementsAfterContingencies(Set.of(line8, line1), Set.of(lineFrBe))
            .build();
        XnecProvider xnecProviderId2 = XnecProviderByIds.builder()
            .addContingency(line8, Set.of(line8))
            .addNetworkElementsAfterContingencies(Set.of(lineBeFr, lineFrBe), Set.of(line8))
            .build();
        XnecProvider xnecProviderId3 = XnecProviderByIds.builder()
            .addContingency(line8, Set.of(line8))
            .addNetworkElementsAfterContingencies(Set.of(line2, line1), Set.of(line8))
            .build();
        XnecProvider xnecProvider = new XnecProviderUnion(List.of(xnecProviderId1, xnecProviderId2, xnecProviderId3));

        List<Contingency> contingencies = xnecProvider.getContingencies(network);
        assertEquals(2, contingencies.size());
        assertTrue(contingencies.contains(Contingency.builder(lineFrBe).addBranch(lineFrBe).build()));
        assertTrue(contingencies.contains(Contingency.builder(line8).addBranch(line8).build()));

        Set<Branch<?>> branchSet = xnecProvider.getNetworkElements(network);
        assertTrue(branchSet.isEmpty());

        Set<Branch<?>> branchSetFrBe = xnecProvider.getNetworkElements(lineFrBe, network);
        assertTrue(branchSetFrBe.contains(network.getBranch(line1)));
        assertTrue(branchSetFrBe.contains(network.getBranch(line8)));
        assertEquals(2, branchSetFrBe.size());

        Set<Branch<?>> branchSet8 = xnecProvider.getNetworkElements(line8, network);
        assertTrue(branchSet8.contains(network.getBranch(line1)));
        assertTrue(branchSet8.contains(network.getBranch(line2)));
        assertTrue(branchSet8.contains(network.getBranch(lineBeFr)));
        assertTrue(branchSet8.contains(network.getBranch(lineFrBe)));
        assertEquals(4, branchSet8.size());

        Map<String, Set<Branch<?>>> networkElementsPerContingency = xnecProvider.getNetworkElementsPerContingency(network);
        assertEquals(2, networkElementsPerContingency.size());
        assertTrue(networkElementsPerContingency.containsKey(lineFrBe));
        assertTrue(networkElementsPerContingency.containsKey(line8));
        assertEquals(2, networkElementsPerContingency.get(lineFrBe).size());
        assertTrue(networkElementsPerContingency.get(lineFrBe).contains(network.getBranch(line1)));
        assertTrue(networkElementsPerContingency.get(lineFrBe).contains(network.getBranch(line8)));
        assertEquals(4, networkElementsPerContingency.get(line8).size());
        assertTrue(networkElementsPerContingency.get(line8).contains(network.getBranch(line1)));
        assertTrue(networkElementsPerContingency.get(line8).contains(network.getBranch(line2)));
        assertTrue(networkElementsPerContingency.get(line8).contains(network.getBranch(lineBeFr)));
        assertTrue(networkElementsPerContingency.get(line8).contains(network.getBranch(lineFrBe)));
    }

    @Test
    void testContingencyConsistencyCacheIsUpdatedWhenChangingNetworks() {
        String networkFileName1 = "NETWORK_PST_FLOW_WITH_COUNTRIES.uct";
        String networkFileName2 = "NETWORK_PST_FLOW_WITH_COUNTRIES_NON_NEUTRAL.uct";
        String x1 = "FGEN  11 BLOAD 11 1";
        String x2 = "FGEN  11 BLOAD 12 1";

        Network network1 = TestUtils.importNetwork(networkFileName1);
        Network network2 = TestUtils.importNetwork(networkFileName2);

        XnecProvider xnecProviderId1 = XnecProviderByIds.builder()
            .addContingency(x1, Set.of(x1))
            .addNetworkElementsAfterContingencies(Set.of(x2), Set.of(x1))
            .build();
        XnecProvider xnecProviderId2 = XnecProviderByIds.builder()
            .addContingency(x2, Set.of(x2))
            .addNetworkElementsAfterContingencies(Set.of(x1), Set.of(x2))
            .build();
        XnecProvider xnecProvider = new XnecProviderUnion(List.of(xnecProviderId1, xnecProviderId2));

        testXnecProviderValidForNetwork(network1, xnecProvider, x1, x2);
        testXnecProviderValidForNetwork(network2, xnecProvider, x1, x2);
    }

    private static void testXnecProviderValidForNetwork(Network network, XnecProvider xnecProvider, String x1, String x2) {
        List<Contingency> contingencies = xnecProvider.getContingencies(network);
        assertEquals(2, contingencies.size());
        assertTrue(contingencies.contains(Contingency.builder(x1).addBranch(x1).build()));
        assertTrue(contingencies.contains(Contingency.builder(x2).addBranch(x2).build()));

        Set<Branch<?>> branchSet1 = xnecProvider.getNetworkElements(network);
        assertTrue(branchSet1.isEmpty());

        Set<Branch<?>> branchSet1x1 = xnecProvider.getNetworkElements(x1, network);
        assertTrue(branchSet1x1.contains(network.getBranch(x2)));
        assertEquals(1, branchSet1x1.size());

        Set<Branch<?>> branchSet1x2 = xnecProvider.getNetworkElements(x2, network);
        assertTrue(branchSet1x2.contains(network.getBranch(x1)));
        assertEquals(1, branchSet1x2.size());

        Map<String, Set<Branch<?>>> networkElementsPerContingency = xnecProvider.getNetworkElementsPerContingency(network);
        assertEquals(2, networkElementsPerContingency.size());
        assertTrue(networkElementsPerContingency.containsKey(x1));
        assertTrue(networkElementsPerContingency.containsKey(x2));
        assertEquals(1, networkElementsPerContingency.get(x1).size());
        assertTrue(networkElementsPerContingency.get(x1).contains(network.getBranch(x2)));
        assertEquals(1, networkElementsPerContingency.get(x2).size());
        assertTrue(networkElementsPerContingency.get(x2).contains(network.getBranch(x1)));
    }
}
