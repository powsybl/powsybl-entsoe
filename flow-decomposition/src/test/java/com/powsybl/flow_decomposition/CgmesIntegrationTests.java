/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.flow_decomposition;

import com.powsybl.cgmes.conformity.CgmesConformity1Catalog;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Load;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowParameters;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 */
class CgmesIntegrationTests {
    private static final double EPSILON = 1e-3;

    @Test
    void checkThatLossCompensationWorksWithNodeBreakerTopology() {
        Network network = Importers.importData("CGMES", CgmesConformity1Catalog.smallNodeBreakerHvdc().dataSource(), null);
        LoadFlowParameters loadFlowParameters = new LoadFlowParameters();
        loadFlowParameters.setDc(false);
        LoadFlow.run(network, loadFlowParameters);
        String branchId = network.getBranchStream().iterator().next().getId();
        Branch branch = network.getBranch(branchId);
        double p = branch.getTerminal1().getP() + branch.getTerminal2().getP();

        LossesCompensator lossesCompensator = new LossesCompensator(loadFlowParameters,
            FlowDecompositionParameters.DISABLE_LOSSES_COMPENSATION_EPSILON);
        lossesCompensator.run(network);

        Load load = network.getLoad(String.format("LOSSES %s", branchId));
        assertNotNull(load);
        assertEquals(p, load.getP0());
        assertEquals(load.getTerminal().getBusBreakerView().getBus(), branch.getTerminal2().getBusBreakerView().getBus());
        assertNotEquals(load.getTerminal().getBusBreakerView().getBus(), branch.getTerminal1().getBusBreakerView().getBus());
    }

    @Test
    void checkFlowDecompositionWorksOnCgmesFile() {
        Network network = Importers.importData("CGMES", CgmesConformity1Catalog.smallNodeBreakerHvdc().dataSource(), null);

        FlowDecompositionParameters flowDecompositionParameters = new FlowDecompositionParameters()
            .setSaveIntermediates(FlowDecompositionParameters.DO_NOT_SAVE_INTERMEDIATES)
            .setEnableLossesCompensation(FlowDecompositionParameters.ENABLE_LOSSES_COMPENSATION)
            .setLossesCompensationEpsilon(FlowDecompositionParameters.DISABLE_LOSSES_COMPENSATION_EPSILON)
            .setSensitivityEpsilon(FlowDecompositionParameters.DISABLE_SENSITIVITY_EPSILON)
            .setRescaleEnabled(FlowDecompositionParameters.ENABLE_RESCALED_RESULTS)
            .setXnecSelectionStrategy(FlowDecompositionParameters.XnecSelectionStrategy.INTERCONNECTION_OR_ZONE_TO_ZONE_PTDF_GT_5PC);
        FlowDecompositionComputer flowDecompositionComputer = new FlowDecompositionComputer(flowDecompositionParameters);
        FlowDecompositionResults flowDecompositionResults = flowDecompositionComputer.run(network);
        assertNotNull(flowDecompositionResults.getDecomposedFlowMap());
    }
}
