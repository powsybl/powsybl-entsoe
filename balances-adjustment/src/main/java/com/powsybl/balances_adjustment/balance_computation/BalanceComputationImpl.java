/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.balances_adjustment.balance_computation;

import com.powsybl.balances_adjustment.util.NetworkArea;
import com.powsybl.balances_adjustment.util.Reports;
import com.powsybl.commons.report.ReportNode;
import com.powsybl.commons.report.TypedValue;
import com.powsybl.computation.ComputationManager;
import com.powsybl.iidm.modification.scalable.Scalable;
import com.powsybl.iidm.network.ComponentConstants;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * This class contains the balance adjustment computation process.
 * <p>
 *     The calculation starts with defined network and areas and consists
 *     of several stages :
 * <ul>
 *     <li>LoadFlow computation (optional)</li>
 *     <li>Comparison of network area's net position with the target value</li>
 *     <li>Apply injections scaling</li>
 * </ul>
 * </p>
 *
 * @author Ameni Walha {@literal <ameni.walha at rte-france.com>}
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 * @author Mathieu Bague {@literal <mathieu.bague at rte-france.com>}
 * @author Marine Guibert {@literal <marine.guibert at artelys.com>}
 * @author Damien Jeandemange {@literal <damien.jeandemange at artelys.com>}
 */
public class BalanceComputationImpl implements BalanceComputation {

    private static final Logger LOGGER = LoggerFactory.getLogger(BalanceComputationImpl.class);

    private final List<BalanceComputationArea> areas;

    private final ComputationManager computationManager;
    private final LoadFlow.Runner loadFlowRunner;

    public BalanceComputationImpl(List<BalanceComputationArea> areas, ComputationManager computationManager, LoadFlow.Runner loadFlowRunner) {
        this.areas = Objects.requireNonNull(areas);
        this.computationManager = Objects.requireNonNull(computationManager);
        this.loadFlowRunner = Objects.requireNonNull(loadFlowRunner);
    }

    /**
     * Run balances adjustment computation in several iterations
     */
    @Override
    public CompletableFuture<BalanceComputationResult> run(Network network, String workingStateId, BalanceComputationParameters parameters) {
        return this.run(network, workingStateId, parameters, ReportNode.NO_OP);
    }

