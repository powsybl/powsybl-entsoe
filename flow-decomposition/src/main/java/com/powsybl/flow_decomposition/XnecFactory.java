/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.flow_decomposition;

import com.powsybl.contingency.Contingency;
import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.VariantManagerConstants;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 */
class XnecFactory {

    List<XnecWithDecomposition> run(Network network, List<Branch> branchList) {
        return run(network, branchList, Collections.emptyMap());
    }

    List<XnecWithDecomposition> run(Network network, List<Branch> branchList, Map<String, Contingency> variantContingenciesMap) {
        return getXnecList(network, branchList, variantContingenciesMap);
    }

    private static List<XnecWithDecomposition> getXnecList(Network network, List<Branch> branchList, Map<String, Contingency> variantContingenciesMap) {
        List<XnecWithDecomposition> xnecList = getNStateXnecs(network, branchList);
        xnecList.addAll(mapContingenciesPerBranch(network, branchList, variantContingenciesMap));
        return xnecList;
    }

    private static List<XnecWithDecomposition> getNStateXnecs(Network network, List<Branch> branchList) {
        String originVariantId = network.getVariantManager().getWorkingVariantId();
        return branchList.stream()
            .map(branch -> new XnecWithDecomposition(branch, originVariantId)).collect(Collectors.toList());
    }

    private static List<XnecWithDecomposition> mapContingenciesPerBranch(Network network,
                                                                         List<Branch> branchList,
                                                                         Map<String, Contingency> variantContingenciesMap) {
        if (variantContingenciesMap.isEmpty()) {
            return Collections.emptyList();
        }
        String originVariant = network.getVariantManager().getWorkingVariantId();
        List<XnecWithDecomposition> worstCaseXnecs = branchList.stream()
            .map(branch -> getXnecInWorstSenario(network, branch, variantContingenciesMap))
            .filter(Xnec::isBranchNotContainedInContingency)
            .collect(Collectors.toList());
        network.getVariantManager().setWorkingVariant(originVariant);
        return worstCaseXnecs;
    }

    private static XnecWithDecomposition getXnecInWorstSenario(Network network,
                                                               Branch branch,
                                                               Map<String, Contingency> variantContingenciesMap) {
        String variantId = getVariantWithMaximalBranchFlow(network, branch);
        return new XnecWithDecomposition(branch, variantId, variantContingenciesMap.get(variantId));
    }

    private static String getVariantWithMaximalBranchFlow(Network network, Branch branch) {
        return network.getVariantManager().getVariantIds().stream()
            .filter(XnecFactory::variantIsNotInitialVariant)
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
}
