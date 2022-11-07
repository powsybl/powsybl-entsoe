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

import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * This class provides flow decomposition results from a network.
 * Those results are returned by a flowDecompositionComputer when run on a network.
 * By default, the results only contain the flow decomposition of the XNECs.
 * If this runner has its argument {@code saveIntermediates} set to {@code true},
 * then the results will contain supplementary information.
 *
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 * @see FlowDecompositionComputer
 * @see DecomposedFlow
 */
class FlowDecompositionResultsBuilder {
    private static final double NO_FLOW = 0.;
    private final Map<String, Branch> xnecMap;
    private final Network network;
    private final Date date;
    private SparseMatrixWithIndexesCSC allocatedAndLoopFlowsMatrix;
    private Map<String, Map<String, Double>> pstFlowMap;
    private Map<String, Double> acReferenceFlow;
    private Map<String, Double> dcReferenceFlow;

    FlowDecompositionResultsBuilder(List<Branch> xnecList, Network network) {
        this.xnecMap = xnecList.stream().collect(Collectors.toMap(Identifiable::getId, Function.identity()));
        this.network = network;
        this.date = Date.from(Instant.now());
    }

    void saveAllocatedAndLoopFlowsMatrix(SparseMatrixWithIndexesCSC allocatedAndLoopFlowsMatrix) {
        this.allocatedAndLoopFlowsMatrix = allocatedAndLoopFlowsMatrix;
    }

    void savePstFlowMatrix(SparseMatrixWithIndexesCSC pstFlowMatrix) {
        this.pstFlowMap = pstFlowMatrix.toMap();
    }

    void saveAcReferenceFlow(Map<String, Double> acReferenceFlow) {
        this.acReferenceFlow = acReferenceFlow;
    }

    void saveDcReferenceFlow(Map<String, Double> dcReferenceFlow) {
        this.dcReferenceFlow = dcReferenceFlow;
    }

    FlowDecompositionResults build(boolean isRescaleEnable) {
        Map<String, DecomposedFlow> decomposedFlowMap = new HashMap<>();
        allocatedAndLoopFlowsMatrix.toMap()
            .forEach((xnecId, decomposedFlow) -> decomposedFlowMap.put(xnecId, createDecomposedFlow(xnecId, decomposedFlow, isRescaleEnable)));
        return new FlowDecompositionResults(network, date, decomposedFlowMap);
    }

    private DecomposedFlow createDecomposedFlow(String xnecId, Map<String, Double> allocatedAndLoopFlowMap, boolean isRescaleEnable) {
        Map<String, Double> loopFlowsMap = allocatedAndLoopFlowMap.entrySet().stream()
            .filter(entry -> entry.getKey().startsWith(NetworkUtil.LOOP_FLOWS_COLUMN_PREFIX))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        double allocatedFlow = allocatedAndLoopFlowMap.get(DecomposedFlow.ALLOCATED_COLUMN_NAME);
        double pstFlow = pstFlowMap.getOrDefault(xnecId, Collections.emptyMap()).getOrDefault(DecomposedFlow.PST_COLUMN_NAME, NO_FLOW);
        Country country1 = NetworkUtil.getTerminalCountry(xnecMap.get(xnecId).getTerminal1());
        Country country2 = NetworkUtil.getTerminalCountry(xnecMap.get(xnecId).getTerminal2());
        double internalFlow = extractInternalFlow(loopFlowsMap, country1, country2);
        DecomposedFlow decomposedFlow = new DecomposedFlow(loopFlowsMap, internalFlow, allocatedFlow, pstFlow,
            acReferenceFlow.get(xnecId), dcReferenceFlow.get(xnecId),
            country1, country2
        );
        if (isRescaleEnable) {
            return DecomposedFlowsRescaler.rescale(decomposedFlow);
        }
        return decomposedFlow;
    }

    private double extractInternalFlow(Map<String, Double> loopFlowsMap, Country country1, Country country2) {
        if (Objects.equals(country1, country2)) {
            return Optional.ofNullable(loopFlowsMap.remove(NetworkUtil.getLoopFlowIdFromCountry(country1)))
                .orElse(NO_FLOW);
        }
        return NO_FLOW;
    }
}
