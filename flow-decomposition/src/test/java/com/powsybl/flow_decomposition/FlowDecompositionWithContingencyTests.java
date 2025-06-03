/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.flow_decomposition;

import com.powsybl.contingency.Contingency;
import com.powsybl.flow_decomposition.xnec_provider.XnecProviderByIds;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 */
class FlowDecompositionWithContingencyTests {
    private static final double EPSILON = 1e-3;

    @Test
    void testSingleN1PostContingencyState() {
        String networkFileName = "19700101_0000_FO4_UX1.uct";
        String branchId = "DB000011 DF000011 1";
        String contingencyId = "DD000011 DF000011 1";
        String xnecId = "DB000011 DF000011 1_DD000011 DF000011 1";

        Network network = TestUtils.importNetwork(networkFileName);
        XnecProvider xnecProvider = XnecProviderByIds.builder()
            .addContingency(contingencyId, Set.of(contingencyId))
            .addNetworkElementsAfterContingencies(Set.of(branchId), Set.of(contingencyId))
            .build();
        FlowDecompositionParameters flowDecompositionParameters = FlowDecompositionParameters.load()
            .setEnableLossesCompensation(FlowDecompositionParameters.ENABLE_LOSSES_COMPENSATION)
            .setRescaleMode(FlowDecompositionParameters.RescaleMode.NONE);
        FlowDecompositionComputer flowComputer = new FlowDecompositionComputer(flowDecompositionParameters);
        FlowDecompositionResults flowDecompositionResults = flowComputer.run(xnecProvider, network);
        TestUtils.assertCoherenceTotalFlow(flowDecompositionParameters.getRescaleMode(), flowDecompositionResults);

        Map<String, DecomposedFlow> decomposedFlowMap = flowDecompositionResults.getDecomposedFlowMap();
        validateFlowDecompositionOnXnec(xnecId, branchId, contingencyId, decomposedFlowMap.get(xnecId), -1269.932, 31.943);
    }

    @Test
    void testSingleN1PostBranchContingencyState() {
        String networkFileName = "19700101_0000_FO4_UX1.uct";
        String branchId = "DB000011 DF000011 1";
        String contingencyId = "DD000011 DF000011 1";
        String xnecId = "DB000011 DF000011 1_DD000011 DF000011 1";

        Network network = TestUtils.importNetwork(networkFileName);
        Contingency contingency = Contingency.builder(contingencyId).addBranch(contingencyId).build();
        XnecProvider xnecProvider = XnecProviderByIds.builder()
                .addContingency(contingency)
                .addNetworkElementsAfterContingencies(Set.of(branchId), Set.of(contingencyId))
                .build();
        FlowDecompositionParameters flowDecompositionParameters = FlowDecompositionParameters.load()
                .setEnableLossesCompensation(FlowDecompositionParameters.ENABLE_LOSSES_COMPENSATION)
                .setRescaleMode(FlowDecompositionParameters.RescaleMode.NONE);
        FlowDecompositionComputer flowComputer = new FlowDecompositionComputer(flowDecompositionParameters);
        FlowDecompositionResults flowDecompositionResults = flowComputer.run(xnecProvider, network);
        TestUtils.assertCoherenceTotalFlow(flowDecompositionParameters.getRescaleMode(), flowDecompositionResults);

        Map<String, DecomposedFlow> decomposedFlowMap = flowDecompositionResults.getDecomposedFlowMap();
        validateFlowDecompositionOnXnec(xnecId, branchId, contingencyId, decomposedFlowMap.get(xnecId), -1269.932, 31.943);
    }

    @Test
    void testSingleN1PostLoadContingencyState() {
        String networkFileName = "19700101_0000_FO4_UX1.uct";
        String branchId = "DB000011 DF000011 1";
        String contingencyId = "FD000011_load";
        String xnecId = "DB000011 DF000011 1_FD000011_load";

        Network network = TestUtils.importNetwork(networkFileName);
        Contingency contingency = Contingency.builder(contingencyId).addLoad(contingencyId).build();
        XnecProvider xnecProvider = XnecProviderByIds.builder()
                .addContingency(contingency)
                .addNetworkElementsAfterContingencies(Set.of(branchId), Set.of(contingencyId))
                .build();
        FlowDecompositionParameters flowDecompositionParameters = FlowDecompositionParameters.load()
                .setEnableLossesCompensation(FlowDecompositionParameters.ENABLE_LOSSES_COMPENSATION)
                .setRescaleMode(FlowDecompositionParameters.RescaleMode.NONE);
        FlowDecompositionComputer flowComputer = new FlowDecompositionComputer(flowDecompositionParameters);
        FlowDecompositionResults flowDecompositionResults = flowComputer.run(xnecProvider, network);
        TestUtils.assertCoherenceTotalFlow(flowDecompositionParameters.getRescaleMode(), flowDecompositionResults);

        Map<String, DecomposedFlow> decomposedFlowMap = flowDecompositionResults.getDecomposedFlowMap();
        validateFlowDecompositionOnXnec(xnecId, branchId, contingencyId, decomposedFlowMap.get(xnecId), -312.215, 29.285);
    }

