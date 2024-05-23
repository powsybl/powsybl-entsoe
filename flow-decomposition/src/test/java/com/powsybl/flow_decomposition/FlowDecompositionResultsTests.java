/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.flow_decomposition;

import com.powsybl.flow_decomposition.xnec_provider.XnecProviderByIds;
import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import org.ejml.data.DMatrixSparseCSC;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.powsybl.flow_decomposition.DecomposedFlow.ALLOCATED_COLUMN_NAME;
import static com.powsybl.flow_decomposition.DecomposedFlow.PST_COLUMN_NAME;
import static com.powsybl.iidm.network.Country.BE;
import static com.powsybl.iidm.network.Country.DE;
import static com.powsybl.iidm.network.Country.FR;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 */
class FlowDecompositionResultsTests {
    private String branchId;
    private String contingencyId2;
    private String contingencyId3;
    private Network network;
    private XnecProviderByIds xnecProvider;
    private FlowDecompositionResults flowDecompositionResults;
    private DMatrixSparseCSC alloMatrix;
    private DMatrixSparseCSC pstMatrix;
    private Map<String, Integer> xnecMap;

    @BeforeEach
    void setup() {
        String networkFileName = "19700101_0000_FO4_UX1.uct";
        branchId = "DB000011 DF000011 1";
        String contingencyElementId1 = "FB000011 FD000011 1";
        String contingencyElementId2 = "FB000021 FD000021 1";
        contingencyId2 = "DD000011 DF000011 1";
        contingencyId3 = "FB000011 FD000011 1_FB000021 FD000021 1";

        network = TestUtils.importNetwork(networkFileName);
        xnecProvider = XnecProviderByIds.builder()
            .addContingencies(Map.of(contingencyId2, Set.of(contingencyId2), contingencyId3, Set.of(contingencyElementId1, contingencyElementId2)))
            .addNetworkElementsAfterContingencies(Set.of(branchId), Set.of(contingencyId2, contingencyId3))
            .addNetworkElementsOnBasecase(Set.of(branchId))
            .build();
        flowDecompositionResults = new FlowDecompositionResults(network);

        xnecMap = NetworkUtil.getIndex(List.of(branchId));
        alloMatrix = new DMatrixSparseCSC(1, 2);
        alloMatrix.set(0, 0, 20.0);
        alloMatrix.set(0, 1, 12.0);
        pstMatrix = new DMatrixSparseCSC(1, 1);
        pstMatrix.set(0, 0, 2);
    }

    @Test
    void testResultMetadata() {
        assertEquals(network.getNameOrId(), flowDecompositionResults.getNetworkId());
        assertTrue(flowDecompositionResults.getId().startsWith("Flow_Decomposition_Results_of_"));
        Set<Country> zoneSet = flowDecompositionResults.getZoneSet();
        assertEquals(3, zoneSet.size());
        assertTrue(zoneSet.contains(FR));
        assertTrue(zoneSet.contains(DE));
        assertTrue(zoneSet.contains(BE));
    }

    @Test
    void testBuilderNState() {
        Set<Branch> nStateXnecList = xnecProvider.getNetworkElements(network);
        FlowDecompositionResults.PerStateBuilder nStateBuilder = flowDecompositionResults.getBuilder(nStateXnecList);
        DecomposedFlowRescaler decomposedFlowRescaler = new DecomposedFlowRescalerNoOp();

        nStateBuilder.saveAcTerminal1ReferenceFlow(Map.of(branchId, 10.0));
        nStateBuilder.saveAcTerminal2ReferenceFlow(Map.of(branchId, 10.0));
        nStateBuilder.saveDcReferenceFlow(Map.of(branchId, 11.0));
        nStateBuilder.saveAllocatedAndLoopFlowsMatrix(new SparseMatrixWithIndexesCSC(xnecMap, Map.of(ALLOCATED_COLUMN_NAME, 0, NetworkUtil.getLoopFlowIdFromCountry(FR), 1), alloMatrix));
        nStateBuilder.savePstFlowMatrix(new SparseMatrixWithIndexesCSC(xnecMap, Map.of(PST_COLUMN_NAME, 0), pstMatrix));
        nStateBuilder.build(decomposedFlowRescaler);

        Map<String, DecomposedFlow> decomposedFlowMap = flowDecompositionResults.getDecomposedFlowMap();
        assertEquals(1, decomposedFlowMap.size());

        DecomposedFlow decomposedFlow = flowDecompositionResults.getDecomposedFlowMap().get(branchId);
        assertEquals(branchId, decomposedFlow.getBranchId());
        assertEquals("", decomposedFlow.getContingencyId());
        assertEquals(branchId, decomposedFlow.getId());
        assertEquals(10.0, decomposedFlow.getAcTerminal1ReferenceFlow());
        assertEquals(11.0, decomposedFlow.getDcReferenceFlow());
        assertEquals(20.0, decomposedFlow.getAllocatedFlow());
        assertEquals(12.0, decomposedFlow.getLoopFlow(FR));
        assertEquals(2.0, decomposedFlow.getPstFlow());
    }

