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
public class ContingencyTests {
    @Test
    void onlyNStateTest() {
        String networkFileName = "NETWORK_SINGLE_LOAD_TWO_GENERATORS_WITH_COUNTRIES.uct";
        Network network = AllocatedFlowTests.importNetwork(networkFileName);

        FlowDecompositionParameters flowDecompositionParameters = new FlowDecompositionParameters()
            .setSaveIntermediates(FlowDecompositionParameters.SAVE_INTERMEDIATES)
            .setContingencyStrategy(FlowDecompositionParameters.ContingencyStrategy.ONLY_N_STATE);

        FlowDecompositionComputer flowDecompositionComputer = new FlowDecompositionComputer(flowDecompositionParameters);
        FlowDecompositionResults flowDecompositionResults = flowDecompositionComputer.run(network);

        RescalingTests.assertCoherenceTotalFlow(flowDecompositionParameters.isRescaleEnabled(), flowDecompositionResults);
        assertTrue(flowDecompositionResults.getContingencies().isPresent());
        assertEquals(0, flowDecompositionResults.getContingencies().get().size());
    }

    @Test
    void autoContingencyTest() {
        String networkFileName = "NETWORK_SINGLE_LOAD_TWO_GENERATORS_WITH_COUNTRIES.uct";
        Network network = AllocatedFlowTests.importNetwork(networkFileName);

        FlowDecompositionParameters flowDecompositionParameters = new FlowDecompositionParameters()
            .setSaveIntermediates(FlowDecompositionParameters.SAVE_INTERMEDIATES)
            .setContingencyStrategy(FlowDecompositionParameters.ContingencyStrategy.AUTO_CONTINGENCY);

        FlowDecompositionComputer flowDecompositionComputer = new FlowDecompositionComputer(flowDecompositionParameters);
        FlowDecompositionResults flowDecompositionResults = flowDecompositionComputer.run(network);

        RescalingTests.assertCoherenceTotalFlow(flowDecompositionParameters.isRescaleEnabled(), flowDecompositionResults);
        assertTrue(flowDecompositionResults.getContingencies().isPresent());
        assertEquals(2, flowDecompositionResults.getContingencies().get().size());
        String branchId1 = "FGEN1 11 BLOAD 11 1";
        String branchId2 = "BLOAD 11 BGEN2 11 1";
        assertEquals(branchId1, flowDecompositionResults.getContingencies().get().get(0).getId());
        assertEquals(branchId2, flowDecompositionResults.getContingencies().get().get(1).getId());
    }
}
