/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.flow_decomposition;

import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.DanglingLineFilter;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.TieLine;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowParameters;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static com.powsybl.flow_decomposition.TestUtils.importNetwork;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 * @author Hugo Schindler{@literal <hugo.schindler at rte-france.com>}
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
    void testLinesSingleSideDisconnected() {
        Network network = Network.read("testCase.xiidm", getClass().getResourceAsStream("testCase.xiidm"));
        network.getBranch("NNL2AA1  BBE3AA1  1").getTerminal1().disconnect();
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

    @Test
    void testTieLine() {
        String networkFileName = "NETWORK_SINGLE_LOAD_TWO_GENERATORS_WITH_XNODE.uct";

        Network network = importNetwork(networkFileName);
        TieLine tieLine = network.getTieLine("FGEN1 11 X     11 1 + X     11 BLOAD 11 1");
        tieLine.getDanglingLine1().setR(0.5);
        tieLine.getDanglingLine2().setR(0.5);
        assertEquals(0, tieLine.getG1(), 1e-10);
        assertEquals(0, tieLine.getG2(), 1e-10);
        assertEquals(0, tieLine.getB1(), 1e-10);
        assertEquals(0, tieLine.getB2(), 1e-10);

        LoadFlow.run(network, new LoadFlowParameters().setDc(false));
        assertEquals(0.031, tieLine.getDanglingLine1().getBoundary().getP() + tieLine.getDanglingLine1().getTerminal().getP(), DOUBLE_TOLERANCE);
        assertEquals(0.031, tieLine.getDanglingLine2().getBoundary().getP() + tieLine.getDanglingLine2().getTerminal().getP(), DOUBLE_TOLERANCE);
        assertEquals(0, tieLine.getDanglingLine1().getBoundary().getP() + tieLine.getDanglingLine2().getBoundary().getP(), DOUBLE_TOLERANCE);

        Map<Country, Double> netPositions = NetPositionComputer.computeNetPositions(network);
        assertEquals(0.0, netPositions.values().stream().mapToDouble(Double::doubleValue).sum(), DOUBLE_TOLERANCE);
        assertEquals(-100.093, netPositions.get(Country.BE), DOUBLE_TOLERANCE);
        assertEquals(100.093, netPositions.get(Country.FR), DOUBLE_TOLERANCE);
    }

    @Test
    void testTieLineWithDifferentShunt() {
        String networkFileName = "NETWORK_SINGLE_LOAD_TWO_GENERATORS_WITH_XNODE.uct";

        Network network = importNetwork(networkFileName);
        TieLine tieLine = network.getTieLine("FGEN1 11 X     11 1 + X     11 BLOAD 11 1");
        tieLine.getDanglingLine1().setR(0.5);
        tieLine.getDanglingLine1().setB(1e-2);
        tieLine.getDanglingLine1().setG(1e-5);
        tieLine.getDanglingLine2().setR(0.5);
        tieLine.getDanglingLine2().setB(1e-9);
        tieLine.getDanglingLine2().setG(1e-9);

        LoadFlow.run(network, new LoadFlowParameters().setDc(false));
        assertEquals(2.764, tieLine.getDanglingLine1().getBoundary().getP() + tieLine.getDanglingLine1().getTerminal().getP(), DOUBLE_TOLERANCE);
        assertEquals(1.163, tieLine.getDanglingLine2().getBoundary().getP() + tieLine.getDanglingLine2().getTerminal().getP(), DOUBLE_TOLERANCE);
        assertEquals(0, tieLine.getDanglingLine1().getBoundary().getP() + tieLine.getDanglingLine2().getBoundary().getP(), DOUBLE_TOLERANCE);

        Map<Country, Double> netPositions = NetPositionComputer.computeNetPositions(network);
        assertEquals(0, netPositions.values().stream().mapToDouble(Double::doubleValue).sum(), DOUBLE_TOLERANCE);
        assertEquals(-100.362, netPositions.get(Country.BE), DOUBLE_TOLERANCE);
        assertEquals(100.362, netPositions.get(Country.FR), DOUBLE_TOLERANCE);
    }

    @Test
    void testTieLineWithDifferentShuntFlip() {
        String networkFileName = "NETWORK_SINGLE_LOAD_TWO_GENERATORS_WITH_XNODE.uct";

        Network network = importNetwork(networkFileName);
        TieLine tieLine = network.getTieLine("FGEN1 11 X     11 1 + X     11 BLOAD 11 1");
        tieLine.getDanglingLine1().setR(0.5);
        tieLine.getDanglingLine1().setB(1e-9);
        tieLine.getDanglingLine1().setG(1e-9);
        tieLine.getDanglingLine2().setR(0.5);
        tieLine.getDanglingLine2().setB(1e-2);
        tieLine.getDanglingLine2().setG(1e-5);

        LoadFlow.run(network, new LoadFlowParameters().setDc(false));
        assertEquals(1.154, tieLine.getDanglingLine1().getBoundary().getP() + tieLine.getDanglingLine1().getTerminal().getP(), DOUBLE_TOLERANCE);
        assertEquals(2.753, tieLine.getDanglingLine2().getBoundary().getP() + tieLine.getDanglingLine2().getTerminal().getP(), DOUBLE_TOLERANCE);
        assertEquals(0, tieLine.getDanglingLine1().getBoundary().getP() + tieLine.getDanglingLine2().getBoundary().getP(), DOUBLE_TOLERANCE);

        Map<Country, Double> netPositions = NetPositionComputer.computeNetPositions(network);
        assertEquals(0, netPositions.values().stream().mapToDouble(Double::doubleValue).sum(), DOUBLE_TOLERANCE);
        assertEquals(-103.973, netPositions.get(Country.BE), DOUBLE_TOLERANCE);
        assertEquals(103.973, netPositions.get(Country.FR), DOUBLE_TOLERANCE);
    }

    @Test
    void testTieLineWithDisconnectedDanglingLine() {
        String networkFileName = "19700101_0000_FO4_UX1.uct";
        Network network = importNetwork(networkFileName);
        network.getDanglingLineStream().forEach(danglingLine -> {
            danglingLine.setR(1E-1);
            danglingLine.setB(1E-3);
        });

        LoadFlow.run(network, new LoadFlowParameters().setDc(false));
        assertEquals(799.993, network.getDanglingLineStream(DanglingLineFilter.UNPAIRED).mapToDouble(danglingLine -> danglingLine.getBoundary().getP()).sum(), DOUBLE_TOLERANCE);
        assertEquals(0, network.getDanglingLineStream(DanglingLineFilter.PAIRED).mapToDouble(danglingLine -> danglingLine.getBoundary().getP()).sum(), DOUBLE_TOLERANCE);

        TieLine tieLine = network.getTieLine("XBF00011 BF000011 1 + XBF00011 FB000011 1");
        assertEquals(0.381, tieLine.getDanglingLine1().getBoundary().getP() + tieLine.getDanglingLine1().getTerminal().getP(), DOUBLE_TOLERANCE);
        assertEquals(0.381, tieLine.getDanglingLine2().getBoundary().getP() + tieLine.getDanglingLine2().getTerminal().getP(), DOUBLE_TOLERANCE);
        assertEquals(0, tieLine.getDanglingLine1().getBoundary().getP() + tieLine.getDanglingLine2().getBoundary().getP(), DOUBLE_TOLERANCE);

        Map<Country, Double> netPositions = NetPositionComputer.computeNetPositions(network);
        assertEquals(-800.651, netPositions.get(Country.BE), DOUBLE_TOLERANCE);
        assertEquals(1401.519, netPositions.get(Country.FR), DOUBLE_TOLERANCE);
        assertEquals(-1400.860, netPositions.get(Country.DE), DOUBLE_TOLERANCE);
        assertEquals(-799.993, netPositions.values().stream().mapToDouble(Double::doubleValue).sum(), DOUBLE_TOLERANCE);
    }
}
