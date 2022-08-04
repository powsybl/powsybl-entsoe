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
import com.powsybl.iidm.network.VariantManagerConstants;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 */
interface XnecSelector {

    List<Xnec> run(Network network);

    static List<Branch> getBranches(Network network, Predicate<Branch> filter) {
        return NetworkUtil.getAllValidBranches(network)
            .stream()
            .filter(filter)
            .collect(Collectors.toList());
    }

    static List<Xnec> getXnecList(Network network, List<Branch> branchList) {
        List<Xnec> xnecList = getNStateXnecs(network, branchList);
        xnecList.addAll(mapContingenciesPerBranch(network, branchList));
        return xnecList;
    }

    static List<Xnec> getNStateXnecs(Network network, List<Branch> branchList) {
        String originVariantId = network.getVariantManager().getWorkingVariantId();
        return branchList.stream()
            .map(branch -> new Xnec(branch, originVariantId)).collect(Collectors.toList());
    }

    private static List<Xnec> mapContingenciesPerBranch(Network network, List<Branch> branchList) {
        Collection<String> variants = network.getVariantManager().getVariantIds();
        if (variants.size() == 1 && variants.contains(VariantManagerConstants.INITIAL_VARIANT_ID)) {
            return Collections.emptyList();
        }
        String originVariant = network.getVariantManager().getWorkingVariantId();
        List<Xnec> worstCaseXnecs = branchList.stream()
            .map(branch -> new Xnec(branch, getVariantWithMaximalBranchFlow(network, branch)))
            .filter(Xnec::isValid)
            .collect(Collectors.toList());
        network.getVariantManager().setWorkingVariant(originVariant);
        return worstCaseXnecs;
    }

    private static String getVariantWithMaximalBranchFlow(Network network, Branch branch) {
        return network.getVariantManager().getVariantIds().stream()
            .filter(XnecSelector::variantIsNotInitialVariant)
            .collect(Collectors.toMap(Function.identity(), getFlowOnBranchGivenVariant(network, branch)))
            .entrySet().stream().sorted(Comparator.comparing(Map.Entry::getValue))
            .map(Map.Entry::getKey).collect(Collectors.toList()).iterator().next();
    }

    private static boolean variantIsNotInitialVariant(String variantId) {
        return !Objects.equals(variantId, VariantManagerConstants.INITIAL_VARIANT_ID);
    }

    private static Function<String, Double> getFlowOnBranchGivenVariant(Network network, Branch branch) {
        return variantId -> {
            network.getVariantManager().setWorkingVariant(variantId);
            return -(Math.abs(branch.getTerminal1().getP()) + Math.abs(branch.getTerminal2().getP())) / 2;
        };
    }

    static boolean isAnInterconnection(Branch<?> branch) {
        Country country1 = NetworkUtil.getTerminalCountry(branch.getTerminal1());
        Country country2 = NetworkUtil.getTerminalCountry(branch.getTerminal2());
        return !country1.equals(country2);
    }
}
