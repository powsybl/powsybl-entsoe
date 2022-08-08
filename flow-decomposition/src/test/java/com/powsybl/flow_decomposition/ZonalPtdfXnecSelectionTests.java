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
        Network network = AllocatedFlowTests.importNetwork(NETWORK_FILE_NAME);

        FlowDecompositionResults flowDecompositionResults = getFlowDecompositionResultsNoZonalPtdf(network);

        assertTrue(flowDecompositionResults.getZonalPtdf().isPresent());
        assertTrue(flowDecompositionResults.getZonalPtdf().get().isEmpty());
    }

    @Test
    void testEnableZonalPtdfComputationWithNoHighPtdfLine() {
        Network network = AllocatedFlowTests.importNetwork(NETWORK_FILE_NAME);

        FlowDecompositionResults flowDecompositionResults = getFlowDecompositionResultsWithZonalPtdf(network);

        assertTrue(flowDecompositionResults.getZonalPtdf().isPresent());
        assertFalse(flowDecompositionResults.getZonalPtdf().get().isEmpty());
        assertEquals(2, flowDecompositionResults.getXnecsWithDecomposition().size());
    }

    @Test
    void testDisableZonalPtdfComputationWithHighPtdfLines() {
        Network network = getHighPtdfNetwork();

        FlowDecompositionResults flowDecompositionResults = getFlowDecompositionResultsNoZonalPtdf(network);

        assertTrue(flowDecompositionResults.getZonalPtdf().isPresent());
        assertTrue(flowDecompositionResults.getZonalPtdf().get().isEmpty());
        assertEquals(2, flowDecompositionResults.getXnecsWithDecomposition().size());
    }

    @Test
    void testEnableZonalPtdfComputationWithHighPtdfLines() {
        Network network = getHighPtdfNetwork();

        FlowDecompositionResults flowDecompositionResults = getFlowDecompositionResultsWithZonalPtdf(network);

        assertTrue(flowDecompositionResults.getZonalPtdf().isPresent());
        assertFalse(flowDecompositionResults.getZonalPtdf().get().isEmpty());
        assertEquals(2 + 10 - 1, flowDecompositionResults.getXnecsWithDecomposition().size());
    }

    private static Network getHighPtdfNetwork() {
        String line = "FGEN  11 FLOAD 11 A";
        Network network = AllocatedFlowTests.importNetwork(NETWORK_FILE_NAME);
        network.getLine(line).getTerminal1().disconnect();
        return network;
    }

    private static FlowDecompositionResults getFlowDecompositionResultsNoZonalPtdf(Network network) {
        return getFlowDecompositionResults(FlowDecompositionParameters.BranchSelectionStrategy.ONLY_INTERCONNECTIONS, network);
    }

    private static FlowDecompositionResults getFlowDecompositionResultsWithZonalPtdf(Network network) {
        return getFlowDecompositionResults(FlowDecompositionParameters.BranchSelectionStrategy.ZONE_TO_ZONE_PTDF_CRITERIA, network);
    }

    private static FlowDecompositionResults getFlowDecompositionResults(FlowDecompositionParameters.BranchSelectionStrategy branchSelectionStrategy, Network network) {
        FlowDecompositionParameters flowDecompositionParameters = new FlowDecompositionParameters();
        flowDecompositionParameters.setSaveIntermediates(FlowDecompositionParameters.SAVE_INTERMEDIATES);
        flowDecompositionParameters.setBranchSelectionStrategy(branchSelectionStrategy);
        FlowDecompositionComputer flowComputer = new FlowDecompositionComputer(flowDecompositionParameters);
        return flowComputer.run(network);
    }
}
