/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.flow_decomposition;

import com.powsybl.flow_decomposition.xnec_provider.XnecProviderByIds;
import com.powsybl.iidm.network.Bus;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Load;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowParameters;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.powsybl.flow_decomposition.TestUtils.getLossOnBus;
import static com.powsybl.flow_decomposition.TestUtils.importNetwork;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
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
        XnecProvider xnecProvider = XnecProviderByIds.builder().addNetworkElementsOnBasecase(Set.of(xnecId)).build();
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
        XnecProvider xnecProvider = XnecProviderByIds.builder().addNetworkElementsOnBasecase(Set.of(xnecId)).build();
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
        XnecProvider xnecProvider = XnecProviderByIds.builder().addNetworkElementsOnBasecase(Set.of(xnecId)).build();
        FlowDecompositionResults flowDecompositionResults = flowDecompositionComputer.run(xnecProvider, network);

        assertEquals(99.813, flowDecompositionResults.getDecomposedFlowMap().get(xnecId).getAllocatedFlow(), EPSILON);
        assertEquals(0.0936, flowDecompositionResults.getDecomposedFlowMap().get(xnecId).getLoopFlow(Country.FR), EPSILON);
        assertEquals(0.0936, flowDecompositionResults.getDecomposedFlowMap().get(xnecId).getLoopFlow(Country.BE), EPSILON);
    }

    @Test
    void testLossCompensationCanBeAppliedToNetworkWithVariants() {
        LoadFlowParameters loadFlowParameters = new LoadFlowParameters();
        loadFlowParameters.setDc(AC_LOAD_FLOW);

        LossesCompensator lossesCompensator = new LossesCompensator(FlowDecompositionParameters.DISABLE_LOSSES_COMPENSATION_EPSILON);

        String networkFileName = "NETWORK_SINGLE_LOAD_TWO_GENERATORS_WITH_COUNTRIES.uct";
        Network network = importNetwork(networkFileName);
        network.getVariantManager().cloneVariant("InitialState", "NewState");
        int networkHash = network.hashCode();

        LoadFlow.run(network, loadFlowParameters);
        assertEquals(networkHash, network.hashCode());

        Map<Bus, Double> busToLossMap = network.getBusBreakerView().getBusStream()
            .collect(Collectors.toMap(Function.identity(), bus -> getLossOnBus(network, bus)
            ));

        lossesCompensator.run(network);
        assertEquals(networkHash, network.hashCode());

        busToLossMap.forEach((bus, losses) -> {
            Load load = network.getLoad(LossesCompensator.getLossesId(bus.getId()));
            assertNotNull(load);
            assertEquals(losses, load.getP0());
        });

        network.getVariantManager().setWorkingVariant("NewState");
        assertEquals(networkHash, network.hashCode());

        // On the other variant, the loss loads are still zero MW
        busToLossMap.keySet().forEach(bus -> {
            Load load = network.getLoad(LossesCompensator.getLossesId(bus.getId()));
            assertNotNull(load);
            assertEquals(0.0, load.getP0());
        });

        LoadFlow.run(network, loadFlowParameters);
        assertEquals(networkHash, network.hashCode());

        lossesCompensator.run(network);
        assertEquals(networkHash, network.hashCode());

        // On the other variant, the loss loads are not zero MW anymore
        busToLossMap.forEach((bus, losses) -> {
            Load load = network.getLoad(LossesCompensator.getLossesId(bus.getId()));
            assertEquals(losses, load.getP0());
        });

        network.getVariantManager().setWorkingVariant("InitialState");
        assertEquals(networkHash, network.hashCode());

        //But the loss loads have not been reset on the initial variant
        busToLossMap.forEach((bus, losses) -> {
            Load load = network.getLoad(LossesCompensator.getLossesId(bus.getId()));
            assertEquals(losses, load.getP0());
        });
    }

    @Test
    void testLossCompensationComputerCanBeAppliedToMultipleNetworks() {
        LoadFlowParameters loadFlowParameters = new LoadFlowParameters();
        loadFlowParameters.setDc(AC_LOAD_FLOW);

        LossesCompensator lossesCompensator = new LossesCompensator(FlowDecompositionParameters.DISABLE_LOSSES_COMPENSATION_EPSILON);

        String networkFileName1 = "NETWORK_SINGLE_LOAD_TWO_GENERATORS_WITH_COUNTRIES.uct";
        String networkFileName2 = "NETWORK_SINGLE_LOAD_TWO_GENERATORS_WITH_COUNTRIES_INVERTED.uct";

        Network network1 = importNetwork(networkFileName1);
        Network network2 = importNetwork(networkFileName2);

        int hashCode1 = network1.hashCode();
        int hashCode2 = network2.hashCode();
        assertNotEquals(hashCode1, hashCode2);

        LoadFlow.run(network1, loadFlowParameters);
        LoadFlow.run(network2, loadFlowParameters);

        assertEquals(hashCode1, network1.hashCode());
        assertEquals(hashCode2, network2.hashCode());

        Map<Bus, Double> busToLossMap1 = network1.getBusBreakerView().getBusStream()
            .collect(Collectors.toMap(Function.identity(), bus -> getLossOnBus(network1, bus)
            ));
        Map<Bus, Double> busToLossMap2 = network2.getBusBreakerView().getBusStream()
            .collect(Collectors.toMap(Function.identity(), bus -> getLossOnBus(network2, bus)
            ));

        lossesCompensator.run(network1);
        lossesCompensator.run(network2);

        assertEquals(hashCode1, network1.hashCode());
        assertEquals(hashCode2, network2.hashCode());

        busToLossMap1.forEach((bus, losses) -> {
            Load load = network1.getLoad(LossesCompensator.getLossesId(bus.getId()));
            assertNotNull(load);
            assertEquals(losses, load.getP0());
        });
        busToLossMap2.forEach((bus, losses) -> {
            Load load = network2.getLoad(LossesCompensator.getLossesId(bus.getId()));
            assertNotNull(load);
            assertEquals(losses, load.getP0());
        });
    }

    /*
        Test flow decomposition with losses compensation in a simple network
          FR : bus1
          BE : bus2, bus3
                                   ____ bus3
                                  |      |
                                 l32     d3 (1)
                                  |
                bus1 --- l21 --- bus2
                 |                |
                g1 (3)           d2 (2)

         Line l32 is open at side 1 (bus 3)
         Line characteristics: (r,x,g,b) = (0.01, 0.1, 0.0, 0.5)
     */
    @Test
    void testLossCompensationWithLineConnectedToOnlyOneSide() {
        String networkFileName = "lossesCompensatorLineConnectedToOneSide.xiidm";
        Network network = importNetwork(networkFileName);

        String xnecId = "l21";
        XnecProvider xnecProvider = XnecProviderByIds.builder().addNetworkElementsOnBasecase(Set.of(xnecId)).build();

        // setup parameters
        LoadFlowParameters loadFlowParameters = new LoadFlowParameters();
        loadFlowParameters.setDistributedSlack(true);
        loadFlowParameters.setBalanceType(LoadFlowParameters.BalanceType.PROPORTIONAL_TO_GENERATION_P_MAX);
        FlowDecompositionParameters flowDecompositionParameters = new FlowDecompositionParameters();
        flowDecompositionParameters.setEnableLossesCompensation(FlowDecompositionParameters.ENABLE_LOSSES_COMPENSATION);

        // run flow decomposition
        FlowDecompositionComputer flowDecompositionComputer = new FlowDecompositionComputer(flowDecompositionParameters, loadFlowParameters);
        FlowDecompositionResults flowDecompositionResults = flowDecompositionComputer.run(xnecProvider, network);

        // check results
        DecomposedFlow xnecDecomposedFlow = flowDecompositionResults.getDecomposedFlowMap().get(xnecId);

        assertEquals(-2.004, xnecDecomposedFlow.getAcReferenceFlow(), EPSILON);
        assertEquals(-2.004, xnecDecomposedFlow.getDcReferenceFlow(), EPSILON);
        assertEquals(2.004, xnecDecomposedFlow.getTotalFlow(), EPSILON);
        assertEquals(2.004, xnecDecomposedFlow.getLoopFlow(Country.BE), EPSILON);
    }

    /*
        Same network as in the test `testLossCompensationWithLineConnectedToOnlyOneSide`
        but this time line l32 is fully opened
     */
    @Test
    void testLossCompensationWithLineFullyDisconnected() {
        String networkFileName = "lossesCompensatorLineConnectedToOneSide.xiidm";
        Network network = importNetwork(networkFileName);
        network.getLine("l32").getTerminal2().disconnect();

        String xnecId = "l21";
        XnecProvider xnecProvider = XnecProviderByIds.builder().addNetworkElementsOnBasecase(Set.of(xnecId)).build();

        // setup parameters
        LoadFlowParameters loadFlowParameters = new LoadFlowParameters();
        loadFlowParameters.setDistributedSlack(true);
        loadFlowParameters.setBalanceType(LoadFlowParameters.BalanceType.PROPORTIONAL_TO_GENERATION_P_MAX);
        FlowDecompositionParameters flowDecompositionParameters = new FlowDecompositionParameters();
        flowDecompositionParameters.setEnableLossesCompensation(FlowDecompositionParameters.ENABLE_LOSSES_COMPENSATION);

        // run flow decomposition
        FlowDecompositionComputer flowDecompositionComputer = new FlowDecompositionComputer(flowDecompositionParameters, loadFlowParameters);
        FlowDecompositionResults flowDecompositionResults = flowDecompositionComputer.run(xnecProvider, network);

        // check results
        DecomposedFlow xnecDecomposedFlow = flowDecompositionResults.getDecomposedFlowMap().get(xnecId);

        assertEquals(-1.999, xnecDecomposedFlow.getAcReferenceFlow(), EPSILON);
        assertEquals(-2.0, xnecDecomposedFlow.getDcReferenceFlow(), EPSILON);
        assertEquals(2.0, xnecDecomposedFlow.getTotalFlow(), EPSILON);
        assertEquals(2.0, xnecDecomposedFlow.getLoopFlow(Country.BE), EPSILON);
    }

    /*
        Same network as in the test `testLossCompensationWithLineConnectedToOnlyOneSide`
        but this time line l32 is reconnected
     */
    @Test
    void testLossCompensationWithSmallP1() {
        String networkFileName = "lossesCompensatorLineConnectedToOneSide.xiidm";
        Network network = importNetwork(networkFileName);
        network.getLine("l32").getTerminal1().connect();

        String xnecId = "l21";
        XnecProvider xnecProvider = XnecProviderByIds.builder().addNetworkElementsOnBasecase(Set.of(xnecId)).build();

        // setup parameters
        LoadFlowParameters loadFlowParameters = new LoadFlowParameters();
        loadFlowParameters.setDistributedSlack(true);
        loadFlowParameters.setBalanceType(LoadFlowParameters.BalanceType.PROPORTIONAL_TO_GENERATION_P_MAX);
        FlowDecompositionParameters flowDecompositionParameters = new FlowDecompositionParameters();
        flowDecompositionParameters.setEnableLossesCompensation(FlowDecompositionParameters.ENABLE_LOSSES_COMPENSATION);

        // run flow decomposition
        FlowDecompositionComputer flowDecompositionComputer = new FlowDecompositionComputer(flowDecompositionParameters, loadFlowParameters);
        FlowDecompositionResults flowDecompositionResults = flowDecompositionComputer.run(xnecProvider, network);

        // check results
        DecomposedFlow xnecDecomposedFlow = flowDecompositionResults.getDecomposedFlowMap().get(xnecId);

        assertEquals(-2.904, xnecDecomposedFlow.getAcReferenceFlow(), EPSILON);
        assertEquals(-3.011, xnecDecomposedFlow.getDcReferenceFlow(), EPSILON);
        assertEquals(3.011, xnecDecomposedFlow.getTotalFlow(), EPSILON);
        assertEquals(3.011, xnecDecomposedFlow.getLoopFlow(Country.BE), EPSILON);
    }
}
