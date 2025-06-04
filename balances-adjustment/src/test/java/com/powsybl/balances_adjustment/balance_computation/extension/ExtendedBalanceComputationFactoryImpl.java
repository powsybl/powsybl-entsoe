/**
 * Copyright (c) 2023, Coreso SA (https://www.coreso.eu/) and TSCNET Services GmbH (https://www.tscnet.eu/)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.balances_adjustment.balance_computation.extension;

import com.powsybl.balances_adjustment.balance_computation.BalanceComputation;
import com.powsybl.balances_adjustment.balance_computation.BalanceComputationArea;
import com.powsybl.balances_adjustment.balance_computation.BalanceComputationFactory;
import com.powsybl.computation.ComputationManager;
import com.powsybl.loadflow.LoadFlow;

import java.util.List;

/**
 * @author Marine Guibert {@literal <marine.guibert at artelys.com>}
 * @author Damien Jeandemange {@literal <damien.jeandemange at artelys.com>}
 */
public class ExtendedBalanceComputationFactoryImpl implements BalanceComputationFactory {

    @Override
    public BalanceComputation create(List<BalanceComputationArea> areas) {
        return null;
    }

    @Override
    public BalanceComputation create(List<BalanceComputationArea> areas, LoadFlow.Runner loadFlowRunner, ComputationManager computationManager) {
        return new ExtendedBalanceComputationImpl(areas, computationManager, loadFlowRunner);
    }
}
