/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.glsk.nc;

import com.powsybl.glsk.api.GlskPoint;
import com.powsybl.glsk.api.GlskRegisteredResource;
import com.powsybl.glsk.api.GlskShiftKey;
import com.powsybl.glsk.commons.ZonalData;
import com.powsybl.iidm.modification.scalable.Scalable;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.test.EurostagTutorialExample1Factory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static com.powsybl.glsk.nc.NcGlskTestUtil.importTestCase;
import static com.powsybl.glsk.nc.NcGlskTestUtil.warningsLoggedBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests of the explicitSchedule strategy: the merit order built from the power bid schedules of the SIS profile. The
 * profiles are in the test cases merit-order (one remedial action per scenario, named "mo-..." and valid on 12 August 2029),
 * merit-order-sis-window, merit-order-ra-window, merit-order-no-common-validity and merit-order-resource-classes.
 *
 * @author Víctor Cardozo {@literal <victor.cardozo at artelys.com>}
 */
class NcGlskMeritOrderTest {

    private static final String START = "2029-08-12T00:00:00Z";
    private static final String NOON = "2029-08-12T12:00:00Z";
    private static final String END = "2029-08-13T00:00:00Z";

    private static NcGlskDocument document;
    private static List<String> warnings;

    @BeforeAll
    static void importTestCaseOnce() {
        warnings = warningsLoggedBy(NcGlskMeritOrderBuilder.class, () -> document = importTestCase("merit-order"));
    }

    private static String zone(String remedialAction) {
        return "zone-" + remedialAction;
    }

    private static List<String> warningsAbout(String bid) {
        return warnings.stream().filter(warning -> warning.contains("Power bid schedule pbs-" + bid + " ")).toList();
    }

    private static GlskPoint onlyPoint(NcGlskDocument document, String zone) {
        List<GlskPoint> points = document.getGlskPoints(zone);
        assertEquals(1, points.size());
        return points.getFirst();
    }

    private static GlskPoint onlyPoint(String remedialAction) {
        return onlyPoint(document, zone(remedialAction));
    }

    private static GlskPoint pointAt(String remedialAction, String instant) {
        return document.getGlskPoints(zone(remedialAction)).stream()
            .filter(point -> point.containsInstant(Instant.parse(instant))).findFirst().orElseThrow();
    }

    /**
     * Checks a step of the merit order: its position, its generating unit, and its maximum (up) or minimum (down) power.
     */
    private static void assertStep(GlskShiftKey shiftKey, String remedialAction, int position, String generatingUnit, Double minimumCapacity, Double maximumCapacity) {
        assertStep(shiftKey, remedialAction, position, generatingUnit, 1.0, minimumCapacity, maximumCapacity);
    }

    private static void assertStep(GlskShiftKey shiftKey, String remedialAction, int position, String generatingUnit, double participationFactor,
                                   Double minimumCapacity, Double maximumCapacity) {
        assertEquals("B45", shiftKey.getBusinessType());
        assertEquals("A04", shiftKey.getPsrType());
        assertEquals(zone(remedialAction), shiftKey.getSubjectDomainmRID());
        assertEquals(position, shiftKey.getMeritOrderPosition());
        assertEquals(position > 0 ? "A01" : "A02", shiftKey.getFlowDirection());
        assertEquals(1, shiftKey.getRegisteredResourceArrayList().size());
        GlskRegisteredResource resource = shiftKey.getRegisteredResourceArrayList().getFirst();
        assertEquals(generatingUnit, resource.getmRID());
        assertEquals(participationFactor, resource.getParticipationFactor());
        assertEquals(Optional.ofNullable(minimumCapacity), resource.getMinimumCapacity());
        assertEquals(Optional.ofNullable(maximumCapacity), resource.getMaximumCapacity());
    }

    private static void assertInterval(GlskPoint point, String start, String end) {
        assertEquals(Instant.parse(start), point.getPointInterval().getStart());
        assertEquals(Instant.parse(end), point.getPointInterval().getEnd());
    }

