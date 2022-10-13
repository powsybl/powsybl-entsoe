/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.flow_decomposition;

import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Network;

import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This class contains helper functions for tests.
 *
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 */
final class TestUtils {
    private TestUtils() {
    }

    static Network importNetwork(String networkResourcePath) {
        String networkName = Paths.get(networkResourcePath).getFileName().toString();
        return Importers.loadNetwork(networkName, TestUtils.class.getResourceAsStream(networkResourcePath));
    }

    static List<Xnec> getXnecList(Network network) {
        return network.getBranchStream().map(Xnec::new).collect(Collectors.toList());
    }
}
