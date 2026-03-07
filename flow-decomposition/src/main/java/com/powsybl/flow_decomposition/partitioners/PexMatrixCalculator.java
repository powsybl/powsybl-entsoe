/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.flow_decomposition.partitioners;

import com.powsybl.flow_decomposition.NetworkUtil;
import org.ejml.data.DMatrixSparseCSC;
import org.ejml.data.DMatrixSparseTriplet;
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

    // Precomputed per-vertex data (big speedup: avoids outgoingEdgesOf(...).stream() in hot loops)
    private final double[] outgoingFlowSums;
    private final double[] loadCoeffs;
    private final double[] associatedGenerations;

    public PexMatrixCalculator(PexGraph pexGraph) {
        this.pexGraph = Objects.requireNonNull(pexGraph);
        this.vertexIdMapper = NetworkUtil.getIndex(pexGraph.vertexSet().stream().map(PexGraphVertex::getId).toList());

        pexGraph.vertexSet().forEach(vertex -> vertexMapper.put(vertex, Objects.requireNonNull(vertexIdMapper.get(vertex.getId()))));

        int matrixSize = pexGraph.vertexSet().size();
        this.outgoingFlowSums = new double[matrixSize];
        this.loadCoeffs = new double[matrixSize];
        this.associatedGenerations = new double[matrixSize];

        // Fill loads/generations once
        for (PexGraphVertex v : pexGraph.vertexSet()) {
            int idx = vertexMapper.get(v);
            loadCoeffs[idx] = v.getAssociatedLoad();
            associatedGenerations[idx] = v.getAssociatedGeneration();
        }

        // Sum outgoing flows in one pass over edges
        for (PexGraphEdge e : pexGraph.edgeSet()) {
            PexGraphVertex src = pexGraph.getEdgeSource(e);
            int srcIdx = vertexMapper.get(src);
            outgoingFlowSums[srcIdx] += e.getAssociatedFlow();
        }
    }

    private static boolean determineIfGraphHasCycle(PexGraph pexGraph1) {
        CycleDetector<PexGraphVertex, PexGraphEdge> detector = new CycleDetector<>(pexGraph1);
        boolean hasCycle = detector.detectCycles();
        LOGGER.debug("PEX graph: vertices={}, edges={}, hasDirectedCycle={}",
            pexGraph1.vertexSet().size(),
            pexGraph1.edgeSet().size(),
            hasCycle);

        //if (hasCycle) {
        //    LOGGER.debug("PEX graph cycle vertices={}", detector.findCycles());
        //}
        return hasCycle;
    }

    private static double l1Norm(DMatrixSparseCSC m) {
        double sum = 0.0;
        for (int k = 0; k < m.nz_length; k++) {
            sum += Math.abs(m.nz_values[k]);
        }
        return sum;
    }

    private static DMatrixSparseCSC computePexMatrixWithNeumann(int matrixSize, boolean hasCycle, DMatrixSparseCSC distributionMatrix, double[] generationCoeffs, double[] loadCoeffs) {
        LOGGER.debug("Computing approximate matrix inversion using Neumann series. Matrix size={}", matrixSize);

        int maxIteration = matrixSize;
        if (hasCycle && matrixSize < MAX_ITERATION) {
            maxIteration = MAX_ITERATION;
            LOGGER.debug("Graph has some cycles. Increasing maximum number of iterations to {}", maxIteration);
        }

        DMatrixSparseCSC transfer = CommonOps_DSCC.diag(generationCoeffs);
        CommonOps_DSCC.multColumns(transfer, loadCoeffs, 0);
        DMatrixSparseCSC stack = distributionMatrix.copy();
        CommonOps_DSCC.multRows(generationCoeffs, 0, stack);
        CommonOps_DSCC.removeZeros(stack, DROP_TOLERANCE);

        DMatrixSparseCSC neumannCoefficient = stack.copy();
        CommonOps_DSCC.multColumns(neumannCoefficient, loadCoeffs, 0);
        CommonOps_DSCC.removeZeros(neumannCoefficient, DROP_TOLERANCE);

        double initialStackL1Norm = l1Norm(stack);

        DMatrixSparseCSC nextTransfer = new DMatrixSparseCSC(transfer);
        DMatrixSparseCSC nextStack = new DMatrixSparseCSC(stack);
        DMatrixSparseCSC tmp;

        int i = 0;
        while (i <= maxIteration) {
            CommonOps_DSCC.add(1.0, transfer, 1.0, neumannCoefficient, nextTransfer, null, null);
            CommonOps_DSCC.removeZeros(nextTransfer, DROP_TOLERANCE);

            CommonOps_DSCC.mult(stack, distributionMatrix, nextStack);
            CommonOps_DSCC.removeZeros(nextStack, DROP_TOLERANCE);

            neumannCoefficient = nextStack.copy();
            CommonOps_DSCC.multColumns(neumannCoefficient, loadCoeffs, 0);
            CommonOps_DSCC.removeZeros(neumannCoefficient, DROP_TOLERANCE);

            tmp = transfer;
            transfer = nextTransfer;
            nextTransfer = tmp;

            tmp = stack;
            stack = nextStack;
            nextStack = tmp;

            // Use the current stack (post-swap). Also avoid String.format overhead.
            double stackL1Norm = l1Norm(stack);
            //if (LOGGER.isDebugEnabled()) {
            //    LOGGER.debug(
            //        "Iteration {}/{}: relative L1 norm of stack matrix is {}% (stack nnz={}, transfer nnz={}, ith neumann nnz={}, sparse pex matrix {}%)",
            //        i, maxIteration, 100.0 * stackL1Norm / initialStackL1Norm, stack.nz_length, transfer.nz_length, neumannCoefficient.nz_length, 100 * (double) transfer.nz_length / (transfer.numRows * transfer.numCols)
            //    );
            //}

            if (stackL1Norm / initialStackL1Norm < L1_NORM_RELATIVE_TOLERANCE) {
                LOGGER.debug("Stack matrix is close enough to zero, stopping iterations");
                break;
            }

            if (i == maxIteration) {
                LOGGER.debug("Maximum number of iterations reached, matrix inversion may not be accurate");
            }
            i++;
        }
        LOGGER.debug("Completed {} iterations for PEX matrix calculation (numRows {}, nnz {}, sparse {}%)", i, transfer.numRows, transfer.nz_length, 100 * (double) transfer.nz_length / (transfer.numRows * transfer.numCols));
        return transfer;
    }

    private void fillDistributionTripletsWithVertex(PexGraphVertex vertex, DMatrixSparseTriplet distributionTriplet) {
        assert distributionTriplet != null;

        int vIdx = vertexMapper.get(vertex);
        double load = loadCoeffs[vIdx];
        double gen = associatedGenerations[vIdx];
        double transferredFlow = Math.min(load, gen);

        double sumOfLeavingAndAbsorbedFlows = load + transferredFlow + outgoingFlowSums[vIdx];

        distributionTriplet.unsafe_set(
            vIdx,
            vIdx,
            Math.abs(sumOfLeavingAndAbsorbedFlows) < EPSILON ? 0 : transferredFlow / sumOfLeavingAndAbsorbedFlows
        );
    }

    private void fillDistributionTripletsWithEdge(PexGraphEdge edge, DMatrixSparseTriplet distributionTriplet) {
        assert distributionTriplet != null;

        PexGraphVertex sourceVertex = pexGraph.getEdgeSource(edge);
        PexGraphVertex targetVertex = pexGraph.getEdgeTarget(edge);

        int sIdx = vertexMapper.get(sourceVertex);
        int tIdx = vertexMapper.get(targetVertex);

        double targetLoad = loadCoeffs[tIdx];
        double targetGen = associatedGenerations[tIdx];
        double targetTransferredFlow = Math.min(targetLoad, targetGen);

        double sumOfLeavingAndAbsorbedFlows = targetLoad + targetTransferredFlow + outgoingFlowSums[tIdx];
        double increase = Math.abs(sumOfLeavingAndAbsorbedFlows) < EPSILON ? 0 : edge.getAssociatedFlow() / sumOfLeavingAndAbsorbedFlows;

        double oldValue = distributionTriplet.get(sIdx, tIdx);
        distributionTriplet.set(sIdx, tIdx, oldValue + increase);
    }

    private double getGenerationCoeff(PexGraphVertex vertex) {
        int vIdx = vertexMapper.get(vertex);
        double load = loadCoeffs[vIdx];
        double gen = associatedGenerations[vIdx];
        double transferredFlow = Math.min(load, gen);

        double sumOfLeavingAndAbsorbedFlows = load + transferredFlow + outgoingFlowSums[vIdx];
        return Math.abs(sumOfLeavingAndAbsorbedFlows) < EPSILON ? 0 : gen / sumOfLeavingAndAbsorbedFlows;
    }

    public DMatrixSparseCSC computePexMatrix() {
        int matrixSize = pexGraph.vertexSet().size();
        boolean hasCycle = determineIfGraphHasCycle(pexGraph);

        int initialNnz = pexGraph.edgeSet().size() + pexGraph.vertexSet().size();
        DMatrixSparseTriplet distributionTriplet = new DMatrixSparseTriplet(matrixSize, matrixSize, initialNnz);

        pexGraph.edgeSet().forEach(edge -> fillDistributionTripletsWithEdge(edge, distributionTriplet));
        pexGraph.vertexSet().forEach(vertex -> fillDistributionTripletsWithVertex(vertex, distributionTriplet));

        DMatrixSparseCSC distributionMatrix = DConvertMatrixStruct.convert(distributionTriplet, (DMatrixSparseCSC) null);
        CommonOps_DSCC.removeZeros(distributionMatrix, DROP_TOLERANCE);

        double[] generationCoeffs = new double[matrixSize];
        vertexMapper.forEach((key, value) -> generationCoeffs[value] = getGenerationCoeff(key));

        double[] loadCoeffs = new double[matrixSize];
        vertexMapper.forEach((key, value) -> loadCoeffs[value] = this.loadCoeffs[value]);

        return computePexMatrixWithNeumann(matrixSize, hasCycle, distributionMatrix, generationCoeffs, loadCoeffs);
    }

    public Map<String, Integer> getVertexIdMapper() {
        return vertexIdMapper;
    }
}
