/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.flow_decomposition.partitioners;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.powsybl.flow_decomposition.FlowPartition;
import com.powsybl.flow_decomposition.NetworkUtil;
import com.powsybl.iidm.network.*;
import org.ejml.data.DMatrix;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Flow decomposition calculator based on calculated matrix for
 * PEX, PTDF and PSDF
 *
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
public class FlowDecompositionCalculator {
    private static final Logger LOGGER = LoggerFactory.getLogger(FlowDecompositionCalculator.class);
    private static final double EPSILON = 1e-5;
    private final Set<Branch<?>> xnecs;
    private final DMatrix pexMatrix;
    private final Map<String, Map<String, Double>> ptdfMatrix;
    private final Map<String, Map<String, Double>> pstFlowMatrix;
    private final Map<String, Integer> vertexMapping;
    private final Map<String, String> anyInjectionOnBus;
    private final Map<String, Bus> idToBus;

    public FlowDecompositionCalculator(Set<Branch<?>> xnecs, DMatrix pexMatrix, SparseMatrixWithIndexesTriplet ptdfMatrix, SparseMatrixWithIndexesCSC pstFlowMatrix, List<Bus> busesInMainSynchronousComponent, Map<String, Integer> vertexMapping) {
        this.xnecs = Objects.requireNonNull(xnecs);
        this.pexMatrix = Objects.requireNonNull(pexMatrix);
        this.ptdfMatrix = Objects.requireNonNull(ptdfMatrix).toMap();
        this.pstFlowMatrix = Objects.requireNonNull(pstFlowMatrix).toMap();
        this.anyInjectionOnBus = busesInMainSynchronousComponent.stream().collect(Collectors.toMap(Identifiable::getId, bus -> NetworkUtil.getInjectionStream(bus).findAny().orElseThrow().getId()));
        this.vertexMapping = vertexMapping;
        this.idToBus = new HashMap<>();
        busesInMainSynchronousComponent.forEach(bus -> idToBus.put(bus.getId(), bus));
    }

    public Map<String, FlowPartition> computeDecomposition() {
        return xnecs.stream()
                .collect(
                Collectors.toMap(
                        Identifiable::getId,
                        this::decomposeFlow
                )
        );
    }

    private FlowPartition decomposeFlow(Branch<?> branch) {
        LOGGER.info("Decomposing flow on branch {}", branch.getId());

        if (!NetworkUtil.isConnectedAndInMainSynchronousComponent(branch)) {
            return new FlowPartition(0., 0., Collections.emptyMap(), 0., 0.);
        }

        Table<Country, Country, Double> countryExchangeFlows = HashBasedTable.create();
        Country branchCountry1 = NetworkUtil.getBranchSideCountry(branch, TwoSides.ONE);

        final double[] xNodeFlow = {0};
        Map<String, Double> ptdfs = ptdfMatrix.get(branch.getId());
        vertexMapping.forEach((sourceId, sourceIndex) -> {
            vertexMapping.forEach((sinkId, sinkIndex) -> {
                computeFlowBasedOnExchangeBetweenSourceAndSink(sourceId, sourceIndex, sinkId, sinkIndex, ptdfs, countryExchangeFlows, xNodeFlow);
            });
        });

        double internalFlow = Optional.ofNullable(countryExchangeFlows.get(branchCountry1, branchCountry1)).orElse(0.);
        double allocatedFlow = countryExchangeFlows.cellSet().stream()
                .filter(cell -> !cell.getRowKey().equals(cell.getColumnKey()))
                .mapToDouble(Table.Cell::getValue)
                .sum();
        Map<Country, Double> loopFlowsPerCountry = countryExchangeFlows.cellSet().stream()
                .filter(cell -> cell.getRowKey().equals(cell.getColumnKey())) // loop flow
                .filter(cell -> !cell.getRowKey().equals(branchCountry1)) // not internal flow
                .collect(Collectors.toMap(Table.Cell::getRowKey, Table.Cell::getValue));
        double pstFlow = pstFlowMatrix.getOrDefault(branch.getId(), Collections.emptyMap()).values().stream()
                .mapToDouble(d -> d)
                .sum();
        return new FlowPartition(internalFlow, allocatedFlow, loopFlowsPerCountry, pstFlow, xNodeFlow[0]);
    }

    private void computeFlowBasedOnExchangeBetweenSourceAndSink(String sourceId, Integer sourceIndex, String sinkId, Integer sinkIndex, Map<String, Double> ptdfs, Table<Country, Country, Double> countryExchangeFlows, double[] xNodeFlow) {
        double exchangeBetweenFromAndTo = pexMatrix.get(sourceIndex, sinkIndex);
        if (Math.abs(exchangeBetweenFromAndTo) < EPSILON) {
            return;
        }

        String injectionFrom = Optional.ofNullable(anyInjectionOnBus.get(sourceId)).orElse(sourceId); // If id is not a bus, it is a dangling line
        String injectionTo = Optional.ofNullable(anyInjectionOnBus.get(sinkId)).orElse(sinkId);
        double ptdf = ptdfs.get(injectionFrom) - ptdfs.get(injectionTo);
        double increase = ptdf * exchangeBetweenFromAndTo;

        if ((idToBus.containsKey(sourceId) && idToBus.containsKey(sinkId))) {
            // Loop flow
            Country countryFrom = idToBus.get(sourceId).getVoltageLevel().getSubstation().orElseThrow().getCountry().orElse(null);
            Country countryTo = idToBus.get(sinkId).getVoltageLevel().getSubstation().orElseThrow().getCountry().orElse(null);
            if (countryFrom != null && countryTo != null) {
                double current = countryExchangeFlows.row(countryFrom).getOrDefault(countryTo, 0.);
                countryExchangeFlows.put(countryFrom, countryTo, current + increase);
            } else {
                LOGGER.warn("Cannot compute loop flow for bus {} and {} because of invalid country", sourceId, sinkId);
            }
        } else {
            // XNode flow
            xNodeFlow[0] += increase;
        }
    }
}
