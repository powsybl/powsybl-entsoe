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
    private final double acTerminal1ReferenceFlow;
    private final double acTerminal2ReferenceFlow;
    private final double dcReferenceFlow;
    private final double allocatedFlow;
    private final double xNodeFlow;
    private final double pstFlow;
    private final double internalFlow;
    private final Map<String, Double> loopFlowsMap = new TreeMap<>();
    static final double NO_FLOW = 0.;
    static final String AC_REFERENCE_FLOW_1_COLUMN_NAME = "Reference AC Flow 1";
    static final String AC_REFERENCE_FLOW_2_COLUMN_NAME = "Reference AC Flow 2";
    static final String DC_REFERENCE_FLOW_COLUMN_NAME = "Reference DC Flow";
    static final String ALLOCATED_COLUMN_NAME = "Allocated Flow";
    static final String XNODE_COLUMN_NAME = "Xnode Flow";
    static final String PST_COLUMN_NAME = "PST Flow";
    static final String INTERNAL_COLUMN_NAME = "Internal Flow";

    protected DecomposedFlow(DecomposedFlowBuilder builder) {
        this.branchId = Objects.requireNonNull(builder.branchId);
        this.contingencyId = Objects.requireNonNull(builder.contingencyId);
        this.country1 = Objects.requireNonNull(builder.country1);
        this.country2 = Objects.requireNonNull(builder.country2);
        this.acTerminal1ReferenceFlow = builder.acTerminal1ReferenceFlow;
        this.acTerminal2ReferenceFlow = builder.acTerminal2ReferenceFlow;
        this.dcReferenceFlow = builder.dcReferenceFlow;
        this.allocatedFlow = builder.allocatedFlow;
        this.xNodeFlow = builder.xNodeFlow;
        this.pstFlow = builder.pstFlow;
        this.internalFlow = builder.internalFlow;
        this.loopFlowsMap.putAll(Objects.requireNonNull(builder.loopFlowsMap));
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

    public double getAcTerminal1ReferenceFlow() {
        return acTerminal1ReferenceFlow;
    }

    public double getAcTerminal2ReferenceFlow() {
        return acTerminal2ReferenceFlow;
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

    public double getMaxAbsAcFlow() {
        return Math.max(Math.abs(acTerminal1ReferenceFlow), Math.abs(acTerminal2ReferenceFlow));
    }

    @Override
    public String toString() {
        return String.format("branchId: %s, contingencyId: %s, decomposition: %s", branchId, contingencyId, getAllKeyMap());
    }

    private TreeMap<String, Double> getAllKeyMap() {
        TreeMap<String, Double> localDecomposedFlowMap = new TreeMap<>();
        localDecomposedFlowMap.put(AC_REFERENCE_FLOW_1_COLUMN_NAME, getAcTerminal1ReferenceFlow());
        localDecomposedFlowMap.put(AC_REFERENCE_FLOW_2_COLUMN_NAME, getAcTerminal2ReferenceFlow());
        localDecomposedFlowMap.put(DC_REFERENCE_FLOW_COLUMN_NAME, getDcReferenceFlow());
        localDecomposedFlowMap.put(ALLOCATED_COLUMN_NAME, getAllocatedFlow());
        localDecomposedFlowMap.put(XNODE_COLUMN_NAME, getXNodeFlow());
        localDecomposedFlowMap.put(PST_COLUMN_NAME, getPstFlow());
        localDecomposedFlowMap.put(INTERNAL_COLUMN_NAME, getInternalFlow());
        localDecomposedFlowMap.putAll(loopFlowsMap);
        return localDecomposedFlowMap;
    }
}
