/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.entsoe.cgmes.balances_adjustment.data_exchange;

/**
 * Electronic Data Interchange.
 * The exact business nature identifying the principal characteristic of a time series.
 *
 * https://www.entsoe.eu/publications/electronic-data-interchange-edi-library/
 *
 * @author Thomas Adam {@literal <tadam at silicom.fr>}
 */
public enum StandardBusinessType {

    /**
     * The aggregated netted external schedules for a given border.
     */
    B63("Aggregated netted external schedule"),
    /**
     * The AC position for a given area..
     */
    B64("Netted area AC position"),
    /**
     * The AC and DC position for a given area..
     */
    B65("Netted area position");

    private final String description;

    StandardBusinessType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
