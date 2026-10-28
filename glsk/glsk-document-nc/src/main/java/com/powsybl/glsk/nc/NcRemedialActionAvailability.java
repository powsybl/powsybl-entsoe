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
import java.util.Comparator;
import java.util.List;
import java.util.NavigableSet;
import java.util.Optional;
import java.util.TreeSet;

/**
 * Availability of a countertrade remedial action over time, established from the three datasets considered together,
 * in this order of precedence (the last one prevails):
 * <ol>
 *     <li>the RA profile: {@code normalAvailable}, valid over the validity window of the remedial action;</li>
 *     <li>the SIS profile: the time points of its availability schedules, and only within the validity window of the profile
 *     they belong to. With the interpolation next, the value at an instant is the one of the first time point at or after it;</li>
 *     <li>the SSI profile: {@code available}, valid over the validity window of the profile.</li>
 * </ol>
 *
 * @author Víctor Cardozo {@literal <victor.cardozo at artelys.com>}
 */
final class NcRemedialActionAvailability {

    // Availability of the remedial action from the RA profile
    private final boolean normalAvailable;
    // Time points of the availability schedule from the SIS profile
    private final List<AvailabilityTimePoint> availabilityTimePoints = new ArrayList<>();
    // Availability of the remedial action from the SSI profile
    private final List<SsiAvailability> ssiAvailabilities = new ArrayList<>();

    /**
     * @param normalAvailable {@code normalAvailable} of the RA profile
     */
    NcRemedialActionAvailability(boolean normalAvailable) {
        this.normalAvailable = normalAvailable;
    }

    /**
     * Adds an {@code AvailabilityTimePoint} of an availability schedule of the SIS profile.
     *
     * @param atTime        time from which the availability takes effect
     * @param available     availability from that time
     * @param validity      validity window of the SIS profile the time point belongs to
     */
    void addAvailabilityTimePoint(Instant atTime, boolean available, Interval validity) {
        availabilityTimePoints.add(new AvailabilityTimePoint(atTime, available, validity));
    }

    /**
     * Adds the {@code RemedialAction.available} of the SSI profile.
     *
     * @param available availability over the validity window
     * @param validity  validity window of the SSI profile
     */
    void addSsiAvailability(boolean available, Interval validity) {
        ssiAvailabilities.add(new SsiAvailability(available, validity));
    }

    /**
     * @return whether the remedial action is available at an instant: the SSI profile prevails over the SIS profile,
     * which prevails over the RA profile
     */
    boolean isAvailable(Instant instant) {
        Optional<SsiAvailability> ssiAvailability = ssiAvailabilities.stream()
            .filter(value -> value.validity().contains(instant))
            .max(Comparator.comparing(value -> value.validity().getStart()));
        if (ssiAvailability.isPresent()) {
            return ssiAvailability.get().available();
        }
        List<AvailabilityTimePoint> inWindow = availabilityTimePoints.stream()
            .filter(timePoint -> timePoint.validity().contains(instant))
            .toList();
        // Interpolation next: the first time point at or after the instant, or the last one if there is none
        Optional<AvailabilityTimePoint> next = inWindow.stream().filter(timePoint -> !timePoint.atTime().isBefore(instant))
            .min(Comparator.comparing(AvailabilityTimePoint::atTime));
        if (next.isEmpty()) {
            next = inWindow.stream().max(Comparator.comparing(AvailabilityTimePoint::atTime));
        }
        return next.map(AvailabilityTimePoint::available).orElse(normalAvailable);
    }

    /**
     * @return the instants at which the availability can change
     */
    NavigableSet<Instant> changeInstants() {
        NavigableSet<Instant> instants = new TreeSet<>();
        ssiAvailabilities.forEach(value -> {
            instants.add(value.validity().getStart());
            instants.add(value.validity().getEnd());
        });
        availabilityTimePoints.forEach(timePoint -> {
            instants.add(timePoint.atTime());
            instants.add(timePoint.validity().getStart());
            instants.add(timePoint.validity().getEnd());
        });
        return instants;
    }

    /**
     * @return the periods, within the given interval, during which the remedial action is available. Consecutive periods
     * are merged.
     */
    List<Interval> availablePeriods(Instant start, Instant end) {
        NavigableSet<Instant> boundaries = new TreeSet<>(changeInstants().subSet(start, false, end, false));
        boundaries.add(start);
        boundaries.add(end);
        List<Instant> instants = new ArrayList<>(boundaries);

        List<Interval> periods = new ArrayList<>();
        Instant periodStart = null;
        for (int i = 0; i < instants.size() - 1; i++) {
            boolean available = isAvailable(instants.get(i));
            if (available && periodStart == null) {
                periodStart = instants.get(i);
            }
            if (!available && periodStart != null) {
                periods.add(Interval.of(periodStart, instants.get(i)));
                periodStart = null;
            }
        }
        if (periodStart != null) {
            periods.add(Interval.of(periodStart, end));
        }
        return periods;
    }

    /**
     * The {@code AvailabilityTimePoint} of the SIS profile, with the validity window of its profile.
     */
    private record AvailabilityTimePoint(Instant atTime, boolean available, Interval validity) {
    }

    /**
     * The {@code RemedialAction.available} of the SSI profile, with the validity window of the profile.
     */
    private record SsiAvailability(boolean available, Interval validity) {
    }
}
