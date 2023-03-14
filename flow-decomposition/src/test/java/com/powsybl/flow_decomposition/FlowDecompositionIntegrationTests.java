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
class FlowDecompositionIntegrationTests {
    public static final String X1 = "FGEN  11 BLOAD 11 1";
    public static final String X2 = "FGEN  11 BLOAD 12 1";
    public static final String X2_WITH_X1_CONTINGENCY = X2 + "_" + X1;
    public static final String PST = "BLOAD 11 BLOAD 12 2";
    public static final String PST_WITH_X1_CONTINGENCY = PST + "_" + X1;
    public static final String N2_CONTINGENCY_ID = X1 + "_" + PST;
    public static final String X2_WITH_N2_CONTINGENCY = X2 + "_" + N2_CONTINGENCY_ID;
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
    void testIntegrationWithRescaling() {
        FlowDecompositionParameters parameters = new FlowDecompositionParameters()
            .setEnableLossesCompensation(FlowDecompositionParameters.DISABLE_LOSSES_COMPENSATION)
            .setRescaleEnabled(FlowDecompositionParameters.ENABLE_RESCALED_RESULTS);
        FlowDecompositionResults flowDecompositionResults = getFlowDecompositionResultsOnPSTNetwork(parameters);
        assertEquals(new DecomposedFlow(X1, "", Country.FR, Country.BE, 29.003009422979176, 24.999999999999993, 33.002024914534545, 0.0, 0.0, 0.0, Map.of("Loop Flow from BE", -1.9995077457776844, "Loop Flow from FR", -1.9995077457776862)), flowDecompositionResults.getDecomposedFlowMap().get(X1));
        assertEquals(new DecomposedFlow(X2, "", Country.FR, Country.BE, 87.00911182341697, 74.99999999999999, 99.00615829808308, 0.0, 0.0, 0.0, Map.of("Loop Flow from BE", -5.998523237333046, "Loop Flow from FR", -5.998523237333059)), flowDecompositionResults.getDecomposedFlowMap().get(X2));
        assertEquals(new DecomposedFlow(PST, "", Country.BE, Country.BE, 3.0056664783579006, -25.0, 7.004681969913275, 0.0, -0.0, -1.9995077457776844, Map.of("Loop Flow from FR", -1.9995077457776862)), flowDecompositionResults.getDecomposedFlowMap().get(PST));
        assertEquals(new DecomposedFlow(X2, X1, Country.FR, Country.BE, 116.01617882330939, 100.0, 132.0122407895309, 0.0, 0.0, 0.0, Map.of("Loop Flow from BE", -7.998030983110738, "Loop Flow from FR", -7.998030983110745)), flowDecompositionResults.getDecomposedFlowMap().get(X2_WITH_X1_CONTINGENCY));
        assertEquals(new DecomposedFlow(PST, X1, Country.BE, Country.BE, 31.99999999999722, -0.0, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Collections.emptyMap()), flowDecompositionResults.getDecomposedFlowMap().get(PST_WITH_X1_CONTINGENCY));
        assertEquals(new DecomposedFlow(X2, N2_CONTINGENCY_ID, Country.FR, Country.BE, 100.03453149519564, 100.0, 116.03059346141713, 0.0, 0.0, 0.0, Map.of("Loop Flow from BE", -7.998030983110738, "Loop Flow from FR", -7.998030983110745)), flowDecompositionResults.getDecomposedFlowMap().get(X2_WITH_N2_CONTINGENCY));
    }

