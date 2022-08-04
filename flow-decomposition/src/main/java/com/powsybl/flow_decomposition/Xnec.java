/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.flow_decomposition;

import com.powsybl.iidm.network.Branch;

import java.util.Objects;

/**
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 */
public class Xnec {
    private final Branch branch;
    private final String variantId;

    Xnec(Branch branch, String variantId) {
        this.branch = branch;
        this.variantId = variantId;
    }

    Branch getBranch() {
        return branch;
    }

    String getVariantId() {
        return variantId;
    }

    String getId() {
        return createId(branch.getId(), variantId);
    }

    public static String createId(String branchId, String variantId) {
        return branchId + "_" + variantId;
    }

    public boolean isValid() {
        return !Objects.equals(branch.getId(), variantId);
    }
}
