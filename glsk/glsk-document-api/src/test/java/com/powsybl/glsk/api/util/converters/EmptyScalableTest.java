/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.glsk.api.util.converters;

import com.powsybl.action.util.Scalable;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Network;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class EmptyScalableTest {

    private EmptyScalable scalable;
    private Network network;

    @Before
    public void setUp() {
        scalable = new EmptyScalable();
        network = Importers.loadNetwork("testCase.xiidm", getClass().getResourceAsStream("testCase.xiidm"));
    }

    @Test
    public void testInitialValue() {
        assertEquals(0, scalable.initialValue(network), 1);
    }

    @Test
    public void testMaximumValues() {
        assertEquals(0, scalable.maximumValue(network), 1);
        assertEquals(0, scalable.maximumValue(network, Scalable.ScalingConvention.GENERATOR), 1);
        assertEquals(0, scalable.maximumValue(network, Scalable.ScalingConvention.LOAD), 1);
    }

    @Test
    public void testMinimumValues() {
        assertEquals(0, scalable.minimumValue(network), 1);
        assertEquals(0, scalable.minimumValue(network, Scalable.ScalingConvention.GENERATOR), 1);
        assertEquals(0, scalable.minimumValue(network, Scalable.ScalingConvention.LOAD), 1);
    }

    @Test
    public void testListGenerators() {
        assertEquals(0, scalable.listGenerators(network).size());
    }

    @Test
    public void testScale() {
        assertEquals(0, scalable.scale(network, 500), 1);
        assertEquals(0, scalable.scale(network, -500), 1);
        assertEquals(0, scalable.scale(network, 500, Scalable.ScalingConvention.GENERATOR), 1);
        assertEquals(0, scalable.scale(network, -500, Scalable.ScalingConvention.GENERATOR), 1);
        assertEquals(0, scalable.scale(network, 500, Scalable.ScalingConvention.LOAD), 1);
        assertEquals(0, scalable.scale(network, -500, Scalable.ScalingConvention.LOAD), 1);
    }
}
