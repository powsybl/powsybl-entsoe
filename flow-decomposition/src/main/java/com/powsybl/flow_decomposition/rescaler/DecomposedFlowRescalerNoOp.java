/**
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.flow_decomposition.rescaler;

import com.powsybl.commons.PowsyblException;
import com.powsybl.flow_decomposition.DecomposedFlow;
import com.powsybl.flow_decomposition.FlowPartition;
import com.powsybl.iidm.network.Network;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 * @author Caio Luke {@literal <caio.luke at artelys.com>}
 */
public class DecomposedFlowRescalerNoOp extends AbstractDecomposedFlowRescaler {

    public DecomposedFlowRescalerNoOp() {
        // empty constructor
    }

    @Override
    protected boolean shouldRescaleFlows(DecomposedFlow decomposedFlow) {
        return false;
    }

    @Override
    protected FlowPartition computeRescaledFlowsPartition(DecomposedFlow decomposedFlow, Network network) {
        throw new PowsyblException("This class is not supposed compute rescaled flows");
    }
}
