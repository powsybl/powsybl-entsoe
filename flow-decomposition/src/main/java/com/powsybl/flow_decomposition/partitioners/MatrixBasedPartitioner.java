/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.flow_decomposition.partitioners;

import com.powsybl.flow_decomposition.*;
import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Identifiable;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.sensitivity.SensitivityAnalysis;
import com.powsybl.sensitivity.SensitivityVariableType;

import java.util.*;
import java.util.stream.Collectors;

import static com.powsybl.flow_decomposition.DecomposedFlow.*;
import static com.powsybl.flow_decomposition.NetworkUtil.LOOP_FLOWS_COLUMN_PREFIX;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
public class MatrixBasedPartitioner implements FlowPartitioner {
    private final LoadFlowParameters loadFlowParameters;
    private final FlowDecompositionParameters parameters;
    private final SensitivityAnalysis.Runner sensitivityAnalysisRunner;
    private final FlowDecompositionObserverList observers;

    public MatrixBasedPartitioner(LoadFlowParameters loadFlowParameters, FlowDecompositionParameters parameters, SensitivityAnalysis.Runner sensitivityAnalysisRunner, FlowDecompositionObserverList observers) {
        this.loadFlowParameters = loadFlowParameters;
        this.parameters = parameters;
        this.sensitivityAnalysisRunner = sensitivityAnalysisRunner;
        this.observers = observers;
    }

    @Override
    public Map<String, FlowPartition> computeFlowPartitions(Network network, Set<Branch<?>> xnecs, Map<Country, Double> netPositions, Map<Country, Map<String, Double>> glsks) {
        NetworkMatrixIndexes networkMatrixIndexes = new NetworkMatrixIndexes(network, new ArrayList<>(xnecs));
        SparseMatrixWithIndexesTriplet nodalInjectionsMatrix = getNodalInjectionsMatrix(network, netPositions,
                networkMatrixIndexes, glsks);
        SensitivityAnalyser sensitivityAnalyser = getSensitivityAnalyser(network, networkMatrixIndexes);
        SparseMatrixWithIndexesTriplet ptdfMatrix = getPtdfMatrix(networkMatrixIndexes, sensitivityAnalyser);
        SparseMatrixWithIndexesTriplet psdfMatrix = getPsdfMatrix(networkMatrixIndexes, sensitivityAnalyser);

        // Flows
        SparseMatrixWithIndexesCSC allocatedLoopFlowsMatrix =
                SparseMatrixWithIndexesCSC.mult(ptdfMatrix.toCSCMatrix(), nodalInjectionsMatrix.toCSCMatrix());
        PstFlowComputer pstFlowComputer = new PstFlowComputer();
        SparseMatrixWithIndexesCSC pstFlowMatrix = pstFlowComputer.run(network, networkMatrixIndexes, psdfMatrix);
        return xnecs.stream().collect(Collectors.toMap(
                Identifiable::getId,
                xnec -> flowPartitionForXnec(xnec, allocatedLoopFlowsMatrix.toMap().getOrDefault(xnec.getId(), Collections.emptyMap()), pstFlowMatrix.toMap().getOrDefault(xnec.getId(), Collections.emptyMap()).getOrDefault(PST_COLUMN_NAME, NO_FLOW))
        ));
    }

    private SparseMatrixWithIndexesTriplet getNodalInjectionsMatrix(Network network,
                                                                    Map<Country, Double> netPositions,
                                                                    NetworkMatrixIndexes networkMatrixIndexes,
                                                                    Map<Country, Map<String, Double>> glsks) {
        NodalInjectionComputer nodalInjectionComputer = new NodalInjectionComputer(networkMatrixIndexes);
        SparseMatrixWithIndexesTriplet nodalInjectionsMatrix = nodalInjectionComputer.run(network, glsks, netPositions);
        observers.computedNodalInjectionsMatrix(nodalInjectionsMatrix.toMap());
        return nodalInjectionsMatrix;
    }

    private SensitivityAnalyser getSensitivityAnalyser(Network network, NetworkMatrixIndexes networkMatrixIndexes) {
        return new SensitivityAnalyser(loadFlowParameters, parameters, sensitivityAnalysisRunner, network, networkMatrixIndexes);
    }

    private SparseMatrixWithIndexesTriplet getPtdfMatrix(NetworkMatrixIndexes networkMatrixIndexes,
                                                         SensitivityAnalyser sensitivityAnalyser) {
        SparseMatrixWithIndexesTriplet ptdfMatrix = sensitivityAnalyser.run(networkMatrixIndexes.getNodeIdList(),
                networkMatrixIndexes.getNodeIndex(),
                SensitivityVariableType.INJECTION_ACTIVE_POWER);
        observers.computedPtdfMatrix(ptdfMatrix.toMap());
        return ptdfMatrix;
    }

    private SparseMatrixWithIndexesTriplet getPsdfMatrix(NetworkMatrixIndexes networkMatrixIndexes,
                                                         SensitivityAnalyser sensitivityAnalyser) {
        SparseMatrixWithIndexesTriplet psdfMatrix = sensitivityAnalyser.run(networkMatrixIndexes.getPstList(),
                networkMatrixIndexes.getPstIndex(), SensitivityVariableType.TRANSFORMER_PHASE);
        observers.computedPsdfMatrix(psdfMatrix.toMap());
        return psdfMatrix;
    }

    private FlowPartition flowPartitionForXnec(Branch<?> xnec, Map<String, Double> allocatedLoopFlowsMap, double pstFlow) {
        double allocatedFlow = allocatedLoopFlowsMap.getOrDefault(ALLOCATED_COLUMN_NAME, NO_FLOW);
        double xnodeFlow = allocatedLoopFlowsMap.getOrDefault(XNODE_COLUMN_NAME, NO_FLOW);
        Country country1 = NetworkUtil.getTerminalCountry(xnec.getTerminal1());
        Country country2 = NetworkUtil.getTerminalCountry(xnec.getTerminal2());
        double internalFlow = extractInternalFlow(allocatedLoopFlowsMap, country1, country2);
        Map<Country, Double> loopFlow = allocatedLoopFlowsMap.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith(LOOP_FLOWS_COLUMN_PREFIX))
                .collect(Collectors.toMap(
                        entry -> Country.valueOf(entry.getKey().substring((LOOP_FLOWS_COLUMN_PREFIX + " ").length())),
                        Map.Entry::getValue
                ));
        return new FlowPartition(internalFlow, allocatedFlow, loopFlow, pstFlow, xnodeFlow);
    }

    private double extractInternalFlow(Map<String, Double> loopFlowsMap, Country country1, Country country2) {
        if (Objects.equals(country1, country2)) {
            return Optional.ofNullable(loopFlowsMap.remove(NetworkUtil.getLoopFlowIdFromCountry(country1)))
                    .orElse(NO_FLOW);
        }
        return NO_FLOW;
    }
}
