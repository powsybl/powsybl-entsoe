/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.flow_decomposition;

import com.powsybl.iidm.network.Branch;

/**
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 */
class Xnec {
    private static final String NO_CONTINGENCY_ID = "";
    private final Branch branch;
    private final String contingencyId;

    public Xnec(Branch branch) {
        this.branch = branch;
        this.contingencyId = NO_CONTINGENCY_ID;
    }

    public Xnec(Branch branch, String contingencyId) {
        this.branch = branch;
        this.contingencyId = contingencyId;
    }

    public Branch getBranch() {
        return branch;
    }

    public String getContingencyId() {
        return contingencyId;
    }

    public String getId() {
        return NetworkUtil.getIdWithContingency(branch.getId(), contingencyId);
    }
}
