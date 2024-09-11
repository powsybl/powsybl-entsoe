/**
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.flow_decomposition.rescaler;

import com.powsybl.flow_decomposition.DecomposedFlow;
import com.powsybl.iidm.network.Network;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Caio Luke {@literal <caio.luke at artelys.com>}
 */
public class DecomposedFlowRescalerProportional extends AbstractDecomposedRescaler {
    private final double minFlowTolerance; // min flow in MW to rescale
    public static final double DEFAULT_MIN_FLOW_TOLERANCE = 1E-6; // default min tolerance is 1 W = 1E-6 MW

    public DecomposedFlowRescalerProportional(double minFlowTolerance) {
        this.minFlowTolerance = minFlowTolerance;
    }

    public DecomposedFlowRescalerProportional() {
        this(DEFAULT_MIN_FLOW_TOLERANCE);
    }

    @Override
    protected boolean shouldRescaleFlows(DecomposedFlow decomposedFlow) {
        // - if AC flows are NaN
        // - if dcReferenceFlow is too small
        return !Double.isNaN(decomposedFlow.getAcTerminal1ReferenceFlow()) && !Double.isNaN(decomposedFlow.getAcTerminal2ReferenceFlow()) && !(Math.abs(decomposedFlow.getDcReferenceFlow()) < minFlowTolerance);
    }

    @Override
    protected RescaledFlows computeRescaledFlows(DecomposedFlow decomposedFlow, Network network) {
        // rescale proportionally to max (abs) ac flow
        double acMaxAbsFlow = decomposedFlow.getMaxAbsAcFlow();
        double rescaleFactor = Math.abs(acMaxAbsFlow / decomposedFlow.getDcReferenceFlow());

        double rescaledAllocatedFlow = rescaleFactor * decomposedFlow.getAllocatedFlow();
        double rescaledXNodeFlow = rescaleFactor * decomposedFlow.getXNodeFlow();
        double rescaledPstFlow = rescaleFactor * decomposedFlow.getPstFlow();
        double rescaleInternalFlow = rescaleFactor * decomposedFlow.getInternalFlow();
        Map<String, Double> rescaledLoopFlows = decomposedFlow.getLoopFlows().entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> rescaleFactor * entry.getValue()));

        return new RescaledFlows(rescaledAllocatedFlow, rescaledXNodeFlow, rescaledPstFlow, rescaleInternalFlow, rescaledLoopFlows);
    }
}
