/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.glsk.nc;

import org.junit.jupiter.api.Test;
import org.threeten.extra.Interval;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests of the segments in which a timeline is cut: each boundary is a segment of its own, one nanosecond long, followed by the
 * gap up to the next boundary.
 *
 * @author Víctor Cardozo {@literal <victor.cardozo at artelys.com>}
 */
class NcTimeSegmentsTest {

    private static final Instant T0 = Instant.parse("2029-08-12T00:00:00Z");
    private static final Instant T1 = Instant.parse("2029-08-12T06:00:00Z");
    private static final Instant T2 = Instant.parse("2029-08-12T12:00:00Z");

    @Test
    void eachBoundaryIsASegmentOfItsOwnFollowedByTheGap() {
        List<Interval> segments = NcTimeSegments.of(List.of(T0, T1, T2));
        assertEquals(List.of(
            Interval.of(T0, T0.plusNanos(1)), Interval.of(T0.plusNanos(1), T1),
            Interval.of(T1, T1.plusNanos(1)), Interval.of(T1.plusNanos(1), T2)), segments);
    }

    @Test
    void theSegmentOfABoundaryContainsOnlyThatInstant() {
        Interval segment = NcTimeSegments.of(List.of(T0, T1)).getFirst();
        assertTrue(segment.contains(T0));
        assertFalse(segment.contains(T0.plusNanos(1)));
        assertFalse(segment.contains(T0.minusNanos(1)));
    }

    @Test
    void segmentsAreConsecutiveAndCoverTheTimeline() {
        List<Interval> segments = NcTimeSegments.of(List.of(T0, T1, T2));
        for (int i = 0; i < segments.size() - 1; i++) {
            assertEquals(segments.get(i).getEnd(), segments.get(i + 1).getStart());
        }
        assertEquals(T0, segments.getFirst().getStart());
        assertEquals(T2, segments.getLast().getEnd());
    }

    @Test
    void aGapOfOneNanosecondHasNoGapSegment() {
        List<Interval> segments = NcTimeSegments.of(List.of(T0, T0.plusNanos(1), T1));
        assertEquals(List.of(
            Interval.of(T0, T0.plusNanos(1)),
            Interval.of(T0.plusNanos(1), T0.plusNanos(2)), Interval.of(T0.plusNanos(2), T1)), segments);
    }

    @Test
    void lessThanTwoBoundariesGiveNoSegment() {
        assertEquals(List.of(), NcTimeSegments.of(List.of()));
        assertEquals(List.of(), NcTimeSegments.of(List.of(T0)));
    }
}
