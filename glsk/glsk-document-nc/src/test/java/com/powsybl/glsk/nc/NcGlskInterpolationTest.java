/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.glsk.nc;

import com.powsybl.glsk.api.GlskPoint;
import com.powsybl.glsk.api.GlskRegisteredResource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static com.powsybl.glsk.nc.NcGlskTestUtil.importTestCase;
import static com.powsybl.glsk.nc.NcGlskTestUtil.warningsLoggedBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests of the interpolation of the time series of the SIS profile. The availability schedules only support the kind next
 * (the value at an instant is the one of the first time point at or after it, and the last one after the last time point):
 * a missing kind is read as next, and none and linear are ignored with a warning. The power bid schedules support none (the
 * value of a time point holds until the next one) and next (the same rule as the availability schedules): a missing
 * kind is read as none, and linear is ignored with a warning. The profiles are in the test case interpolation, valid on
 * 12 August 2029.
 *
 * @author Víctor Cardozo {@literal <victor.cardozo at artelys.com>}
 */
class NcGlskInterpolationTest {

    private static final String DAY_START = "2029-08-12T00:00:00Z";
    private static final String DAY_END = "2029-08-13T00:00:00Z";

    private static NcGlskDocument document;
    private static List<String> warnings;

    @BeforeAll
    static void importTestCaseOnce() {
        // The availability is read by the parser, the bids by the builder of the merit order
        List<String> builderWarnings = new ArrayList<>();
        List<String> parserWarnings = warningsLoggedBy(NcGlskDatasetParser.class, () -> builderWarnings.addAll(
            warningsLoggedBy(NcGlskMeritOrderBuilder.class, () -> document = importTestCase("interpolation"))));
        warnings = new ArrayList<>(parserWarnings);
        warnings.addAll(builderWarnings);
    }

    private static String t(int hour) {
        return String.format("2029-08-12T%02d:00:00Z", hour);
    }

    private static List<GlskPoint> points(String remedialAction) {
        if (!document.getZones().contains("zone-" + remedialAction)) {
            return List.of();
        }
        return document.getGlskPoints("zone-" + remedialAction).stream()
            .sorted(Comparator.comparing(point -> point.getPointInterval().getStart()))
            .toList();
    }

    private static List<String> periods(String remedialAction) {
        return points(remedialAction).stream()
            .map(point -> point.getPointInterval().getStart() + "/" + point.getPointInterval().getEnd())
            .toList();
    }

    private static String period(String start, String end) {
        return Instant.parse(start) + "/" + Instant.parse(end);
    }

    private static List<String> warningsAbout(String remedialAction) {
        return warnings.stream().filter(warning -> warning.contains("remedial action " + remedialAction + " ")).toList();
    }

    /**
     * @return the maximum active power of the only step of a point
     */
    private static double power(GlskPoint point) {
        assertEquals(1, point.getGlskShiftKeys().size());
        GlskRegisteredResource resource = point.getGlskShiftKeys().getFirst().getRegisteredResourceArrayList().getFirst();
        return resource.getMaximumCapacity().orElseThrow();
    }

    @Test
    void availabilityWithNextInterpolationTakesTheTimePointAtOrAfterTheInstant() {
        // Time points at 06:00 (unavailable) and 18:00 (available): unavailable up to 06:00 (included, the time point itself
        // is in force at its time), then the value of the time point of 18:00, from which it is available
        assertEquals(List.of(period(t(18), DAY_END)), periods("ip-av-next"));
        assertEquals(List.of(), warningsAbout("ip-av-next"));
    }

    @Test
    void availabilityWithoutInterpolationKindIsReadAsNext() {
        assertEquals(periods("ip-av-next"), periods("ip-av-missing"));
        assertEquals(List.of(), warningsAbout("ip-av-missing"));
    }

    @Test
    void availabilityWithNoInterpolationIsIgnored() {
        // The schedule is ignored and the RA profile applies (available)
        assertEquals(List.of(period(DAY_START, DAY_END)), periods("ip-av-none"));
        assertTrue(warnings.stream().anyMatch(warning -> warning.contains("Availability schedule") && warning.contains("ip-av-none")
            && warning.contains("interpolation kind none is not supported (only next)")));
    }

    @Test
    void availabilityWithLinearInterpolationIsIgnored() {
        // The RA profile says not available, and the schedule (available at 06:00) is ignored
        assertEquals(List.of(), periods("ip-av-linear"));
        assertTrue(warnings.stream().anyMatch(warning -> warning.contains("Availability schedule") && warning.contains("ip-av-linear")
            && warning.contains("interpolation kind linear is not supported (only next)")));
    }

    @Test
    void bidWithNoInterpolationHoldsUntilTheNextTimePoint() {
        assertEquals(List.of(period(DAY_START, t(12)), period(t(12), DAY_END)), periods("ip-bid-none"));
        List<GlskPoint> points = points("ip-bid-none");
        assertEquals(100., power(points.get(0)));
        assertEquals(200., power(points.get(1)));
    }

    @Test
    void bidWithNextInterpolationTakesTheTimePointAtOrAfterTheInstant() {
        assertEquals(List.of(period(DAY_START, t(12)), period(t(12), DAY_END)), periods("ip-bid-next"));
        List<GlskPoint> points = points("ip-bid-next");
        // At 00:00 the time point of 00:00, at 12:00 the one of 12:00, which holds after it as the last one
        assertEquals(100., power(points.get(0)));
        assertEquals(200., power(points.get(1)));
    }

    @Test
    void bidWithNextInterpolationIsInForceBeforeItsFirstTimePoint() {
        assertEquals(List.of(period(DAY_START, t(6)), period(t(6), DAY_END)), periods("ip-bid-next-single"));
        points("ip-bid-next-single").forEach(point -> assertEquals(200., power(point)));
    }

    @Test
    void bidWithNoInterpolationIsNotInForceBeforeItsFirstTimePoint() {
        assertEquals(List.of(period(t(6), DAY_END)), periods("ip-bid-none-single"));
    }

    @Test
    void bidWithoutInterpolationKindIsReadAsNone() {
        assertEquals(periods("ip-bid-none"), periods("ip-bid-missing"));
        assertEquals(100., power(points("ip-bid-missing").getFirst()));
        assertEquals(List.of(), warningsAbout("ip-bid-missing"));
    }

    @Test
    void bidWithLinearInterpolationIsIgnored() {
        assertEquals(List.of(), periods("ip-bid-linear"));
        assertTrue(warnings.stream().anyMatch(warning -> warning.contains("pbs-ip-bid-linear")
            && warning.contains("interpolation kind linear is not supported (only none and next)")));
    }
}
