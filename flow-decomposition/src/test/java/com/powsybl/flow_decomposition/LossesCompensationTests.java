/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.flow_decomposition;

import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Load;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlowParameters;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
class LossesCompensationTests {
    private static final double EPSILON = 1e-3;
    private static final boolean AC_LOAD_FLOW = false;

    static Network importNetwork(String networkResourcePath) {
        String networkName = Paths.get(networkResourcePath).getFileName().toString();
        //networkName = networkName.replace(".zip", "");
        return Importers.loadNetwork(networkName, AllocatedFlowTests.class.getResourceAsStream(networkResourcePath));
    }

    @Test
    void checkThatLossesCompensationIsDoneCorrectly() {
        String networkFileName = "NETWORK_SINGLE_LOAD_TWO_GENERATORS_WITH_COUNTRIES.uct";

        LoadFlowParameters loadFlowParameters = new LoadFlowParameters();
        loadFlowParameters.setDc(AC_LOAD_FLOW);

        Network network = importNetwork(networkFileName);
        LossesCompensator lossesCompensator = new LossesCompensator(loadFlowParameters,
            FlowDecompositionParameters.DISABLE_LOSSES_COMPENSATION_EPSILON);
        lossesCompensator.run(network);

        assessSingleLoadTwoGeneratorsNetworkLossesCompensation(network);
    }

    @Test
    void checkThatLossesCompensationDoesEnforceAcLoadflow() {
        String networkFileName = "NETWORK_SINGLE_LOAD_TWO_GENERATORS_WITH_COUNTRIES.uct";

        LoadFlowParameters loadFlowParameters = new LoadFlowParameters();
        loadFlowParameters.setDc(FlowDecompositionComputer.DC_LOAD_FLOW);

        Network network = importNetwork(networkFileName);
        LossesCompensator lossesCompensator = new LossesCompensator(loadFlowParameters,
            FlowDecompositionParameters.DISABLE_LOSSES_COMPENSATION_EPSILON);
        lossesCompensator.run(network);

        assessSingleLoadTwoGeneratorsNetworkLossesCompensation(network);
    }

    private void assessSingleLoadTwoGeneratorsNetworkLossesCompensation(Network network) {
        Load lossesFgenBload = network.getLoad("LOSSES FGEN1 11 BLOAD 11 1");
        assertNotNull(lossesFgenBload);
        assertEquals("FGEN1 1", lossesFgenBload.getTerminal().getVoltageLevel().getId());
        assertEquals(0.0625, lossesFgenBload.getP0(), EPSILON);
        Load lossesBloadBgen = network.getLoad("LOSSES BLOAD 11 BGEN2 11 1");
        assertNotNull(lossesBloadBgen);
        assertEquals("BGEN2 1", lossesBloadBgen.getTerminal().getVoltageLevel().getId());
        assertEquals(0.0625, lossesBloadBgen.getP0(), EPSILON);
    }

    @Test
    void checkThatLossesCompensationOnTieLineDoesDispatchLossesProportionallyToEachSideResistance() {
        String networkFileName = "NETWORK_SINGLE_LOAD_TWO_GENERATORS_WITH_XNODE.uct";

        LoadFlowParameters loadFlowParameters = new LoadFlowParameters();
        loadFlowParameters.setDc(AC_LOAD_FLOW);

        Network network = importNetwork(networkFileName);
        LossesCompensator lossesCompensator = new LossesCompensator(loadFlowParameters,
            FlowDecompositionParameters.DISABLE_LOSSES_COMPENSATION_EPSILON);
        lossesCompensator.run(network);

        Load lossesFgenBload = network.getLoad("LOSSES FGEN1 11 X     11 1 + X     11 BLOAD 11 1");
        assertNull(lossesFgenBload);

        Load lossesFgenX = network.getLoad("LOSSES FGEN1 11 X     11 1");
        assertNotNull(lossesFgenX);
        assertEquals("FGEN1 1", lossesFgenX.getTerminal().getVoltageLevel().getId());
        assertEquals(0.015625, lossesFgenX.getP0(), EPSILON);
        Load lossesBloadX = network.getLoad("LOSSES X     11 BLOAD 11 1");
        assertNotNull(lossesBloadX);
        assertEquals("BLOAD 1", lossesBloadX.getTerminal().getVoltageLevel().getId());
        assertEquals(0.046875, lossesBloadX.getP0(), EPSILON);
    }

