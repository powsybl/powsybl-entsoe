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

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 * @author Caio Luke {@literal <caio.luke at artelys.com>}
 */
public class DecomposedFlowRescalerAcerMethodology extends AbstractDecomposedFlowRescaler {

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
    protected boolean shouldRescaleFlows(DecomposedFlow decomposedFlow) {
        return hasFiniteAcFlowsOnEachTerminal(decomposedFlow);
    }

    @Override
    protected FlowPartition computeRescaledFlowsPartition(DecomposedFlow decomposedFlow, Network network) {
        FlowPartition initialPartition = decomposedFlow.getFlowPartition();
        double acTerminal1ReferenceFlow = decomposedFlow.getAcTerminal1ReferenceFlow();
        double allocatedFlow = initialPartition.allocatedFlow();
        double xNodeFlow = initialPartition.xNodeFlow();
        double pstFlow = initialPartition.pstFlow();
        double internalFlow = initialPartition.internalFlow();
        Map<Country, Double> loopFlows = initialPartition.loopFlowPerCountry();
        double deltaToRescale = acTerminal1ReferenceFlow * Math.signum(acTerminal1ReferenceFlow) - decomposedFlow.getTotalFlow();
        double sumOfReLUFlows = reLU(allocatedFlow) + reLU(pstFlow) + reLU(xNodeFlow) + loopFlows.values().stream().mapToDouble(DecomposedFlowRescalerAcerMethodology::reLU).sum() + reLU(internalFlow);

        double rescaledAllocatedFlow = rescaleValue(allocatedFlow, deltaToRescale, sumOfReLUFlows);
        double rescaledXNodeFlow = rescaleValue(xNodeFlow, deltaToRescale, sumOfReLUFlows);
        double rescaledPstFlow = rescaleValue(pstFlow, deltaToRescale, sumOfReLUFlows);
        double rescaleInternalFlow = rescaleValue(internalFlow, deltaToRescale, sumOfReLUFlows);
        Map<Country, Double> rescaledLoopFlows = loopFlows.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> rescaleValue(entry.getValue(), deltaToRescale, sumOfReLUFlows)));
        return new FlowPartition(rescaleInternalFlow, rescaledAllocatedFlow, rescaledLoopFlows, rescaledPstFlow, rescaledXNodeFlow);
    }
}
