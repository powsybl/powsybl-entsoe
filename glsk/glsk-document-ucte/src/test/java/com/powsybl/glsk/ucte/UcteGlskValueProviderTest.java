/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.glsk.ucte;

import com.powsybl.glsk.api.GlskPoint;
import com.powsybl.glsk.api.util.converters.GlskPointLinearGlskConverter;
import com.powsybl.glsk.api.util.converters.GlskPointScalableConverter;
import com.powsybl.glsk.commons.GlskException;
import com.powsybl.glsk.commons.ZonalData;
import com.powsybl.glsk.commons.ZonalDataChronology;
import com.powsybl.iidm.modification.scalable.Scalable;
import com.powsybl.iidm.network.Generator;
import com.powsybl.iidm.network.Injection;
import com.powsybl.iidm.network.Network;
import com.powsybl.sensitivity.SensitivityVariableSet;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * FlowBased Glsk Values Provider Test for Ucte format
 *
 * @author Baptiste Seguinot {@literal <baptiste.seguinot at rte-france.com>}
 * @author Peter Mitri {@literal <peter.mitri@rte-france.com>}
 */
class UcteGlskValueProviderTest {

    private static final double EPSILON = 0.0001;
    private static final String FRANCE = "10YFR-RTE------C";

    @Test
    void testProvideOkUcteGlsk() {
        Network network = Network.read("testCase.xiidm", getClass().getResourceAsStream("/testCase.xiidm"));
        Instant instant = Instant.parse("2016-07-29T10:00:00Z");

        ZonalData<SensitivityVariableSet> ucteGlskProvider = UcteGlskDocument.importGlsk(getClass().getResourceAsStream("/20170322_1844_SN3_FR2_GLSK_test.xml"))
                .getZonalGlsks(network, instant);
        assertEquals(3, ucteGlskProvider.getData("10YFR-RTE------C").getVariables().size());
        assertEquals(0.3, ucteGlskProvider.getData("10YFR-RTE------C").getVariable("FFR1AA1 _generator").getWeight(), EPSILON);
    }

    @Test
    void testProvideUcteGlskEmptyInstant() {
        Network network = Network.read("testCase.xiidm", getClass().getResourceAsStream("/testCase.xiidm"));
        Instant instant = Instant.parse("2020-07-29T10:00:00Z");

        ZonalData<SensitivityVariableSet> ucteGlskProvider = UcteGlskDocument.importGlsk(getClass().getResourceAsStream("/20170322_1844_SN3_FR2_GLSK_test.xml"))
                .getZonalGlsks(network, instant);

        assertTrue(ucteGlskProvider.getDataPerZone().isEmpty());
    }

    @Test
    void testProvideUcteGlskUnknownCountry() {
        Network network = Network.read("testCase.xiidm", getClass().getResourceAsStream("/testCase.xiidm"));
        Instant instant = Instant.parse("2016-07-29T10:00:00Z");

        ZonalData<SensitivityVariableSet> ucteGlskProvider = UcteGlskDocument.importGlsk(getClass().getResourceAsStream("/20170322_1844_SN3_FR2_GLSK_test.xml"))
                .getZonalGlsks(network, instant);

        assertNull(ucteGlskProvider.getData("unknowncountry"));
    }

    @Test
    void testProvideUcteGlskWithWrongFormat() {
        Network network = Network.read("testCase.xiidm", getClass().getResourceAsStream("/testCase.xiidm"));
        Instant instant = Instant.parse("2016-07-29T10:00:00Z");
        ZonalData<SensitivityVariableSet> ucteGlskProvider = UcteGlskDocument.importGlsk(getClass().getResourceAsStream("/GlskCountry.xml"))
                .getZonalGlsks(network, instant);
        assertTrue(ucteGlskProvider.getDataPerZone().isEmpty());
    }

