/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.balances_adjustment.util;

import com.powsybl.commons.PowsyblException;
import com.powsybl.iidm.modification.scalable.Scalable;
import com.powsybl.iidm.network.Identifiable;
import com.powsybl.iidm.network.Injection;
import com.powsybl.iidm.network.Load;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.test.EurostagTutorialExample1Factory;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

/**
 * @author Miora Ralambotiana <miora.ralambotiana at rte-france.com>
 */
public class NetworkAreaUtilTest {

    @Test
    public void testConformLoadsScalables() {
        Network network = EurostagTutorialExample1Factory.create();
        NetworkAreaFactory factory = new VoltageLevelsAreaFactory(network.getVoltageLevelStream().map(Identifiable::getId).toArray(String[]::new));
        NetworkArea area = factory.create(network);
        Scalable scalable = NetworkAreaUtil.createConformLoadScalable(area);
        List<Injection> injections = scalable.filterInjections(network);
        assertEquals(1, injections.size());
        Load load = network.getLoad("LOAD");
        assertTrue(injections.contains(load));
        assertEquals(10, scalable.scale(network, 10), 0.0);
    }

    @Test
    public void testNoLoadScalables() {
        Network network = EurostagTutorialExample1Factory.create();
        NetworkAreaFactory factory = new VoltageLevelsAreaFactory("VLGEN");
        NetworkArea area = factory.create(network);
        try {
            NetworkAreaUtil.createConformLoadScalable(area);
            fail();
        } catch (PowsyblException e) {
            assertEquals("There is no load in this area", e.getMessage());
        }
    }

    @Test
    public void testNullLoadScalables() {
        Network network = EurostagTutorialExample1Factory.create();
        network.getLoad("LOAD").setP0(0.0);
        NetworkAreaFactory factory = new VoltageLevelsAreaFactory(network.getVoltageLevelStream().map(Identifiable::getId).toArray(String[]::new));
        NetworkArea area = factory.create(network);
        try {
            NetworkAreaUtil.createConformLoadScalable(area);
            fail();
        } catch (PowsyblException e) {
            assertEquals("All loads' active power flows is null", e.getMessage());
        }
    }
}
