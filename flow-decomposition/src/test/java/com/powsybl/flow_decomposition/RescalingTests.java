/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.flow_decomposition;

import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import org.apache.commons.math3.util.Pair;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 */
class RescalingTests {
    private static final double EPSILON = 1e-5;

    private void checkRescaleAcReference(double acReferenceFlow, double dcReferenceFlow, DecomposedFlow rescaledFlow, double expectedAllocatedFlow, double expectedPstFlow, double expectedLoopFlowBE, double expectedLoopFlowES) {
        double expectedLoopFlowFR = -300;
        double expectedLoopFlowGE = -100;
        assertEquals(Math.abs(acReferenceFlow), rescaledFlow.getTotalFlow(), EPSILON);
        assertEquals(expectedAllocatedFlow, rescaledFlow.getAllocatedFlow(), EPSILON);
        assertEquals(expectedPstFlow, rescaledFlow.getPstFlow(), EPSILON);
        assertEquals(expectedLoopFlowBE, rescaledFlow.getLoopFlow(Country.BE), EPSILON);
        assertEquals(expectedLoopFlowFR, rescaledFlow.getLoopFlow(Country.FR), EPSILON);
        assertEquals(expectedLoopFlowGE, rescaledFlow.getLoopFlow(Country.GE), EPSILON);
        assertEquals(expectedLoopFlowES, rescaledFlow.getLoopFlow(Country.ES), EPSILON);
        assertEquals(acReferenceFlow, rescaledFlow.getAcReferenceFlow(), EPSILON);
        assertEquals(dcReferenceFlow, rescaledFlow.getDcReferenceFlow(), EPSILON);
    }

    private void checkRescaleSmallerAcReference(double acReferenceFlow, double dcReferenceFlow, DecomposedFlow rescaledFlow) {
        checkRescaleAcReference(acReferenceFlow, dcReferenceFlow, rescaledFlow, 80, 160, 400, 560);
    }

    private void checkRescaleBiggerAcReference(double acReferenceFlow, double dcReferenceFlow, DecomposedFlow rescaledFlow) {
        checkRescaleAcReference(acReferenceFlow, dcReferenceFlow, rescaledFlow, 120, 240, 600, 840);
    }

    private DecomposedFlow getDecomposedFlow(double acReferenceFlow, double dcReferenceFlow) {
        Xnec xnec = null;
        Map<String, Double> loopFlows = new TreeMap<>();
        double allocatedFlow = 100;
        double pstFlow = 200.;
        loopFlows.put(NetworkUtil.getLoopFlowIdFromCountry(Country.BE), 500.);
        loopFlows.put(NetworkUtil.getLoopFlowIdFromCountry(Country.FR), -300.);
        loopFlows.put(NetworkUtil.getLoopFlowIdFromCountry(Country.GE), -100.);
        loopFlows.put(NetworkUtil.getLoopFlowIdFromCountry(Country.ES), 700.);
        Pair<Country, Country> countries = new Pair<>(Country.FR, Country.FR);
        return new DecomposedFlow(xnec, loopFlows, allocatedFlow, pstFlow, acReferenceFlow, dcReferenceFlow);
    }

    private DecomposedFlow getRescaledFlow(double acReferenceFlow, double dcReferenceFlow) {
        DecomposedFlow decomposedFlow = getDecomposedFlow(acReferenceFlow, dcReferenceFlow);
        assertEquals(Math.abs(dcReferenceFlow), decomposedFlow.getTotalFlow(), EPSILON);

        DecomposedFlowsRescaler rescaler = new DecomposedFlowsRescalerACER();
        return rescaler.rescale(decomposedFlow);
    }

    @Test
    void testAcerNormalizationWithPositiveBiggerReferenceFlows() {
        double acReferenceFlow = 1400.;
        double dcReferenceFlow = 1100.;
        DecomposedFlow rescaledFlow = getRescaledFlow(acReferenceFlow, dcReferenceFlow);
        checkRescaleBiggerAcReference(acReferenceFlow, dcReferenceFlow, rescaledFlow);
    }

    @Test
    void testAcerNormalizationWithNegativeAbsoluteBiggerReferenceFlows() {
        double acReferenceFlow = -1400.;
        double dcReferenceFlow = 1100.;
        DecomposedFlow rescaledFlow = getRescaledFlow(acReferenceFlow, dcReferenceFlow);
        checkRescaleBiggerAcReference(acReferenceFlow, dcReferenceFlow, rescaledFlow);
    }

    @Test
    void testAcerNormalizationWithPositiveSmallerReferenceFlows() {
        double acReferenceFlow = 800.;
        double dcReferenceFlow = 1100.;
        DecomposedFlow rescaledFlow = getRescaledFlow(acReferenceFlow, dcReferenceFlow);
        checkRescaleSmallerAcReference(acReferenceFlow, dcReferenceFlow, rescaledFlow);
    }

