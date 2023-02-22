/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.flow_decomposition.glsk_provider;

import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static com.powsybl.flow_decomposition.TestUtils.importNetwork;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 */
class AutoGlskProviderTests {
    private static final double EPSILON = 1e-3;

    @Test
    void testThatGlskAreWellComputed() {
        String networkFileName = "NETWORK_SINGLE_LOAD_TWO_GENERATORS_WITH_COUNTRIES.uct";
        String genBe = "BGEN2 11_generator";
        String loadBe = "BLOAD 11_load";
        String genFr = "FGEN1 11_generator";
        Network network = importNetwork(networkFileName);
        AutoGlskProvider autoGlskProvider = new AutoGlskProvider();
        Map<Country, Map<String, Double>> glsks = autoGlskProvider.getGlsk(network);
        assertEquals(1.0, glsks.get(Country.FR).get(genFr), EPSILON);
        assertEquals(1.0, glsks.get(Country.BE).get(genBe), EPSILON);
        assertNull(glsks.get(Country.BE).get(loadBe));
    }
}
