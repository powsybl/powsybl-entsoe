/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.flow_decomposition.computers.matrix_computer;

import org.ejml.data.DMatrixSparseCSC;
import org.ejml.data.DMatrixSparseTriplet;
import org.ejml.ops.DConvertMatrixStruct;

import java.util.Map;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 */
public class SparseMatrixWithIndexesTriplet extends AbstractSparseMatrixWithIndexes {
    private static final double NO_FILTERING_EPSILON = -1;
    private final DMatrixSparseTriplet tripletMatrix;
    private final double epsilon;

    public SparseMatrixWithIndexesTriplet(Map<String, Integer> rowIndex,
                                          Map<String, Integer> colIndex,
                                          Integer initLength,
                                          double epsilon) {
        super(rowIndex, colIndex);
        this.tripletMatrix = new DMatrixSparseTriplet(rowIndex.size(), colIndex.size(), initLength);
        this.epsilon = epsilon;
    }

    public SparseMatrixWithIndexesTriplet(Map<String, Integer> rowIndex,
                                          Map<String, Integer> colIndex,
                                          Integer initLength) {
        this(rowIndex, colIndex, initLength, NO_FILTERING_EPSILON);
    }

    public SparseMatrixWithIndexesTriplet(Map<String, Integer> rowIndex, String columnName, int size) {
        this(rowIndex, Map.of(columnName, 0), size);
    }

    private boolean isNotZero(double value) {
        return Math.abs(value) > epsilon;
    }

    public void addItem(String row, String col, double value) {
        if (!Double.isNaN(value) && isNotZero(value)) {
            tripletMatrix.addItem(rowIndex.get(row), colIndex.get(col), value);
        }
    }

    public SparseMatrixWithIndexesCSC toCSCMatrix() {
        DMatrixSparseCSC cscMatrix = DConvertMatrixStruct.convert(tripletMatrix, (DMatrixSparseCSC) null);
        return new SparseMatrixWithIndexesCSC(this.rowIndex, this.colIndex, cscMatrix);
    }

    public Map<String, Map<String, Double>> toMap(boolean fillZeros) {
        return toCSCMatrix().toMap(fillZeros);
    }
}
