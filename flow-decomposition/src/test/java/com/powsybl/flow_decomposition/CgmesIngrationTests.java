/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.flow_decomposition;

import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Network;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 */
class CgmesIngrationTests {
    private static final double EPSILON = 1e-3;

    @Test
    void checkThatCgmesNetworkFlowDecompositionWorksWithDefaultParameters() {
        Network network = Importers.loadNetwork(new File(CgmesIngrationTests.class.getResource("MicroGrid.zip").getFile()).toString());

        FlowDecompositionComputer flowDecompositionComputer = new FlowDecompositionComputer();
        FlowDecompositionResults flowDecompositionResults = flowDecompositionComputer.run(network);
        System.out.println(flowDecompositionResults.getDecomposedFlowMap());
    }

    @Test
    void checkThatCgmesNetworkFlowDecompositionWorksWithLossCompensation() {
        Network network = Importers.loadNetwork(new File(CgmesIngrationTests.class.getResource("MicroGrid.zip").getFile()).toString());

        FlowDecompositionParameters flowDecompositionParameters = new FlowDecompositionParameters();
        flowDecompositionParameters.setEnableLossesCompensation(FlowDecompositionParameters.ENABLE_LOSSES_COMPENSATION);
        FlowDecompositionComputer flowDecompositionComputer = new FlowDecompositionComputer(flowDecompositionParameters);
        FlowDecompositionResults flowDecompositionResults = flowDecompositionComputer.run(network);
        System.out.println(flowDecompositionResults.getDecomposedFlowMap());
    }

    @Test
    void checkThatCgmesNetworkFlowDecompositionWorksWithZonalFiltering() {
        Network network = Importers.loadNetwork(new File(CgmesIngrationTests.class.getResource("MicroGrid.zip").getFile()).toString());

        FlowDecompositionParameters flowDecompositionParameters = new FlowDecompositionParameters();
        flowDecompositionParameters.setXnecSelectionStrategy(FlowDecompositionParameters.XnecSelectionStrategy.INTERCONNECTION_OR_ZONE_TO_ZONE_PTDF_GT_5PC);
        FlowDecompositionComputer flowDecompositionComputer = new FlowDecompositionComputer(flowDecompositionParameters);
        FlowDecompositionResults flowDecompositionResults = flowDecompositionComputer.run(network);
        System.out.println(flowDecompositionResults.getDecomposedFlowMap());
    }
}