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

    public static Map<String, Double> calculateAcReferenceFlows(Collection<Branch> xnecList, LoadFlowRunningService.Result loadFlowServiceAcResult) {
        if (loadFlowServiceAcResult.fallbackHasBeenActivated()) {
            return xnecList.stream().collect(Collectors.toMap(Identifiable::getId, branch -> Double.NaN));
        }
        return getReferenceFlow(xnecList);
    }

    public static Map<String, Double> calculateAcMaxFlows(Collection<Branch> xnecList, LoadFlowRunningService.Result loadFlowServiceAcResult) {
        if (loadFlowServiceAcResult.fallbackHasBeenActivated()) {
            return xnecList.stream().collect(Collectors.toMap(Identifiable::getId, branch -> Double.NaN));
        }
        return getMaxFlow(xnecList);
    }

    public static Map<String, Double> getReferenceFlow(Collection<Branch> xnecList) {
        return xnecList.stream()
                .collect(Collectors.toMap(
                        Identifiable::getId,
                        FlowComputerUtils::getReferenceP
                ));
    }

    public static Map<String, Double> getMaxFlow(Collection<Branch> xnecList) {
        return xnecList.stream()
                .collect(Collectors.toMap(
                        Identifiable::getId,
                        branch -> Math.max(Math.abs(branch.getTerminal1().getP()), Math.abs(branch.getTerminal2().getP()))
                ));
    }

    private static double getReferenceP(Branch branch) {
        return branch.getTerminal1().getP();
    }
}
