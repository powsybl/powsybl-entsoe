/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.glsk.nc;

import java.time.Instant;
import java.util.Arrays;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Optional;

/**
 * TimeSeriesInterpolationKind enumeration of the NC profiles: the interpolation of the values between two time points of
 * a time series. {@link #LINEAR} is not supported.
 * <ul>
 *     <li>{@code none}: no interpolation, the value of a time point holds from its time until the next time point;</li>
 *     <li>{@code next}: the value between two time points is the value of the next one, so the value of a time point holds
 *     back from the previous time point (excluded) up to its own time (included). After the last time point, its value holds.</li>
 * </ul>
 * The power bid schedules support both. The availability schedules only support {@code next}, which they read on their own
 * (see {@link NcRemedialActionAvailability}).
 *
 * @author Víctor Cardozo {@literal <victor.cardozo at artelys.com>}
 */
enum TimeSeriesInterpolationKind {
    NONE("none"),
    NEXT("next"),
    LINEAR("linear");

    private static final String URL = NcConstants.ENTSOE_NS_NC_URL + "#TimeSeriesInterpolationKind.";

    private final String value;

    TimeSeriesInterpolationKind(String value) {
        this.value = value;
    }

    /**
     * @return the URL of the literal in the profiles
     */
    @Override
    public String toString() {
        return URL + value;
    }

    /**
     * @param url the URL of a literal, as found in a profile
     * @return the interpolation kind, or empty if it is missing or not the URL of a TimeSeriesInterpolationKind literal
     */
    static Optional<TimeSeriesInterpolationKind> fromUrl(String url) {
        return Arrays.stream(values()).filter(kind -> kind.toString().equals(url)).findFirst();
    }

    /**
     * @return the time point in force at an instant, or {@code null} if there is none: with {@code none} the latest one at
     * or before the instant (none before the first time point); with {@code next} the first one at or after the instant (the
     * last one after the last time point)
     * @throws UnsupportedOperationException with {@code linear}, which is not supported
     */
    <T> Map.Entry<Instant, T> timePointAt(NavigableMap<Instant, T> timePoints, Instant instant) {
        return switch (this) {
            case NEXT -> {
                Map.Entry<Instant, T> next = timePoints.ceilingEntry(instant);
                yield next != null ? next : timePoints.lastEntry();
            }
            case NONE -> timePoints.floorEntry(instant);
            case LINEAR -> throw new UnsupportedOperationException("The interpolation kind linear is not supported");
        };
    }
}
