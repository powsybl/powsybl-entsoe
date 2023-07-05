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
    private static final String NETWORK_ID = "networkId";

    private static final String AREA_NAME = "areaName";

    private Reports() {
    }

    public static Reporter createLoadFlowReporter(Reporter reporter, String networkId, int iteration) {
        return reporter.createSubReporter("Balance computation loadFlow", "Load flow on network '${networkId}' iteration '${iteration}'",
                Map.of(NETWORK_ID, new TypedValue(networkId, TypedValue.UNTYPED), ITERATION, new TypedValue(iteration, TypedValue.UNTYPED)));
    }

    public static void reportScaling(Reporter reporter, int iteration, String areaName, double offset, double done) {
        reporter.report(Report.builder()
                .withKey("areaScaling")
                .withDefaultMessage("Iteration={}, Scaling for area {}: offset={}, done={}")
                .withValue(ITERATION, iteration)
                .withValue(AREA_NAME, areaName)
                .withValue("offset", offset)
                .withValue("done", done)
                .withSeverity(TypedValue.INFO_SEVERITY)
                .build());
    }

    public static void reportConvergenceError(Reporter reporter, int iteration) {
        reporter.report(Report.builder()
                .withKey("convergenceError")
                .withDefaultMessage("Iteration={}, LoadFlow on network does not converge")
                .withValue(ITERATION, iteration)
                .withSeverity(TypedValue.ERROR_SEVERITY)
                .build());
    }

    public static void reportAreaMismatch(Reporter reporter, int iteration, String areaName, double mismatch, double target, double balance) {
        reporter.report(Report.builder()
                .withKey("areaMismatch")
                .withDefaultMessage("Iteration={}, Mismatch for area {}: {} (target={}, balance={})")
                .withValue(ITERATION, iteration)
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
                .withDefaultMessage("Areas {} are balanced after {} iterations")
                .withValue("networkAreasName", networkAreasName.toString())
                .withValue("iterationCount", iterationCount)
                .withSeverity(TypedValue.INFO_SEVERITY)
                .build());
    }

    public static void reportUnbalancedAreas(Reporter reporter, int iteration, BigDecimal totalMismatch) {
        reporter.report(Report.builder()
                .withKey("unbalancedAreas")
                .withDefaultMessage("Areas are unbalanced after {} iterations, total mismatch is {}")
                .withValue(ITERATION, iteration)
                .withValue("totalMismatch", totalMismatch.toString())
                .withSeverity(TypedValue.ERROR_SEVERITY)
                .build());
    }

}
