/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.glsk.nc;

import com.powsybl.glsk.api.GlskPoint;
import com.powsybl.glsk.nc.NcGlskDatasetParser.CountertradeRAData;
import com.powsybl.glsk.nc.NcGlskDatasetParser.GlskScheduleData;
import com.powsybl.glsk.nc.NcGlskDatasetParser.PowerBidScheduleData;
import com.powsybl.glsk.nc.NcGlskDatasetParser.PriceAndPower;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.threeten.extra.Interval;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Optional;
import java.util.TreeSet;

/**
 * Builds the merit order (explicitSchedule strategy) of a countertrade remedial action from the power bid schedules of
 * the SIS profile.
 * <p>
 * Each bid has a direction (up or down), a resource and a price/power time series. For a given instant, the bids
 * in force are sorted by ascending price, separately for each direction, and each one becomes a step of the merit
 * order: a B45 shift key with the resource and its maximum (up) or minimum (down) reachable active power, whose
 * position is 1..N for the upward direction and -1..-N for the downward one.
 * <p>
 * The merit order changes over time, so one GLSK point is built for each period during which nothing changes: the
 * boundaries of the periods are the validity windows of the profiles and every time at which a price, a power or the
 * availability of the remedial action takes effect.
 * <p>
 * The participation factor does not change the merit order. The TSOs send it as 1 and it does not vary over time, so the
 * first one of the schedule of the resource is passed as it is to the resource of the step (1 if the schedule has none), and
 * a schedule with several different values is only reported.
 *
 * @author Víctor Cardozo {@literal <victor.cardozo at artelys.com>}
 */
final class NcGlskMeritOrderBuilder {

    private static final Logger LOGGER = LoggerFactory.getLogger(NcGlskMeritOrderBuilder.class);

    private static final double DEFAULT_PARTICIPATION_FACTOR = 1.0;

    private NcGlskMeritOrderBuilder() {
    }

    /**
     * @param remedialAction the countertrade remedial action, with a validity window
     * @param bids           the power bid schedules of the remedial action
     * @param availability   the availability of the remedial action
     * @return one point for each period with at least one bid in force, during which the remedial action is available,
     * within its validity window
     */
    static List<GlskPoint> build(CountertradeRAData remedialAction, Collection<PowerBidScheduleData> bids, NcRemedialActionAvailability availability) {
        List<PowerBidScheduleData> supportedBids = bids.stream().filter(bid -> isSupported(remedialAction, bid)).toList();
        if (supportedBids.isEmpty()) {
            return List.of();
        }

        List<Instant> boundaries = boundaries(remedialAction, supportedBids, availability);
        List<GlskPoint> points = new ArrayList<>();
        for (int i = 0; i < boundaries.size() - 1; i++) {
            Instant start = boundaries.get(i);
            Instant end = boundaries.get(i + 1);
            if (!availability.isAvailable(start)) {
                continue;
            }
            List<Step> steps = supportedBids.stream().map(bid -> resolveStep(bid, start)).flatMap(Optional::stream).toList();
            if (!steps.isEmpty()) {
                points.add(createPoint(remedialAction.biddingZone(), Interval.of(start, end), steps));
            }
        }
        return points;
    }

    /**
     * Checks that a bid can be used, logging why otherwise.
     */
    private static boolean isSupported(CountertradeRAData remedialAction, PowerBidScheduleData bid) {
        String reason = null;
        String bidName = bid.mRID() != null ? bid.mRID() : bid.uri();
        if (bid.direction() != BidDirectionKind.UP && bid.direction() != BidDirectionKind.DOWN) {
            reason = "its direction is " + bid.directionValue() + " but only up and down are supported";
        } else if (bid.validityStart() == null || bid.validityEnd() == null || !bid.validityEnd().isAfter(bid.validityStart())) {
            reason = "its profile has no valid validity window";
        } else if (bid.glskSchedules().size() != 1) {
            reason = "it must be bound to exactly one resource but is bound to " + bid.glskSchedules().size();
        } else if (bid.timePoints().isEmpty()) {
            reason = "it has no price and power time point";
        } else if (interpolation(bid).isEmpty()) {
            reason = "its interpolation kind " + bid.interpolationKindValue() + " is not supported (only none and next)";
        }
        if (reason != null) {
            LOGGER.warn("Power bid schedule {} of countertrade remedial action {} is ignored: {}", bidName, remedialAction.mRID(), reason);
            return false;
        }
        if (bid.glskSchedules().getFirst().participationFactors().values().stream().distinct().count() > 1) {
            LOGGER.warn("Power bid schedule {} of countertrade remedial action {} has several participation factors: the first one is used",
                bidName, remedialAction.mRID());
        }
        if (bid.timePoints().values().stream().anyMatch(NcGlskMeritOrderBuilder::isIncomplete)) {
            LOGGER.warn("Power bid schedule {} of countertrade remedial action {} has time points without price or power: they are ignored",
                bidName, remedialAction.mRID());
        }
        return true;
    }