    @Test
    void upwardStepsAreSortedByAscendingPrice() {
        String ra = "mo-sorted-up";
        GlskPoint point = onlyPoint(ra);

        assertInterval(point, START, END);
        assertEquals(zone(ra), point.getSubjectDomainmRID());
        assertEquals(3, point.getGlskShiftKeys().size());
        assertStep(point.getGlskShiftKeys().get(0), ra, 1, "unit-2", null, 200.);
        assertStep(point.getGlskShiftKeys().get(1), ra, 2, "unit-3", null, 150.);
        assertStep(point.getGlskShiftKeys().get(2), ra, 3, "unit-1", null, 100.);
    }

    @Test
    void downwardStepsAreSortedByAscendingPrice() {
        String ra = "mo-sorted-down";
        GlskPoint point = onlyPoint(ra);

        assertEquals(2, point.getGlskShiftKeys().size());
        assertStep(point.getGlskShiftKeys().get(0), ra, -1, "unit-2", 300., null);
        assertStep(point.getGlskShiftKeys().get(1), ra, -2, "unit-1", 200., null);
    }

    @Test
    void upwardAndDownwardStepsAreIndependent() {
        String ra = "mo-up-down";
        GlskPoint point = onlyPoint(ra);

        assertEquals(3, point.getGlskShiftKeys().size());
        assertStep(point.getGlskShiftKeys().get(0), ra, 1, "unit-1", null, 100.);
        assertStep(point.getGlskShiftKeys().get(1), ra, 2, "unit-2", null, 200.);
        assertStep(point.getGlskShiftKeys().get(2), ra, -1, "unit-3", 50., null);
    }

    @Test
    void equalPricesAreOrderedByBid() {
        String ra = "mo-equal-prices";
        GlskPoint point = onlyPoint(ra);

        assertStep(point.getGlskShiftKeys().get(0), ra, 1, "unit-a", null, 200.);
        assertStep(point.getGlskShiftKeys().get(1), ra, 2, "unit-b", null, 100.);
    }

    @Test
    void meritOrderFollowsThePricesAndPowersInForce() {
        String ra = "mo-time-series";
        assertEquals(2, document.getGlskPoints(zone(ra)).size());

        // Until noon, B is the cheapest
        GlskPoint morning = pointAt(ra, START);
        assertInterval(morning, START, NOON);
        assertStep(morning.getGlskShiftKeys().get(0), ra, 1, "unit-b", null, 200.);
        assertStep(morning.getGlskShiftKeys().get(1), ra, 2, "unit-a", null, 100.);

        // From noon, B has a new price and a new power
        GlskPoint afternoon = pointAt(ra, NOON);
        assertInterval(afternoon, NOON, END);
        assertStep(afternoon.getGlskShiftKeys().get(0), ra, 1, "unit-a", null, 100.);
        assertStep(afternoon.getGlskShiftKeys().get(1), ra, 2, "unit-b", null, 250.);
    }

    @Test
    void bidContributesNothingBeforeItsFirstTimePoint() {
        String ra = "mo-bid-starts-later";
        assertEquals(2, document.getGlskPoints(zone(ra)).size());

        GlskPoint morning = pointAt(ra, START);
        assertEquals(1, morning.getGlskShiftKeys().size());
        assertStep(morning.getGlskShiftKeys().getFirst(), ra, 1, "unit-a", null, 100.);
        assertEquals(2, pointAt(ra, NOON).getGlskShiftKeys().size());
    }

    @Test
    void noPointBeforeTheFirstTimePointOfAnyBid() {
        GlskPoint point = onlyPoint("mo-first-point-at-noon");

        assertInterval(point, NOON, END);
        assertFalse(point.containsInstant(Instant.parse(START)));
    }

    @Test
    void timePointWithoutPriceOrPowerIsIgnored() {
        String ra = "mo-incomplete-time-point";
        GlskPoint afternoon = pointAt(ra, NOON);

        assertEquals(1, afternoon.getGlskShiftKeys().size());
        assertStep(afternoon.getGlskShiftKeys().getFirst(), ra, 1, "unit-a", null, 100.);
        assertEquals(1, warningsAbout(ra + "-b").size());
        assertTrue(warningsAbout(ra + "-b").getFirst().contains("time points without price or power"));
    }

