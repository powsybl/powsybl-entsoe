/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.flow_decomposition;

import com.powsybl.iidm.network.Network;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 */
class PstFlowTests {
    private static final double EPSILON = 1e-3;

    @Test
    void checkThatPSTFlowsAreExtractedForEachXnecAndForEachPSTGivenABasicNetworkWithNeutralTap() {
        String networkFileName = "NETWORK_PST_FLOW_WITH_COUNTRIES.uct";
        String x1 = "FGEN  11 BLOAD 11 1";
        String x2 = "FGEN  11 BLOAD 12 1";

        Network network = TestUtils.importNetwork(networkFileName);
        FlowDecompositionComputer flowComputer = new FlowDecompositionComputer();
        FlowDecompositionResults flowDecompositionResults = flowComputer.run(network);

        Map<String, DecomposedFlow> decomposedFlowMap = flowDecompositionResults.getDecomposedFlowMap();
        assertEquals(0, decomposedFlowMap.get(x1).getPstFlow(), EPSILON);
        assertEquals(0, decomposedFlowMap.get(x2).getPstFlow(), EPSILON);
    }

    @Test
    void checkThatPSTFlowsAreExtractedForEachXnecAndForEachPSTGivenABasicNetworkWithNonNeutralTap() {
        String networkFileName = "NETWORK_PST_FLOW_WITH_COUNTRIES_NON_NEUTRAL.uct";
        String x1 = "FGEN  11 BLOAD 11 1";
        String x2 = "FGEN  11 BLOAD 12 1";

        Network network = TestUtils.importNetwork(networkFileName);
        FlowDecompositionComputer flowComputer = new FlowDecompositionComputer();
        FlowDecompositionResults flowDecompositionResults = flowComputer.run(network);

        Map<String, DecomposedFlow> decomposedFlowMap = flowDecompositionResults.getDecomposedFlowMap();
        assertEquals(163.652702605, decomposedFlowMap.get(x1).getPstFlow(), EPSILON);
        assertEquals(163.652702605, decomposedFlowMap.get(x2).getPstFlow(), EPSILON);
    }
}
