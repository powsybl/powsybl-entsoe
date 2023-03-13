/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.flow_decomposition;

import com.powsybl.flow_decomposition.xnec_provider.XnecProviderAllBranches;
import com.powsybl.flow_decomposition.xnec_provider.XnecProviderByIds;
import com.powsybl.flow_decomposition.xnec_provider.XnecProviderInterconnection;
import com.powsybl.flow_decomposition.xnec_provider.XnecProviderUnion;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
            .setRescaleEnabled(FlowDecompositionParameters.DISABLE_RESCALED_RESULTS);
        FlowDecompositionComputer flowComputer = new FlowDecompositionComputer(flowDecompositionParameters);
        FlowDecompositionResults flowDecompositionResults = flowComputer.run(xnecProvider, network);
        TestUtils.assertCoherenceTotalFlow(flowDecompositionParameters.isRescaleEnabled(), flowDecompositionResults);

        Map<String, DecomposedFlow> decomposedFlowMap = flowDecompositionResults.getDecomposedFlowMap();
        validateFlowDecompositionOnXnec(xnecId, branchId, contingencyId, decomposedFlowMap.get(xnecId), -1269.932, -22.027);
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
            .setRescaleEnabled(FlowDecompositionParameters.DISABLE_RESCALED_RESULTS);
        FlowDecompositionComputer flowComputer = new FlowDecompositionComputer(flowDecompositionParameters);
        FlowDecompositionResults flowDecompositionResults = flowComputer.run(xnecProvider, network);
        TestUtils.assertCoherenceTotalFlow(flowDecompositionParameters.isRescaleEnabled(), flowDecompositionResults);

        Map<String, DecomposedFlow> decomposedFlowMap = flowDecompositionResults.getDecomposedFlowMap();
        validateFlowDecompositionOnXnec(xnecId1, branchId, contingencyId1, decomposedFlowMap.get(xnecId1), -300.420, -15.496);
        validateFlowDecompositionOnXnec(xnecId2, branchId, contingencyId2, decomposedFlowMap.get(xnecId2), -1269.932, -22.027);
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
            .setRescaleEnabled(FlowDecompositionParameters.DISABLE_RESCALED_RESULTS);
        FlowDecompositionComputer flowComputer = new FlowDecompositionComputer(flowDecompositionParameters);
        FlowDecompositionResults flowDecompositionResults = flowComputer.run(xnecProvider, network);
        TestUtils.assertCoherenceTotalFlow(flowDecompositionParameters.isRescaleEnabled(), flowDecompositionResults);

        Map<String, DecomposedFlow> decomposedFlowMap = flowDecompositionResults.getDecomposedFlowMap();
        validateFlowDecompositionOnXnec(xnecId, branchId, contingencyId, decomposedFlowMap.get(xnecId), -406.204, 3.329);
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
            .setRescaleEnabled(FlowDecompositionParameters.DISABLE_RESCALED_RESULTS);
        FlowDecompositionComputer flowComputer = new FlowDecompositionComputer(flowDecompositionParameters);
        FlowDecompositionResults flowDecompositionResults = flowComputer.run(xnecProvider, network);
        TestUtils.assertCoherenceTotalFlow(flowDecompositionParameters.isRescaleEnabled(), flowDecompositionResults);

        Map<String, DecomposedFlow> decomposedFlowMap = flowDecompositionResults.getDecomposedFlowMap();
        validateFlowDecompositionOnXnec(xnecId1, branchId, contingencyId1, decomposedFlowMap.get(xnecId1), -300.420, -15.496);
        validateFlowDecompositionOnXnec(xnecId2, branchId, contingencyId2, decomposedFlowMap.get(xnecId2), -1269.932, -22.027);
        validateFlowDecompositionOnXnec(xnecId3, branchId, contingencyId3, decomposedFlowMap.get(xnecId3), -406.204, 3.329);
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

    @Test
    void testFullIntegrationWithRescaling() {
        String networkFileName = "NETWORK_PST_FLOW_WITH_COUNTRIES.uct";
        String x1 = "FGEN  11 BLOAD 11 1";
        String x2 = "FGEN  11 BLOAD 12 1";
        String pst = "BLOAD 11 BLOAD 12 2";
        String contingencyId = x1 + "_" + pst;

        Network network = TestUtils.importNetwork(networkFileName);
        FlowDecompositionParameters parameters = new FlowDecompositionParameters()
            .setRescaleEnabled(true);
        FlowDecompositionComputer flowComputer = new FlowDecompositionComputer(parameters);
        XnecProviderByIds xnecProviderByIds = XnecProviderByIds.builder()
            .addContingency(x1, Set.of(x1))
            .addNetworkElementsAfterContingencies(Set.of(x1, x2, pst), Set.of(x1))
            .addContingency(contingencyId, Set.of(x1, pst))
            .addNetworkElementsAfterContingencies(Set.of(x1, x2, pst), Set.of(contingencyId))
            .build();
        XnecProviderInterconnection xnecProviderInterconnection = new XnecProviderInterconnection();
        XnecProviderAllBranches xnecProviderAllBranches = new XnecProviderAllBranches();
        XnecProvider xnecProvider = new XnecProviderUnion(List.of(xnecProviderByIds, xnecProviderAllBranches, xnecProviderInterconnection));
        FlowDecompositionResults flowDecompositionResults = flowComputer.run(xnecProvider, network);
        assertEquals(6, flowDecompositionResults.getDecomposedFlowMap().size());
        assertTrue(flowDecompositionResults.getDecomposedFlowMap().containsKey(x1));
        assertTrue(flowDecompositionResults.getDecomposedFlowMap().containsKey(x2));
        assertTrue(flowDecompositionResults.getDecomposedFlowMap().containsKey(pst));
        assertTrue(flowDecompositionResults.getDecomposedFlowMap().containsKey(x2 + "_" + x1));
        assertTrue(flowDecompositionResults.getDecomposedFlowMap().containsKey(pst + "_" + x1));
        assertTrue(flowDecompositionResults.getDecomposedFlowMap().containsKey(x2 + "_" + contingencyId));
        assertEquals(new DecomposedFlow(x1, "", Country.FR, Country.BE, 29.003009422979176, 24.999999999999993, 33.002024914534545, 0.0, 0.0, 0.0, Map.of("Loop Flow from BE", -1.9995077457776844, "Loop Flow from FR", -1.9995077457776862)), flowDecompositionResults.getDecomposedFlowMap().get(x1));
        assertEquals(new DecomposedFlow(pst, x1, Country.BE, Country.BE, 31.99999999999722, -0.0, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Collections.emptyMap()), flowDecompositionResults.getDecomposedFlowMap().get(pst + "_" + x1));
    }

    @Test
    void testFullIntegrationWithoutRescaling() {
        String networkFileName = "NETWORK_PST_FLOW_WITH_COUNTRIES.uct";
        String x1 = "FGEN  11 BLOAD 11 1";
        String x2 = "FGEN  11 BLOAD 12 1";
        String pst = "BLOAD 11 BLOAD 12 2";
        String contingencyId = x1 + "_" + pst;

        Network network = TestUtils.importNetwork(networkFileName);
        FlowDecompositionParameters parameters = new FlowDecompositionParameters()
            .setRescaleEnabled(false);
        FlowDecompositionComputer flowComputer = new FlowDecompositionComputer(parameters);
        XnecProviderByIds xnecProviderByIds = XnecProviderByIds.builder()
            .addContingency(x1, Set.of(x1))
            .addNetworkElementsAfterContingencies(Set.of(x1, x2, pst), Set.of(x1))
            .addContingency(contingencyId, Set.of(x1, pst))
            .addNetworkElementsAfterContingencies(Set.of(x1, x2, pst), Set.of(contingencyId))
            .build();
        XnecProviderInterconnection xnecProviderInterconnection = new XnecProviderInterconnection();
        XnecProviderAllBranches xnecProviderAllBranches = new XnecProviderAllBranches();
        XnecProvider xnecProvider = new XnecProviderUnion(List.of(xnecProviderByIds, xnecProviderAllBranches, xnecProviderInterconnection));
        FlowDecompositionResults flowDecompositionResults = flowComputer.run(xnecProvider, network);
        assertEquals(6, flowDecompositionResults.getDecomposedFlowMap().size());
        assertTrue(flowDecompositionResults.getDecomposedFlowMap().containsKey(x1));
        assertTrue(flowDecompositionResults.getDecomposedFlowMap().containsKey(x2));
        assertTrue(flowDecompositionResults.getDecomposedFlowMap().containsKey(pst));
        assertTrue(flowDecompositionResults.getDecomposedFlowMap().containsKey(x2 + "_" + x1));
        assertTrue(flowDecompositionResults.getDecomposedFlowMap().containsKey(pst + "_" + x1));
        assertTrue(flowDecompositionResults.getDecomposedFlowMap().containsKey(x2 + "_" + contingencyId));
        assertEquals(new DecomposedFlow(x1, "", Country.FR, Country.BE, 29.003009422979176, 24.999999999999993, 28.999015491555372, 0.0, -0.0, 0.0, Map.of("Loop Flow from BE", -1.9995077457776844, "Loop Flow from FR", -1.9995077457776862)), flowDecompositionResults.getDecomposedFlowMap().get(x1));
        assertEquals(new DecomposedFlow(pst, x1, Country.BE, Country.BE, 31.99999999999722, -0.0, 0.0, 0.0, 0.0, -0.0, Collections.emptyMap()), flowDecompositionResults.getDecomposedFlowMap().get(pst + "_" + x1));
    }
}
