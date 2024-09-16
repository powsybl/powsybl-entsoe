/**
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.flow_decomposition.rescaler;

import com.powsybl.flow_decomposition.DecomposedFlow;
import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.CurrentLimits;
import com.powsybl.iidm.network.Network;

import java.util.Map;
import java.util.stream.Collectors;

import static com.powsybl.flow_decomposition.FlowDecompositionParameters.DEFAULT_PROPORTIONAL_RESCALER_MIN_FLOW_TOLERANCE;

/**
 * @author Caio Luke {@literal <caio.luke at artelys.com>}
 */
public class DecomposedFlowRescalerMaxCurrentOverload extends AbstractDecomposedFlowRescaler {

    private final double minFlowTolerance; // min flow in MW to rescale

    public DecomposedFlowRescalerMaxCurrentOverload(double minFlowTolerance) {
        this.minFlowTolerance = minFlowTolerance;
    }

    public DecomposedFlowRescalerMaxCurrentOverload() {
        this(DEFAULT_PROPORTIONAL_RESCALER_MIN_FLOW_TOLERANCE);
    }

    @Override
    protected boolean shouldRescaleFlows(DecomposedFlow decomposedFlow) {
        return hasFiniteAcFlowsOnEachTerminal(decomposedFlow) && hasAbsDcFlowGreaterThanTolerance(decomposedFlow, minFlowTolerance);
    }

    @Override
    protected RescaledFlows computeRescaledFlows(DecomposedFlow decomposedFlow, Network network) {
        double acTerminal1Current = decomposedFlow.getAcTerminal1Current();
        double acTerminal2Current = decomposedFlow.getAcTerminal2Current();

        Branch<?> branch = network.getBranch(decomposedFlow.getBranchId());
        double nominalTerminal1Voltage = branch.getTerminal1().getVoltageLevel().getNominalV();
        double nominalTerminal2Voltage = branch.getTerminal2().getVoltageLevel().getNominalV();
        CurrentLimits currentLimitsTerminal1 = branch.getNullableCurrentLimits1();
        CurrentLimits currentLimitsTerminal2 = branch.getNullableCurrentLimits2();

        // Calculate active power P = sqrt(3) * I * (V/1000) * cos(phi)
        // with cos(phi) = 1, therefore considering active power only
        double pTerminal1ActivePowerOnly = acTerminal1Current * (nominalTerminal1Voltage / 1000) * Math.sqrt(3);
        double pTerminal2ActivePowerOnly = acTerminal2Current * (nominalTerminal2Voltage / 1000) * Math.sqrt(3);

        // if branch has limits, compare current overloads
        // if it doesn't, compare currents
        double pActivePowerOnly;
        if (currentLimitsTerminal1 == null || currentLimitsTerminal2 == null) {
            pActivePowerOnly = acTerminal1Current >= acTerminal2Current ? pTerminal1ActivePowerOnly : pTerminal2ActivePowerOnly;
        } else {
            double currentOverloadTerminal1 = acTerminal1Current / currentLimitsTerminal1.getPermanentLimit();
            double currentOverloadTerminal2 = acTerminal2Current / currentLimitsTerminal2.getPermanentLimit();
            pActivePowerOnly = currentOverloadTerminal1 >= currentOverloadTerminal2 ? pTerminal1ActivePowerOnly : pTerminal2ActivePowerOnly;
        }
        double rescaleFactor = Math.abs(pActivePowerOnly / decomposedFlow.getDcReferenceFlow());

        double rescaledAllocatedFlow = rescaleFactor * decomposedFlow.getAllocatedFlow();
        double rescaledXNodeFlow = rescaleFactor * decomposedFlow.getXNodeFlow();
        double rescaledPstFlow = rescaleFactor * decomposedFlow.getPstFlow();
        double rescaleInternalFlow = rescaleFactor * decomposedFlow.getInternalFlow();
        Map<String, Double> rescaledLoopFlows = decomposedFlow.getLoopFlows().entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> rescaleFactor * entry.getValue()));

        return new RescaledFlows(rescaledAllocatedFlow, rescaledXNodeFlow, rescaledPstFlow, rescaleInternalFlow, rescaledLoopFlows);
    }
}
