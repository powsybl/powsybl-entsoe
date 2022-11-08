/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.flow_decomposition;

import com.powsybl.iidm.network.Network;

import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * This class contains helper functions for tests.
 *
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 */
public final class TestUtils {
    private static final double EPSILON = 1e-1;

    private TestUtils() {
    }

    static Network importNetwork(String networkResourcePath) {
        String networkName = Paths.get(networkResourcePath).getFileName().toString();
        return Network.read(networkName, TestUtils.class.getResourceAsStream(networkResourcePath));
    }

    public static void assertCoherenceTotalFlow(boolean enableRescaledResults, FlowDecompositionResults flowDecompositionResults) {
        for (String xnec : flowDecompositionResults.getDecomposedFlowMap().keySet()) {
            DecomposedFlow decomposedFlow = flowDecompositionResults.getDecomposedFlowMap().get(xnec);
            if (enableRescaledResults) {
                assertEquals(Math.abs(decomposedFlow.getAcReferenceFlow()), Math.abs(decomposedFlow.getTotalFlow()), EPSILON);
            } else {
                assertEquals(Math.abs(decomposedFlow.getDcReferenceFlow()), Math.abs(decomposedFlow.getTotalFlow()), EPSILON);
            }
        }
    }
}