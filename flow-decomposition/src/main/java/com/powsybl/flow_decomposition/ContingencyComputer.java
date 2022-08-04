/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.flow_decomposition;

import com.powsybl.commons.PowsyblException;
import com.powsybl.contingency.Contingency;
import com.powsybl.iidm.network.Identifiable;
import com.powsybl.iidm.network.Network;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 */
class ContingencyComputer {

    private final FlowDecompositionParameters.ContingencyStrategy contingencyStrategy;

    public ContingencyComputer(FlowDecompositionParameters parameters) {
        contingencyStrategy = parameters.getContingencyStrategy();
    }

    List<Contingency> run(Network network) {
        switch (contingencyStrategy) {
            case ONLY_N_STATE:
                return Collections.emptyList();
            case AUTO_CONTINGENCY:
                return getAutoContingencyList(network);
            default:
                throw new PowsyblException(String.format("ContingencyStrategy %s is not valid",
                    contingencyStrategy));
        }
    }

    private static List<Contingency> getAutoContingencyList(Network network) {
        return network.getBranchStream()
            .sorted(Comparator.comparing(branch -> Math.abs(branch.getTerminal1().getP())))
            .limit(10)
            .map(Identifiable::getId)
            .map(branch -> Contingency.builder(branch).addBranch(branch).build())
            .collect(Collectors.toList());
    }
}
