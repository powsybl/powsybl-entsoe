/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.flow_decomposition;

/**
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 */
class DecomposedFlowsRescalerIdentity implements DecomposedFlowsRescaler {
    public DecomposedFlow rescale(DecomposedFlow decomposedFlow) {
        return decomposedFlow;
    }
}