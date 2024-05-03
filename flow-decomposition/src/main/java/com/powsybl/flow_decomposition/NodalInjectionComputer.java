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

import static com.powsybl.flow_decomposition.DecomposedFlow.ALLOCATED_COLUMN_NAME;
import static com.powsybl.flow_decomposition.DecomposedFlow.NO_FLOW;
import static com.powsybl.flow_decomposition.DecomposedFlow.XNODE_COLUMN_NAME;

/**
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
class NodalInjectionComputer {
    private static final double DEFAULT_GLSK_FACTOR = 0.0;
    public static final double DEFAULT_NET_POSITION = 0.0;
    private final NetworkMatrixIndexes networkMatrixIndexes;

    NodalInjectionComputer(NetworkMatrixIndexes networkMatrixIndexes) {
        this.networkMatrixIndexes = networkMatrixIndexes;
    }

    SparseMatrixWithIndexesTriplet run(Network network,
                                       Map<Country, Map<String, Double>> glsks,
                                       Map<Country, Double> netPositions) {
        ReferenceNodalInjectionComputer referenceNodalInjectionComputer = new ReferenceNodalInjectionComputer();
        Map<String, Double> nodalInjectionDcReference = referenceNodalInjectionComputer.run(networkMatrixIndexes.getNodeList());
        Map<String, Double> nodalInjectionForXNodeFlow = referenceNodalInjectionComputer.run(networkMatrixIndexes.getUnmergedXNodeList());
        Map<String, Double> nodalInjectionsForAllocatedFlow = getNodalInjectionsForAllocatedFlows(glsks, netPositions);

        SparseMatrixWithIndexesTriplet nodalInjectionMatrix = getEmptyNodalInjectionMatrix(glsks,
            nodalInjectionsForAllocatedFlow.size() + nodalInjectionDcReference.size() + nodalInjectionForXNodeFlow.size());
        fillNodalInjectionsWithAllocatedFlow(nodalInjectionsForAllocatedFlow, nodalInjectionMatrix);
        fillNodalInjectionsWithXNodeFlow(nodalInjectionForXNodeFlow, nodalInjectionMatrix);
        fillNodalInjectionsWithLoopFlow(network, nodalInjectionsForAllocatedFlow, nodalInjectionForXNodeFlow, nodalInjectionDcReference, nodalInjectionMatrix);
        return nodalInjectionMatrix;
    }

    private Map<String, Double> getNodalInjectionsForAllocatedFlows(Map<Country, Map<String, Double>> glsks,
                                                                    Map<Country, Double> netPositions) {
        return networkMatrixIndexes.getNodeList().stream()
            .collect(Collectors.toMap(
                    Injection::getId,
                    injection -> getIndividualNodalInjectionForAllocatedFlows(injection, glsks, netPositions)
                )
            );
    }

    private double getIndividualNodalInjectionForAllocatedFlows(Injection<?> injection,
                                                                Map<Country, Map<String, Double>> glsks,
                                                                Map<Country, Double> netPositions) {
        Country injectionCountry = NetworkUtil.getInjectionCountry(injection);
        return glsks.get(injectionCountry).getOrDefault(injection.getId(), DEFAULT_GLSK_FACTOR)
                * netPositions.getOrDefault(injectionCountry, DEFAULT_NET_POSITION);
    }

    private SparseMatrixWithIndexesTriplet getEmptyNodalInjectionMatrix(Map<Country, Map<String, Double>> glsks, Integer size) {
        List<String> columns = glsks.keySet().stream()
            .map(NetworkUtil::getLoopFlowIdFromCountry)
            .collect(Collectors.toList());
        columns.add(ALLOCATED_COLUMN_NAME);
        columns.add(XNODE_COLUMN_NAME);
        return new SparseMatrixWithIndexesTriplet(
            networkMatrixIndexes.getNodeIndex(), NetworkUtil.getIndex(columns), size);
    }

    private void fillNodalInjectionsWithAllocatedFlow(Map<String, Double> nodalInjectionsForAllocatedFlow,
                                                      SparseMatrixWithIndexesTriplet nodalInjectionMatrix) {
        nodalInjectionsForAllocatedFlow.forEach(
            (injectionId, injectionValue) -> nodalInjectionMatrix.addItem(injectionId,
                ALLOCATED_COLUMN_NAME, injectionValue)
        );
    }

    private void fillNodalInjectionsWithXNodeFlow(Map<String, Double> xNodeInjection,
                                                  SparseMatrixWithIndexesTriplet nodalInjectionMatrix) {
        xNodeInjection.forEach(
            (injectionId, injectionValue) -> nodalInjectionMatrix.addItem(injectionId,
                XNODE_COLUMN_NAME, injectionValue)
        );
    }

    private void fillNodalInjectionsWithLoopFlow(Network network,
                                                 Map<String, Double> nodalInjectionsForAllocatedFlow,
                                                 Map<String, Double> nodalInjectionsForXNodeFlow,
                                                 Map<String, Double> nodalInjectionDcReference,
                                                 SparseMatrixWithIndexesTriplet nodalInjectionMatrix) {
        networkMatrixIndexes.getNodeList().forEach(
            node -> {
                String nodeId = node.getId();
                String countryString = NetworkUtil.getLoopFlowIdFromCountry(network, nodeId);
                if (countryString == null) {
                    return;
                }
                nodalInjectionMatrix.addItem(
                    nodeId,
                    countryString,
                    computeNodalInjectionForLoopFLow(
                        nodalInjectionDcReference.get(nodeId),
                        nodalInjectionsForAllocatedFlow.get(nodeId),
                        nodalInjectionsForXNodeFlow.getOrDefault(nodeId, NO_FLOW)
                    )
                );
            });
    }

    private double computeNodalInjectionForLoopFLow(double referenceDcNodalInjection,
                                                    double nodalInjectionForAllocatedFlow,
                                                    double nodalInjectionForXNodeFlow) {
        return referenceDcNodalInjection - nodalInjectionForAllocatedFlow - nodalInjectionForXNodeFlow;
    }
}