    @Test
    void testSingleN1PostSwitchContingencyState() {
        String networkFileName = "19700101_0000_FO4_UX1.uct";
        String branchId = "DB000011 DF000011 1";
        String contingencyId = "FB000021 FB000022 1";
        String xnecId = "DB000011 DF000011 1_FB000021 FB000022 1";

        Network network = TestUtils.importNetwork(networkFileName);
        Contingency contingency = Contingency.builder(contingencyId).addSwitch(contingencyId).build();
        XnecProvider xnecProvider = XnecProviderByIds.builder()
                .addContingency(contingency)
                .addNetworkElementsAfterContingencies(Set.of(branchId), Set.of(contingencyId))
                .build();
        FlowDecompositionParameters flowDecompositionParameters = FlowDecompositionParameters.load()
                .setEnableLossesCompensation(FlowDecompositionParameters.ENABLE_LOSSES_COMPENSATION)
                .setRescaleMode(FlowDecompositionParameters.RescaleMode.NONE);
        FlowDecompositionComputer flowComputer = new FlowDecompositionComputer(flowDecompositionParameters);
        FlowDecompositionResults flowDecompositionResults = flowComputer.run(xnecProvider, network);
        TestUtils.assertCoherenceTotalFlow(flowDecompositionParameters.getRescaleMode(), flowDecompositionResults);

        Map<String, DecomposedFlow> decomposedFlowMap = flowDecompositionResults.getDecomposedFlowMap();
        validateFlowDecompositionOnXnec(xnecId, branchId, contingencyId, decomposedFlowMap.get(xnecId), -300.186, 22.162);
    }

    @Test
    void testSingleN1PostDanglingLineContingencyState() {
        String networkFileName = "19700101_0000_FO4_UX1.uct";
        String branchId = "DB000011 DF000011 1";
        String contingencyId = "XBF00011 FB000011 1";
        String xnecId = "DB000011 DF000011 1_XBF00011 FB000011 1";

        Network network = TestUtils.importNetwork(networkFileName);
        Contingency contingency = Contingency.builder(contingencyId).addDanglingLine(contingencyId).build();
        XnecProvider xnecProvider = XnecProviderByIds.builder()
                .addContingency(contingency)
                .addNetworkElementsAfterContingencies(Set.of(branchId), Set.of(contingencyId))
                .build();
        FlowDecompositionParameters flowDecompositionParameters = FlowDecompositionParameters.load()
                .setEnableLossesCompensation(FlowDecompositionParameters.ENABLE_LOSSES_COMPENSATION)
                .setRescaleMode(FlowDecompositionParameters.RescaleMode.NONE);
        FlowDecompositionComputer flowComputer = new FlowDecompositionComputer(flowDecompositionParameters);
        FlowDecompositionResults flowDecompositionResults = flowComputer.run(xnecProvider, network);
        TestUtils.assertCoherenceTotalFlow(flowDecompositionParameters.getRescaleMode(), flowDecompositionResults);

        Map<String, DecomposedFlow> decomposedFlowMap = flowDecompositionResults.getDecomposedFlowMap();
        validateFlowDecompositionOnXnec(xnecId, branchId, contingencyId, decomposedFlowMap.get(xnecId), -545.584, 32.623);
    }

    @Test
    void testNStateAndN1PostContingencyState() {
        String networkFileName = "19700101_0000_FO4_UX1.uct";
        String branchId = "DB000011 DF000011 1";
        String contingencyId1 = "";
        String contingencyId2 = "DD000011 DF000011 1";
        String xnecId1 = "DB000011 DF000011 1";
        String xnecId2 = "DB000011 DF000011 1_DD000011 DF000011 1";

        Network network = TestUtils.importNetwork(networkFileName);
        XnecProvider xnecProvider = XnecProviderByIds.builder()
            .addContingency(contingencyId2, Set.of(contingencyId2))
            .addNetworkElementsAfterContingencies(Set.of(branchId), Set.of(contingencyId2))
            .addNetworkElementsOnBasecase(Set.of(branchId))
            .build();
        FlowDecompositionParameters flowDecompositionParameters = FlowDecompositionParameters.load()
            .setEnableLossesCompensation(FlowDecompositionParameters.ENABLE_LOSSES_COMPENSATION)
            .setRescaleMode(FlowDecompositionParameters.RescaleMode.NONE);
        FlowDecompositionComputer flowComputer = new FlowDecompositionComputer(flowDecompositionParameters);
        FlowDecompositionResults flowDecompositionResults = flowComputer.run(xnecProvider, network);
        TestUtils.assertCoherenceTotalFlow(flowDecompositionParameters.getRescaleMode(), flowDecompositionResults);

        Map<String, DecomposedFlow> decomposedFlowMap = flowDecompositionResults.getDecomposedFlowMap();
        validateFlowDecompositionOnXnec(xnecId1, branchId, contingencyId1, decomposedFlowMap.get(xnecId1), -300.420, 22.472);
        validateFlowDecompositionOnXnec(xnecId2, branchId, contingencyId2, decomposedFlowMap.get(xnecId2), -1269.932, 31.943);
    }