    @Test
    void testAcerNormalizationWithNegativeAbsoluteSmallerReferenceFlows() {
        double acReferenceFlow = -800.;
        double dcReferenceFlow = 1100.;
        DecomposedFlow rescaledFlow = getRescaledFlow(acReferenceFlow, dcReferenceFlow);
        checkRescaleSmallerAcReference(acReferenceFlow, dcReferenceFlow, rescaledFlow);
    }

    @Test
    void testAcerNormalizationWithNegativeBiggerReferenceFlows() {
        double acReferenceFlow = -1400.;
        double dcReferenceFlow = -1100.;
        DecomposedFlow rescaledFlow = getRescaledFlow(acReferenceFlow, dcReferenceFlow);
        checkRescaleBiggerAcReference(acReferenceFlow, dcReferenceFlow, rescaledFlow);
    }

    @Test
    void testAcerNormalizationWithPositiveAbsoluteBiggerReferenceFlows() {
        double acReferenceFlow = 1400.;
        double dcReferenceFlow = -1100.;
        DecomposedFlow rescaledFlow = getRescaledFlow(acReferenceFlow, dcReferenceFlow);
        checkRescaleBiggerAcReference(acReferenceFlow, dcReferenceFlow, rescaledFlow);
    }

    @Test
    void testAcerNormalizationWithNegativeSmallerReferenceFlows() {
        double acReferenceFlow = -800.;
        double dcReferenceFlow = -1100.;
        DecomposedFlow rescaledFlow = getRescaledFlow(acReferenceFlow, dcReferenceFlow);
        checkRescaleSmallerAcReference(acReferenceFlow, dcReferenceFlow, rescaledFlow);
    }

    @Test
    void testAcerNormalizationWithPositiveAbsoluteSmallerReferenceFlows() {
        double acReferenceFlow = 800.;
        double dcReferenceFlow = -1100.;
        DecomposedFlow rescaledFlow = getRescaledFlow(acReferenceFlow, dcReferenceFlow);
        checkRescaleSmallerAcReference(acReferenceFlow, dcReferenceFlow, rescaledFlow);
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

        assertCoherenceTotalFlow(enableRescaledResults, flowDecompositionResults);
    }

    static void assertCoherenceTotalFlow(boolean enableRescaledResults, FlowDecompositionResults flowDecompositionResults) {
        for (XnecWithDecomposition xnec : flowDecompositionResults.getXnecsWithDecomposition()) {
            DecomposedFlow decomposedFlow = xnec.getDecomposedFlowBeforeRescaling();
            assertEquals(Math.abs(decomposedFlow.getDcReferenceFlow()), Math.abs(decomposedFlow.getTotalFlow()), EPSILON);
            if (enableRescaledResults) {
                DecomposedFlow rescaledDecomposedFlow = xnec.getDecomposedFlow();
                assertEquals(Math.abs(rescaledDecomposedFlow.getAcReferenceFlow()), Math.abs(rescaledDecomposedFlow.getTotalFlow()), EPSILON);
            }
        }
    }

    @Test
    void testNormalizationNaNAcReferenceNetwork() {
        String networkFileName = "NETWORK_LOOP_FLOW_WITH_COUNTRIES.uct";
        Network network = AllocatedFlowTests.importNetwork(networkFileName);

        FlowDecompositionParameters flowDecompositionParameters = new FlowDecompositionParameters();
        flowDecompositionParameters.setEnableLossesCompensation(FlowDecompositionParameters.ENABLE_LOSSES_COMPENSATION);
        flowDecompositionParameters.setLossesCompensationEpsilon(FlowDecompositionParameters.DISABLE_LOSSES_COMPENSATION_EPSILON);
        flowDecompositionParameters.setSensitivityEpsilon(FlowDecompositionParameters.DISABLE_SENSITIVITY_EPSILON);
        flowDecompositionParameters.setRescaleEnabled(FlowDecompositionParameters.ENABLE_RESCALED_RESULTS);

        FlowDecompositionComputer flowDecompositionComputer = new FlowDecompositionComputer(flowDecompositionParameters);
        FlowDecompositionResults flowDecompositionResults = flowDecompositionComputer.run(network);

        assertTrue(Double.isNaN(flowDecompositionResults.get("BLOAD 11 FLOAD 11 1_InitialState").getAcReferenceFlow()));
        assertTrue(Double.isFinite(flowDecompositionResults.get("BLOAD 11 FLOAD 11 1_InitialState").getAllocatedFlow()));
    }
}
