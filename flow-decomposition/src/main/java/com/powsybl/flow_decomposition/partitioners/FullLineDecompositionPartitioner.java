/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.flow_decomposition.partitioners;

import com.powsybl.flow_decomposition.*;
import com.powsybl.iidm.network.*;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.sensitivity.SensitivityAnalysis;
import com.powsybl.sensitivity.SensitivityVariableType;
import org.ejml.data.DMatrix;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class FullLineDecompositionPartitioner implements FlowPartitioner {
    private final LoadFlowParameters loadFlowParameters;
    private final FlowDecompositionParameters parameters;
    private final SensitivityAnalysis.Runner sensitivityAnalysisRunner;
    private final FlowDecompositionObserverList observers;

    public FullLineDecompositionPartitioner(LoadFlowParameters loadFlowParameters, FlowDecompositionParameters parameters, SensitivityAnalysis.Runner sensitivityAnalysisRunner, FlowDecompositionObserverList observers) {
        this.loadFlowParameters = loadFlowParameters;
        this.parameters = parameters;
        this.sensitivityAnalysisRunner = sensitivityAnalysisRunner;
        this.observers = observers;
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(FullLineDecompositionPartitioner.class);

    @Override
    public Map<String, FlowPartition> computeFlowPartitions(Network network, Set<Branch<?>> xnecs, Map<Country, Double> netPositions, Map<Country, Map<String, Double>> glsks) {
        LOGGER.info("{} === Bus mapping", LocalDateTime.now());
        List<Bus> busesInMainSynchronousComponent = network.getBusView().getBusStream()
                .filter(Bus::isInMainSynchronousComponent)
                .toList();
        Map<String, Integer> busMapping = NetworkUtil.getIndex(busesInMainSynchronousComponent.stream().map(Bus::getId).toList());
        List<Branch> branchesConnectedInMainSynchronousComponent = network.getBranchStream()
                .filter(NetworkUtil::isConnectedAndInMainSynchronous)
                .toList();

        NetworkMatrixIndexes networkMatrixIndexes = new NetworkMatrixIndexes(network, xnecs.stream().toList());
        LOGGER.info("{} === PEX graph generation", LocalDateTime.now());
        PexGraph pexGraph = new PexGraph(busesInMainSynchronousComponent, branchesConnectedInMainSynchronousComponent);

        LOGGER.info("{} === PEX matrix computation", LocalDateTime.now());
        PexMatrixCalculator pexMatrixCalculator = new PexMatrixCalculator(pexGraph, busMapping);
        DMatrix pexMatrix = pexMatrixCalculator.computePexMatrix();

        SensitivityAnalyser sensitivityAnalyser = getSensitivityAnalyser(network, networkMatrixIndexes);
        LOGGER.info("{} === PTDF matrix computation", LocalDateTime.now());
        SparseMatrixWithIndexesTriplet ptdfMatrix = getPtdfMatrix(networkMatrixIndexes, sensitivityAnalyser);

        LOGGER.info("{} === Final PST treatment", LocalDateTime.now());
        PstFlowComputer pstFlowComputer = new PstFlowComputer();
        SparseMatrixWithIndexesTriplet psdfMatrix = getPsdfMatrix(networkMatrixIndexes, sensitivityAnalyser);
        SparseMatrixWithIndexesCSC pstFlowMatrix = pstFlowComputer.run(network, networkMatrixIndexes, psdfMatrix);

        LOGGER.info("{} === Flow decomposition", LocalDateTime.now());
        FlowDecompositionCalculator flowDecompositionCalculator = new FlowDecompositionCalculator(xnecs, pexMatrix, ptdfMatrix, pstFlowMatrix, busesInMainSynchronousComponent, busMapping);
        Map<String, FlowPartition> results = flowDecompositionCalculator.computeDecomposition();

        LOGGER.info("{} === End of computation", LocalDateTime.now());

        return results;
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
}
