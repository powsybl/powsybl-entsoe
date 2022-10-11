/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.flow_decomposition;

import com.powsybl.iidm.network.*;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 */
class AllocatedFlowTests {
    private static final double EPSILON = 1e-3;

    @Test
    void checkThatAllocatedFlowAreExtractedForEachXnecGivenABasicNetwork() {
        String networkFileName = "NETWORK_SINGLE_LOAD_TWO_GENERATORS_WITH_COUNTRIES.uct";
        String genBe = "BGEN2 11_generator";
        String loadBe = "BLOAD 11_load";
        String genFr = "FGEN1 11_generator";
        String xnecFrBee = "FGEN1 11 BLOAD 11 1";
        String allocated = "Allocated Flow";

        Network network = TestUtil.importNetwork(networkFileName);
        FlowDecompositionParameters flowDecompositionParameters = new FlowDecompositionParameters();
        flowDecompositionParameters.setSaveIntermediates(FlowDecompositionParameters.SAVE_INTERMEDIATES);
        FlowDecompositionComputer allocatedFlowComputer = new FlowDecompositionComputer(flowDecompositionParameters);
        FlowDecompositionResults flowDecompositionResults = allocatedFlowComputer.run(network);

        String networkId = flowDecompositionResults.getNetworkId();
        String expectedNetworkId = networkFileName.split(".uct")[0];
        assertEquals(expectedNetworkId, networkId);
        String id = flowDecompositionResults.getId();
        assertTrue(id.contains(expectedNetworkId));

        Set<Country> zones = flowDecompositionResults.getZoneSet();
        assertTrue(zones.contains(Country.FR));
        assertTrue(zones.contains(Country.BE));
        assertEquals(2, zones.size());

        Map<String, DecomposedFlow> decomposedFlowMap = flowDecompositionResults.getDecomposedFlowMap();
        assertEquals(100.0935, decomposedFlowMap.get(xnecFrBee).getAllocatedFlow(), EPSILON);

        var optionalPtdfs = flowDecompositionResults.getPtdfMap();
        assertTrue(optionalPtdfs.isPresent());
        var ptdfs = optionalPtdfs.get();
        assertEquals(-0.5, ptdfs.get(xnecFrBee).get(loadBe), EPSILON);
        assertEquals(-0.5, ptdfs.get(xnecFrBee).get(genBe), EPSILON);
        assertEquals(+0.5, ptdfs.get(xnecFrBee).get(genFr), EPSILON);

        var optionalNodalInjections = flowDecompositionResults.getAllocatedAndLoopFlowNodalInjectionsMap();
        assertTrue(optionalNodalInjections.isPresent());
        var nodalInjections = optionalNodalInjections.get();
        assertEquals(-100.0935, nodalInjections.get(genBe).get(allocated), EPSILON);
        assertEquals(+100.0935, nodalInjections.get(genFr).get(allocated), EPSILON);
    }

    @Test
    void checkThatAllocatedFlowAreExtractedForEachXnecGivenABasicNetworkWithInvertedConvention() {
        String networkFileName = "NETWORK_SINGLE_LOAD_TWO_GENERATORS_WITH_COUNTRIES_INVERTED.uct";
        String genBe = "BGEN2 11_generator";
        String loadBe = "BLOAD 11_load";
        String genFr = "FGEN1 11_generator";
        String xnecFrBee = "BLOAD 11 FGEN1 11 1";
        String allocated = "Allocated Flow";

        Network network = TestUtil.importNetwork(networkFileName);
        FlowDecompositionParameters flowDecompositionParameters = new FlowDecompositionParameters();
        flowDecompositionParameters.setSaveIntermediates(FlowDecompositionParameters.SAVE_INTERMEDIATES);
        FlowDecompositionComputer allocatedFlowComputer = new FlowDecompositionComputer(flowDecompositionParameters);
        FlowDecompositionResults flowDecompositionResults = allocatedFlowComputer.run(network);

        Map<String, DecomposedFlow> decomposedFlowMap = flowDecompositionResults.getDecomposedFlowMap();
        assertEquals(100.0935, decomposedFlowMap.get(xnecFrBee).getAllocatedFlow(), EPSILON);

        var optionalPtdfs = flowDecompositionResults.getPtdfMap();
        assertTrue(optionalPtdfs.isPresent());
        var ptdfs = optionalPtdfs.get();
        assertEquals(-0.5, ptdfs.get(xnecFrBee).get(loadBe), EPSILON);
        assertEquals(-0.5, ptdfs.get(xnecFrBee).get(genBe), EPSILON);
        assertEquals(+0.5, ptdfs.get(xnecFrBee).get(genFr), EPSILON);

        var optionalNodalInjections = flowDecompositionResults.getAllocatedAndLoopFlowNodalInjectionsMap();
        assertTrue(optionalNodalInjections.isPresent());
        var nodalInjections = optionalNodalInjections.get();
        assertEquals(-100.0935, nodalInjections.get(genBe).get(allocated), EPSILON);
        assertEquals(+100.0935, nodalInjections.get(genFr).get(allocated), EPSILON);
    }

    @Test
    @Deprecated
    void checkThatFlowDecompositionDoesNotExtractIntermediateResultsByDefault() {
        String networkFileName = "NETWORK_SINGLE_LOAD_TWO_GENERATORS_WITH_COUNTRIES.uct";
        Network network = TestUtil.importNetwork(networkFileName);
        FlowDecompositionComputer allocatedFlowComputer = new FlowDecompositionComputer();
        FlowDecompositionResults flowDecompositionResults = allocatedFlowComputer.run(network);
        assertTrue(flowDecompositionResults.getPtdfMap().isEmpty());
        assertTrue(flowDecompositionResults.getAllocatedAndLoopFlowNodalInjectionsMap().isEmpty());
    }

}
