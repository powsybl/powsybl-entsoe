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
 * ShiftMethodKind enumeration of the NC profiles: how the shift of the net position of a bidding zone is distributed
 * among its resources. It goes with the GLSK strategy of the countertrade remedial action:
 * <ul>
 *     <li>{@link #SHARED} with proportionalForGenerator (proportional, or "implicit", GLSK);</li>
 *     <li>{@link #PRIORITY} with explicitSchedule (merit order, or "explicit", GLSK).</li>
 * </ul>
 *
 * @author Víctor Cardozo {@literal <victor.cardozo at artelys.com>}
 */
enum ShiftMethodKind {

    PRIORITY("priority"),
    SHARED("shared");

    private static final String URL = NcConstants.ENTSOE_NS_NC_URL + "#ShiftMethodKind.";

    private final String value;

    ShiftMethodKind(String value) {
        this.value = value;
    }

    /**
     * @return the name of the literal, e.g. "shared"
     */
    String value() {
        return value;
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
     * @return the shift method, or empty if it is missing or not the URL of a ShiftMethodKind literal
     */
    static Optional<ShiftMethodKind> fromUrl(String url) {
        return Arrays.stream(values()).filter(kind -> kind.toString().equals(url)).findFirst();
    }
}
