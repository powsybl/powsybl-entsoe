/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.flow_decomposition;

import com.powsybl.iidm.network.Network;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 */
class ZonalPtdfXnecSelectionTests {

    public static final String NETWORK_FILE_NAME = "NETWORK_PARALLEL_LINES_PTDF.uct";

    @Test
    void testDisableZonalPtdfComputationWithNoHighPtdfLine() {
        Network network = TestUtil.importNetwork(NETWORK_FILE_NAME);

        FlowDecompositionResults flowDecompositionResults = getFlowDecompositionResultsNoZonalPtdf(network);

        assertTrue(flowDecompositionResults.getZonalPtdfMap().isEmpty());
        assertEquals(2, flowDecompositionResults.getDecomposedFlowMap().size());
    }

    @Test
    void testEnableZonalPtdfComputationWithNoHighPtdfLine() {
        Network network = TestUtil.importNetwork(NETWORK_FILE_NAME);

        FlowDecompositionResults flowDecompositionResults = getFlowDecompositionResultsWithZonalPtdf(network);

        assertTrue(flowDecompositionResults.getZonalPtdfMap().isPresent());
        assertFalse(flowDecompositionResults.getZonalPtdfMap().get().isEmpty());
        assertEquals(2, flowDecompositionResults.getDecomposedFlowMap().size());
    }

    @Test
    void testDisableZonalPtdfComputationWithHighPtdfLines() {
        Network network = getHighPtdfNetwork();

        FlowDecompositionResults flowDecompositionResults = getFlowDecompositionResultsNoZonalPtdf(network);

        assertTrue(flowDecompositionResults.getZonalPtdfMap().isEmpty());
        assertEquals(2, flowDecompositionResults.getDecomposedFlowMap().size());
    }

    @Test
    void testEnableZonalPtdfComputationWithHighPtdfLines() {
        Network network = getHighPtdfNetwork();

        FlowDecompositionResults flowDecompositionResults = getFlowDecompositionResultsWithZonalPtdf(network);

        assertTrue(flowDecompositionResults.getZonalPtdfMap().isPresent());
        assertFalse(flowDecompositionResults.getZonalPtdfMap().get().isEmpty());
        assertEquals(2 + 10 - 1, flowDecompositionResults.getDecomposedFlowMap().size());
    }

    private static Network getHighPtdfNetwork() {
        String line = "FGEN  11 FLOAD 11 A";
        Network network = TestUtil.importNetwork(NETWORK_FILE_NAME);
        network.getLine(line).getTerminal1().disconnect();
        return network;
    }

    private static FlowDecompositionResults getFlowDecompositionResultsNoZonalPtdf(Network network) {
        return getFlowDecompositionResults(FlowDecompositionParameters.XnecSelectionStrategy.ONLY_INTERCONNECTIONS, network);
    }

    private static FlowDecompositionResults getFlowDecompositionResultsWithZonalPtdf(Network network) {
        return getFlowDecompositionResults(FlowDecompositionParameters.XnecSelectionStrategy.INTERCONNECTION_OR_ZONE_TO_ZONE_PTDF_GT_5PC, network);
    }

    private static FlowDecompositionResults getFlowDecompositionResults(FlowDecompositionParameters.XnecSelectionStrategy xnecSelectionStrategy, Network network) {
        FlowDecompositionParameters flowDecompositionParameters = new FlowDecompositionParameters();
        flowDecompositionParameters.setSaveIntermediates(FlowDecompositionParameters.SAVE_INTERMEDIATES);
        flowDecompositionParameters.setXnecSelectionStrategy(xnecSelectionStrategy);
        FlowDecompositionComputer flowComputer = new FlowDecompositionComputer(flowDecompositionParameters);
        return flowComputer.run(network);
    }
}
