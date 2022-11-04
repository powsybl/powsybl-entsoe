/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.balances_adjustment.util;

import com.powsybl.iidm.network.Network;

import java.util.Arrays;
import java.util.List;

/**
 * A {@link NetworkAreaFactory} instance that creates new {@link VoltageLevelsArea}.
 *
 * @author Mathieu Bague {@literal <mathieu.bague at rte-france.com>}
 */
public class VoltageLevelsAreaFactory implements NetworkAreaFactory {

    private final List<String> voltageLevelIds;

    public VoltageLevelsAreaFactory(String... voltageLevelIds) {
        this.voltageLevelIds = Arrays.asList(voltageLevelIds);
    }

    @Override
    public VoltageLevelsArea create(Network network) {
        return new VoltageLevelsArea(network, voltageLevelIds);
    }
}
