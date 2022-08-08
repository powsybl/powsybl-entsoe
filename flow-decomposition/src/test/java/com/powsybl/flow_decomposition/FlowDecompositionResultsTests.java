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
class FlowDecompositionResultsTests {
    @Test
    void checkThatFlowDecompositionDoesNotExtractIntermediateResultsByDefault() {
        String networkFileName = "NETWORK_SINGLE_LOAD_TWO_GENERATORS_WITH_COUNTRIES.uct";
        Network network = AllocatedFlowTests.importNetwork(networkFileName);
        FlowDecompositionComputer allocatedFlowComputer = new FlowDecompositionComputer();
        FlowDecompositionResults flowDecompositionResults = allocatedFlowComputer.run(network);
        assertFalse(flowDecompositionResults.getXnecsWithDecomposition().isEmpty());
        assertTrue(flowDecompositionResults.getContingencies().isEmpty());
        assertTrue(flowDecompositionResults.getAcReferenceFlows().isEmpty());
        assertTrue(flowDecompositionResults.getDcReferenceFlows().isEmpty());
        assertTrue(flowDecompositionResults.getAcNetPositions().isEmpty());
        assertTrue(flowDecompositionResults.getGlsks().isEmpty());
        assertTrue(flowDecompositionResults.getZonalPtdf().isEmpty());
        assertTrue(flowDecompositionResults.getNodalPtdf().isEmpty());
        assertTrue(flowDecompositionResults.getPsdf().isEmpty());
        assertTrue(flowDecompositionResults.getNodalInjections().isEmpty());
        assertTrue(flowDecompositionResults.getDcNodalInjections().isEmpty());
        assertTrue(flowDecompositionResults.getAllocatedAndLoopFlows().isEmpty());
        assertTrue(flowDecompositionResults.getPstFlows().isEmpty());
    }

    @Test
    void checkThatFlowDecompositionDoesExtractIntermediateResults() {
        String networkFileName = "NETWORK_SINGLE_LOAD_TWO_GENERATORS_WITH_COUNTRIES.uct";
        Network network = AllocatedFlowTests.importNetwork(networkFileName);
        FlowDecompositionParameters flowDecompositionParameters = new FlowDecompositionParameters()
            .setSaveIntermediates(FlowDecompositionParameters.SAVE_INTERMEDIATES);
        FlowDecompositionComputer flowDecompositionComputer = new FlowDecompositionComputer(flowDecompositionParameters);
        FlowDecompositionResults flowDecompositionResults = flowDecompositionComputer.run(network);
        assertFalse(flowDecompositionResults.getXnecsWithDecomposition().isEmpty());
        assertTrue(flowDecompositionResults.getContingencies().isPresent());
        assertTrue(flowDecompositionResults.getAcReferenceFlows().isPresent());
        assertTrue(flowDecompositionResults.getDcReferenceFlows().isPresent());
        assertTrue(flowDecompositionResults.getAcNetPositions().isPresent());
        assertTrue(flowDecompositionResults.getGlsks().isPresent());
        assertTrue(flowDecompositionResults.getZonalPtdf().isPresent());
        assertTrue(flowDecompositionResults.getNodalPtdf().isPresent());
        assertTrue(flowDecompositionResults.getPsdf().isPresent());
        assertTrue(flowDecompositionResults.getNodalInjections().isPresent());
        assertTrue(flowDecompositionResults.getDcNodalInjections().isPresent());
        assertTrue(flowDecompositionResults.getAllocatedAndLoopFlows().isPresent());
        assertTrue(flowDecompositionResults.getPstFlows().isPresent());
    }
}
