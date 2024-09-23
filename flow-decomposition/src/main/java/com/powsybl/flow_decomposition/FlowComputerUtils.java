/*
 * Copyright (c) 2024, Coreso SA (https://www.coreso.eu/) and TSCNET Services GmbH (https://www.tscnet.eu/)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.flow_decomposition;

import com.powsybl.iidm.network.*;

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

    public static Map<String, Double> calculateAcTerminalReferenceFlows(Collection<Branch> xnecList, LoadFlowRunningService.Result loadFlowServiceAcResult, boolean enableResultsForPairedHalfLine, TwoSides side) {
        if (loadFlowServiceAcResult.fallbackHasBeenActivated()) {
            Map<String, Double> acTerminalReferenceFlows = xnecList.stream().collect(Collectors.toMap(Identifiable::getId, branch -> Double.NaN));
            if (!enableResultsForPairedHalfLine) {
                return acTerminalReferenceFlows;
            }
            xnecList.stream().filter(TieLine.class::isInstance).forEach(xnec -> {
                acTerminalReferenceFlows.put(((TieLine) xnec).getDanglingLine1().getId(), Double.NaN);
                acTerminalReferenceFlows.put(((TieLine) xnec).getDanglingLine2().getId(), Double.NaN);
            });
            return acTerminalReferenceFlows;
        }
        return getTerminalReferenceFlows(xnecList, enableResultsForPairedHalfLine, side);
    }

    public static Map<String, Double> getTerminalReferenceFlows(Collection<Branch> xnecList, boolean enableResultsForPairedHalfLine, TwoSides side) {
        Map<String, Double> acTerminalReferenceFlows = xnecList.stream()
                .collect(Collectors.toMap(
                        Identifiable::getId,
                        branch -> branch.getTerminal(side).getP()
                ));
        if (!enableResultsForPairedHalfLine) {
            return acTerminalReferenceFlows;
        }
        xnecList.stream().filter(TieLine.class::isInstance).forEach(xnec -> {
            DanglingLine danglingLine1 = ((TieLine) xnec).getDanglingLine1();
            DanglingLine danglingLine2 = ((TieLine) xnec).getDanglingLine2();
            acTerminalReferenceFlows.put(danglingLine1.getId(), getDanglingLineAcReferenceFlow(danglingLine1, side));
            acTerminalReferenceFlows.put(danglingLine2.getId(), getDanglingLineAcReferenceFlow(danglingLine2, side));
        });
        return acTerminalReferenceFlows;
    }

    private static Double getDanglingLineAcReferenceFlow(DanglingLine danglingLine, TwoSides side) {
        return side.equals(TwoSides.ONE) ? danglingLine.getTerminal().getP() : danglingLine.getBoundary().getP();
    }

    public static Map<String, Double> calculateAcTerminalCurrents(Collection<Branch> xnecList, LoadFlowRunningService.Result loadFlowServiceAcResult, boolean enableResultsForPairedHalfLine, TwoSides side) {
        if (loadFlowServiceAcResult.fallbackHasBeenActivated()) {
            Map<String, Double> acTerminalCurrents = xnecList.stream().collect(Collectors.toMap(Identifiable::getId, branch -> Double.NaN));
            if (!enableResultsForPairedHalfLine) {
                return acTerminalCurrents;
            }
            xnecList.stream().filter(TieLine.class::isInstance).forEach(xnec -> {
                acTerminalCurrents.put(((TieLine) xnec).getDanglingLine1().getId(), Double.NaN);
                acTerminalCurrents.put(((TieLine) xnec).getDanglingLine2().getId(), Double.NaN);
            });
            return acTerminalCurrents;
        }
        return getTerminalCurrent(xnecList, enableResultsForPairedHalfLine, side);
    }

    public static Map<String, Double> getTerminalCurrent(Collection<Branch> xnecList, boolean enableResultsForPairedHalfLine, TwoSides side) {
        Map<String, Double> acTerminalCurrents = xnecList.stream()
                .collect(Collectors.toMap(
                        Identifiable::getId,
                        branch -> branch.getTerminal(side).getI()
                ));
        if (!enableResultsForPairedHalfLine) {
            return acTerminalCurrents;
        }
        xnecList.stream().filter(TieLine.class::isInstance).forEach(xnec -> {
            DanglingLine danglingLine1 = ((TieLine) xnec).getDanglingLine1();
            DanglingLine danglingLine2 = ((TieLine) xnec).getDanglingLine2();
            acTerminalCurrents.put(danglingLine1.getId(), getDanglingLineAcCurrent(danglingLine1, side));
            acTerminalCurrents.put(danglingLine2.getId(), getDanglingLineAcCurrent(danglingLine2, side));
        });
        return acTerminalCurrents;
    }

    private static Double getDanglingLineAcCurrent(DanglingLine danglingLine, TwoSides side) {
        if (side.equals(TwoSides.ONE)) {
            return danglingLine.getTerminal().getI();
        }
        Boundary boundary = danglingLine.getBoundary();
        return Math.hypot(boundary.getP(), boundary.getQ()) / (Math.sqrt(3.) * boundary.getV() / 1000);
    }
}
