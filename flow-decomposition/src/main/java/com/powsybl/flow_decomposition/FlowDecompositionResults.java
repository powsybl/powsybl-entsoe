/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.flow_decomposition;

import com.powsybl.flow_decomposition.rescaler.DecomposedFlowRescaler;
import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Identifiable;
import com.powsybl.iidm.network.Network;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.powsybl.flow_decomposition.DecomposedFlow.ALLOCATED_COLUMN_NAME;
import static com.powsybl.flow_decomposition.DecomposedFlow.NO_FLOW;
import static com.powsybl.flow_decomposition.DecomposedFlow.PST_COLUMN_NAME;
import static com.powsybl.flow_decomposition.DecomposedFlow.XNODE_COLUMN_NAME;
import static com.powsybl.flow_decomposition.NetworkUtil.LOOP_FLOWS_COLUMN_PREFIX;

/**
 * This class provides flow decomposition results from a network.
 * Those results are returned by a flowDecompositionComputer when run on a network.
 *
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 * @see FlowDecompositionComputer
 * @see DecomposedFlow
 */
public class FlowDecompositionResults {
    private static final String NO_CONTINGENCY_ID = "";
    private final String networkId;
    private final String id;
    private final Set<Country> zoneSet;
    private final Map<String, DecomposedFlow> decomposedFlowMap = new HashMap<>();

    class PerStateBuilder {
        private final Map<String, Branch> xnecMap;
        private final String contingencyId;
        private SparseMatrixWithIndexesCSC allocatedAndLoopFlowsMatrix;
        private Map<String, Map<String, Double>> pstFlowMap;
        private Map<String, Double> acTerminal1ReferenceFlow;
        private Map<String, Double> acTerminal2ReferenceFlow;
        private Map<String, Double> dcReferenceFlow;

        PerStateBuilder(String contingencyId, Set<Branch> xnecList) {
            this.xnecMap = xnecList.stream().collect(Collectors.toMap(Identifiable::getId, Function.identity()));
            this.contingencyId = contingencyId;
        }

        void saveAllocatedAndLoopFlowsMatrix(SparseMatrixWithIndexesCSC allocatedAndLoopFlowsMatrix) {
            this.allocatedAndLoopFlowsMatrix = allocatedAndLoopFlowsMatrix;
        }

        void savePstFlowMatrix(SparseMatrixWithIndexesCSC pstFlowMatrix) {
            this.pstFlowMap = pstFlowMatrix.toMap();
        }

        void saveAcTerminal1ReferenceFlow(Map<String, Double> acTerminal1ReferenceFlow) {
            this.acTerminal1ReferenceFlow = acTerminal1ReferenceFlow;
        }

        void saveAcTerminal2ReferenceFlow(Map<String, Double> acTerminal2ReferenceFlow) {
            this.acTerminal2ReferenceFlow = acTerminal2ReferenceFlow;
        }

        void saveDcReferenceFlow(Map<String, Double> dcReferenceFlow) {
            this.dcReferenceFlow = dcReferenceFlow;
        }

        void build(DecomposedFlowRescaler decomposedFlowRescaler) {
            allocatedAndLoopFlowsMatrix.toMap()
                .forEach((branchId, decomposedFlow) -> {
                    String xnecId = DecomposedFlow.getXnecId(contingencyId, branchId);
                    decomposedFlowMap.put(xnecId, createDecomposedFlow(branchId, decomposedFlow, decomposedFlowRescaler));
                });
        }

        private DecomposedFlow createDecomposedFlow(String branchId, Map<String, Double> allocatedAndLoopFlowMap, DecomposedFlowRescaler decomposedFlowRescaler) {
            Map<String, Double> loopFlowsMap = allocatedAndLoopFlowMap.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith(LOOP_FLOWS_COLUMN_PREFIX))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
            double allocatedFlow = allocatedAndLoopFlowMap.get(ALLOCATED_COLUMN_NAME);
            double pstFlow = pstFlowMap.getOrDefault(branchId, Collections.emptyMap()).getOrDefault(PST_COLUMN_NAME, NO_FLOW);
            double xNodeFlow = allocatedAndLoopFlowMap.getOrDefault(XNODE_COLUMN_NAME, NO_FLOW);
            Country country1 = NetworkUtil.getTerminalCountry(xnecMap.get(branchId).getTerminal1());
            Country country2 = NetworkUtil.getTerminalCountry(xnecMap.get(branchId).getTerminal2());
            double internalFlow = extractInternalFlow(loopFlowsMap, country1, country2);
            DecomposedFlow decomposedFlow = new DecomposedFlowBuilder()
                    .withBranchId(branchId)
                    .withContingencyId(contingencyId)
                    .withCountry1(country1)
                    .withCountry2(country2)
                    .withAcTerminal1ReferenceFlow(acTerminal1ReferenceFlow.get(branchId))
                    .withAcTerminal2ReferenceFlow(acTerminal2ReferenceFlow.get(branchId))
                    .withDcReferenceFlow(dcReferenceFlow.get(branchId))
                    .withAllocatedFlow(allocatedFlow)
                    .withXNodeFlow(xNodeFlow)
                    .withPstFlow(pstFlow)
                    .withInternalFlow(internalFlow)
                    .withLoopFlowsMap(loopFlowsMap)
                    .build();
            return decomposedFlowRescaler.rescale(decomposedFlow);
        }

        private double extractInternalFlow(Map<String, Double> loopFlowsMap, Country country1, Country country2) {
            if (Objects.equals(country1, country2)) {
                return Optional.ofNullable(loopFlowsMap.remove(NetworkUtil.getLoopFlowIdFromCountry(country1)))
                    .orElse(NO_FLOW);
            }
            return NO_FLOW;
        }
    }

    FlowDecompositionResults(Network network) {
        Date date = Date.from(Instant.now());
        String dateString = new SimpleDateFormat("yyyyMMdd-HHmmss").format(date);
        this.networkId = network.getNameOrId();
        this.id = "Flow_Decomposition_Results_of_" + dateString + "_on_network_" + networkId;
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
     * @return A rescaled flow decomposition map. The keys are the XNEC and the values are {@code DecomposedFlow} objects.
     */
    public Map<String, DecomposedFlow> getDecomposedFlowMap() {
        return decomposedFlowMap;
    }

    PerStateBuilder getBuilder(String contingencyId, Set<Branch> xnecList) {
        return new PerStateBuilder(contingencyId, xnecList);
    }

    public PerStateBuilder getBuilder(Set<Branch> xnecList) {
        return new PerStateBuilder(NO_CONTINGENCY_ID, xnecList);
    }
}
