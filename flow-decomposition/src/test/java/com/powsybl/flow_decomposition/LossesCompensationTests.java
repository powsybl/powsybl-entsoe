/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.flow_decomposition;

import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Load;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowParameters;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.powsybl.flow_decomposition.TestUtils.importNetwork;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
class LossesCompensationTests {
    private static final double EPSILON = 1e-3;
    private static final boolean AC_LOAD_FLOW = false;

    @Test
    void checkThatLossesCompensationIsDoneCorrectly() {
        String networkFileName = "NETWORK_SINGLE_LOAD_TWO_GENERATORS_WITH_COUNTRIES.uct";

        Network network = importNetwork(networkFileName);
        LoadFlowParameters loadFlowParameters = new LoadFlowParameters();
        loadFlowParameters.setDc(AC_LOAD_FLOW);
        LoadFlow.run(network, loadFlowParameters);
        LossesCompensator lossesCompensator = new LossesCompensator(FlowDecompositionParameters.DISABLE_LOSSES_COMPENSATION_EPSILON);
        lossesCompensator.run(network);

        assessSingleLoadTwoGeneratorsNetworkLossesCompensation(network);
    }

    @Test
    void checkThatLossesCompensationDoesEnforceAcLoadflow() {
        String networkFileName = "NETWORK_SINGLE_LOAD_TWO_GENERATORS_WITH_COUNTRIES.uct";
        Network network = importNetwork(networkFileName);
        LoadFlowParameters loadFlowParameters = new LoadFlowParameters();
        loadFlowParameters.setDc(AC_LOAD_FLOW);
        LoadFlow.run(network, loadFlowParameters);
        LossesCompensator lossesCompensator = new LossesCompensator(FlowDecompositionParameters.DISABLE_LOSSES_COMPENSATION_EPSILON);
        lossesCompensator.run(network);

        assessSingleLoadTwoGeneratorsNetworkLossesCompensation(network);
    }

    private void assessSingleLoadTwoGeneratorsNetworkLossesCompensation(Network network) {
        Load lossesFgen = network.getLoad("LOSSES FGEN1 11");
        assertNotNull(lossesFgen);
        assertEquals("FGEN1 1", lossesFgen.getTerminal().getVoltageLevel().getId());
        assertEquals(0.0625, lossesFgen.getP0(), EPSILON);
        Load lossesBgen = network.getLoad("LOSSES BGEN2 11");
        assertNotNull(lossesBgen);
        assertEquals("BGEN2 1", lossesBgen.getTerminal().getVoltageLevel().getId());
        assertEquals(0.0625, lossesBgen.getP0(), EPSILON);
        Load lossesBload = network.getLoad("LOSSES BLOAD 11");
        assertNotNull(lossesBload);
        assertEquals("BLOAD 1", lossesBload.getTerminal().getVoltageLevel().getId());
        assertEquals(0.0, lossesBload.getP0(), EPSILON);
    }

    @Test
    void checkThatLossesCompensationOnCentralGeneratorDoesAggregateLosses() {
        String networkFileName = "NETWORK_TWO_LOADS_SINGLE_GENERATOR_WITH_COUNTRIES.uct";

        Network network = importNetwork(networkFileName);
        LoadFlowParameters loadFlowParameters = new LoadFlowParameters();
        loadFlowParameters.setDc(AC_LOAD_FLOW);
        LoadFlow.run(network, loadFlowParameters);
        LossesCompensator lossesCompensator = new LossesCompensator(FlowDecompositionParameters.DISABLE_LOSSES_COMPENSATION_EPSILON);
        lossesCompensator.run(network);

        Load lossesFgenX = network.getLoad("LOSSES FGEN1 11");
        assertNotNull(lossesFgenX);
        assertEquals("FGEN1 1", lossesFgenX.getTerminal().getVoltageLevel().getId());
        assertEquals(0.0625 * 4, lossesFgenX.getP0(), EPSILON);
        Load lossesBloadX = network.getLoad("LOSSES BLOAD111");
        assertNotNull(lossesBloadX);
        assertEquals("BLOAD11", lossesBloadX.getTerminal().getVoltageLevel().getId());
        assertEquals(0.0, lossesBloadX.getP0(), EPSILON);
    }

