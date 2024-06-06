/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.flow_decomposition;

import com.powsybl.commons.PowsyblException;
import com.powsybl.iidm.network.*;
import com.powsybl.iidm.network.test.FourSubstationsNodeBreakerFactory;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 */
class NetworkUtilTest {

    @Test
    void testGetFlowIdFromCountry() {
        Network network = TestUtils.importNetwork("19700101_0000_FO4_UX1.uct");

        assertEquals("Loop Flow from SI", NetworkUtil.getLoopFlowIdFromCountry(Country.SI));

        assertEquals("Loop Flow from BE", NetworkUtil.getLoopFlowIdFromCountry(network, "BB000021_load"));
        PowsyblException exception = assertThrows(PowsyblException.class, () -> NetworkUtil.getLoopFlowIdFromCountry(network, "DUMMY"));

        assertEquals("Identifiable DUMMY must be an Injection", exception.getMessage());
    }

    @Test
    void testGetFlowIdFromCountryWithNetworkWithoutCountries() {
        Network network = FourSubstationsNodeBreakerFactory.create();
        PowsyblException exception = assertThrows(PowsyblException.class, () -> NetworkUtil.getLoopFlowIdFromCountry(network, "GH1"));
        assertEquals("Substation S1 does not have country property needed for the algorithm.", exception.getMessage());
    }

    @Test
    void testGetIndex() {
        Map<String, Integer> result = Map.of("a", 0, "b", 1, "c", 2);
        assertEquals(result, NetworkUtil.getIndex(List.of("a", "b", "c")));
    }

    @Test
    void testGetAllValidBranches() {
        Network network = TestUtils.importNetwork("NETWORK_LOOP_FLOW_WITH_COUNTRIES.uct");

        List<Branch> allValidBranches = NetworkUtil.getAllValidBranches(network);
        assertEquals(5, allValidBranches.size());
        assertTrue(allValidBranches.contains(network.getBranch("EGEN  11 FGEN  11 1")));
        assertTrue(allValidBranches.contains(network.getBranch("FGEN  11 BGEN  11 1")));
        assertTrue(allValidBranches.contains(network.getBranch("BGEN  11 BLOAD 11 1")));
        assertTrue(allValidBranches.contains(network.getBranch("BLOAD 11 FLOAD 11 1")));
        assertTrue(allValidBranches.contains(network.getBranch("FLOAD 11 ELOAD 11 1")));
    }

