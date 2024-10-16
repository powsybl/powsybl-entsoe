/*
 * Copyright (c) 2024, Coreso SA (https://www.coreso.eu/) and TSCNET Services GmbH (https://www.tscnet.eu/)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.flow_decomposition;

import com.powsybl.commons.PowsyblException;
import com.powsybl.iidm.network.*;

import java.util.Map;
import java.util.Set;
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

    public static Map<String, Double> calculateAcTerminalReferenceFlows(Set<Identifiable<?>> xnecList, LoadFlowRunningService.Result loadFlowServiceAcResult, TwoSides side) {
        if (loadFlowServiceAcResult.fallbackHasBeenActivated()) {
            return xnecList.stream().collect(Collectors.toMap(Identifiable::getId, branch -> Double.NaN));
        }
        return getTerminalReferenceFlow(xnecList, side);
    }

    public static Map<String, Double> getTerminalReferenceFlow(Set<Identifiable<?>> xnecList, TwoSides side) {
        return xnecList.stream()
                .collect(Collectors.toMap(
                        Identifiable::getId,
                        identifiable -> {
                            if (identifiable instanceof Branch<?> branch) {
                                return branch.getTerminal(side).getP();
                            } else if (identifiable instanceof DanglingLine danglingLine) {
                                return TwoSides.ONE.equals(side) ? danglingLine.getTerminal().getP() : danglingLine.getBoundary().getP();
                            } else {
                                throw new PowsyblException("xnecList should contain only Branches and paired DanglingLines");
                            }
                        }
                ));
    }

    public static Map<String, Double> calculateAcTerminalCurrents(Set<Identifiable<?>> xnecList, LoadFlowRunningService.Result loadFlowServiceAcResult, TwoSides side) {
        if (loadFlowServiceAcResult.fallbackHasBeenActivated()) {
            return xnecList.stream().collect(Collectors.toMap(Identifiable::getId, branch -> Double.NaN));
        }
        return getTerminalCurrent(xnecList, side);
    }

    public static Map<String, Double> getTerminalCurrent(Set<Identifiable<?>> xnecList, TwoSides side) {
        return xnecList.stream()
                .collect(Collectors.toMap(
                        Identifiable::getId,
                        identifiable -> {
                            if (identifiable instanceof Branch<?> branch) {
                                return branch.getTerminal(side).getI();
                            } else if (identifiable instanceof DanglingLine danglingLine) {
                                return getDanglingLineAcCurrent(danglingLine, side);
                            } else {
                                throw new PowsyblException("xnecList should contain only Branches and paired DanglingLines");
                            }
                        }
                ));
    }

    private static Double getDanglingLineAcCurrent(DanglingLine danglingLine, TwoSides side) {
        // FIXME once this PR in powsybl-core is merged (https://github.com/powsybl/powsybl-core/pull/3168)
        // currently boundary don't have a method .getI()
        if (TwoSides.ONE.equals(side)) {
            return danglingLine.getTerminal().getI();
        }
        Boundary boundary = danglingLine.getBoundary();
        return Math.hypot(boundary.getP(), boundary.getQ()) / (Math.sqrt(3.) * boundary.getV() / 1000);
    }
}
