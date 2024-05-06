/*
 * Copyright (c) 2024, Coreso SA (https://www.coreso.eu/) and TSCNET Services GmbH (https://www.tscnet.eu/)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.flow_decomposition;

import com.powsybl.flow_decomposition.xnec_provider.XnecProviderByIds;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * @author Guillaume Verger {@literal <guillaume.verger at artelys.com>}
 */
class FlowDecompositionObserverTest {

    @Test
    void testNStateN1AndN2PostContingencyState() {
        String networkFileName = "19700101_0000_FO4_UX1.uct";
        String branchId = "DB000011 DF000011 1";
        String contingencyElementId1 = "FB000011 FD000011 1";
        String contingencyElementId2 = "FB000021 FD000021 1";
        String contingencyId1 = "DD000011 DF000011 1";
        String contingencyId2 = "FB000011 FD000011 1_FB000021 FD000021 1";
        Network network = TestUtils.importNetwork(networkFileName);
        Map<String, Set<String>> contingencies = Map.ofEntries(
            Map.entry(contingencyId1, Set.of(contingencyId1)),
            Map.entry(contingencyId2, Set.of(contingencyElementId1, contingencyElementId2)));
        XnecProvider xnecProvider = XnecProviderByIds.builder()
                                                     .addContingencies(contingencies)
                                                     .addNetworkElementsAfterContingencies(
                                                         Set.of(branchId),
                                                         Set.of(contingencyId1, contingencyId2))
                                                     .addNetworkElementsOnBasecase(Set.of(branchId))
                                                     .build();
        var flowDecompositionParameters = FlowDecompositionParameters.load();
        FlowDecompositionComputer flowComputer = new FlowDecompositionComputer(flowDecompositionParameters);
        var report = new FlowDecompositionOberserverImpl();
        flowComputer.addObserver(report);
        flowComputer.run(xnecProvider, network);
        assertEventsFired(report.allEvents(), FlowDecompositionOberserverImpl.Event.COMPUTED_GLSK, FlowDecompositionOberserverImpl.Event.COMPUTED_NET_POSITIONS);
        assertEventsFired(
            report.eventsForBaseCase(),
            FlowDecompositionOberserverImpl.Event.COMPUTED_AC_FLOWS,
            FlowDecompositionOberserverImpl.Event.COMPUTED_AC_NODAL_INJECTIONS,
            FlowDecompositionOberserverImpl.Event.COMPUTED_DC_FLOWS,
            FlowDecompositionOberserverImpl.Event.COMPUTED_DC_NODAL_INJECTIONS,
            FlowDecompositionOberserverImpl.Event.COMPUTED_NODAL_INJECTIONS_MATRIX,
            FlowDecompositionOberserverImpl.Event.COMPUTED_PTDF_MATRIX,
            FlowDecompositionOberserverImpl.Event.COMPUTED_PSDF_MATRIX);

        for (var contingencyId : List.of(contingencyId1, contingencyId2)) {
            assertEventsFired(
                report.eventsForContingency(contingencyId),
                FlowDecompositionOberserverImpl.Event.COMPUTED_AC_FLOWS,
                FlowDecompositionOberserverImpl.Event.COMPUTED_AC_NODAL_INJECTIONS,
                FlowDecompositionOberserverImpl.Event.COMPUTED_DC_FLOWS,
                FlowDecompositionOberserverImpl.Event.COMPUTED_DC_NODAL_INJECTIONS,
                FlowDecompositionOberserverImpl.Event.COMPUTED_NODAL_INJECTIONS_MATRIX,
                FlowDecompositionOberserverImpl.Event.COMPUTED_PTDF_MATRIX,
                FlowDecompositionOberserverImpl.Event.COMPUTED_PSDF_MATRIX);
        }

        // Checking GLSK
        assertEquals(Set.of(Country.BE, Country.DE, Country.FR), report.getGlsks().keySet());
        assertEquals(Set.of("DB000011_generator", "DF000011_generator"), report.getGlsks().get(Country.DE).keySet());

        // Checking net positions
        assertEquals(Set.of(Country.BE, Country.DE, Country.FR), report.getNetPositions().keySet());

        var xnecNodes = Set.of(
            "BB000021_load",
            "BF000011_generator",
            "BF000021_load",
            "DB000011_generator",
            "DD000011_load",
            "DF000011_generator",
            "FB000021_generator",
            "FB000022_load",
            "FD000011_load",
            "FF000011_generator",
            "XES00011 FD000011 1",
            "XNL00011 BB000011 1");

        // Checking nodal injections
        for (var contingencyId : List.of(FlowDecompositionOberserverImpl.BASE_CASE, contingencyId1, contingencyId2)) {
            assertEquals(xnecNodes, report.getNodalInjections().forContingency(contingencyId).keySet());
            assertEquals(
                Set.of("Allocated Flow", "Loop Flow from BE"),
                report.getNodalInjections().forContingency(contingencyId).get("BB000021_load").keySet());
        }

        // Checking PTDFs
        for (var contingencyId : List.of(FlowDecompositionOberserverImpl.BASE_CASE, contingencyId1, contingencyId2)) {
            var branches = Set.of(branchId);
            assertEquals(branches, report.getPtdfs().forContingency(contingencyId).keySet());
            assertEquals(xnecNodes, report.getPtdfs().forContingency(contingencyId).get(branchId).keySet());
        }

        // Checking PSDFs
        for (var contingencyId : List.of(FlowDecompositionOberserverImpl.BASE_CASE, contingencyId1, contingencyId2)) {
            var branches = Set.of(branchId);
            var pstNodes = Set.of("BF000011 BF000012 1");
            assertEquals(branches, report.getPsdfs().forContingency(contingencyId).keySet());
            assertEquals(
                pstNodes,
                report.getPsdfs().forContingency(contingencyId).get(branchId).keySet(),
                "contingency = " + contingencyId);
        }

        // Checking AC nodal injections
        for (var contingencyId : List.of(FlowDecompositionOberserverImpl.BASE_CASE, contingencyId1, contingencyId2)) {
            assertEquals(xnecNodes, report.getAcNodalInjections().forContingency(contingencyId).keySet());
        }

        // Checking DC nodal injections
        for (var contingencyId : List.of(FlowDecompositionOberserverImpl.BASE_CASE, contingencyId1, contingencyId2)) {
            assertEquals(xnecNodes, report.getDcNodalInjections().forContingency(contingencyId).keySet());
        }

        var allBranches = Set.of(
            "BB000011 BB000021 1",
            "BB000011 BD000011 1",
            "BB000011 BF000012 1",
            "BB000021 BD000021 1",
            "BB000021 BF000021 1",
            "BD000011 BD000021 1",
            "BD000011 BF000011 1",
            "BD000021 BF000021 1",
            "BF000011 BF000012 1",
            "BF000011 BF000021 1",
            "DB000011 DD000011 1",
            "DB000011 DF000011 1",
            "DD000011 DF000011 1",
            "FB000011 FB000022 1",
            "FB000011 FD000011 1",
            "FB000011 FF000011 1",
            "FB000021 FD000021 1",
            "FD000011 FD000021 1",
            "FD000011 FF000011 1",
            "FD000011 FF000011 2",
            "XBD00011 BD000011 1 + XBD00011 DB000011 1",
            "XBD00012 BD000011 1 + XBD00012 DB000011 1",
            "XBF00011 BF000011 1 + XBF00011 FB000011 1",
            "XBF00021 BF000021 1 + XBF00021 FB000021 1",
            "XBF00022 BF000021 1 + XBF00022 FB000022 1",
            "XDF00011 DF000011 1 + XDF00011 FD000011 1");

        // Checking AC flows
        for (var contingencyId : List.of(FlowDecompositionOberserverImpl.BASE_CASE, contingencyId1, contingencyId2)) {
            assertEquals(allBranches, report.getAcFlows().forContingency(contingencyId).keySet());
        }

        // Checking DC flows
        for (var contingencyId : List.of(FlowDecompositionOberserverImpl.BASE_CASE, contingencyId1, contingencyId2)) {
            assertEquals(allBranches, report.getDcFlows().forContingency(contingencyId).keySet());
        }
    }