    @Test
    void testGetAllValidBranchesWithDisconnectedTerminals() {
        Network network = TestUtils.importNetwork("NETWORK_LOOP_FLOW_WITH_COUNTRIES.uct");
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
    void testGetAllValidBranchesWithLinesOutsideMainConnectedComponent() {
        Network network = TestUtils.importNetwork("NETWORK_LOOP_FLOW_WITH_COUNTRIES.uct");
        network.getLine("FGEN  11 BGEN  11 1").disconnect();
        assertFalse(network.getBranch("EGEN  11 FGEN  11 1").getTerminal1().getBusBreakerView().getBus().isInMainConnectedComponent());
        assertFalse(network.getBranch("EGEN  11 FGEN  11 1").getTerminal1().getBusBreakerView().getBus().isInMainSynchronousComponent());

        List<Branch> allValidBranches = NetworkUtil.getAllValidBranches(network);
        assertEquals(3, allValidBranches.size());
        assertFalse(allValidBranches.contains(network.getBranch("EGEN  11 FGEN  11 1")));
        assertFalse(allValidBranches.contains(network.getBranch("FGEN  11 BGEN  11 1")));
        assertTrue(allValidBranches.contains(network.getBranch("BGEN  11 BLOAD 11 1")));
        assertTrue(allValidBranches.contains(network.getBranch("BLOAD 11 FLOAD 11 1")));
        assertTrue(allValidBranches.contains(network.getBranch("FLOAD 11 ELOAD 11 1")));
    }

    @Test
    void testGetAllValidBranchesWithLinesOutsideMainSynchronousComponent() {
        Network network = TestUtils.importNetwork("TestCase16NodesWithHvdc.xiidm");
        assertEquals(1, network.getBusView().getConnectedComponents().size());
        assertEquals(1, network.getBusView().getSynchronousComponents().size());
        Line lineOutsideSync = network.getLine("BBE1AA11 FFR5AA11 1");
        assertTrue(lineOutsideSync.getTerminal1().getBusBreakerView().getBus().isInMainConnectedComponent());
        assertTrue(lineOutsideSync.getTerminal1().getBusBreakerView().getBus().isInMainSynchronousComponent());

        Line lineDisconnected1 = network.getLine("DDE2AA11 NNL3AA11 1");
        Line lineDisconnected2 = network.getLine("FFR3AA11 FFR5AA11 1");
        lineDisconnected1.disconnect();
        lineDisconnected2.disconnect();
        assertEquals(1, network.getBusView().getConnectedComponents().size());
        assertEquals(2, network.getBusView().getSynchronousComponents().size());
        assertTrue(lineOutsideSync.getTerminal1().getBusBreakerView().getBus().isInMainConnectedComponent());
        assertFalse(lineOutsideSync.getTerminal1().getBusBreakerView().getBus().isInMainSynchronousComponent());

        validateValidBranches(network);
    }

    @Test
    void testGetAllValidBranchesWithLinesOutsideMainConnectedAndSynchronousComponent() {
        Network network = TestUtils.importNetwork("TestCase16NodesWithHvdc.xiidm");
        assertEquals(1, network.getBusView().getConnectedComponents().size());
        assertEquals(1, network.getBusView().getSynchronousComponents().size());
        Line lineOutsideSync = network.getLine("BBE1AA11 FFR5AA11 1");
        assertTrue(lineOutsideSync.getTerminal1().getBusBreakerView().getBus().isInMainConnectedComponent());
        assertTrue(lineOutsideSync.getTerminal1().getBusBreakerView().getBus().isInMainSynchronousComponent());
        Line lineOutsideConnected = network.getLine("NNL1AA11 NNL3AA11 1");
        assertTrue(lineOutsideConnected.getTerminal1().getBusBreakerView().getBus().isInMainConnectedComponent());
        assertTrue(lineOutsideConnected.getTerminal1().getBusBreakerView().getBus().isInMainSynchronousComponent());

        Line lineDisconnected1 = network.getLine("DDE2AA11 NNL3AA11 1");
        Line lineDisconnected2 = network.getLine("FFR3AA11 FFR5AA11 1");
        Line lineDisconnected3 = network.getLine("NNL2AA11 BBE3AA11 1");
        lineDisconnected1.disconnect();
        lineDisconnected2.disconnect();
        lineDisconnected3.disconnect();
        assertEquals(2, network.getBusView().getConnectedComponents().size());
        assertEquals(3, network.getBusView().getSynchronousComponents().size());
        assertTrue(lineOutsideSync.getTerminal1().getBusBreakerView().getBus().isInMainConnectedComponent());
        assertFalse(lineOutsideSync.getTerminal1().getBusBreakerView().getBus().isInMainSynchronousComponent());
        assertFalse(lineOutsideConnected.getTerminal1().getBusBreakerView().getBus().isInMainConnectedComponent());
        assertFalse(lineOutsideConnected.getTerminal1().getBusBreakerView().getBus().isInMainSynchronousComponent());

        validateValidBranches(network);
    }

    private static void validateValidBranches(Network network) {
        List<Branch> allValidBranches = NetworkUtil.getAllValidBranches(network);
        assertEquals(12, allValidBranches.size());
        assertTrue(allValidBranches.contains(network.getLine("DDE1AA11 DDE2AA11 1")));
        assertTrue(allValidBranches.contains(network.getLine("DDE1AA11 DDE4AA11 1")));
        assertTrue(allValidBranches.contains(network.getLine("DDE2AA11 DDE3AA11 1")));
        assertTrue(allValidBranches.contains(network.getLine("FFR1AA11 FFR2AA11 1")));
        assertTrue(allValidBranches.contains(network.getLine("FFR1AA11 FFR3AA11 1")));
        assertTrue(allValidBranches.contains(network.getLine("FFR1AA11 FFR4AA11 1")));
        assertTrue(allValidBranches.contains(network.getLine("FFR2AA11 DDE3AA11 1")));
        assertTrue(allValidBranches.contains(network.getLine("FFR2AA11 FFR3AA11 1")));
        assertTrue(allValidBranches.contains(network.getLine("FFR2AA11 FFR3AA11 2")));
        assertTrue(allValidBranches.contains(network.getLine("FFR4AA11 DDE1AA11 1")));
        assertTrue(allValidBranches.contains(network.getLine("FFR4AA11 DDE4AA11 1")));
        assertTrue(allValidBranches.contains(network.getTwoWindingsTransformer("FFR2AA11 FFR4AA11 1")));
    }

    @Test
    void testGetNodeList() {
        Network network = TestUtils.importNetwork("NETWORK_LOOP_FLOW_WITH_COUNTRIES.uct");

        List<Injection<?>> nodeList = NetworkUtil.getNodeList(network);
        assertEquals(6, nodeList.size());
        assertTrue(nodeList.contains(network.getGenerator("EGEN  11_generator")));
        assertTrue(nodeList.contains(network.getGenerator("FGEN  11_generator")));
        assertTrue(nodeList.contains(network.getGenerator("BGEN  11_generator")));
        assertTrue(nodeList.contains(network.getLoad("BLOAD 11_load")));
        assertTrue(nodeList.contains(network.getLoad("FLOAD 11_load")));
        assertTrue(nodeList.contains(network.getLoad("ELOAD 11_load")));
    }

    @Test
    void testGetNodeListWithDisconnectedTerminals() {
        Network network = TestUtils.importNetwork("NETWORK_LOOP_FLOW_WITH_COUNTRIES.uct");
        network.getBranch("BLOAD 11 FLOAD 11 1").getTerminal2().disconnect();
        network.getBranch("FGEN  11 BGEN  11 1").getTerminal1().disconnect();

        List<Injection<?>> nodeList = NetworkUtil.getNodeList(network);
        assertEquals(2, nodeList.size());
        assertTrue(nodeList.contains(network.getGenerator("EGEN  11_generator")));
        assertTrue(nodeList.contains(network.getGenerator("FGEN  11_generator")));
    }

    @Test
    void testGetNodeListWithoutBusBarSection() {
        Network network = TestUtils.getMicroGridNetworkWithBusBarSectionOnly();

        List<Injection<?>> nodeList = NetworkUtil.getNodeList(network);
        validateNodeListForMicroGridBE3DanglingLinesSameBoundary1Disconnected(nodeList, network);
    }

    @Test
    void testGetNodeListWithoutShuntCompensator() {
        Network network = TestUtils.getMicroGridNetworkWithShuntCompensatorOnly();

        List<Injection<?>> nodeList = NetworkUtil.getNodeList(network);
        validateNodeListForMicroGridBE3DanglingLinesSameBoundary1Disconnected(nodeList, network);
    }

    private static void validateNodeListForMicroGridBE3DanglingLinesSameBoundary1Disconnected(List<Injection<?>> nodeList, Network network) {
        assertEquals(9, nodeList.size());
        assertTrue(nodeList.contains(network.getGenerator("3a3b27be-b18b-4385-b557-6735d733baf0")));
        assertTrue(nodeList.contains(network.getGenerator("550ebe0d-f2b2-48c1-991f-cebea43a21aa")));
        assertTrue(nodeList.contains(network.getLoad("b1480a00-b427-4001-a26c-51954d2bb7e9")));
        assertTrue(nodeList.contains(network.getLoad("1c6beed6-1acf-42e7-ba55-0cc9f04bddd8")));
        assertTrue(nodeList.contains(network.getLoad("cb459405-cc14-4215-a45c-416789205904")));
        assertTrue(nodeList.contains(network.getDanglingLine("ed0c5d75-4a54-43c8-b782-b20d7431630b")));
        assertTrue(nodeList.contains(network.getDanglingLine("b18cd1aa-7808-49b9-a7cf-605eaf07b006")));
        assertTrue(nodeList.contains(network.getDanglingLine("a16b4a6c-70b1-4abf-9a9d-bd0fa47f9fe4")));
        assertTrue(nodeList.contains(network.getDanglingLine("17086487-56ba-4979-b8de-064025a6b4da")));
    }

    @Test
    void testGetNodeListWithoutStaticVarCompensator() {
        Network network = TestUtils.getNetworkWithStaticVarCompensatorOnly();

        List<Injection<?>> nodeList = NetworkUtil.getNodeList(network);
        assertEquals(2, nodeList.size());
        assertTrue(nodeList.contains(network.getGenerator("G1")));
        assertTrue(nodeList.contains(network.getLoad("L2")));
    }

    @Test
    void testGetXNodeList() {
        Network network = TestUtils.importNetwork("NETWORK_SINGLE_LOAD_TWO_GENERATORS_WITH_UNBOUNDED_XNODE.uct");

        List<Injection<?>> nodeList = NetworkUtil.getXNodeList(network);
        assertEquals(1, nodeList.size());
        assertTrue(nodeList.contains(network.getDanglingLine("BLOAD 11 X     11 1")));
    }

    @Test
    void testGetPstIdList() {
        Network network = TestUtils.importNetwork("NETWORK_PST_FLOW_WITH_COUNTRIES.uct");
        List<String> nodeList = NetworkUtil.getPstIdList(network);
        assertEquals(1, nodeList.size());
        assertTrue(nodeList.contains("BLOAD 11 BLOAD 12 2"));
    }
}
