/**
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.entsoe.cgmes.balances_adjustment.util;

import com.powsybl.iidm.network.*;

import java.util.Optional;

/**
 * @author Hugo Schindler{@literal <hugo.schindler at rte-france.com>}
 */
public final class CgmesAreaUtils {
    private CgmesAreaUtils() {
        // Utility class
    }

    static boolean hasAreaBoundaries(Area area) {
        return area.getAreaBoundaryStream().map(AreaBoundary::getBoundary).anyMatch(Optional::isPresent);
    }

    static boolean hasAreaBoundaryTerminals(Area area) {
        return area.getAreaBoundaryStream().map(AreaBoundary::getTerminal).anyMatch(Optional::isPresent);
    }

    static boolean isIdInAreaBoundaryTerminals(String elementId, Area area) {
        return area.getAreaBoundaryStream()
                .map(AreaBoundary::getTerminal)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(Terminal::getConnectable)
                .map(Identifiable::getId)
                .anyMatch(terminalId -> terminalId.equals(elementId));
    }

    static boolean isIdInAreaBoundariesDanglingLines(String elementId, Area area) {
        return area.getAreaBoundaryStream()
                .map(AreaBoundary::getBoundary)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(Boundary::getDanglingLine)
                .map(Identifiable::getId)
                .anyMatch(danglingLineId -> danglingLineId.equals(elementId));
    }
}
