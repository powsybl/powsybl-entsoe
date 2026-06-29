/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.flowdecomposition.partitioners;

import com.powsybl.iidm.network.BoundaryLine;
import com.powsybl.iidm.network.Branch;

import java.util.Objects;

/**
 * Edge business object in PEX graph
 * Stands for network branches
 *
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
class PexGraphEdge {
    private final double associatedFlow;
    private final String id;

    PexGraphEdge(Branch<?> branch) {
        Objects.requireNonNull(branch);
        double branchSide1Flow = branch.getTerminal1().getP();
        this.associatedFlow = (Double.isNaN(branchSide1Flow)) ? 0. : Math.abs(branchSide1Flow);
        this.id = branch.getId();
    }

    PexGraphEdge(BoundaryLine boundaryLine) {
        Objects.requireNonNull(boundaryLine);
        double boundaryLineFlow = boundaryLine.getTerminal().getP();
        this.associatedFlow = (Double.isNaN(boundaryLineFlow)) ? 0. : Math.abs(boundaryLineFlow);
        this.id = boundaryLine.getId();
    }

    double getAssociatedFlow() {
        return associatedFlow;
    }

    String getId() {
        return id;
    }
}
