/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.balances_adjustment.util;

import com.powsybl.iidm.network.Network;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * @author Ameni Walha {@literal <ameni.walha at rte-france.com>}
 */
public class VoltageLevelsAreaTest {

    private Network testNetwork;
    private VoltageLevelsAreaFactory voltageLevelsArea;

    @Before
    public void setUp() {
        testNetwork = Network.read("testCase.xiidm", VoltageLevelsAreaTest.class.getResourceAsStream("/testCase.xiidm"));

        voltageLevelsArea = new VoltageLevelsAreaFactory("FFR1AA1", "DDE3AA1");

    }

    @Test
    public void testGetNetPosition() {
        List<Double> flows = new ArrayList<>();
        flows.add(testNetwork.getBranch("FFR1AA1  FFR3AA1  1").getTerminal1().getP());
        flows.add(testNetwork.getBranch("FFR2AA1  FFR3AA1  1").getTerminal1().getP());
        flows.add(testNetwork.getBranch("DDE1AA1  DDE3AA1  1").getTerminal2().getP());
        flows.add(testNetwork.getBranch("DDE2AA1  DDE3AA1  1").getTerminal2().getP());

        assertEquals(flows.stream().mapToDouble(f -> f).sum(), voltageLevelsArea.create(testNetwork).getNetPosition(), 1e-3);
    }

    @Test
    public void testSpecialDevices() {
        Network network = Network.read("testCaseSpecialDevices.xiidm", getClass().getResourceAsStream("/testCaseSpecialDevices.xiidm"));

        NetworkAreaFactory test3wtFactory = new VoltageLevelsAreaFactory("VOLTAGE_LEVEL_FR_225KV");
        assertEquals(0, test3wtFactory.create(network).getNetPosition(), 1e-3);

        NetworkAreaFactory testHvdcFactory = new VoltageLevelsAreaFactory("VOLTAGE_LEVEL_FR_225KV", "VOLTAGE_LEVEL_FR_400KV");
        assertEquals(100, testHvdcFactory.create(network).getNetPosition(), 1e-3);

        NetworkAreaFactory test400kVFactory = new VoltageLevelsAreaFactory("VOLTAGE_LEVEL_FR_400KV", "VOLTAGE_LEVEL_BE_400KV");
        assertEquals(100, test400kVFactory.create(network).getNetPosition(), 1e-3);

    }
}