    @Test
    void checkThatLossesCompensationOnTieLineDoesDispatchLossesProportionallyToEachSideResistance() {
        String networkFileName = "NETWORK_SINGLE_LOAD_TWO_GENERATORS_WITH_XNODE.uct";

        Network network = importNetwork(networkFileName);
        LoadFlowParameters loadFlowParameters = new LoadFlowParameters();
        loadFlowParameters.setDc(AC_LOAD_FLOW);
        LoadFlow.run(network, loadFlowParameters);
        LossesCompensator lossesCompensator = new LossesCompensator(FlowDecompositionParameters.DISABLE_LOSSES_COMPENSATION_EPSILON);
        lossesCompensator.run(network);

        Load lossesFgenBload = network.getLoad("LOSSES FGEN1 11 X     11 1 + X     11 BLOAD 11 1");
        assertNull(lossesFgenBload);

        Load lossesFgenX = network.getLoad("LOSSES FGEN1 11");
        assertNotNull(lossesFgenX);
        assertEquals("FGEN1 1", lossesFgenX.getTerminal().getVoltageLevel().getId());
        assertEquals(0.015625, lossesFgenX.getP0(), EPSILON);
        Load lossesBloadX = network.getLoad("LOSSES BLOAD 11");
        assertNotNull(lossesBloadX);
        assertEquals("BLOAD 1", lossesBloadX.getTerminal().getVoltageLevel().getId());
        assertEquals(0.046875, lossesBloadX.getP0(), EPSILON);
    }

    @Test
    void checkThatDefaultFlowDecompositionDoesNotCompensateLosses() {
        String networkFileName = "NETWORK_SINGLE_LOAD_TWO_GENERATORS_WITH_COUNTRIES_EXTRA_SUBSTATION.uct";
        Network network = importNetwork(networkFileName);
        String xnecId = "FLOAD 11 BLOAD 11 1";

        FlowDecompositionComputer flowDecompositionComputer = new FlowDecompositionComputer();
        XnecProvider xnecProvider = new XnecProviderImpl(List.of(xnecId));
        FlowDecompositionResults flowDecompositionResults = flowDecompositionComputer.run(xnecProvider, network);

        assertEquals(99.813, flowDecompositionResults.getDecomposedFlowMap().get(xnecId).getAllocatedFlow(), EPSILON);
        assertEquals(0.0936, flowDecompositionResults.getDecomposedFlowMap().get(xnecId).getLoopFlow(Country.FR), EPSILON);
        assertEquals(0.0936, flowDecompositionResults.getDecomposedFlowMap().get(xnecId).getLoopFlow(Country.BE), EPSILON);
    }

    @Test
    void checkThatEnablingLossesCompensationDoesImpactFlowDecompositionCorrectly() {
        String networkFileName = "NETWORK_SINGLE_LOAD_TWO_GENERATORS_WITH_COUNTRIES_EXTRA_SUBSTATION.uct";
        Network network = importNetwork(networkFileName);
        String xnecId = "FLOAD 11 BLOAD 11 1";

        FlowDecompositionParameters flowDecompositionParameters = new FlowDecompositionParameters();
        flowDecompositionParameters.setEnableLossesCompensation(FlowDecompositionParameters.ENABLE_LOSSES_COMPENSATION);
        FlowDecompositionComputer flowDecompositionComputer = new FlowDecompositionComputer(flowDecompositionParameters);
        XnecProvider xnecProvider = new XnecProviderImpl(List.of(xnecId));
        FlowDecompositionResults flowDecompositionResults = flowDecompositionComputer.run(xnecProvider, network);

        assertEquals(99.813, flowDecompositionResults.getDecomposedFlowMap().get(xnecId).getAllocatedFlow(), EPSILON);
        assertEquals(-0.609, flowDecompositionResults.getDecomposedFlowMap().get(xnecId).getLoopFlow(Country.FR), EPSILON);
        assertEquals(-0.609, flowDecompositionResults.getDecomposedFlowMap().get(xnecId).getLoopFlow(Country.BE), EPSILON);
    }

    @Test
    void checkThatDisablingLossesCompensationDoesImpactFlowDecompositionCorrectly() {
        String networkFileName = "NETWORK_SINGLE_LOAD_TWO_GENERATORS_WITH_COUNTRIES_EXTRA_SUBSTATION.uct";
        Network network = importNetwork(networkFileName);
        String xnecId = "FLOAD 11 BLOAD 11 1";

        FlowDecompositionParameters flowDecompositionParameters = new FlowDecompositionParameters();
        flowDecompositionParameters.setEnableLossesCompensation(FlowDecompositionParameters.DISABLE_LOSSES_COMPENSATION);
        FlowDecompositionComputer flowDecompositionComputer = new FlowDecompositionComputer(flowDecompositionParameters);
        XnecProvider xnecProvider = new XnecProviderImpl(List.of(xnecId));
        FlowDecompositionResults flowDecompositionResults = flowDecompositionComputer.run(xnecProvider, network);

        assertEquals(99.813, flowDecompositionResults.getDecomposedFlowMap().get(xnecId).getAllocatedFlow(), EPSILON);
        assertEquals(0.0936, flowDecompositionResults.getDecomposedFlowMap().get(xnecId).getLoopFlow(Country.FR), EPSILON);
        assertEquals(0.0936, flowDecompositionResults.getDecomposedFlowMap().get(xnecId).getLoopFlow(Country.BE), EPSILON);
    }
}
