/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.flow_decomposition;

import org.ejml.data.DMatrixSparse;
import org.ejml.data.DMatrixSparseCSC;
import org.ejml.sparse.csc.CommonOps_DSCC;

import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 */
class SparseMatrixWithIndexesCSC extends AbstractSparseMatrixWithIndexes {
    private final DMatrixSparseCSC cscMatrix;

    SparseMatrixWithIndexesCSC(Map<String, Integer> rowIndex, Map<String, Integer> colIndex, DMatrixSparseCSC cscMatrix) {
        super(rowIndex, colIndex);
        this.cscMatrix = cscMatrix;
    }

    SparseMatrixWithIndexesCSC(Map<String, Integer> rowIndex, Map<String, Integer> colIndex) {
        this(rowIndex, colIndex, new DMatrixSparseCSC(rowIndex.size(), colIndex.size()));
    }

    private Map<Integer, String> inverseIndex(Map<String, Integer> index) {
        return index.entrySet()
            .stream()
            .collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));
    }

    private void getZeroMatrixAsMap(Map<String, Map<String, Double>> result) {
        for (String col : colIndex.keySet()) {
            for (String row : rowIndex.keySet()) {
                result.computeIfAbsent(row, v -> new TreeMap<>())
                    .put(col, 0.0);
            }
        }
    }

    Map<String, Map<String, Double>> toMap(boolean fillZeros) {
        Map<Integer, String> colIndexInverse = inverseIndex(colIndex);
        Map<Integer, String> rowIndexInverse = inverseIndex(rowIndex);
        Map<String, Map<String, Double>> result = new TreeMap<>();
        if (fillZeros) {
            getZeroMatrixAsMap(result);
        }
        for (Iterator<DMatrixSparse.CoordinateRealValue> iterator = cscMatrix.createCoordinateIterator(); iterator.hasNext(); ) {
            DMatrixSparse.CoordinateRealValue cell = iterator.next();
            result.computeIfAbsent(rowIndexInverse.get(cell.row), v -> new TreeMap<>())
                    .put(colIndexInverse.get(cell.col), cell.value);
        }
        return result;
    }

    static SparseMatrixWithIndexesCSC mult(SparseMatrixWithIndexesCSC matrix1, SparseMatrixWithIndexesCSC matrix2) {
        SparseMatrixWithIndexesCSC multiplicationResult = new SparseMatrixWithIndexesCSC(matrix1.rowIndex, matrix2.colIndex);
        CommonOps_DSCC.mult(matrix1.cscMatrix, matrix2.cscMatrix, multiplicationResult.cscMatrix);
        return multiplicationResult;
    }
}