    @Test
    void checkThatDefaultFlowDecompositionDoesNotCompensateLosses() {
        String networkFileName = "NETWORK_SINGLE_LOAD_TWO_GENERATORS_WITH_COUNTRIES_EXTRA_SUBSTATION.uct";
        Network network = importNetwork(networkFileName);

        FlowDecompositionComputer flowDecompositionComputer = new FlowDecompositionComputer();
        FlowDecompositionResults flowDecompositionResults = flowDecompositionComputer.run(network);

        assertEquals(99.813, flowDecompositionResults.getDecomposedFlowMap().get("FLOAD 11 BLOAD 11 1").getAllocatedFlow(), EPSILON);
        assertEquals(0.0936, flowDecompositionResults.getDecomposedFlowMap().get("FLOAD 11 BLOAD 11 1").getLoopFlow(Country.FR), EPSILON);
        assertEquals(0.0936, flowDecompositionResults.getDecomposedFlowMap().get("FLOAD 11 BLOAD 11 1").getLoopFlow(Country.BE), EPSILON);
    }

    @Test
    void checkThatEnablingLossesCompensationDoesImpactFlowDecompositionCorrectly() {
        String networkFileName = "NETWORK_SINGLE_LOAD_TWO_GENERATORS_WITH_COUNTRIES_EXTRA_SUBSTATION.uct";
        Network network = importNetwork(networkFileName);

        FlowDecompositionParameters flowDecompositionParameters = new FlowDecompositionParameters();
        flowDecompositionParameters.setEnableLossesCompensation(FlowDecompositionParameters.ENABLE_LOSSES_COMPENSATION);
        FlowDecompositionComputer flowDecompositionComputer = new FlowDecompositionComputer(flowDecompositionParameters);
        FlowDecompositionResults flowDecompositionResults = flowDecompositionComputer.run(network);

        assertEquals(99.813, flowDecompositionResults.getDecomposedFlowMap().get("FLOAD 11 BLOAD 11 1").getAllocatedFlow(), EPSILON);
        assertEquals(-0.609, flowDecompositionResults.getDecomposedFlowMap().get("FLOAD 11 BLOAD 11 1").getLoopFlow(Country.FR), EPSILON);
        assertEquals(-0.609, flowDecompositionResults.getDecomposedFlowMap().get("FLOAD 11 BLOAD 11 1").getLoopFlow(Country.BE), EPSILON);
    }

    @Test
    void checkThatDisablingLossesCompensationDoesImpactFlowDecompositionCorrectly() {
        String networkFileName = "NETWORK_SINGLE_LOAD_TWO_GENERATORS_WITH_COUNTRIES_EXTRA_SUBSTATION.uct";
        Network network = importNetwork(networkFileName);

        FlowDecompositionParameters flowDecompositionParameters = new FlowDecompositionParameters();
        flowDecompositionParameters.setEnableLossesCompensation(FlowDecompositionParameters.DISABLE_LOSSES_COMPENSATION);
        FlowDecompositionComputer flowDecompositionComputer = new FlowDecompositionComputer(flowDecompositionParameters);
        FlowDecompositionResults flowDecompositionResults = flowDecompositionComputer.run(network);

        assertEquals(99.813, flowDecompositionResults.getDecomposedFlowMap().get("FLOAD 11 BLOAD 11 1").getAllocatedFlow(), EPSILON);
        assertEquals(0.0936, flowDecompositionResults.getDecomposedFlowMap().get("FLOAD 11 BLOAD 11 1").getLoopFlow(Country.FR), EPSILON);
        assertEquals(0.0936, flowDecompositionResults.getDecomposedFlowMap().get("FLOAD 11 BLOAD 11 1").getLoopFlow(Country.BE), EPSILON);
    }

    @Test
    void testCgmes() {
        //String networkFileName = "locally/20220714_0230_CGM_ESFR_hand_made.zip";
        //networkFileName = "MicroGrid.zip";
        //Network network = importNetwork(networkFileName);
        //Network network = Importers.loadNetwork(new File(LossesCompensationTests.class.getResource("MicroGrid.zip").getFile()).toString());
        Network network = Importers.loadNetwork(new File(LossesCompensationTests.class.getResource("locally/20210402_0230_2D5_UX0.zip").getFile()).toString());

        FlowDecompositionParameters flowDecompositionParameters = new FlowDecompositionParameters();
        flowDecompositionParameters.setEnableLossesCompensation(FlowDecompositionParameters.ENABLE_LOSSES_COMPENSATION);
        FlowDecompositionComputer flowDecompositionComputer = new FlowDecompositionComputer(flowDecompositionParameters);
        FlowDecompositionResults flowDecompositionResults = flowDecompositionComputer.run(network);

    }
}
