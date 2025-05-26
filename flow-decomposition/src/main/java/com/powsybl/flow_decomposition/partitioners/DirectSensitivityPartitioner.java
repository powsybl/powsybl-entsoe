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
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.sensitivity.SensitivityAnalysis;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

import static com.powsybl.flow_decomposition.DecomposedFlow.*;
import static com.powsybl.flow_decomposition.DecomposedFlow.NO_FLOW;
import static com.powsybl.flow_decomposition.NetworkUtil.LOOP_FLOWS_COLUMN_PREFIX;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
public class DirectSensitivityPartitioner implements FlowPartitioner {
    private static final Logger LOGGER = LoggerFactory.getLogger(DirectSensitivityPartitioner.class);
    private final LoadFlowParameters loadFlowParameters;
    private final SensitivityAnalysis.Runner sensitivityAnalysisRunner;
    private FlowDecompositionObserverList observers;

    public DirectSensitivityPartitioner(LoadFlowParameters loadFlowParameters, SensitivityAnalysis.Runner sensitivityAnalysisRunner, FlowDecompositionObserverList observers) {
        this.loadFlowParameters = loadFlowParameters;
        this.sensitivityAnalysisRunner = sensitivityAnalysisRunner;
        this.observers = observers;
    }

    @Override
    public Map<String, FlowPartition> computeFlowPartitions(Network network, Set<Branch> xnecs, FlowDecompositionResults.PerStateBuilder flowDecompositionResultsBuilder, Map<Country, Double> netPositions, Map<Country, Map<String, Double>> glsks) {
        LOGGER.warn("Using fast mode of flow decomposition, detailed info (as nodal PTDF and PSDF matrices) won't be reported");
        NetworkMatrixIndexes networkMatrixIndexes = new NetworkMatrixIndexes(network, new ArrayList<>(xnecs));
        SparseMatrixWithIndexesTriplet nodalInjectionsMatrix = getNodalInjectionsMatrix(network, netPositions,
                networkMatrixIndexes, glsks);
        FastModeSensitivityAnalyser sensitivityAnalyser = new FastModeSensitivityAnalyser(loadFlowParameters, sensitivityAnalysisRunner, network, xnecs, nodalInjectionsMatrix);
        Map<String, Map<String, Double>> decomposedFlow = sensitivityAnalyser.run();
        return xnecs.stream().collect(Collectors.toMap(
                xnec -> xnec.getId(),
                xnec -> buildFlowPartition(xnec, decomposedFlow.getOrDefault(xnec.getId(), Collections.emptyMap()))
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

    private FlowPartition buildFlowPartition(Branch xnec, Map<String, Double> value) {
        Map<Country, Double> loopFlowsCountryMap = value.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith(LOOP_FLOWS_COLUMN_PREFIX))
                .collect(Collectors.toMap(entry -> Country.valueOf(entry.getKey().substring((LOOP_FLOWS_COLUMN_PREFIX + " ").length())), Map.Entry::getValue));
        Country country1 = NetworkUtil.getTerminalCountry(xnec.getTerminal1());
        Country country2 = NetworkUtil.getTerminalCountry(xnec.getTerminal2());
        double allocatedFlow = value.get(ALLOCATED_COLUMN_NAME);
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
