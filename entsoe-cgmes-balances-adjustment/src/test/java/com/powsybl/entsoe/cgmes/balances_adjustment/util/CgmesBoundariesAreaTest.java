/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.entsoe.cgmes.balances_adjustment.util;

import com.powsybl.balances_adjustment.util.NetworkArea;
import com.powsybl.balances_adjustment.util.NetworkAreaFactory;
import com.powsybl.cgmes.extensions.CgmesControlAreas;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.test.DanglingLineNetworkFactory;
import org.junit.Test;

import java.util.ArrayList;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Miora Ralambotiana <miora.ralambotiana at rte-france.com>
 */
public class CgmesBoundariesAreaTest {

    public static double DELTA_POWER = 1e-5;

    @Test
    public void testWithNoArea() {
        Network network = DanglingLineNetworkFactory.create();
        NetworkAreaFactory factory = new CgmesBoundariesAreaFactory();
        NetworkArea area = factory.create(network);
        assertEquals(network.getDanglingLine("DL").getBoundary().getP(), area.getNetPosition(), DELTA_POWER);
        assertTrue(area.getContainedBusViewBuses().isEmpty());
    }

    @Test
    public void testWithArea() {
        Network network = Network.read("controlArea.xiidm", getClass().getResourceAsStream("/controlArea.xiidm"));
        NetworkAreaFactory factory = new CgmesBoundariesAreaFactory(new ArrayList<>(network.getExtension(CgmesControlAreas.class).getCgmesControlAreas()));
        NetworkArea area = factory.create(network);
        assertEquals(Stream.of(network.getDanglingLine("_78736387-5f60-4832-b3fe-d50daf81b0a6"),
                network.getDanglingLine("_17086487-56ba-4979-b8de-064025a6b4da"),
                network.getDanglingLine("_b18cd1aa-7808-49b9-a7cf-605eaf07b006"))
                        .mapToDouble(dl -> dl.getBoundary().getP()).sum(),
                area.getNetPosition(), DELTA_POWER);
        assertTrue(area.getContainedBusViewBuses().isEmpty());
    }
}
