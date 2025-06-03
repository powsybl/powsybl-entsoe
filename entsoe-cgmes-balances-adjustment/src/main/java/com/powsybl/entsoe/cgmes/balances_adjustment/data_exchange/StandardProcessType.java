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
 * Indicates the nature of process that the document addresses.
 *
 *  https://www.entsoe.eu/publications/electronic-data-interchange-edi-library/
 *
 * @author Thomas Adam {@literal <tadam at silicom.fr>}
 */
@Deprecated(since = "2.14", forRemoval = true)
public enum StandardProcessType {

    /**
     * The information provided concerns a day ahead process.
     */
    A01("Day ahead"),
    /**
     * The information provided concerns an intraday ahead process.
     */
    A18("Total intraday"),
    /**
     * Two days ahead
     */
    A45("Two days ahead");

    private final String description;

    StandardProcessType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
