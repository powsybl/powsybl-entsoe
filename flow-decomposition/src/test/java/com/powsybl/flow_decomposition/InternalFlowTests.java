/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.flow_decomposition;

import com.powsybl.flow_decomposition.xnec_provider.XnecProviderAllBranches;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 */
class InternalFlowTests {

    public static final String NETWORK_FILE_NAME = "NETWORK_PARALLEL_LINES_PTDF.uct";
    private static final double EPSILON = 1e-3;
    public static final String X_INTERNAL_FR_FORMAT = "FGEN  11 FLOAD 11 %d";

    public static final String X_GFR_LBE = "FGEN  11 BLOAD 11 1";
    public static final String X_LFR_LBE = "FLOAD 11 BLOAD 11 1";

    @Test
    void testNetworkWithInternalFlow() {
        Network network = TestUtils.importNetwork(NETWORK_FILE_NAME);

        FlowDecompositionParameters flowDecompositionParameters = new FlowDecompositionParameters()
            .setEnableLossesCompensation(FlowDecompositionParameters.DISABLE_LOSSES_COMPENSATION);
        FlowDecompositionComputer flowComputer = new FlowDecompositionComputer(flowDecompositionParameters);
        XnecProvider xnecProvider = new XnecProviderAllBranches();
        FlowDecompositionResults flowDecompositionResults = flowComputer.run(xnecProvider, network);

        assertEquals(11, flowDecompositionResults.getDecomposedFlowMap().size());
        assertEquals(0.0, flowDecompositionResults.getDecomposedFlowMap().get(X_GFR_LBE).getInternalFlow());
        assertEquals(Country.FR, flowDecompositionResults.getDecomposedFlowMap().get(X_GFR_LBE).getCountry1());
        assertEquals(Country.BE, flowDecompositionResults.getDecomposedFlowMap().get(X_GFR_LBE).getCountry2());
        assertEquals(0.0, flowDecompositionResults.getDecomposedFlowMap().get(X_LFR_LBE).getInternalFlow());
        assertEquals(Country.FR, flowDecompositionResults.getDecomposedFlowMap().get(X_LFR_LBE).getCountry1());
        assertEquals(Country.BE, flowDecompositionResults.getDecomposedFlowMap().get(X_LFR_LBE).getCountry2());
        for (int i = 1; i < 10; i++) {
            String lineId = String.format(X_INTERNAL_FR_FORMAT, i);
            assertEquals(10., flowDecompositionResults.getDecomposedFlowMap().get(lineId).getInternalFlow(), EPSILON);
            assertEquals(Country.FR, flowDecompositionResults.getDecomposedFlowMap().get(lineId).getCountry1());
        }
    }

    @Test
    void testRescalingPositiveInternalFlow() {
        double internalFlow = 600.;
        double acReferenceFlow = 4100.;
        double dcReferenceFlow = 2000.;
        DecomposedFlow rescaledFlow = getRescaledFlow(internalFlow, acReferenceFlow, dcReferenceFlow);
        double expectedAllocatedFlow = 200.;
        double expectedInternalFlow = 1200.;
        double expectedPstFlow = 400.;
        double expectedLoopFlowBE = 1000.;
        double expectedLoopFlowES = 1400.;
        checkRescaleAcReference(acReferenceFlow, dcReferenceFlow, rescaledFlow, expectedAllocatedFlow, expectedInternalFlow, expectedPstFlow, expectedLoopFlowBE, expectedLoopFlowES);
    }

    @Test
    void testRescalingNegativeInternalFlow() {
        double internalFlow = -600.;
        double acReferenceFlow = 500.;
        double dcReferenceFlow = 800.;
        DecomposedFlow rescaledFlow = getRescaledFlow(internalFlow, acReferenceFlow, dcReferenceFlow);
        double expectedAllocatedFlow = 80.;
        double expectedInternalFlow = internalFlow;
        double expectedPstFlow = 160.;
        double expectedLoopFlowBE = 400.;
        double expectedLoopFlowES = 560.;
        checkRescaleAcReference(acReferenceFlow, dcReferenceFlow, rescaledFlow, expectedAllocatedFlow, expectedInternalFlow, expectedPstFlow, expectedLoopFlowBE, expectedLoopFlowES);

    }

    private DecomposedFlow getDecomposedFlow(double internalFlow, double acReferenceFlow, double dcReferenceFlow) {
        Map<String, Double> loopFlows = new TreeMap<>();
        double allocatedFlow = 100;
        double pstFlow = 200.;
        loopFlows.put(NetworkUtil.getLoopFlowIdFromCountry(Country.BE), 500.);
        loopFlows.put(NetworkUtil.getLoopFlowIdFromCountry(Country.GE), -100.);
        loopFlows.put(NetworkUtil.getLoopFlowIdFromCountry(Country.ES), 700.);
        Country country1 = Country.FR;
        Country country2 = Country.FR;
        return new DecomposedFlow(loopFlows, internalFlow, allocatedFlow, pstFlow, acReferenceFlow, dcReferenceFlow, country1, country2);
    }

    private DecomposedFlow getRescaledFlow(double internalFlow, double acReferenceFlow, double dcReferenceFlow) {
        DecomposedFlow decomposedFlow = getDecomposedFlow(internalFlow, acReferenceFlow, dcReferenceFlow);
        assertEquals(Math.abs(dcReferenceFlow), decomposedFlow.getTotalFlow(), EPSILON);

        return DecomposedFlowsRescaler.rescale(decomposedFlow);
    }

    private void checkRescaleAcReference(double acReferenceFlow, double dcReferenceFlow, DecomposedFlow rescaledFlow, double expectedAllocatedFlow, double expectedInternalFlow, double expectedPstFlow, double expectedLoopFlowBE, double expectedLoopFlowES) {
        double expectedLoopFlowGE = -100;
        assertEquals(Math.abs(acReferenceFlow), rescaledFlow.getTotalFlow(), EPSILON);
        assertEquals(expectedAllocatedFlow, rescaledFlow.getAllocatedFlow(), EPSILON);
        assertEquals(expectedPstFlow, rescaledFlow.getPstFlow(), EPSILON);
        assertEquals(expectedLoopFlowBE, rescaledFlow.getLoopFlow(Country.BE), EPSILON);
        assertEquals(expectedLoopFlowGE, rescaledFlow.getLoopFlow(Country.GE), EPSILON);
        assertEquals(expectedLoopFlowES, rescaledFlow.getLoopFlow(Country.ES), EPSILON);
        assertEquals(expectedInternalFlow, rescaledFlow.getInternalFlow(), EPSILON);
        assertEquals(acReferenceFlow, rescaledFlow.getAcReferenceFlow(), EPSILON);
        assertEquals(dcReferenceFlow, rescaledFlow.getDcReferenceFlow(), EPSILON);
    }
}
