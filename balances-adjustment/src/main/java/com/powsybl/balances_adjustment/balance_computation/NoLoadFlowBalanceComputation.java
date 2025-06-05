/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.balances_adjustment.balance_computation;

import com.powsybl.balances_adjustment.util.Reports;
import com.powsybl.commons.report.ReportNode;
import com.powsybl.iidm.modification.scalable.Scalable;
import com.powsybl.iidm.network.Network;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * @author Joris Mancini <joris.mancini_externe at rte-france.com>
 */
public class NoLoadFlowBalanceComputation implements BalanceComputation {
    private static final Logger LOGGER = LoggerFactory.getLogger(NoLoadFlowBalanceComputation.class);

    private final List<BalanceComputationArea> areas;

    public NoLoadFlowBalanceComputation(List<BalanceComputationArea> areas) {
        this.areas = areas;
    }

    @Override
    public CompletableFuture<BalanceComputationResult> run(Network network, String workingStateId, BalanceComputationParameters parameters) {
        return this.run(network, workingStateId, parameters, ReportNode.NO_OP);
    }

    @Override
    public CompletableFuture<BalanceComputationResult> run(Network network, String workingStateId, BalanceComputationParameters parameters, ReportNode reportNode) {
        Objects.requireNonNull(network);
        Objects.requireNonNull(workingStateId);
        Objects.requireNonNull(parameters);
        Objects.requireNonNull(reportNode);

        Map<BalanceComputationArea, Double> balanceOffsets = new HashMap<>();

        areas.parallelStream().forEach(area -> {
            var na = area.getNetworkAreaFactory().create(network);
            var offset = area.getTargetNetPosition() - na.getNetPosition();
            Scalable scalable = area.getScalable();
            double done = scalable.scale(network, offset, parameters.getScalingParameters());
            ReportNode scalingReportNode = reportNode.newReportNode().withMessageTemplate("scaling", "Scaling").add();
            Reports.reportScaling(scalingReportNode, area.getName(), offset, done);
            LOGGER.info("Scaling for area {}: offset={}, done={}", area.getName(), offset, done);
            balanceOffsets.put(area, done);
        });

        return CompletableFuture.completedFuture(new BalanceComputationResult(BalanceComputationResult.Status.SUCCESS, 1, balanceOffsets));
    }
}
