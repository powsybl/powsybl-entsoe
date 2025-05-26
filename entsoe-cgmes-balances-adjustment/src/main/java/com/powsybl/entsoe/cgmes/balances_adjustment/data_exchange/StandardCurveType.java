/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.entsoe.cgmes.balances_adjustment.data_exchange;

/**
 * @deprecated This module has not either been maintained nor used. We will remove it soon. Please report on Slack if you are using it.
 * Electronic Data Interchange.
 * The type of curve being defined in the time series.
 *
 * https://www.entsoe.eu/publications/electronic-data-interchange-edi-library/
 *
 * @author Thomas Adam {@literal <tadam at silicom.fr>}
 */
@Deprecated(since = "2.14", forRemoval = true)
public enum StandardCurveType {

    /**
     * The curve is made of successive Intervals of time (Blocks) of constant duration (size), where the size of the Blocks is equal to the Resolution of the Period.
     */
    A01("Sequential fixed size block"),
    /**
     * The curve is made of successive instants of time (Points).
     */
    A02("Point"),
    /**
     * The curve is made of successive Intervals of time (Blocks) of variable duration (size), where the end date and end time of each Block are equal to the start date and start time of the next Interval.
     * For the last Block the end date and end time of the last Interval would be equal to EndDateTime of TimeInterval.
     */
    A03("Variable sized Block");

    private final String description;

    StandardCurveType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
