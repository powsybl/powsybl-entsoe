/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.flow_decomposition;

import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Identifiable;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 */
class ReferenceFlowComputer {
    Map<String, Double> run(List<Xnec> xnecList) {
        return xnecList.stream()
            .map(Xnec::getBranch)
            .collect(Collectors.toMap(
                Identifiable::getId,
                this::getP
            ));
    }

    private double getP(Branch branch) {
        return branch.getTerminal1().getP();
    }
}
