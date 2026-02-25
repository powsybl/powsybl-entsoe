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
    private final List<Bus> busesOfInterest;
    private final Map<String, Integer> busMapping;
    private final Map<Bus, Injection<?>> anyInjectionOnBus;

    public FlowDecompositionCalculator(Set<Branch<?>> xnecs, DMatrix pexMatrix, SparseMatrixWithIndexesTriplet ptdfMatrix, SparseMatrixWithIndexesCSC pstFlowMatrix, List<Bus> busesInMainSynchronousComponent, Map<String, Integer> busMapping) {
        this.xnecs = Objects.requireNonNull(xnecs);
        this.pexMatrix = Objects.requireNonNull(pexMatrix);
        this.ptdfMatrix = Objects.requireNonNull(ptdfMatrix).toMap();
        this.pstFlowMatrix = Objects.requireNonNull(pstFlowMatrix).toMap();
        this.busesOfInterest = busesInMainSynchronousComponent;
        this.anyInjectionOnBus = busesOfInterest.stream().collect(Collectors.toMap(bus -> bus, bus -> NetworkUtil.getInjectionStream(bus).filter(NetworkUtil::isConnectedAndInMainSynchronousComponent).findAny().orElseThrow()));
        this.busMapping = busMapping;
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

        Map<String, Double> ptdfs = ptdfMatrix.get(branch.getId());
        for (Bus busFrom : busesOfInterest) {
            for (Bus busTo : busesOfInterest) {
                int busFromIndex = busMapping.get(busFrom.getId());
                int busToIndex = busMapping.get(busTo.getId());
                double exchangeBetweenFromAndTo = pexMatrix.get(busFromIndex, busToIndex);
                if (Math.abs(exchangeBetweenFromAndTo) < EPSILON) {
                    // Avoid failing on PTDF retrieval if no injection on the buses
                    continue;
                }
                Country countryFrom = busFrom.getVoltageLevel().getSubstation().orElseThrow().getCountry().orElse(null);
                Country countryTo = busTo.getVoltageLevel().getSubstation().orElseThrow().getCountry().orElse(null);

                Injection<?> injectionFrom = anyInjectionOnBus.get(busFrom);
                Injection<?> injectionTo = anyInjectionOnBus.get(busTo);
                double increase = (ptdfs.get(injectionFrom.getId()) - ptdfs.get(injectionTo.getId())) * exchangeBetweenFromAndTo;

                if (countryFrom != null && countryTo != null) {
                    double current = countryExchangeFlows.row(countryFrom).getOrDefault(countryTo, 0.);
                    countryExchangeFlows.put(countryFrom, countryTo, current + increase);
                }
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
        double xNodeFlow = 0;
        return new FlowPartition(internalFlow, allocatedFlow, loopFlowsPerCountry, pstFlow, xNodeFlow);
    }
}
