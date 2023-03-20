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

import java.util.List;

/**
 * an example extending default BalanceComputationImpl
 * @author Marine Guibert {@literal <marine.guibert at artelys.com>}
 * @author Damien Jeandemange {@literal <damien.jeandemange at artelys.com>}
 */
class ExtendedBalanceComputationImpl extends BalanceComputationImpl {

    public ExtendedBalanceComputationImpl(List<BalanceComputationArea> areas, ComputationManager computationManager, LoadFlow.Runner loadFlowRunner) {
        super(areas, computationManager, loadFlowRunner);
    }

    @Override
    protected boolean isLoadFlowResultOk(BalanceComputationRunningContext context, LoadFlowResult loadFlowResult) {
        // example storing results in extension
        context.getParameters().getExtension(BalanceComputationParametersExtension.class).addLoadFlowResults(loadFlowResult);
        // example override requiring all components to be converged (just for testing - this is not a practical use case)
        return loadFlowResult.getComponentResults().stream()
                .map(LoadFlowResult.ComponentResult::getStatus)
                .allMatch(LoadFlowResult.ComponentResult.Status.CONVERGED::equals);
    }

    @Override
    protected double computeTotalMismatch(BalanceComputationRunningContext context) {
        // example override using max mismatch
        final double totalMismatch = context.getBalanceMismatches().values().stream().mapToDouble(Double::doubleValue)
                .map(Math::abs).max()
                .orElse(0.0);
        // example storing results in extension
        context.getParameters().getExtension(BalanceComputationParametersExtension.class).addTotalMismatchResult(totalMismatch);
        return totalMismatch;
    }
}
