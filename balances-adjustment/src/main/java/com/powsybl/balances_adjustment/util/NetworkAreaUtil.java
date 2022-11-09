/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.balances_adjustment.util;

import com.powsybl.commons.PowsyblException;
import com.powsybl.iidm.modification.scalable.Scalable;
import com.powsybl.iidm.network.Bus;
import com.powsybl.iidm.network.Load;
import com.powsybl.iidm.network.extensions.LoadDetail;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Miora Ralambotiana <miora.ralambotiana at rte-france.com>
 */
public final class NetworkAreaUtil {

    /**
     * Create a ProportionalScalable containing all the conform loads contained in a given network area with an associated percentage proportional to their p0.
     * If no conform load is contained in the given network area, the ProportionalScalable contains all the loads contained in the given network area.
     * If no load is contained in the given network area, an exception is thrown.
     * If all selected load (conform or not) have a null p0, an exception is thrown.
     */
    public static Scalable createConformLoadScalable(NetworkArea area) {
        List<Load> loads = area.getContainedBusViewBuses().stream()
                .flatMap(Bus::getConnectedTerminalStream)
                .filter(t -> t.getConnectable() instanceof Load)
                .map(t -> (Load) t.getConnectable())
                .filter(load -> load.getP0() >= 0)
                .filter(load -> load.getExtension(LoadDetail.class) != null && load.getExtension(LoadDetail.class).getVariableActivePower() != 0)
                .collect(Collectors.toList());
        if (loads.isEmpty()) {
            loads = area.getContainedBusViewBuses().stream()
                    .flatMap(Bus::getConnectedTerminalStream)
                    .filter(t -> t.getConnectable() instanceof Load)
                    .map(t -> (Load) t.getConnectable())
                    .filter(load -> load.getP0() >= 0)
                    .collect(Collectors.toList());
            if (loads.isEmpty()) {
                throw new PowsyblException("There is no load in this area");
            }
        }
        float totalP0 = (float) loads.stream().mapToDouble(Load::getP0).sum();
        if (totalP0 == 0.0) {
            throw new PowsyblException("All loads' active power flows is null"); // this case should never happen
        }
        List<Float> percentages = loads.stream().map(load -> (float) (100f * load.getP0() / totalP0)).collect(Collectors.toList());
        return Scalable.proportional(percentages, loads.stream().map(inj -> (Scalable) Scalable.onLoad(inj.getId())).collect(Collectors.toList()));
    }

    private NetworkAreaUtil() {
    }
}
