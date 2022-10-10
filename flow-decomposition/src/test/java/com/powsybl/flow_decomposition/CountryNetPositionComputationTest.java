/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.flow_decomposition;

import com.powsybl.flow_decomposition.computers.NetPositionComputer;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlow;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 */
class CountryNetPositionComputationTest {
    private static final double DOUBLE_TOLERANCE = 1e-3;

    @Test
    void testLines() {
        Network network = Importers.loadNetwork("testCase.xiidm", getClass().getResourceAsStream("testCase.xiidm"));
        Map<Country, Double> netPositions = NetPositionComputer.computeNetPositions(network);
        assertEquals(1000.0, netPositions.get(Country.FR), DOUBLE_TOLERANCE);
        assertEquals(1500.0, netPositions.get(Country.BE), DOUBLE_TOLERANCE);
        assertEquals(0.0, netPositions.get(Country.NL), DOUBLE_TOLERANCE);
        assertEquals(-2500.0, netPositions.get(Country.DE), DOUBLE_TOLERANCE);
        double sumAllNetPositions = netPositions.values().stream().mapToDouble(Double::doubleValue).sum();
        assertEquals(0.0, sumAllNetPositions, DOUBLE_TOLERANCE);
    }

    @Test
    void testDanglingLines() {
        Network network = Importers.loadNetwork("TestCaseDangling.xiidm", getClass().getResourceAsStream("TestCaseDangling.xiidm"));
        LoadFlow.run(network);
        Map<Country, Double> netPositions = NetPositionComputer.computeNetPositions(network);
        assertEquals(1000.0, netPositions.get(Country.FR), DOUBLE_TOLERANCE);
        assertEquals(2300.0, netPositions.get(Country.BE), DOUBLE_TOLERANCE);
        assertEquals(-500.0, netPositions.get(Country.NL), DOUBLE_TOLERANCE);
        assertEquals(-2800.0, netPositions.get(Country.DE), DOUBLE_TOLERANCE);
        double sumAllNetPositions = netPositions.values().stream().mapToDouble(Double::doubleValue).sum();
        assertEquals(0.0, sumAllNetPositions, DOUBLE_TOLERANCE);
    }

    @Test
    void testHvdcLines() {
        Network network = Importers.loadNetwork("TestCaseHvdc.xiidm", getClass().getResourceAsStream("TestCaseHvdc.xiidm"));
        Map<Country, Double> netPositions = NetPositionComputer.computeNetPositions(network);
        assertEquals(272.0, netPositions.get(Country.FR), DOUBLE_TOLERANCE);
        assertEquals(-272.0, netPositions.get(Country.DE), DOUBLE_TOLERANCE);
        double sumAllNetPositions = netPositions.values().stream().mapToDouble(Double::doubleValue).sum();
        assertEquals(0.0, sumAllNetPositions, DOUBLE_TOLERANCE);
    }

}
