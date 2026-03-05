/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.flow_decomposition.partitioners;

import com.powsybl.flow_decomposition.FlowPartition;
import com.powsybl.flow_decomposition.NetworkUtil;
import com.powsybl.iidm.network.*;
import org.ejml.data.DMatrixSparse;
import org.ejml.data.DMatrixSparseCSC;
import org.ejml.sparse.csc.CommonOps_DSCC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Function;
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
    private final DMatrixSparseCSC pexMatrix;
    private final SparseMatrixWithIndexesCSC transposedPtdfMatrix;
    private final Map<String, Map<String, Double>> pstFlowMatrix;
    private final String[] vertexIds;
    private final boolean[] isBusByVertexIndex;
    private final Integer[] countriesByVertexPos;
    private final Map<Country, Integer> countryIndex;

    public FlowDecompositionCalculator(Set<Branch<?>> xnecs, DMatrixSparseCSC pexMatrix, SparseMatrixWithIndexesCSC sparsePtdfMatrix, SparseMatrixWithIndexesCSC pstFlowMatrix, List<Bus> busesInMainSynchronousComponent, Map<String, Integer> vertexIdMapping) {
        this.xnecs = Objects.requireNonNull(xnecs);
        this.pexMatrix = new DMatrixSparseCSC(pexMatrix.numRows, pexMatrix.numCols, pexMatrix.nz_length);
        CommonOps_DSCC.removeZeros(Objects.requireNonNull(pexMatrix), this.pexMatrix, 1e-9);
        this.transposedPtdfMatrix = Objects.requireNonNull(sparsePtdfMatrix).transpose();

        this.pstFlowMatrix = Objects.requireNonNull(pstFlowMatrix).toMap();

        Map<String, String> anyInjectionOnBus = busesInMainSynchronousComponent.stream().collect(Collectors.toMap(Identifiable::getId, bus -> NetworkUtil.getInjectionStream(bus).findAny().orElseThrow().getId()));
        Map<String, Bus> idToBus = new HashMap<>();
        busesInMainSynchronousComponent.forEach(bus -> idToBus.put(bus.getId(), bus));

        int nVertex = vertexIdMapping.size();
        this.vertexIds = new String[nVertex];
        this.isBusByVertexIndex = new boolean[nVertex];
        this.countriesByVertexPos = new Integer[nVertex];

        this.countryIndex = NetworkUtil.getIndex(busesInMainSynchronousComponent.stream().map(bus -> bus.getVoltageLevel().getSubstation().orElseThrow().getCountry().orElse(null)).distinct().toList());

        vertexIdMapping.forEach((id, index) -> {
            this.vertexIds[index] = id;
            String inj = anyInjectionOnBus.get(id);

            Bus bus = idToBus.get(id);
            if (bus != null) {
                this.isBusByVertexIndex[index] = true;
                Country country = bus.getVoltageLevel()
                    .getSubstation().orElseThrow()
                    .getCountry().orElse(null);
                this.countriesByVertexPos[index] = countryIndex.get(country);
            }
        });

        int toto =0;

    }

    public Map<String, FlowPartition> computeDecomposition() {
        LOGGER.debug("Decomposing flow on branches");
        return xnecs.stream()
            .collect(
                Collectors.toMap(
                    Identifiable::getId,
                    this::decomposeFlow
                )
            );
    }

    private FlowPartition decomposeFlow(Branch<?> branch) {
        String branchId = branch.getId();

        if (!NetworkUtil.isConnectedAndInMainSynchronousComponent(branch)) {
            LOGGER.warn("Branch {} is not connected or not in main synchronous component. Returning empty decomposition", branchId);
            return new FlowPartition(0., 0., Collections.emptyMap(), 0., 0.);
        }

        double xNodeFlow = 0;
        double internalFlow = 0.0;
        double allocatedFlow = 0.0;
        double[] loopFlowsPerCountry = new double[countryIndex.size()];

        Integer branchCountry1 = countryIndex.get(NetworkUtil.getBranchSideCountry(branch, TwoSides.ONE));
        Integer branchCountry2 = countryIndex.get(NetworkUtil.getBranchSideCountry(branch, TwoSides.TWO));
        double[] column = transposedPtdfMatrix.getColumnAsArray(branchId);

        Iterator<DMatrixSparse.CoordinateRealValue> coordinateRealValueIterator = pexMatrix.createCoordinateIterator();
        while (coordinateRealValueIterator.hasNext()) {
            DMatrixSparse.CoordinateRealValue e = coordinateRealValueIterator.next();
            int sourceIndex = e.row;
            int sinkIndex = e.col;
            double exchangeBetweenFromAndTo = e.value;


            Double ptdfFrom = column[sourceIndex];
            Double ptdfTo = column[sinkIndex];
            double increase = (ptdfFrom - ptdfTo) * exchangeBetweenFromAndTo;
            if (Math.abs(increase) < 1e-10) {
                continue;
            }

            if ((isBusByVertexIndex[sourceIndex] && isBusByVertexIndex[sinkIndex])) {
                // Loop flow
                Integer countryFrom = countriesByVertexPos[sourceIndex];
                Integer countryTo = countriesByVertexPos[sinkIndex];
                if (countryFrom == null || countryTo == null) {
                    String sourceId = vertexIds[sourceIndex];
                    String sinkId = vertexIds[sinkIndex];
                    LOGGER.warn("Cannot compute loop flow for bus {} and {} because of invalid country", sourceId, sinkId);
                    continue;
                }
                if (countryFrom.equals(countryTo)) {
                    if (countryFrom.equals(branchCountry1) && countryFrom.equals(branchCountry2)) {
                        internalFlow += increase;
                    } else {
                        loopFlowsPerCountry[countryFrom] += increase;
                    }
                } else {
                    allocatedFlow += increase;
                }
            } else {
                xNodeFlow += increase;
            }
        }

        double pstFlow = pstFlowMatrix.getOrDefault(branchId, Collections.emptyMap()).values().stream()
            .mapToDouble(d -> d)
            .sum();
        Map<Country, Double> loopFlowsPerCountryMap = countryIndex.keySet().stream().collect(Collectors.toMap(Function.identity(), country -> loopFlowsPerCountry[countryIndex.get(country)]));
        return new FlowPartition(internalFlow, allocatedFlow, loopFlowsPerCountryMap, pstFlow, xNodeFlow);
    }
}
