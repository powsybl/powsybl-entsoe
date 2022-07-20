/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.flow_decomposition;

import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 */
class DecomposedFlowsRescaler {

    private double reLU(double value) {
        return value > 0 ? value : 0.;
    }

    private double rescaleValue(double initialValue, double delta, double sumOfReLUFlows) {
        return initialValue + delta * reLU(initialValue) / sumOfReLUFlows;
    }

    Map<String, DecomposedFlow> rescale(Map<String, DecomposedFlow> decomposedFlowMap) {
        Map<String, DecomposedFlow> rescaledDecomposedFlowMap = new TreeMap<>();
        decomposedFlowMap.forEach((s, decomposedFlow) -> rescaledDecomposedFlowMap.put(s, rescale(decomposedFlow)));
        return rescaledDecomposedFlowMap;
    }

    DecomposedFlow rescale(DecomposedFlow decomposedFlow) {
        double allocatedFlow = decomposedFlow.getAllocatedFlow();
        double pstFlow = decomposedFlow.getPstFlow();
        Map<String, Double> loopFlows = decomposedFlow.getLoopFlows();
        double acReferenceFlow = decomposedFlow.getAcReferenceFlow();
        double dcReferenceFlow = decomposedFlow.getDcReferenceFlow();
        double deltaToRescale = (acReferenceFlow * Math.signum(acReferenceFlow) - decomposedFlow.getTotalFlow());
        double sumOfReLUFlows = reLU(allocatedFlow) + reLU(pstFlow) + loopFlows.values().stream().mapToDouble(this::reLU).sum();
        Map<String, Double> rescaledLoopFlows = loopFlows.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> rescaleValue(entry.getValue(), deltaToRescale, sumOfReLUFlows)));
        double rescaledAllocatedFlow = rescaleValue(allocatedFlow, deltaToRescale, sumOfReLUFlows);
        double rescaledPstFlow = rescaleValue(pstFlow, deltaToRescale, sumOfReLUFlows);
        return new DecomposedFlow(rescaledLoopFlows, rescaledAllocatedFlow, rescaledPstFlow, acReferenceFlow, dcReferenceFlow);
    }
}
