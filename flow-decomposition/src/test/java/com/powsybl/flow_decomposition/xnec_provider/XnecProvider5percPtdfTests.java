/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.flow_decomposition.xnec_provider;

import com.powsybl.flow_decomposition.TestUtils;
import com.powsybl.flow_decomposition.XnecProvider;
import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Network;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 */
class XnecProvider5percPtdfTests {
    @Test
    void testXnecProvider() {
        String networkFileName = "NETWORK_PARALLEL_LINES_PTDF.uct";
        Network network = TestUtils.importNetwork(networkFileName);
        String lineFrBe = "FLOAD 11 BLOAD 11 1";
        String lineBeFr = "FGEN  11 BLOAD 11 1";
        String line1 = "FGEN  11 FLOAD 11 1";
        String line2 = "FGEN  11 FLOAD 11 2";
        String line3 = "FGEN  11 FLOAD 11 3";
        String line4 = "FGEN  11 FLOAD 11 4";
        String line5 = "FGEN  11 FLOAD 11 5";
        String line6 = "FGEN  11 FLOAD 11 6";
        String line7 = "FGEN  11 FLOAD 11 7";
        String line8 = "FGEN  11 FLOAD 11 8";
        String line9 = "FGEN  11 FLOAD 11 9";
        XnecProvider xnecProvider = new XnecProvider5percPtdf();
        Set<Branch> branchSet = xnecProvider.getNetworkElements(network);
        assertTrue(branchSet.contains(network.getBranch(lineFrBe)));
        assertTrue(branchSet.contains(network.getBranch(lineBeFr)));
        assertTrue(branchSet.contains(network.getBranch(line1)));
        assertTrue(branchSet.contains(network.getBranch(line2)));
        assertTrue(branchSet.contains(network.getBranch(line3)));
        assertTrue(branchSet.contains(network.getBranch(line4)));
        assertTrue(branchSet.contains(network.getBranch(line5)));
        assertTrue(branchSet.contains(network.getBranch(line6)));
        assertTrue(branchSet.contains(network.getBranch(line7)));
        assertTrue(branchSet.contains(network.getBranch(line8)));
        assertTrue(branchSet.contains(network.getBranch(line9)));
        assertEquals(11, branchSet.size());
    }

    @Test
    void testInterfaceDoesNotSupportContingencies() {
        String networkFileName = "NETWORK_PARALLEL_LINES_PTDF.uct";
        Network network = TestUtils.importNetwork(networkFileName);
        XnecProvider xnecProvider = new XnecProvider5percPtdf();
        assertTrue(xnecProvider.getNetworkElements("contingency id", network).isEmpty());
        assertTrue(xnecProvider.getNetworkElementsPerContingency(network).isEmpty());
        assertTrue(xnecProvider.getContingencies(network).isEmpty());
    }

    @Test
    void testInterfaceDoesNotSupportContingencies() {
        String networkFileName = "NETWORK_PARALLEL_LINES_PTDF.uct";
        Network network = TestUtils.importNetwork(networkFileName);
        XnecProvider xnecProvider = new XnecProvider5percPtdf();
        assertTrue(xnecProvider.getNetworkElements("contingency id", network).isEmpty());
        assertTrue(xnecProvider.getNetworkElementsPerContingency(network).isEmpty());
        assertTrue(xnecProvider.getContingencies(network).isEmpty());
    }
}
