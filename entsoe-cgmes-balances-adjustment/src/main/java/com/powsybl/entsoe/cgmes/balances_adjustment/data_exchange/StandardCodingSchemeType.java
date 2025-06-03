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
 * Codification scheme used to identify the coding scheme used for the set of coded values to identify specific objects.
 *
 * https://www.entsoe.eu/publications/electronic-data-interchange-edi-library/
 *
 * @author Thomas Adam {@literal <tadam at silicom.fr>}
 */
@Deprecated(since = "2.14", forRemoval = true)
public enum StandardCodingSchemeType {
    /**
     * EIC.
     * The coding scheme is the Energy Identification Coding Scheme (EIC), maintained by ENTSO-E.
     */
    A01("EIC"),
    /**
     * CGM.
     * The coding scheme used for Common Grid Model Exchange Standard (CGMES).
     */
    A02("CGM");

    private final String description;

    StandardCodingSchemeType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
