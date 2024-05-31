/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.flow_decomposition;

import com.powsybl.flow_decomposition.xnec_provider.XnecProviderByIds;
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
        String xnecFrBee = "FGEN1 11 BLOAD 11 1";

        Network network = TestUtils.importNetwork(networkFileName);
        FlowDecompositionComputer allocatedFlowComputer = new FlowDecompositionComputer();
        XnecProvider xnecProvider = XnecProviderByIds.builder().addNetworkElementsOnBasecase(Set.of(xnecFrBee)).build();
        FlowDecompositionResults flowDecompositionResults = allocatedFlowComputer.run(xnecProvider, network);

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
        TestUtils.assertCoherenceTotalFlow(FlowDecompositionParameters.DEFAULT_RESCALE_MODE, flowDecompositionResults);
    }

    @Test
    void checkThatAllocatedFlowAreExtractedForEachXnecGivenABasicNetworkWithInvertedConvention() {
        String networkFileName = "NETWORK_SINGLE_LOAD_TWO_GENERATORS_WITH_COUNTRIES_INVERTED.uct";
        String xnecFrBee = "BLOAD 11 FGEN1 11 1";

        Network network = TestUtils.importNetwork(networkFileName);
        FlowDecompositionComputer allocatedFlowComputer = new FlowDecompositionComputer();
        XnecProvider xnecProvider = XnecProviderByIds.builder().addNetworkElementsOnBasecase(Set.of(xnecFrBee)).build();
        FlowDecompositionResults flowDecompositionResults = allocatedFlowComputer.run(xnecProvider, network);

        Map<String, DecomposedFlow> decomposedFlowMap = flowDecompositionResults.getDecomposedFlowMap();
        assertEquals(100.0935, decomposedFlowMap.get(xnecFrBee).getAllocatedFlow(), EPSILON);
        TestUtils.assertCoherenceTotalFlow(FlowDecompositionParameters.DEFAULT_RESCALE_MODE, flowDecompositionResults);
    }
}
