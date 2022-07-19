/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.flow_decomposition;

import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 */
class XnecSelector {
    List<Branch> run(Network network) {
        return selectXnecs(network);
    }

    private List<Branch> selectXnecs(Network network) {
        return network.getBranchStream()
            .filter(this::isConnected)
            .filter(this::isInMainSynchronousComponent)
            .filter(this::isAnInterconnection)
            .collect(Collectors.toList());
    }

    private boolean isConnected(Branch<?> branch) {
        return branch.getTerminal1().isConnected() && branch.getTerminal2().isConnected();
    }

    private boolean isInMainSynchronousComponent(Branch<?> branch) {
        return NetworkUtil.isTerminalInMainSynchronousComponent(branch.getTerminal1())
            && NetworkUtil.isTerminalInMainSynchronousComponent(branch.getTerminal2());
    }

    private boolean isAnInterconnection(Branch<?> branch) {
        Country country1 = NetworkUtil.getTerminalCountry(branch.getTerminal1());
        Country country2 = NetworkUtil.getTerminalCountry(branch.getTerminal2());
        return !country1.equals(country2);
    }

}
