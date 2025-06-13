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
