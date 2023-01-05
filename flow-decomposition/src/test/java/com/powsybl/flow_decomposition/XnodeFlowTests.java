/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.flow_decomposition;

import com.powsybl.flow_decomposition.xnec_provider.XnecProviderByIds;
import com.powsybl.iidm.network.Network;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 */
class XnodeFlowTests {
    private static final double EPSILON = 1e-3;

    @Test
    void testXnodeFlowDecomposition() {
        String networkFileName = "19700101_0000_FO4_UX1.uct";
        String branchId1 = "XBF00021 BF000021 1 + XBF00021 FB000021 1";
        String branchId2 = "XBD00011 BD000011 1 + XBD00011 DB000011 1";
        String branchId3 = "XDF00011 DF000011 1 + XDF00011 FD000011 1";
        String branchId4 = "XBD00012 BD000011 1 + XBD00012 DB000011 1";
        String branchId5 = "XBF00011 BF000011 1 + XBF00011 FB000011 1";
        String branchId6 = "XBF00022 BF000021 1 + XBF00022 FB000022 1";

        Network network = TestUtils.importNetwork(networkFileName);
        XnecProvider xnecProvider = XnecProviderByIds.builder()
            .addNetworkElementsOnBasecase(Set.of(branchId1, branchId2, branchId3, branchId4, branchId5, branchId6))
            .build();
        FlowDecompositionParameters flowDecompositionParameters = FlowDecompositionParameters.load()
            .setEnableLossesCompensation(FlowDecompositionParameters.DISABLE_LOSSES_COMPENSATION)
            .setRescaleEnabled(FlowDecompositionParameters.DISABLE_RESCALED_RESULTS);
        FlowDecompositionComputer flowComputer = new FlowDecompositionComputer(flowDecompositionParameters);
        FlowDecompositionResults flowDecompositionResults = flowComputer.run(xnecProvider, network);
        TestUtils.assertCoherenceTotalFlow(flowDecompositionParameters.isRescaleEnabled(), flowDecompositionResults);

        Map<String, DecomposedFlow> decomposedFlowMap = flowDecompositionResults.getDecomposedFlowMap();
        assertEquals(44.109, decomposedFlowMap.get(branchId1).getXNodeFlow(), EPSILON);
        assertEquals(-33.155, decomposedFlowMap.get(branchId2).getXNodeFlow(), EPSILON);
        assertEquals(216.311, decomposedFlowMap.get(branchId3).getXNodeFlow(), EPSILON);
        assertEquals(-33.155, decomposedFlowMap.get(branchId4).getXNodeFlow(), EPSILON);
        assertEquals(170.472, decomposedFlowMap.get(branchId5).getXNodeFlow(), EPSILON);
        assertEquals(44.109, decomposedFlowMap.get(branchId6).getXNodeFlow(), EPSILON);
    }
}
