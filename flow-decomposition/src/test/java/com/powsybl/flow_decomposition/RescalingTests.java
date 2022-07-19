/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.flow_decomposition;

import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 */
class RescalingTests {
    private static final double EPSILON = 1e-5;

    @Test
    void testAcerNormalizationWithPositiveBiggerReferenceFlows() {
        Map<String, Double> loopFlows = new TreeMap<>();
        double allocatedFlow = 100;
        double pstFlow = 200.;
        loopFlows.put(NetworkUtil.getLoopFlowIdFromCountry(Country.BE), 500.);
        loopFlows.put(NetworkUtil.getLoopFlowIdFromCountry(Country.FR), -300.);
        loopFlows.put(NetworkUtil.getLoopFlowIdFromCountry(Country.GE), -100.);
        loopFlows.put(NetworkUtil.getLoopFlowIdFromCountry(Country.ES), 700.);
        double acReferenceFlow = 1400.;
        double dcReferenceFlow = 1100.;
        DecomposedFlow decomposedFlow = new DecomposedFlow(loopFlows, allocatedFlow, pstFlow, acReferenceFlow, dcReferenceFlow);
        assertEquals(dcReferenceFlow, decomposedFlow.getReferenceOrientedTotalFlow(), EPSILON);

        DecomposedFlowsRescaler rescaler = new DecomposedFlowsRescaler();
        DecomposedFlow rescaledFlow = rescaler.rescale(decomposedFlow);
        assertEquals(acReferenceFlow, rescaledFlow.getReferenceOrientedTotalFlow(), EPSILON);
        assertEquals(120, rescaledFlow.getAllocatedFlow(), EPSILON);
        assertEquals(240, rescaledFlow.getPstFlow(), EPSILON);
        assertEquals(600, rescaledFlow.getLoopFlow(Country.BE), EPSILON);
        assertEquals(-300, rescaledFlow.getLoopFlow(Country.FR), EPSILON);
        assertEquals(-100, rescaledFlow.getLoopFlow(Country.GE), EPSILON);
        assertEquals(840, rescaledFlow.getLoopFlow(Country.ES), EPSILON);
        assertEquals(acReferenceFlow, rescaledFlow.getAcReferenceFlow(), EPSILON);
        assertEquals(dcReferenceFlow, rescaledFlow.getDcReferenceFlow(), EPSILON);
    }

    @Test
    void testAcerNormalizationWithPositiveSmallerReferenceFlows() {
        Map<String, Double> loopFlows = new TreeMap<>();
        double allocatedFlow = 100;
        double pstFlow = 200.;
        loopFlows.put(NetworkUtil.getLoopFlowIdFromCountry(Country.BE), 500.);
        loopFlows.put(NetworkUtil.getLoopFlowIdFromCountry(Country.FR), -300.);
        loopFlows.put(NetworkUtil.getLoopFlowIdFromCountry(Country.GE), -100.);
        loopFlows.put(NetworkUtil.getLoopFlowIdFromCountry(Country.ES), 700.);
        double acReferenceFlow = 800.;
        double dcReferenceFlow = 1100.;
        DecomposedFlow decomposedFlow = new DecomposedFlow(loopFlows, allocatedFlow, pstFlow, acReferenceFlow, dcReferenceFlow);
        assertEquals(dcReferenceFlow, decomposedFlow.getReferenceOrientedTotalFlow(), EPSILON);

        DecomposedFlowsRescaler rescaler = new DecomposedFlowsRescaler();
        DecomposedFlow rescaledFlow = rescaler.rescale(decomposedFlow);
        assertEquals(acReferenceFlow, rescaledFlow.getReferenceOrientedTotalFlow(), EPSILON);
        assertEquals(80, rescaledFlow.getAllocatedFlow(), EPSILON);
        assertEquals(160, rescaledFlow.getPstFlow(), EPSILON);
        assertEquals(400, rescaledFlow.getLoopFlow(Country.BE), EPSILON);
        assertEquals(-300, rescaledFlow.getLoopFlow(Country.FR), EPSILON);
        assertEquals(-100, rescaledFlow.getLoopFlow(Country.GE), EPSILON);
        assertEquals(560, rescaledFlow.getLoopFlow(Country.ES), EPSILON);
        assertEquals(acReferenceFlow, rescaledFlow.getAcReferenceFlow(), EPSILON);
        assertEquals(dcReferenceFlow, rescaledFlow.getDcReferenceFlow(), EPSILON);
    }

    @Test
    void testAcerNormalizationWithNegativeBiggerReferenceFlows() {
        Map<String, Double> loopFlows = new TreeMap<>();
        double allocatedFlow = 100;
        double pstFlow = 200.;
        loopFlows.put(NetworkUtil.getLoopFlowIdFromCountry(Country.BE), 500.);
        loopFlows.put(NetworkUtil.getLoopFlowIdFromCountry(Country.FR), -300.);
        loopFlows.put(NetworkUtil.getLoopFlowIdFromCountry(Country.GE), -100.);
        loopFlows.put(NetworkUtil.getLoopFlowIdFromCountry(Country.ES), 700.);
        double acReferenceFlow = -1400.;
        double dcReferenceFlow = -1100.;
        DecomposedFlow decomposedFlow = new DecomposedFlow(loopFlows, allocatedFlow, pstFlow, acReferenceFlow, dcReferenceFlow);
        assertEquals(dcReferenceFlow, decomposedFlow.getReferenceOrientedTotalFlow(), EPSILON);

        DecomposedFlowsRescaler rescaler = new DecomposedFlowsRescaler();
        DecomposedFlow rescaledFlow = rescaler.rescale(decomposedFlow);
        assertEquals(acReferenceFlow, rescaledFlow.getReferenceOrientedTotalFlow(), EPSILON);
        assertEquals(120, rescaledFlow.getAllocatedFlow(), EPSILON);
        assertEquals(240, rescaledFlow.getPstFlow(), EPSILON);
        assertEquals(600, rescaledFlow.getLoopFlow(Country.BE), EPSILON);
        assertEquals(-300, rescaledFlow.getLoopFlow(Country.FR), EPSILON);
        assertEquals(-100, rescaledFlow.getLoopFlow(Country.GE), EPSILON);
        assertEquals(840, rescaledFlow.getLoopFlow(Country.ES), EPSILON);
        assertEquals(acReferenceFlow, rescaledFlow.getAcReferenceFlow(), EPSILON);
        assertEquals(dcReferenceFlow, rescaledFlow.getDcReferenceFlow(), EPSILON);
    }

