/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.flow_decomposition.partitioners;

import com.powsybl.commons.PowsyblException;
import com.powsybl.flow_decomposition.FlowPartition;
import com.powsybl.flow_decomposition.NetworkUtil;
import com.powsybl.iidm.network.*;
import org.ejml.data.DMatrixSparse;
import org.ejml.data.DMatrixSparseCSC;
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
class FlowDecompositionCalculator {
    private static final Logger LOGGER = LoggerFactory.getLogger(FlowDecompositionCalculator.class);
    private final Set<Branch<?>> xnecs;
    private final DMatrixSparseCSC pexMatrix;
    private final SparseMatrixWithIndexesCSC transposedPtdfMatrix;
    private final Map<String, Map<String, Double>> pstFlowMatrix;
    private final String[] vertexIds;
    private final boolean[] isBusByVertexIndex;
    private final Country[] countriesByVertexPos;

    FlowDecompositionCalculator(Set<Branch<?>> xnecs, DMatrixSparseCSC pexMatrix, SparseMatrixWithIndexesCSC sparsePtdfMatrix, SparseMatrixWithIndexesCSC pstFlowMatrix, List<Bus> busesInMainSynchronousComponent, Map<String, Integer> vertexIdMapping) {
        this.xnecs = Objects.requireNonNull(xnecs);
        this.pexMatrix = pexMatrix;
        this.transposedPtdfMatrix = Objects.requireNonNull(sparsePtdfMatrix).transpose();

        this.pstFlowMatrix = Objects.requireNonNull(pstFlowMatrix).toMap();

        Map<String, Bus> idToBus = new HashMap<>();
        busesInMainSynchronousComponent.forEach(bus -> idToBus.put(bus.getId(), bus));

        int nVertex = vertexIdMapping.size();
        this.vertexIds = new String[nVertex];
        this.isBusByVertexIndex = new boolean[nVertex];
        this.countriesByVertexPos = new Country[nVertex];

        NetworkUtil.getIndex(busesInMainSynchronousComponent.stream().map(bus -> bus.getVoltageLevel().getSubstation().orElseThrow().getCountry().orElse(null)).distinct().toList());

        vertexIdMapping.forEach((id, index) -> {
            this.vertexIds[index] = id;

            Bus bus = idToBus.get(id);
            if (bus != null) {
                this.isBusByVertexIndex[index] = true;
                Country country = bus.getVoltageLevel()
                    .getSubstation().orElseThrow()
                    .getCountry().orElse(null);
                this.countriesByVertexPos[index] = country;
            }
        });
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
            LOGGER.debug("Branch {} is not connected or not in main synchronous component. Returning empty decomposition", branchId);
            return new FlowPartition(0., 0., Collections.emptyMap(), 0., 0.);
        }

        double xNodeFlow = 0;
        double internalFlow = 0.0;
        double allocatedFlow = 0.0;
        Map<Country, Double> loopFlowsPerCountry = new EnumMap<>(Country.class);

        Country branchCountry1 = NetworkUtil.getBranchSideCountry(branch, TwoSides.ONE);
        Country branchCountry2 = NetworkUtil.getBranchSideCountry(branch, TwoSides.TWO);
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

            if (isBusByVertexIndex[sourceIndex] && isBusByVertexIndex[sinkIndex]) {
                // Loop flow
                Country countryFrom = countriesByVertexPos[sourceIndex];
                Country countryTo = countriesByVertexPos[sinkIndex];
                if (countryFrom == null || countryTo == null) {
                    String sourceId = vertexIds[sourceIndex];
                    String sinkId = vertexIds[sinkIndex];
                    throw new PowsyblException(String.format("Cannot compute loop flow for bus %s and %s because of invalid country", sourceId, sinkId));
                }
                if (countryFrom.equals(countryTo)) {
                    if (countryFrom.equals(branchCountry1) && countryFrom.equals(branchCountry2)) {
                        internalFlow += increase;
                    } else {
                        loopFlowsPerCountry.compute(countryFrom, (country, value) -> value == null ? increase : value + increase);
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
        return new FlowPartition(internalFlow, allocatedFlow, loopFlowsPerCountry, pstFlow, xNodeFlow);
    }
}
