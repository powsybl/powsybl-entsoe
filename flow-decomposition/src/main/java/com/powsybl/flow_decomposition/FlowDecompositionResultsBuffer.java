/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.flow_decomposition;

import com.powsybl.iidm.network.Network;

import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 */
class FlowDecompositionResultsBuffer {
    Map<String, DecomposedFlow> decomposedFlowMap = new HashMap<>();
    private final Network network;
    private final Date date;

    FlowDecompositionResultsBuffer(Network network) {
        this.network = network;
        this.date = Date.from(Instant.now());
    }

    void add(Map<String, DecomposedFlow> decomposedFlowMap) {
        this.decomposedFlowMap.putAll(decomposedFlowMap);
    }

    FlowDecompositionResults build() {
        return new FlowDecompositionResults(network, date, decomposedFlowMap);
    }
}