    /**
     * A missing interpolation kind is read as none.
     */
    private static Optional<TimeSeriesInterpolationKind> interpolation(PowerBidScheduleData bid) {
        if (bid.interpolationKindValue() == null) {
            return Optional.of(TimeSeriesInterpolationKind.NONE);
        }
        return Optional.ofNullable(bid.interpolationKind()).filter(kind -> kind != TimeSeriesInterpolationKind.LINEAR);
    }

    private static boolean isIncomplete(PriceAndPower priceAndPower) {
        return priceAndPower.price() == null || priceAndPower.power() == null;
    }

    /**
     * The instants at which the merit order can change, within the validity window of the remedial action.
     */
    private static List<Instant> boundaries(CountertradeRAData remedialAction, List<PowerBidScheduleData> bids, NcRemedialActionAvailability availability) {
        NavigableSet<Instant> all = new TreeSet<>(availability.changeInstants());
        all.add(remedialAction.validityStart());
        all.add(remedialAction.validityEnd());
        for (PowerBidScheduleData bid : bids) {
            all.add(bid.validityStart());
            all.add(bid.validityEnd());
            all.addAll(bid.timePoints().keySet());
        }
        return new ArrayList<>(all.subSet(remedialAction.validityStart(), true, remedialAction.validityEnd(), true));
    }

    /**
     * Resolves what a bid is at a given instant: the price and power in force, and its resource. The bid is not
     * part of the merit order if its profile is not valid at this instant, if none of its price and power time points
     * has taken effect yet (with the interpolation next, the first one is in force before it), or if the time point in force has
     * no price or no power.
     */
    private static Optional<Step> resolveStep(PowerBidScheduleData bid, Instant instant) {
        if (instant.isBefore(bid.validityStart()) || !instant.isBefore(bid.validityEnd())) {
            return Optional.empty();
        }
        Map.Entry<Instant, PriceAndPower> timePoint = interpolation(bid).orElseThrow().timePointAt(bid.timePoints(), instant);
        if (timePoint == null || isIncomplete(timePoint.getValue())) {
            return Optional.empty();
        }
        GlskScheduleData schedule = bid.glskSchedules().getFirst();
        double participationFactor = schedule.participationFactors().isEmpty()
            ? DEFAULT_PARTICIPATION_FACTOR : schedule.participationFactors().firstEntry().getValue();
        return Optional.of(new Step(bid.uri(), bid.direction(), timePoint.getValue().price(), timePoint.getValue().power(),
            schedule.resource(), participationFactor));
    }

    private static GlskPoint createPoint(String zone, Interval interval, List<Step> steps) {
        List<NcGlskShiftKey> shiftKeys = new ArrayList<>();
        addShiftKeys(shiftKeys, steps, BidDirectionKind.UP, 1, zone, interval);
        addShiftKeys(shiftKeys, steps, BidDirectionKind.DOWN, -1, zone, interval);
        return new NcGlskPoint(zone, interval, shiftKeys);
    }

    /**
     * Adds the steps of a direction, sorted by ascending price (then by bid, to be deterministic): the cheapest one gets
     * the position 1 (or -1), the next one the position 2 (or -2), and so on.
     */
    private static void addShiftKeys(List<NcGlskShiftKey> shiftKeys, List<Step> steps, BidDirectionKind direction, int sign,
                                     String zone, Interval interval) {
        List<Step> sortedSteps = steps.stream()
            .filter(step -> direction == step.direction())
            .sorted(Comparator.comparingDouble(Step::price).thenComparing(Step::bidUri))
            .toList();
        for (int i = 0; i < sortedSteps.size(); i++) {
            Step step = sortedSteps.get(i);
            // The power of a bid is the maximum (up) or minimum (down) active power its resource can reach
            NcGlskRegisteredResource resource = direction == BidDirectionKind.UP
                ? new NcGlskRegisteredResource(step.resource(), step.participationFactor(), null, step.power())
                : new NcGlskRegisteredResource(step.resource(), step.participationFactor(), step.power(), null);
            shiftKeys.add(NcGlskShiftKey.meritOrderStep(interval, zone, sign * (i + 1), resource));
        }
    }

    /**
     * A bid, as it is at a given instant.
     */
    private record Step(String bidUri, BidDirectionKind direction, double price, double power, String resource, double participationFactor) {
    }
}
