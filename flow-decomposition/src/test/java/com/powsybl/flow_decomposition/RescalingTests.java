/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.flow_decomposition;

import com.powsybl.flow_decomposition.rescaler.DecomposedFlowRescalerAcerMethodology;
import com.powsybl.flow_decomposition.rescaler.DecomposedFlowRescalerMaxCurrentOverload;
import com.powsybl.flow_decomposition.rescaler.DecomposedFlowRescalerNoOp;
import com.powsybl.flow_decomposition.rescaler.DecomposedFlowRescalerProportional;
import com.powsybl.flow_decomposition.xnec_provider.XnecProviderAllBranches;
import com.powsybl.flow_decomposition.xnec_provider.XnecProviderByIds;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 * @author Caio Luke {@literal <caio.luke at artelys.com>}
 */
class RescalingTests {
    private static final double EPSILON = 1e-5;

    private void checkRescaleAcReference(double acReferenceFlow, double dcReferenceFlow, DecomposedFlow rescaledFlow, double expectedAllocatedFlow, double expectedPstFlow, double expectedLoopFlowBE, double expectedLoopFlowES) {
        double expectedInternalFlow = -300;
        double expectedLoopFlowGE = -100;
        assertEquals(Math.abs(acReferenceFlow), rescaledFlow.getTotalFlow(), EPSILON);
        assertEquals(expectedAllocatedFlow, rescaledFlow.getAllocatedFlow(), EPSILON);
        assertEquals(expectedPstFlow, rescaledFlow.getPstFlow(), EPSILON);
        assertEquals(expectedLoopFlowBE, rescaledFlow.getLoopFlow(Country.BE), EPSILON);
        assertEquals(expectedLoopFlowGE, rescaledFlow.getLoopFlow(Country.GE), EPSILON);
        assertEquals(expectedLoopFlowES, rescaledFlow.getLoopFlow(Country.ES), EPSILON);
        assertEquals(expectedInternalFlow, rescaledFlow.getInternalFlow(), EPSILON);
        assertEquals(acReferenceFlow, rescaledFlow.getAcTerminal1ReferenceFlow(), EPSILON);
        assertEquals(dcReferenceFlow, rescaledFlow.getDcReferenceFlow(), EPSILON);
    }

    private void checkRescaleSmallerAcReference(double acReferenceFlow, double dcReferenceFlow, DecomposedFlow rescaledFlow) {
        checkRescaleAcReference(acReferenceFlow, dcReferenceFlow, rescaledFlow, 80, 160, 400, 560);
    }

    private void checkRescaleBiggerAcReference(double acReferenceFlow, double dcReferenceFlow, DecomposedFlow rescaledFlow) {
        checkRescaleAcReference(acReferenceFlow, dcReferenceFlow, rescaledFlow, 120, 240, 600, 840);
    }

    private DecomposedFlow getDecomposedFlow(double acReferenceFlow, double dcReferenceFlow) {
        Map<String, Double> loopFlows = new TreeMap<>();
        double allocatedFlow = 100;
        double pstFlow = 200.;
        double internalFlow = -300.;
        loopFlows.put(NetworkUtil.getLoopFlowIdFromCountry(Country.BE), 500.);
        loopFlows.put(NetworkUtil.getLoopFlowIdFromCountry(Country.GE), -100.);
        loopFlows.put(NetworkUtil.getLoopFlowIdFromCountry(Country.ES), 700.);
        Country country1 = Country.FR;
        Country country2 = Country.FR;
        return new DecomposedFlowBuilder()
                .withBranchId("")
                .withContingencyId("")
                .withCountry1(country1)
                .withCountry2(country2)
                .withAcTerminal1ReferenceFlow(acReferenceFlow)
                .withAcTerminal2ReferenceFlow(acReferenceFlow)
                .withDcReferenceFlow(dcReferenceFlow)
                .withAllocatedFlow(allocatedFlow)
                .withXNodeFlow(0)
                .withPstFlow(pstFlow)
                .withInternalFlow(internalFlow)
                .withLoopFlowsMap(loopFlows)
                .build();
    }

