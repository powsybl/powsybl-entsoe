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

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 */
class LoopFlowTests {
    private static final double EPSILON = 1e-3;

    @Test
    void checkThatLoopFlowsAreExtractedForEachXnecAndForEachCountryGivenABasicNetwork() {
        String networkFileName = "NETWORK_LOOP_FLOW_WITH_COUNTRIES.uct";
        String x1 = "EGEN  11 FGEN  11 1";
        String x2 = "FGEN  11 BGEN  11 1";
        String x4 = "BLOAD 11 FLOAD 11 1";
        String x5 = "FLOAD 11 ELOAD 11 1";

        Network network = TestUtils.importNetwork(networkFileName);
        XnecProvider xnecProvider = new XnecProviderByIds(List.of(x1, x2, x4, x5));
        FlowDecompositionComputer flowComputer = new FlowDecompositionComputer();
        FlowDecompositionResults flowDecompositionResults = flowComputer.run(xnecProvider, network);

        Map<String, DecomposedFlow> decomposedFlowMap = flowDecompositionResults.getDecomposedFlowMap();
        assertEquals(0, decomposedFlowMap.get(x1).getAllocatedFlow(), EPSILON);
        assertEquals(0, decomposedFlowMap.get(x2).getAllocatedFlow(), EPSILON);
        assertEquals(0, decomposedFlowMap.get(x4).getAllocatedFlow(), EPSILON);
        assertEquals(0, decomposedFlowMap.get(x5).getAllocatedFlow(), EPSILON);
        assertEquals(0, decomposedFlowMap.get(x1).getLoopFlow(Country.BE), EPSILON);
        assertEquals(100, decomposedFlowMap.get(x1).getLoopFlow(Country.ES), EPSILON);
        assertEquals(0, decomposedFlowMap.get(x1).getLoopFlow(Country.FR), EPSILON);
        assertEquals(0, decomposedFlowMap.get(x2).getLoopFlow(Country.BE), EPSILON);
        assertEquals(100, decomposedFlowMap.get(x2).getLoopFlow(Country.ES), EPSILON);
        assertEquals(100, decomposedFlowMap.get(x2).getLoopFlow(Country.FR), EPSILON);
        assertEquals(0, decomposedFlowMap.get(x4).getLoopFlow(Country.BE), EPSILON);
        assertEquals(100, decomposedFlowMap.get(x4).getLoopFlow(Country.ES), EPSILON);
        assertEquals(100, decomposedFlowMap.get(x4).getLoopFlow(Country.FR), EPSILON);
        assertEquals(0, decomposedFlowMap.get(x5).getLoopFlow(Country.BE), EPSILON);
        assertEquals(100, decomposedFlowMap.get(x5).getLoopFlow(Country.ES), EPSILON);
        assertEquals(0, decomposedFlowMap.get(x5).getLoopFlow(Country.FR), EPSILON);

        assertTrue(Double.isNaN(decomposedFlowMap.get(x1).getAcReferenceFlow()));
        assertTrue(Double.isNaN(decomposedFlowMap.get(x2).getAcReferenceFlow()));
        assertTrue(Double.isNaN(decomposedFlowMap.get(x4).getAcReferenceFlow()));
        assertTrue(Double.isNaN(decomposedFlowMap.get(x5).getAcReferenceFlow()));

        assertEquals(100, decomposedFlowMap.get(x1).getDcReferenceFlow(), EPSILON);
        assertEquals(200, decomposedFlowMap.get(x2).getDcReferenceFlow(), EPSILON);
        assertEquals(200, decomposedFlowMap.get(x4).getDcReferenceFlow(), EPSILON);
        assertEquals(100, decomposedFlowMap.get(x5).getDcReferenceFlow(), EPSILON);
    }
}
