/*
 * Copyright (c) 2024, Coreso SA (https://www.coreso.eu/) and TSCNET Services GmbH (https://www.tscnet.eu/)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.flow_decomposition;

import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Guillaume Verger {@literal <guillaume.verger at artelys.com>}
 * @author Caio Luke {@literal <caio.luke at artelys.com>}
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

    public void addObserversFrom(FlowDecompositionObserverList otherList) {
        for (FlowDecompositionObserver o : otherList.getObservers()) {
            this.addObserver(o);
        }
    }

    public List<FlowDecompositionObserver> getObservers() {
        return this.observers;
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

    public void computedNodalInjectionsMatrix(Map<String, Map<String, Double>> matrix) {
        for (FlowDecompositionObserver o : observers) {
            o.computedNodalInjectionsMatrix(matrix);
        }
    }

    public void computedPtdfMatrix(Map<String, Map<String, Double>> matrix) {
        for (FlowDecompositionObserver o : observers) {
            o.computedPtdfMatrix(matrix);
        }
    }

    public void computedPsdfMatrix(Map<String, Map<String, Double>> matrix) {
        for (FlowDecompositionObserver o : observers) {
            o.computedPsdfMatrix(matrix);
        }
    }

    public void computedAcLoadFlowResults(Network network, LoadFlowRunningService.Result loadFlowServiceAcResult) {
        for (FlowDecompositionObserver o : observers) {
            o.computedAcLoadFlowResults(network, loadFlowServiceAcResult.getLoadFlowResult(), loadFlowServiceAcResult.fallbackHasBeenActivated());
        }
    }

    public void computedDcLoadFlowResults(Network network, LoadFlowRunningService.Result loadFlowServiceDcResult) {
        for (FlowDecompositionObserver o : observers) {
            o.computedDcLoadFlowResults(network, loadFlowServiceDcResult.getLoadFlowResult());
        }
    }

    public void computedPreRescalingDecomposedFlows(DecomposedFlow decomposedFlow) {
        for (FlowDecompositionObserver o : observers) {
            o.computedPreRescalingDecomposedFlows(decomposedFlow);
        }
    }

    @FunctionalInterface
    private interface MatrixNotification {
        void sendMatrix(FlowDecompositionObserver o, Map<String, Map<String, Double>> matrix);
    }
}