    /**
     * Run balances adjustment computation in several iterations
     */
    @Override
    public CompletableFuture<BalanceComputationResult> run(Network network, String workingStateId, BalanceComputationParameters parameters, ReportNode reportNode) {
        Objects.requireNonNull(network);
        Objects.requireNonNull(workingStateId);
        Objects.requireNonNull(parameters);
        Objects.requireNonNull(reportNode);

        BalanceComputationRunningContext context = new BalanceComputationRunningContext(areas, network, parameters, reportNode);
        BalanceComputationResult result;

        String initialVariantId = network.getVariantManager().getWorkingVariantId();
        String workingVariantCopyId = workingStateId + " COPY";
        network.getVariantManager().cloneVariant(workingStateId, workingVariantCopyId);
        network.getVariantManager().setWorkingVariant(workingVariantCopyId);

        do {
            ReportNode iterationReportNode = Reports.createBalanceComputationIterationReporter(reportNode, context.getIterationNum());
            context.setIterationReportNode(iterationReportNode);

            // Step 1: Perform the scaling
            ReportNode scalingReportNode = Reports.createScalingReporter(iterationReportNode);
            context.getBalanceOffsets().forEach((area, offset) -> {
                Scalable scalable = area.getScalable();
                double done = scalable.scale(network, offset, parameters.getScalingParameters());
                Reports.reportAreaScaling(scalingReportNode, area.getName(), offset, done);
                LOGGER.info("Iteration={}, Scaling for area {}: offset={}, done={}", context.getIterationNum(), area.getName(), offset, done);
            });

            // Step 2: compute Load Flow (skip if isWithLoadFlow is false)
            if (parameters.isWithLoadFlow()) {
                LoadFlowResult loadFlowResult = loadFlowRunner.run(network, workingVariantCopyId, computationManager, parameters.getLoadFlowParameters(), iterationReportNode);
                if (!isLoadFlowResultOk(context, loadFlowResult)) {
                    LOGGER.error("Iteration={}, LoadFlow on network {} does not converge", context.getIterationNum(), network.getId());
                    result = new BalanceComputationResult(BalanceComputationResult.Status.FAILED, context.getIterationNum());
                    return CompletableFuture.completedFuture(result);
                }
            } else {
                // Report that LoadFlow was skipped
                Reports.createSkipLoadFlowReport(iterationReportNode);
                LOGGER.info("Iteration={}, LoadFlow computation skipped as per configuration", context.getIterationNum());
            }

            // Step 3: Compute balance and mismatch for each area
            ReportNode mismatchReportNode = Reports.createMismatchReporter(iterationReportNode);
            for (BalanceComputationArea area : areas) {
                NetworkArea na = context.getNetworkArea(area);
                double target = area.getTargetNetPosition();
                double balance = na.getNetPosition();
                double mismatch = target - balance;
                Reports.reportAreaMismatch(mismatchReportNode, area.getName(), mismatch, target, balance);
                LOGGER.info("Iteration={}, Mismatch for area {}: {} (target={}, balance={})", context.getIterationNum(), area.getName(), mismatch, target, balance);
                context.updateAreaOffsetAndMismatch(area, mismatch);
            }

            // Step 4: Checks balance adjustment results
            // When isWithLoadFlow is false, always return after one iteration
            if (!parameters.isWithLoadFlow() || computeTotalMismatch(context) < parameters.getThresholdNetPosition()) {
                result = new BalanceComputationResult(BalanceComputationResult.Status.SUCCESS, context.nextIteration(), context.getBalanceOffsets());
                network.getVariantManager().cloneVariant(workingVariantCopyId, workingStateId, true);
            } else {
                // Reset current variant with initial state
                network.getVariantManager().cloneVariant(workingStateId, workingVariantCopyId, true);
                result = new BalanceComputationResult(BalanceComputationResult.Status.FAILED, context.nextIteration(), context.getBalanceOffsets());
            }
        } while (context.getIterationNum() < parameters.getMaxNumberIterations() && result.getStatus() != BalanceComputationResult.Status.SUCCESS);

        ReportNode statusReportNode = Reports.createStatusReporter(reportNode);
        if (result.getStatus() == BalanceComputationResult.Status.SUCCESS) {
            List<String> networkAreasName = areas.stream()
                    .map(BalanceComputationArea::getName).collect(Collectors.toList());
            Reports.reportBalancedAreas(statusReportNode, networkAreasName, result.getIterationCount());
            LOGGER.info("Areas {} are balanced after {} iterations", networkAreasName, result.getIterationCount());

        } else {
            BigDecimal totalMismatch = BigDecimal.valueOf(computeTotalMismatch(context)).setScale(2, RoundingMode.UP);
            Reports.reportUnbalancedAreas(statusReportNode, context.getIterationNum(), totalMismatch);
            LOGGER.error("Areas are unbalanced after {} iterations, total mismatch is {}", context.getIterationNum(), totalMismatch);
        }

        network.getVariantManager().removeVariant(workingVariantCopyId);
        network.getVariantManager().setWorkingVariant(initialVariantId);

        return CompletableFuture.completedFuture(result);
    }

    /**
     * default implementation is sum of squared mismatches
     * @return total mismatch to compare with balance computation net position threshold
     */
    protected double computeTotalMismatch(BalanceComputationRunningContext context) {
        if (context.parameters.getMismatchMode() == BalanceComputationParameters.MismatchMode.SQUARED) {
            return context.getBalanceMismatches().values().stream().mapToDouble(Double::doubleValue)
                    .map(v -> v * v)
                    .sum();
        } else {
            return context.getBalanceMismatches().values().stream().mapToDouble(Double::doubleValue)
                    .map(Math::abs).max()
                    .orElse(0.0);
        }
    }

