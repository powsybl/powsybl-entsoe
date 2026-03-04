/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.flow_decomposition.partitioners;

import com.powsybl.flow_decomposition.NetworkUtil;
import org.ejml.data.DGrowArray;
import org.ejml.data.DMatrixSparseCSC;
import org.ejml.data.DMatrixSparseTriplet;
import org.ejml.data.IGrowArray;
import org.ejml.ops.DConvertMatrixStruct;
import org.ejml.sparse.csc.CommonOps_DSCC;
import org.jgrapht.alg.cycle.CycleDetector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Object dedicated to PEX matrix calculation
 *
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
public class PexMatrixCalculator {
    public static final int MAX_ITERATION = 1000;
    public static final double L1_NORM_RELATIVE_TOLERANCE = 1e-9;
    public static final double DROP_TOLERANCE = 1e-10;
    private static final double EPSILON = 1e-5;
    private static final Logger LOGGER = LoggerFactory.getLogger(PexMatrixCalculator.class);
    private final PexGraph pexGraph;
    private final Map<PexGraphVertex, Integer> vertexMapper = new HashMap<>();
    private final Map<String, Integer> vertexIdMapper;

    public PexMatrixCalculator(PexGraph pexGraph) {
        this.pexGraph = Objects.requireNonNull(pexGraph);
        this.vertexIdMapper = NetworkUtil.getIndex(pexGraph.vertexSet().stream().map(PexGraphVertex::getId).toList());

        pexGraph.vertexSet().forEach(vertex -> vertexMapper.put(vertex, Objects.requireNonNull(vertexIdMapper.get(vertex.getId()))));
    }

    private static boolean determineIfGraphHasCycle(PexGraph pexGraph1) {
        boolean hasCycle = new CycleDetector<>(pexGraph1).detectCycles();
        LOGGER.info("PEX graph: vertices={}, edges={}, hasDirectedCycle={}",
            pexGraph1.vertexSet().size(),
            pexGraph1.edgeSet().size(),
            hasCycle);

        if (hasCycle) {
            LOGGER.info("PEX graph cycle vertices={}", new CycleDetector<>(pexGraph1).findCycles());
        }
        return hasCycle;
    }

    private static double l1Norm(DMatrixSparseCSC m) {
        double sum = 0.0;
        for (int k = 0; k < m.nz_length; k++) {
            sum += Math.abs(m.nz_values[k]);
        }
        return sum;
    }

    private static DMatrixSparseCSC computeTransferMatrix(int matrixSize, boolean hasCycle, DMatrixSparseCSC distributionMatrix) {
        LOGGER.debug("Computing approximate matrix inversion using Neumann series");

        int maxIteration = matrixSize;
        if (hasCycle && matrixSize < MAX_ITERATION) {
            maxIteration = MAX_ITERATION;
            LOGGER.warn("Graph has some cycles. Increasing maximum number of iterations to {}", maxIteration);
        }
        // Workspaces to reduce allocations inside EJML sparse ops
        IGrowArray gw = new IGrowArray();
        DGrowArray gx = new DGrowArray();

        DMatrixSparseCSC transfer = CommonOps_DSCC.identity(matrixSize);

        DMatrixSparseCSC stack = distributionMatrix.copy();
        double initialStackL1Norm = l1Norm(stack);

        DMatrixSparseCSC nextTransfer = new DMatrixSparseCSC(matrixSize, matrixSize, transfer.nz_length + stack.nz_length);
        DMatrixSparseCSC nextStack = new DMatrixSparseCSC(matrixSize, matrixSize, stack.nz_length);

        int i = 0;
        while (i <= maxIteration) {
            nextTransfer.reshape(matrixSize, matrixSize, transfer.nz_length + stack.nz_length);
            CommonOps_DSCC.add(1.0, transfer, 1.0, stack, nextTransfer, gw, gx);
            CommonOps_DSCC.removeZeros(nextTransfer, transfer, DROP_TOLERANCE);

            nextStack.reshape(matrixSize, matrixSize, stack.nz_length); // capacity hint
            CommonOps_DSCC.mult(stack, distributionMatrix, nextStack, gw, gx);
            CommonOps_DSCC.removeZeros(nextStack, stack, DROP_TOLERANCE);

            double stackL1Norm = l1Norm(stack);
            LOGGER.debug(String.format("Iteration %s/%s: relative L1 norm of stack matrix is %.10f%% (nnz=%d)", i, maxIteration, 100 * stackL1Norm / initialStackL1Norm, stack.nz_length));

            if (stackL1Norm / initialStackL1Norm < L1_NORM_RELATIVE_TOLERANCE) {
                LOGGER.debug("Stack matrix is close enough to zero, stopping iterations");
                break;
            }

            if (i == maxIteration) {
                LOGGER.warn("Maximum number of iterations reached, matrix inversion may not be accurate");
            }
            i++;
        }
        return transfer;
    }

