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
        DecomposedFlow localDecomposedFlow = new DecomposedFlow("dummy branch id", "dummy contingency id", Country.FR, Country.BE, 29, 24, 33, 25, 26, 58, Map.of("Loop Flow from BE", -1., "Loop Flow from FR", -9.));
        assertEquals(DECOMPOSED_FLOW, DECOMPOSED_FLOW);
        assertEquals(DECOMPOSED_FLOW, localDecomposedFlow);
        assertNotEquals(1, DECOMPOSED_FLOW);
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
