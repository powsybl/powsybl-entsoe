/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.balances_adjustment.util;

import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Joris Mancini <joris.mancini_externe at rte-france.com>
 */
class StaticCountryAreaTest {

    private static final double EPSILON = 1e-3;

    @Test
    void testGetNetPosition() {
        var network = Network.read("testCase.xiidm", getClass().getResourceAsStream("/testCase.xiidm"));

        var countryAreaFactoryFR = new CountryAreaFactory(true, Country.FR);
        var countryAreaFactoryES = new CountryAreaFactory(true, Country.ES);
        var countryAreaFactoryBE = new CountryAreaFactory(true, Country.BE);

        assertEquals(0, countryAreaFactoryES.create(network).getNetPosition(), EPSILON);
        assertEquals(1000, countryAreaFactoryFR.create(network).getNetPosition(), EPSILON);
        assertEquals(1500, countryAreaFactoryBE.create(network).getNetPosition(), EPSILON);
    }
}
