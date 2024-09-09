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
import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.CurrentLimits;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Caio Luke {@literal <caio.luke at artelys.com>}
 */
public class DecomposedFlowRescalerMaxCurrentOverload implements DecomposedFlowRescaler {

    private final double minFlowTolerance; // min flow in MW to rescale

    public DecomposedFlowRescalerMaxCurrentOverload(double minFlowTolerance) {
        this.minFlowTolerance = minFlowTolerance;
    }

    public DecomposedFlowRescalerMaxCurrentOverload() {
        this(DecomposedFlowRescalerProportional.DEFAULT_MIN_FLOW_TOLERANCE);
    }

    @Override
    public DecomposedFlow rescale(DecomposedFlow decomposedFlow) {
        double acTerminal1ReferenceFlow = decomposedFlow.getAcTerminal1ReferenceFlow();
        double acTerminal2ReferenceFlow = decomposedFlow.getAcTerminal2ReferenceFlow();
        if (Double.isNaN(acTerminal1ReferenceFlow) || Double.isNaN(acTerminal2ReferenceFlow)) {
            return decomposedFlow;
        }

        // if dcReferenceFlow is too small, do not rescale
        double dcReferenceFlow = decomposedFlow.getDcReferenceFlow();
        if (Math.abs(dcReferenceFlow) < minFlowTolerance) {
            return decomposedFlow;
        }

        Branch<?> branch = decomposedFlow.getBranch();
        String contingencyId = decomposedFlow.getContingencyId();
        Country country1 = decomposedFlow.getCountry1();
        Country country2 = decomposedFlow.getCountry2();
        double allocatedFlow = decomposedFlow.getAllocatedFlow();
        double xNodeFlow = decomposedFlow.getXNodeFlow();
        double pstFlow = decomposedFlow.getPstFlow();
        double internalFlow = decomposedFlow.getInternalFlow();
        Map<String, Double> loopFlows = decomposedFlow.getLoopFlows();

        double acTerminal1Current = decomposedFlow.getAcTerminal1Current();
        double acTerminal2Current = decomposedFlow.getAcTerminal2Current();
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
            double permanentLimitTerminal1 = currentLimitsTerminal1.getPermanentLimit();
            double permanentLimitTerminal2 = currentLimitsTerminal2.getPermanentLimit();
            double currentOverloadTerminal1 = acTerminal1Current / permanentLimitTerminal1;
            double currentOverloadTerminal2 = acTerminal2Current / permanentLimitTerminal2;
            pActivePowerOnly = currentOverloadTerminal1 >= currentOverloadTerminal2 ? pTerminal1ActivePowerOnly : pTerminal2ActivePowerOnly;
        }
        double rescaleFactor = Math.abs(pActivePowerOnly / dcReferenceFlow);

        double rescaledAllocatedFlow = rescaleFactor * allocatedFlow;
        double rescaledXNodeFlow = rescaleFactor * xNodeFlow;
        double rescaledPstFlow = rescaleFactor * pstFlow;
        double rescaleInternalFlow = rescaleFactor * internalFlow;
        Map<String, Double> rescaledLoopFlows = loopFlows.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> rescaleFactor * entry.getValue()));

        return new DecomposedFlowBuilder()
                .withBranch(branch)
                .withContingencyId(contingencyId)
                .withCountry1(country1)
                .withCountry2(country2)
                .withAcTerminal1ReferenceFlow(acTerminal1ReferenceFlow)
                .withAcTerminal2ReferenceFlow(acTerminal2ReferenceFlow)
                .withDcReferenceFlow(dcReferenceFlow)
                .withAllocatedFlow(rescaledAllocatedFlow)
                .withXNodeFlow(rescaledXNodeFlow)
                .withPstFlow(rescaledPstFlow)
                .withInternalFlow(rescaleInternalFlow)
                .withLoopFlowsMap(rescaledLoopFlows)
                .withAcCurrentTerminal1(acTerminal1Current)
                .withAcCurrentTerminal2(acTerminal2Current)
                .build();
    }
}
