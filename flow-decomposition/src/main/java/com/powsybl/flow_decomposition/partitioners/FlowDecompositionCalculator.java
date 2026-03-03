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
    private final String[] vertexIds;
    private final String[] injectionIdByVertexIndex;
    private final boolean[] isBusByVertexIndex;
    private final Country[] countriesByVertexPos;

    public FlowDecompositionCalculator(Set<Branch<?>> xnecs, DMatrix pexMatrix, SparseMatrixWithIndexesTriplet ptdfMatrix, SparseMatrixWithIndexesCSC pstFlowMatrix, List<Bus> busesInMainSynchronousComponent, Map<String, Integer> vertexMapping) {
        this.xnecs = Objects.requireNonNull(xnecs);
        this.pexMatrix = Objects.requireNonNull(pexMatrix);
        this.ptdfMatrix = Objects.requireNonNull(ptdfMatrix).toMap();
        this.pstFlowMatrix = Objects.requireNonNull(pstFlowMatrix).toMap();

        Map<String, String> anyInjectionOnBus = busesInMainSynchronousComponent.stream().collect(Collectors.toMap(Identifiable::getId, bus -> NetworkUtil.getInjectionStream(bus).findAny().orElseThrow().getId()));
        Map<String, Bus> idToBus = new HashMap<>();
        busesInMainSynchronousComponent.forEach(bus -> idToBus.put(bus.getId(), bus));

        int nVertex = vertexMapping.size();
        this.vertexIds = new String[nVertex];
        this.injectionIdByVertexIndex = new String[nVertex];
        this.isBusByVertexIndex = new boolean[nVertex];
        this.countriesByVertexPos = new Country[nVertex];

        vertexMapping.forEach((id, index) -> {
            this.vertexIds[index] = id;
            String inj = anyInjectionOnBus.get(id);
            this.injectionIdByVertexIndex[index] = Optional.ofNullable(inj).orElse(id); // dangling line fallback

            Bus bus = idToBus.get(id);
            if (bus != null) {
                this.isBusByVertexIndex[index] = true;
                this.countriesByVertexPos[index] = bus.getVoltageLevel()
                    .getSubstation().orElseThrow()
                    .getCountry().orElse(null);
            }


        });

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
        int nVertex = vertexIds.length;
        for (int sourceIndex = 0; sourceIndex < nVertex; sourceIndex++) {
            for (int sinkIndex = 0; sinkIndex < nVertex; sinkIndex++) {
                String sourceId = vertexIds[sourceIndex];
                String sinkId = vertexIds[sinkIndex];
                computeFlowBasedOnExchangeBetweenSourceAndSink(sourceId, sourceIndex, sinkId, sinkIndex, ptdfs, countryExchangeFlows, xNodeFlow);
            }
        }

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

        String injectionFrom = injectionIdByVertexIndex[sourceIndex];
        String injectionTo = injectionIdByVertexIndex[sinkIndex];

        Double ptdfFrom = ptdfs.get(injectionFrom);
        Double ptdfTo = ptdfs.get(injectionTo);
        if (ptdfFrom == null || ptdfTo == null) {
            return;
        }
        double increase = (ptdfFrom - ptdfTo) * exchangeBetweenFromAndTo;

        if ((isBusByVertexIndex[sourceIndex] && isBusByVertexIndex[sinkIndex])) {
            // Loop flow
            Country countryFrom = countriesByVertexPos[sourceIndex];
            Country countryTo = countriesByVertexPos[sinkIndex];
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
