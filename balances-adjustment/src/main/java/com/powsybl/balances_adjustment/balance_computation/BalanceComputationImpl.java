/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.balances_adjustment.balance_computation;

import com.powsybl.balances_adjustment.util.NetworkArea;
import com.powsybl.balances_adjustment.util.Reports;
import com.powsybl.commons.reporter.Reporter;
import com.powsybl.commons.reporter.TypedValue;
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
 *     <li>LoadFlow computation</li>
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
        return this.run(network, workingStateId, parameters, Reporter.NO_OP);
    }

    /**
     * Run balances adjustment computation in several iterations
     */
    @Override
    public CompletableFuture<BalanceComputationResult> run(Network network, String workingStateId, BalanceComputationParameters parameters, Reporter reporter) {
        Objects.requireNonNull(network);
        Objects.requireNonNull(workingStateId);
        Objects.requireNonNull(parameters);
        Objects.requireNonNull(reporter);

        BalanceComputationRunningContext context = new BalanceComputationRunningContext(areas, network, parameters, reporter);
        BalanceComputationResult result;

        String initialVariantId = network.getVariantManager().getWorkingVariantId();
        String workingVariantCopyId = workingStateId + " COPY";
        network.getVariantManager().cloneVariant(workingStateId, workingVariantCopyId);
        network.getVariantManager().setWorkingVariant(workingVariantCopyId);

        do {
            Reporter iterationReporter = Reports.createBalanceComputationIterationReporter(reporter, context.getIterationNum());
            context.setIterationReporter(iterationReporter);
            // Step 1: Perform the scaling
            Reporter scalingReporter = iterationReporter.createSubReporter("scaling", "Scaling");
            context.getBalanceOffsets().forEach((area, offset) -> {
                Scalable scalable = area.getScalable();
                double done = scalable.scale(network, offset, parameters.getScalingParameters());
                Reports.reportScaling(scalingReporter, area.getName(), offset, done);
                LOGGER.info("Iteration={}, Scaling for area {}: offset={}, done={}", context.getIterationNum(), area.getName(), offset, done);
            });

            // Step 2: compute Load Flow
            LoadFlowResult loadFlowResult = loadFlowRunner.run(network, workingVariantCopyId, computationManager, parameters.getLoadFlowParameters(), iterationReporter);
            if (!isLoadFlowResultOk(context, loadFlowResult)) {
                LOGGER.error("Iteration={}, LoadFlow on network {} does not converge", context.getIterationNum(), network.getId());
                result = new BalanceComputationResult(BalanceComputationResult.Status.FAILED, context.getIterationNum());
                return CompletableFuture.completedFuture(result);
            }

            // Step 3: Compute balance and mismatch for each area
            Reporter mismatchReporter = iterationReporter.createSubReporter("mismatch", "Mismatch");
            for (BalanceComputationArea area : areas) {
                NetworkArea na = context.getNetworkArea(area);
                double target = area.getTargetNetPosition();
                double balance = na.getNetPosition();
                double mismatch = target - balance;
                Reports.reportAreaMismatch(mismatchReporter, area.getName(), mismatch, target, balance);
                LOGGER.info("Iteration={}, Mismatch for area {}: {} (target={}, balance={})", context.getIterationNum(), area.getName(), mismatch, target, balance);
                context.updateAreaOffsetAndMismatch(area, mismatch);
            }

            // Step 4: Checks balance adjustment results
            if (computeTotalMismatch(context) < parameters.getThresholdNetPosition()) {
                result = new BalanceComputationResult(BalanceComputationResult.Status.SUCCESS, context.nextIteration(), context.getBalanceOffsets());
                network.getVariantManager().cloneVariant(workingVariantCopyId, workingStateId, true);
            } else {
                // Reset current variant with initial state
                network.getVariantManager().cloneVariant(workingStateId, workingVariantCopyId, true);
                result = new BalanceComputationResult(BalanceComputationResult.Status.FAILED, context.nextIteration(), context.getBalanceOffsets());
            }
        } while (context.getIterationNum() < parameters.getMaxNumberIterations() && result.getStatus() != BalanceComputationResult.Status.SUCCESS);

        Reporter statusReporter = reporter.createSubReporter("status", "Status");
        if (result.getStatus() == BalanceComputationResult.Status.SUCCESS) {
            List<String> networkAreasName = areas.stream()
                    .map(BalanceComputationArea::getName).collect(Collectors.toList());
            Reports.reportBalancedAreas(statusReporter, networkAreasName, result.getIterationCount());
            LOGGER.info("Areas {} are balanced after {} iterations", networkAreasName, result.getIterationCount());

        } else {
            BigDecimal totalMismatch = BigDecimal.valueOf(computeTotalMismatch(context)).setScale(2, RoundingMode.UP);
            Reports.reportUnbalancedAreas(statusReporter, context.getIterationNum(), totalMismatch);
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
                    Reporter lfStatusReporter = context.getIterationReporter().createSubReporter("loadFlowStatus", "Checking load flow status");
                    final var severity = cr.getStatus() == LoadFlowResult.ComponentResult.Status.CONVERGED ? TypedValue.INFO_SEVERITY : TypedValue.ERROR_SEVERITY;
                    Reports.reportLfStatus(lfStatusReporter, cr.getConnectedComponentNum(), cr.getSynchronousComponentNum(), cr.getStatus().name(), severity);
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
        private final Reporter reporter;
        private Reporter iterationReporter;

        public BalanceComputationRunningContext(List<BalanceComputationArea> areas, Network network, BalanceComputationParameters parameters) {
            this(areas, network, parameters, Reporter.NO_OP);
        }

        public BalanceComputationRunningContext(List<BalanceComputationArea> areas, Network network, BalanceComputationParameters parameters, Reporter reporter) {
            this.iterationNum = 0;
            this.network = network;
            this.parameters = parameters;
            this.reporter = reporter;
            this.iterationReporter = Reporter.NO_OP;
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

        public Reporter getReporter() {
            return reporter;
        }

        public Reporter getIterationReporter() {
            return iterationReporter;
        }

        public BalanceComputationRunningContext setIterationReporter(Reporter iterationReporter) {
            this.iterationReporter = iterationReporter;
            return this;
        }
    }
}
