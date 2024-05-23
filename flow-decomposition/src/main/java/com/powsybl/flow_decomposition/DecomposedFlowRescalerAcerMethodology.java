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
public class DecomposedFlowRescalerAcerMethodology implements DecomposedFlowRescaler {

    public DecomposedFlowRescalerAcerMethodology() {
        // empty constructor
    }

    private static double reLU(double value) {
        return value > 0 ? value : 0.;
    }

    private static double rescaleValue(double initialValue, double delta, double sumOfReLUFlows) {
        return initialValue + delta * reLU(initialValue) / sumOfReLUFlows;
    }

    @Override
    public DecomposedFlow rescale(DecomposedFlow decomposedFlow) {
        double acReferenceFlow = decomposedFlow.getAcReferenceFlow();
        if (Double.isNaN(acReferenceFlow)) {
            return decomposedFlow;
        }

        String branchId = decomposedFlow.getBranchId();
        String contingencyId = decomposedFlow.getContingencyId();
        Country country1 = decomposedFlow.getCountry1();
        Country country2 = decomposedFlow.getCountry2();
        double acMaxFlow = decomposedFlow.getAcMaxFlow();
        double dcReferenceFlow = decomposedFlow.getDcReferenceFlow();
        double allocatedFlow = decomposedFlow.getAllocatedFlow();
        double xNodeFlow = decomposedFlow.getXNodeFlow();
        double pstFlow = decomposedFlow.getPstFlow();
        double internalFlow = decomposedFlow.getInternalFlow();
        Map<String, Double> loopFlows = decomposedFlow.getLoopFlows();

        double deltaToRescale = acReferenceFlow * Math.signum(acReferenceFlow) - decomposedFlow.getTotalFlow();
        double sumOfReLUFlows = reLU(allocatedFlow) + reLU(pstFlow) + reLU(xNodeFlow) + loopFlows.values().stream().mapToDouble(DecomposedFlowRescalerAcerMethodology::reLU).sum() + reLU(internalFlow);

        double rescaledAllocatedFlow = rescaleValue(allocatedFlow, deltaToRescale, sumOfReLUFlows);
        double rescaledXNodeFlow = rescaleValue(xNodeFlow, deltaToRescale, sumOfReLUFlows);
        double rescaledPstFlow = rescaleValue(pstFlow, deltaToRescale, sumOfReLUFlows);
        double rescaleInternalFlow = rescaleValue(internalFlow, deltaToRescale, sumOfReLUFlows);
        Map<String, Double> rescaledLoopFlows = loopFlows.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> rescaleValue(entry.getValue(), deltaToRescale, sumOfReLUFlows)));

        return new DecomposedFlowBuilder()
                .addBranchId(branchId)
                .addContingencyId(contingencyId)
                .addCountry1(country1)
                .addCountry2(country2)
                .addAcReferenceFlow(acReferenceFlow)
                .addAcMaxFlow(acMaxFlow)
                .addDcReferenceFlow(dcReferenceFlow)
                .addAllocatedFlow(rescaledAllocatedFlow)
                .addXNodeFlow(rescaledXNodeFlow)
                .addPstFlow(rescaledPstFlow)
                .addInternalFlow(rescaleInternalFlow)
                .addLoopFlowsMap(rescaledLoopFlows)
                .build();
    }
}
