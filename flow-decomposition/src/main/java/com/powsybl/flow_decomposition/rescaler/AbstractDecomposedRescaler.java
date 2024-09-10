/**
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.flow_decomposition.rescaler;

import com.powsybl.flow_decomposition.DecomposedFlow;
import com.powsybl.flow_decomposition.DecomposedFlowBuilder;
import com.powsybl.iidm.network.Network;

import java.util.Map;

/**
 * @author Caio Luke {@literal <caio.luke at artelys.com>}
 */
abstract class AbstractDecomposedRescaler implements DecomposedFlowRescaler {

    protected AbstractDecomposedRescaler() {
        // empty constructor
    }

    protected abstract boolean shouldRescaleFlows(DecomposedFlow decomposedFlow);

    protected abstract RescaledFlows computeRescaledFlows(DecomposedFlow decomposedFlow, Network network);

    @Override
    public DecomposedFlow rescale(DecomposedFlow decomposedFlow, Network network) {
        if (!shouldRescaleFlows(decomposedFlow)) {
            return decomposedFlow;
        }

        RescaledFlows rescaledFlows = computeRescaledFlows(decomposedFlow, network);

        return new DecomposedFlowBuilder()
                .withBranchId(decomposedFlow.getBranchId())
                .withContingencyId(decomposedFlow.getContingencyId())
                .withCountry1(decomposedFlow.getCountry1())
                .withCountry2(decomposedFlow.getCountry2())
                .withAcTerminal1ReferenceFlow(decomposedFlow.getAcTerminal1ReferenceFlow())
                .withAcTerminal2ReferenceFlow(decomposedFlow.getAcTerminal2ReferenceFlow())
                .withDcReferenceFlow(decomposedFlow.getDcReferenceFlow())
                .withAcCurrentTerminal1(decomposedFlow.getAcTerminal1Current())
                .withAcCurrentTerminal2(decomposedFlow.getAcTerminal2Current())
                .withAllocatedFlow(rescaledFlows.rescaledAllocatedFlow())
                .withXNodeFlow(rescaledFlows.rescaledXNodeFlow())
                .withPstFlow(rescaledFlows.rescaledPstFlow())
                .withInternalFlow(rescaledFlows.rescaleInternalFlow())
                .withLoopFlowsMap(rescaledFlows.rescaledLoopFlows())
                .build();
    }

    protected record RescaledFlows(double rescaledAllocatedFlow, double rescaledXNodeFlow, double rescaledPstFlow, double rescaleInternalFlow, Map<String, Double> rescaledLoopFlows) { }
}
