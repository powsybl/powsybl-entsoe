/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.flow_decomposition;

import com.powsybl.iidm.network.Country;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 * @author Caio Luke {@literal <caio.luke at artelys.com>}
 */
public class DecomposedFlow {
    private final String branchId;
    private final String contingencyId;
    private final Country country1;
    private final Country country2;
    private final double acTerminal1ReferenceFlow;
    private final double acTerminal2ReferenceFlow;
    private final double dcReferenceFlow;
    private final double acTerminal1Current;
    private final double acTerminal2Current;
    private final FlowPartition flowPartition;
    public static final double NO_FLOW = 0.;
    public static final String AC_REFERENCE_FLOW_1_COLUMN_NAME = "Reference AC Flow 1";
    public static final String AC_REFERENCE_FLOW_2_COLUMN_NAME = "Reference AC Flow 2";
    public static final String DC_REFERENCE_FLOW_COLUMN_NAME = "Reference DC Flow";
    public static final String ALLOCATED_COLUMN_NAME = "Allocated Flow";
    public static final String XNODE_COLUMN_NAME = "Xnode Flow";
    public static final String PST_COLUMN_NAME = "PST Flow";
    public static final String INTERNAL_COLUMN_NAME = "Internal Flow";

    protected DecomposedFlow(DecomposedFlowBuilder builder) {
        this.branchId = Objects.requireNonNull(builder.branchId);
        this.contingencyId = Objects.requireNonNull(builder.contingencyId);
        this.country1 = Objects.requireNonNull(builder.country1);
        this.country2 = Objects.requireNonNull(builder.country2);
        this.acTerminal1ReferenceFlow = builder.acTerminal1ReferenceFlow;
        this.acTerminal2ReferenceFlow = builder.acTerminal2ReferenceFlow;
        this.dcReferenceFlow = builder.dcReferenceFlow;
        this.acTerminal1Current = builder.acCurrentTerminal1;
        this.acTerminal2Current = builder.acCurrentTerminal2;
        this.flowPartition = builder.flowPartition;
    }

    public String getBranchId() {
        return branchId;
    }

    public String getContingencyId() {
        return contingencyId;
    }

    public String getId() {
        return getXnecId(contingencyId, branchId);
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
        return flowPartition.allocatedFlow();
    }

    public double getXNodeFlow() {
        return flowPartition.xNodeFlow();
    }

    public double getPstFlow() {
        return flowPartition.pstFlow();
    }

    public double getInternalFlow() {
        return flowPartition.internalFlow();
    }

    public double getLoopFlow(Country country) {
        return flowPartition.loopFlowPerCountry().getOrDefault(country, NO_FLOW);
    }

    public Map<Country, Double> getLoopFlows() {
        return Collections.unmodifiableMap(flowPartition.loopFlowPerCountry());
    }

    private double getTotalLoopFlow() {
        return getLoopFlows().values().stream().reduce(0., Double::sum);
    }

    public double getTotalFlow() {
        return getAllocatedFlow() + getXNodeFlow() + getPstFlow() + getInternalFlow() + getTotalLoopFlow();
    }

    public double getMaxAbsAcFlow() {
        return Math.max(Math.abs(acTerminal1ReferenceFlow), Math.abs(acTerminal2ReferenceFlow));
    }

    public double getAcTerminal1Current() {
        return acTerminal1Current;
    }

    public double getAcTerminal2Current() {
        return acTerminal2Current;
    }

    public FlowPartition getFlowPartition() {
        return flowPartition;
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
        localDecomposedFlowMap.putAll(getLoopFlows().entrySet().stream().collect(Collectors.toMap(entry -> NetworkUtil.getLoopFlowIdFromCountry(entry.getKey()), Map.Entry::getValue)));
        return localDecomposedFlowMap;
    }

    public static String getXnecId(String contingencyId, String branchId) {
        return contingencyId.isEmpty() ? branchId : String.format("%s_%s", branchId, contingencyId);
    }
}
