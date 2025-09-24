/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.flow_decomposition;

import com.powsybl.cgmes.conformity.Cgmes3Catalog;
import com.powsybl.cgmes.conformity.CgmesConformity1Catalog;
import com.powsybl.flow_decomposition.xnec_provider.XnecProviderByIds;
import com.powsybl.iidm.network.*;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowParameters;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.powsybl.flow_decomposition.TestUtils.getLossOnBus;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 */
class CgmesIntegrationTests {
    private static final boolean AC_LOAD_FLOW = false;
    private static final double DOUBLE_TOLERANCE = 1e-3;

    @Test
    void checkThatLossCompensationWorksWithNodeBreakerTopology() {
        Network network = Importers.importData("CGMES", CgmesConformity1Catalog.smallNodeBreakerHvdc().dataSource(), null);
        LoadFlowParameters loadFlowParameters = LoadFlowParameters.load();
        loadFlowParameters.setDc(AC_LOAD_FLOW);
        LoadFlow.run(network, loadFlowParameters);
        Map<Bus, Double> busToLossMap = network.getBusBreakerView().getBusStream()
            .collect(Collectors.toMap(Function.identity(), bus -> getLossOnBus(network, bus)
            ));

        LossesCompensator lossesCompensator = new LossesCompensator(FlowDecompositionParameters.DISABLE_LOSSES_COMPENSATION_EPSILON);
        lossesCompensator.run(network);

        busToLossMap.forEach((bus, losses) -> {
            Load load = network.getLoad(LossesCompensator.getLossesId(bus.getId()));
            assertNotNull(load);
            assertEquals(losses, load.getP0());
        });
    }

    @Test
    void checkFlowDecompositionWorksOnCgmesFile() {
        Network network = Importers.importData("CGMES", CgmesConformity1Catalog.smallNodeBreakerHvdc().dataSource(), null);

        FlowDecompositionParameters flowDecompositionParameters = new FlowDecompositionParameters()
            .setEnableLossesCompensation(FlowDecompositionParameters.ENABLE_LOSSES_COMPENSATION)
            .setLossesCompensationEpsilon(FlowDecompositionParameters.DISABLE_LOSSES_COMPENSATION_EPSILON)
            .setSensitivityEpsilon(FlowDecompositionParameters.DISABLE_SENSITIVITY_EPSILON)
            .setRescaleMode(FlowDecompositionParameters.RescaleMode.ACER_METHODOLOGY);
        String xnecId = "044cd006-c766-11e1-8775-005056c00008";
        XnecProvider xnecProvider = XnecProviderByIds.builder().addNetworkElementsOnBasecase(Set.of(xnecId)).build();
        FlowDecompositionComputer flowDecompositionComputer = new FlowDecompositionComputer(flowDecompositionParameters);
        FlowDecompositionResults flowDecompositionResults = flowDecompositionComputer.run(xnecProvider, network);
        assertNotNull(flowDecompositionResults.getDecomposedFlowMap().get(xnecId));
        assertEquals(1, flowDecompositionResults.getDecomposedFlowMap().size());
        TestUtils.assertCoherenceTotalFlow(flowDecompositionParameters.getRescaleMode(), flowDecompositionResults);
    }

    @Test
    void testCoherentNetPosition() {
        Properties importParams = new Properties();
        Network network = Importers.importData("CGMES", Cgmes3Catalog.microGrid().dataSource(), importParams);
        LoadFlow.run(network, LoadFlowParameters.load().setDc(false));
        assertEquals(0.0, network.getDanglingLineStream(DanglingLineFilter.PAIRED).filter(danglingLine -> Double.isFinite(danglingLine.getBoundary().getP())).mapToDouble(danglingLine -> danglingLine.getBoundary().getP()).sum(), DOUBLE_TOLERANCE);

        Map<Country, Double> netPositions = NetPositionComputer.computeNetPositions(network);

        assertEquals(0, netPositions.values().stream().mapToDouble(Double::doubleValue).sum(), DOUBLE_TOLERANCE);
        assertEquals(-245.189, netPositions.get(Country.BE), DOUBLE_TOLERANCE);
        assertEquals(245.189, netPositions.get(Country.NL), DOUBLE_TOLERANCE);
    }
}
