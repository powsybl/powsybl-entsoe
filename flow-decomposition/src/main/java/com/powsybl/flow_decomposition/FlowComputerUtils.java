/*
 * Copyright (c) 2024, Coreso SA (https://www.coreso.eu/) and TSCNET Services GmbH (https://www.tscnet.eu/)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.flow_decomposition;

import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Identifiable;
import com.powsybl.iidm.network.TwoSides;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 * @author Guillaume Verger {@literal <guillaume.verger at artelys.com>}
 * @author Caio Luke {@literal <caio.luke at artelys.com>}
 */
public final class FlowComputerUtils {

    private FlowComputerUtils() {
        // empty constructor
    }

    public static Map<String, Double> calculateAcTerminalReferenceFlows(Collection<Branch> xnecList, LoadFlowRunningService.Result loadFlowServiceAcResult, TwoSides side) {
        if (loadFlowServiceAcResult.fallbackHasBeenActivated()) {
            return xnecList.stream().collect(Collectors.toMap(Identifiable::getId, branch -> Double.NaN));
        }
        return getTerminalReferenceFlow(xnecList, side);
    }

    public static Map<String, Pair<Double, Double>> getBothTerminalsReferenceFlowsAc(Collection<Branch> xnecList, LoadFlowRunningService.Result loadFlowServiceAcResult) {
        if (loadFlowServiceAcResult.fallbackHasBeenActivated()) {
            return xnecList.stream().collect(Collectors.toMap(Identifiable::getId, branch -> Pair.of(Double.NaN, Double.NaN)));
        }
        return getBothTerminalsReferenceFlows(xnecList);
    }

    public static Map<String, Double> getTerminalReferenceFlow(Collection<Branch> xnecList, TwoSides side) {
        return xnecList.stream()
                .collect(Collectors.toMap(
                        Identifiable::getId,
                        branch -> branch.getTerminal(side).getP()
                ));
    }

    public static Map<String, Pair<Double, Double>> getBothTerminalsReferenceFlows(Collection<Branch> xnecList) {
        return xnecList.stream()
                .collect(Collectors.toMap(
                        Identifiable::getId,
                        branch -> Pair.of(branch.getTerminal1().getP(), branch.getTerminal2().getP())
                ));
    }

    public static Map<String, Double> calculateAcTerminalCurrents(Collection<Branch> xnecList, LoadFlowRunningService.Result loadFlowServiceAcResult, TwoSides side) {
        if (loadFlowServiceAcResult.fallbackHasBeenActivated()) {
            return xnecList.stream().collect(Collectors.toMap(Identifiable::getId, branch -> Double.NaN));
        }
        return getTerminalCurrent(xnecList, side);
    }

    public static Map<String, Pair<Double, Double>> getBothTerminalsCurrentsAc(Collection<Branch> xnecList, LoadFlowRunningService.Result loadFlowServiceAcResult) {
        if (loadFlowServiceAcResult.fallbackHasBeenActivated()) {
            return xnecList.stream().collect(Collectors.toMap(Identifiable::getId, branch -> Pair.of(Double.NaN, Double.NaN)));
        }
        return getBothTerminalsCurrents(xnecList);
    }

    public static Map<String, Double> getTerminalCurrent(Collection<Branch> xnecList, TwoSides side) {
        return xnecList.stream()
                .collect(Collectors.toMap(
                        Identifiable::getId,
                        branch -> branch.getTerminal(side).getI()
                ));
    }

    public static Map<String, Pair<Double, Double>> getBothTerminalsCurrents(Collection<Branch> xnecList) {
        return xnecList.stream()
                .collect(Collectors.toMap(
                        Identifiable::getId,
                        branch -> Pair.of(branch.getTerminal1().getI(), branch.getTerminal2().getI())
                ));
    }
}
