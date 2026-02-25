/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.flow_decomposition.partitioners;

import com.powsybl.flow_decomposition.NetworkUtil;
import com.powsybl.flow_decomposition.TestUtils;
import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Bus;
import com.powsybl.iidm.network.Network;
import org.ejml.data.DMatrix;
import org.ejml.data.DMatrixSparseCSC;
import org.ejml.ops.DConvertMatrixStruct;
import org.ejml.sparse.csc.CommonOps_DSCC;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
class PexMatrixCalculatorTest {
    private static final double EPSILON = 1e-3f;

    private PexGraph pexGraph;
    private Map<String, Integer> busMapping;

    @BeforeEach
    void setUp() {
        Network testNetwork = TestUtils.importNetwork("testCase.xiidm");
        List<Bus> busesInMainSynchronousComponent = testNetwork.getBusView().getBusStream()
                .filter(Bus::isInMainSynchronousComponent)
                .toList();
        busMapping = NetworkUtil.getIndex(busesInMainSynchronousComponent.stream().map(Bus::getId).toList());
        List<Branch<?>> branchesConnectedInMainSynchronousComponent = NetworkUtil.getAllValidBranches(testNetwork);
        pexGraph = new PexGraph(busesInMainSynchronousComponent, branchesConnectedInMainSynchronousComponent);
    }

    private double relativeError(double a, double b) {
        return a == 0 ? a - b : (a - b) / a;
    }

    private void checkIsColumnSumEqualToLoad(DMatrixSparseCSC pexMatrix, int index, double expectedLoad) {
        DMatrixSparseCSC column = CommonOps_DSCC.identity(pexMatrix.numRows, 1);
        CommonOps_DSCC.extractColumn(pexMatrix, index, column);
        assertEquals(0, relativeError(expectedLoad, Math.abs(CommonOps_DSCC.elementSum(column))), EPSILON);
    }

    private void checkIsRowSumEqualToGen(DMatrixSparseCSC pexMatrix, int index, double expectedGeneration) {
        DMatrixSparseCSC row = CommonOps_DSCC.identity(1, pexMatrix.numCols);
        CommonOps_DSCC.extractRows(pexMatrix, index, index + 1, row);
        assertEquals(0, relativeError(expectedGeneration, Math.abs(CommonOps_DSCC.elementSum(row))), EPSILON);
    }

    private void checkMatrixOkForBus(DMatrixSparseCSC pexMatrix, PexGraphVertex vertex) {
        checkIsColumnSumEqualToLoad(pexMatrix, busMapping.get(vertex.getAssociatedBus().getId()), vertex.getAssociatedLoad());
        checkIsRowSumEqualToGen(pexMatrix, busMapping.get(vertex.getAssociatedBus().getId()), vertex.getAssociatedGeneration());
    }

    private void checkMatrixOk(DMatrix pexMatrix) {
        DMatrixSparseCSC matrixSparseCSC = new DMatrixSparseCSC(pexMatrix.getNumRows(), pexMatrix.getNumCols());
        DConvertMatrixStruct.convert(pexMatrix, matrixSparseCSC);
        pexGraph.vertexSet().forEach(vertex -> checkMatrixOkForBus(matrixSparseCSC, vertex));
    }

    @Test
    void computePexMatrix() {
        PexMatrixCalculator calculator = new PexMatrixCalculator(pexGraph, busMapping);
        DMatrix pexMatrix = calculator.computePexMatrix();
        checkMatrixOk(pexMatrix);
    }
}
