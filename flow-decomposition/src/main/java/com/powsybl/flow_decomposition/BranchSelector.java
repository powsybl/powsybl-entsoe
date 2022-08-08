/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.flow_decomposition;

import com.powsybl.commons.PowsyblException;
import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 */
interface BranchSelector {

    static BranchSelector factory(FlowDecompositionParameters parameters, Map<String, Map<Country, Double>> zonalPtdf) {
        switch (parameters.getBranchSelectionStrategy()) {
            case ONLY_INTERCONNECTIONS:
                return new BranchSelectorInterconnection();
            case ZONE_TO_ZONE_PTDF_CRITERIA:
                return new BranchSelector5PercPtdf(zonalPtdf);
            default:
                throw new PowsyblException(String.format("BranchSelectionStrategy %s is not valid",
                    parameters.getBranchSelectionStrategy()));
        }
    }

    List<Branch> run(Network network);

    static List<Branch> getBranches(Network network, Predicate<Branch> filter) {
        return NetworkUtil.getAllValidBranches(network)
            .stream()
            .filter(filter)
            .collect(Collectors.toList());
    }

    static boolean isAnInterconnection(Branch<?> branch) {
        Country country1 = NetworkUtil.getTerminalCountry(branch.getTerminal1());
        Country country2 = NetworkUtil.getTerminalCountry(branch.getTerminal2());
        return !country1.equals(country2);
    }
}