    /**
     * default implementation considers LF result OK if the largest synchronous component converged
     *
     * @param context        balance computation context
     * @param loadFlowResult LF result
     * @return true if the loadFlowResult is to be considered successful
     */
    protected boolean isLoadFlowResultOk(BalanceComputationRunningContext context, final LoadFlowResult loadFlowResult) {
        return loadFlowResult.getComponentResults().stream()
            .filter(cr -> ComponentConstants.MAIN_NUM == cr.getSynchronousComponentNum())
            .collect(Collectors.collectingAndThen(
                Collectors.toList(),
                list -> {
                    if (list.size() > 1) {
                        throw new IllegalStateException("Expecting no more than 1 main synchronous component in LoadFlowResult");
                    } else if (list.isEmpty()) {
                        return false;
                    }
                    final var cr = list.get(0);
                    ReportNode lfStatusReportNode = Reports.createLoadFlowStatusReporter(context.getIterationReportNode());
                    final var severity = cr.getStatus() == LoadFlowResult.ComponentResult.Status.CONVERGED ? TypedValue.INFO_SEVERITY : TypedValue.ERROR_SEVERITY;
                    Reports.reportLfStatus(lfStatusReportNode, cr.getConnectedComponentNum(), cr.getSynchronousComponentNum(), cr.getStatus().name(), severity);
                    return cr.getStatus() == LoadFlowResult.ComponentResult.Status.CONVERGED;
                })
            );
    }

    protected static class BalanceComputationRunningContext {
        Network network;
        BalanceComputationParameters parameters;
        private int iterationNum;
        private final Map<BalanceComputationArea, NetworkArea> networkAreas;
        private final Map<BalanceComputationArea, Double> balanceOffsets = new LinkedHashMap<>();
        private final Map<BalanceComputationArea, Double> balanceMismatches = new HashMap<>();
        private final ReportNode reportNode;
        private ReportNode iterationReportNode;

        public BalanceComputationRunningContext(List<BalanceComputationArea> areas, Network network, BalanceComputationParameters parameters) {
            this(areas, network, parameters, ReportNode.NO_OP);
        }

        public BalanceComputationRunningContext(List<BalanceComputationArea> areas, Network network, BalanceComputationParameters parameters, ReportNode reportNode) {
            this.iterationNum = 0;
            this.network = network;
            this.parameters = parameters;
            this.reportNode = reportNode;
            this.iterationReportNode = ReportNode.NO_OP;
            networkAreas = areas.stream().collect(Collectors.toMap(Function.identity(), ba -> ba.getNetworkAreaFactory().create(network), (v1, v2) -> v1, LinkedHashMap::new));
            balanceOffsets.clear();
            balanceMismatches.clear();
        }

        public BalanceComputationParameters getParameters() {
            return parameters;
        }

        public Network getNetwork() {
            return network;
        }

        public int getIterationNum() {
            return iterationNum;
        }

        public int nextIteration() {
            return ++iterationNum;
        }

        public NetworkArea getNetworkArea(BalanceComputationArea area) {
            return networkAreas.get(area);
        }

        public Map<BalanceComputationArea, Double> getBalanceOffsets() {
            return new LinkedHashMap<>(balanceOffsets);
        }

        public Map<BalanceComputationArea, Double> getBalanceMismatches() {
            return Map.copyOf(balanceMismatches);
        }

        public void updateAreaOffsetAndMismatch(BalanceComputationArea area, double mismatch) {
            double oldBalanceOffset = balanceOffsets.computeIfAbsent(area, k -> 0.0);
            balanceOffsets.put(area, oldBalanceOffset + mismatch);
            balanceMismatches.put(area, mismatch);
        }

        public ReportNode getReportNode() {
            return reportNode;
        }

        public ReportNode getIterationReportNode() {
            return iterationReportNode;
        }

        public BalanceComputationRunningContext setIterationReportNode(ReportNode iterationReportNode) {
            this.iterationReportNode = iterationReportNode;
            return this;
        }
    }
}
