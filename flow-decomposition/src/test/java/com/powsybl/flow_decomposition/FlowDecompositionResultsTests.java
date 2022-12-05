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
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.powsybl.iidm.network.Country.BE;
import static com.powsybl.iidm.network.Country.DE;
import static com.powsybl.iidm.network.Country.FR;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 */
class FlowDecompositionResultsTests {
    @Test
    void testResultMetadata() {
        String networkFileName = "19700101_0000_FO4_UX1.uct";
        Network network = TestUtils.importNetwork(networkFileName);
        FlowDecompositionResults flowDecompositionResults = new FlowDecompositionResults(network);
        assertEquals(network.getNameOrId(), flowDecompositionResults.getNetworkId());
        assertTrue(flowDecompositionResults.getId().startsWith("Flow_Decomposition_Results_of_"));
        Set<Country> zoneSet = flowDecompositionResults.getZoneSet();
        assertEquals(3, zoneSet.size());
        assertTrue(zoneSet.contains(FR));
        assertTrue(zoneSet.contains(DE));
        assertTrue(zoneSet.contains(BE));
    }

    @Test
    void testBuilder() {
        String networkFileName = "19700101_0000_FO4_UX1.uct";
        String branchId = "DB000011 DF000011 1";
        String contingencyElementId1 = "FB000011 FD000011 1";
        String contingencyElementId2 = "FB000021 FD000021 1";
        String contingencyId2 = "DD000011 DF000011 1";
        String contingencyId3 = "FB000011 FD000011 1_FB000021 FD000021 1";

        Network network = TestUtils.importNetwork(networkFileName);
        XnecProviderByIds xnecProvider = XnecProviderByIds.builder()
            .addContingencies(Map.of(contingencyId2, Set.of(contingencyId2), contingencyId3, Set.of(contingencyElementId1, contingencyElementId2)))
            .addNetworkElementsAfterContingencies(Set.of(branchId), Set.of(contingencyId2, contingencyId3))
            .addNetworkElementsOnBasecase(Set.of(branchId))
            .build();
        FlowDecompositionResults flowDecompositionResults = new FlowDecompositionResults(network);
        List<Branch> nStateXnecList = xnecProvider.getNetworkElements(network);
        List<Branch> n1StateContingency2XnecList = xnecProvider.getNetworkElements(contingencyId2, network);
        List<Branch> n1StateContingency3XnecList = xnecProvider.getNetworkElements(contingencyId3, network);
        FlowDecompositionResults.PerStateBuilder nStateBuilder = flowDecompositionResults.getBuilder(nStateXnecList);
        FlowDecompositionResults.PerStateBuilder n1StateBuilderContingency2 = flowDecompositionResults.getBuilder(contingencyId2, n1StateContingency2XnecList);
        FlowDecompositionResults.PerStateBuilder n1StateBuilderContingency3 = flowDecompositionResults.getBuilder(contingencyId3, n1StateContingency3XnecList);

        FlowDecompositionResults.PerStateBuilder builder = nStateBuilder;
        builder.saveAcReferenceFlow(Map.of(branchId, 10.0));
        builder.saveDcReferenceFlow(Map.of(branchId, 11.0));
        DMatrixSparseCSC alloMatrix = new DMatrixSparseCSC(1, 2);
        alloMatrix.set(0, 0, 20.0);
        alloMatrix.set(0, 1, 12.0);
        Map<String, Integer> xnecMap = NetworkUtil.getIndex(List.of(branchId));
        builder.saveAllocatedAndLoopFlowsMatrix(new SparseMatrixWithIndexesCSC(xnecMap, Map.of(DecomposedFlow.ALLOCATED_COLUMN_NAME, 0, NetworkUtil.getLoopFlowIdFromCountry(FR), 1), alloMatrix));
        DMatrixSparseCSC pstMatrix = new DMatrixSparseCSC(1, 1);
        pstMatrix.set(0, 0, 2);
        builder.savePstFlowMatrix(new SparseMatrixWithIndexesCSC(xnecMap, Map.of(DecomposedFlow.PST_COLUMN_NAME, 0), pstMatrix));
        builder.build(FlowDecompositionParameters.DISABLE_RESCALED_RESULTS);
        Map<String, DecomposedFlow> decomposedFlowMap = flowDecompositionResults.getDecomposedFlowMap();
        assertEquals(1, decomposedFlowMap.size());
        DecomposedFlow decomposedFlow = flowDecompositionResults.getDecomposedFlowMap().get(branchId);
        assertEquals(10.0, decomposedFlow.getAcReferenceFlow());
        assertEquals(11.0, decomposedFlow.getDcReferenceFlow());
        assertEquals(20.0, decomposedFlow.getAllocatedFlow());
        assertEquals(12.0, decomposedFlow.getLoopFlow(FR));
        assertEquals(2.0, decomposedFlow.getPstFlow());
        //TODO
    }
}
