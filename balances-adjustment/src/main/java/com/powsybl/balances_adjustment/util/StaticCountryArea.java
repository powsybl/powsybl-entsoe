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
public class StaticCountryArea implements NetworkArea {
    private final List<Country> countries = new ArrayList<>();
    private final Set<Bus> busesCache;
    private final List<Load> loadsCache;
    private final List<Generator> generatorsCache;

    public StaticCountryArea(Network network, List<Country> countries) {
        this.countries.addAll(countries);
        busesCache = network.getBusView().getBusStream()
            .filter(bus -> bus.getVoltageLevel().getSubstation().flatMap(Substation::getCountry).map(countries::contains).orElse(false))
            .collect(Collectors.toSet());
        loadsCache = network.getLoadStream()
            .filter(this::isInCountry)
            .toList();
        generatorsCache = network.getGeneratorStream()
            .filter(this::isInCountry)
            .toList();
    }

    @Override
    public double getNetPosition() {
        return generatorsCache.stream().mapToDouble(Generator::getTargetP).sum() - loadsCache.stream().mapToDouble(Load::getP0).sum();
    }

    @Override
    public Collection<Bus> getContainedBusViewBuses() {
        return Collections.unmodifiableCollection(busesCache);
    }

    private boolean isInCountry(Injection<?> injection) {
        return injection.getTerminal().getVoltageLevel().getSubstation().flatMap(Substation::getCountry).map(countries::contains).orElse(false);
    }
}
