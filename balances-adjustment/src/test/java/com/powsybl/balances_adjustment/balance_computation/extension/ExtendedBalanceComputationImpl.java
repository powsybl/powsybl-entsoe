/**
 * Copyright (c) 2023, Coreso SA (https://www.coreso.eu/) and TSCNET Services GmbH (https://www.tscnet.eu/)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.balances_adjustment.balance_computation.extension;

import com.powsybl.balances_adjustment.balance_computation.BalanceComputationArea;
import com.powsybl.balances_adjustment.balance_computation.BalanceComputationImpl;
import com.powsybl.computation.ComputationManager;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * an example extending default BalanceComputationImpl
 * @author Marine Guibert {@literal <marine.guibert at artelys.com>}
 * @author Damien Jeandemange {@literal <damien.jeandemange at artelys.com>}
 */
class ExtendedBalanceComputationImpl extends BalanceComputationImpl {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExtendedBalanceComputationImpl.class);

    public ExtendedBalanceComputationImpl(List<BalanceComputationArea> areas, ComputationManager computationManager, LoadFlow.Runner loadFlowRunner) {
        super(areas, computationManager, loadFlowRunner);
    }

    @Override
    protected boolean isLoadFlowResultOk(BalanceComputationRunningContext context, LoadFlowResult loadFlowResult) {
        // example override requiring all components to be converged (just for testing - this is not a practical use case)
        return loadFlowResult.getComponentResults().stream()
                .map(LoadFlowResult.ComponentResult::getStatus)
                .allMatch(LoadFlowResult.ComponentResult.Status.CONVERGED::equals);
    }

    @Override
    protected double computeTotalMismatch(BalanceComputationRunningContext context) {
        context.getBalanceMismatches().forEach((area, mismatch) -> LOGGER.info("{} area mismatch is {} (thresholdNetPosition is {})", area.getName(), mismatch, context.getParameters().getThresholdNetPosition()));
        // example override using max mismatch
        return context.getBalanceMismatches().values().stream().mapToDouble(Double::doubleValue)
                .map(Math::abs).max()
                .orElse(0.0);
    }
}
