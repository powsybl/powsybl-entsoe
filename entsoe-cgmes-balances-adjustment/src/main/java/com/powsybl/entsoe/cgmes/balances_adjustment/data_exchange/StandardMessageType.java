/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.entsoe.cgmes.balances_adjustment.data_exchange;

/**
 * Electronic Data Interchange.
 * The coded type of a document. The message type describes the principal characteristic of a document.
 * This enumeration is used in the XML instances based on IEC 62325.
 *
 *  https://www.entsoe.eu/publications/electronic-data-interchange-edi-library/
 *
 * @author Thomas Adam {@literal <tadam at silicom.fr>}
 */
public enum StandardMessageType {
    /**
     * Reporting information market document.
     * A document used to report  the information concerning aggregated netted external schedules,
     * aggregated netted external market schedules,
     * aggregated netted external TSO schedules,
     * compensation program schedules,
     * netted area position schedules and netted area AC position schedules to an interested party.
     */
    B19("Reporting information market document");

    private final String description;

    StandardMessageType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
