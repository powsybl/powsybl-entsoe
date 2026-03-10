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
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowParameters;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
class PexGraphTest {
    @Test
    void testGraphInjectionSummed() {
        Network testNetwork = TestUtils.importNetwork("testCase.xiidm");
        List<Bus> busesInMainSynchronousComponent = NetworkUtil.getBusesInMainSynchronousComponent(testNetwork);
        List<Branch<?>> branchesConnectedInMainSynchronousComponent = NetworkUtil.getAllValidBranches(testNetwork);
        // Test while summing injections
        PexGraph pexGraph = new PexGraph(busesInMainSynchronousComponent, branchesConnectedInMainSynchronousComponent);
        assertEquals(testNetwork.getBusView().getBusStream().count(), pexGraph.vertexSet().size());
        assertEquals(testNetwork.getBranchCount(), pexGraph.edgeSet().size());

        PexGraphVertex pexGraphVertex = pexGraph.vertexSet()
            .stream()
            .filter(vertex -> vertex.getId().equals("NNL2AA1_0"))
            .findAny().get();
        // load = 1000, gen = 500 --> if injection summed associated load = 500, associated gen = 0
        assertEquals(500., pexGraphVertex.getAssociatedLoad(), 0.);
        assertEquals(0., pexGraphVertex.getAssociatedGeneration(), 0.);

        PexGraphEdge pexGraphEdge = pexGraph.edgeSet()
            .stream()
            .filter(edge -> edge.getId().equals("BBE2AA1  FFR3AA1  1"))
            .findAny().get();
        assertEquals(324.6, pexGraphEdge.getAssociatedFlow(), 0.1);
    }

    @Test
    void testGraphInjectionDecomposed() {
        Network testNetwork = TestUtils.importNetwork("testCase.xiidm");
        List<Bus> busesInMainSynchronousComponent = NetworkUtil.getBusesInMainSynchronousComponent(testNetwork);
        List<Branch<?>> branchesConnectedInMainSynchronousComponent = NetworkUtil.getAllValidBranches(testNetwork);
        // Test while summing injections
        PexGraph pexGraph = new PexGraph(busesInMainSynchronousComponent, branchesConnectedInMainSynchronousComponent, PexGraph.InjectionStrategy.DECOMPOSE_INJECTIONS);
        assertEquals(testNetwork.getBusView().getBusStream().count(), pexGraph.vertexSet().size());
        assertEquals(testNetwork.getBranchCount(), pexGraph.edgeSet().size());

        PexGraphVertex pexGraphVertex = pexGraph.vertexSet()
            .stream()
            .filter(vertex -> vertex.getId().equals("NNL2AA1_0"))
            .findAny().get();
        // load = 1000, gen = 500
        assertEquals(1000., pexGraphVertex.getAssociatedLoad(), 0.);
        assertEquals(500., pexGraphVertex.getAssociatedGeneration(), 0.);

        PexGraphEdge pexGraphEdge = pexGraph.edgeSet()
            .stream()
            .filter(edge -> edge.getId().equals("BBE2AA1  FFR3AA1  1"))
            .findAny().get();
        assertEquals(324.6, pexGraphEdge.getAssociatedFlow(), 0.1);
    }

    @Test
    void testGraphInjectionSummedWithXNodeGen() throws IOException {
        Network testNetwork = TestUtils.importNetwork("TestCaseDangling.xiidm");
        LoadFlow.run(testNetwork, LoadFlowParameters.load().setDc(true));
        List<Bus> busesInMainSynchronousComponent = NetworkUtil.getBusesInMainSynchronousComponent(testNetwork);
        List<Branch<?>> branchesConnectedInMainSynchronousComponent = NetworkUtil.getAllValidBranches(testNetwork);
        // Test while summing injections
        PexGraph pexGraph = new PexGraph(busesInMainSynchronousComponent, branchesConnectedInMainSynchronousComponent);
        int nXnodes = NetworkUtil.getXNodeList(testNetwork).size();
        assertEquals(busesInMainSynchronousComponent.size() + nXnodes, pexGraph.vertexSet().size());
        assertEquals(testNetwork.getBranchCount() + nXnodes, pexGraph.edgeSet().size());

        PexGraphVertex pexGraphVertexGen = pexGraph.vertexSet()
            .stream()
            .filter(vertex -> vertex.getId().equals("BBE2AA1_0"))
            .findAny().get();
        // load = 1000, gen = 3000 --> if injection summed associated load = 0, associated gen = 2000
        assertEquals(0., pexGraphVertexGen.getAssociatedLoad(), 0.);
        assertEquals(2000., pexGraphVertexGen.getAssociatedGeneration(), 0.);

        PexGraphVertex pexGraphVertexLoad = pexGraph.vertexSet()
            .stream()
            .filter(vertex -> vertex.getId().equals("NNL2AA1_0"))
            .findAny().get();
        // load = 1000, gen = 500 --> if injection summed associated load = 500, associated gen = 0
        assertEquals(500., pexGraphVertexLoad.getAssociatedLoad(), 0.);
        assertEquals(0., pexGraphVertexLoad.getAssociatedGeneration(), 0.);

        PexGraphVertex pexGraphVertexXNode = pexGraph.vertexSet()
            .stream()
            .filter(vertex -> vertex.getId().equals("BBE2AA1  X_BEFR1  1"))
            .findAny().get();
        // load = 0, gen = 300 --> if injection summed associated load = 0, associated gen = 300
        assertEquals(0., pexGraphVertexXNode.getAssociatedLoad(), 0.);
        assertEquals(300., pexGraphVertexXNode.getAssociatedGeneration(), 0.);

        PexGraphEdge pexGraphEdge = pexGraph.edgeSet()
            .stream()
            .filter(edge -> edge.getId().equals("BBE2AA1  FFR3AA1  1"))
            .findAny().get();
        assertEquals(665, pexGraphEdge.getAssociatedFlow(), 0.1);

        PexGraphEdge pexGraphEdgeXNode = pexGraph.edgeSet()
            .stream()
            .filter(edge -> edge.getId().equals("BBE2AA1  X_BEFR1  1"))
            .findAny().get();
        assertEquals(300, pexGraphEdgeXNode.getAssociatedFlow(), 0.1);
    }

