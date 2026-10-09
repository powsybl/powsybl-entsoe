/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.glsk.nc;

import com.powsybl.glsk.api.GlskPoint;
import com.powsybl.glsk.api.GlskShiftKey;
import com.powsybl.glsk.commons.ZonalData;
import com.powsybl.iidm.modification.scalable.Scalable;
import com.powsybl.iidm.network.Network;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import static com.powsybl.glsk.nc.NcGlskTestUtil.importTestCase;
import static com.powsybl.glsk.nc.NcGlskTestUtil.warningsLoggedBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests of the proportionalForGenerator strategy, which needs only the RA profile: a single point per RA, valid over
 * the validity window of its profile, applying to all the generators of the country of the bidding zone.
 *
 * @author Víctor Cardozo {@literal <victor.cardozo at artelys.com>}
 */
class NcGlskProportionalTest {

    private static final String FR_ZONE = "10YFR-RTE------C";
    private static final String BE_ZONE = "10YBE----------2";
    private static final Instant FR_RA_START = Instant.parse("2029-08-11T23:00:00Z");
    private static final Instant FR_RA_END = Instant.parse("2029-08-12T23:00:00Z");

    private static NcGlskDocument importProportionalFr() {
        return importTestCase("proportional-fr");
    }

    @Test
    void proportionalForGeneratorPoint() {
        NcGlskDocument document = importProportionalFr();

        assertEquals(List.of(FR_ZONE), document.getZones());
        List<GlskPoint> points = document.getGlskPoints(FR_ZONE);
        assertEquals(1, points.size());

        GlskPoint point = points.getFirst();
        assertEquals(FR_ZONE, point.getSubjectDomainmRID());
        assertEquals(FR_RA_START, point.getPointInterval().getStart());
        assertEquals(FR_RA_END, point.getPointInterval().getEnd());

        // A single country-level shift key: B42 without registered resources, on generators
        assertEquals(1, point.getGlskShiftKeys().size());
        GlskShiftKey shiftKey = point.getGlskShiftKeys().getFirst();
        assertEquals("B42", shiftKey.getBusinessType());
        assertEquals("A04", shiftKey.getPsrType());
        assertEquals(FR_ZONE, shiftKey.getSubjectDomainmRID());
        assertEquals(1.0, shiftKey.getQuantity());
        assertTrue(shiftKey.getRegisteredResourceArrayList().isEmpty());
    }

    @Test
    void pointIsValidOverTheValidityWindowOfTheRaProfile() {
        GlskPoint point = importProportionalFr().getGlskPoints(FR_ZONE).getFirst();

        assertTrue(point.containsInstant(FR_RA_START));
        assertTrue(point.containsInstant(Instant.parse("2029-08-12T00:00:00Z")));
        assertFalse(point.containsInstant(FR_RA_END));
        assertFalse(point.containsInstant(FR_RA_START.minusSeconds(1)));
    }

    @Test
    void proportionalGlskIsConvertedToScalable() {
        Network network = Network.read("testCase.xiidm", getClass().getResourceAsStream("/networks/testCase.xiidm"));
        NcGlskDocument document = importProportionalFr();

        ZonalData<Scalable> zonalScalable = document.getZonalScalable(network, Instant.parse("2029-08-12T00:00:00Z"));
        assertEquals(Set.of(FR_ZONE), zonalScalable.getDataPerZone().keySet());

        // The 3 generators of FR have a target active power of 2000, 2000 and 3000 MW: they share a shift of 700 MW
        // as 2/7, 2/7 and 3/7. The generators of the other countries are not shifted.
        double done = zonalScalable.getData(FR_ZONE).scale(network, 700.);

        assertEquals(700., done, 1e-3);
        assertEquals(2200., network.getGenerator("FFR1AA1 _generator").getTargetP(), 1e-3);
        assertEquals(2200., network.getGenerator("FFR2AA1 _generator").getTargetP(), 1e-3);
        assertEquals(3300., network.getGenerator("FFR3AA1 _generator").getTargetP(), 1e-3);
        assertEquals(1500., network.getGenerator("BBE1AA1 _generator").getTargetP(), 1e-3);
    }

    @Test
    void noScalableOutsideTheValidityWindow() {
        Network network = Network.read("testCase.xiidm", getClass().getResourceAsStream("/networks/testCase.xiidm"));

        ZonalData<Scalable> zonalScalable = importProportionalFr().getZonalScalable(network, Instant.parse("2029-08-13T00:00:00Z"));

        assertTrue(zonalScalable.getDataPerZone().isEmpty());
    }

