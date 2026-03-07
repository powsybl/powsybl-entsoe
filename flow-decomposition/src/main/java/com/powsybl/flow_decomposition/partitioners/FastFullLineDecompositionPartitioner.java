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
import org.ejml.data.DMatrixSparseCSC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static com.powsybl.flow_decomposition.DecomposedFlow.*;
import static com.powsybl.flow_decomposition.NetworkUtil.LOOP_FLOWS_COLUMN_PREFIX;

/**
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 */
public class FastFullLineDecompositionPartitioner implements FlowPartitioner {
    private static final Logger LOGGER = LoggerFactory.getLogger(FastFullLineDecompositionPartitioner.class);
    private final LoadFlowParameters loadFlowParameters;
    private final SensitivityAnalysis.Runner sensitivityAnalysisRunner;

    public FastFullLineDecompositionPartitioner(LoadFlowParameters loadFlowParameters, SensitivityAnalysis.Runner sensitivityAnalysisRunner) {
        this.loadFlowParameters = loadFlowParameters;
        this.sensitivityAnalysisRunner = sensitivityAnalysisRunner;
    }

    @Override
    public Map<String, FlowPartition> computeFlowPartitions(Network network, Set<Branch<?>> xnecs, Map<Country, Double> netPositions, Map<Country, Map<String, Double>> glsks) {
        LOGGER.warn("Using fast mode of flow decomposition, detailed info (as nodal PTDF and PSDF matrices) won't be reported");
        LOGGER.debug("{} === Bus mapping", LocalDateTime.now());
        List<Bus> busesInMainSynchronousComponent = NetworkUtil.getBusesInMainSynchronousComponent(network);
        List<Branch<?>> branchesConnectedInMainSynchronousComponent = NetworkUtil.getAllValidBranches(network);

        NetworkMatrixIndexes networkMatrixIndexes = new NetworkMatrixIndexes(network, xnecs.stream().toList());
        LOGGER.debug("{} === PEX graph generation", LocalDateTime.now());
        PexGraph pexGraph = new PexGraph(busesInMainSynchronousComponent, branchesConnectedInMainSynchronousComponent);

        LOGGER.debug("{} === PEX matrix computation", LocalDateTime.now());
        PexMatrixCalculator pexMatrixCalculator = new PexMatrixCalculator(pexGraph);
        Map<String, Integer> vertexIdMapping = pexMatrixCalculator.getVertexIdMapper();
        DMatrixSparseCSC pexMatrix = pexMatrixCalculator.computePexMatrix();

        LOGGER.debug("{} === Fast Full Line decomposition", LocalDateTime.now());
        FastFLDSensitivityAnalyser sensitivityAnalyser = new FastFLDSensitivityAnalyser(loadFlowParameters, sensitivityAnalysisRunner, network, xnecs, vertexIdMapping, pexMatrix, busesInMainSynchronousComponent);
        Map<String, Map<String, Double>> decomposedFlow = sensitivityAnalyser.run();

        Map<String, FlowPartition> results = xnecs.stream().collect(Collectors.toMap(
            Identifiable::getId,
            xnec -> buildFlowPartition(xnec, decomposedFlow.getOrDefault(xnec.getId(), Collections.emptyMap()))
        ));

        LOGGER.debug("{} === End of computation", LocalDateTime.now());

        return results;
    }

    private FlowPartition buildFlowPartition(Branch<?> xnec, Map<String, Double> value) {
        Map<Country, Double> loopFlowsCountryMap = value.entrySet().stream()
            .filter(entry -> entry.getKey().startsWith(LOOP_FLOWS_COLUMN_PREFIX))
            .collect(Collectors.toMap(entry -> Country.valueOf(entry.getKey().substring((LOOP_FLOWS_COLUMN_PREFIX + " ").length())), Map.Entry::getValue));
        Country country1 = NetworkUtil.getTerminalCountry(xnec.getTerminal1());
        Country country2 = NetworkUtil.getTerminalCountry(xnec.getTerminal2());
        double allocatedFlow = value.getOrDefault(ALLOCATED_COLUMN_NAME, NO_FLOW);
        double pstFlow = value.getOrDefault(PST_COLUMN_NAME, NO_FLOW);
        double xNodeFlow = value.getOrDefault(XNODE_COLUMN_NAME, NO_FLOW);
        double internalFlow = extractInternalFlowFromMap(loopFlowsCountryMap, country1, country2);
        return new FlowPartition(internalFlow, allocatedFlow, loopFlowsCountryMap, pstFlow, xNodeFlow);
    }

    private double extractInternalFlowFromMap(Map<Country, Double> loopFlowsMap, Country country1, Country country2) {
        if (Objects.equals(country1, country2)) {
            return Optional.ofNullable(loopFlowsMap.remove(country1))
                .orElse(NO_FLOW);
        }
        return NO_FLOW;
    }
}
