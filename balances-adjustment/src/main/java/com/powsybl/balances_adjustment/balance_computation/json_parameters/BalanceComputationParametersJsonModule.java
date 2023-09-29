/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.balances_adjustment.balance_computation.json_parameters;

import com.fasterxml.jackson.databind.module.SimpleModule;
import com.powsybl.balances_adjustment.balance_computation.BalanceComputationParameters;

/**
 * @author Mohamed Ben Rejeb {@literal <mohamed.benrejeb at rte-france.com>}
 */
public class BalanceComputationParametersJsonModule extends SimpleModule {

    public BalanceComputationParametersJsonModule() {
        addDeserializer(BalanceComputationParameters.class, new BalanceComputationParametersDeserializer());
        addSerializer(BalanceComputationParameters.class, new BalanceComputationParametersSerializer());
    }
}
