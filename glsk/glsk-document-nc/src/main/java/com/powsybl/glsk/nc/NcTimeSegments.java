/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.glsk.nc;

import org.threeten.extra.Interval;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Cuts a timeline into segments on which the time series of the NC profiles are constant, whatever their interpolation kind.
 * <p>
 * The time series give values at instants, plus a rule for the gaps between them: with {@code none} the value at a time
 * point holds after it, with {@code next} the value of a time point holds before it, up to its time included. A GLSK point is
 * valid over an {@link Interval} {@code [start, end)}: a single closure cannot fit both rules. So each boundary <i>t</i>
 * gives two segments, both encoded as {@code [start, end)}:
 * <ul>
 *     <li>the instant <i>t</i> itself, as {@code [t, t + 1 ns)};</li>
 *     <li>the open gap up to the next boundary, as {@code [t + 1 ns, next)}.</li>
 * </ul>
 * One nanosecond is the resolution of {@link Instant}: {@code [t, t + 1 ns)} contains <i>t</i> and no other instant, so the
 * encoding is exact. Every series is constant on each segment if the boundaries contain all its time points, so it can be
 * evaluated at the start of the segment. Consecutive segments with the same content are expected to be merged by the caller.
 *
 * @author Víctor Cardozo {@literal <victor.cardozo at artelys.com>}
 */
final class NcTimeSegments {

    private static final long INSTANT_RESOLUTION_NANOS = 1;

    private NcTimeSegments() {
    }

    /**
     * @param boundaries sorted distinct instants, from the start to the end of the timeline
     * @return the segments, in order, covering {@code [first boundary, last boundary)}
     */
    static List<Interval> of(List<Instant> boundaries) {
        List<Interval> segments = new ArrayList<>();
        for (int i = 0; i < boundaries.size() - 1; i++) {
            Instant time = boundaries.get(i);
            Instant next = boundaries.get(i + 1);
            Instant afterTime = justAfter(time);
            segments.add(Interval.of(time, afterTime));
            if (afterTime.isBefore(next)) {
                segments.add(Interval.of(afterTime, next));
            }
        }
        return segments;
    }

    /**
     * @return the first instant after an instant, whose resolution is the nanosecond
     */
    static Instant justAfter(Instant instant) {
        return instant.plusNanos(INSTANT_RESOLUTION_NANOS);
    }
}
