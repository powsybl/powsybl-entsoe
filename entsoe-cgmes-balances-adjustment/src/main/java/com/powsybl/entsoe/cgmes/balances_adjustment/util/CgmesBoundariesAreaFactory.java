/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.entsoe.cgmes.balances_adjustment.util;

import com.powsybl.balances_adjustment.util.NetworkArea;
import com.powsybl.balances_adjustment.util.NetworkAreaFactory;
import com.powsybl.iidm.network.Area;
import com.powsybl.iidm.network.Network;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author Miora Ralambotiana {@literal <miora.ralambotiana at rte-france.com>}
 */
@Deprecated
public class CgmesBoundariesAreaFactory implements NetworkAreaFactory {

    private final List<Area> areas = new ArrayList<>();

    public CgmesBoundariesAreaFactory() {
    }

    public CgmesBoundariesAreaFactory(List<Area> areas) {
        this.areas.addAll(Objects.requireNonNull(areas));
    }

    @Override
    public NetworkArea create(Network network) {
        return new CgmesBoundariesArea(network, areas);
    }
}
