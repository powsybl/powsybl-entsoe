/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.flow_decomposition;

import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Injection;
import com.powsybl.iidm.network.Network;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
class NodalInjectionComputer {
    private static final double DEFAULT_GLSK_FACTOR = 0.0;
    private final NetworkMatrixIndexes networkMatrixIndexes;

    NodalInjectionComputer(NetworkMatrixIndexes networkMatrixIndexes) {
        this.networkMatrixIndexes = networkMatrixIndexes;
    }

    SparseMatrixWithIndexesTriplet run(
        Network network,
        Map<Country, Map<String, Double>> glsks,
        Map<Country, Double> netPositions,
        Map<String, Double> dcNodalInjection) {
        Map<String, Double> nodalInjectionsForAllocatedFlow = getNodalInjectionsForAllocatedFlows(glsks, netPositions);
        return convertToNodalInjectionMatrix(network, glsks, nodalInjectionsForAllocatedFlow, dcNodalInjection);
    }

    private Map<String, Double> getNodalInjectionsForAllocatedFlows(
        Map<Country, Map<String, Double>> glsks,
        Map<Country, Double> netPositions) {
        return networkMatrixIndexes.getNodeList().stream()
            .collect(Collectors.toMap(
                    Injection::getId,
                    injection -> getIndividualNodalInjectionForAllocatedFlows(injection, glsks, netPositions)
                )
            );
    }

    private double getIndividualNodalInjectionForAllocatedFlows(
        Injection<?> injection,
        Map<Country, Map<String, Double>> glsks,
        Map<Country, Double> netPositions) {
        Country injectionCountry = NetworkUtil.getInjectionCountry(injection);
        return glsks.get(injectionCountry).getOrDefault(injection.getId(), DEFAULT_GLSK_FACTOR)
            * netPositions.get(injectionCountry);
    }

    private SparseMatrixWithIndexesTriplet getEmptyNodalInjectionMatrix(Map<Country, Map<String, Double>> glsks, Integer size) {
        List<String> columns = glsks.keySet().stream()
            .map(NetworkUtil::getLoopFlowIdFromCountry)
            .collect(Collectors.toList());
        columns.add(DecomposedFlow.ALLOCATED_COLUMN_NAME);
        return new SparseMatrixWithIndexesTriplet(
            networkMatrixIndexes.getNodeIndex(), NetworkUtil.getIndex(columns), size);
    }

    private SparseMatrixWithIndexesTriplet convertToNodalInjectionMatrix(
        Network network,
        Map<Country, Map<String, Double>> glsks,
        Map<String, Double> nodalInjectionsForAllocatedFlow,
        Map<String, Double> dcNodalInjection) {
        SparseMatrixWithIndexesTriplet nodalInjectionMatrix = getEmptyNodalInjectionMatrix(glsks,
            nodalInjectionsForAllocatedFlow.size() + dcNodalInjection.size());
        fillNodalInjectionsWithAllocatedFlow(nodalInjectionsForAllocatedFlow, nodalInjectionMatrix);
        fillNodalInjectionsWithLoopFlow(network, nodalInjectionsForAllocatedFlow, dcNodalInjection, nodalInjectionMatrix);
        return nodalInjectionMatrix;
    }

    private void fillNodalInjectionsWithAllocatedFlow(Map<String, Double> nodalInjectionsForAllocatedFlow,
                                                      SparseMatrixWithIndexesTriplet nodalInjectionMatrix) {
        nodalInjectionsForAllocatedFlow.forEach(
            (injectionId, injectionValue) -> nodalInjectionMatrix.addItem(injectionId,
                DecomposedFlow.ALLOCATED_COLUMN_NAME, injectionValue)
        );
    }

    private void fillNodalInjectionsWithLoopFlow(Network network,
                                                 Map<String, Double> nodalInjectionsForAllocatedFlow,
                                                 Map<String, Double> dcNodalInjection,
                                                 SparseMatrixWithIndexesTriplet nodalInjectionMatrix) {
        dcNodalInjection.forEach(
            (dcInjectionId, dcInjectionValue) -> nodalInjectionMatrix.addItem(
                dcInjectionId,
                NetworkUtil.getLoopFlowIdFromCountry(network, dcInjectionId),
                computeNodalInjectionForLoopFLow(nodalInjectionsForAllocatedFlow.get(dcInjectionId), dcInjectionValue)
            ));
    }

    private double computeNodalInjectionForLoopFLow(double nodalInjectionForAllocatedFlow, double dcInjectionValue) {
        return dcInjectionValue - nodalInjectionForAllocatedFlow;
    }
}
