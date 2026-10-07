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

    private final String value;

    ShiftMethodKind(String value) {
        this.value = value;
    }

    String value() {
        return value;
    }

    /**
     * @param value the name of a shift method, e.g. "shared"
     * @return the shift method, or empty if it is not a known ShiftMethodKind value
     */
    static Optional<ShiftMethodKind> fromValue(String value) {
        return Arrays.stream(values()).filter(kind -> kind.value.equals(value)).findFirst();
    }
}
