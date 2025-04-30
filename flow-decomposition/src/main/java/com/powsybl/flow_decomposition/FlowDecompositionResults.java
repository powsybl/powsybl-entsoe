/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.flow_decomposition;

import com.powsybl.flow_decomposition.rescaler.DecomposedFlowRescaler;
import com.powsybl.iidm.network.*;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

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

    public class PerStateBuilder {
        private final Map<String, Branch> xnecMap;
        private final String contingencyId;
        private Map<String, Double> acTerminal1ReferenceFlow;
        private Map<String, Double> acTerminal2ReferenceFlow;
        private Map<String, Double> acCurrentTerminal1;
        private Map<String, Double> acCurrentTerminal2;
        private Map<String, Double> dcReferenceFlow;
        private Map<String, FlowPartition> flowPartitions = new HashMap<>();
        private final FlowDecompositionObserverList observers = new FlowDecompositionObserverList();

        PerStateBuilder(String contingencyId, Set<Branch> xnecList) {
            this.xnecMap = xnecList.stream().collect(Collectors.toMap(Identifiable::getId, Function.identity()));
            this.contingencyId = contingencyId;
        }

        void saveAcTerminal1ReferenceFlow(Map<String, Double> acTerminal1ReferenceFlow) {
            this.acTerminal1ReferenceFlow = acTerminal1ReferenceFlow;
        }

        void saveAcTerminal2ReferenceFlow(Map<String, Double> acTerminal2ReferenceFlow) {
            this.acTerminal2ReferenceFlow = acTerminal2ReferenceFlow;
        }

        void saveAcCurrentTerminal1(Map<String, Double> acTerminal1Current) {
            this.acCurrentTerminal1 = acTerminal1Current;
        }

        void saveAcCurrentTerminal2(Map<String, Double> acTerminal2Current) {
            this.acCurrentTerminal2 = acTerminal2Current;
        }

        void saveDcReferenceFlow(Map<String, Double> dcReferenceFlow) {
            this.dcReferenceFlow = dcReferenceFlow;
        }

        public void saveFlowPartitions(Map<String, FlowPartition> flowPartitions) {
            this.flowPartitions = flowPartitions;
        }

        void addObserversList(FlowDecompositionObserverList observers) {
            this.observers.addObserversFrom(observers);
        }

        void build(DecomposedFlowRescaler decomposedFlowRescaler, Network network) {
            flowPartitions
                .forEach((branchId, flowPartition) -> {
                    String xnecId = DecomposedFlow.getXnecId(contingencyId, branchId);
                    decomposedFlowMap.put(xnecId, createDecomposedFlow(branchId, flowPartition, decomposedFlowRescaler, network));
                });
        }

        private DecomposedFlow createDecomposedFlow(String branchId, FlowPartition flowPartition, DecomposedFlowRescaler decomposedFlowRescaler, Network network) {
            Country country1 = NetworkUtil.getTerminalCountry(xnecMap.get(branchId).getTerminal1());
            Country country2 = NetworkUtil.getTerminalCountry(xnecMap.get(branchId).getTerminal2());
            DecomposedFlow decomposedFlow = new DecomposedFlowBuilder()
                    .withBranchId(branchId)
                    .withContingencyId(contingencyId)
                    .withCountry1(country1)
                    .withCountry2(country2)
                    .withAcTerminal1ReferenceFlow(acTerminal1ReferenceFlow.get(branchId))
                    .withAcTerminal2ReferenceFlow(acTerminal2ReferenceFlow.get(branchId))
                    .withDcReferenceFlow(dcReferenceFlow.get(branchId))
                    .withAcCurrentTerminal1(acCurrentTerminal1.get(branchId))
                    .withAcCurrentTerminal2(acCurrentTerminal2.get(branchId))
                    .withFlowPartition(flowPartition)
                    .build();
            observers.computedPreRescalingDecomposedFlows(decomposedFlow);
            return decomposedFlowRescaler.rescale(decomposedFlow, network);
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
