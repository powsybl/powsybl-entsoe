package com.powsybl.balances_adjustment.balance_computation.extension;

import com.powsybl.balances_adjustment.balance_computation.BalanceComputationParameters;
import com.powsybl.commons.extensions.AbstractExtension;
import com.powsybl.loadflow.LoadFlowResult;

import java.util.ArrayList;
import java.util.List;

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
