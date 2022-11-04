/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.balances_adjustment.util;

import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Ameni Walha {@literal <ameni.walha at rte-france.com>}
 */
public class NetworkAreaTest {

    private Network testNetwork1;
    private NetworkAreaFactory countryAreaFR;

    private NetworkAreaFactory voltageLevelsArea1;

    @Before
    public void setUp() {
        testNetwork1 = Network.read("testCase.xiidm", NetworkAreaTest.class.getResourceAsStream("/testCase.xiidm"));

        voltageLevelsArea1 = new VoltageLevelsAreaFactory("FFR1AA1", "FFR3AA1");

        countryAreaFR = new CountryAreaFactory(Country.FR);

    }

    @Test
    public void testGetNetPosition() {
        assertEquals(countryAreaFR.create(testNetwork1).getNetPosition(), voltageLevelsArea1.create(testNetwork1).getNetPosition(), 1e-3);
    }

}
