/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.flow_decomposition;

import com.powsybl.commons.PowsyblException;
import com.powsybl.iidm.network.Country;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 * @author Caio Luke {@literal <caio.luke at artelys.com>}
 */
final class DecomposedFlowsRescaler {

    private static final double MIN_FLOW_TOLERANCE = 1E-6; // min flow in MW to rescale

    private DecomposedFlowsRescaler() {
    }

    private static double reLU(double value) {
        return value > 0 ? value : 0.;
    }

    private static double rescaleValue(double initialValue, double delta, double sumOfReLUFlows) {
        return initialValue + delta * reLU(initialValue) / sumOfReLUFlows;
    }

    static DecomposedFlow rescale(DecomposedFlow decomposedFlow, FlowDecompositionParameters.RescaleMode rescaleMode) {
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

        switch (rescaleMode) {
            case RELU -> {
                double deltaToRescale = acReferenceFlow * Math.signum(acReferenceFlow) - decomposedFlow.getTotalFlow();
                double sumOfReLUFlows = reLU(allocatedFlow) + reLU(pstFlow) + reLU(xNodeFlow) + loopFlows.values().stream().mapToDouble(DecomposedFlowsRescaler::reLU).sum() + reLU(internalFlow);

                double rescaledAllocatedFlow = rescaleValue(allocatedFlow, deltaToRescale, sumOfReLUFlows);
                double rescaledXNodeFlow = rescaleValue(xNodeFlow, deltaToRescale, sumOfReLUFlows);
                double rescaledPstFlow = rescaleValue(pstFlow, deltaToRescale, sumOfReLUFlows);
                double rescaleInternalFlow = rescaleValue(internalFlow, deltaToRescale, sumOfReLUFlows);
                Map<String, Double> rescaledLoopFlows = loopFlows.entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, entry -> rescaleValue(entry.getValue(), deltaToRescale, sumOfReLUFlows)));

                return new DecomposedFlow(branchId, contingencyId, country1, country2, acReferenceFlow, acMaxFlow, dcReferenceFlow, rescaledAllocatedFlow, rescaledXNodeFlow, rescaledPstFlow, rescaleInternalFlow, rescaledLoopFlows);
            }
            case PROPORTIONAL -> {
                // if dcReferenceFlow is too small, do not rescale
                if (Math.abs(dcReferenceFlow) < MIN_FLOW_TOLERANCE) {
                    return decomposedFlow;
                }
                double rescaleFactor = Math.abs(acMaxFlow / dcReferenceFlow);

                double rescaledAllocatedFlow = rescaleFactor * allocatedFlow;
                double rescaledXNodeFlow = rescaleFactor * xNodeFlow;
                double rescaledPstFlow = rescaleFactor * pstFlow;
                double rescaleInternalFlow = rescaleFactor * internalFlow;
                Map<String, Double> rescaledLoopFlows = loopFlows.entrySet().stream()
                        .collect(Collectors.toMap(Map.Entry::getKey, entry -> rescaleFactor * entry.getValue()));

                return new DecomposedFlow(branchId, contingencyId, country1, country2, acReferenceFlow, acMaxFlow, dcReferenceFlow, rescaledAllocatedFlow, rescaledXNodeFlow, rescaledPstFlow, rescaleInternalFlow, rescaledLoopFlows);
            }
            default -> throw new PowsyblException("Rescale mode not defined: " + rescaleMode);
        }
    }
}
