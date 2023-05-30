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
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowParameters;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Miora Ralambotiana <miora.ralambotiana at rte-france.com>
 */
class CgmesBoundariesAreaTest {

    static double DELTA_POWER = 1e-5;

    @Test
    void testWithNoArea() {
        Network network = DanglingLineNetworkFactory.create();
        NetworkAreaFactory factory = new CgmesBoundariesAreaFactory();
        NetworkArea area = factory.create(network);
        assertEquals(network.getDanglingLine("DL").getBoundary().getP(), area.getNetPosition(), DELTA_POWER);
        assertTrue(area.getContainedBusViewBuses().isEmpty());
    }

    @Test
    void testWithArea() {
        Network network = Network.read("controlArea.xiidm", getClass().getResourceAsStream("/controlArea.xiidm"));
        NetworkAreaFactory factory = new CgmesBoundariesAreaFactory(new ArrayList<>(network.getExtension(CgmesControlAreas.class).getCgmesControlAreas()));
        NetworkArea area = factory.create(network);

        // we must run a DC load flow to have P at the tie line terminals
        LoadFlow.run(network, new LoadFlowParameters().setDc(true));

        List<Double> ps = Stream.of(
                    network.getDanglingLine("_78736387-5f60-4832-b3fe-d50daf81b0a6"),
                    network.getDanglingLine("_17086487-56ba-4979-b8de-064025a6b4da"),
                    network.getDanglingLine("_b18cd1aa-7808-49b9-a7cf-605eaf07b006"))
                .map(dl -> dl.getBoundary().getP()).collect(Collectors.toList());
        double sum = ps.stream().mapToDouble(n -> n).sum();
        // FIXME: Boundary.getP() is not working for the moment.
        // DC approximation.
        sum = sum - network.getTieLine("TL_fict").getDanglingLine1().getTerminal().getP(); // FIXME.
        System.out.println(sum);
        System.out.println(ps);
        double anp = area.getNetPosition();
        System.out.println(anp);

        // FIXME: we miss the line "_b58bf21a-096a-4dae-9a01-3f03b60c24c7_fict_2" because it is not a tie line.
        assertEquals(sum, area.getNetPosition(), DELTA_POWER);
        assertTrue(area.getContainedBusViewBuses().isEmpty());
    }
}
