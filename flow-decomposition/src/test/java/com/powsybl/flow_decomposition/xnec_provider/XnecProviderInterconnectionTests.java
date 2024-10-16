/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.flow_decomposition.xnec_provider;

import com.powsybl.flow_decomposition.TestUtils;
import com.powsybl.flow_decomposition.XnecProvider;
import com.powsybl.iidm.network.Identifiable;
import com.powsybl.iidm.network.Network;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 */
class XnecProviderInterconnectionTests {
    @Test
    void testXnecProvider() {
        String networkFileName = "NETWORK_SINGLE_LOAD_TWO_GENERATORS_WITH_COUNTRIES.uct";
        Network network = TestUtils.importNetwork(networkFileName);
        String xnecFrBee = "FGEN1 11 BLOAD 11 1";
        XnecProvider xnecProvider = new XnecProviderInterconnection();
        Set<Identifiable<?>> branchSet = xnecProvider.getNetworkElements(network);
        assertTrue(branchSet.contains(network.getBranch(xnecFrBee)));
        assertEquals(1, branchSet.size());
    }

    @Test
    void testInterfaceDoesNotSupportContingencies() {
        String networkFileName = "NETWORK_SINGLE_LOAD_TWO_GENERATORS_WITH_COUNTRIES.uct";
        Network network = TestUtils.importNetwork(networkFileName);
        XnecProvider xnecProvider = new XnecProviderInterconnection();
        assertTrue(xnecProvider.getNetworkElements("contingency id", network).isEmpty());
        assertTrue(xnecProvider.getNetworkElementsPerContingency(network).isEmpty());
        assertTrue(xnecProvider.getContingencies(network).isEmpty());
    }
}
