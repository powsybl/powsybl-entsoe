/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.flow_decomposition.partitioners;

import org.ejml.data.DMatrix;
import org.ejml.data.DMatrixRMaj;
import org.ejml.data.DMatrixSparseCSC;
import org.ejml.data.DMatrixSparseTriplet;
import org.ejml.ops.DConvertMatrixStruct;
import org.ejml.sparse.csc.CommonOps_DSCC;
import org.ejml.sparse.csc.MatrixFeatures_DSCC;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Object dedicated to PEX matrix calculation
 *
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
public class PexMatrixCalculator {
    private static final double EPSILON = 1e-5;
    private static final double MATRIX_CONSTRUCTION_TOLERANCE = 0;
    private final PexGraph pexGraph;
    private final Map<PexGraphVertex, Integer> vertexMapper = new HashMap<>();

    public PexMatrixCalculator(PexGraph pexGraph, Map<String, Integer> busMapper) {
        this.pexGraph = Objects.requireNonNull(pexGraph);
        Objects.requireNonNull(busMapper);

        pexGraph.vertexSet().forEach(vertex -> vertexMapper.put(vertex, busMapper.get(vertex.getAssociatedBus().getId())));
    }

    private void fillDistributionTripletsWithVertex(PexGraphVertex vertex, DMatrixSparseTriplet distributionTriplet) {
        assert distributionTriplet != null;

        double sumOfLeavingAndAbsorbedFlows = vertex.getAssociatedLoad() + Math.min(vertex.getAssociatedLoad(), vertex.getAssociatedGeneration()) +
                pexGraph.outgoingEdgesOf(vertex).stream().mapToDouble(PexGraphEdge::getAssociatedFlow).sum();
        double transferedFlow = Math.min(vertex.getAssociatedLoad(), vertex.getAssociatedGeneration());

        distributionTriplet.unsafe_set(
                vertexMapper.get(vertex),
                vertexMapper.get(vertex),
                Math.abs(sumOfLeavingAndAbsorbedFlows) < EPSILON ? 0 : transferedFlow / sumOfLeavingAndAbsorbedFlows
        );
    }

    private void fillDistributionTripletsWithEdge(PexGraphEdge edge, DMatrixSparseTriplet distributionTriplet) {
        assert distributionTriplet != null;

        //Analyse edge target
        PexGraphVertex sourceVertex = pexGraph.getEdgeSource(edge);
        PexGraphVertex targetVertex = pexGraph.getEdgeTarget(edge);

        double sumOfLeavingAndAbsorbedFlows = targetVertex.getAssociatedLoad() + Math.min(targetVertex.getAssociatedLoad(), targetVertex.getAssociatedGeneration()) +
                pexGraph.outgoingEdgesOf(targetVertex).stream().mapToDouble(PexGraphEdge::getAssociatedFlow).sum();
        double transferedFlow = edge.getAssociatedFlow();

        double oldValue = distributionTriplet.get(vertexMapper.get(sourceVertex), vertexMapper.get(targetVertex));
        double increase = Math.abs(sumOfLeavingAndAbsorbedFlows) < EPSILON ? 0 : transferedFlow / sumOfLeavingAndAbsorbedFlows;
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
        return Math.abs(sumOfLeavingAndAbsorbedFlows) < EPSILON ? 0 : vertex.getAssociatedGeneration() / sumOfLeavingAndAbsorbedFlows;
    }

    public DMatrix computePexMatrix() {
        int matrixSize = pexGraph.vertexSet().size();
        double estimatedSparseCoeff = 0.1;

        // easy to work with sparse format, but hard to do computations with
        DMatrixSparseTriplet distributionTriplet = new DMatrixSparseTriplet(matrixSize, matrixSize, (int) (matrixSize * matrixSize * estimatedSparseCoeff));
        pexGraph.edgeSet().forEach(edge -> fillDistributionTripletsWithEdge(edge, distributionTriplet));
        pexGraph.vertexSet().forEach(vertex -> fillDistributionTripletsWithVertex(vertex, distributionTriplet));

        // convert into a format that's easier to perform math with
        DMatrixSparseCSC distributionMatrix = DConvertMatrixStruct.convert(distributionTriplet, (DMatrixSparseCSC) null);

        // Initialize transfer matrix
        DMatrixSparseCSC transferMatrix = CommonOps_DSCC.identity(matrixSize);
        DMatrixSparseCSC stackMatrix = distributionMatrix.copy();
        for (int i = 0; i < matrixSize; i++) {
            CommonOps_DSCC.add(1., transferMatrix.copy(), 1., stackMatrix, transferMatrix, null, null);
            CommonOps_DSCC.mult(stackMatrix.copy(), distributionMatrix, stackMatrix);
            if (MatrixFeatures_DSCC.isZeros(stackMatrix, MATRIX_CONSTRUCTION_TOLERANCE)) {
                // PW: stop when matrix rank reached to speed up computation
                break;
            }
        }

        // Compute power injection matrix
        double[] generationCoeffs = new double[matrixSize];
        vertexMapper.forEach((key, value) -> generationCoeffs[value] = getGenerationCoeff(key));
        DMatrixSparseCSC generationCoeffMatrix = CommonOps_DSCC.diag(generationCoeffs);

        double[] loadCoeffs = new double[matrixSize];
        vertexMapper.forEach((key, value) -> loadCoeffs[value] = key.getAssociatedLoad());
        DMatrixSparseCSC loadCoeffMatrix = CommonOps_DSCC.diag(loadCoeffs);

        // Compute PEX matrix
        DMatrixSparseCSC pexMatrix = transferMatrix;
        CommonOps_DSCC.mult(generationCoeffMatrix, pexMatrix.copy(), pexMatrix);
        CommonOps_DSCC.mult(pexMatrix.copy(), loadCoeffMatrix, pexMatrix);

        return DConvertMatrixStruct.convert(pexMatrix, (DMatrixRMaj) null);
    }
}
