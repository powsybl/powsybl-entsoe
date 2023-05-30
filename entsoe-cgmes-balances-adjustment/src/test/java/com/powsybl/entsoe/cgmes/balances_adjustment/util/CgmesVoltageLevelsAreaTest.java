/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.entsoe.cgmes.balances_adjustment.util;

import com.powsybl.balances_adjustment.util.NetworkArea;
import com.powsybl.balances_adjustment.util.NetworkAreaFactory;
import com.powsybl.cgmes.extensions.CgmesControlArea;
import com.powsybl.cgmes.extensions.CgmesControlAreas;
import com.powsybl.iidm.network.Line;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.TieLine;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowParameters;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * @author Miora Ralambotiana <miora.ralambotiana at rte-france.com>
 */
class CgmesVoltageLevelsAreaTest {

    private final String[] voltageLevelsIds = {"_d0486169-2205-40b2-895e-b672ecb9e5fc", "_4ba71b59-ee2f-450b-9f7d-cc2f1cc5e386", "_8bbd7e74-ae20-4dce-8780-c20f8e18c2e0", "_469df5f7-058f-4451-a998-57a48e8a56fe", "_b10b171b-3bc5-4849-bb1f-61ed9ea1ec7c"};
    private final Network network = Network.read("controlArea.xiidm", getClass().getResourceAsStream("/controlArea.xiidm"));

    static final double DELTA_POWER = 1e-5;

    @Test
    void testWithArea() {
        CgmesControlArea cgmesArea = network.getExtension(CgmesControlAreas.class).getCgmesControlArea("_BECONTROLAREA");
        NetworkAreaFactory factory = new CgmesVoltageLevelsAreaFactory(cgmesArea, voltageLevelsIds);
        NetworkArea area = factory.create(network);
        LoadFlow.run(network, new LoadFlowParameters().setDc(true));
        assertEquals(5, area.getContainedBusViewBuses().size());
        Line line = network.getLine("_b58bf21a-096a-4dae-9a01-3f03b60c24c7_fict_2");
        double lineFlow = (line.getTerminal2().getP() - line.getTerminal1().getP()) / 2;
        assertFalse(Double.isNaN(lineFlow));
        TieLine tieLine = network.getTieLine("TL_fict");
        double tieLineFlow = (tieLine.getDanglingLine1().getTerminal().getP() - tieLine.getDanglingLine2().getTerminal().getP()) / 2;
        assertFalse(Double.isNaN(tieLineFlow));

        double realNetPosition = -network.getDanglingLine("_78736387-5f60-4832-b3fe-d50daf81b0a6").getBoundary().getP()
                - network.getDanglingLine("_17086487-56ba-4979-b8de-064025a6b4da").getBoundary().getP()
                - network.getDanglingLine("_b18cd1aa-7808-49b9-a7cf-605eaf07b006").getBoundary().getP()
                + lineFlow + tieLineFlow;
        assertEquals(realNetPosition, area.getNetPosition(), DELTA_POWER);
    }

    @Test
    void testWithExcludedXnodes() {
        NetworkAreaFactory factory = new CgmesVoltageLevelsAreaFactory(Arrays.asList("TN_Border_GY11", "XNODE"), Arrays.asList(voltageLevelsIds));
        NetworkArea area = factory.create(network);
        assertEquals(5, area.getContainedBusViewBuses().size());
        Line line1 = network.getLine("_b58bf21a-096a-4dae-9a01-3f03b60c24c7_fict");
        Line line2 = network.getLine("_b58bf21a-096a-4dae-9a01-3f03b60c24c7_fict_2");
        double lineFlow = (line1.getTerminal2().getP() - line1.getTerminal1().getP()) / 2
                + (line2.getTerminal1().getP() - line2.getTerminal2().getP()) / 2;
        double realNetPosition = -network.getDanglingLine("_78736387-5f60-4832-b3fe-d50daf81b0a6").getBoundary().getP()
                - network.getDanglingLine("_17086487-56ba-4979-b8de-064025a6b4da").getBoundary().getP()
                - network.getDanglingLine("_ed0c5d75-4a54-43c8-b782-b20d7431630b").getBoundary().getP()
                + lineFlow;
        assertEquals(realNetPosition, area.getNetPosition(), DELTA_POWER);
    }
}