    @Test
    void testIntegrationWithoutRescaling() {
        FlowDecompositionParameters parameters = new FlowDecompositionParameters()
            .setEnableLossesCompensation(FlowDecompositionParameters.DISABLE_LOSSES_COMPENSATION)
            .setRescaleEnabled(FlowDecompositionParameters.DISABLE_RESCALED_RESULTS);
        FlowDecompositionResults flowDecompositionResults = getFlowDecompositionResultsOnPSTNetwork(parameters);
        assertEquals(new DecomposedFlow(X1, "", Country.FR, Country.BE, 29.003009422979176, 24.999999999999993, 28.999015491555372, 0.0, -0.0, 0.0, Map.of("Loop Flow from BE", -1.9995077457776844, "Loop Flow from FR", -1.9995077457776862)), flowDecompositionResults.getDecomposedFlowMap().get(X1));
        assertEquals(new DecomposedFlow(X2, "", Country.FR, Country.BE, 87.00911182341697, 74.99999999999999, 86.99704647466612, 0.0, 0.0, 0.0, Map.of("Loop Flow from BE", -5.998523237333046, "Loop Flow from FR", -5.998523237333059)), flowDecompositionResults.getDecomposedFlowMap().get(X2));
        assertEquals(new DecomposedFlow(PST, "", Country.BE, Country.BE, 3.0056664783579006, -25.0, 28.999015491555372, 0.0, -0.0, -1.9995077457776844, Map.of("Loop Flow from FR", -1.9995077457776862)), flowDecompositionResults.getDecomposedFlowMap().get(PST));
        assertEquals(new DecomposedFlow(X2, X1, Country.FR, Country.BE, 116.01617882330939, 100.0, 115.99606196622149, 0.0, 0.0, 0.0, Map.of("Loop Flow from BE", -7.998030983110738, "Loop Flow from FR", -7.998030983110745)), flowDecompositionResults.getDecomposedFlowMap().get(X2_WITH_X1_CONTINGENCY));
        assertEquals(new DecomposedFlow(PST, X1, Country.BE, Country.BE, 31.99999999999722, -0.0, 0.0, 0.0, 0.0, -0.0, Collections.emptyMap()), flowDecompositionResults.getDecomposedFlowMap().get(PST_WITH_X1_CONTINGENCY));
        assertEquals(new DecomposedFlow(X2, N2_CONTINGENCY_ID, Country.FR, Country.BE, 100.03453149519564, 100.0, 115.99606196622149, 0.0, 0.0, 0.0, Map.of("Loop Flow from BE", -7.998030983110738, "Loop Flow from FR", -7.998030983110745)), flowDecompositionResults.getDecomposedFlowMap().get(X2_WITH_N2_CONTINGENCY));
    }

    @Test
    void testIntegrationWithRescalingWithLossCompensation() {
        FlowDecompositionParameters parameters = new FlowDecompositionParameters()
            .setEnableLossesCompensation(FlowDecompositionParameters.ENABLE_LOSSES_COMPENSATION)
            .setLossesCompensationEpsilon(FlowDecompositionParameters.DISABLE_LOSSES_COMPENSATION_EPSILON)
            .setRescaleEnabled(FlowDecompositionParameters.ENABLE_RESCALED_RESULTS);
        FlowDecompositionResults flowDecompositionResults = getFlowDecompositionResultsOnPSTNetwork(parameters);
        assertEquals(new DecomposedFlow(X1, "", Country.FR, Country.BE, 29.003009422979176, 28.996350163597047, 29.00567475093749, 0.0, 0.0, 0.0, Map.of("Loop Flow from BE", -0.0013326639791566564, "Loop Flow from FR", -0.0013326639791579886)), flowDecompositionResults.getDecomposedFlowMap().get(X1));
        assertEquals(new DecomposedFlow(X2, "", Country.FR, Country.BE, 87.00911182341697, 86.98905049079114, 87.01710780729192, 0.0, 0.0, 0.0, Map.of("Loop Flow from BE", -0.003997991937470857, "Loop Flow from FR", -0.003997991937473966)), flowDecompositionResults.getDecomposedFlowMap().get(X2));
        assertEquals(new DecomposedFlow(PST, "", Country.BE, Country.BE, 3.0056664783579006, -28.99635016359705, 3.0083318063162174, 0.0, -0.0, -0.0013326639791566564, Map.of("Loop Flow from FR", -0.0013326639791579886)), flowDecompositionResults.getDecomposedFlowMap().get(PST));
        assertEquals(new DecomposedFlow(X2, X1, Country.FR, Country.BE, 116.01617882330939, 115.97664177197838, 116.03559901755251, 0.0, 0.0, 0.0, Map.of("Loop Flow from BE", -0.009710097121567784, "Loop Flow from FR", -0.00971009712155535)), flowDecompositionResults.getDecomposedFlowMap().get(X2_WITH_X1_CONTINGENCY));
        assertEquals(new DecomposedFlow(PST, X1, Country.BE, Country.BE, 31.99999999999722, -0.0, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Collections.emptyMap()), flowDecompositionResults.getDecomposedFlowMap().get(PST_WITH_X1_CONTINGENCY));
        assertEquals(new DecomposedFlow(X2, N2_CONTINGENCY_ID, Country.FR, Country.BE, 100.03453149519564, 99.9826329237988, 116.04796053761832, 0.0, 0.0, 0.0, Map.of("Loop Flow from BE", -8.006714521211336, "Loop Flow from FR", -8.006714521211343)), flowDecompositionResults.getDecomposedFlowMap().get(X2_WITH_N2_CONTINGENCY));
    }