    @Test
    void bidWithUnsupportedDirectionIsIgnored() {
        String ra = "mo-unsupported-direction";
        GlskPoint point = onlyPoint(ra);

        assertEquals(1, point.getGlskShiftKeys().size());
        assertStep(point.getGlskShiftKeys().getFirst(), ra, 1, "unit-a", null, 100.);
        assertTrue(warningsAbout(ra + "-b").getFirst().contains("its direction is upAndDown but only up and down are supported"));
        assertTrue(warningsAbout(ra + "-c").getFirst().contains("its direction is stable but only up and down are supported"));
        // A literal of another enumeration (RelativeDirectionKind.up) is not a BidDirectionKind literal
        assertTrue(warningsAbout(ra + "-d").getFirst().contains("its direction is up but only up and down are supported"));
    }

    @Test
    void bidWithoutResourceIsIgnored() {
        String ra = "mo-without-resource";
        GlskPoint point = onlyPoint(ra);

        assertEquals(1, point.getGlskShiftKeys().size());
        assertStep(point.getGlskShiftKeys().getFirst(), ra, 1, "unit-a", null, 100.);
        assertTrue(warningsAbout(ra + "-b").getFirst().contains("it must be bound to exactly one resource but is bound to 0"));
    }

    @Test
    void bidWithSeveralResourcesIsIgnored() {
        String ra = "mo-several-resources";
        GlskPoint point = onlyPoint(ra);

        assertEquals(1, point.getGlskShiftKeys().size());
        assertStep(point.getGlskShiftKeys().getFirst(), ra, 1, "unit-a", null, 100.);
        assertTrue(warningsAbout(ra + "-b").getFirst().contains("it must be bound to exactly one resource but is bound to 2"));
    }

    @Test
    void participationFactorIsPassedAsItIs() {
        String ra = "mo-participation-zero";
        GlskPoint point = onlyPoint(ra);

        // The unit of the bid b has a factor of 0: it is not excluded, and its factor is passed as it is
        assertEquals(2, point.getGlskShiftKeys().size());
        assertStep(point.getGlskShiftKeys().getFirst(), ra, 1, "unit-b", 0.0, null, 200.);
        assertStep(point.getGlskShiftKeys().get(1), ra, 2, "unit-a", null, 100.);
        assertEquals(0.0, point.getGlskShiftKeys().getFirst().getRegisteredResourceArrayList().getFirst().getParticipationFactor());
        assertEquals(1.0, point.getGlskShiftKeys().get(1).getRegisteredResourceArrayList().getFirst().getParticipationFactor());
        assertEquals(List.of(), warningsAbout(ra + "-a"));
        assertEquals(List.of(), warningsAbout(ra + "-b"));
    }

    @Test
    void severalParticipationFactorsAreReportedAndTheFirstOneIsUsed() {
        String ra = "mo-participation-changes";
        GlskPoint point = onlyPoint(ra);

        assertEquals(0.5, point.getGlskShiftKeys().getFirst().getRegisteredResourceArrayList().getFirst().getParticipationFactor());
        assertTrue(warningsAbout(ra).getFirst().contains("has several participation factors: the first one is used"));
    }

    @Test
    void bidWithoutParticipationFactorIsKeptWithAFactorOfOne() {
        String ra = "mo-participation-missing";
        GlskPoint point = onlyPoint(ra);

        assertEquals(1, point.getGlskShiftKeys().size());
        assertStep(point.getGlskShiftKeys().getFirst(), ra, 1, "unit-a", null, 100.);
        assertEquals(List.of(), warningsAbout(ra));
    }

    @Test
    void participationFactorTimeDoesNotChangeTheMeritOrder() {
        String ra = "mo-participation-from-noon";

        // The factor of the unit of the bid b takes effect at noon, which neither splits the period nor excludes the unit before
        GlskPoint point = onlyPoint(ra);
        assertEquals(2, point.getGlskShiftKeys().size());
        assertStep(point.getGlskShiftKeys().getFirst(), ra, 1, "unit-b", null, 200.);
        assertEquals(List.of(), warningsAbout(ra + "-b"));
    }

