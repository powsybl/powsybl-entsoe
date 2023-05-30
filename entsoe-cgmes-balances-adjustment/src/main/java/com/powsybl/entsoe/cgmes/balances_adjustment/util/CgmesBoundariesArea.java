/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.entsoe.cgmes.balances_adjustment.util;

import com.powsybl.balances_adjustment.util.NetworkArea;
import com.powsybl.cgmes.extensions.CgmesControlArea;
import com.powsybl.cgmes.extensions.CgmesDanglingLineBoundaryNode;
import com.powsybl.iidm.network.Bus;
import com.powsybl.iidm.network.DanglingLine;
import com.powsybl.iidm.network.Network;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Miora Ralambotiana <miora.ralambotiana at rte-france.com>
 */
class CgmesBoundariesArea implements NetworkArea {

    private final Set<DanglingLine> danglingLinesCache;

    CgmesBoundariesArea(Network network, List<CgmesControlArea> areas) {
        danglingLinesCache = network.getDanglingLineStream()
                .filter(dl -> dl.getExtension(CgmesDanglingLineBoundaryNode.class) == null || !dl.getExtension(CgmesDanglingLineBoundaryNode.class).isHvdc())
                .filter(dl -> dl.getTerminal().getBusView().getBus() != null && dl.getTerminal().getBusView().getBus().isInMainSynchronousComponent())
                .filter(dl -> areas.isEmpty() || areas.stream().anyMatch(area -> area.getTerminals().stream().anyMatch(t -> t.getConnectable().getId().equals(dl.getId()))
                        || area.getBoundaries().stream().anyMatch(b -> b.getDanglingLine().getId().equals(dl.getId()))))
                .collect(Collectors.toSet());
    }

    @Override
    public double getNetPosition() {
        return danglingLinesCache.parallelStream().mapToDouble(dl -> {
            double boundaryP = 0.0;
            if (dl.getTerminal().isConnected()) {
                boundaryP = !Double.isNaN(dl.getBoundary().getP()) ? dl.getBoundary().getP() : -dl.getTerminal().getP();
            }
            return boundaryP;
        }).sum();
    }

    @Override
    public Collection<Bus> getContainedBusViewBuses() {
        return Collections.emptyList();
    }
}
