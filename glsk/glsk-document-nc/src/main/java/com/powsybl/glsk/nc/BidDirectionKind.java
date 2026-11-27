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
 * BidDirectionKind enumeration of the NC profiles: the direction of a power bid schedule. Only {@link #UP} and
 * {@link #DOWN} are supported by the importer.
 *
 * @author Víctor Cardozo {@literal <victor.cardozo at artelys.com>}
 */
enum BidDirectionKind {

    UP("up"),
    DOWN("down"),
    UP_AND_DOWN("upAndDown"),
    STABLE("stable");

    private static final String URL = NcConstants.ENTSOE_NS_NC_URL + "#BidDirectionKind.";

    private final String value;

    BidDirectionKind(String value) {
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
     * @return the direction, or empty if it is missing or not the URL of a BidDirectionKind literal
     */
    static Optional<BidDirectionKind> fromUrl(String url) {
        return Arrays.stream(values()).filter(kind -> kind.toString().equals(url)).findFirst();
    }
}