    @Test
    void testGraphInjectionSummedWithXNodeLoad() throws IOException {
        Network testNetwork = TestUtils.importNetwork("TestCaseDangling.xiidm");
        testNetwork.getDanglingLine("BBE2AA1  X_BEFR1  1").setP0(300);
        testNetwork.getGenerator("BBE2AA1 _generator").setTargetP(3600);
        LoadFlow.run(testNetwork, LoadFlowParameters.load().setDc(true));
        List<Bus> busesInMainSynchronousComponent = NetworkUtil.getBusesInMainSynchronousComponent(testNetwork);
        List<Branch<?>> branchesConnectedInMainSynchronousComponent = NetworkUtil.getAllValidBranches(testNetwork);
        // Test while summing injections
        PexGraph pexGraph = new PexGraph(busesInMainSynchronousComponent, branchesConnectedInMainSynchronousComponent);
        int nXnodes = NetworkUtil.getXNodeList(testNetwork).size();
        assertEquals(busesInMainSynchronousComponent.size() + nXnodes, pexGraph.vertexSet().size());
        assertEquals(testNetwork.getBranchCount() + nXnodes, pexGraph.edgeSet().size());

        PexGraphVertex pexGraphVertexGen = pexGraph.vertexSet()
            .stream()
            .filter(vertex -> vertex.getId().equals("BBE2AA1_0"))
            .findAny().get();
        // load = 1000, gen = 3600 --> if injection summed associated load = 0, associated gen = 2600
        assertEquals(0., pexGraphVertexGen.getAssociatedLoad(), 0.);
        assertEquals(2600., pexGraphVertexGen.getAssociatedGeneration(), 0.);

        PexGraphVertex pexGraphVertexLoad = pexGraph.vertexSet()
            .stream()
            .filter(vertex -> vertex.getId().equals("NNL2AA1_0"))
            .findAny().get();
        // load = 1000, gen = 500 --> if injection summed associated load = 500, associated gen = 0
        assertEquals(500., pexGraphVertexLoad.getAssociatedLoad(), 0.);
        assertEquals(0., pexGraphVertexLoad.getAssociatedGeneration(), 0.);

        PexGraphVertex pexGraphVertexXNode = pexGraph.vertexSet()
            .stream()
            .filter(vertex -> vertex.getId().equals("BBE2AA1  X_BEFR1  1"))
            .findAny().get();
        // load = 300, gen = 0 --> if injection summed associated load = 0, associated gen = 300
        assertEquals(300., pexGraphVertexXNode.getAssociatedLoad(), 0.);
        assertEquals(0., pexGraphVertexXNode.getAssociatedGeneration(), 0.);

        PexGraphEdge pexGraphEdge = pexGraph.edgeSet()
            .stream()
            .filter(edge -> edge.getId().equals("BBE2AA1  FFR3AA1  1"))
            .findAny().get();
        assertEquals(665, pexGraphEdge.getAssociatedFlow(), 0.1);

        PexGraphEdge pexGraphEdgeXNode = pexGraph.edgeSet()
            .stream()
            .filter(edge -> edge.getId().equals("BBE2AA1  X_BEFR1  1"))
            .findAny().get();
        assertEquals(300, pexGraphEdgeXNode.getAssociatedFlow(), 0.1);
    }

