/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.glsk.nc;

import com.powsybl.glsk.api.GlskPoint;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

import static com.powsybl.glsk.nc.NcGlskTestUtil.importTestCase;
import static com.powsybl.glsk.nc.NcGlskTestUtil.warningsLoggedBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests of the availability of the remedial actions. It is established from the three datasets considered together, in
 * this order of precedence (the last one prevails): the RA profile (normalAvailable), the SIS profile (availability
 * schedule) and the SSI profile (available). A GLSK is only built for the periods during which the remedial action is
 * available. The profiles are in the test case availability, valid on 12 August 2029.
 *
 * @author Víctor Cardozo {@literal <victor.cardozo at artelys.com>}
 */
class NcRemedialActionAvailabilityTest {

    private static final String DAY_START = "2029-08-12T00:00:00Z";
    private static final String DAY_END = "2029-08-13T00:00:00Z";

    private static NcGlskDocument document;
    private static List<String> warnings;

    @BeforeAll
    static void importTestCaseOnce() {
        warnings = warningsLoggedBy(NcGlskDatasetParser.class, () -> document = importTestCase("availability"));
    }

    private static String t(int hour) {
        return String.format("2029-08-12T%02d:00:00Z", hour);
    }

    /**
     * @return the periods of the GLSK of a remedial action, as "start/end" (from the earliest one)
     */
    private static List<String> periods(String remedialAction) {
        if (!document.getZones().contains("zone-" + remedialAction)) {
            return List.of();
        }
        return document.getGlskPoints("zone-" + remedialAction).stream()
            .sorted(Comparator.comparing(point -> point.getPointInterval().getStart()))
            .map(point -> point.getPointInterval().getStart() + "/" + point.getPointInterval().getEnd())
            .toList();
    }

    private static String period(String start, String end) {
        return Instant.parse(start) + "/" + Instant.parse(end);
    }

    private static List<String> warningsAbout(String remedialAction) {
        return warnings.stream().filter(warning -> warning.contains("remedial action " + remedialAction + " ")).toList();
    }

    @Test
    void availableRemedialActionHasAGlskOverItsWholeValidity() {
        assertEquals(List.of(period(DAY_START, DAY_END)), periods("av-available"));
        assertEquals(List.of(), warningsAbout("av-available"));
    }

    @Test
    void unavailableRemedialActionHasNoGlsk() {
        assertEquals(List.of(), periods("av-unavailable"));
        assertEquals(1, warningsAbout("av-unavailable").size());
        assertTrue(warningsAbout("av-unavailable").getFirst().contains("not available during its validity window"));
    }

    @Test
    void sisProfilePrevailsOverRaProfile() {
        // Not available in the RA profile, available in the SIS profile from 06:00
        assertEquals(List.of(period(t(6), DAY_END)), periods("av-sis-over-ra"));
    }

    @Test
    void sisAvailabilityScheduleSplitsTheGlsk() {
        // Available in the RA profile, unavailable in the SIS profile from 06:00 to 12:00
        assertEquals(List.of(period(DAY_START, t(6)), period(t(12), DAY_END)), periods("av-sis-schedule"));
    }

    @Test
    void availabilityOfAProfileThatIsNotAnSsiOneIsNotRead() {
        assertEquals(List.of(period(DAY_START, DAY_END)), periods("av-not-ssi"));
    }

    @Test
    void ssiProfilePrevailsOverRaProfile() {
        // Not available in the RA profile, available in the SSI profile from 06:00 to 18:00
        assertEquals(List.of(period(t(6), t(18))), periods("av-ssi-true-over-ra"));
    }

    @Test
    void ssiProfileCanMakeTheRemedialActionUnavailable() {
        // Available in the RA profile, not available in the SSI profile from 06:00 to 18:00
        assertEquals(List.of(period(DAY_START, t(6)), period(t(18), DAY_END)), periods("av-ssi-false-over-ra"));
    }

    @Test
    void ssiProfilePrevailsOverSisProfile() {
        // Unavailable the whole day in the SIS profile, available from 08:00 to 10:00 in the SSI profile
        assertEquals(List.of(period(t(8), t(10))), periods("av-ssi-over-sis"));
    }

    @Test
    void meritOrderIsSplitAroundTheUnavailablePeriod() {
        String remedialAction = "av-explicit-ssi-false";
        assertEquals(List.of(period(DAY_START, t(6)), period(t(18), DAY_END)), periods(remedialAction));

        // The merit order of each period has its step
        for (GlskPoint point : document.getGlskPoints("zone-" + remedialAction)) {
            assertEquals(1, point.getGlskShiftKeys().size());
            assertEquals("unit-a", point.getGlskShiftKeys().getFirst().getRegisteredResourceArrayList().getFirst().getmRID());
        }
        assertFalse(document.getGlskPoints("zone-" + remedialAction).stream()
            .anyMatch(point -> point.containsInstant(Instant.parse(t(12)))));
    }

    @Test
    void unavailableRemedialActionHasNoMeritOrder() {
        assertEquals(List.of(), periods("av-explicit-unavailable"));
        assertEquals(1, warningsAbout("av-explicit-unavailable").size());
        assertTrue(warningsAbout("av-explicit-unavailable").getFirst().contains("not available during its validity window"));
    }
}
