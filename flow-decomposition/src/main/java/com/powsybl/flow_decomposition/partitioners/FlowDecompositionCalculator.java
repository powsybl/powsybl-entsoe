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
    public static final double EPSILON = 1e-10;
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

        int nVertex = vertexIdMapping.size();
        this.vertexIds = new String[nVertex];
        this.isBusByVertexIndex = new boolean[nVertex];
        this.countriesByVertexPos = new Country[nVertex];

        fillMemberArraysBasedOnVertexIndexMapping(busesInMainSynchronousComponent, vertexIdMapping);
    }

    private void fillMemberArraysBasedOnVertexIndexMapping(List<Bus> busesInMainSynchronousComponent, Map<String, Integer> vertexIdMapping) {
        Map<String, Bus> idToBus = new HashMap<>();
        busesInMainSynchronousComponent.forEach(bus -> idToBus.put(bus.getId(), bus));
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

    Map<String, FlowPartition> computeDecomposition() {
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

        Country branchCountry1 = NetworkUtil.getBranchSideCountry(branch, TwoSides.ONE);
        Country branchCountry2 = NetworkUtil.getBranchSideCountry(branch, TwoSides.TWO);
        double[] ptdfColumn = transposedPtdfMatrix.getColumnAsArray(branchId);

        MutableTemporaryResult result = new MutableTemporaryResult(branchCountry1, branchCountry2);
        Iterator<DMatrixSparse.CoordinateRealValue> it = pexMatrix.createCoordinateIterator();
        while (it.hasNext()) {
            DMatrixSparse.CoordinateRealValue e = it.next();
            double exchange = e.value;
            double ptdfFrom = ptdfColumn[e.row];
            double ptdfTo = ptdfColumn[e.col];
            double increase = (ptdfFrom - ptdfTo) * exchange;

            if (Math.abs(increase) >= EPSILON) {
                processIncrease(e.row, e.col, increase, result);
            }
        }

        double pstFlow = pstFlowMatrix.getOrDefault(branchId, Collections.emptyMap()).values().stream()
            .mapToDouble(Double::doubleValue)
            .sum();
        return new FlowPartition(result.internalFlow, result.allocatedFlow, result.loopFlowsPerCountry, pstFlow, result.xNodeFlow);
    }

    private void processIncrease(int sourceIndex, int sinkIndex, double increase, MutableTemporaryResult result) {
        if (isBusByVertexIndex[sourceIndex] && isBusByVertexIndex[sinkIndex]) {
            Country countryFrom = countriesByVertexPos[sourceIndex];
            Country countryTo = countriesByVertexPos[sinkIndex];

            if (countryFrom == null || countryTo == null) {
                throw new PowsyblException(String.format("Cannot compute loop flow for bus %s and %s because of invalid country",
                    vertexIds[sourceIndex], vertexIds[sinkIndex]));
            }

            if (countryFrom == countryTo) {
                if (countryFrom == result.branchCountry1 && countryFrom == result.branchCountry2) {
                    result.internalFlow += increase;
                } else {
                    result.loopFlowsPerCountry.merge(countryFrom, increase, Double::sum);
                }
            } else {
                result.allocatedFlow += increase;
            }
        } else {
            result.xNodeFlow += increase;
        }
    }

    private static class MutableTemporaryResult {
        private final Country branchCountry1;
        private final Country branchCountry2;
        private double xNodeFlow = 0;
        private double internalFlow = 0;
        private double allocatedFlow = 0;
        private final Map<Country, Double> loopFlowsPerCountry = new EnumMap<>(Country.class);

        MutableTemporaryResult(Country branchCountry1, Country branchCountry2) {
            this.branchCountry1 = branchCountry1;
            this.branchCountry2 = branchCountry2;
        }
    }
}
