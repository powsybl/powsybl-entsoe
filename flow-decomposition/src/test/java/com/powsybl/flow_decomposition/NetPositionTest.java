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

        LoadFlow.run(network, new LoadFlowParameters().setDc(false));
        assertEquals(0, tieLine.getG1(), 1e-10);
        assertEquals(0, tieLine.getG2(), 1e-10);
        assertEquals(0, tieLine.getB1(), 1e-10);
        assertEquals(0, tieLine.getB2(), 1e-10);
        assertEquals(-100.109, tieLine.getDanglingLine1().getBoundary().getP(), DOUBLE_TOLERANCE);
        assertEquals(100.109, tieLine.getDanglingLine2().getBoundary().getP(), DOUBLE_TOLERANCE);
        assertEquals(100.125, tieLine.getDanglingLine1().getTerminal().getP(), DOUBLE_TOLERANCE);
        assertEquals(-100.062, tieLine.getDanglingLine2().getTerminal().getP(), DOUBLE_TOLERANCE);
        assertEquals(0.063, tieLine.getDanglingLine1().getTerminal().getP() + tieLine.getDanglingLine2().getTerminal().getP(), DOUBLE_TOLERANCE);
        assertEquals(0.016, tieLine.getDanglingLine1().getBoundary().getP() + tieLine.getDanglingLine1().getTerminal().getP(), DOUBLE_TOLERANCE);
        assertEquals(0.047, tieLine.getDanglingLine2().getBoundary().getP() + tieLine.getDanglingLine2().getTerminal().getP(), DOUBLE_TOLERANCE);
        assertEquals(0, tieLine.getDanglingLine1().getBoundary().getP() + tieLine.getDanglingLine2().getBoundary().getP(), DOUBLE_TOLERANCE);
        Map<Country, Double> netPositions = NetPositionComputer.computeNetPositions(network);

        double sumAllNetPositions = netPositions.values().stream().mapToDouble(Double::doubleValue).sum();
        assertEquals(0.0, sumAllNetPositions, DOUBLE_TOLERANCE);
        assertEquals(-100.109, netPositions.get(Country.BE), DOUBLE_TOLERANCE);
        assertEquals(100.109, netPositions.get(Country.FR), DOUBLE_TOLERANCE);
    }

    @Test
    void testTieLineWithDifferentB() {
        String networkFileName = "NETWORK_SINGLE_LOAD_TWO_GENERATORS_WITH_XNODE.uct";

        Network network = importNetwork(networkFileName);
        TieLine tieLine = network.getTieLine("FGEN1 11 X     11 1 + X     11 BLOAD 11 1");
        tieLine.getDanglingLine1().setB(6.815E-3);
        tieLine.getDanglingLine2().setB(1E-9);

        LoadFlow.run(network, new LoadFlowParameters().setDc(false));
        assertEquals(0, tieLine.getG1(), 1e-10);
        assertEquals(0, tieLine.getG2(), 1e-10);
        assertEquals(0.00681, tieLine.getB1(), 1e-05);
        assertEquals(1e-9, tieLine.getB2(), 1e-10);
        assertEquals(-100.199, tieLine.getDanglingLine1().getBoundary().getP(), DOUBLE_TOLERANCE);
        assertEquals(100.199, tieLine.getDanglingLine2().getBoundary().getP(), DOUBLE_TOLERANCE);
        assertEquals(100.228, tieLine.getDanglingLine1().getTerminal().getP(), DOUBLE_TOLERANCE);
        assertEquals(-100.113, tieLine.getDanglingLine2().getTerminal().getP(), DOUBLE_TOLERANCE);
        assertEquals(0.114, tieLine.getDanglingLine1().getTerminal().getP() + tieLine.getDanglingLine2().getTerminal().getP(), DOUBLE_TOLERANCE);
        assertEquals(0.029, tieLine.getDanglingLine1().getBoundary().getP() + tieLine.getDanglingLine1().getTerminal().getP(), DOUBLE_TOLERANCE);
        assertEquals(0.085, tieLine.getDanglingLine2().getBoundary().getP() + tieLine.getDanglingLine2().getTerminal().getP(), DOUBLE_TOLERANCE);
        assertEquals(0, tieLine.getDanglingLine1().getBoundary().getP() + tieLine.getDanglingLine2().getBoundary().getP(), DOUBLE_TOLERANCE);
        Map<Country, Double> netPositions = NetPositionComputer.computeNetPositions(network);

        double sumAllNetPositions = netPositions.values().stream().mapToDouble(Double::doubleValue).sum();
        assertEquals(0, sumAllNetPositions, DOUBLE_TOLERANCE);
        assertEquals(-100.199, netPositions.get(Country.BE), DOUBLE_TOLERANCE);
        assertEquals(100.199, netPositions.get(Country.FR), DOUBLE_TOLERANCE);
    }

    @Test
    void testTieLineWithDifferentBFlip() {
        String networkFileName = "NETWORK_SINGLE_LOAD_TWO_GENERATORS_WITH_XNODE.uct";

        Network network = importNetwork(networkFileName);
        TieLine tieLine = network.getTieLine("FGEN1 11 X     11 1 + X     11 BLOAD 11 1");
        tieLine.getDanglingLine1().setB(1E-9);
        tieLine.getDanglingLine2().setB(6.815E-3);

        LoadFlow.run(network, new LoadFlowParameters().setDc(false));
        assertEquals(0, tieLine.getG1(), 1e-10);
        assertEquals(0, tieLine.getG2(), 1e-10);
        assertEquals(1e-9, tieLine.getB1(), 1e-10);
        assertEquals(0.00681, tieLine.getB2(), 1e-5);
        assertEquals(-101.919, tieLine.getDanglingLine1().getBoundary().getP(), DOUBLE_TOLERANCE);
        assertEquals(101.919, tieLine.getDanglingLine2().getBoundary().getP(), DOUBLE_TOLERANCE);
        assertEquals(102.151, tieLine.getDanglingLine1().getTerminal().getP(), DOUBLE_TOLERANCE);
        assertEquals(-101.223, tieLine.getDanglingLine2().getTerminal().getP(), DOUBLE_TOLERANCE);
        assertEquals(0.928, tieLine.getDanglingLine1().getTerminal().getP() + tieLine.getDanglingLine2().getTerminal().getP(), DOUBLE_TOLERANCE);
        assertEquals(0.232, tieLine.getDanglingLine1().getBoundary().getP() + tieLine.getDanglingLine1().getTerminal().getP(), DOUBLE_TOLERANCE);
        assertEquals(0.696, tieLine.getDanglingLine2().getBoundary().getP() + tieLine.getDanglingLine2().getTerminal().getP(), DOUBLE_TOLERANCE);
        assertEquals(0, tieLine.getDanglingLine1().getBoundary().getP() + tieLine.getDanglingLine2().getBoundary().getP(), DOUBLE_TOLERANCE);

        Map<Country, Double> netPositions = NetPositionComputer.computeNetPositions(network);

        double sumAllNetPositions = netPositions.values().stream().mapToDouble(Double::doubleValue).sum();
        assertEquals(0, sumAllNetPositions, DOUBLE_TOLERANCE);
        assertEquals(-101.919, netPositions.get(Country.BE), DOUBLE_TOLERANCE);
        assertEquals(101.919, netPositions.get(Country.FR), DOUBLE_TOLERANCE);
    }

    @Test
    void testTieLineSingleSideDisconnected() {
        String networkFileName = "NETWORK_SINGLE_LOAD_TWO_GENERATORS_WITH_XNODE.uct";

        Network network = importNetwork(networkFileName);
        TieLine tieLine = network.getTieLine("FGEN1 11 X     11 1 + X     11 BLOAD 11 1");
        tieLine.getDanglingLine1().setB(6.815E-3);
        tieLine.getDanglingLine2().setB(1E-9);
        tieLine.getTerminal1().disconnect();

        LoadFlow.run(network, new LoadFlowParameters().setDc(false));
        assertEquals(0.0, tieLine.getTerminal1().getP(), DOUBLE_TOLERANCE);
        assertEquals(7.426, tieLine.getTerminal2().getP(), DOUBLE_TOLERANCE);
        Map<Country, Double> netPositions = NetPositionComputer.computeNetPositions(network);

        double sumAllNetPositions = netPositions.values().stream().mapToDouble(Double::doubleValue).sum();
        assertEquals(1.856, sumAllNetPositions, DOUBLE_TOLERANCE);
        assertEquals(1.856, netPositions.get(Country.BE), DOUBLE_TOLERANCE);
        assertEquals(0, netPositions.get(Country.FR), DOUBLE_TOLERANCE);
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
        TieLine tieLine = network.getTieLine("XBF00011 BF000011 1 + XBF00011 FB000011 1");
        assertEquals(0, tieLine.getG1(), 1e-10);
        assertEquals(0, tieLine.getG2(), 1e-10);
        assertEquals(1e-3, tieLine.getB1(), 1e-10);
        assertEquals(1e-3, tieLine.getB2(), 1e-10);
        assertEquals(780.229, tieLine.getDanglingLine1().getBoundary().getP(), DOUBLE_TOLERANCE);
        assertEquals(-780.229, tieLine.getDanglingLine2().getBoundary().getP(), DOUBLE_TOLERANCE);
        assertEquals(-779.848, tieLine.getDanglingLine1().getTerminal().getP(), DOUBLE_TOLERANCE);
        assertEquals(780.610, tieLine.getDanglingLine2().getTerminal().getP(), DOUBLE_TOLERANCE);
        assertEquals(0.761, tieLine.getDanglingLine1().getTerminal().getP() + tieLine.getDanglingLine2().getTerminal().getP(), DOUBLE_TOLERANCE);
        assertEquals(0.381, tieLine.getDanglingLine1().getBoundary().getP() + tieLine.getDanglingLine1().getTerminal().getP(), DOUBLE_TOLERANCE);
        assertEquals(0.381, tieLine.getDanglingLine2().getBoundary().getP() + tieLine.getDanglingLine2().getTerminal().getP(), DOUBLE_TOLERANCE);
        assertEquals(0, tieLine.getDanglingLine1().getBoundary().getP() + tieLine.getDanglingLine2().getBoundary().getP(), DOUBLE_TOLERANCE);
        Map<Country, Double> netPositions = NetPositionComputer.computeNetPositions(network);
        assertEquals(-800.651, netPositions.get(Country.BE), DOUBLE_TOLERANCE);
        assertEquals(1401.519, netPositions.get(Country.FR), DOUBLE_TOLERANCE);
        assertEquals(-1400.860, netPositions.get(Country.DE), DOUBLE_TOLERANCE);
        assertEquals(-799.993, netPositions.values().stream().mapToDouble(Double::doubleValue).sum(), DOUBLE_TOLERANCE);
        assertEquals(-799.345, network.getDanglingLineStream(DanglingLineFilter.UNPAIRED).mapToDouble(danglingLine -> danglingLine.getTerminal().getP()).sum(), DOUBLE_TOLERANCE);
        assertEquals(799.993, network.getDanglingLineStream(DanglingLineFilter.UNPAIRED).mapToDouble(danglingLine -> danglingLine.getBoundary().getP()).sum(), DOUBLE_TOLERANCE);
        assertEquals(0, network.getDanglingLineStream(DanglingLineFilter.PAIRED).mapToDouble(danglingLine -> danglingLine.getBoundary().getP()).sum(), DOUBLE_TOLERANCE);

        tieLine.getTerminal1().disconnect(); // TODO is this really a good idea ?

        LoadFlow.run(network, new LoadFlowParameters().setDc(false));
        assertEquals(Double.NaN, tieLine.getDanglingLine1().getBoundary().getP(), DOUBLE_TOLERANCE);
        assertEquals(-0.016, tieLine.getDanglingLine2().getBoundary().getP(), DOUBLE_TOLERANCE); // TODO is this great ?
        assertEquals(0, tieLine.getDanglingLine1().getTerminal().getP(), DOUBLE_TOLERANCE);
        assertEquals(0.032, tieLine.getDanglingLine2().getTerminal().getP(), DOUBLE_TOLERANCE);
        assertEquals(0.032, tieLine.getDanglingLine1().getTerminal().getP() + tieLine.getDanglingLine2().getTerminal().getP(), DOUBLE_TOLERANCE);
        assertEquals(Double.NaN, tieLine.getDanglingLine1().getBoundary().getP() + tieLine.getDanglingLine1().getTerminal().getP(), DOUBLE_TOLERANCE);
        assertEquals(0.016, tieLine.getDanglingLine2().getBoundary().getP() + tieLine.getDanglingLine2().getTerminal().getP(), DOUBLE_TOLERANCE);
        assertEquals(Double.NaN, tieLine.getDanglingLine1().getBoundary().getP() + tieLine.getDanglingLine2().getBoundary().getP(), DOUBLE_TOLERANCE);
        Map<Country, Double> newNetPositions = NetPositionComputer.computeNetPositions(network);
        assertEquals(-800.783, newNetPositions.get(Country.BE), DOUBLE_TOLERANCE);
        assertEquals(1402.308, newNetPositions.get(Country.FR), DOUBLE_TOLERANCE);
        assertEquals(-1401.501, newNetPositions.get(Country.DE), DOUBLE_TOLERANCE);
        assertEquals(-799.977, newNetPositions.values().stream().mapToDouble(Double::doubleValue).sum(), DOUBLE_TOLERANCE);
        assertEquals(-799.345, network.getDanglingLineStream(DanglingLineFilter.UNPAIRED).mapToDouble(danglingLine -> danglingLine.getTerminal().getP()).sum(), DOUBLE_TOLERANCE);
        assertEquals(799.993, network.getDanglingLineStream(DanglingLineFilter.UNPAIRED).mapToDouble(danglingLine -> danglingLine.getBoundary().getP()).sum(), DOUBLE_TOLERANCE);
        assertEquals(-0.016, network.getDanglingLineStream(DanglingLineFilter.PAIRED).filter(danglingLine -> Double.isFinite(danglingLine.getBoundary().getP())).mapToDouble(danglingLine -> danglingLine.getBoundary().getP()).sum(), DOUBLE_TOLERANCE);
    }
}
