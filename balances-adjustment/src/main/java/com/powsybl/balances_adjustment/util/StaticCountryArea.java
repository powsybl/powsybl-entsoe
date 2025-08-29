/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.balances_adjustment.util;

import com.powsybl.iidm.network.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Joris Mancini <joris.mancini_externe at rte-france.com>
 */
public class StaticCountryArea extends AbstractStaticArea {

    public StaticCountryArea(Network network, List<Country> countries) {
        busesCache = network.getBusView().getBusStream()
            .filter(bus -> bus.getVoltageLevel().getSubstation().flatMap(Substation::getCountry).map(countries::contains).orElse(false))
            .collect(Collectors.toSet());
        loadsCache = network.getLoadStream()
            .filter(load -> NetworkAreaUtil.isInCountry(load, countries))
            .toList();
        generatorsCache = network.getGeneratorStream()
            .filter(generator -> NetworkAreaUtil.isInCountry(generator, countries))
            .toList();
    }
}