    private void fillDistributionTripletsWithVertex(PexGraphVertex vertex, DMatrixSparseTriplet distributionTriplet) {
        assert distributionTriplet != null;

        double sumOfLeavingAndAbsorbedFlows = vertex.getAssociatedLoad() + Math.min(vertex.getAssociatedLoad(), vertex.getAssociatedGeneration()) +
            pexGraph.outgoingEdgesOf(vertex).stream().mapToDouble(PexGraphEdge::getAssociatedFlow).sum();
        double transferredFlow = Math.min(vertex.getAssociatedLoad(), vertex.getAssociatedGeneration());

        distributionTriplet.unsafe_set(
            vertexMapper.get(vertex),
            vertexMapper.get(vertex),
            Math.abs(sumOfLeavingAndAbsorbedFlows) < EPSILON ? 0 : transferredFlow / sumOfLeavingAndAbsorbedFlows
        );
    }

    private void fillDistributionTripletsWithEdge(PexGraphEdge edge, DMatrixSparseTriplet distributionTriplet) {
        assert distributionTriplet != null;

        //Analyse edge target
        PexGraphVertex sourceVertex = pexGraph.getEdgeSource(edge);
        PexGraphVertex targetVertex = pexGraph.getEdgeTarget(edge);

        double sumOfLeavingAndAbsorbedFlows = targetVertex.getAssociatedLoad() + Math.min(targetVertex.getAssociatedLoad(), targetVertex.getAssociatedGeneration()) +
            pexGraph.outgoingEdgesOf(targetVertex).stream().mapToDouble(PexGraphEdge::getAssociatedFlow).sum();
        double transferredFlow = edge.getAssociatedFlow();

        double oldValue = distributionTriplet.get(vertexMapper.get(sourceVertex), vertexMapper.get(targetVertex));
        double increase = Math.abs(sumOfLeavingAndAbsorbedFlows) < EPSILON ? 0 : transferredFlow / sumOfLeavingAndAbsorbedFlows;
        double newValue = oldValue + increase;

        distributionTriplet.set(
            vertexMapper.get(sourceVertex),
            vertexMapper.get(targetVertex),
            newValue
        );
    }

    private double getGenerationCoeff(PexGraphVertex vertex) {
        double sumOfLeavingAndAbsorbedFlows = vertex.getAssociatedLoad() + Math.min(vertex.getAssociatedLoad(), vertex.getAssociatedGeneration()) +
            pexGraph.outgoingEdgesOf(vertex).stream().mapToDouble(PexGraphEdge::getAssociatedFlow).sum();
        return Math.abs(sumOfLeavingAndAbsorbedFlows) < EPSILON ? 0 : (vertex.getAssociatedGeneration()) / sumOfLeavingAndAbsorbedFlows;
    }

    public DMatrixSparseCSC computePexMatrix() {
        int matrixSize = pexGraph.vertexSet().size();
        double estimatedSparseCoeff = 0.1;
        boolean hasCycle = determineIfGraphHasCycle(pexGraph);

        // easy to work with sparse format, but hard to do computations with
        DMatrixSparseTriplet distributionTriplet = new DMatrixSparseTriplet(matrixSize, matrixSize, (int) (matrixSize * matrixSize * estimatedSparseCoeff));
        pexGraph.edgeSet().forEach(edge -> fillDistributionTripletsWithEdge(edge, distributionTriplet));
        pexGraph.vertexSet().forEach(vertex -> fillDistributionTripletsWithVertex(vertex, distributionTriplet));

        // convert into a format that's easier to perform math with
        DMatrixSparseCSC distributionMatrix = DConvertMatrixStruct.convert(distributionTriplet, (DMatrixSparseCSC) null);
        CommonOps_DSCC.removeZeros(distributionMatrix, DROP_TOLERANCE);

        // Initialize transfer matrix
        DMatrixSparseCSC transferMatrix = computeTransferMatrix(matrixSize, hasCycle, distributionMatrix);

        // Compute power injection matrix
        double[] generationCoeffs = new double[matrixSize];
        vertexMapper.forEach((key, value) -> generationCoeffs[value] = getGenerationCoeff(key));
        DMatrixSparseCSC generationCoeffMatrix = CommonOps_DSCC.diag(generationCoeffs);

        double[] loadCoeffs = new double[matrixSize];
        vertexMapper.forEach((key, value) -> loadCoeffs[value] = key.getAssociatedLoad());
        DMatrixSparseCSC loadCoeffMatrix = CommonOps_DSCC.diag(loadCoeffs);

        // Compute PEX matrix
        DMatrixSparseCSC pexMatrix = transferMatrix;
        CommonOps_DSCC.multRowsCols(generationCoeffs, 0, pexMatrix, loadCoeffs, 0);

        return pexMatrix;
    }

    public Map<String, Integer> getVertexIdMapper() {
        return vertexIdMapper;
    }
}