    @Test
    void testMultiGskSeries() {
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
    void checkConversionOfMultiGskSeriesToScalable() {
        Network network = Network.read("testCase.xiidm", getClass().getResourceAsStream("/testCase.xiidm"));
        GlskPoint multiGlskSeries = UcteGlskDocument.importGlsk(getClass().getResourceAsStream("/ThreeGskSeries.xml")).getGlskPoints("10YFR-RTE------C").get(0);
        assertEquals(3, multiGlskSeries.getGlskShiftKeys().size());
        Scalable scalable = GlskPointScalableConverter.convert(network, multiGlskSeries);
        assertThrows(GlskException.class, () -> GlskPointLinearGlskConverter.convert(network, multiGlskSeries));
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
    void testOverlappingPointsThroughConsumers() {
        Network network = Network.read("testCase.xiidm", getClass().getResourceAsStream("/testCase.xiidm"));
        UcteGlskDocument document = UcteGlskDocument.importGlsk(getClass().getResourceAsStream("/OverlappingConsumerPoints.xml"));
        ZonalDataChronology<SensitivityVariableSet> chronology = document.getZonalGlsksChronology(network);

        Instant before = Instant.parse("2016-07-29T08:30:00Z");
        Instant overlapStart = Instant.parse("2016-07-29T09:00:00Z");
        Instant withinOverlap = Instant.parse("2016-07-29T09:30:00Z");
        Instant overlapEnd = Instant.parse("2016-07-29T10:00:00Z");
        Instant after = Instant.parse("2016-07-29T10:30:00Z");

        assertResources(document.getGlskPointsForInstant(before).get(FRANCE), "FFR1AA1 ", "FFR3AA1 ");
        assertConsumerResults(document, network, chronology, before, "FFR1AA1 _generator", "FFR3AA1 _generator");

        assertResources(document.getGlskPointsForInstant(overlapStart).get(FRANCE), "FFR1AA1 ", "FFR2AA1 ");
        assertConsumerResults(document, network, chronology, overlapStart, "FFR1AA1 _generator", "FFR2AA1 _generator");
        assertConsumerResults(document, network, chronology, withinOverlap, "FFR1AA1 _generator", "FFR2AA1 _generator");

        assertResources(document.getGlskPointsForInstant(overlapEnd).get(FRANCE), "FFR2AA1 ", "FFR3AA1 ");
        assertConsumerResults(document, network, chronology, overlapEnd, "FFR2AA1 _generator", "FFR3AA1 _generator");
        assertConsumerResults(document, network, chronology, after, "FFR2AA1 _generator", "FFR3AA1 _generator");
    }

    private void assertConsumerResults(UcteGlskDocument document, Network network,
                                      ZonalDataChronology<SensitivityVariableSet> chronology,
                                      Instant instant, String... expectedGenerators) {
        assertLinearResults(document.getZonalGlsks(network, instant), expectedGenerators);
        assertLinearResults(chronology.selectInstant(instant), expectedGenerators);

        List<Injection> scalableGenerators = document.getZonalScalable(network, instant)
                .getData(FRANCE).filterInjections(network);
        assertEquals(expectedGenerators.length, scalableGenerators.size());
        for (String generatorId : expectedGenerators) {
            assertTrue(scalableGenerators.contains(network.getGenerator(generatorId)));
        }
    }

    private void assertLinearResults(ZonalData<SensitivityVariableSet> zonalData, String... expectedGenerators) {
        SensitivityVariableSet sensitivityVariableSet = zonalData.getData(FRANCE);
        assertNotNull(sensitivityVariableSet);
        assertEquals(expectedGenerators.length, sensitivityVariableSet.getVariables().size());
        for (String generatorId : expectedGenerators) {
            assertEquals(0.5, sensitivityVariableSet.getVariable(generatorId).getWeight(), EPSILON);
        }
    }

    private void assertResources(UcteGlskPoint point, String... resourceIds) {
        assertEquals(List.of(resourceIds), point.getGlskShiftKeys().stream()
                .flatMap(key -> key.getRegisteredResourceArrayList().stream())
                .map(resource -> resource.getmRID()).toList());
    }

    @Test
    void testMultiShare() {
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
    void testZeroGsk() {
        Network network = Network.read("testCase.xiidm", getClass().getResourceAsStream("/testCase.xiidm"));
        Instant instant = Instant.parse("2016-07-29T10:00:00Z");
        UcteGlskDocument ucteGlskDocument = UcteGlskDocument.importGlsk(getClass().getResourceAsStream("/TestZeroGsk.xml"));
        ZonalData<SensitivityVariableSet> ucteGlskProvider = ucteGlskDocument.getZonalGlsks(network, instant);
        assertNull(ucteGlskProvider.getData("10YFR-RTE------C"));
    }

    @Test
    void testZeroLsk() {
        Network network = Network.read("testCase.xiidm", getClass().getResourceAsStream("/testCase.xiidm"));
        Instant instant = Instant.parse("2016-07-29T10:00:00Z");
        UcteGlskDocument ucteGlskDocument = UcteGlskDocument.importGlsk(getClass().getResourceAsStream("/TestZeroLsk.xml"));
        ZonalData<SensitivityVariableSet> ucteGlskProvider = ucteGlskDocument.getZonalGlsks(network, instant);
        assertNull(ucteGlskProvider.getData("10YFR-RTE------C"));
    }

    @Test
    void testWithBoundaryLines() {
        Network network = Network.read("testCaseWithDanglingLines.xiidm", getClass().getResourceAsStream("/testCaseWithDanglingLines.xiidm"));
        Instant instant = Instant.parse("2016-07-29T10:00:00Z");
        UcteGlskDocument ucteGlskDocument = UcteGlskDocument.importGlsk(getClass().getResourceAsStream("/GlskWithDanglingLines.xml"));
        ZonalData<SensitivityVariableSet> ucteGlskProvider = ucteGlskDocument.getZonalGlsks(network, instant);
        SensitivityVariableSet linearGlsk = ucteGlskProvider.getData("10YFR-RTE------C");

        assertNotNull(linearGlsk);
        assertEquals(5, linearGlsk.getVariables().size());
        assertEquals(0.2, linearGlsk.getVariable("FFR1AA1 _generator").getWeight(), EPSILON); // 2000/3000 * 0.3 = 0.2
        assertEquals(0.1, linearGlsk.getVariable("DDE3AA1  XNODE_1A 1").getWeight(), EPSILON); // 1000/3000 * 0.3 = 0.1
        assertEquals(0.1272, linearGlsk.getVariable("FFR1AA1 _load").getWeight(), EPSILON); // 1000/5500 * 0.7 = 0.1272
        assertEquals(0.4454, linearGlsk.getVariable("FFR2AA1 _load").getWeight(), EPSILON); // 3500/5500 * 0.7 = 0.4454
        assertEquals(0.1272, linearGlsk.getVariable("BBE2AA1  XNODE_1B 1").getWeight(), EPSILON); // 1000/5500 * 0.7 = 0.1272
    }
}
