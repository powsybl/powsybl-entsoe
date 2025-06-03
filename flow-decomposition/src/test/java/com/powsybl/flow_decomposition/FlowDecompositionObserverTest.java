/*
 * Copyright (c) 2024, Coreso SA (https://www.coreso.eu/) and TSCNET Services GmbH (https://www.tscnet.eu/)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.flow_decomposition;

import com.powsybl.contingency.Contingency;
import com.powsybl.flow_decomposition.partitioners.ReferenceNodalInjectionComputer;
import com.powsybl.flow_decomposition.xnec_provider.XnecProviderAllBranches;
import com.powsybl.flow_decomposition.xnec_provider.XnecProviderByIds;
import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.TwoSides;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.loadflow.LoadFlowResult;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.stream.Collectors;

import static com.powsybl.flow_decomposition.partitioners.SensitivityAnalyser.respectFlowSignConvention;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Guillaume Verger {@literal <guillaume.verger at artelys.com>}
 * @author Caio Luke {@literal <caio.luke at artelys.com>}
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
        COMPUTED_DC_FLOWS,
        COMPUTED_AC_CURRENTS,
        COMPUTED_PRE_RESCALING_DECOMPOSED_FLOWS
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
        private final ContingencyValue<LoadFlowResult> acLoadFlowResult = new ContingencyValue<>();
        private final ContingencyValue<Boolean> acLoadFlowFallbackHasBeenActivated = new ContingencyValue<>();
        private final ContingencyValue<LoadFlowResult> dcLoadFlowResult = new ContingencyValue<>();
        private final ContingencyValue<Map<String, Map<String, Double>>> nodalInjections = new ContingencyValue<>();
        private final ContingencyValue<Map<String, Map<String, Double>>> ptdfs = new ContingencyValue<>();
        private final ContingencyValue<Map<String, Map<String, Double>>> psdfs = new ContingencyValue<>();
        private final ContingencyValue<Map<String, Double>> acNodalInjections = new ContingencyValue<>();
        private final ContingencyValue<Map<String, Double>> dcNodalInjections = new ContingencyValue<>();
        private final ContingencyValue<Map<String, Double>> acFlowsTerminal1 = new ContingencyValue<>();
        private final ContingencyValue<Map<String, Double>> acFlowsTerminal2 = new ContingencyValue<>();
        private final ContingencyValue<Map<String, Double>> dcFlows = new ContingencyValue<>();
        private final ContingencyValue<Map<String, Double>> acCurrentsTerminal1 = new ContingencyValue<>();
        private final ContingencyValue<Map<String, Double>> acCurrentsTerminal2 = new ContingencyValue<>();
        private final ContingencyValue<Map<String, DecomposedFlow>> preRescalingDecomposedFlows = new ContingencyValue<>();

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
        public void computedPtdfMatrix(Map<String, Map<String, Double>> ptdfMatrix) {
            addEvent(Event.COMPUTED_PTDF_MATRIX);
            this.ptdfs.put(currentContingency, ptdfMatrix);
        }

        @Override
        public void computedPsdfMatrix(Map<String, Map<String, Double>> psdfMatrix) {
            addEvent(Event.COMPUTED_PSDF_MATRIX);
            this.psdfs.put(currentContingency, psdfMatrix);
        }

        @Override
        public void computedAcLoadFlowResults(Network network, LoadFlowResult loadFlowResult, boolean fallbackHasBeenActivated) {
            this.acLoadFlowResult.put(currentContingency, loadFlowResult);
            this.acLoadFlowFallbackHasBeenActivated.put(currentContingency, fallbackHasBeenActivated);
            computedAcNodalInjections(network);
            computedAcFlowsTerminal1(network, fallbackHasBeenActivated);
            computedAcFlowsTerminal2(network, fallbackHasBeenActivated);
            computedAcCurrentsTerminal1(network, fallbackHasBeenActivated);
            computedAcCurrentsTerminal2(network, fallbackHasBeenActivated);
        }

        @Override
        public void computedDcLoadFlowResults(Network network, LoadFlowResult loadFlowResult) {
            this.dcLoadFlowResult.put(currentContingency, loadFlowResult);
            computedDcNodalInjections(network);
            computedDcFlows(network);
        }

        private void computedAcNodalInjections(Network network) {
            addEvent(Event.COMPUTED_AC_NODAL_INJECTIONS);
            Map<String, Double> injections = new ReferenceNodalInjectionComputer().run(NetworkUtil.getNodeList(network));
            this.acNodalInjections.put(currentContingency, injections);
        }

        private void computedDcNodalInjections(Network network) {
            addEvent(Event.COMPUTED_DC_NODAL_INJECTIONS);
            Map<String, Double> injections = new ReferenceNodalInjectionComputer().run(NetworkUtil.getNodeList(network));
            this.dcNodalInjections.put(currentContingency, injections);
        }

        private void computedAcFlowsTerminal1(Network network, boolean fallbackHasBeenActivated) {
            addEvent(Event.COMPUTED_AC_FLOWS);
            Map<String, Double> flows = FlowComputerUtils.calculateAcTerminalReferenceFlows(network.getBranchStream().map(branch -> (Branch<?>) branch).collect(Collectors.toList()), fallbackHasBeenActivated, TwoSides.ONE);
            this.acFlowsTerminal1.put(currentContingency, flows);
        }

        private void computedAcFlowsTerminal2(Network network, boolean fallbackHasBeenActivated) {
            addEvent(Event.COMPUTED_AC_FLOWS);
            Map<String, Double> flows = FlowComputerUtils.calculateAcTerminalReferenceFlows(network.getBranchStream().map(branch -> (Branch<?>) branch).collect(Collectors.toList()), fallbackHasBeenActivated, TwoSides.TWO);
            this.acFlowsTerminal2.put(currentContingency, flows);
        }

        private void computedDcFlows(Network network) {
            addEvent(Event.COMPUTED_DC_FLOWS);
            Map<String, Double> flows = FlowComputerUtils.getTerminalReferenceFlow(network.getBranchStream().map(branch -> (Branch<?>) branch).collect(Collectors.toList()), TwoSides.ONE);
            this.dcFlows.put(currentContingency, flows);
        }

        private void computedAcCurrentsTerminal1(Network network, boolean fallbackHasBeenActivated) {
            addEvent(Event.COMPUTED_AC_CURRENTS);
            Map<String, Double> currents = FlowComputerUtils.calculateAcTerminalCurrents(network.getBranchStream().map(branch -> (Branch<?>) branch).collect(Collectors.toList()), fallbackHasBeenActivated, TwoSides.ONE);
            this.acCurrentsTerminal1.put(currentContingency, currents);
        }

        private void computedAcCurrentsTerminal2(Network network, boolean fallbackHasBeenActivated) {
            addEvent(Event.COMPUTED_AC_CURRENTS);
            Map<String, Double> currents = FlowComputerUtils.calculateAcTerminalCurrents(network.getBranchStream().map(branch -> (Branch<?>) branch).collect(Collectors.toList()), fallbackHasBeenActivated, TwoSides.TWO);
            this.acCurrentsTerminal2.put(currentContingency, currents);
        }

        @Override
        public void computedPreRescalingDecomposedFlows(DecomposedFlow decomposedFlow) {
            addEvent(Event.COMPUTED_PRE_RESCALING_DECOMPOSED_FLOWS);
            this.preRescalingDecomposedFlows.put(currentContingency, Map.of(decomposedFlow.getBranchId(), decomposedFlow));
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

        validateObserverReportLoadFlowResult(report, List.of(BASE_CASE, contingencyId1, contingencyId2));
        validateObserverReportEvents(report, List.of(BASE_CASE, contingencyId1, contingencyId2), Boolean.TRUE);
        validateObserverReportGlsk(report, Set.of("DB000011_generator", "DF000011_generator"));
        validateObserverReportNetPositions(report, Set.of(Country.BE, Country.DE, Country.FR));

        Set<String> xnecNodes = Set.of(
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
        validateObserverReportNodalInjections(
                report,
                List.of(BASE_CASE, contingencyId1, contingencyId2),
                xnecNodes,
                "BB000021_load",
                Set.of("Allocated Flow", "Loop Flow from BE"));
        validateObserverReportPtdfs(report, List.of(BASE_CASE, contingencyId1, contingencyId2), xnecNodes, branchId);
        validateObserverReportPsdfs(report, List.of(BASE_CASE, contingencyId1, contingencyId2), branchId, Set.of(
                "BF000011 BF000012 1"));

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

        validateObserverReportFlows(report, List.of(BASE_CASE, contingencyId1, contingencyId2), allBranches);

        var decomposedFlowBranches = Set.of("DB000011 DF000011 1");
        validateObserverReportDecomposedFlows(report, List.of(contingencyId1), decomposedFlowBranches);
    }

    @Test
    void testNStateN1LoadContingencyState() {
        String networkFileName = "19700101_0000_FO4_UX1.uct";
        String branchId = "DB000011 DF000011 1";
        String contingencyId1 = "FD000011_load";
        Network network = TestUtils.importNetwork(networkFileName);
        Contingency contingency = Contingency.builder(contingencyId1).addLoad(contingencyId1).build();
        XnecProvider xnecProvider = XnecProviderByIds.builder()
                .addContingency(contingency)
                .addNetworkElementsAfterContingencies(Set.of(branchId), Set.of(contingencyId1))
                .build();
        var flowDecompositionParameters = FlowDecompositionParameters.load();
        FlowDecompositionComputer flowComputer = new FlowDecompositionComputer(flowDecompositionParameters);
        var report = new ObserverReport();
        flowComputer.addObserver(report);
        flowComputer.run(xnecProvider, network);

        validateObserverReportLoadFlowResult(report, List.of(contingencyId1));
        validateObserverReportEvents(report, List.of(contingencyId1), Boolean.FALSE);
        validateObserverReportGlsk(report, Set.of("DB000011_generator", "DF000011_generator"));
        validateObserverReportNetPositions(report, Set.of(Country.BE, Country.DE, Country.FR));

        Set<String> xnecNodes = Set.of(
                "BB000021_load",
                "BF000011_generator",
                "BF000021_load",
                "DB000011_generator",
                "DD000011_load",
                "DF000011_generator",
                "FB000021_generator",
                "FB000022_load",
                "FF000011_generator",
                "XES00011 FD000011 1",
                "XNL00011 BB000011 1");
        validateObserverReportNodalInjections(
                report,
                List.of(contingencyId1),
                xnecNodes,
                "BF000021_load",
                Set.of("Allocated Flow", "Loop Flow from BE"));
        validateObserverReportPtdfs(report, List.of(contingencyId1), xnecNodes, branchId);
        validateObserverReportPsdfs(report, List.of(contingencyId1), branchId, Set.of("BF000011 BF000012 1"));

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

        validateObserverReportFlows(report, List.of(contingencyId1), allBranches);

        var decomposedFlowBranches = Set.of("DB000011 DF000011 1");
        validateObserverReportDecomposedFlows(report, List.of(contingencyId1), decomposedFlowBranches);
    }

    private static void validateObserverReportLoadFlowResult(ObserverReport report, List<String> contingencyIds) {
        for (var contingencyId : contingencyIds) {
            LoadFlowResult contingencyAcLoadFlowResult = report.acLoadFlowResult.forContingency(contingencyId);
            boolean contingencyAcLoadFlowFallbackHasBeenActivated = report.acLoadFlowFallbackHasBeenActivated.forContingency(contingencyId);
            LoadFlowResult contingencyDcLoadFlowResult = report.dcLoadFlowResult.forContingency(contingencyId);
            assertTrue(contingencyAcLoadFlowResult.isFullyConverged());
            assertFalse(contingencyAcLoadFlowFallbackHasBeenActivated);
            assertTrue(contingencyDcLoadFlowResult.isFullyConverged());
        }
    }

    private static void validateObserverReportEvents(ObserverReport report, List<String> contingencyIds, Boolean isBaseCaseExecuted) {
        assertEventsFired(report.allEvents(), Event.COMPUTED_GLSK, Event.COMPUTED_NET_POSITIONS);

        if (isBaseCaseExecuted) {
            assertEventsFired(report.eventsForBaseCase(),
                    Event.COMPUTED_AC_FLOWS,
                    Event.COMPUTED_AC_NODAL_INJECTIONS,
                    Event.COMPUTED_DC_FLOWS,
                    Event.COMPUTED_DC_NODAL_INJECTIONS,
                    Event.COMPUTED_NODAL_INJECTIONS_MATRIX,
                    Event.COMPUTED_PTDF_MATRIX,
                    Event.COMPUTED_PSDF_MATRIX,
                    Event.COMPUTED_PRE_RESCALING_DECOMPOSED_FLOWS);
        }

        for (var contingencyId : contingencyIds) {
            assertEventsFired(report.eventsForContingency(contingencyId),
                    Event.COMPUTED_AC_FLOWS,
                    Event.COMPUTED_AC_NODAL_INJECTIONS,
                    Event.COMPUTED_DC_FLOWS,
                    Event.COMPUTED_DC_NODAL_INJECTIONS,
                    Event.COMPUTED_NODAL_INJECTIONS_MATRIX,
                    Event.COMPUTED_PTDF_MATRIX,
                    Event.COMPUTED_PSDF_MATRIX,
                    Event.COMPUTED_PRE_RESCALING_DECOMPOSED_FLOWS);
        }
    }

    private static void validateObserverReportGlsk(ObserverReport report, Set<String> expectedResources) {
        assertEquals(Set.of(Country.BE, Country.DE, Country.FR), report.glsks.keySet());
        assertEquals(expectedResources, report.glsks.get(Country.DE).keySet());
    }

    private static void validateObserverReportNetPositions(ObserverReport report, Set<Country> expectedCountries) {
        assertEquals(expectedCountries, report.netPositions.keySet());
    }

    private static void validateObserverReportNodalInjections(ObserverReport report, List<String> caseIds,
                                                              Set<String> xnecNodes, String nodeId, Set<String> expectedFields) {
        for (var contingencyId : caseIds) {
            assertEquals(xnecNodes, report.nodalInjections.forContingency(contingencyId).keySet());
            assertEquals(xnecNodes, report.acNodalInjections.forContingency(contingencyId).keySet());
            assertEquals(xnecNodes, report.dcNodalInjections.forContingency(contingencyId).keySet());
            assertEquals(expectedFields, report.nodalInjections.forContingency(contingencyId).get(nodeId).keySet());
        }
    }

    private static void validateObserverReportPtdfs(ObserverReport report, List<String> caseIds, Set<String> xnecNodes, String branchId) {
        for (var contingencyId : caseIds) {
            var branches = Set.of(branchId);
            assertEquals(branches, report.ptdfs.forContingency(contingencyId).keySet());
            assertEquals(xnecNodes, report.ptdfs.forContingency(contingencyId).get(branchId).keySet());
        }
    }

    private static void validateObserverReportPsdfs(ObserverReport report, List<String> caseIds, String branchId, Set<String> pstNodes) {
        for (var contingencyId : caseIds) {
            var branches = Set.of(branchId);
            assertEquals(branches, report.psdfs.forContingency(contingencyId).keySet());
            assertEquals(pstNodes, report.psdfs.forContingency(contingencyId).get(branchId).keySet(), "contingency = " + contingencyId);
        }
    }

    private static void validateObserverReportFlows(ObserverReport report, List<String> caseIds, Set<String> allBranches) {
        for (var contingencyId : caseIds) {
            assertEquals(allBranches, report.acFlowsTerminal1.forContingency(contingencyId).keySet());
            assertEquals(allBranches, report.dcFlows.forContingency(contingencyId).keySet());
        }
    }

    private static void validateObserverReportDecomposedFlows(ObserverReport report, List<String> caseIds, Set<String> allBranches) {
        for (var contingencyId : caseIds) {
            assertEquals(allBranches, report.preRescalingDecomposedFlows.forContingency(contingencyId).keySet());
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

    @Test
    void testPtdfsStayTheSameWithLossCompensationAndSlackDistributionOnLoads() {
        String networkFileName = "ptdf_instability.xiidm";
        Network network = TestUtils.importNetwork(networkFileName);
        XnecProvider xnecProvider = new XnecProviderAllBranches();

        LoadFlowParameters loadFlowParameters = new LoadFlowParameters();
        loadFlowParameters.setBalanceType(LoadFlowParameters.BalanceType.PROPORTIONAL_TO_LOAD).setDistributedSlack(true);
        FlowDecompositionParameters flowDecompositionParameters = new FlowDecompositionParameters().setEnableLossesCompensation(false);

        // Without loss compensation
        FlowDecompositionComputer flowDecompositionComputer1 = new FlowDecompositionComputer(flowDecompositionParameters, loadFlowParameters);
        ObserverReport report1 = new ObserverReport();
        flowDecompositionComputer1.addObserver(report1);
        flowDecompositionComputer1.run(xnecProvider, network);

        // With loss compensation
        flowDecompositionParameters.setEnableLossesCompensation(true);
        FlowDecompositionComputer flowDecompositionComputer2 = new FlowDecompositionComputer(flowDecompositionParameters, loadFlowParameters);
        ObserverReport report2 = new ObserverReport();
        flowDecompositionComputer2.addObserver(report2);
        flowDecompositionComputer2.run(xnecProvider, network);

        // Intermediate results
        // With loss compensation
        var dcFlows1 = report1.dcFlows.forBaseCase();
        var ptdfs1 = report1.ptdfs.forBaseCase();

        // Without loss compensation
        var dcFlows2 = report2.dcFlows.forBaseCase();
        var ptdfs2 = report2.ptdfs.forBaseCase();

        // Ensure that ptdfs are the same with or without loss compensation
        ptdfs1.forEach((branchId, ptdfInjections1) -> {
            var ptdfInjections2 = ptdfs2.get(branchId);
            ptdfInjections1.forEach((injectionId, ptdfValue1) -> {
                Double ptdfValue2 = ptdfInjections2.get(injectionId);
                double ptdfSignConvention1 = respectFlowSignConvention(ptdfValue1, dcFlows1.get(branchId));
                double ptdfSignConvention2 = respectFlowSignConvention(ptdfValue2, dcFlows2.get(branchId));
                assertEquals(ptdfSignConvention1, ptdfSignConvention2, 1E-2);
            });
        });
    }

    private static void assertEventsFired(Collection<Event> firedEvents, Event... expectedEvents) {
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
