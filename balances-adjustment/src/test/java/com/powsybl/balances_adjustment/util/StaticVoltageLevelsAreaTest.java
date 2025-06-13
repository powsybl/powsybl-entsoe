/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.balances_adjustment.util;

import com.powsybl.iidm.network.Network;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Joris Mancini <joris.mancini_externe at rte-france.com>
 */
class StaticVoltageLevelsAreaTest {

    private Network testNetwork;
    private VoltageLevelsAreaFactory voltageLevelsArea;

    @BeforeEach
    void setUp() {
        testNetwork = Network.read("testCase.xiidm", VoltageLevelsAreaTest.class.getResourceAsStream("/testCase.xiidm"));

        voltageLevelsArea = new VoltageLevelsAreaFactory(true, "FFR1AA1", "DDE3AA1");
    }

    @Test
    void testGetNetPosition() {
        assertEquals(-1000., voltageLevelsArea.create(testNetwork).getNetPosition(), 1e-3);
    }

    @Test
    void testSpecialDevices() {
        Network network = Network.read("testCaseSpecialDevices.xiidm", getClass().getResourceAsStream("/testCaseSpecialDevices.xiidm"));

        NetworkAreaFactory test3wtFactory = new VoltageLevelsAreaFactory(true, "VOLTAGE_LEVEL_FR_225KV");
        assertEquals(0, test3wtFactory.create(network).getNetPosition(), 1e-3);

        NetworkAreaFactory testHvdcFactory = new VoltageLevelsAreaFactory(true, "VOLTAGE_LEVEL_FR_225KV", "VOLTAGE_LEVEL_FR_400KV");
        assertEquals(100, testHvdcFactory.create(network).getNetPosition(), 1e-3);

        NetworkAreaFactory test400kVFactory = new VoltageLevelsAreaFactory(true, "VOLTAGE_LEVEL_FR_400KV", "VOLTAGE_LEVEL_BE_400KV");
        assertEquals(100, test400kVFactory.create(network).getNetPosition(), 1e-3);

    }
}
