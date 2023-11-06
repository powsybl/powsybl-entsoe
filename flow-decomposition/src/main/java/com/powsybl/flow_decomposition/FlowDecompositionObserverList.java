package com.powsybl.flow_decomposition;

import com.powsybl.iidm.network.Country;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FlowDecompositionObserverList implements FlowDecompositionObserver {
    private final List<FlowDecompositionObserver> observers;

    public FlowDecompositionObserverList() {
        this.observers = new ArrayList<>();
    }

    public void addObserver(FlowDecompositionObserver o) {
        this.observers.add(o);
    }

    public void removeObserver(FlowDecompositionObserver o) {
        this.observers.remove(o);
    }

    @Override
    public void computingBaseCase() {
        for (FlowDecompositionObserver o : observers) {
            o.computingBaseCase();
        }
    }

    @Override
    public void computingContingency(String contingencyId) {
        for (FlowDecompositionObserver o : observers) {
            o.computingContingency(contingencyId);
        }
    }

    @Override
    public void computedGlsk(Map<Country, Map<String, Double>> glsks) {
        for (FlowDecompositionObserver o : observers) {
            o.computedGlsk(glsks);
        }
    }

    @Override
    public void computedNetPositions(Map<Country, Double> netPositions) {
        for (FlowDecompositionObserver o : observers) {
            o.computedNetPositions(netPositions);
        }
    }

    @Override
    public void computedNodalInjectionsMatrix(SparseMatrixWithIndexesTriplet matrix) {
        for (FlowDecompositionObserver o : observers) {
            o.computedNodalInjectionsMatrix(matrix);
        }
    }

    @Override
    public void computedPtdfMatrix(SparseMatrixWithIndexesTriplet matrix) {
        for (FlowDecompositionObserver o : observers) {
            o.computedPtdfMatrix(matrix);
        }
    }

    @Override
    public void computedPsdfMatrix(SparseMatrixWithIndexesTriplet matrix) {
        for (FlowDecompositionObserver o : observers) {
            o.computedPsdfMatrix(matrix);
        }
    }
}
