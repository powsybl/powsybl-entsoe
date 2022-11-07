/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.flow_decomposition;

import com.powsybl.cgmes.conformity.CgmesConformity1Catalog;
import com.powsybl.iidm.network.*;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowParameters;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 */
class CgmesIntegrationTests {
    private static final boolean AC_LOAD_FLOW = false;

    @Test
    void checkThatLossCompensationWorksWithNodeBreakerTopology() {
        Network network = Importers.importData("CGMES", CgmesConformity1Catalog.smallNodeBreakerHvdc().dataSource(), null);
        LoadFlowParameters loadFlowParameters = new LoadFlowParameters();
        loadFlowParameters.setDc(AC_LOAD_FLOW);
        LoadFlow.run(network, loadFlowParameters);
        Map<Bus, Double> busToLossMap = network.getBusBreakerView().getBusStream()
            .collect(Collectors.toMap(Function.identity(), bus -> getLossOnBus(network, bus)
            ));

        LossesCompensator lossesCompensator = new LossesCompensator(FlowDecompositionParameters.DISABLE_LOSSES_COMPENSATION_EPSILON);
        lossesCompensator.run(network);

        busToLossMap.forEach((bus, losses) -> {
            Load load = network.getLoad(String.format("LOSSES %s", bus.getId()));
            assertNotNull(load);
            assertEquals(losses, load.getP0());
        });
    }

    private static double getLossOnBus(Network network, Bus bus) {
        return network.getBranchStream()
            .filter(branch -> terminalIsSendingPowerToBus(branch.getTerminal1(), bus) || terminalIsSendingPowerToBus(branch.getTerminal2(), bus))
            .mapToDouble(branch -> branch.getTerminal1().getP() + branch.getTerminal2().getP())
            .sum();
    }

    private static boolean terminalIsSendingPowerToBus(Terminal terminal, Bus bus) {
        return terminal.getBusBreakerView().getBus() == bus && terminal.getP() > 0;
    }

    @Test
    void checkFlowDecompositionWorksOnCgmesFile() {
        Network network = Importers.importData("CGMES", CgmesConformity1Catalog.smallNodeBreakerHvdc().dataSource(), null);

        FlowDecompositionParameters flowDecompositionParameters = new FlowDecompositionParameters()
            .setEnableLossesCompensation(FlowDecompositionParameters.ENABLE_LOSSES_COMPENSATION)
            .setLossesCompensationEpsilon(FlowDecompositionParameters.DISABLE_LOSSES_COMPENSATION_EPSILON)
            .setSensitivityEpsilon(FlowDecompositionParameters.DISABLE_SENSITIVITY_EPSILON)
            .setRescaleEnabled(FlowDecompositionParameters.ENABLE_RESCALED_RESULTS);
        Optional<Branch> optionalBranch = network.getBranchStream().findAny();
        if (optionalBranch.isPresent()) {
            String xnecId = optionalBranch.get().getId();
            XnecProvider xnecProvider = new XnecProviderImpl(List.of(xnecId));
            FlowDecompositionComputer flowDecompositionComputer = new FlowDecompositionComputer(flowDecompositionParameters);
            FlowDecompositionResults flowDecompositionResults = flowDecompositionComputer.run(xnecProvider, network);
            assertNotNull(flowDecompositionResults.getDecomposedFlowMap().get(xnecId));
        }
    }
}
