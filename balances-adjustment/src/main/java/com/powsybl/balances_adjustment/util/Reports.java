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

    public static void reportAreaScaling(ReportNode reportNode, String areaName, double offset, double done) {
        reportNode.newReportNode()
                .withMessageTemplate("entsoe.balances_adjustment.areaScaling")
                .withUntypedValue(AREA_NAME, areaName)
                .withUntypedValue("offset", offset)
                .withUntypedValue("done", done)
                .withSeverity(TypedValue.INFO_SEVERITY)
                .add();
    }

    public static void reportLfStatus(ReportNode reportNode, int networkNumCc, int networkNumSc, String status, TypedValue severity) {
        reportNode.newReportNode()
                .withMessageTemplate("entsoe.balances_adjustment.lfStatus")
                .withUntypedValue("networkNumCc", networkNumCc)
                .withUntypedValue("networkNumSc", networkNumSc)
                .withUntypedValue("status", status)
                .withSeverity(severity)
                .add();
    }

    public static void reportAreaMismatch(ReportNode reportNode, String areaName, double mismatch, double target, double balance) {
        reportNode.newReportNode()
                .withMessageTemplate("entsoe.balances_adjustment.areaMismatch")
                .withUntypedValue(AREA_NAME, areaName)
                .withUntypedValue("mismatch", mismatch)
                .withUntypedValue("target", target)
                .withUntypedValue("balance", balance)
                .withSeverity(TypedValue.INFO_SEVERITY)
                .add();
    }

    public static void reportBalancedAreas(ReportNode reportNode, List<String> networkAreasName, int iterationCount) {
        reportNode.newReportNode()
                .withMessageTemplate("entsoe.balances_adjustment.balancedAreas")
                .withUntypedValue("networkAreasName", networkAreasName.toString())
                .withUntypedValue("iterationCount", iterationCount)
                .withSeverity(TypedValue.INFO_SEVERITY)
                .add();
    }

    public static void reportUnbalancedAreas(ReportNode reportNode, int iteration, BigDecimal totalMismatch) {
        reportNode.newReportNode()
                .withMessageTemplate("entsoe.balances_adjustment.unbalancedAreas")
                .withUntypedValue(ITERATION, iteration)
                .withUntypedValue("totalMismatch", totalMismatch.toString())
                .withSeverity(TypedValue.ERROR_SEVERITY)
                .add();
    }

    public static ReportNode createBalanceComputationIterationReporter(ReportNode reportNode, int iteration) {
        return reportNode.newReportNode()
                .withMessageTemplate("entsoe.balances_adjustment.balanceComputation")
                .withUntypedValue(ITERATION, iteration)
                .add();
    }

    public static ReportNode createStatusReporter(ReportNode reportNode) {
        return reportNode.newReportNode()
                .withMessageTemplate("entsoe.balances_adjustment.status")
                .add();
    }

    public static ReportNode createMismatchReporter(ReportNode iterationReportNode) {
        return iterationReportNode.newReportNode()
                .withMessageTemplate("entsoe.balances_adjustment.mismatch")
                .add();
    }

    public static ReportNode createScalingReporter(ReportNode iterationReportNode) {
        return iterationReportNode.newReportNode()
                .withMessageTemplate("entsoe.balances_adjustment.scaling")
                .add();
    }

    public static ReportNode createLoadFlowStatusReporter(ReportNode reportNode) {
        return reportNode.newReportNode()
                .withMessageTemplate("entsoe.balances_adjustment.loadFlowStatus")
                .add();
    }
}
