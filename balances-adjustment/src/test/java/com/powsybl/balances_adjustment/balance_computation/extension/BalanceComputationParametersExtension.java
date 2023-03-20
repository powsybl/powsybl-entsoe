/**
 * Copyright (c) 2023, Coreso SA (https://www.coreso.eu/) and TSCNET Services GmbH (https://www.tscnet.eu/)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.balances_adjustment.balance_computation.extension;

import com.powsybl.balances_adjustment.balance_computation.BalanceComputationParameters;
import com.powsybl.commons.extensions.AbstractExtension;
import com.powsybl.loadflow.LoadFlowResult;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Marine Guibert {@literal <marine.guibert at artelys.com>}
 * @author Damien Jeandemange {@literal <damien.jeandemange at artelys.com>}
 */
public class BalanceComputationParametersExtension extends AbstractExtension<BalanceComputationParameters> {

    private final List<LoadFlowResult> loadFlowResultsPerIteration = new ArrayList<>();
    private final List<Double> totalMismatchesPerIteration = new ArrayList<>();

    @Override
    public String getName() {
        return "TestBalanceExtension";
    }

    public void addLoadFlowResults(LoadFlowResult loadFlowResult) {
        loadFlowResultsPerIteration.add(loadFlowResult);
    }

    public void addTotalMismatchResult(double totalMismatch) {
        totalMismatchesPerIteration.add(totalMismatch);
    }

    public List<LoadFlowResult> getLoadFlowResultsPerIteration() {
        return List.copyOf(loadFlowResultsPerIteration);
    }

    public List<Double> getTotalMismatchesPerIteration() {
        return List.copyOf(totalMismatchesPerIteration);
    }
}
