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

import java.text.SimpleDateFormat;
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
public class FlowDecompositionResults {
    private static final double NO_FLOW = 0.;
    private final String id;
    private final String networkId;
    private SparseMatrixWithIndexesCSC allocatedAndLoopFlowsMatrix;
    private Map<String, Map<String, Double>> pstFlowMap;
    private Map<String, Double> acReferenceFlow;
    private Map<String, Double> dcReferenceFlow;
    private Map<String, DecomposedFlow> decomposedFlowsMapBeforeRescaling;
    private Map<String, DecomposedFlow> decomposedFlowMapAfterRescaling;
    private final Set<Country> zoneSet;
    private Map<String, Branch> xnecMap;

    FlowDecompositionResults(Network network) {
        this.networkId = network.getNameOrId();
        String date = new SimpleDateFormat("yyyyMMdd-HHmmss").format(Date.from(Instant.now()));
        this.id = "Flow_Decomposition_Results_of_" + date + "_on_network_" + networkId;
        this.zoneSet = network.getCountries();
    }

    /**
     * @return Network Id
     */
    public String getNetworkId() {
        return networkId;
    }

    /**
     * @return Id composed of a time format and the network id.
     */
    public String getId() {
        return id;
    }

    /**
     * @return the set of available zones in this result
     */
    public Set<Country> getZoneSet() {
        return zoneSet;
    }

    /**
     * @return A flow decomposition map. The keys are the XNEC ids and the values are {@code DecomposedFlow} objects.
     */
    public Map<String, DecomposedFlow> getDecomposedFlowMapBeforeRescaling() {
        if (!isDecomposedFlowMapCacheValid()) {
            initializeDecomposedFlowMapCache();
        }
        return decomposedFlowsMapBeforeRescaling;
    }

    /**
     * @return A rescaled flow decomposition map. The keys are the XNEC ids and the values are {@code DecomposedFlow} objects. This object is dense.
     */
    public Map<String, DecomposedFlow> getDecomposedFlowMap() {
        return decomposedFlowMapAfterRescaling;
    }

    private boolean isDecomposedFlowMapCacheValid() {
        return Objects.nonNull(decomposedFlowsMapBeforeRescaling);
    }

    private void initializeDecomposedFlowMapCache() {
        invalidateDecomposedFlowMapCache();
        decomposedFlowsMapBeforeRescaling = new TreeMap<>();
        allocatedAndLoopFlowsMatrix.toMap()
            .forEach((xnecId, decomposedFlow) -> decomposedFlowsMapBeforeRescaling.put(xnecId, createDecomposedFlow(xnecId, decomposedFlow)));
    }

    private void invalidateDecomposedFlowMapCache() {
        this.decomposedFlowsMapBeforeRescaling = null;
    }

    private DecomposedFlow createDecomposedFlow(String xnecId, Map<String, Double> allocatedAndLoopFlowMap) {
        Map<String, Double> loopFlowsMap = allocatedAndLoopFlowMap.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith(NetworkUtil.LOOP_FLOWS_COLUMN_PREFIX))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        double allocatedFlow = allocatedAndLoopFlowMap.get(DecomposedFlow.ALLOCATED_COLUMN_NAME);
        double pstFlow = pstFlowMap.getOrDefault(xnecId, Collections.emptyMap()).getOrDefault(DecomposedFlow.PST_COLUMN_NAME, NO_FLOW);
        Country country1 = NetworkUtil.getTerminalCountry(xnecMap.get(xnecId).getTerminal1());
        Country country2 = NetworkUtil.getTerminalCountry(xnecMap.get(xnecId).getTerminal2());
        double internalFlow = extractInternalFlow(loopFlowsMap, country1, country2);
        return new DecomposedFlow(loopFlowsMap, internalFlow, allocatedFlow, pstFlow,
            acReferenceFlow.get(xnecId), dcReferenceFlow.get(xnecId),
            country1, country2
            );
    }

    private double extractInternalFlow(Map<String, Double> loopFlowsMap, Country country1, Country country2) {
        if (Objects.equals(country1, country2)) {
            return Optional.ofNullable(loopFlowsMap.remove(NetworkUtil.getLoopFlowIdFromCountry(country1)))
                .orElse(NO_FLOW);
        }
        return NO_FLOW;
    }

    void saveAllocatedAndLoopFlowsMatrix(SparseMatrixWithIndexesCSC allocatedAndLoopFlowsMatrix) {
        this.allocatedAndLoopFlowsMatrix = allocatedAndLoopFlowsMatrix;
        invalidateDecomposedFlowMapCache();
    }

    void savePstFlowMatrix(SparseMatrixWithIndexesCSC pstFlowMatrix) {
        this.pstFlowMap = pstFlowMatrix.toMap();
        invalidateDecomposedFlowMapCache();
    }

    void saveAcReferenceFlow(Map<String, Double> acReferenceFlow) {
        this.acReferenceFlow = acReferenceFlow;
        invalidateDecomposedFlowMapCache();
    }

    void saveDcReferenceFlow(Map<String, Double> dcReferenceFlow) {
        this.dcReferenceFlow = dcReferenceFlow;
        invalidateDecomposedFlowMapCache();
    }

    void saveRescaledDecomposedFlowMap(Map<String, DecomposedFlow> decomposedFlowMap) {
        this.decomposedFlowMapAfterRescaling = decomposedFlowMap;
    }

    public void saveXnec(List<Branch> xnecList) {
        this.xnecMap = xnecList.stream().collect(Collectors.toMap(Identifiable::getId, Function.identity()));
    }
}
