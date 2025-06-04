/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.balances_adjustment.util;

import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;

import java.util.Arrays;
import java.util.List;

/**
 * A {@link NetworkAreaFactory} instance that creates new {@link CountryArea}.
 *
 * @author Mathieu Bague {@literal <mathieu.bague at rte-france.com>}
 */
public class CountryAreaFactory implements NetworkAreaFactory {

    private final List<Country> countries;
    private final boolean isStatic;

    public CountryAreaFactory(Country... countries) {
        this(false, countries);
    }

    public CountryAreaFactory(boolean isStatic, Country... countries) {
        this.countries = Arrays.asList(countries);
        this.isStatic = isStatic;
    }

    @Override
    public NetworkArea create(Network network) {
        if (isStatic) {
            return new StaticCountryArea(network, countries);
        }
        return new CountryArea(network, countries);
    }
}
