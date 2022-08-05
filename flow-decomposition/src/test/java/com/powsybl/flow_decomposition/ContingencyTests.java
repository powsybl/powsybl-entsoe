/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.flow_decomposition;

import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.VariantManagerConstants;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 */
class ContingencyTests {
    private static final double EPSILON = 1e-3;

    @Test
    void onlyNStateTest() {
        String networkFileName = "NETWORK_SINGLE_LOAD_TWO_GENERATORS_WITH_COUNTRIES.uct";
        Network network = AllocatedFlowTests.importNetwork(networkFileName);

        FlowDecompositionParameters flowDecompositionParameters = new FlowDecompositionParameters()
            .setSaveIntermediates(FlowDecompositionParameters.SAVE_INTERMEDIATES)
            .setContingencyStrategy(FlowDecompositionParameters.ContingencyStrategy.ONLY_N_STATE);

        FlowDecompositionComputer flowDecompositionComputer = new FlowDecompositionComputer(flowDecompositionParameters);
        FlowDecompositionResults flowDecompositionResults = flowDecompositionComputer.run(network);

        RescalingTests.assertCoherenceTotalFlow(flowDecompositionParameters.isRescaleEnabled(), flowDecompositionResults);
        assertTrue(flowDecompositionResults.getContingencies().isPresent());
        assertEquals(0, flowDecompositionResults.getContingencies().get().size());
        assertEquals(1, flowDecompositionResults.getXnecsWithDecomposition().size());
        XnecWithDecomposition xnecWithDecomposition = flowDecompositionResults.get("FGEN1 11 BLOAD 11 1_InitialState");
        assertNotEquals(FlowDecompositionResults.DEFAULT_XNEC_WITH_DECOMPOSITION, xnecWithDecomposition);
        assertEquals(VariantManagerConstants.INITIAL_VARIANT_ID, xnecWithDecomposition.getVariantId());
        assertNull(xnecWithDecomposition.getContingency());
    }

    @Test
    void autoContingencyTest() {
        String networkFileName = "NETWORK_SINGLE_LOAD_TWO_GENERATORS_WITH_COUNTRIES.uct";
        Network network = AllocatedFlowTests.importNetwork(networkFileName);

        FlowDecompositionParameters flowDecompositionParameters = new FlowDecompositionParameters()
            .setSaveIntermediates(FlowDecompositionParameters.SAVE_INTERMEDIATES)
            .setContingencyStrategy(FlowDecompositionParameters.ContingencyStrategy.AUTO_CONTINGENCY);

        FlowDecompositionComputer flowDecompositionComputer = new FlowDecompositionComputer(flowDecompositionParameters);
        FlowDecompositionResults flowDecompositionResults = flowDecompositionComputer.run(network);

        RescalingTests.assertCoherenceTotalFlow(flowDecompositionParameters.isRescaleEnabled(), flowDecompositionResults);
        assertTrue(flowDecompositionResults.getContingencies().isPresent());
        assertEquals(2, flowDecompositionResults.getContingencies().get().size());
        String branchId1 = "FGEN1 11 BLOAD 11 1";
        String branchId2 = "BLOAD 11 BGEN2 11 1";
        assertEquals(branchId1, flowDecompositionResults.getContingencies().get().get(branchId1).getId());
        assertEquals(branchId2, flowDecompositionResults.getContingencies().get().get(branchId2).getId());
        assertEquals(2, flowDecompositionResults.getXnecsWithDecomposition().size());
        XnecWithDecomposition xnecWithDecomposition1 = flowDecompositionResults.get("FGEN1 11 BLOAD 11 1_InitialState");
        assertNotEquals(FlowDecompositionResults.DEFAULT_XNEC_WITH_DECOMPOSITION, xnecWithDecomposition1);
        assertEquals(VariantManagerConstants.INITIAL_VARIANT_ID, xnecWithDecomposition1.getVariantId());
        assertNull(xnecWithDecomposition1.getContingency());
        XnecWithDecomposition xnecWithDecomposition2 = flowDecompositionResults.get("FGEN1 11 BLOAD 11 1_BLOAD 11 BGEN2 11 1");
        assertNotEquals(FlowDecompositionResults.DEFAULT_XNEC_WITH_DECOMPOSITION, xnecWithDecomposition2);
        assertEquals("BLOAD 11 BGEN2 11 1", xnecWithDecomposition2.getVariantId());
        assertEquals("BLOAD 11 BGEN2 11 1", xnecWithDecomposition2.getContingency().getId());
    }

    @Test
    void autoContingencyWithLossCompensation() {
        String networkFileName = "NETWORK_SINGLE_LOAD_TWO_GENERATORS_WITH_COUNTRIES.uct";
        Network network = AllocatedFlowTests.importNetwork(networkFileName);

        FlowDecompositionParameters flowDecompositionParameters = new FlowDecompositionParameters()
            .setSaveIntermediates(FlowDecompositionParameters.SAVE_INTERMEDIATES)
            .setEnableLossesCompensation(FlowDecompositionParameters.ENABLE_LOSSES_COMPENSATION)
            .setContingencyStrategy(FlowDecompositionParameters.ContingencyStrategy.AUTO_CONTINGENCY);

        FlowDecompositionComputer flowDecompositionComputer = new FlowDecompositionComputer(flowDecompositionParameters);
        FlowDecompositionResults flowDecompositionResults = flowDecompositionComputer.run(network);

        assertTrue(flowDecompositionResults.getDcNodalInjections().isPresent());
        Map<String, Map<String, Double>> dcNodalInjections = flowDecompositionResults.getDcNodalInjections().get();
        String variantId1 = "InitialState";
        String variantId2 = "BLOAD 11 BGEN2 11 1";
        String lossesBusLBe1 = "LOSSES BLOAD 11";
        String lossesBusGFr1 = "LOSSES FGEN1 11";
        String lossesBusGBe2 = "LOSSES BGEN2 11";
        assertEquals(+0.0000, dcNodalInjections.get(variantId1).get(lossesBusLBe1), EPSILON);
        assertEquals(-0.0625, dcNodalInjections.get(variantId1).get(lossesBusGFr1), EPSILON);
        assertEquals(-0.0625, dcNodalInjections.get(variantId1).get(lossesBusGBe2), EPSILON);
        assertEquals(+0.0000, dcNodalInjections.get(variantId2).get(lossesBusLBe1), EPSILON);
        assertEquals(-0.2500, dcNodalInjections.get(variantId2).get(lossesBusGFr1), EPSILON);
    }
}
