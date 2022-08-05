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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 */
class LoopFlowTests {
    private static final double EPSILON = 1e-3;

    @Test
    void checkThatLoopFlowsAreExtractedForEachXnecAndForEachCountryGivenABasicNetwork() {
        String networkFileName = "NETWORK_LOOP_FLOW_WITH_COUNTRIES.uct";

        String gBe = "BGEN  11_generator";
        String lBe = "BLOAD 11_load";
        String gFr = "FGEN  11_generator";
        String lFr = "FLOAD 11_load";
        String gEs = "EGEN  11_generator";
        String lEs = "ELOAD 11_load";

        String variantId = "InitialState";
        String x1 = Xnec.createId("EGEN  11 FGEN  11 1", variantId);
        String x2 = Xnec.createId("FGEN  11 BGEN  11 1", variantId);
        String x4 = Xnec.createId("BLOAD 11 FLOAD 11 1", variantId);
        String x5 = Xnec.createId("FLOAD 11 ELOAD 11 1", variantId);

        String allocated = "Allocated Flow";

        Network network = AllocatedFlowTests.importNetwork(networkFileName);
        FlowDecompositionParameters flowDecompositionParameters = new FlowDecompositionParameters();
        flowDecompositionParameters.setSaveIntermediates(FlowDecompositionParameters.SAVE_INTERMEDIATES);
        FlowDecompositionComputer flowComputer = new FlowDecompositionComputer(flowDecompositionParameters);
        FlowDecompositionResults flowDecompositionResults = flowComputer.run(network);

        var optionalGlsks = flowDecompositionResults.getGlsks();
        assertTrue(optionalGlsks.isPresent());
        var glsks = optionalGlsks.get();
        assertEquals(1.0, glsks.get(Country.BE).get(gBe), EPSILON);
        assertEquals(1.0, glsks.get(Country.ES).get(gEs), EPSILON);
        assertEquals(1.0, glsks.get(Country.FR).get(gFr), EPSILON);

        var optionalPtdfs = flowDecompositionResults.getNodalPtdf();
        assertTrue(optionalPtdfs.isPresent());

        var optionalReferenceNodalInjections = flowDecompositionResults.getDcNodalInjections();
        assertTrue(optionalReferenceNodalInjections.isPresent());
        var referenceNodalInjections = optionalReferenceNodalInjections.get().get(variantId);
        assertEquals(100, referenceNodalInjections.get(gBe));
        assertEquals(100, referenceNodalInjections.get(gEs));
        assertEquals(100, referenceNodalInjections.get(gFr));
        assertEquals(-100, referenceNodalInjections.get(lBe));
        assertEquals(-100, referenceNodalInjections.get(lEs));
        assertEquals(-100, referenceNodalInjections.get(lFr));

        var optionalNodalInjections = flowDecompositionResults.getNodalInjections();
        assertTrue(optionalNodalInjections.isPresent());
        var nodalInjections = optionalNodalInjections.get();
        assertEquals(0, nodalInjections.get(variantId).get(gBe).get(allocated), EPSILON);
        assertEquals(0, nodalInjections.get(variantId).get(gEs).get(allocated), EPSILON);
        assertEquals(0, nodalInjections.get(variantId).get(gFr).get(allocated), EPSILON);
        assertEquals(0, nodalInjections.get(variantId).get(lBe).get(allocated), EPSILON);
        assertEquals(0, nodalInjections.get(variantId).get(lEs).get(allocated), EPSILON);
        assertEquals(0, nodalInjections.get(variantId).get(lFr).get(allocated), EPSILON);
        assertEquals(100., nodalInjections.get(variantId).get(gBe).get(NetworkUtil.getLoopFlowIdFromCountry(Country.BE)), EPSILON);
        assertEquals(100., nodalInjections.get(variantId).get(gEs).get(NetworkUtil.getLoopFlowIdFromCountry(Country.ES)), EPSILON);
        assertEquals(100., nodalInjections.get(variantId).get(gFr).get(NetworkUtil.getLoopFlowIdFromCountry(Country.FR)), EPSILON);
        assertEquals(-100, nodalInjections.get(variantId).get(lBe).get(NetworkUtil.getLoopFlowIdFromCountry(Country.BE)), EPSILON);
        assertEquals(-100, nodalInjections.get(variantId).get(lEs).get(NetworkUtil.getLoopFlowIdFromCountry(Country.ES)), EPSILON);
        assertEquals(-100, nodalInjections.get(variantId).get(lFr).get(NetworkUtil.getLoopFlowIdFromCountry(Country.FR)), EPSILON);

        assertEquals(0, flowDecompositionResults.get(x1).getAllocatedFlow(), EPSILON);
        assertEquals(0, flowDecompositionResults.get(x2).getAllocatedFlow(), EPSILON);
        assertEquals(0, flowDecompositionResults.get(x4).getAllocatedFlow(), EPSILON);
        assertEquals(0, flowDecompositionResults.get(x5).getAllocatedFlow(), EPSILON);
        assertEquals(0, flowDecompositionResults.get(x1).getLoopFlow(Country.BE), EPSILON);
        assertEquals(100, flowDecompositionResults.get(x1).getLoopFlow(Country.ES), EPSILON);
        assertEquals(0, flowDecompositionResults.get(x1).getLoopFlow(Country.FR), EPSILON);
        assertEquals(0, flowDecompositionResults.get(x2).getLoopFlow(Country.BE), EPSILON);
        assertEquals(100, flowDecompositionResults.get(x2).getLoopFlow(Country.ES), EPSILON);
        assertEquals(100, flowDecompositionResults.get(x2).getLoopFlow(Country.FR), EPSILON);
        assertEquals(0, flowDecompositionResults.get(x4).getLoopFlow(Country.BE), EPSILON);
        assertEquals(100, flowDecompositionResults.get(x4).getLoopFlow(Country.ES), EPSILON);
        assertEquals(100, flowDecompositionResults.get(x4).getLoopFlow(Country.FR), EPSILON);
        assertEquals(0, flowDecompositionResults.get(x5).getLoopFlow(Country.BE), EPSILON);
        assertEquals(100, flowDecompositionResults.get(x5).getLoopFlow(Country.ES), EPSILON);
        assertEquals(0, flowDecompositionResults.get(x5).getLoopFlow(Country.FR), EPSILON);
    }
}
