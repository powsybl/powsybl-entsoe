/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.balances_adjustment.balance_computation;

import java.beans.ConstructorProperties;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Ameni Walha {@literal <ameni.walha at rte-france.com>}
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
public class BalanceComputationResult {

    /**
     * Status of balance computation
     */
    public enum Status {
        FAILED,
        SUCCESS
    }

    private final Status status;

    /**
     * Number of iteration for this balance computation result (optional)
     */
    private final int iterationCount;

    /**
     * Values of scaling power applied for each area to reach the target net position
     */
    private final Map<BalanceComputationArea, Double> balancedScalingMap;

    @ConstructorProperties("status")
    public BalanceComputationResult(Status status) {
        this(status, 0);
    }

    public BalanceComputationResult(Status status, int iterationCount) {
        this(status, iterationCount, new HashMap<>());
    }

    public BalanceComputationResult(Status status, int iterationCount, Map<BalanceComputationArea, Double> scalingMap) {
        this.status = status;
        this.iterationCount = iterationCount;
        this.balancedScalingMap = scalingMap;
    }

    public Status getStatus() {
        return status;
    }

    public int getIterationCount() {
        return iterationCount;
    }

    public Map<BalanceComputationArea, Double> getBalancedScalingMap() {
        return balancedScalingMap;
    }
}
