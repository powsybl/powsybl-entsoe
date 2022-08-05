/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.flow_decomposition;

import com.powsybl.contingency.Contingency;
import com.powsybl.contingency.ContingencyElement;
import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Country;

import java.util.Objects;

/**
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 */
public class Xnec {
    private final Branch branch;
    private final String variantId;
    private final Contingency contingency;
    private final Country countryTerminal1;
    private final Country countryTerminal2;

    Xnec(Branch branch, String variantId, Contingency contingency) {
        this.branch = branch;
        this.variantId = variantId;
        this.contingency = contingency;
        this.countryTerminal1 = NetworkUtil.getTerminalCountry(branch.getTerminal1());
        this.countryTerminal2 = NetworkUtil.getTerminalCountry(branch.getTerminal2());
    }

    Xnec(Branch branch, String variantId) {
        this(branch, variantId, null);
    }

    public Branch getBranch() {
        return branch;
    }

    public String getVariantId() {
        return variantId;
    }

    public String getId() {
        return createId(branch.getId(), variantId);
    }

    public static String createId(String branchId, String variantId) {
        return branchId + "_" + variantId;
    }

    public Contingency getContingency() {
        return contingency;
    }

    public Country getCountryTerminal1() {
        return countryTerminal1;
    }

    public Country getCountryTerminal2() {
        return countryTerminal2;
    }

    public boolean isInternalBranch() {
        return Objects.equals(getCountryTerminal1(), getCountryTerminal2());
    }

    boolean isBranchNotContainedInContingency() {
        return contingency.getElements().stream().map(ContingencyElement::getId).noneMatch(s -> s.equals(branch.getId()));
    }
}
