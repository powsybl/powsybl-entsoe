/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.balances_adjustment.balance_computation;

import com.powsybl.balances_adjustment.util.NetworkArea;
import com.powsybl.computation.ComputationManager;
import com.powsybl.iidm.modification.scalable.Scalable;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        Objects.requireNonNull(network);
        Objects.requireNonNull(workingStateId);
        Objects.requireNonNull(parameters);

        BalanceComputationResult result;
        int iterationCounter = 0;

        String initialVariantId = network.getVariantManager().getWorkingVariantId();
        String workingVariantCopyId = workingStateId + " COPY";
        network.getVariantManager().cloneVariant(workingStateId, workingVariantCopyId);
        network.getVariantManager().setWorkingVariant(workingVariantCopyId);

        Map<BalanceComputationArea, Double> balanceOffsets = new HashMap<>();

        // Step 0: reset all network areas cache
        Map<BalanceComputationArea, NetworkArea> networkAreas = areas.stream()
                .collect(Collectors.toMap(Function.identity(), ba -> ba.getNetworkAreaFactory().create(network)));

        do {
            // Step 1: Perform the scaling
            for (Map.Entry<BalanceComputationArea, Double> entry : balanceOffsets.entrySet()) {
                BalanceComputationArea area = entry.getKey();
                double asked = entry.getValue();

                Scalable scalable = area.getScalable();
                double done = 0;
                if (parameters.isLoadPowerFactorConstant()) {
                    done = scalable.scaleWithConstantPowerFactor(network, balanceOffsets.get(area));
                } else {
                    done = scalable.scale(network, balanceOffsets.get(area));
                }
                LOGGER.info("Scaling for area {}: asked={}, done={}", area.getName(), asked, done);
            }

            // Step 2: compute Loadflow
            LoadFlowResult loadFlowResult = loadFlowRunner.run(network, workingVariantCopyId, computationManager, parameters.getLoadFlowParameters());
            if (!loadFlowResult.isOk()) {
                LOGGER.error("Loadflow on network {} does not converge", network.getId());
                result = new BalanceComputationResult(BalanceComputationResult.Status.FAILED, iterationCounter);
                return CompletableFuture.completedFuture(result);
            }

            // Step 3: Compute balance and mismatch for each area
            double mismatchesNorm = 0.0;
            for (BalanceComputationArea area : areas) {
                NetworkArea na = networkAreas.get(area);
                double target = area.getTargetNetPosition();
                double balance = na.getNetPosition();
                double oldMismatch = balanceOffsets.computeIfAbsent(area, k -> 0.0);
                double mismatch = target - balance;
                balanceOffsets.put(area, oldMismatch + mismatch);
                LOGGER.info("Mismatch for area {}: {} (target={}, balance={})", area.getName(), mismatch, target, balance);

                mismatchesNorm += mismatch * mismatch;
            }

            // Step 4: Checks balance adjustment results
            if (mismatchesNorm < parameters.getThresholdNetPosition()) {
                result = new BalanceComputationResult(BalanceComputationResult.Status.SUCCESS, ++iterationCounter, balanceOffsets);
                network.getVariantManager().cloneVariant(workingVariantCopyId, workingStateId, true);
            } else {
                // Reset current variant with initial state
                network.getVariantManager().cloneVariant(workingStateId, workingVariantCopyId, true);
                result = new BalanceComputationResult(BalanceComputationResult.Status.FAILED, ++iterationCounter, balanceOffsets);
            }
        } while (iterationCounter < parameters.getMaxNumberIterations() && result.getStatus() != BalanceComputationResult.Status.SUCCESS);

        if (result.getStatus() == BalanceComputationResult.Status.SUCCESS) {
            List<String> networkAreasName = areas.stream()
                    .map(BalanceComputationArea::getName).collect(Collectors.toList());
            LOGGER.info(" Areas : {} are balanced after {} iterations", networkAreasName, result.getIterationCount());

        } else {
            LOGGER.error(" Areas are unbalanced after {} iterations", iterationCounter);
        }

        network.getVariantManager().removeVariant(workingVariantCopyId);
        network.getVariantManager().setWorkingVariant(initialVariantId);

        return CompletableFuture.completedFuture(result);
    }
}
