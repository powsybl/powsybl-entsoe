/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.flow_decomposition;

import com.powsybl.iidm.network.Country;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 */
final class DecomposedFlowsRescaler {
    private DecomposedFlowsRescaler() {
    }

    private static double reLU(double value) {
        return value > 0 ? value : 0.;
    }

    private static double rescaleValue(double initialValue, double delta, double sumOfReLUFlows) {
        return initialValue + delta * reLU(initialValue) / sumOfReLUFlows;
    }

    static DecomposedFlow rescale(DecomposedFlow decomposedFlow) {
        double allocatedFlow = decomposedFlow.getAllocatedFlow();
        double pstFlow = decomposedFlow.getPstFlow();
        Map<String, Double> loopFlows = decomposedFlow.getLoopFlows();
        double acReferenceFlow = decomposedFlow.getAcReferenceFlow();
        double dcReferenceFlow = decomposedFlow.getDcReferenceFlow();
        Country country1 = decomposedFlow.getCountry1();
        Country country2 = decomposedFlow.getCountry2();
        double internalFlow = decomposedFlow.getInternalFlow();
        if (Double.isNaN(acReferenceFlow)) {
            return decomposedFlow;
        }
        double deltaToRescale = acReferenceFlow * Math.signum(acReferenceFlow) - decomposedFlow.getTotalFlow();
        double sumOfReLUFlows = reLU(allocatedFlow) + reLU(pstFlow) + loopFlows.values().stream().mapToDouble(DecomposedFlowsRescaler::reLU).sum() + reLU(internalFlow);
        Map<String, Double> rescaledLoopFlows = loopFlows.entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey, entry -> rescaleValue(entry.getValue(), deltaToRescale, sumOfReLUFlows)));
        double rescaledAllocatedFlow = rescaleValue(allocatedFlow, deltaToRescale, sumOfReLUFlows);
        double rescaledPstFlow = rescaleValue(pstFlow, deltaToRescale, sumOfReLUFlows);
        double rescaleInternalFlow = rescaleValue(internalFlow, deltaToRescale, sumOfReLUFlows);
        return new DecomposedFlow(rescaledLoopFlows, rescaleInternalFlow, rescaledAllocatedFlow, rescaledPstFlow, acReferenceFlow, dcReferenceFlow, country1, country2);
    }
}
