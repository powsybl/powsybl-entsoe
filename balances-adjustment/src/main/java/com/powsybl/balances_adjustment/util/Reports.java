/**
 * Copyright (c) 2023, Coreso SA (https://www.coreso.eu/) and TSCNET Services GmbH (https://www.tscnet.eu/)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.balances_adjustment.util;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.commons.report.TypedValue;

import java.math.BigDecimal;
import java.util.List;

/**
 * @author George Budau {@literal <george.budau at artelys.com>}
 */
public final class Reports {

    private static final String ITERATION = "iteration";
    private static final String AREA_NAME = "areaName";

    private Reports() {
    }

    public static void reportScaling(ReportNode reportNode, String areaName, double offset, double done) {
        reportNode.newReportNode().withMessageTemplate("areaScaling",
                        "Scaling for area ${areaName}: offset=${offset}, done=${done}")
                .withUntypedValue(AREA_NAME, areaName)
                .withUntypedValue("offset", offset)
                .withUntypedValue("done", done)
                .withSeverity(TypedValue.INFO_SEVERITY)
                .add();
    }

    public static void reportLfStatus(ReportNode reportNode, int networkNumCc, int networkNumSc, String status, TypedValue severity) {
        reportNode.newReportNode().withMessageTemplate("lfStatus",
                        "Network CC${networkNumCc} SC${networkNumSc} Load flow complete with status '${status}'")
                .withUntypedValue("networkNumCc", networkNumCc)
                .withUntypedValue("networkNumSc", networkNumSc)
                .withUntypedValue("status", status)
                .withSeverity(severity)
                .add();
    }

    public static void reportAreaMismatch(ReportNode reportNode, String areaName, double mismatch, double target, double balance) {
        reportNode.newReportNode().withMessageTemplate("areaMismatch",
                        "Mismatch for area ${areaName}: ${mismatch} (target=${target}, balance=${balance})")
                .withUntypedValue(AREA_NAME, areaName)
                .withUntypedValue("mismatch", mismatch)
                .withUntypedValue("target", target)
                .withUntypedValue("balance", balance)
                .withSeverity(TypedValue.INFO_SEVERITY)
                .add();
    }

    public static void reportBalancedAreas(ReportNode reportNode, List<String> networkAreasName, int iterationCount) {
        reportNode.newReportNode().withMessageTemplate("balancedAreas",
                        "Areas ${networkAreasName} are balanced after ${iterationCount} iterations")
                .withUntypedValue("networkAreasName", networkAreasName.toString())
                .withUntypedValue("iterationCount", iterationCount)
                .withSeverity(TypedValue.INFO_SEVERITY)
                .add();
    }

    public static void reportUnbalancedAreas(ReportNode reportNode, int iteration, BigDecimal totalMismatch) {
        reportNode.newReportNode().withMessageTemplate("unbalancedAreas",
                "Areas are unbalanced after ${iteration} iterations, total mismatch is ${totalMismatch}")
                .withUntypedValue(ITERATION, iteration)
                .withUntypedValue("totalMismatch", totalMismatch.toString())
                .withSeverity(TypedValue.ERROR_SEVERITY)
                .add();
    }

    public static ReportNode createBalanceComputationIterationReporter(ReportNode reportNode, int iteration) {
        return reportNode.newReportNode().withMessageTemplate("balanceComputation", "Balance Computation iteration '${iteration}'")
                .withUntypedValue(ITERATION, iteration)
                .add();
    }
}
