/**
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.flow_decomposition.rescaler;

import com.powsybl.flow_decomposition.DecomposedFlow;
import com.powsybl.flow_decomposition.FlowPartition;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;

import java.util.Map;
import java.util.stream.Collectors;

import static com.powsybl.flow_decomposition.FlowDecompositionParameters.DEFAULT_PROPORTIONAL_RESCALER_MIN_FLOW_TOLERANCE;

/**
 * @author Caio Luke {@literal <caio.luke at artelys.com>}
 */
public class DecomposedFlowRescalerProportional extends AbstractDecomposedFlowRescaler {
    private final double minFlowTolerance; // min flow in MW to rescale

    public DecomposedFlowRescalerProportional(double minFlowTolerance) {
        this.minFlowTolerance = minFlowTolerance;
    }

    public DecomposedFlowRescalerProportional() {
        this(DEFAULT_PROPORTIONAL_RESCALER_MIN_FLOW_TOLERANCE);
    }

    @Override
    protected boolean shouldRescaleFlows(DecomposedFlow decomposedFlow) {
        return hasFiniteAcFlowsOnEachTerminal(decomposedFlow) && hasAbsDcFlowGreaterThanTolerance(decomposedFlow, minFlowTolerance);
    }

    @Override
    protected FlowPartition computeRescaledFlowsPartition(DecomposedFlow decomposedFlow, Network network) {
        // rescale proportionally to max (abs) ac flow
        FlowPartition initialFlowPartition = decomposedFlow.getFlowPartition();
        double acMaxAbsFlow = decomposedFlow.getMaxAbsAcFlow();
        double rescaleFactor = Math.abs(acMaxAbsFlow / decomposedFlow.getDcReferenceFlow());

        double rescaledAllocatedFlow = rescaleFactor * initialFlowPartition.allocatedFlow();
        double rescaledXNodeFlow = rescaleFactor * initialFlowPartition.xNodeFlow();
        double rescaledPstFlow = rescaleFactor * initialFlowPartition.pstFlow();
        double rescaleInternalFlow = rescaleFactor * initialFlowPartition.internalFlow();
        Map<Country, Double> rescaledLoopFlows = initialFlowPartition.loopFlowPerCountry().entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> rescaleFactor * entry.getValue()));
        return new FlowPartition(rescaleInternalFlow, rescaledAllocatedFlow, rescaledLoopFlows, rescaledPstFlow, rescaledXNodeFlow);
    }
}
