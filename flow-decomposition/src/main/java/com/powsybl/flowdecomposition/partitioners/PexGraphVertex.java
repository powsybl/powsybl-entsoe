/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.flowdecomposition.partitioners;

import com.powsybl.flowdecomposition.NetworkUtil;
import com.powsybl.iidm.network.BoundaryLine;
import com.powsybl.iidm.network.Bus;

import java.util.Objects;

/**
 * Vertex business object in PEX graph
 * Stands for network buses
 *
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
class PexGraphVertex {
    private final double associatedGeneration;
    private final double associatedLoad;
    private final String id;

    PexGraphVertex(Bus associatedBus, PexGraph.InjectionStrategy injectionStrategy) {
        Objects.requireNonNull(associatedBus);

        double totalGeneration = -NetworkUtil.getInjectionStream(associatedBus)
            .mapToDouble(injection -> injection.getTerminal().getP())
            .filter(d -> !Double.isNaN(d))
            .filter(d -> d < 0)
            .sum();

        double totalLoad = NetworkUtil.getInjectionStream(associatedBus)
            .mapToDouble(injection -> injection.getTerminal().getP())
            .filter(d -> !Double.isNaN(d))
            .filter(d -> d > 0)
            .sum();
        this.associatedGeneration = switch (injectionStrategy) {
            case PexGraph.InjectionStrategy.SUM_INJECTIONS ->
                totalGeneration > totalLoad ? totalGeneration - totalLoad : 0;
            case PexGraph.InjectionStrategy.DECOMPOSE_INJECTIONS -> totalGeneration;
        };
        this.associatedLoad = switch (injectionStrategy) {
            case PexGraph.InjectionStrategy.SUM_INJECTIONS ->
                totalLoad > totalGeneration ? totalLoad - totalGeneration : 0;
            case PexGraph.InjectionStrategy.DECOMPOSE_INJECTIONS -> totalLoad;
        };
        this.id = associatedBus.getId();
    }

    PexGraphVertex(BoundaryLine boundaryLine) {
        Objects.requireNonNull(boundaryLine);
        double power = boundaryLine.getTerminal().getP();
        this.associatedGeneration = Math.max(0, -power);
        this.associatedLoad = Math.max(0, power);
        this.id = boundaryLine.getId();
    }

    double getAssociatedLoad() {
        return associatedLoad;
    }

    double getAssociatedGeneration() {
        return associatedGeneration;
    }

    public String getId() {
        return id;
    }

    @Override
    public String toString() {
        return id;
    }
}
