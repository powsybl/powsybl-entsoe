/**
 * Copyright (c) 2023, Coreso SA (https://www.coreso.eu/) and TSCNET Services GmbH (https://www.tscnet.eu/)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.balances_adjustment.util;

import com.powsybl.commons.reporter.Report;
import com.powsybl.commons.reporter.Reporter;
import com.powsybl.commons.reporter.TypedValue;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * @author George Budau {@literal <george.budau at artelys.com>}
 */
public final class Reports {

    private static final String ITERATION = "iteration";
    private static final String AREA_NAME = "areaName";

    private Reports() {
    }

    public static void reportScaling(Reporter reporter, String areaName, double offset, double done) {
        reporter.report(Report.builder()
                .withKey("areaScaling")
                .withDefaultMessage("Scaling for area ${areaName}: offset=${offset}, done=${done}")
                .withValue(AREA_NAME, areaName)
                .withValue("offset", offset)
                .withValue("done", done)
                .withSeverity(TypedValue.INFO_SEVERITY)
                .build());
    }

    public static void reportLfStatus(Reporter reporter, int networkNumCc, int networkNumSc, String status, TypedValue severity) {
        reporter.report(Report.builder()
                .withKey("lfStatus")
                .withDefaultMessage("Network CC${networkNumCc} SC${networkNumSc} Load flow complete with status '${status}'")
                .withValue("networkNumCc", networkNumCc)
                .withValue("networkNumSc", networkNumSc)
                .withValue("status", status)
                .withSeverity(severity)
                .build());
    }

    public static void reportAreaMismatch(Reporter reporter, String areaName, double mismatch, double target, double balance) {
        reporter.report(Report.builder()
                .withKey("areaMismatch")
                .withDefaultMessage("Mismatch for area ${areaName}: ${mismatch} (target=${target}, balance=${balance})")
                .withValue(AREA_NAME, areaName)
                .withValue("mismatch", mismatch)
                .withValue("target", target)
                .withValue("balance", balance)
                .withSeverity(TypedValue.INFO_SEVERITY)
                .build());
    }

    public static void reportBalancedAreas(Reporter reporter, List<String> networkAreasName, int iterationCount) {
        reporter.report(Report.builder()
                .withKey("balancedAreas")
                .withDefaultMessage("Areas ${networkAreasName} are balanced after ${iterationCount} iterations")
                .withValue("networkAreasName", networkAreasName.toString())
                .withValue("iterationCount", iterationCount)
                .withSeverity(TypedValue.INFO_SEVERITY)
                .build());
    }

    public static void reportUnbalancedAreas(Reporter reporter, int iteration, BigDecimal totalMismatch) {
        reporter.report(Report.builder()
                .withKey("unbalancedAreas")
                .withDefaultMessage("Areas are unbalanced after ${iteration} iterations, total mismatch is ${totalMismatch}")
                .withValue(ITERATION, iteration)
                .withValue("totalMismatch", totalMismatch.toString())
                .withSeverity(TypedValue.ERROR_SEVERITY)
                .build());
    }

    public static Reporter createBalanceComputationIterationReporter(Reporter reporter, int iteration) {
        return reporter.createSubReporter("balanceComputation", "Balances Computation iteration '${iteration}'",
                Map.of(ITERATION, new TypedValue(iteration, TypedValue.UNTYPED)));
    }

    public static Reporter createLfReporter(Reporter reporter, int networkNumCc, int networkNumSc) {
        return reporter.createSubReporter("loadFlowStatus", "Checking Load flow status",
                Map.of("networkNumCc", new TypedValue(networkNumCc, TypedValue.UNTYPED),
                        "networkNumSc", new TypedValue(networkNumSc, TypedValue.UNTYPED)));
    }
}