    @Test
    void testBuilderN1State() {
        Set<Branch> n1StateContingency2XnecList = xnecProvider.getNetworkElements(contingencyId2, network);
        FlowDecompositionResults.PerStateBuilder n1StateBuilder = flowDecompositionResults.getBuilder(contingencyId2, n1StateContingency2XnecList);
        String xnecId = "DB000011 DF000011 1_DD000011 DF000011 1";
        DecomposedFlowRescaler decomposedFlowRescaler = new DecomposedFlowRescalerNoOp();

        n1StateBuilder.saveAcTerminal1ReferenceFlow(Map.of(branchId, 10.0));
        n1StateBuilder.saveAcTerminal2ReferenceFlow(Map.of(branchId, 10.0));
        n1StateBuilder.saveDcReferenceFlow(Map.of(branchId, 11.0));
        n1StateBuilder.saveAllocatedAndLoopFlowsMatrix(new SparseMatrixWithIndexesCSC(xnecMap, Map.of(ALLOCATED_COLUMN_NAME, 0, NetworkUtil.getLoopFlowIdFromCountry(FR), 1), alloMatrix));
        n1StateBuilder.savePstFlowMatrix(new SparseMatrixWithIndexesCSC(xnecMap, Map.of(PST_COLUMN_NAME, 0), pstMatrix));
        n1StateBuilder.build(decomposedFlowRescaler);

        Map<String, DecomposedFlow> decomposedFlowMap = flowDecompositionResults.getDecomposedFlowMap();
        assertEquals(1, decomposedFlowMap.size());

        DecomposedFlow decomposedFlow = flowDecompositionResults.getDecomposedFlowMap().get(xnecId);
        assertEquals(branchId, decomposedFlow.getBranchId());
        assertEquals(contingencyId2, decomposedFlow.getContingencyId());
        assertEquals(xnecId, decomposedFlow.getId());
        assertEquals(10.0, decomposedFlow.getAcTerminal1ReferenceFlow());
        assertEquals(11.0, decomposedFlow.getDcReferenceFlow());
        assertEquals(20.0, decomposedFlow.getAllocatedFlow());
        assertEquals(12.0, decomposedFlow.getLoopFlow(FR));
        assertEquals(2.0, decomposedFlow.getPstFlow());
    }

    @Test
    void testBuilderN2State() {
        Set<Branch> n1StateContingency3XnecList = xnecProvider.getNetworkElements(contingencyId3, network);
        FlowDecompositionResults.PerStateBuilder n2StateBuilder = flowDecompositionResults.getBuilder(contingencyId3, n1StateContingency3XnecList);
        String xnecId = "DB000011 DF000011 1_FB000011 FD000011 1_FB000021 FD000021 1";
        DecomposedFlowRescaler decomposedFlowRescaler = new DecomposedFlowRescalerNoOp();

        n2StateBuilder.saveAcTerminal1ReferenceFlow(Map.of(branchId, 10.0));
        n2StateBuilder.saveAcTerminal2ReferenceFlow(Map.of(branchId, 10.0));
        n2StateBuilder.saveDcReferenceFlow(Map.of(branchId, 11.0));
        n2StateBuilder.saveAllocatedAndLoopFlowsMatrix(new SparseMatrixWithIndexesCSC(xnecMap, Map.of(ALLOCATED_COLUMN_NAME, 0, NetworkUtil.getLoopFlowIdFromCountry(FR), 1), alloMatrix));
        n2StateBuilder.savePstFlowMatrix(new SparseMatrixWithIndexesCSC(xnecMap, Map.of(PST_COLUMN_NAME, 0), pstMatrix));
        n2StateBuilder.build(decomposedFlowRescaler);

        Map<String, DecomposedFlow> decomposedFlowMap = flowDecompositionResults.getDecomposedFlowMap();
        assertEquals(1, decomposedFlowMap.size());

        DecomposedFlow decomposedFlow = flowDecompositionResults.getDecomposedFlowMap().get(xnecId);
        assertEquals(branchId, decomposedFlow.getBranchId());
        assertEquals(contingencyId3, decomposedFlow.getContingencyId());
        assertEquals(xnecId, decomposedFlow.getId());
        assertEquals(10.0, decomposedFlow.getAcTerminal1ReferenceFlow());
        assertEquals(11.0, decomposedFlow.getDcReferenceFlow());
        assertEquals(20.0, decomposedFlow.getAllocatedFlow());
        assertEquals(12.0, decomposedFlow.getLoopFlow(FR));
        assertEquals(2.0, decomposedFlow.getPstFlow());
    }
}
