/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.glsk.cim;

import com.powsybl.glsk.commons.ZonalData;
import com.powsybl.iidm.modification.scalable.Scalable;
import com.powsybl.iidm.network.Network;
import com.powsybl.sensitivity.SensitivityVariableSet;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * FlowBased Glsk Values Provider Test
 *
 * @author Luc Di Gallo {@literal <luc.di-gallo at rte-france.com>}
 */
class CimGlskTest {

    private Network testNetwork;
    private Instant instant;

    @BeforeEach
    void setUp() {
        testNetwork = Network.read("testCase.xiidm", getClass().getResourceAsStream("/testCase.xiidm"));
        instant = Instant.parse("2018-08-28T22:00:00Z");
    }

    @Test
    void run() {
        ZonalData<SensitivityVariableSet> zonalGlsks = CimGlskDocument.importGlsk(getClass().getResourceAsStream("/GlskCountry.xml"))
            .getZonalGlsks(testNetwork, instant);
        Map<String, SensitivityVariableSet> map = zonalGlsks.getDataPerZone();
        Assertions.assertFalse(map.isEmpty());

        SensitivityVariableSet linearGlsk = zonalGlsks.getData("10YBE----------2");
        Assertions.assertFalse(linearGlsk.getVariables().isEmpty());
    }

    @Test
    void runWithInvalidCountry() {
        ZonalData<SensitivityVariableSet> zonalGlsks = CimGlskDocument.importGlsk(getClass().getResourceAsStream("/GlskCountry.xml"))
            .getZonalGlsks(testNetwork, instant);
        Assertions.assertNull(zonalGlsks.getData("fake-area"));
    }

    @Test
    void zonalScalableTest() {
        CimGlskDocument cimGlskDocument = CimGlskDocument.importGlsk(getClass().getResourceAsStream("/GlskB45CurveTypeA03test.xml"));
        assertEquals(1, cimGlskDocument.getZones().size());
        Instant instant1 = Instant.parse("2017-04-13T07:00:00Z");
        ZonalData<Scalable> zonalScalables = cimGlskDocument.getZonalScalable(testNetwork, instant1);
        assertEquals(1, zonalScalables.getDataPerZone().size());
        Scalable scalableFR = zonalScalables.getData("10YFR-RTE------C");
        assertEquals(1, scalableFR.filterInjections(testNetwork).size());
        assertEquals("FFR3AA1 _generator", scalableFR.filterInjections(testNetwork).get(0).getId());
    }
}
