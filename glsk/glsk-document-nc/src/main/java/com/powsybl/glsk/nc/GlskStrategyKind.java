/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.glsk.nc;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.HashMap;
import java.util.Map;

/**
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
    private final String value;
    private static final Map<String, GlskStrategyKind> CONSTANTS = new HashMap<>();

    static {
        for (GlskStrategyKind c : values()) {
            CONSTANTS.put(c.value, c);
        }
    }

    GlskStrategyKind(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return this.value;
    }

    @JsonValue
    public String value() {
        return this.value;
    }

    @JsonCreator
    public static GlskStrategyKind fromValue(String value) {
        GlskStrategyKind constant = CONSTANTS.get(value);
        if (constant == null) {
            throw new IllegalArgumentException(value);
        } else {
            return constant;
        }
    }

}
