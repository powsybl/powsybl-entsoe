/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.glsk.ucte;

import com.powsybl.glsk.api.GlskPoint;
import com.powsybl.glsk.api.util.converters.GlskPointScalableConverter;
import com.powsybl.glsk.commons.ZonalData;
import com.powsybl.iidm.modification.scalable.Scalable;
import com.powsybl.iidm.network.Generator;
import com.powsybl.iidm.network.Network;
import com.powsybl.sensitivity.SensitivityVariableSet;
import org.junit.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * FlowBased Glsk Values Provider Test for Ucte format
 *
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 */
public class UcteGlskValueProviderTest {

    private static final double EPSILON = 0.0001;

    @Test
    public void testProvideOkUcteGlsk() {
        Network network = Network.read("testCase.xiidm", getClass().getResourceAsStream("/testCase.xiidm"));
        Instant instant = Instant.parse("2016-07-29T10:00:00Z");

        ZonalData<SensitivityVariableSet> ucteGlskProvider = UcteGlskDocument.importGlsk(getClass().getResourceAsStream("/20170322_1844_SN3_FR2_GLSK_test.xml"))
                .getZonalGlsks(network, instant);
        assertEquals(3, ucteGlskProvider.getData("10YFR-RTE------C").getVariables().size());
        assertEquals(0.3, ucteGlskProvider.getData("10YFR-RTE------C").getVariable("FFR1AA1 _generator").getWeight(), EPSILON);
    }

    @Test
    public void testProvideUcteGlskEmptyInstant() {
        Network network = Network.read("testCase.xiidm", getClass().getResourceAsStream("/testCase.xiidm"));
        Instant instant = Instant.parse("2020-07-29T10:00:00Z");

        ZonalData<SensitivityVariableSet> ucteGlskProvider = UcteGlskDocument.importGlsk(getClass().getResourceAsStream("/20170322_1844_SN3_FR2_GLSK_test.xml"))
                .getZonalGlsks(network, instant);

        assertTrue(ucteGlskProvider.getDataPerZone().isEmpty());
    }

    @Test
    public void testProvideUcteGlskUnknownCountry() {
        Network network = Network.read("testCase.xiidm", getClass().getResourceAsStream("/testCase.xiidm"));
        Instant instant = Instant.parse("2016-07-29T10:00:00Z");

        ZonalData<SensitivityVariableSet> ucteGlskProvider = UcteGlskDocument.importGlsk(getClass().getResourceAsStream("/20170322_1844_SN3_FR2_GLSK_test.xml"))
                .getZonalGlsks(network, instant);

        assertNull(ucteGlskProvider.getData("unknowncountry"));
    }

    @Test
    public void testProvideUcteGlskWithWrongFormat() {
        Network network = Network.read("testCase.xiidm", getClass().getResourceAsStream("/testCase.xiidm"));
        Instant instant = Instant.parse("2016-07-29T10:00:00Z");
        ZonalData<SensitivityVariableSet> ucteGlskProvider = UcteGlskDocument.importGlsk(getClass().getResourceAsStream("/GlskCountry.xml"))
                .getZonalGlsks(network, instant);
        assertTrue(ucteGlskProvider.getDataPerZone().isEmpty());
    }

    @Test
    public void testMultiGskSeries() {
        Network network = Network.read("testCase.xiidm", getClass().getResourceAsStream("/testCase.xiidm"));
        Instant instant = Instant.parse("2016-07-29T10:00:00Z");
        UcteGlskDocument ucteGlskDocument = UcteGlskDocument.importGlsk(getClass().getResourceAsStream("/TestMultiGskSeries.xml"));
        ZonalData<Scalable> ucteScalableProvider = ucteGlskDocument.getZonalScalable(network, instant);
        assertEquals(2, ucteScalableProvider.getData("10YFR-RTE------C").filterInjections(network).size());
        assertTrue(ucteScalableProvider.getData("10YFR-RTE------C").filterInjections(network).contains(network.getGenerator("FFR1AA1 _generator")));
        assertTrue(ucteScalableProvider.getData("10YFR-RTE------C").filterInjections(network).contains(network.getGenerator("FFR2AA1 _generator")));

        ZonalData<SensitivityVariableSet> ucteGlskProvider = ucteGlskDocument.getZonalGlsks(network, instant);
        assertEquals(2, ucteGlskProvider.getData("10YFR-RTE------C").getVariables().size());
        assertEquals(0.5, ucteGlskProvider.getData("10YFR-RTE------C").getVariable("FFR1AA1 _generator").getWeight(), EPSILON);
        assertEquals(0.5, ucteGlskProvider.getData("10YFR-RTE------C").getVariable("FFR2AA1 _generator").getWeight(), EPSILON);
    }

    @Test
    public void checkConversionOfMultiGskSeriesToScalable() {
        Network network = Network.read("testCase.xiidm", getClass().getResourceAsStream("/testCase.xiidm"));
        GlskPoint multiGlskSeries = UcteGlskDocument.importGlsk(getClass().getResourceAsStream("/ThreeGskSeries.xml")).getGlskPoints("10YFR-RTE------C").get(0);
        Scalable scalable = GlskPointScalableConverter.convert(network, multiGlskSeries);
        double generationBeforeScale = network.getGeneratorStream().mapToDouble(Generator::getTargetP).sum();
        assertEquals(24500.0, generationBeforeScale, 0.1);
        assertEquals(2000.0, network.getGenerator("FFR1AA1 _generator").getTargetP(), 0.1);
        assertEquals(2000.0, network.getGenerator("FFR2AA1 _generator").getTargetP(), 0.1);

        scalable.scale(network, 1000.0);

        double generationAfterScale = network.getGeneratorStream().mapToDouble(Generator::getTargetP).sum();
        assertEquals(25500.0, generationAfterScale, 0.1);
        assertEquals(2700.0, network.getGenerator("FFR1AA1 _generator").getTargetP(), 0.1);
        assertEquals(2300.0, network.getGenerator("FFR2AA1 _generator").getTargetP(), 0.1);
    }