    private DecomposedFlow getRescaledFlow(double acReferenceFlow, double dcReferenceFlow) {
        DecomposedFlow decomposedFlow = getDecomposedFlow(acReferenceFlow, dcReferenceFlow);
        assertEquals(Math.abs(dcReferenceFlow), decomposedFlow.getTotalFlow(), EPSILON);

        return new DecomposedFlowRescalerAcerMethodology().rescale(decomposedFlow);
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
    void testReLUNormalizationWithFlowDecompositionResultsWithPstNetwork() {
        String networkFileName = "NETWORK_PST_FLOW_WITH_COUNTRIES.uct";
        testNormalizationWithFlowDecompositionResults(networkFileName, FlowDecompositionParameters.RescaleMode.ACER_METHODOLOGY);
    }

    @Test
    void testNoNormalizationWithFlowDecompositionResultsWithPstNetwork() {
        String networkFileName = "NETWORK_PST_FLOW_WITH_COUNTRIES.uct";
        testNormalizationWithFlowDecompositionResults(networkFileName, FlowDecompositionParameters.RescaleMode.NONE);
    }

    @Test
    void testProportionalNormalizationWithFlowDecompositionResultsWithPstNetwork() {
        String networkFileName = "NETWORK_PST_FLOW_WITH_COUNTRIES.uct";
        testNormalizationWithFlowDecompositionResults(networkFileName, FlowDecompositionParameters.RescaleMode.PROPORTIONAL);
    }

    static void testNormalizationWithFlowDecompositionResults(String networkFileName, FlowDecompositionParameters.RescaleMode rescaleMode) {
        Network network = TestUtils.importNetwork(networkFileName);

        FlowDecompositionParameters flowDecompositionParameters = new FlowDecompositionParameters()
            .setEnableLossesCompensation(FlowDecompositionParameters.ENABLE_LOSSES_COMPENSATION)
            .setLossesCompensationEpsilon(FlowDecompositionParameters.DISABLE_LOSSES_COMPENSATION_EPSILON)
            .setSensitivityEpsilon(FlowDecompositionParameters.DISABLE_SENSITIVITY_EPSILON)
            .setRescaleMode(rescaleMode);

        FlowDecompositionComputer flowDecompositionComputer = new FlowDecompositionComputer(flowDecompositionParameters);
        XnecProvider xnecProvider = new XnecProviderAllBranches();
        FlowDecompositionResults flowDecompositionResults = flowDecompositionComputer.run(xnecProvider, network);

        TestUtils.assertCoherenceTotalFlow(rescaleMode, flowDecompositionResults);
    }

    @Test
    void testRescalingDoesNotOccurWhenAcDiverge() {
        String networkFileName = "NETWORK_LOOP_FLOW_WITH_COUNTRIES.uct";
        Network network = TestUtils.importNetwork(networkFileName);
        String xnecId = "BLOAD 11 FLOAD 11 1";

        FlowDecompositionParameters flowDecompositionParameters = new FlowDecompositionParameters()
            .setEnableLossesCompensation(FlowDecompositionParameters.ENABLE_LOSSES_COMPENSATION)
            .setLossesCompensationEpsilon(FlowDecompositionParameters.DISABLE_LOSSES_COMPENSATION_EPSILON)
            .setSensitivityEpsilon(FlowDecompositionParameters.DISABLE_SENSITIVITY_EPSILON)
            .setRescaleMode(FlowDecompositionParameters.RescaleMode.ACER_METHODOLOGY);

        FlowDecompositionComputer flowDecompositionComputer = new FlowDecompositionComputer(flowDecompositionParameters);
        XnecProvider xnecProvider = XnecProviderByIds.builder().addNetworkElementsOnBasecase(Set.of(xnecId)).build();
        FlowDecompositionResults flowDecompositionResults = flowDecompositionComputer.run(xnecProvider, network);

        assertTrue(Double.isNaN(flowDecompositionResults.getDecomposedFlowMap().get(xnecId).getAcTerminal1ReferenceFlow()));
        assertFalse(Double.isNaN(flowDecompositionResults.getDecomposedFlowMap().get(xnecId).getAllocatedFlow()));

    }

    @Test
    void testRescalingNoOpDoesNotRescale() {
        double acReferenceFlow = 1.0;
        double dcReferenceFlow = 0.9;
        DecomposedFlow decomposedFlow = getDecomposedFlow(acReferenceFlow, dcReferenceFlow);
        DecomposedFlow decomposedFlowRescaled = new DecomposedFlowRescalerNoOp().rescale(decomposedFlow);
        // check that same object is returned by rescaler
        assertSame(decomposedFlow, decomposedFlowRescaled);
    }

    @Test
    void testRescalingAcerMethodologyDoesNotRescaleNaN() {
        double acReferenceFlow = Double.NaN;
        double dcReferenceFlow = 0.9;
        DecomposedFlow decomposedFlow = getDecomposedFlow(acReferenceFlow, dcReferenceFlow);
        DecomposedFlow decomposedFlowRescaled = new DecomposedFlowRescalerAcerMethodology().rescale(decomposedFlow);
        // check that same object is returned by rescaler
        assertSame(decomposedFlow, decomposedFlowRescaled);
    }

    @Test
    void testRescalingProportionalDoesNotRescaleNaN() {
        double acReferenceFlow = Double.NaN;
        double dcReferenceFlow = 0.9;
        DecomposedFlow decomposedFlow = getDecomposedFlow(acReferenceFlow, dcReferenceFlow);
        DecomposedFlow decomposedFlowRescaled = new DecomposedFlowRescalerProportional().rescale(decomposedFlow);
        // check that same object is returned by rescaler
        assertSame(decomposedFlow, decomposedFlowRescaled);
    }

    @Test
    void testRescalingProportionalDoesNotRescaleWithSmallFlow() {
        double acReferenceFlow = 1.0;
        double dcReferenceFlow = 0.001;
        DecomposedFlow decomposedFlow = getDecomposedFlow(acReferenceFlow, dcReferenceFlow);
        DecomposedFlow decomposedFlowRescaled = new DecomposedFlowRescalerProportional(0.5).rescale(decomposedFlow);
        // check that same object is returned by rescaler
        assertSame(decomposedFlow, decomposedFlowRescaled);
    }

    @Test
    void testRescalingMaxCurrentOverloadDoesNotRescaleNaN() {
        double acReferenceFlow = Double.NaN;
        double dcReferenceFlow = 0.9;
        DecomposedFlow decomposedFlow = getDecomposedFlow(acReferenceFlow, dcReferenceFlow);
        DecomposedFlow decomposedFlowRescaled = new DecomposedFlowRescalerMaxCurrentOverload().rescale(decomposedFlow);
        // check that same object is returned by rescaler
        assertSame(decomposedFlow, decomposedFlowRescaled);
    }

    @Test
    void testRescalingMaxCurrentOverloadDoesNotRescaleWithSmallFlow() {
        double acReferenceFlow = 1.0;
        double dcReferenceFlow = 0.001;
        DecomposedFlow decomposedFlow = getDecomposedFlow(acReferenceFlow, dcReferenceFlow);
        DecomposedFlow decomposedFlowRescaled = new DecomposedFlowRescalerMaxCurrentOverload(0.5).rescale(decomposedFlow);
        // check that same object is returned by rescaler
        assertSame(decomposedFlow, decomposedFlowRescaled);
    }

    private DecomposedFlow getDecomposedFlowForMaxCurrentOverload(String branchId) {
        double acReferenceFlow1 = 100;
        double acReferenceFlow2 = 90;
        double dcReferenceFlow = 120;
        double acCurrentTerminal1 = 50;
        double acCurrentTerminal2 = 40;
        Map<String, Double> loopFlows = new TreeMap<>();
        double allocatedFlow = 100;
        double pstFlow = 200.;
        double internalFlow = -300.;
        loopFlows.put(NetworkUtil.getLoopFlowIdFromCountry(Country.BE), 500.);
        loopFlows.put(NetworkUtil.getLoopFlowIdFromCountry(Country.GE), -100.);
        loopFlows.put(NetworkUtil.getLoopFlowIdFromCountry(Country.ES), 700.);
        Country country1 = Country.FR;
        Country country2 = Country.FR;
        return new DecomposedFlowBuilder()
                .withBranchId(branchId)
                .withContingencyId("")
                .withCountry1(country1)
                .withCountry2(country2)
                .withAcTerminal1ReferenceFlow(acReferenceFlow1)
                .withAcTerminal2ReferenceFlow(acReferenceFlow2)
                .withDcReferenceFlow(dcReferenceFlow)
                .withAcCurrentTerminal1(acCurrentTerminal1)
                .withAcCurrentTerminal2(acCurrentTerminal2)
                .withAllocatedFlow(allocatedFlow)
                .withXNodeFlow(0)
                .withPstFlow(pstFlow)
                .withInternalFlow(internalFlow)
                .withLoopFlowsMap(loopFlows)
                .build();
    }

    @Test
    void testRescalingMaxCurrentOverloadWithCurrentLimits() {
        Network network = TestUtils.importNetwork("19700101_0000_FO4_UX1.uct");
        DecomposedFlow decomposedFlow = getDecomposedFlowForMaxCurrentOverload("BB000011 BD000011 1");
        DecomposedFlow decomposedFlowRescaled = new DecomposedFlowRescalerMaxCurrentOverload().rescale(decomposedFlow, network);
        final double expectedRescaledInternalFlow = -82.27241335952168;
        assertTrue(Math.abs(decomposedFlowRescaled.getInternalFlow() - expectedRescaledInternalFlow) < 1E-6);
    }

    @Test
    void testRescalingMaxCurrentOverloadWithoutCurrentLimits() {
        Network network = TestUtils.importNetwork("19700101_0000_FO4_UX1.uct");
        DecomposedFlow decomposedFlow = getDecomposedFlowForMaxCurrentOverload("BB000011 BB000021 1");
        DecomposedFlow decomposedFlowRescaled = new DecomposedFlowRescalerMaxCurrentOverload().rescale(decomposedFlow, network);
        final double expectedRescaledInternalFlow = -47.63139720814412;
        assertTrue(Math.abs(decomposedFlowRescaled.getInternalFlow() - expectedRescaledInternalFlow) < 1E-6);
    }
}
