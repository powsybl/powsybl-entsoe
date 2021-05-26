/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.entsoe.cgmes.balances_adjustment.util;

import com.powsybl.balances_adjustment.util.NetworkArea;
import com.powsybl.balances_adjustment.util.NetworkAreaFactory;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.test.DanglingLineNetworkFactory;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Miora Ralambotiana <miora.ralambotiana at rte-france.com>
 */
public class CgmesBoundariesAreaTest {

    @Test
    public void test() {
        Network network = DanglingLineNetworkFactory.create();
        NetworkAreaFactory factory = new CgmesBoundariesAreaFactory();
        NetworkArea area = factory.create(network);
        assertEquals(network.getDanglingLine("DL").getBoundary().getP(), area.getNetPosition(), 0.0);
    }
}