    @Test
    public void testMultiShare() {
        Network network = Network.read("testCase.xiidm", getClass().getResourceAsStream("/testCase.xiidm"));
        UcteGlskDocument ucteGlskDocument = UcteGlskDocument.importGlsk(getClass().getResourceAsStream("/TestMultiShare2.xml"));

        List<Instant> instants = new ArrayList<>();
        instants.add(Instant.parse("2016-07-28T22:00:00Z"));
        instants.add(Instant.parse("2016-07-29T10:00:00Z"));
        instants.add(Instant.parse("2016-07-29T11:59:59Z"));
        for (Instant i : instants) {
            ZonalData<SensitivityVariableSet> ucteGlskProvidert = ucteGlskDocument.getZonalGlsks(network, i);
            assertEquals(3, ucteGlskProvidert.getData("10YFR-RTE------C").getVariables().size());
            assertEquals(0.3 * 1.0, ucteGlskProvidert.getData("10YFR-RTE------C").getVariable("FFR1AA1 _generator").getWeight(), EPSILON);
            assertEquals(0.7 * 0.2, ucteGlskProvidert.getData("10YFR-RTE------C").getVariable("FFR1AA1 _load").getWeight(), EPSILON);
            assertEquals(0.7 * 0.8, ucteGlskProvidert.getData("10YFR-RTE------C").getVariable("FFR2AA1 _load").getWeight(), EPSILON);
        }

        List<Instant> instants2 = new ArrayList<>();
        instants2.add(Instant.parse("2016-07-29T12:00:00Z"));
        instants2.add(Instant.parse("2016-07-29T16:00:00Z"));
        instants2.add(Instant.parse("2016-07-29T18:59:59Z"));

        for (Instant i : instants2) {
            ZonalData<SensitivityVariableSet> ucteGlskProvidert = ucteGlskDocument.getZonalGlsks(network, i);
            assertEquals(3, ucteGlskProvidert.getData("10YFR-RTE------C").getVariables().size());
            assertEquals(0.5 * 1.0, ucteGlskProvidert.getData("10YFR-RTE------C").getVariable("FFR1AA1 _generator").getWeight(), EPSILON);
            assertEquals(0.5 * 0.6, ucteGlskProvidert.getData("10YFR-RTE------C").getVariable("FFR1AA1 _load").getWeight(), EPSILON);
            assertEquals(0.5 * 0.4, ucteGlskProvidert.getData("10YFR-RTE------C").getVariable("FFR2AA1 _load").getWeight(), EPSILON);
        }

        List<Instant> instants3 = new ArrayList<>();
        instants3.add(Instant.parse("2016-07-29T19:00:00Z"));
        instants3.add(Instant.parse("2016-07-29T21:00:00Z"));
        instants3.add(Instant.parse("2016-07-29T21:59:59Z"));

        for (Instant i : instants3) {
            ZonalData<SensitivityVariableSet> ucteGlskProvidert = ucteGlskDocument.getZonalGlsks(network, i);
            assertEquals(3, ucteGlskProvidert.getData("10YFR-RTE------C").getVariables().size());
            assertEquals(0.3 * 1.0, ucteGlskProvidert.getData("10YFR-RTE------C").getVariable("FFR1AA1 _generator").getWeight(), EPSILON);
            assertEquals(0.7 * 0.3, ucteGlskProvidert.getData("10YFR-RTE------C").getVariable("FFR1AA1 _load").getWeight(), EPSILON);
            assertEquals(0.7 * 0.7, ucteGlskProvidert.getData("10YFR-RTE------C").getVariable("FFR2AA1 _load").getWeight(), EPSILON);
        }
    }

    @Test
    public void testZeroGsk() {
        Network network = Network.read("testCase.xiidm", getClass().getResourceAsStream("/testCase.xiidm"));
        Instant instant = Instant.parse("2016-07-29T10:00:00Z");
        UcteGlskDocument ucteGlskDocument = UcteGlskDocument.importGlsk(getClass().getResourceAsStream("/TestZeroGsk.xml"));
        ZonalData<SensitivityVariableSet> ucteGlskProvider = ucteGlskDocument.getZonalGlsks(network, instant);
        assertNull(ucteGlskProvider.getData("10YFR-RTE------C"));
    }

    @Test
    public void testZeroLsk() {
        Network network = Network.read("testCase.xiidm", getClass().getResourceAsStream("/testCase.xiidm"));
        Instant instant = Instant.parse("2016-07-29T10:00:00Z");
        UcteGlskDocument ucteGlskDocument = UcteGlskDocument.importGlsk(getClass().getResourceAsStream("/TestZeroLsk.xml"));
        ZonalData<SensitivityVariableSet> ucteGlskProvider = ucteGlskDocument.getZonalGlsks(network, instant);
        assertNull(ucteGlskProvider.getData("10YFR-RTE------C"));
    }
}
