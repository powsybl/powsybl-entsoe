/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.flow_decomposition;

import com.powsybl.commons.PowsyblException;
import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Injection;
import com.powsybl.iidm.network.Network;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class NetworkUtilTest {

    @Test
    void testGetFlowIdFromCountry() {
        String networkFileName = "19700101_0000_FO4_UX1.uct";
        Network network = TestUtils.importNetwork(networkFileName);
        assertEquals("Loop Flow from SI", NetworkUtil.getLoopFlowIdFromCountry(Country.SI));
        assertEquals("Loop Flow from BE", NetworkUtil.getLoopFlowIdFromCountry(network, "BB000021_load"));
        PowsyblException exception = assertThrows(PowsyblException.class, () -> NetworkUtil.getLoopFlowIdFromCountry(network, "DUMMY"));
        assertEquals("Identifiable DUMMY must be an Injection", exception.getMessage());
    }

    @Test
    void testGetIndex() {
        Map<String, Integer> result = Map.of("a", 0, "b", 1, "c", 2);
        assertEquals(result, NetworkUtil.getIndex(List.of("a", "b", "c")));
    }

    @Test
    void testGetAllValidBranches() {
        String networkFileName = "NETWORK_LOOP_FLOW_WITH_COUNTRIES.uct";
        Network network = TestUtils.importNetwork(networkFileName);
        network.getBranch("BLOAD 11 FLOAD 11 1").getTerminal2().disconnect();
        network.getBranch("FGEN  11 BGEN  11 1").getTerminal1().disconnect();
        List<Branch> allValidBranches = NetworkUtil.getAllValidBranches(network);
        assertEquals(1, allValidBranches.size());
        assertTrue(allValidBranches.contains(network.getBranch("EGEN  11 FGEN  11 1")));
        assertFalse(allValidBranches.contains(network.getBranch("FGEN  11 BGEN  11 1")));
        assertFalse(allValidBranches.contains(network.getBranch("BGEN  11 BLOAD 11 1")));
        assertFalse(allValidBranches.contains(network.getBranch("BLOAD 11 FLOAD 11 1")));
        assertFalse(allValidBranches.contains(network.getBranch("FLOAD 11 ELOAD 11 1")));
    }

    @Test
    void testGetNodeList() {
        String networkFileName = "NETWORK_LOOP_FLOW_WITH_COUNTRIES.uct";
        Network network = TestUtils.importNetwork(networkFileName);
        network.getBranch("BLOAD 11 FLOAD 11 1").getTerminal2().disconnect();
        network.getBranch("FGEN  11 BGEN  11 1").getTerminal1().disconnect();
        List<Injection<?>> nodeList = NetworkUtil.getNodeList(network);
        assertEquals(2, nodeList.size());
        assertTrue(nodeList.contains(network.getGenerator("EGEN  11_generator")));
        assertTrue(nodeList.contains(network.getGenerator("FGEN  11_generator")));
    }

    @Test
    void testGetXNodeList() {
        String networkFileName = "NETWORK_SINGLE_LOAD_TWO_GENERATORS_WITH_UNBOUNDED_XNODE.uct";
        Network network = TestUtils.importNetwork(networkFileName);
        List<Injection<?>> nodeList = NetworkUtil.getXNodeList(network);
        assertEquals(1, nodeList.size());
        assertTrue(nodeList.contains(network.getDanglingLine("BLOAD 11 X     11 1")));
    }

    @Test
    void testGetPstIdList() {
        String networkFileName = "NETWORK_PST_FLOW_WITH_COUNTRIES.uct";
        Network network = TestUtils.importNetwork(networkFileName);
        List<String> nodeList = NetworkUtil.getPstIdList(network);
        assertEquals(1, nodeList.size());
        assertTrue(nodeList.contains("BLOAD 11 BLOAD 12 2"));
    }
}