    @Test
    void testSingleN2PostContingencyState() {
        String networkFileName = "19700101_0000_FO4_UX1.uct";
        String branchId = "DB000011 DF000011 1";
        String contingencyElementId1 = "FB000011 FD000011 1";
        String contingencyElementId2 = "FB000021 FD000021 1";
        String contingencyId = "FB000011 FD000011 1_FB000021 FD000021 1";
        String xnecId = "DB000011 DF000011 1_FB000011 FD000011 1_FB000021 FD000021 1";

        Network network = TestUtils.importNetwork(networkFileName);
        XnecProvider xnecProvider = XnecProviderByIds.builder()
            .addContingency(contingencyId, Set.of(contingencyElementId1, contingencyElementId2))
            .addNetworkElementsAfterContingencies(Set.of(branchId), Set.of(contingencyId))
            .build();
        FlowDecompositionParameters flowDecompositionParameters = FlowDecompositionParameters.load()
            .setEnableLossesCompensation(FlowDecompositionParameters.ENABLE_LOSSES_COMPENSATION)
            .setRescaleMode(FlowDecompositionParameters.RescaleMode.NONE);
        FlowDecompositionComputer flowComputer = new FlowDecompositionComputer(flowDecompositionParameters);
        FlowDecompositionResults flowDecompositionResults = flowComputer.run(xnecProvider, network);
        TestUtils.assertCoherenceTotalFlow(flowDecompositionParameters.getRescaleMode(), flowDecompositionResults);

        Map<String, DecomposedFlow> decomposedFlowMap = flowDecompositionResults.getDecomposedFlowMap();
        validateFlowDecompositionOnXnec(xnecId, branchId, contingencyId, decomposedFlowMap.get(xnecId), -406.204, 48.362);
    }

    @Test
    void testNStateN1AndN2PostContingencyState() {
        String networkFileName = "19700101_0000_FO4_UX1.uct";
        String branchId = "DB000011 DF000011 1";
        String contingencyElementId1 = "FB000011 FD000011 1";
        String contingencyElementId2 = "FB000021 FD000021 1";
        String contingencyId1 = "";
        String contingencyId2 = "DD000011 DF000011 1";
        String contingencyId3 = "FB000011 FD000011 1_FB000021 FD000021 1";
        String xnecId1 = "DB000011 DF000011 1";
        String xnecId2 = "DB000011 DF000011 1_DD000011 DF000011 1";
        String xnecId3 = "DB000011 DF000011 1_FB000011 FD000011 1_FB000021 FD000021 1";

        Network network = TestUtils.importNetwork(networkFileName);
        XnecProvider xnecProvider = XnecProviderByIds.builder()
            .addContingencies(Map.of(contingencyId2, Set.of(contingencyId2), contingencyId3, Set.of(contingencyElementId1, contingencyElementId2)))
            .addNetworkElementsAfterContingencies(Set.of(branchId), Set.of(contingencyId2, contingencyId3))
            .addNetworkElementsOnBasecase(Set.of(branchId))
            .build();
        FlowDecompositionParameters flowDecompositionParameters = FlowDecompositionParameters.load()
            .setEnableLossesCompensation(FlowDecompositionParameters.ENABLE_LOSSES_COMPENSATION)
            .setRescaleMode(FlowDecompositionParameters.RescaleMode.NONE);
        FlowDecompositionComputer flowComputer = new FlowDecompositionComputer(flowDecompositionParameters);
        FlowDecompositionResults flowDecompositionResults = flowComputer.run(xnecProvider, network);
        TestUtils.assertCoherenceTotalFlow(flowDecompositionParameters.getRescaleMode(), flowDecompositionResults);

        Map<String, DecomposedFlow> decomposedFlowMap = flowDecompositionResults.getDecomposedFlowMap();
        validateFlowDecompositionOnXnec(xnecId1, branchId, contingencyId1, decomposedFlowMap.get(xnecId1), -300.420, 22.472);
        validateFlowDecompositionOnXnec(xnecId2, branchId, contingencyId2, decomposedFlowMap.get(xnecId2), -1269.932, 31.943);
        validateFlowDecompositionOnXnec(xnecId3, branchId, contingencyId3, decomposedFlowMap.get(xnecId3), -406.204, 48.362);
    }

    private static void validateFlowDecompositionOnXnec(String xnecId,
                                                        String branchId,
                                                        String contingencyId,
                                                        DecomposedFlow decomposedFlow,
                                                        double expectedDcReferenceFlow,
                                                        double expectedFrLoopFlow) {
        assertEquals(branchId, decomposedFlow.getBranchId());
        assertEquals(contingencyId, decomposedFlow.getContingencyId());
        assertEquals(xnecId, decomposedFlow.getId());
        assertEquals(expectedDcReferenceFlow, decomposedFlow.getDcReferenceFlow(), EPSILON);
        assertEquals(expectedFrLoopFlow, decomposedFlow.getLoopFlow(Country.FR), EPSILON);
    }
}
