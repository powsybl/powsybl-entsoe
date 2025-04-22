/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.entsoe.cgmes.balances_adjustment.util;

import com.powsybl.balances_adjustment.util.NetworkArea;
import com.powsybl.cgmes.extensions.CgmesDanglingLineBoundaryNode;
import com.powsybl.iidm.network.Area;
import com.powsybl.iidm.network.DanglingLine;
import com.powsybl.iidm.network.Network;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Miora Ralambotiana {@literal <miora.ralambotiana at rte-france.com>}
 */
@Deprecated
class CgmesBoundariesArea implements NetworkArea {

    private final Set<DanglingLine> danglingLinesCache;

    CgmesBoundariesArea(Network network, List<Area> areas) {
        danglingLinesCache = network.getDanglingLineStream()
                .filter(dl -> dl.getExtension(CgmesDanglingLineBoundaryNode.class) == null || !dl.getExtension(CgmesDanglingLineBoundaryNode.class).isHvdc())
                .filter(dl -> dl.getTerminal().getBusView().getBus() != null && dl.getTerminal().getBusView().getBus().isInMainSynchronousComponent())
                .filter(dl -> areas.isEmpty() || areas.stream().anyMatch(area -> CgmesAreaUtils.isIdInAreaBoundaryTerminals(dl.getId(), area) || CgmesAreaUtils.isIdInAreaBoundariesDanglingLines(dl.getId(), area)))
                .collect(Collectors.toSet());
    }

    @Override
    public double getNetPosition() {
        return danglingLinesCache.parallelStream().mapToDouble(dl -> dl.getTerminal().isConnected() ? -dl.getBoundary().getP() : 0).sum();
    }
}
