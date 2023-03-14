/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.flow_decomposition;

import com.powsybl.iidm.network.Country;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 */
class DecomposedFlowTests {

    private static final DecomposedFlow DECOMPOSED_FLOW = new DecomposedFlow("dummy branch id", "dummy contingency id", Country.FR, Country.BE, 29, 24, 33, 25, 26, 58, Map.of("Loop Flow from BE", -1., "Loop Flow from FR", -9.));

    @Test
    void checkEquals() {
        DecomposedFlow sameDecomposedFlow = new DecomposedFlow("dummy branch id", "dummy contingency id", Country.FR, Country.BE, 29, 24, 33, 25, 26, 58, Map.of("Loop Flow from BE", -1., "Loop Flow from FR", -9.));
        DecomposedFlow differentDecomposedFlow1 = new DecomposedFlow("other dummy branch id", "dummy contingency id", Country.FR, Country.BE, 29, 24, 33, 25, 26, 58, Map.of("Loop Flow from BE", -1., "Loop Flow from FR", -9.));
        DecomposedFlow differentDecomposedFlow2 = new DecomposedFlow("dummy branch id", "other dummy contingency id", Country.FR, Country.BE, 29, 24, 33, 25, 26, 58, Map.of("Loop Flow from BE", -1., "Loop Flow from FR", -9.));
        DecomposedFlow differentDecomposedFlow3 = new DecomposedFlow("dummy branch id", "dummy contingency id", Country.DE, Country.BE, 29, 24, 33, 25, 26, 58, Map.of("Loop Flow from BE", -1., "Loop Flow from FR", -9.));
        DecomposedFlow differentDecomposedFlow4 = new DecomposedFlow("dummy branch id", "dummy contingency id", Country.FR, Country.DK, 29, 24, 33, 25, 26, 58, Map.of("Loop Flow from BE", -1., "Loop Flow from FR", -9.));
        DecomposedFlow differentDecomposedFlow5 = new DecomposedFlow("dummy branch id", "dummy contingency id", Country.FR, Country.BE, 30, 24, 33, 25, 26, 58, Map.of("Loop Flow from BE", -1., "Loop Flow from FR", -9.));
        DecomposedFlow differentDecomposedFlow6 = new DecomposedFlow("dummy branch id", "dummy contingency id", Country.FR, Country.BE, 29, 25, 33, 25, 26, 58, Map.of("Loop Flow from BE", -1., "Loop Flow from FR", -9.));
        DecomposedFlow differentDecomposedFlow7 = new DecomposedFlow("dummy branch id", "dummy contingency id", Country.FR, Country.BE, 29, 24, 34, 25, 26, 58, Map.of("Loop Flow from BE", -1., "Loop Flow from FR", -9.));
        DecomposedFlow differentDecomposedFlow8 = new DecomposedFlow("dummy branch id", "dummy contingency id", Country.FR, Country.BE, 29, 24, 33, 26, 26, 58, Map.of("Loop Flow from BE", -1., "Loop Flow from FR", -9.));
        DecomposedFlow differentDecomposedFlow9 = new DecomposedFlow("dummy branch id", "dummy contingency id", Country.FR, Country.BE, 29, 24, 33, 25, 27, 58, Map.of("Loop Flow from BE", -1., "Loop Flow from FR", -9.));
        DecomposedFlow differentDecomposedFlow10 = new DecomposedFlow("dummy branch id", "dummy contingency id", Country.FR, Country.BE, 29, 24, 33, 25, 26, 59, Map.of("Loop Flow from BE", -1., "Loop Flow from FR", -9.));
        DecomposedFlow differentDecomposedFlow11 = new DecomposedFlow("dummy branch id", "dummy contingency id", Country.FR, Country.BE, 29, 24, 33, 25, 26, 58, Map.of("Loop Flow from BE", 1., "Loop Flow from FR", -9.));
        DecomposedFlow differentDecomposedFlow12 = new DecomposedFlow("dummy branch id", "dummy contingency id", Country.FR, Country.BE, 29, 24, 33, 25, 26, 58, Map.of("Loop Flow from BE", -1., "Loop Flow from FR", -4.));
        DecomposedFlow differentDecomposedFlow13 = new DecomposedFlow("dummy branch id", "dummy contingency id", Country.FR, Country.BE, 29, 24, 33, 25, 26, 58, Collections.emptyMap());
        DecomposedFlow differentDecomposedFlow14 = new DecomposedFlow("dummy branch id", "dummy contingency id", Country.FR, Country.BE, 29, 24, 33, 25, 26, 58, Map.of("Loop Flow from NL", -4.));
        DecomposedFlow differentDecomposedFlow15 = new DecomposedFlow("dummy branch id", "dummy contingency id", Country.FR, Country.BE, 29, 24, 33, 25, 26, 58, Map.of("Loop Flow from BE", -1., "Loop Flow from FR", -9., "Loop Flow from NL", -4.));
        assertEquals(DECOMPOSED_FLOW, DECOMPOSED_FLOW);
        assertEquals(DECOMPOSED_FLOW, sameDecomposedFlow);
        assertNotEquals(DECOMPOSED_FLOW, differentDecomposedFlow1);
        assertNotEquals(DECOMPOSED_FLOW, differentDecomposedFlow2);
        assertNotEquals(DECOMPOSED_FLOW, differentDecomposedFlow3);
        assertNotEquals(DECOMPOSED_FLOW, differentDecomposedFlow4);
        assertNotEquals(DECOMPOSED_FLOW, differentDecomposedFlow5);
        assertNotEquals(DECOMPOSED_FLOW, differentDecomposedFlow6);
        assertNotEquals(DECOMPOSED_FLOW, differentDecomposedFlow7);
        assertNotEquals(DECOMPOSED_FLOW, differentDecomposedFlow8);
        assertNotEquals(DECOMPOSED_FLOW, differentDecomposedFlow9);
        assertNotEquals(DECOMPOSED_FLOW, differentDecomposedFlow10);
        assertNotEquals(DECOMPOSED_FLOW, differentDecomposedFlow11);
        assertNotEquals(DECOMPOSED_FLOW, differentDecomposedFlow12);
        assertNotEquals(DECOMPOSED_FLOW, differentDecomposedFlow13);
        assertNotEquals(DECOMPOSED_FLOW, differentDecomposedFlow14);
        assertNotEquals(DECOMPOSED_FLOW, differentDecomposedFlow15);
        assertNotEquals(1, DECOMPOSED_FLOW);
        assertNotEquals(null, DECOMPOSED_FLOW);
    }

    @Test
    void checkHash() {
        assertNotEquals(0, DECOMPOSED_FLOW.hashCode());
    }

    @Test
    void checkToString() {
        assertEquals("branchId: dummy branch id, contingencyId: dummy contingency id, decomposition: {Allocated Flow=33.0, Internal Flow=58.0, Loop Flow from BE=-1.0, Loop Flow from FR=-9.0, PST Flow=26.0, Reference AC Flow=29.0, Reference DC Flow=24.0, Xnode Flow=25.0}", DECOMPOSED_FLOW.toString());
    }
}
