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
 * @deprecated Use module powsybl-balances-adjustment instead of this class
 *
 * @author Miora Ralambotiana {@literal <miora.ralambotiana at rte-france.com>}
 */
@Deprecated(since = "2.14", forRemoval = true)
public class CgmesBoundariesAreaFactory implements NetworkAreaFactory {

    private final List<Area> areas = new ArrayList<>();

    public CgmesBoundariesAreaFactory() {
    }

    public CgmesBoundariesAreaFactory(List<Area> areas) {
        this.areas.addAll(Objects.requireNonNull(areas));
    }

    @Override
    public NetworkArea create(Network network, boolean isStatic) {
        return new CgmesBoundariesArea(network, areas);
    }
}
