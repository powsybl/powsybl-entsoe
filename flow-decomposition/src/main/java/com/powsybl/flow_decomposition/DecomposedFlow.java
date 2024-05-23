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
    private final String branchId;
    private final String contingencyId;
    private final Country country1;
    private final Country country2;
    private final double acReferenceFlow;
    private final double acMaxFlow;
    private final double dcReferenceFlow;
    private final double allocatedFlow;
    private final double xNodeFlow;
    private final double pstFlow;
    private final double internalFlow;
    private final Map<String, Double> loopFlowsMap = new TreeMap<>();
    static final double NO_FLOW = 0.;
    static final String AC_REFERENCE_FLOW_COLUMN_NAME = "Reference AC Flow";
    static final String DC_REFERENCE_FLOW_COLUMN_NAME = "Reference DC Flow";
    static final String ALLOCATED_COLUMN_NAME = "Allocated Flow";
    static final String XNODE_COLUMN_NAME = "Xnode Flow";
    static final String PST_COLUMN_NAME = "PST Flow";
    static final String INTERNAL_COLUMN_NAME = "Internal Flow";

    protected DecomposedFlow(DecomposedFlowBuilder builder) {
        this.branchId = builder.branchId;
        this.contingencyId = builder.contingencyId;
        this.country1 = builder.country1;
        this.country2 = builder.country2;
        this.acReferenceFlow = builder.acReferenceFlow;
        this.acMaxFlow = builder.acMaxFlow;
        this.dcReferenceFlow = builder.dcReferenceFlow;
        this.allocatedFlow = builder.allocatedFlow;
        this.xNodeFlow = builder.xNodeFlow;
        this.pstFlow = builder.pstFlow;
        this.internalFlow = builder.internalFlow;
        this.loopFlowsMap.putAll(builder.loopFlowsMap);
    }

    public String getBranchId() {
        return branchId;
    }

    public String getContingencyId() {
        return contingencyId;
    }

    public String getId() {
        return NetworkUtil.getXnecId(contingencyId, branchId);
    }

    public Country getCountry1() {
        return country1;
    }

    public Country getCountry2() {
        return country2;
    }

    public double getAcReferenceFlow() {
        return acReferenceFlow;
    }

    public double getAcMaxFlow() {
        return acMaxFlow;
    }

    public double getDcReferenceFlow() {
        return dcReferenceFlow;
    }

    public double getAllocatedFlow() {
        return allocatedFlow;
    }

    public double getXNodeFlow() {
        return xNodeFlow;
    }

    public double getPstFlow() {
        return pstFlow;
    }

    public double getInternalFlow() {
        return internalFlow;
    }

    public double getLoopFlow(Country country) {
        return getLoopFlow(NetworkUtil.getLoopFlowIdFromCountry(country));
    }

    public double getLoopFlow(String country) {
        return loopFlowsMap.getOrDefault(country, NO_FLOW);
    }

    public Map<String, Double> getLoopFlows() {
        return Collections.unmodifiableMap(loopFlowsMap);
    }

    private double getTotalLoopFlow() {
        return loopFlowsMap.values().stream().reduce(0., Double::sum);
    }

    public double getTotalFlow() {
        return getAllocatedFlow() + getXNodeFlow() + getPstFlow() + getInternalFlow() + getTotalLoopFlow();
    }

    @Override
    public String toString() {
        return String.format("branchId: %s, contingencyId: %s, decomposition: %s", branchId, contingencyId, getAllKeyMap());
    }

    private TreeMap<String, Double> getAllKeyMap() {
        TreeMap<String, Double> localDecomposedFlowMap = new TreeMap<>();
        localDecomposedFlowMap.put(AC_REFERENCE_FLOW_COLUMN_NAME, getAcReferenceFlow());
        localDecomposedFlowMap.put(DC_REFERENCE_FLOW_COLUMN_NAME, getDcReferenceFlow());
        localDecomposedFlowMap.put(ALLOCATED_COLUMN_NAME, getAllocatedFlow());
        localDecomposedFlowMap.put(XNODE_COLUMN_NAME, getXNodeFlow());
        localDecomposedFlowMap.put(PST_COLUMN_NAME, getPstFlow());
        localDecomposedFlowMap.put(INTERNAL_COLUMN_NAME, getInternalFlow());
        localDecomposedFlowMap.putAll(loopFlowsMap);
        return localDecomposedFlowMap;
    }
}