    @Test
    void sisValidityWindowLimitsTheMeritOrder() {
        NcGlskDocument sisWindow = importTestCase("merit-order-sis-window");

        GlskPoint point = onlyPoint(sisWindow, zone("ra-sis-window"));
        assertInterval(point, "2029-08-12T06:00:00Z", "2029-08-12T18:00:00Z");
        assertFalse(point.containsInstant(Instant.parse(START)));
        assertFalse(point.containsInstant(Instant.parse("2029-08-12T18:00:00Z")));
    }

    @Test
    void raValidityWindowLimitsTheMeritOrder() {
        NcGlskDocument raWindow = importTestCase("merit-order-ra-window");

        assertInterval(onlyPoint(raWindow, zone("ra-ra-window")), "2029-08-12T06:00:00Z", "2029-08-12T18:00:00Z");
    }

    @Test
    void profilesWithoutCommonValidityGiveNoMeritOrder() {
        NcGlskDocument noCommonValidity = importTestCase("merit-order-no-common-validity");

        assertTrue(noCommonValidity.getZones().isEmpty());
    }

    @Test
    void bidsOfAnotherRemedialActionAreIgnored() {
        assertFalse(document.getZones().contains(zone("mo-without-own-bids")));
    }

    @Test
    void schedulesWithTheSameMridAreNotMixedUp() {
        // The associations refer to the RDF identifier of the schedule, not to its mRID: two schedules with the same mRID are not mixed up
        String ra = "mo-same-schedule-mrid";
        GlskPoint point = onlyPoint(ra);

        assertEquals(2, point.getGlskShiftKeys().size());
        assertStep(point.getGlskShiftKeys().get(0), ra, 1, "unit-a", null, 100.);
        assertStep(point.getGlskShiftKeys().get(1), ra, 2, "unit-b", null, 200.);
    }

    @Test
    void bidsOfProportionalRemedialActionAreIgnored() {
        GlskPoint point = onlyPoint(document, "10YFR-RTE------C");

        assertEquals(1, point.getGlskShiftKeys().size());
        assertEquals("B42", point.getGlskShiftKeys().getFirst().getBusinessType());
        assertTrue(point.getGlskShiftKeys().getFirst().getRegisteredResourceArrayList().isEmpty());
    }

    @Test
    void meritOrderIsConvertedToScalable() {
        // GEN and GEN2 both produce 607 MW
        Network network = EurostagTutorialExample1Factory.createWithMoreGenerators();

        ZonalData<Scalable> zonalScalable = document.getZonalScalable(network, Instant.parse(NOON));
        Scalable scalable = zonalScalable.getData(zone("mo-scalable"));

        // Upward: GEN is activated first, up to 700 MW, then GEN2
        assertEquals(100., scalable.scale(network, 100.), 1e-3);
        assertEquals(700., network.getGenerator("GEN").getTargetP(), 1e-3);
        assertEquals(614., network.getGenerator("GEN2").getTargetP(), 1e-3);

        // Downward: GEN is activated first, down to 500 MW, then GEN2
        assertEquals(-250., scalable.scale(network, -250.), 1e-3);
        assertEquals(500., network.getGenerator("GEN").getTargetP(), 1e-3);
        assertEquals(564., network.getGenerator("GEN2").getTargetP(), 1e-3);
    }

    @Test
    void resourcesOfAllTheClassesAreRead() {
        List<String> resourceClassesWarnings = warningsLoggedBy(NcGlskMeritOrderBuilder.class, () -> {
            NcGlskDocument resourceClasses = importTestCase("merit-order-resource-classes");

            // GeneratingUnit, PowerElectronicsUnit, EnergyGroup, EnergyBlockOrder, HydroPump, ScheduleResource and EnergyConsumer
            GlskPoint point = resourceClasses.getGlskPoints("zone-resource-classes").getFirst();
            assertEquals(List.of("GEN", "GEN2", "energy-group", "energy-block-order", "hydro-pump", "schedule-resource", "LOAD"),
                point.getGlskShiftKeys().stream().map(shiftKey -> shiftKey.getRegisteredResourceArrayList().getFirst().getmRID()).toList());
        });

        // The bid whose schedule refers to two resources is ignored
        assertEquals(1, resourceClassesWarnings.size());
        assertTrue(resourceClassesWarnings.getFirst().contains("pbs-8"));
        assertTrue(resourceClassesWarnings.getFirst().contains("exactly one resource"));
    }
}
