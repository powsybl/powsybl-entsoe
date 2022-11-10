/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.flow_decomposition;

import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Network;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 */
class XnecProviderByIdsTests {
    @Test
    void testXnecProvider() {
        String networkFileName = "NETWORK_SINGLE_LOAD_TWO_GENERATORS_WITH_COUNTRIES.uct";
        Network network = TestUtils.importNetwork(networkFileName);
        String xnecFrBe = "FGEN1 11 BLOAD 11 1";
        List<String> xnecList = List.of(xnecFrBe);
        XnecProvider xnecProvider = new XnecProviderByIds(xnecList);
        List<Branch> branchList = xnecProvider.getNetworkElements(network);
        assertTrue(branchList.contains(network.getBranch(xnecFrBe)));
        assertEquals(1, branchList.size());
    }

    @Test
    void testXnecProviderNonExistingId() {
        String networkFileName = "NETWORK_SINGLE_LOAD_TWO_GENERATORS_WITH_COUNTRIES.uct";
        Network network = TestUtils.importNetwork(networkFileName);
        String xnecFrBe = "FGEN1 11 BLOAD 11 1";
        List<String> xnecList = List.of(xnecFrBe, "NON EXISTING ID");
        XnecProvider xnecProvider = new XnecProviderByIds(xnecList);
        List<Branch> branchList = xnecProvider.getNetworkElements(network);
        assertTrue(branchList.contains(network.getBranch(xnecFrBe)));
        assertEquals(1, branchList.size());
    }

    @Test
    void testXnecProviderMultipleIds() {
        String networkFileName = "NETWORK_SINGLE_LOAD_TWO_GENERATORS_WITH_COUNTRIES.uct";
        Network network = TestUtils.importNetwork(networkFileName);
        String xnecFrBe = "FGEN1 11 BLOAD 11 1";
        String xnecBeBe = "BLOAD 11 BGEN2 11 1";
        List<String> xnecList = List.of(xnecFrBe, xnecBeBe);
        XnecProvider xnecProvider = new XnecProviderByIds(xnecList);
        List<Branch> branchList = xnecProvider.getNetworkElements(network);
        assertTrue(branchList.contains(network.getBranch(xnecFrBe)));
        assertTrue(branchList.contains(network.getBranch(xnecBeBe)));
        assertEquals(2, branchList.size());
    }

    @Test
    void testXnecProviderMultipleDuplicateIds() {
        String networkFileName = "NETWORK_SINGLE_LOAD_TWO_GENERATORS_WITH_COUNTRIES.uct";
        Network network = TestUtils.importNetwork(networkFileName);
        String xnecFrBe = "FGEN1 11 BLOAD 11 1";
        String xnecBeBe = "BLOAD 11 BGEN2 11 1";
        List<String> xnecList = List.of(xnecFrBe, xnecBeBe, xnecFrBe);
        XnecProvider xnecProvider = new XnecProviderByIds(xnecList);
        List<Branch> branchList = xnecProvider.getNetworkElements(network);
        assertTrue(branchList.contains(network.getBranch(xnecFrBe)));
        assertTrue(branchList.contains(network.getBranch(xnecBeBe)));
        assertEquals(2, branchList.size());
    }
}
