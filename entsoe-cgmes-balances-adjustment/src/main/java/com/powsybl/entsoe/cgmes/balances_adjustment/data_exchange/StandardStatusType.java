/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.entsoe.cgmes.balances_adjustment.data_exchange;

/**
 * Electronic Data Interchange.
 * The condition or position of an object with regard to its standing.
 *
 * https://www.entsoe.eu/publications/electronic-data-interchange-edi-library/
 *
 * @author Thomas Adam {@literal <tadam at silicom.fr>}
 */
public enum StandardStatusType {

    /**
     * The document is in a non finalized state.
     */
    A01("Intermediate"),
    /**
     * The document is in a definitive state.
     */
    A02("Final");

    private final String description;

    StandardStatusType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
