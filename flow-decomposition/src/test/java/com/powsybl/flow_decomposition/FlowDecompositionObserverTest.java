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
    private enum Event {
        RUN_START,
        RUN_DONE,
        COMPUTING_BASE_CASE,
        COMPUTING_CONTINGENCY,
        COMPUTED_GLSK,
        COMPUTED_NET_POSITIONS,
        COMPUTED_NODAL_INJECTIONS_MATRIX,
        COMPUTED_PTDF_MATRIX,
        COMPUTED_PSDF_MATRIX,
        COMPUTED_AC_NODAL_INJECTIONS,
        COMPUTED_DC_NODAL_INJECTIONS,
        COMPUTED_AC_FLOWS,
        COMPUTED_DC_FLOWS
    }

    private static final String BASE_CASE = "base-case";

    /**
     * ObserverReport gathers all observed events from the flow decomposition. It keeps the events occuring, and the
     * matrices
     */
    private static final class ObserverReport implements FlowDecompositionObserver {

        private final List<Event> events = new LinkedList<>();
        private String currentContingency = null;
        private final ContingencyValue<List<Event>> eventsPerContingency = new ContingencyValue<>();
        private Map<Country, Map<String, Double>> glsks;
        private Map<Country, Double> netPositions;
        private final ContingencyValue<Map<String, Map<String, Double>>> nodalInjections = new ContingencyValue<>();
        private final ContingencyValue<Map<String, Map<String, Double>>> ptdfs = new ContingencyValue<>();
        private final ContingencyValue<Map<String, Map<String, Double>>> psdfs = new ContingencyValue<>();
        private final ContingencyValue<Map<String, Double>> acNodalInjections = new ContingencyValue<>();
        private final ContingencyValue<Map<String, Double>> dcNodalInjections = new ContingencyValue<>();
        private final ContingencyValue<Map<String, Double>> acFlows = new ContingencyValue<>();
        private final ContingencyValue<Map<String, Double>> dcFlows = new ContingencyValue<>();

        public List<Event> allEvents() {
            return events;
        }

        public List<Event> eventsForBaseCase() {
            return eventsPerContingency.forBaseCase();
        }

        public List<Event> eventsForContingency(String contingencyId) {
            return eventsPerContingency.forContingency(contingencyId);
        }

        @Override
        public void runStart() {
            addEvent(Event.RUN_START);
        }

        @Override
        public void runDone() {
            addEvent(Event.RUN_DONE);
        }

        @Override
        public void computingBaseCase() {
            currentContingency = BASE_CASE;
            addEvent(Event.COMPUTING_BASE_CASE);
        }

        @Override
        public void computingContingency(String contingencyId) {
            currentContingency = contingencyId;
            addEvent(Event.COMPUTING_CONTINGENCY);
        }

        @Override
        public void computedGlsk(Map<Country, Map<String, Double>> glsks) {
            addEvent(Event.COMPUTED_GLSK);
            this.glsks = glsks;
        }

        @Override
        public void computedNetPositions(Map<Country, Double> netPositions) {
            addEvent(Event.COMPUTED_NET_POSITIONS);
            this.netPositions = netPositions;
        }

        @Override
        public void computedNodalInjectionsMatrix(Map<String, Map<String, Double>> nodalInjections) {
            addEvent(Event.COMPUTED_NODAL_INJECTIONS_MATRIX);
            this.nodalInjections.put(currentContingency, nodalInjections);
        }

        @Override
        public void computedPtdfMatrix(Map<String, Map<String, Double>> pdtfMatrix) {
            addEvent(Event.COMPUTED_PTDF_MATRIX);
            this.ptdfs.put(currentContingency, pdtfMatrix);
        }

        @Override
        public void computedPsdfMatrix(Map<String, Map<String, Double>> psdfMatrix) {
            addEvent(Event.COMPUTED_PSDF_MATRIX);
            this.psdfs.put(currentContingency, psdfMatrix);
        }

        @Override
        public void computedAcNodalInjections(Map<String, Double> positions, boolean fallbackHasBeenActivated) {
            addEvent(Event.COMPUTED_AC_NODAL_INJECTIONS);
            this.acNodalInjections.put(currentContingency, positions);
        }

        @Override
        public void computedDcNodalInjections(Map<String, Double> positions) {
            addEvent(Event.COMPUTED_DC_NODAL_INJECTIONS);
            this.dcNodalInjections.put(currentContingency, positions);
        }

        @Override
        public void computedAcFlows(Map<String, Double> flows) {
            addEvent(Event.COMPUTED_AC_FLOWS);
            this.acFlows.put(currentContingency, flows);
        }

        @Override
        public void computedDcFlows(Map<String, Double> flows) {
            addEvent(Event.COMPUTED_DC_FLOWS);
            this.dcFlows.put(currentContingency, flows);
        }

        private void addEvent(Event e) {
            if (currentContingency != null) {
                eventsPerContingency.putIfAbsent(currentContingency, new LinkedList<>());
                eventsPerContingency.forContingency(currentContingency).add(e);
            }
            events.add(e);
        }
    }

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
        var report = new ObserverReport();

        flowComputer.addObserver(report);

        flowComputer.run(xnecProvider, network);

        assertEventsFired(report.allEvents(), Event.COMPUTED_GLSK, Event.COMPUTED_NET_POSITIONS);

        assertEventsFired(
            report.eventsForBaseCase(),
            Event.COMPUTED_AC_FLOWS,
            Event.COMPUTED_AC_NODAL_INJECTIONS,
            Event.COMPUTED_DC_FLOWS,
            Event.COMPUTED_DC_NODAL_INJECTIONS,
            Event.COMPUTED_NODAL_INJECTIONS_MATRIX,
            Event.COMPUTED_PTDF_MATRIX,
            Event.COMPUTED_PSDF_MATRIX);

        for (var contingencyId : List.of(contingencyId1, contingencyId2)) {
            assertEventsFired(
                report.eventsForContingency(contingencyId),
                Event.COMPUTED_AC_FLOWS,
                Event.COMPUTED_AC_NODAL_INJECTIONS,
                Event.COMPUTED_DC_FLOWS,
                Event.COMPUTED_DC_NODAL_INJECTIONS,
                Event.COMPUTED_NODAL_INJECTIONS_MATRIX,
                Event.COMPUTED_PTDF_MATRIX,
                Event.COMPUTED_PSDF_MATRIX);
        }

        // Checking GLSK
        assertEquals(Set.of(Country.BE, Country.DE, Country.FR), report.glsks.keySet());
        assertEquals(Set.of("DB000011_generator", "DF000011_generator"), report.glsks.get(Country.DE).keySet());

        // Checking net positions
        assertEquals(Set.of(Country.BE, Country.DE, Country.FR), report.netPositions.keySet());

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
        for (var contingencyId : List.of(BASE_CASE, contingencyId1, contingencyId2)) {
            assertEquals(xnecNodes, report.nodalInjections.forContingency(contingencyId).keySet());
            assertEquals(
                Set.of("Allocated Flow", "Loop Flow from BE"),
                report.nodalInjections.forContingency(contingencyId).get("BB000021_load").keySet());
        }

        // Checking PTDFs
        for (var contingencyId : List.of(BASE_CASE, contingencyId1, contingencyId2)) {
            var branches = Set.of(branchId);
            assertEquals(branches, report.ptdfs.forContingency(contingencyId).keySet());
            assertEquals(xnecNodes, report.ptdfs.forContingency(contingencyId).get(branchId).keySet());
        }

        // Checking PSDFs
        for (var contingencyId : List.of(BASE_CASE, contingencyId1, contingencyId2)) {
            var branches = Set.of(branchId);
            var pstNodes = Set.of("BF000011 BF000012 1");
            assertEquals(branches, report.psdfs.forContingency(contingencyId).keySet());
            assertEquals(
                pstNodes,
                report.psdfs.forContingency(contingencyId).get(branchId).keySet(),
                "contingency = " + contingencyId);
        }

        // Checking AC nodal injections
        for (var contingencyId : List.of(BASE_CASE, contingencyId1, contingencyId2)) {
            assertEquals(xnecNodes, report.acNodalInjections.forContingency(contingencyId).keySet());
        }

        // Checking DC nodal injections
        for (var contingencyId : List.of(BASE_CASE, contingencyId1, contingencyId2)) {
            assertEquals(xnecNodes, report.dcNodalInjections.forContingency(contingencyId).keySet());
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
        for (var contingencyId : List.of(BASE_CASE, contingencyId1, contingencyId2)) {
            assertEquals(allBranches, report.acFlows.forContingency(contingencyId).keySet());
        }

        // Checking DC flows
        for (var contingencyId : List.of(BASE_CASE, contingencyId1, contingencyId2)) {
            assertEquals(allBranches, report.dcFlows.forContingency(contingencyId).keySet());
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
        var reportInserted = new ObserverReport();
        flowComputer.addObserver(reportInserted);

        var reportRemoved = new ObserverReport();
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
        var report = new ObserverReport();
        flowComputer.addObserver(report);
        flowComputer.run(xnecProvider, network);

        // there are no losses in acNodalInjection
        report.acNodalInjections.forBaseCase().forEach((inj, p) -> assertFalse(inj.startsWith(LossesCompensator.LOSSES_ID_PREFIX)));
    }

    private void assertEventsFired(Collection<Event> firedEvents, Event... expectedEvents) {
        var missing = new HashSet<Event>();
        Collections.addAll(missing, expectedEvents);
        missing.removeAll(firedEvents);
        assertTrue(missing.isEmpty(), () -> "Missing events: " + missing);
    }

    private static final class ContingencyValue<T> {
        private final Map<String, T> values = new HashMap<>();

        public void put(String contingencyId, T value) {
            values.put(contingencyId, value);
        }

        public void putIfAbsent(String contingencyId, T value) {
            values.putIfAbsent(contingencyId, value);
        }

        public T forContingency(String contingencyId) {
            return values.get(contingencyId);
        }

        public T forBaseCase() {
            return values.get(BASE_CASE);
        }
    }
}
