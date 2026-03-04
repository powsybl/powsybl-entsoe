/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.flow_decomposition.partitioners;

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

    Map<String, Map<String, Double>> toMap() {
        Map<Integer, String> colIndexInverse = inverseIndex(colIndex);
        Map<Integer, String> rowIndexInverse = inverseIndex(rowIndex);
        Map<String, Map<String, Double>> result = new TreeMap<>();
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

    SparseMatrixWithIndexesCSC transpose() {
        DMatrixSparseCSC transposedMatrix = new DMatrixSparseCSC(cscMatrix.numCols, cscMatrix.numRows, cscMatrix.nz_length);
        CommonOps_DSCC.transpose(cscMatrix, transposedMatrix, null);
        return new SparseMatrixWithIndexesCSC(colIndex, rowIndex, transposedMatrix);
    }

    SparseMatrixWithIndexesCSC getColumn(String colId) {
        int colIndex = this.colIndex.get(colId);
        DMatrixSparseCSC colMatrix = new DMatrixSparseCSC(cscMatrix.numRows, 1);
        CommonOps_DSCC.extractColumn(cscMatrix, colIndex, colMatrix);
        return new SparseMatrixWithIndexesCSC(rowIndex, Map.of(colId, 0), colMatrix);
    }

    public SparseMatrixWithIndexesCSC removeZerosInplace(double zeroTolerance) {
        CommonOps_DSCC.removeZeros(cscMatrix, zeroTolerance);
        return this;
    }

    double[] getColumnAsArray(String colId) {
        int col = this.colIndex.get(colId);
        double[] out = new double[cscMatrix.numRows];
        int start = cscMatrix.col_idx[col];
        int end   = cscMatrix.col_idx[col + 1];
        for (int i = start; i < end; i++) {
            int row = cscMatrix.nz_rows[i];
            out[row] = cscMatrix.nz_values[i];
        }
        return out;
    }

    public Double get(String rowId, String colId) {
        return cscMatrix.get(rowIndex.get(rowId), colIndex.get(colId));
    }
}
