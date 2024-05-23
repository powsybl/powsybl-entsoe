/**
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.flow_decomposition;

import com.powsybl.iidm.network.Country;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 * @author Caio Luke {@literal <caio.luke at artelys.com>}
 */
public class DecomposedFlowRescalerProportional implements DecomposedFlowRescaler {
    private static final double MIN_FLOW_TOLERANCE = 1E-6; // min flow in MW to rescale

    public DecomposedFlowRescalerProportional() {
        // empty constructor
    }

    @Override
    public DecomposedFlow rescale(DecomposedFlow decomposedFlow) {
        double acReferenceFlow = decomposedFlow.getAcReferenceFlow();
        if (Double.isNaN(acReferenceFlow)) {
            return decomposedFlow;
        }

        // if dcReferenceFlow is too small, do not rescale
        double dcReferenceFlow = decomposedFlow.getDcReferenceFlow();
        if (Math.abs(dcReferenceFlow) < MIN_FLOW_TOLERANCE) {
            return decomposedFlow;
        }

        String branchId = decomposedFlow.getBranchId();
        String contingencyId = decomposedFlow.getContingencyId();
        Country country1 = decomposedFlow.getCountry1();
        Country country2 = decomposedFlow.getCountry2();
        double acMaxFlow = decomposedFlow.getAcMaxFlow();
        double allocatedFlow = decomposedFlow.getAllocatedFlow();
        double xNodeFlow = decomposedFlow.getXNodeFlow();
        double pstFlow = decomposedFlow.getPstFlow();
        double internalFlow = decomposedFlow.getInternalFlow();
        Map<String, Double> loopFlows = decomposedFlow.getLoopFlows();

        double rescaleFactor = Math.abs(acMaxFlow / dcReferenceFlow);

        double rescaledAllocatedFlow = rescaleFactor * allocatedFlow;
        double rescaledXNodeFlow = rescaleFactor * xNodeFlow;
        double rescaledPstFlow = rescaleFactor * pstFlow;
        double rescaleInternalFlow = rescaleFactor * internalFlow;
        Map<String, Double> rescaledLoopFlows = loopFlows.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> rescaleFactor * entry.getValue()));

        return new DecomposedFlow(branchId, contingencyId, country1, country2, acReferenceFlow, acMaxFlow, dcReferenceFlow, rescaledAllocatedFlow, rescaledXNodeFlow, rescaledPstFlow, rescaleInternalFlow, rescaledLoopFlows);
    }
}