    @Test
    void testIntegrationWithoutRescalingWithLossCompensation() {
        FlowDecompositionParameters parameters = new FlowDecompositionParameters()
            .setEnableLossesCompensation(FlowDecompositionParameters.ENABLE_LOSSES_COMPENSATION)
            .setLossesCompensationEpsilon(FlowDecompositionParameters.DISABLE_LOSSES_COMPENSATION_EPSILON)
            .setRescaleEnabled(FlowDecompositionParameters.DISABLE_RESCALED_RESULTS);
        FlowDecompositionResults flowDecompositionResults = getFlowDecompositionResultsOnPSTNetwork(parameters);
        assertEquals(new DecomposedFlow(X1, "", Country.FR, Country.BE, 29.003009422979176, 28.996350163597047, 28.999015491555372, 0.0, -0.0, 0.0, Map.of("Loop Flow from BE", -0.0013326639791566564, "Loop Flow from FR", -0.0013326639791579886)), flowDecompositionResults.getDecomposedFlowMap().get(X1));
        assertEquals(new DecomposedFlow(X2, "", Country.FR, Country.BE, 87.00911182341697, 86.98905049079114, 86.99704647466612, 0.0, 0.0, 0.0, Map.of("Loop Flow from BE", -0.003997991937470857, "Loop Flow from FR", -0.003997991937473966)), flowDecompositionResults.getDecomposedFlowMap().get(X2));
        assertEquals(new DecomposedFlow(PST, "", Country.BE, Country.BE, 3.0056664783579006, -28.99635016359705, 28.999015491555372, 0.0, -0.0, -0.0013326639791566564, Map.of("Loop Flow from FR", -0.0013326639791579886)), flowDecompositionResults.getDecomposedFlowMap().get(PST));
        assertEquals(new DecomposedFlow(X2, X1, Country.FR, Country.BE, 116.01617882330939, 115.97664177197838, 115.99606196622149, 0.0, 0.0, 0.0, Map.of("Loop Flow from BE", -0.009710097121567784, "Loop Flow from FR", -0.00971009712155535)), flowDecompositionResults.getDecomposedFlowMap().get(X2_WITH_X1_CONTINGENCY));
        assertEquals(new DecomposedFlow(PST, X1, Country.BE, Country.BE, 31.99999999999722, -0.0, 0.0, 0.0, 0.0, -0.0, Collections.emptyMap()), flowDecompositionResults.getDecomposedFlowMap().get(PST_WITH_X1_CONTINGENCY));
        assertEquals(new DecomposedFlow(X2, N2_CONTINGENCY_ID, Country.FR, Country.BE, 100.03453149519564, 99.9826329237988, 115.99606196622149, 0.0, 0.0, 0.0, Map.of("Loop Flow from BE", -8.006714521211336, "Loop Flow from FR", -8.006714521211343)), flowDecompositionResults.getDecomposedFlowMap().get(X2_WITH_N2_CONTINGENCY));
    }

    private static FlowDecompositionResults getFlowDecompositionResultsOnPSTNetwork(FlowDecompositionParameters parameters) {
        String networkFileName = "NETWORK_PST_FLOW_WITH_COUNTRIES.uct";
        Network network = TestUtils.importNetwork(networkFileName);
        FlowDecompositionComputer flowComputer = new FlowDecompositionComputer(parameters);

        XnecProviderByIds xnecProviderByIds = XnecProviderByIds.builder()
            .addContingency(X1, Set.of(X1))
            .addNetworkElementsAfterContingencies(Set.of(X1, X2, PST), Set.of(X1))
            .addContingency(N2_CONTINGENCY_ID, Set.of(X1, PST))
            .addNetworkElementsAfterContingencies(Set.of(X1, X2, PST), Set.of(N2_CONTINGENCY_ID))
            .build();
        XnecProviderInterconnection xnecProviderInterconnection = new XnecProviderInterconnection();
        XnecProviderAllBranches xnecProviderAllBranches = new XnecProviderAllBranches();
        XnecProvider xnecProvider = new XnecProviderUnion(List.of(xnecProviderByIds, xnecProviderAllBranches, xnecProviderInterconnection));

        FlowDecompositionResults flowDecompositionResults = flowComputer.run(xnecProvider, network);

        assertEquals(6, flowDecompositionResults.getDecomposedFlowMap().size());
        assertTrue(flowDecompositionResults.getDecomposedFlowMap().containsKey(X1));
        assertTrue(flowDecompositionResults.getDecomposedFlowMap().containsKey(X2));
        assertTrue(flowDecompositionResults.getDecomposedFlowMap().containsKey(PST));
        assertTrue(flowDecompositionResults.getDecomposedFlowMap().containsKey(X2_WITH_X1_CONTINGENCY));
        assertTrue(flowDecompositionResults.getDecomposedFlowMap().containsKey(PST_WITH_X1_CONTINGENCY));
        assertTrue(flowDecompositionResults.getDecomposedFlowMap().containsKey(X2_WITH_N2_CONTINGENCY));
        return flowDecompositionResults;
    }
}
