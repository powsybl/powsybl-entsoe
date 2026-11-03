/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.glsk.nc;

import com.powsybl.glsk.api.GlskPoint;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static com.powsybl.glsk.nc.NcGlskTestUtil.importTestCase;
import static com.powsybl.glsk.nc.NcGlskTestUtil.warningsLoggedBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests of the shift method of the countertrade remedial actions, on the shift-method test case (one remedial action per
 * scenario, named after its strategy and shift method). It goes with the GLSK strategy: shared with
 * proportionalForGenerator, priority with explicitSchedule. The GLSK strategy prevails, a shift method that does not go
 * with it is only reported.
 *
 * @author Víctor Cardozo {@literal <victor.cardozo at artelys.com>}
 */
class NcGlskShiftMethodTest {

    private static NcGlskDocument document;
    private static List<String> warnings;

    @BeforeAll
    static void importTestCaseOnce() {
        warnings = warningsLoggedBy(NcGlskDatasetParser.class, () -> document = importTestCase("shift-method"));
    }

    /**
     * @return the warnings about a remedial action
     */
    private static List<String> warningsAbout(String remedialAction) {
        return warnings.stream().filter(warning -> warning.contains("remedial action " + remedialAction + " ")).toList();
    }

    private static GlskPoint pointOf(String remedialAction) {
        return document.getGlskPoints("zone-" + remedialAction).getFirst();
    }

    @ParameterizedTest
    @ValueSource(strings = {"ra-proportional-shared", "ra-explicit-priority"})
    void shiftMethodThatGoesWithTheStrategyIsNotReported(String remedialAction) {
        assertEquals(List.of(), warningsAbout(remedialAction));
    }

    @ParameterizedTest
    @CsvSource({"ra-proportional-priority, priority, proportionalForGenerator, shared",
        "ra-explicit-shared, shared, explicitSchedule, priority"})
    void shiftMethodThatDoesNotGoWithTheStrategyIsReported(String remedialAction, String shiftMethod, String strategy, String expected) {
        assertEquals(List.of("Countertrade remedial action " + remedialAction + " has shift method " + shiftMethod + " but its GLSK strategy "
            + strategy + " goes with " + expected + ": the GLSK strategy prevails"), warningsAbout(remedialAction));
    }

    @Test
    void unknownShiftMethodIsReported() {
        assertEquals(List.of("Countertrade remedial action ra-proportional-unknown has shift method unknownMethod but its GLSK strategy"
            + " proportionalForGenerator goes with shared: the GLSK strategy prevails"), warningsAbout("ra-proportional-unknown"));
    }

    @ParameterizedTest
    @CsvSource({"ra-proportional-missing, proportionalForGenerator, shared", "ra-explicit-missing, explicitSchedule, priority"})
    void missingShiftMethodIsReported(String remedialAction, String strategy, String expected) {
        assertEquals(List.of("Countertrade remedial action " + remedialAction + " has no shift method but its GLSK strategy " + strategy
            + " goes with " + expected), warningsAbout(remedialAction));
    }

    @Test
    void proportionalStrategyPrevailsOverShiftMethod() {
        assertEquals("B42", pointOf("ra-proportional-priority").getGlskShiftKeys().getFirst().getBusinessType());
    }

    @Test
    void explicitStrategyPrevailsOverShiftMethod() {
        assertEquals("B45", pointOf("ra-explicit-shared").getGlskShiftKeys().getFirst().getBusinessType());
    }

    @Test
    void shiftMethodOfAnUnsupportedStrategyIsNotReported() {
        // Only the unsupported strategy is reported, as the remedial action is ignored
        assertFalse(document.getZones().contains("zone-ra-unsupported-priority"));
        List<String> remedialActionWarnings = warningsAbout("ra-unsupported-priority");
        assertEquals(1, remedialActionWarnings.size());
        assertTrue(remedialActionWarnings.getFirst().contains("unsupported GLSK strategy"));
    }
}
