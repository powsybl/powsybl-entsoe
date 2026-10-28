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
 * GLSKStrategyKind enumeration of the NC profiles: the strategy used to build the GLSK of a countertrade remedial action.
 * The importer only supports {@link #PROPORTIONAL_FOR_GENERATOR} and {@link #EXPLICIT_SCHEDULE}.
 *
 * @author Víctor Cardozo {@literal <victor.cardozo at artelys.com>}
 */
public enum GlskStrategyKind {

    EXPLICIT_INSTRUCTION("explicitInstruction"),
    EXPLICIT_SCHEDULE("explicitSchedule"),
    GENERATOR_FLAT("generatorFlat"),
    GENERATOR_PMAX("generatorPmax"),
    LOAD_FLAT("loadFlat"),
    PROPORTIONAL_FOR_GENERATOR("proportionalForGenerator"),
    PROPORTIONAL_FOR_GENERATOR_AND_LOAD("proportionalForGeneratorAndLoad"),
    PROPORTIONAL_FOR_LOAD("proportionalForLoad"),
    PROPORTIONAL_FOR_REMAINING_CAPACITY("proportionalForRemainingCapacity");

    private static final String URL = NcConstants.ENTSOE_NS_NC_URL + "#GLSKStrategyKind.";

    private final String value;

    GlskStrategyKind(String value) {
        this.value = value;
    }

    /**
     * @return the name of the literal, e.g. "explicitSchedule"
     */
    public String value() {
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
     * @return the strategy, or empty if it is missing or not the URL of a GLSKStrategyKind literal
     */
    public static Optional<GlskStrategyKind> fromUrl(String url) {
        return Arrays.stream(values()).filter(kind -> kind.toString().equals(url)).findFirst();
    }
}
