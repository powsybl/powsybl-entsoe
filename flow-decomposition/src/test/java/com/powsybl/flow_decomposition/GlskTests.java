/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.flow_decomposition;

import com.powsybl.cgmes.conformity.CgmesConformity1Catalog;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.*;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowParameters;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.powsybl.flow_decomposition.TestUtil.importNetwork;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 */
class GlskTests {
    private static final double EPSILON = 1e-3;

    @Test
    void testThatGlskAreWellComputed() {
        String networkFileName = "NETWORK_SINGLE_LOAD_TWO_GENERATORS_WITH_COUNTRIES.uct";
        String genBe = "BGEN2 11_generator";
        String loadBe = "BLOAD 11_load";
        String genFr = "FGEN1 11_generator";
        Network network = importNetwork(networkFileName);
        GlskComputer glskComputer = new GlskComputer();
        Map<Country, Map<String, Double>> glsks = glskComputer.run(network);
        assertEquals(1.0, glsks.get(Country.FR).get(genFr), EPSILON);
        assertEquals(1.0, glsks.get(Country.BE).get(genBe), EPSILON);
        assertNull(glsks.get(Country.BE).get(loadBe));
    }
}
