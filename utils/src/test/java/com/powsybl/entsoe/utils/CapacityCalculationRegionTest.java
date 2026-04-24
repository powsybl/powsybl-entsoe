/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package com.powsybl.entsoe.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public class CapacityCalculationRegionTest {
    @Test
    void testCCRFromEIC() {
        assertEquals(CapacityCalculationRegion.BALTIC, CapacityCalculationRegion.fromEIC("10Y1001C--00120B"));
        assertEquals(CapacityCalculationRegion.CENTRAL_EUROPE, CapacityCalculationRegion.fromEIC("10Y1001C--00145W"));
        assertEquals(CapacityCalculationRegion.CHANNEL, CapacityCalculationRegion.fromEIC("10Y1001C--000239"));
        assertEquals(CapacityCalculationRegion.CORE, CapacityCalculationRegion.fromEIC("10Y1001C--00059P"));
        assertEquals(CapacityCalculationRegion.GREECE_ITALY, CapacityCalculationRegion.fromEIC("10Y1001C--00138T"));
        assertEquals(CapacityCalculationRegion.HANSA, CapacityCalculationRegion.fromEIC("10Y1001C--00136X"));
        assertEquals(CapacityCalculationRegion.IRELAND_UK, CapacityCalculationRegion.fromEIC("10Y1001C--00022B"));
        assertEquals(CapacityCalculationRegion.ITALY_NORTH, CapacityCalculationRegion.fromEIC("10Y1001C--00137V"));
        assertEquals(CapacityCalculationRegion.NORDIC, CapacityCalculationRegion.fromEIC("10Y1001A1001A91G"));
        assertEquals(CapacityCalculationRegion.SELENE, CapacityCalculationRegion.fromEIC("10Y1001C--00139R"));
        assertEquals(CapacityCalculationRegion.SOUTH_WESTERN_EUROPE, CapacityCalculationRegion.fromEIC("10Y1001C--00095L"));
    }

    @Test
    void testCCRFromNonExistingEIC() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> CapacityCalculationRegion.fromEIC("10YABCDEFGHIJKLM"));
        assertEquals("No Capacity Calculation region found with EIC 10YABCDEFGHIJKLM.", exception.getMessage());
    }
}
