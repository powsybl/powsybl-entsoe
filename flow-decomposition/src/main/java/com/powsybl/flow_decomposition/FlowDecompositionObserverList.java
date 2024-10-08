/*
 * Copyright (c) 2024, Coreso SA (https://www.coreso.eu/) and TSCNET Services GmbH (https://www.tscnet.eu/)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.flow_decomposition;

import com.powsybl.flow_decomposition.LoadFlowRunningService.Result;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.TwoSides;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Guillaume Verger {@literal <guillaume.verger at artelys.com>}
 */
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

    public void computedAcFlows(Network network, Result loadFlowServiceAcResult) {
        if (observers.isEmpty()) {
            return;
        }

        for (FlowDecompositionObserver o : observers) {
            o.computedAcFlowsTerminal1(FlowComputerUtils.calculateAcTerminalReferenceFlows(network.getBranchStream().toList(), loadFlowServiceAcResult, TwoSides.ONE));
            o.computedAcFlowsTerminal2(FlowComputerUtils.calculateAcTerminalReferenceFlows(network.getBranchStream().toList(), loadFlowServiceAcResult, TwoSides.TWO));
        }
    }

    public void computedDcFlows(Network network) {
        if (observers.isEmpty()) {
            return;
        }

        for (FlowDecompositionObserver o : observers) {
            o.computedDcFlows(FlowComputerUtils.getTerminalReferenceFlow(network.getBranchStream().toList(), TwoSides.ONE));
        }
    }

    public void computedAcCurrents(Network network, Result loadFlowServiceAcResult) {
        if (observers.isEmpty()) {
            return;
        }

        for (FlowDecompositionObserver o : observers) {
            o.computedAcCurrentsTerminal1(FlowComputerUtils.calculateAcTerminalCurrents(network.getBranchStream().toList(), loadFlowServiceAcResult, TwoSides.ONE));
            o.computedAcCurrentsTerminal2(FlowComputerUtils.calculateAcTerminalCurrents(network.getBranchStream().toList(), loadFlowServiceAcResult, TwoSides.TWO));
        }
    }

    public void computedAcNodalInjections(Network network, boolean fallbackHasBeenActivated) {
        if (observers.isEmpty()) {
            return;
        }

        Map<String, Double> results = new ReferenceNodalInjectionComputer().run(NetworkUtil.getNodeList(network));

        for (FlowDecompositionObserver o : observers) {
            o.computedAcNodalInjections(results, fallbackHasBeenActivated);
        }
    }

    public void computedDcNodalInjections(Network network) {
        if (observers.isEmpty()) {
            return;
        }

        Map<String, Double> results = new ReferenceNodalInjectionComputer().run(NetworkUtil.getNodeList(network));

        for (FlowDecompositionObserver o : observers) {
            o.computedDcNodalInjections(results);
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
        void sendMatrix(FlowDecompositionObserver o, Map<String, Map<String, Double>> matrix);
    }
}
