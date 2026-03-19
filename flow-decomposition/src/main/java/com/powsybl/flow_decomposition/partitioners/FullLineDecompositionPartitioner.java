/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.flow_decomposition.partitioners;

import com.powsybl.flow_decomposition.*;
import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Bus;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.sensitivity.SensitivityAnalysis;
import org.ejml.data.DMatrixSparseCSC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class FullLineDecompositionPartitioner implements FlowPartitioner {
    private static final Logger LOGGER = LoggerFactory.getLogger(FullLineDecompositionPartitioner.class);
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

    @Override
    public Map<String, FlowPartition> computeFlowPartitions(Network network, Set<Branch<?>> xnecs, Map<Country, Double> netPositions, Map<Country, Map<String, Double>> glsks) {
        LOGGER.info("[FLD] Bus mapping");
        List<Bus> busesInMainSynchronousComponent = NetworkUtil.getBusesInMainSynchronousComponent(network);
        List<Branch<?>> branchesConnectedInMainSynchronousComponent = NetworkUtil.getAllValidBranches(network);

        NetworkMatrixIndexes networkMatrixIndexes = new NetworkMatrixIndexes(network, xnecs.stream().toList());
        LOGGER.info("[FLD] PEX graph generation");
        PexGraph pexGraph = new PexGraph(busesInMainSynchronousComponent, branchesConnectedInMainSynchronousComponent);

        LOGGER.info("[FLD] PEX matrix computation");
        PexMatrixCalculator pexMatrixCalculator = new PexMatrixCalculator(pexGraph);
        Map<String, Integer> vertexIdMapping = pexMatrixCalculator.getVertexIdMapper();
        DMatrixSparseCSC pexMatrix = pexMatrixCalculator.computePexMatrix();

        SensitivityAnalyser sensitivityAnalyser = getSensitivityAnalyser(network, networkMatrixIndexes);
        LOGGER.info("[FLD] PTDF matrix computation");
        Map<String, Integer> injectionIdIndex = NetworkUtil.chooseAnInjectionPerVertexAndKeepSameIndex(vertexIdMapping, network);
        SparseMatrixWithIndexesCSC ptdfMatrix = getNodalPtdfMatrix(injectionIdIndex, sensitivityAnalyser);

        LOGGER.info("[FLD] Final PST treatment");
        PstFlowComputer pstFlowComputer = new PstFlowComputer();
        SparseMatrixWithIndexesTriplet psdfMatrix = getPsdfMatrix(networkMatrixIndexes, sensitivityAnalyser);
        SparseMatrixWithIndexesCSC pstFlowMatrix = pstFlowComputer.run(network, networkMatrixIndexes, psdfMatrix);

        LOGGER.info("[FLD] Flow decomposition");
        FlowDecompositionCalculator flowDecompositionCalculator = new FlowDecompositionCalculator(xnecs, pexMatrix, ptdfMatrix, pstFlowMatrix, busesInMainSynchronousComponent, vertexIdMapping);
        Map<String, FlowPartition> results = flowDecompositionCalculator.computeDecomposition();

        LOGGER.info("[FLD] End of computation");

        return results;
    }

    private SensitivityAnalyser getSensitivityAnalyser(Network network, NetworkMatrixIndexes networkMatrixIndexes) {
        return new SensitivityAnalyser(loadFlowParameters, parameters, sensitivityAnalysisRunner, network, networkMatrixIndexes);
    }

    private SparseMatrixWithIndexesCSC getNodalPtdfMatrix(Map<String, Integer> injectionIdIndex,
                                                              SensitivityAnalyser sensitivityAnalyser) {
        SparseMatrixWithIndexesCSC ptdfMatrix = sensitivityAnalyser.getNodalPtdfMatrix(injectionIdIndex);
        if (!observers.getObservers().isEmpty()) {
            observers.computedPtdfMatrix(ptdfMatrix.toMap());
        }
        return ptdfMatrix;
    }

    private SparseMatrixWithIndexesTriplet getPsdfMatrix(NetworkMatrixIndexes networkMatrixIndexes,
                                                         SensitivityAnalyser sensitivityAnalyser) {
        SparseMatrixWithIndexesTriplet psdfMatrix = sensitivityAnalyser.getPsdfMatrix(networkMatrixIndexes);
        if (!observers.getObservers().isEmpty()) {
            observers.computedPsdfMatrix(psdfMatrix.toMap());
        }
        return psdfMatrix;
    }
}
