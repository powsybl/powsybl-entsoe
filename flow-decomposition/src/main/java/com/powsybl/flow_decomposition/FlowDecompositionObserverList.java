package com.powsybl.flow_decomposition;

import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FlowDecompositionObserverList {
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

    public void runStart() {
        for (FlowDecompositionObserver o : observers) {
            o.runStart();
        }
    }

    public void runDone() {
        for (FlowDecompositionObserver o : observers) {
            o.runDone();
        }
    }

    public void computingBaseCase() {
        for (FlowDecompositionObserver o : observers) {
            o.computingBaseCase();
        }
    }

    public void computingContingency(String contingencyId) {
        for (FlowDecompositionObserver o : observers) {
            o.computingContingency(contingencyId);
        }
    }

    public void computedGlsk(Map<Country, Map<String, Double>> glsks) {
        for (FlowDecompositionObserver o : observers) {
            o.computedGlsk(glsks);
        }
    }

    public void computedNetPositions(Map<Country, Double> netPositions) {
        for (FlowDecompositionObserver o : observers) {
            o.computedNetPositions(netPositions);
        }
    }

    public void computedNodalInjectionsMatrix(SparseMatrixWithIndexesTriplet matrix) {
        sendMatrix(FlowDecompositionObserver::computedNodalInjectionsMatrix, matrix);
    }

    public void computedPtdfMatrix(SparseMatrixWithIndexesTriplet matrix) {
        sendMatrix(FlowDecompositionObserver::computedPtdfMatrix, matrix);
    }

    public void computedPsdfMatrix(SparseMatrixWithIndexesTriplet matrix) {
        sendMatrix(FlowDecompositionObserver::computedPsdfMatrix, matrix);
    }

    public void computedAcFlows(Network network, NetworkMatrixIndexes networkMatrixIndexes, boolean fallbackHasBeenActivated) {
        if (observers.isEmpty()) {
            return;
        }

        ReferenceNodalInjectionComputer referenceNodalInjectionComputer = new ReferenceNodalInjectionComputer();
        Map<String, Double> results = referenceNodalInjectionComputer.run(networkMatrixIndexes.getNodeList());

        for (FlowDecompositionObserver o : observers) {
            o.computedAcFlows(results, fallbackHasBeenActivated);
        }
    }

    public void computedDcFlows(Network network, NetworkMatrixIndexes networkMatrixIndexes) {
        if (observers.isEmpty()) {
            return;
        }

        ReferenceNodalInjectionComputer referenceNodalInjectionComputer = new ReferenceNodalInjectionComputer();
        Map<String, Double> results = referenceNodalInjectionComputer.run(networkMatrixIndexes.getNodeList());

        for (FlowDecompositionObserver o : observers) {
            o.computedDcFlows(results);
        }
    }

    private void sendMatrix(MatrixNotification notification, SparseMatrixWithIndexesTriplet matrix) {
        if (observers.isEmpty()) {
            return;
        }

        Map<String, Map<String, Double>> mapMatrix = matrix.toMap();
        for (FlowDecompositionObserver o : observers) {
            notification.sendMatrix(o, mapMatrix);
        }
    }

    @FunctionalInterface
    private interface MatrixNotification {
        public void sendMatrix(FlowDecompositionObserver o, Map<String, Map<String, Double>> matrix);
    }
}
