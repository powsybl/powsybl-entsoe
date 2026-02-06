/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.flow_decomposition.partitioners;

import com.powsybl.flow_decomposition.NetworkUtil;
import com.powsybl.flow_decomposition.TestUtils;
import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Bus;
import com.powsybl.iidm.network.Network;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
class PexGraphTest {
    private Network testNetwork;
    private List<Bus> busesInMainSynchronousComponent;
    private List<Branch> branchesConnectedInMainSynchronousComponent;

    @BeforeEach
    void setUp() {
        testNetwork = TestUtils.importNetwork("testCase.xiidm");
        busesInMainSynchronousComponent = testNetwork.getBusView().getBusStream()
                .filter(Bus::isInMainSynchronousComponent)
                .toList();
        branchesConnectedInMainSynchronousComponent = testNetwork.getBranchStream()
                .filter(NetworkUtil::isConnectedAndInMainSynchronousComponent)
                .toList();
    }

    @Test
    void testGraphInjectionSummed() {
        // Test while summing injections
        PexGraph pexGraph = new PexGraph(busesInMainSynchronousComponent, branchesConnectedInMainSynchronousComponent);
        assertEquals(testNetwork.getBusView().getBusStream().count(), pexGraph.vertexSet().size());
        assertEquals(testNetwork.getBranchCount(), pexGraph.edgeSet().size());

        PexGraphVertex pexGraphVertex = pexGraph.vertexSet()
                .stream()
                .filter(vertex -> vertex.getAssociatedBus().getId().equals("NNL2AA1_0"))
                .findAny().get();
        // load = 1000, gen = 500 --> if injection summed associated load = 500, associated gen = 0
        assertEquals(500., pexGraphVertex.getAssociatedLoad(), 0.);
        assertEquals(0., pexGraphVertex.getAssociatedGeneration(), 0.);

        PexGraphEdge pexGraphEdge = pexGraph.edgeSet()
                .stream()
                .filter(edge -> edge.getAssociatedBranch().getId().equals("BBE2AA1  FFR3AA1  1"))
                .findAny().get();
        assertEquals(324.6, pexGraphEdge.getAssociatedFlow(), 0.1);
    }

    @Test
    void testGraphInjectionDecomposed() {
        // Test while summing injections
        PexGraph pexGraph = new PexGraph(busesInMainSynchronousComponent, branchesConnectedInMainSynchronousComponent, PexGraph.InjectionStrategy.DECOMPOSE_INJECTIONS);
        assertEquals(testNetwork.getBusView().getBusStream().count(), pexGraph.vertexSet().size());
        assertEquals(testNetwork.getBranchCount(), pexGraph.edgeSet().size());

        PexGraphVertex pexGraphVertex = pexGraph.vertexSet()
                .stream()
                .filter(vertex -> vertex.getAssociatedBus().getId().equals("NNL2AA1_0"))
                .findAny().get();
        // load = 1000, gen = 500
        assertEquals(1000., pexGraphVertex.getAssociatedLoad(), 0.);
        assertEquals(500., pexGraphVertex.getAssociatedGeneration(), 0.);

        PexGraphEdge pexGraphEdge = pexGraph.edgeSet()
                .stream()
                .filter(edge -> edge.getAssociatedBranch().getId().equals("BBE2AA1  FFR3AA1  1"))
                .findAny().get();
        assertEquals(324.6, pexGraphEdge.getAssociatedFlow(), 0.1);
    }
}
