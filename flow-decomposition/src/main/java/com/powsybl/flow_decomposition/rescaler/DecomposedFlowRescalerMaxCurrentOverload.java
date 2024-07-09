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

        String branchId = decomposedFlow.getBranchId();
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
        CurrentLimits currentLimitsTerminal1 = decomposedFlow.getCurrentLimitsTerminal1();
        CurrentLimits currentLimitsTerminal2 = decomposedFlow.getCurrentLimitsTerminal2();

        // if branch has no limits, compare currents
        // if it does, compare overloads
        double acReferenceFlow;
        if (currentLimitsTerminal1 == null || currentLimitsTerminal2 == null) {
            acReferenceFlow = acTerminal1Current >= acTerminal2Current ? acTerminal1ReferenceFlow : acTerminal2ReferenceFlow;
        } else {
            double permanentLimitTerminal1 = currentLimitsTerminal1.getPermanentLimit();
            double permanentLimitTerminal2 = currentLimitsTerminal2.getPermanentLimit();
            double currentOverloadTerminal1 = acTerminal1Current / permanentLimitTerminal1;
            double currentOverloadTerminal2 = acTerminal2Current / permanentLimitTerminal2;
            acReferenceFlow = currentOverloadTerminal1 >= currentOverloadTerminal2 ? acTerminal1ReferenceFlow : acTerminal2ReferenceFlow;
        }
        double rescaleFactor = Math.abs(acReferenceFlow / dcReferenceFlow);

        double rescaledAllocatedFlow = rescaleFactor * allocatedFlow;
        double rescaledXNodeFlow = rescaleFactor * xNodeFlow;
        double rescaledPstFlow = rescaleFactor * pstFlow;
        double rescaleInternalFlow = rescaleFactor * internalFlow;
        Map<String, Double> rescaledLoopFlows = loopFlows.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> rescaleFactor * entry.getValue()));

        return new DecomposedFlowBuilder()
                .withBranchId(branchId)
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
                .withAcTerminal1Current(acTerminal1Current)
                .withAcTerminal2Current(acTerminal2Current)
                .withCurrentLimitsTerminal1(currentLimitsTerminal1)
                .withCurrentLimitsTerminal2(currentLimitsTerminal2)
                .build();
    }
}
