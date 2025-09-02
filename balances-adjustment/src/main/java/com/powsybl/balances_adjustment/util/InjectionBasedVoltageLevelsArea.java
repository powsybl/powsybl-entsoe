/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.balances_adjustment.util;

import com.powsybl.iidm.network.Injection;
import com.powsybl.iidm.network.Network;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Joris Mancini <joris.mancini_externe at rte-france.com>
 */
public class InjectionBasedVoltageLevelsArea extends AbstractInjectionBasedArea {

    public InjectionBasedVoltageLevelsArea(Network network, List<String> voltageLevelIds) {
        loadsCache = network.getLoadStream()
            .filter(l -> isInVoltageLevels(voltageLevelIds, l))
            .toList();
        generatorsCache = network.getGeneratorStream()
            .filter(g -> isInVoltageLevels(voltageLevelIds, g))
            .toList();
        busesCache = network.getBusView().getBusStream()
            .filter(bus -> voltageLevelIds.contains(bus.getVoltageLevel().getId()))
            .collect(Collectors.toSet());
    }

    private boolean isInVoltageLevels(List<String> voltageLevelIds, Injection<?> injection) {
        return voltageLevelIds.contains(injection.getTerminal().getVoltageLevel().getId());
    }
}