    @Test
    void testGraphInjectionDecomposedWithXNodes() {
        Network testNetwork = TestUtils.importNetwork("TestCaseDangling.xiidm");
        LoadFlow.run(testNetwork, LoadFlowParameters.load().setDc(true));
        List<Bus> busesInMainSynchronousComponent = NetworkUtil.getBusesInMainSynchronousComponent(testNetwork);
        List<Branch<?>> branchesConnectedInMainSynchronousComponent = NetworkUtil.getAllValidBranches(testNetwork);
        // Test while summing injections
        PexGraph pexGraph = new PexGraph(busesInMainSynchronousComponent, branchesConnectedInMainSynchronousComponent, PexGraph.InjectionStrategy.DECOMPOSE_INJECTIONS);
        int nXnodes = NetworkUtil.getXNodeList(testNetwork).size();
        assertEquals(busesInMainSynchronousComponent.size() + nXnodes, pexGraph.vertexSet().size());
        assertEquals(testNetwork.getBranchCount() + nXnodes, pexGraph.edgeSet().size());

        PexGraphVertex pexGraphVertexGen = pexGraph.vertexSet()
            .stream()
            .filter(vertex -> vertex.getId().equals("BBE2AA1_0"))
            .findAny().get();
        // load = 1000, gen = 3000
        assertEquals(1000., pexGraphVertexGen.getAssociatedLoad(), 0.);
        assertEquals(3000., pexGraphVertexGen.getAssociatedGeneration(), 0.);

        PexGraphVertex pexGraphVertexLoad = pexGraph.vertexSet()
            .stream()
            .filter(vertex -> vertex.getId().equals("NNL2AA1_0"))
            .findAny().get();
        // load = 1000, gen = 500
        assertEquals(1000., pexGraphVertexLoad.getAssociatedLoad(), 0.);
        assertEquals(500., pexGraphVertexLoad.getAssociatedGeneration(), 0.);

        PexGraphVertex pexGraphVertexXNode = pexGraph.vertexSet()
            .stream()
            .filter(vertex -> vertex.getId().equals("BBE2AA1  X_BEFR1  1"))
            .findAny().get();
        // load = 0, gen = 300 --> if injection summed associated load = 0, associated gen = 300
        assertEquals(0., pexGraphVertexXNode.getAssociatedLoad(), 0.);
        assertEquals(300., pexGraphVertexXNode.getAssociatedGeneration(), 0.);

        PexGraphEdge pexGraphEdge = pexGraph.edgeSet()
            .stream()
            .filter(edge -> edge.getId().equals("BBE2AA1  FFR3AA1  1"))
            .findAny().get();
        assertEquals(665, pexGraphEdge.getAssociatedFlow(), 0.1);

        PexGraphEdge pexGraphEdgeXNode = pexGraph.edgeSet()
            .stream()
            .filter(edge -> edge.getId().equals("BBE2AA1  X_BEFR1  1"))
            .findAny().get();
        assertEquals(300, pexGraphEdgeXNode.getAssociatedFlow(), 0.1);
    }

    @Test
    void testGraphInjectionSummedWithSimpleXNodes() {
        Network testNetwork = TestUtils.importNetwork("NETWORK_SINGLE_LOAD_TWO_GENERATORS_WITH_UNBOUNDED_XNODE.uct");
        LoadFlow.run(testNetwork, LoadFlowParameters.load().setDc(true));
        List<Bus> busesInMainSynchronousComponent = NetworkUtil.getBusesInMainSynchronousComponent(testNetwork);
        List<Branch<?>> branchesConnectedInMainSynchronousComponent = NetworkUtil.getAllValidBranches(testNetwork);
        // Test while summing injections
        PexGraph pexGraph = new PexGraph(busesInMainSynchronousComponent, branchesConnectedInMainSynchronousComponent);
        int nXnodes = NetworkUtil.getXNodeList(testNetwork).size();
        assertEquals(testNetwork.getBusView().getBusStream().count() + nXnodes, pexGraph.vertexSet().size());
        assertEquals(testNetwork.getBranchCount() + nXnodes, pexGraph.edgeSet().size());

        PexGraphVertex pexGraphVertex = pexGraph.vertexSet()
            .stream()
            .filter(vertex -> vertex.getId().equals("FGEN1 1_0"))
            .findAny().get();
        // load = 0, gen = 100 --> if injection summed associated load = 0, associated gen = 100
        assertEquals(0., pexGraphVertex.getAssociatedLoad(), 0.);
        assertEquals(100., pexGraphVertex.getAssociatedGeneration(), 0.);

        PexGraphVertex pexGraphVertexXNode = pexGraph.vertexSet()
            .stream()
            .filter(vertex -> vertex.getId().equals("BLOAD 11 X     11 1"))
            .findAny().get();
        // load = 100, gen = 0 --> if injection summed associated load = 100, associated gen = 0
        assertEquals(100., pexGraphVertexXNode.getAssociatedLoad(), 0.);
        assertEquals(0., pexGraphVertexXNode.getAssociatedGeneration(), 0.);

        PexGraphEdge pexGraphEdge = pexGraph.edgeSet()
            .stream()
            .filter(edge -> edge.getId().equals("FGEN1 11 BLOAD 11 1"))
            .findAny().get();
        assertEquals(100, pexGraphEdge.getAssociatedFlow(), 0.1);

        PexGraphEdge pexGraphEdgeXNode = pexGraph.edgeSet()
            .stream()
            .filter(edge -> edge.getId().equals("BLOAD 11 X     11 1"))
            .findAny().get();
        assertEquals(100, pexGraphEdgeXNode.getAssociatedFlow(), 0.1);
    }
}
