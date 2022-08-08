/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.flow_decomposition;

import com.powsybl.iidm.network.Country;

import java.util.*;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 */
public class DecomposedFlow {
    public static final double DEFAULT_FLOW = 0.;
    private final Xnec xnec;
    private final Map<String, Double> loopFlowsMap = new TreeMap<>();
    private double allocatedFlow;
    private double pstFlow;
    private double acReferenceFlow;
    private double dcReferenceFlow;
    static final String ALLOCATED_COLUMN_NAME = "Allocated Flow";
    static final String PST_COLUMN_NAME = "PST Flow";
    static final String AC_REFERENCE_FLOW_COLUMN_NAME = "Reference AC Flow";
    static final String DC_REFERENCE_FLOW_COLUMN_NAME = "Reference DC Flow";

    protected DecomposedFlow(Xnec xnec, Map<String, Double> loopFlowsMap, double allocatedFlow, double pstFlow, double acReferenceFlow, double dcReferenceFlow) {
        this.xnec = xnec;
        this.loopFlowsMap.putAll(loopFlowsMap);
        this.allocatedFlow = allocatedFlow;
        this.pstFlow = pstFlow;
        this.acReferenceFlow = acReferenceFlow;
        this.dcReferenceFlow = dcReferenceFlow;
    }

    protected DecomposedFlow(Xnec xnec) {
        this(xnec, Collections.emptyMap(), DEFAULT_FLOW, DEFAULT_FLOW, DEFAULT_FLOW, DEFAULT_FLOW);
    }

    @Override
    public String toString() {
        return getAllKeyMap().toString();
    }

    public Xnec getXnec() {
        return xnec;
    }

    public double getAllocatedFlow() {
        return allocatedFlow;
    }

    public double getLoopFlow(Country country) {
        return getLoopFlow(NetworkUtil.getLoopFlowIdFromCountry(country));
    }

    public Map<String, Double> getLoopFlows() {
        return Collections.unmodifiableMap(loopFlowsMap);
    }

    public double getPstFlow() {
        return pstFlow;
    }

    public double getAcReferenceFlow() {
        return acReferenceFlow;
    }

    public double getDcReferenceFlow() {
        return dcReferenceFlow;
    }

    public double getTotalFlow() {
        return getAllocatedFlow() + getPstFlow() + getTotalLoopFlow();
    }

    public double getTotalLoopFlow() {
        return loopFlowsMap.values().stream().reduce(0., Double::sum);
    }

    public double getInternalFlow() {
        if (xnec.isInternalBranch()) {
            return getLoopFlow(xnec.getCountryTerminal1());
        }
        return 0.0;
    }

    void setAcReferenceFlow(double acReferenceFlow) {
        this.acReferenceFlow = acReferenceFlow;
    }

    void setDcReferenceFlow(double dcReferenceFlow) {
        this.dcReferenceFlow = dcReferenceFlow;
    }

    void setAllocatedFlow(double allocatedFlow) {
        this.allocatedFlow = allocatedFlow;
    }

    void setLoopFlow(Map<String, Double> loopFlowsMap) {
        this.loopFlowsMap.putAll(loopFlowsMap);
    }

    void setPstFlow(double pstFlow) {
        this.pstFlow = pstFlow;
    }

    double getLoopFlow(String country) {
        return loopFlowsMap.getOrDefault(country, DEFAULT_FLOW);
    }

    private TreeMap<String, Double> getAllKeyMap() {
        TreeMap<String, Double> localDecomposedFlowMap = new TreeMap<>(loopFlowsMap);
        localDecomposedFlowMap.put(ALLOCATED_COLUMN_NAME, getAllocatedFlow());
        localDecomposedFlowMap.put(PST_COLUMN_NAME, getPstFlow());
        localDecomposedFlowMap.put(AC_REFERENCE_FLOW_COLUMN_NAME, getAcReferenceFlow());
        localDecomposedFlowMap.put(DC_REFERENCE_FLOW_COLUMN_NAME, getDcReferenceFlow());
        return localDecomposedFlowMap;
    }
}