    @Test
    void testRemoveObserver() {
        String networkFileName = "19700101_0000_FO4_UX1.uct";
        String branchId = "DB000011 DF000011 1";
        Network network = TestUtils.importNetwork(networkFileName);
        XnecProvider xnecProvider = XnecProviderByIds.builder()
                                                     .addNetworkElementsOnBasecase(Set.of(branchId))
                                                     .build();
        var flowDecompositionParameters = FlowDecompositionParameters.load();
        FlowDecompositionComputer flowComputer = new FlowDecompositionComputer(flowDecompositionParameters);
        var reportInserted = new FlowDecompositionOberserverImpl();
        flowComputer.addObserver(reportInserted);
        var reportRemoved = new FlowDecompositionOberserverImpl();
        flowComputer.addObserver(reportRemoved);
        flowComputer.removeObserver(reportRemoved);

        flowComputer.run(xnecProvider, network);

        assertFalse(reportInserted.allEvents().isEmpty());
        assertTrue(reportRemoved.allEvents().isEmpty());
    }

    @Test
    void testObserverWithEnableLossesCompensation() {
        String networkFileName = "19700101_0000_FO4_UX1.uct";
        String branchId = "DB000011 DF000011 1";
        Network network = TestUtils.importNetwork(networkFileName);
        XnecProvider xnecProvider = XnecProviderByIds.builder()
                .addNetworkElementsOnBasecase(Set.of(branchId))
                .build();
        var flowDecompositionParameters = FlowDecompositionParameters.load().setEnableLossesCompensation(true);
        FlowDecompositionComputer flowComputer = new FlowDecompositionComputer(flowDecompositionParameters);
        var report = new FlowDecompositionOberserverImpl();
        flowComputer.addObserver(report);
        flowComputer.run(xnecProvider, network);

        // losses at 0 in acNodalInjection
        String lossesId = LossesCompensator.getLossesId("");
        report.getAcNodalInjections().forBaseCase().forEach((inj, p) -> {
            if (inj.startsWith(lossesId)) {
                assertEquals(0, p, 1E-8);
            }
        });
    }

    private void assertEventsFired(Collection<FlowDecompositionOberserverImpl.Event> firedEvents, FlowDecompositionOberserverImpl.Event... expectedEvents) {
        var missing = new HashSet<FlowDecompositionOberserverImpl.Event>();
        Collections.addAll(missing, expectedEvents);
        missing.removeAll(firedEvents);
        assertTrue(missing.isEmpty(), () -> "Missing events: " + missing);
    }
}
