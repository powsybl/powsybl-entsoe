/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.balances_adjustment.balance_computation;

import com.powsybl.computation.ComputationManager;
import com.powsybl.loadflow.LoadFlow;

import java.util.List;

/**
 * Balance computation factory to create <code>BalanceComputationImpl</code> class
 *
 * @author Ameni Walha {@literal <ameni.walha at rte-france.com>}
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 * @author Mathieu Bague {@literal <mathieu.bague at rte-france.com>}
 */
public class BalanceComputationFactoryImpl implements BalanceComputationFactory {

    private final boolean isStatic;

    public BalanceComputationFactoryImpl() {
        this(false);
    }

    public BalanceComputationFactoryImpl(boolean isStatic) {
        this.isStatic = isStatic;
    }

    @Override
    public BalanceComputation create(List<BalanceComputationArea> areas) {
        if (isStatic) {
            return new NoLoadflowBalanceComputation(areas);
        }
        throw new IllegalArgumentException("For non-static balance computation loadflowRunner and computationManager are required");
    }

    @Override
    public BalanceComputation create(List<BalanceComputationArea> areas, LoadFlow.Runner loadFlowRunner, ComputationManager computationManager) {
        if (isStatic) {
            return new NoLoadflowBalanceComputation(areas);
        }
        return new BalanceComputationImpl(areas, computationManager, loadFlowRunner);
    }
}
