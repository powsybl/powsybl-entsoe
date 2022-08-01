/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.flow_decomposition;

import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Identifiable;
import com.powsybl.iidm.network.Network;
import org.apache.commons.math3.util.Pair;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 */
class XnecSelector {
    public static final double PTDF_THRESHOLD = 0.05;

    List<Branch> run(Network network, Map<String, Map<Country, Double>> zonalPtdf, FlowDecompositionResults flowDecompositionResults) {
        List<Branch> xnecList = selectXnecs(network, zonalPtdf);
        flowDecompositionResults.saveXnecToCountry(getXnecToCountry(xnecList));
        return xnecList;
    }

    private List<Branch> selectXnecs(Network network, Map<String, Map<Country, Double>> zonalPtdf) {
        return NetworkUtil.getAllValidBranches(network)
            .stream()
            .filter(branch -> isAXnec(branch, zonalPtdf.getOrDefault(branch.getId(), Collections.emptyMap()).values()))
            .collect(Collectors.toList());
    }

    private boolean isAXnec(Branch branch, Collection<Double> countryPtdfList) {
        return isAnInterconnection(branch) || hasMoreThan5PercentPtdf(countryPtdfList);
    }

    private static boolean hasMoreThan5PercentPtdf(Collection<Double> countryPtdfList) {
        return (!countryPtdfList.isEmpty()) && (Collections.max(countryPtdfList) - Collections.min(countryPtdfList)) > PTDF_THRESHOLD;
    }

    static boolean isAnInterconnection(Branch<?> branch) {
        Country country1 = NetworkUtil.getTerminalCountry(branch.getTerminal1());
        Country country2 = NetworkUtil.getTerminalCountry(branch.getTerminal2());
        return !country1.equals(country2);
    }

    private Map<String, Pair<Country, Country>> getXnecToCountry(List<Branch> xnecList) {
        return xnecList.stream().collect(Collectors.toMap(Identifiable::getId, this::getCountryPair));
    }

    private Pair<Country, Country> getCountryPair(Branch branch) {
        return new Pair<>(NetworkUtil.getTerminalCountry(branch.getTerminal1()),
            NetworkUtil.getTerminalCountry(branch.getTerminal2()));
    }
}
