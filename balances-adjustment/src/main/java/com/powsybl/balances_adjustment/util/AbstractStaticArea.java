/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.balances_adjustment.util;

import com.powsybl.iidm.network.*;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * @author Joris Mancini <joris.mancini_externe at rte-france.com>
 */
public abstract class AbstractStaticArea implements NetworkArea {
    protected Set<Bus> busesCache;
    protected List<Load> loadsCache;
    protected List<Generator> generatorsCache;

    @Override
    public double getNetPosition(boolean ignoreLoadFLowBalance) {
        return generatorsCache.stream().mapToDouble(Generator::getTargetP).sum() - loadsCache.stream().mapToDouble(Load::getP0).sum();
    }

    @Override
    public Collection<Bus> getContainedBusViewBuses() {
        return Collections.unmodifiableCollection(busesCache);
    }
}