    @Test
    void testAcerNormalizationWithNegativeSmallerReferenceFlows() {
        Map<String, Double> loopFlows = new TreeMap<>();
        double allocatedFlow = 100;
        double pstFlow = 200.;
        loopFlows.put(NetworkUtil.getLoopFlowIdFromCountry(Country.BE), 500.);
        loopFlows.put(NetworkUtil.getLoopFlowIdFromCountry(Country.FR), -300.);
        loopFlows.put(NetworkUtil.getLoopFlowIdFromCountry(Country.GE), -100.);
        loopFlows.put(NetworkUtil.getLoopFlowIdFromCountry(Country.ES), 700.);
        double acReferenceFlow = -800.;
        double dcReferenceFlow = -1100.;
        DecomposedFlow decomposedFlow = new DecomposedFlow(loopFlows, allocatedFlow, pstFlow, acReferenceFlow, dcReferenceFlow);
        assertEquals(dcReferenceFlow, decomposedFlow.getReferenceOrientedTotalFlow(), EPSILON);

        DecomposedFlowsRescaler rescaler = new DecomposedFlowsRescaler();
        DecomposedFlow rescaledFlow = rescaler.rescale(decomposedFlow);
        assertEquals(acReferenceFlow, rescaledFlow.getReferenceOrientedTotalFlow(), EPSILON);
        assertEquals(80, rescaledFlow.getAllocatedFlow(), EPSILON);
        assertEquals(160, rescaledFlow.getPstFlow(), EPSILON);
        assertEquals(400, rescaledFlow.getLoopFlow(Country.BE), EPSILON);
        assertEquals(-300, rescaledFlow.getLoopFlow(Country.FR), EPSILON);
        assertEquals(-100, rescaledFlow.getLoopFlow(Country.GE), EPSILON);
        assertEquals(560, rescaledFlow.getLoopFlow(Country.ES), EPSILON);
        assertEquals(acReferenceFlow, rescaledFlow.getAcReferenceFlow(), EPSILON);
        assertEquals(dcReferenceFlow, rescaledFlow.getDcReferenceFlow(), EPSILON);
    }

    @Test
    void testNormalizationWithFlowDecompositionResultsWithPstNetwork() {
        String networkFileName = "NETWORK_PST_FLOW_WITH_COUNTRIES.uct";
        testNormalizationWithFlowDecompositionResults(networkFileName, FlowDecompositionParameters.ENABLE_RESCALED_RESULTS);
    }

    @Test
    void testNoNormalizationWithFlowDecompositionResultsWithPstNetwork() {
        String networkFileName = "NETWORK_PST_FLOW_WITH_COUNTRIES.uct";
        testNormalizationWithFlowDecompositionResults(networkFileName, FlowDecompositionParameters.DISABLE_RESCALED_RESULTS);
    }

    static void testNormalizationWithFlowDecompositionResults(String networkFileName, boolean enableRescaledResults) {
        Network network = AllocatedFlowTests.importNetwork(networkFileName);

        FlowDecompositionParameters flowDecompositionParameters = new FlowDecompositionParameters();
        flowDecompositionParameters.setEnableLossesCompensation(FlowDecompositionParameters.ENABLE_LOSSES_COMPENSATION);
        flowDecompositionParameters.setLossesCompensationEpsilon(FlowDecompositionParameters.DISABLE_LOSSES_COMPENSATION_EPSILON);
        flowDecompositionParameters.setSensitivityEpsilon(FlowDecompositionParameters.DISABLE_SENSITIVITY_EPSILON);
        flowDecompositionParameters.setRescaleEnabled(enableRescaledResults);

        FlowDecompositionComputer flowDecompositionComputer = new FlowDecompositionComputer(flowDecompositionParameters);
        FlowDecompositionResults flowDecompositionResults = flowDecompositionComputer.run(network);

        for (String xnecId : flowDecompositionResults.getDecomposedFlowMap().keySet()) {
            DecomposedFlow decomposedFlow = flowDecompositionResults.getDecomposedFlowMapBeforeRescaling().get(xnecId);
            assertEquals(decomposedFlow.getDcReferenceFlow(), decomposedFlow.getReferenceOrientedTotalFlow(), EPSILON);
            if (enableRescaledResults) {
                DecomposedFlow rescaledDecomposedFlow = flowDecompositionResults.getDecomposedFlowMap().get(xnecId);
                assertEquals(rescaledDecomposedFlow.getAcReferenceFlow(), rescaledDecomposedFlow.getReferenceOrientedTotalFlow(), EPSILON);
            }
        }
    }
}
