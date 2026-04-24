/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package com.powsybl.entsoe.utils;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public enum CapacityCalculationRegion {
    BALTIC("10Y1001C--00120B"),
    CENTRAL_EUROPE("10Y1001C--00145W"),
    CHANNEL("10Y1001C--000239"),
    CORE("10Y1001C--00059P"),
    HANSA("10Y1001C--00136X"),
    GREECE_ITALY("10Y1001C--00138T"),
    IRELAND_UK("10Y1001C--00022B"),
    ITALY_NORTH("10Y1001C--00137V"),
    NORDIC("10Y1001A1001A91G"),
    SELENE("10Y1001C--00139R"),
    SOUTH_WESTERN_EUROPE("10Y1001C--00095L");

    private final String eic;

    CapacityCalculationRegion(String eic) {
        this.eic = eic;
    }

    public String getEIC() {
        return eic;
    }

    public static CapacityCalculationRegion fromEIC(String eic) {
        return switch (eic) {
            case "10Y1001C--00120B" -> BALTIC;
            case "10Y1001C--00145W" -> CENTRAL_EUROPE;
            case "10Y1001C--000239" -> CHANNEL;
            case "10Y1001C--00059P" -> CORE;
            case "10Y1001C--00136X" -> HANSA;
            case "10Y1001C--00138T" -> GREECE_ITALY;
            case "10Y1001C--00022B" -> IRELAND_UK;
            case "10Y1001C--00137V" -> ITALY_NORTH;
            case "10Y1001A1001A91G" -> NORDIC;
            case "10Y1001C--00139R" -> SELENE;
            case "10Y1001C--00095L" -> SOUTH_WESTERN_EUROPE;
            default -> throw new IllegalArgumentException("No Capacity Calculation region found with EIC %s.".formatted(eic));
        };
    }
}
