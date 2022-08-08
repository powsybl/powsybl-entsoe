/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.flow_decomposition;

import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.*;
import org.junit.jupiter.api.Test;

import java.nio.file.Paths;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 */
class AllocatedFlowTests {
    private static final double EPSILON = 1e-3;

    static Network importNetwork(String networkResourcePath) {
        String networkName = Paths.get(networkResourcePath).getFileName().toString();
        return Importers.loadNetwork(networkName, AllocatedFlowTests.class.getResourceAsStream(networkResourcePath));
    }

    @Test
    void checkThatAllocatedFlowAreExtractedForEachXnecGivenABasicNetwork() {
        String networkFileName = "NETWORK_SINGLE_LOAD_TWO_GENERATORS_WITH_COUNTRIES.uct";
        String genBe = "BGEN2 11_generator";
        String loadBe = "BLOAD 11_load";
        String genFr = "FGEN1 11_generator";
        String variantId = "InitialState";
        String xnecFrBee = Xnec.createId("FGEN1 11 BLOAD 11 1", variantId);
        String allocated = "Allocated Flow";

        Network network = importNetwork(networkFileName);
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

        assertEquals(100.0935, flowDecompositionResults.get(xnecFrBee).getAllocatedFlow(), EPSILON);

        var optionalGlsks = flowDecompositionResults.getGlsks();
        assertTrue(optionalGlsks.isPresent());
        var glsks = optionalGlsks.get();
        assertEquals(1.0, glsks.get(Country.FR).get(genFr), EPSILON);
        assertEquals(1.0, glsks.get(Country.BE).get(genBe), EPSILON);

        var optionalNetPositions = flowDecompositionResults.getAcNetPositions();
        assertTrue(optionalNetPositions.isPresent());
        var netPositions = optionalNetPositions.get().get(variantId);
        assertEquals(100.0935, netPositions.get(Country.FR), EPSILON);
        assertEquals(-100.0935, netPositions.get(Country.BE), EPSILON);

        var optionalPtdfs = flowDecompositionResults.getNodalPtdf();
        assertTrue(optionalPtdfs.isPresent());
        var ptdfs = optionalPtdfs.get();
        assertEquals(-0.5, ptdfs.get(xnecFrBee).get(loadBe), EPSILON);
        assertEquals(-0.5, ptdfs.get(xnecFrBee).get(genBe), EPSILON);
        assertEquals(+0.5, ptdfs.get(xnecFrBee).get(genFr), EPSILON);

        var optionalNodalInjections = flowDecompositionResults.getNodalInjections();
        assertTrue(optionalNodalInjections.isPresent());
        var nodalInjections = optionalNodalInjections.get();
        assertEquals(-100.0935, nodalInjections.get(variantId).get(genBe).get(allocated), EPSILON);
        assertEquals(+100.0935, nodalInjections.get(variantId).get(genFr).get(allocated), EPSILON);
    }

    @Test
    void checkThatAllocatedFlowAreExtractedForEachXnecGivenABasicNetworkWithInvertedConvention() {
        String networkFileName = "NETWORK_SINGLE_LOAD_TWO_GENERATORS_WITH_COUNTRIES_INVERTED.uct";
        String genBe = "BGEN2 11_generator";
        String loadBe = "BLOAD 11_load";
        String genFr = "FGEN1 11_generator";
        String variantId = "InitialState";
        String xnecFrBee = Xnec.createId("BLOAD 11 FGEN1 11 1", variantId);
        String allocated = "Allocated Flow";

        Network network = importNetwork(networkFileName);
        FlowDecompositionParameters flowDecompositionParameters = new FlowDecompositionParameters();
        flowDecompositionParameters.setSaveIntermediates(FlowDecompositionParameters.SAVE_INTERMEDIATES);
        FlowDecompositionComputer allocatedFlowComputer = new FlowDecompositionComputer(flowDecompositionParameters);
        FlowDecompositionResults flowDecompositionResults = allocatedFlowComputer.run(network);

        assertEquals(100.0935, flowDecompositionResults.get(xnecFrBee).getAllocatedFlow(), EPSILON);

        var optionalGlsks = flowDecompositionResults.getGlsks();
        assertTrue(optionalGlsks.isPresent());
        var glsks = optionalGlsks.get();
        assertEquals(1.0, glsks.get(Country.FR).get(genFr), EPSILON);
        assertEquals(1.0, glsks.get(Country.BE).get(genBe), EPSILON);

        var optionalNetPositions = flowDecompositionResults.getAcNetPositions();
        assertTrue(optionalNetPositions.isPresent());
        var netPositions = optionalNetPositions.get().get(variantId);
        assertEquals(100.0935, netPositions.get(Country.FR), EPSILON);
        assertEquals(-100.0935, netPositions.get(Country.BE), EPSILON);

        var optionalPtdfs = flowDecompositionResults.getNodalPtdf();
        assertTrue(optionalPtdfs.isPresent());
        var ptdfs = optionalPtdfs.get();
        assertEquals(-0.5, ptdfs.get(xnecFrBee).get(loadBe), EPSILON);
        assertEquals(-0.5, ptdfs.get(xnecFrBee).get(genBe), EPSILON);
        assertEquals(+0.5, ptdfs.get(xnecFrBee).get(genFr), EPSILON);

        var optionalNodalInjections = flowDecompositionResults.getNodalInjections();
        assertTrue(optionalNodalInjections.isPresent());
        var nodalInjections = optionalNodalInjections.get();
        assertEquals(-100.0935, nodalInjections.get(variantId).get(genBe).get(allocated), EPSILON);
        assertEquals(+100.0935, nodalInjections.get(variantId).get(genFr).get(allocated), EPSILON);
    }

    @Test
    void testToStringDecomposedFlowTest() {
        String networkFileName = "NETWORK_SINGLE_LOAD_TWO_GENERATORS_WITH_COUNTRIES_INVERTED.uct";
        String variantId = "InitialState";
        String xnecFrBee = Xnec.createId("BLOAD 11 FGEN1 11 1", variantId);

        Network network = importNetwork(networkFileName);
        FlowDecompositionComputer allocatedFlowComputer = new FlowDecompositionComputer();
        FlowDecompositionResults flowDecompositionResults = allocatedFlowComputer.run(network);

        String str = "{xnec={xnec id=BLOAD 11 FGEN1 11 1_InitialState, branch=BLOAD 11 FGEN1 11 1, contingency=None, " +
            "network variant=InitialState, country of terminal 1=BE, country of terminal 2=FR}, " +
            "decomposed before rescaling={Allocated Flow=100.09348508206307, Loop Flow from BE=-0.0467425410315343, " +
            "Loop Flow from FR=-0.0467425410315343, PST Flow=0.0, Reference AC Flow=-100.06215218462131, " +
            "Reference DC Flow=-100.0}, decomposed after rescaling={Allocated Flow=100.09348508206307, " +
            "Loop Flow from BE=-0.0467425410315343, Loop Flow from FR=-0.0467425410315343, PST Flow=0.0, " +
            "Reference AC Flow=-100.06215218462131, Reference DC Flow=-100.0}}";
        assertEquals(str, flowDecompositionResults.get(xnecFrBee).toString());
    }
}
