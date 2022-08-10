/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.flow_decomposition;

import com.powsybl.contingency.Contingency;
import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Country;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 */
public class XnecWithDecomposition extends Xnec {
    private final DecomposedFlow decomposedFlowBeforeRescaling;
    private DecomposedFlow decomposedFlow;

    XnecWithDecomposition(Branch branch, String variantId, Contingency contingency) {
        super(branch, variantId, contingency);
        this.decomposedFlowBeforeRescaling = new DecomposedFlow(this);
    }

    XnecWithDecomposition(Branch branch, String variantId) {
        super(branch, variantId);
        this.decomposedFlowBeforeRescaling = new DecomposedFlow(this);
    }

    public XnecWithDecomposition(XnecWithDecomposition xnecWithDecomposition) {
        super(xnecWithDecomposition.getBranch(), xnecWithDecomposition.getVariantId(), xnecWithDecomposition.getContingency());
        this.decomposedFlowBeforeRescaling = xnecWithDecomposition.getDecomposedFlowBeforeRescaling();
        this.decomposedFlow = xnecWithDecomposition.getDecomposedFlow();
    }

    public DecomposedFlow getDecomposedFlowBeforeRescaling() {
        return decomposedFlowBeforeRescaling;
    }

    public void setDecomposedFlow(DecomposedFlow decomposedFlow) {
        this.decomposedFlow = decomposedFlow;
    }

    public DecomposedFlow getDecomposedFlow() {
        return decomposedFlow;
    }

    public double getAllocatedFlow() {
        return decomposedFlow.getAllocatedFlow();
    }

    public double getLoopFlow(Country country) {
        return decomposedFlow.getLoopFlow(country);
    }

    public Map<String, Double> getLoopFlows() {
        return decomposedFlow.getLoopFlows();
    }

    public double getPstFlow() {
        return decomposedFlow.getPstFlow();
    }

    public double getAcReferenceFlow() {
        return decomposedFlow.getAcReferenceFlow();
    }

    public double getDcReferenceFlow() {
        return decomposedFlow.getDcReferenceFlow();
    }

    public double getTotalFlow() {
        return decomposedFlow.getTotalFlow();
    }

    public double getTotalLoopFlow() {
        return decomposedFlow.getTotalLoopFlow();
    }

    public double getInternalFlow() {
        return decomposedFlow.getInternalFlow();
    }

    @Override
    public String toString() {
        return toMap().toString();
    }

    private LinkedHashMap<String, String> toMap() {
        LinkedHashMap<String, String> map = new LinkedHashMap<>();
        map.put("xnec", super.toString());
        map.put("decomposed before rescaling", decomposedFlowBeforeRescaling.toString());
        map.put("decomposed after rescaling", decomposedFlow.toString());
        return map;
    }
}
