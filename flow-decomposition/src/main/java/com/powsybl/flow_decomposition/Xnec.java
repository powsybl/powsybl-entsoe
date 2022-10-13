/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.flow_decomposition;

import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Country;

import java.util.Objects;

/**
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 */
public class Xnec {
    private final Branch branch;
    private final Country country1;
    private final Country country2;

    public Xnec(Branch branch) {
        this.branch = branch;
        this.country1 = NetworkUtil.getTerminalCountry(branch.getTerminal1());
        this.country2 = NetworkUtil.getTerminalCountry(branch.getTerminal2());
    }

    public Branch getBranch() {
        return branch;
    }

    public Country getCountry1() {
        return country1;
    }

    public Country getCountry2() {
        return country2;
    }

    public String getId() {
        return branch.getId();
    }

    public boolean isInternalBranch() {
        return Objects.equals(country1, country2);
    }
}
