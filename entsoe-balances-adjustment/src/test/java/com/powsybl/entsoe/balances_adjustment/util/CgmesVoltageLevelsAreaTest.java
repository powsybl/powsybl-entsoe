/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.entsoe.balances_adjustment.util;

import com.powsybl.balances_adjustment.util.NetworkArea;
import com.powsybl.balances_adjustment.util.NetworkAreaFactory;
import com.powsybl.cgmes.extensions.CgmesControlArea;
import com.powsybl.cgmes.extensions.CgmesControlAreas;
import com.powsybl.cgmes.extensions.CgmesDanglingLineBoundaryNodeAdder;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Line;
import com.powsybl.iidm.network.Network;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Miora Ralambotiana <miora.ralambotiana at rte-france.com>
 */
public class CgmesVoltageLevelsAreaTest {

    @Test
    public void test() {
        Network network = Importers.loadNetwork("controlArea.xiidm", getClass().getResourceAsStream("/controlArea.xiidm"));
        network.getDanglingLine("_a16b4a6c-70b1-4abf-9a9d-bd0fa47f9fe4")
                .newExtension(CgmesDanglingLineBoundaryNodeAdder.class)
                .setHvdc(true)
                .add();
        CgmesControlArea cgmesArea = network.getExtension(CgmesControlAreas.class).getCgmesControlArea("_BECONTROLAREA");
        NetworkAreaFactory factory = new CgmesVoltageLevelsAreaFactory(cgmesArea, "_d0486169-2205-40b2-895e-b672ecb9e5fc",
                "_4ba71b59-ee2f-450b-9f7d-cc2f1cc5e386", "_8bbd7e74-ae20-4dce-8780-c20f8e18c2e0", "_469df5f7-058f-4451-a998-57a48e8a56fe",
                "_b10b171b-3bc5-4849-bb1f-61ed9ea1ec7c");
        NetworkArea area = factory.create(network);
        assertEquals(5, area.getContainedBusViewBuses().size());
        Line line = network.getLine("_b58bf21a-096a-4dae-9a01-3f03b60c24c7_fict_2");
        double lineFlow = (line.getTerminal2().getP() - line.getTerminal1().getP()) / 2;
        double real = -network.getDanglingLine("_78736387-5f60-4832-b3fe-d50daf81b0a6").getBoundary().getP() - network.getDanglingLine("_17086487-56ba-4979-b8de-064025a6b4da").getBoundary().getP()
                - network.getDanglingLine("_b18cd1aa-7808-49b9-a7cf-605eaf07b006").getBoundary().getP() + lineFlow;
        assertEquals(real, area.getNetPosition(), 0.0d);
    }
}
