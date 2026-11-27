/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.glsk.nc;

import java.util.Arrays;
import java.util.Optional;

/**
 * Keywords (dcat:keyword) of the headers of the NC profiles the importer reads. The data of a profile is only read by
 * the queries of its keyword.
 *
 * @author Víctor Cardozo {@literal <victor.cardozo at artelys.com>}
 */
public enum NcKeyword {
    REMEDIAL_ACTION("RA"),
    STATE_INSTRUCTION_SCHEDULE("SIS"),
    STEADY_STATE_INSTRUCTION("SSI");

    private final String keyword;

    NcKeyword(String keyword) {
        this.keyword = keyword;
    }

    @Override
    public String toString() {
        return keyword;
    }

    /**
     * @param value the keyword of a profile header, e.g. "RA"
     * @return the keyword, or empty if it is not one the importer reads
     */
    public static Optional<NcKeyword> fromValue(String value) {
        return Arrays.stream(values()).filter(keyword -> keyword.keyword.equals(value)).findFirst();
    }
}