    /**
     * Imports a test case and returns the warnings logged by the parser meanwhile.
     */
    private static List<String> warningsOfImporting(String testCase, Consumer<NcGlskDocument> checks) {
        return warningsLoggedBy(NcGlskDatasetParser.class, () -> checks.accept(importTestCase(testCase)));
    }

    private static List<String> warningsAbout(List<String> warnings, String remedialAction) {
        return warnings.stream().filter(warning -> warning.contains(remedialAction)).toList();
    }

    @ParameterizedTest
    @ValueSource(strings = {"generatorFlat", "generatorPmax", "loadFlat", "proportionalForLoad", "proportionalForGeneratorAndLoad",
        "proportionalForRemainingCapacity", "explicitInstruction", "unknownStrategy"})
    void unsupportedStrategiesAreIgnored(String strategy) {
        List<String> warnings = warningsOfImporting("proportional", document ->
            assertFalse(document.getZones().contains("zone-" + strategy)));

        assertEquals(1, warningsAbout(warnings, "ra-" + strategy).size());
        assertTrue(warningsAbout(warnings, "ra-" + strategy).getFirst().contains("unsupported GLSK strategy"));
    }

    @Test
    void explicitScheduleWithoutScheduleIsIgnored() {
        List<String> warnings = warningsOfImporting("proportional", document ->
            assertFalse(document.getZones().contains("zone-explicit-without-bids")));

        assertEquals(1, warningsAbout(warnings, "ra-explicit-without-bids").size());
        assertTrue(warningsAbout(warnings, "ra-explicit-without-bids").getFirst().contains("no merit order could be built"));
    }

    @Test
    void availabilityOfTheRemedialActionDoesNotAffectItsGlsk() {
        // normalAvailable can be overridden by the SSI profile and it is the business of the remedial action import:
        // the GLSK only describes how a shift is distributed
        NcGlskDocument document = importTestCase("proportional");

        assertTrue(document.getZones().contains("zone-unavailable"));
        assertEquals(1, document.getGlskPoints("zone-unavailable").size());
    }

    @Test
    void supportedStrategyGivesOnePointOverTheValidityWindow() {
        NcGlskDocument document = importTestCase("proportional");

        assertEquals(Set.of("zone-proportional", "zone-unavailable"), Set.copyOf(document.getZones()));
        GlskPoint point = document.getGlskPoints("zone-proportional").getFirst();
        assertEquals(FR_RA_START, point.getPointInterval().getStart());
        assertEquals(FR_RA_END, point.getPointInterval().getEnd());
    }

    @ParameterizedTest
    @ValueSource(strings = {"proportional-no-window", "proportional-empty-window"})
    void profileWithoutValidValidityWindowIsIgnored(String testCase) {
        List<String> warnings = warningsOfImporting(testCase, document -> assertTrue(document.getZones().isEmpty()));

        assertEquals(1, warnings.size());
        assertTrue(warnings.getFirst().contains("no valid validity window"));
    }

    @Test
    void eachZoneHasTheValidityWindowOfItsOwnProfile() {
        NcGlskDocument document = importTestCase("proportional-two-zones");

        assertEquals(Set.of(FR_ZONE, BE_ZONE), Set.copyOf(document.getZones()));

        GlskPoint frPoint = document.getGlskPoints(FR_ZONE).getFirst();
        assertEquals(Instant.parse("2029-08-11T23:00:00Z"), frPoint.getPointInterval().getStart());
        assertEquals(Instant.parse("2029-08-12T23:00:00Z"), frPoint.getPointInterval().getEnd());

        GlskPoint bePoint = document.getGlskPoints(BE_ZONE).getFirst();
        assertEquals(BE_ZONE, bePoint.getSubjectDomainmRID());
        assertEquals(Instant.parse("2030-01-01T00:00:00Z"), bePoint.getPointInterval().getStart());
        assertEquals(Instant.parse("2030-01-02T00:00:00Z"), bePoint.getPointInterval().getEnd());

        // The validity of the document covers all its profiles
        assertEquals(Instant.parse("2029-08-11T23:00:00Z"), document.getInstantStart());
        assertEquals(Instant.parse("2030-01-02T00:00:00Z"), document.getInstantEnd());
    }
}
