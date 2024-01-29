/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.flow_decomposition;

import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowParameters;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 * @author Hugo Schindler{@literal <hugo.schindler@rte-france.com>}
 */
class NetPositionTest {
    private static final double DOUBLE_TOLERANCE = 1e-3;

    private static void assertNetPosition(Network network, double netPositionBe, double netPositionNl, double netPositionDe) {
        Map<Country, Double> netPositions = NetPositionComputer.computeNetPositions(network);
        assertEquals(1000.0, netPositions.get(Country.FR), DOUBLE_TOLERANCE);
        assertEquals(netPositionBe, netPositions.get(Country.BE), DOUBLE_TOLERANCE);
        assertEquals(netPositionNl, netPositions.get(Country.NL), DOUBLE_TOLERANCE);
        assertEquals(netPositionDe, netPositions.get(Country.DE), DOUBLE_TOLERANCE);
        double sumAllNetPositions = netPositions.values().stream().mapToDouble(Double::doubleValue).sum();
        assertEquals(0.0, sumAllNetPositions, DOUBLE_TOLERANCE);
    }

    private static void assertNetPositionForHvdc(Network network, double countryNetPosition) {
        Map<Country, Double> netPositions = NetPositionComputer.computeNetPositions(network);
        assertEquals(countryNetPosition, netPositions.get(Country.FR), DOUBLE_TOLERANCE);
        assertEquals(-countryNetPosition, netPositions.get(Country.DE), DOUBLE_TOLERANCE);
        double sumAllNetPositions = netPositions.values().stream().mapToDouble(Double::doubleValue).sum();
        assertEquals(0.0, sumAllNetPositions, DOUBLE_TOLERANCE);
    }

    @Test
    void testLines() {
        Network network = Network.read("testCase.xiidm", getClass().getResourceAsStream("testCase.xiidm"));
        LoadFlow.run(network, new LoadFlowParameters().setDc(true));
        assertNetPosition(network, 1500.0, 0.0, -2500.0);
    }

    @Test
    void testLinesDisconnected() {
        Network network = Network.read("testCase.xiidm", getClass().getResourceAsStream("testCase.xiidm"));
        network.getBranch("NNL2AA1  BBE3AA1  1").getTerminal1().disconnect();
        network.getBranch("NNL2AA1  BBE3AA1  1").getTerminal2().disconnect();
        LoadFlow.run(network, new LoadFlowParameters().setDc(true));
        assertNetPosition(network, 1500.0, 0.0, -2500.0);
    }

    @Test
    void testLinesNaN() {
        Network network = Network.read("testCase.xiidm", getClass().getResourceAsStream("testCase.xiidm"));
        LoadFlow.run(network, new LoadFlowParameters().setDc(true));
        network.getBranch("NNL2AA1  BBE3AA1  1").getTerminal1().setP(Double.NaN);
        network.getBranch("NNL2AA1  BBE3AA1  1").getTerminal2().setP(Double.NaN);
        assertNetPosition(network, 324.666, 1175.334, -2500.0);
    }

    @Test
    void testDanglingLinesBalanced() {
        Network network = Network.read("TestCaseDangling.xiidm", getClass().getResourceAsStream("TestCaseDangling.xiidm"));
        LoadFlow.run(network, new LoadFlowParameters().setDc(true));
        assertNetPosition(network, 2300.0, -500.0, -2800.0);
    }

    @Test
    void testDanglingLinesDisconnected() {
        Network network = Network.read("TestCaseDangling.xiidm", getClass().getResourceAsStream("TestCaseDangling.xiidm"));
        network.getDanglingLine("BBE2AA1  X_BEFR1  1").getTerminal().disconnect();
        LoadFlow.run(network, new LoadFlowParameters().setDc(true));
        assertNetPosition(network, 2300.0, -500.0, -2800.0);
    }

    @Test
    void testDanglingLinesNaN() {
        Network network = Network.read("TestCaseDangling.xiidm", getClass().getResourceAsStream("TestCaseDangling.xiidm"));
        LoadFlow.run(network, new LoadFlowParameters().setDc(true));
        network.getDanglingLine("BBE2AA1  X_BEFR1  1").getTerminal().setP(Double.NaN);
        assertNetPosition(network, 2300.0, -500.0, -2800.0);
    }

    @Test
    void testDanglingLinesUnbalanced() {
        Network network = Network.read("NETWORK_SINGLE_LOAD_TWO_GENERATORS_WITH_UNBOUNDED_XNODE.uct", getClass().getResourceAsStream("NETWORK_SINGLE_LOAD_TWO_GENERATORS_WITH_UNBOUNDED_XNODE.uct"));
        LoadFlow.run(network, new LoadFlowParameters().setDc(true));
        Map<Country, Double> netPositions = NetPositionComputer.computeNetPositions(network);
        assertEquals(100, netPositions.get(Country.FR), DOUBLE_TOLERANCE);
        assertEquals(0, netPositions.get(Country.BE), DOUBLE_TOLERANCE);
        double sumAllNetPositions = netPositions.values().stream().mapToDouble(Double::doubleValue).sum();
        assertEquals(100, sumAllNetPositions, DOUBLE_TOLERANCE);
    }

    @Test
    void testHvdcLines() {
        Network network = Network.read("TestCaseHvdc.xiidm", getClass().getResourceAsStream("TestCaseHvdc.xiidm"));
        assertNetPositionForHvdc(network, 272.0);
    }

    @Test
    void testHvdcLinesDisconnected() {
        Network network = Network.read("TestCaseHvdc.xiidm", getClass().getResourceAsStream("TestCaseHvdc.xiidm"));
        network.getHvdcLine("hvdc_line_FR_1_DE").getConverterStation1().getTerminal().disconnect();
        network.getHvdcLine("hvdc_line_FR_1_DE").getConverterStation2().getTerminal().disconnect();
        assertNetPositionForHvdc(network, 200.0);
    }

    @Test
    void testHvdcLinesNaN() {
        Network network = Network.read("TestCaseHvdc.xiidm", getClass().getResourceAsStream("TestCaseHvdc.xiidm"));
        network.getHvdcLine("hvdc_line_FR_1_DE").getConverterStation1().getTerminal().setP(Double.NaN);
        network.getHvdcLine("hvdc_line_FR_1_DE").getConverterStation2().getTerminal().setP(Double.NaN);
        assertNetPositionForHvdc(network, 200.0);
    }
}
