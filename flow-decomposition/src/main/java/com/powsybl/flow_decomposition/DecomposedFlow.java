/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.flow_decomposition;

import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Country;

import java.util.*;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 */
public class DecomposedFlow {
    static final String INTERNAL_COLUMN_NAME = "Internal Flow";
    static final String ALLOCATED_COLUMN_NAME = "Allocated Flow";
    static final String PST_COLUMN_NAME = "PST Flow";
    static final String AC_REFERENCE_FLOW_COLUMN_NAME = "Reference AC Flow";
    static final String DC_REFERENCE_FLOW_COLUMN_NAME = "Reference DC Flow";
    private static final double NO_FLOW = 0.;
    private final Branch branch;
    private final Country country1;
    private final Country country2;
    private final Map<String, Double> loopFlowsMap = new TreeMap<>();
    private double internalFlow;
    private double allocatedFlow;
    private double pstFlow;
    private double acReferenceFlow;
    private double dcReferenceFlow;

    protected DecomposedFlow(Branch branch, Map<String, Double> loopFlowsMap, double internalFlow, double allocatedFlow, double pstFlow, double acReferenceFlow, double dcReferenceFlow, Country country1, Country country2) {
        this.branch = branch;
        this.country1 = country1;
        this.country2 = country2;
        this.loopFlowsMap.putAll(loopFlowsMap);
        this.internalFlow = internalFlow;
        this.allocatedFlow = allocatedFlow;
        this.pstFlow = pstFlow;
        this.acReferenceFlow = acReferenceFlow;
        this.dcReferenceFlow = dcReferenceFlow;
    }

    DecomposedFlow(Branch branch) {
        this(branch, Collections.emptyMap(), NO_FLOW, NO_FLOW, NO_FLOW, NO_FLOW, NO_FLOW,
            NetworkUtil.getTerminalCountry(branch.getTerminal1()),
            NetworkUtil.getTerminalCountry(branch.getTerminal2()));
    }

    public Branch getBranch() {
        return branch;
    }

    public Country getCountry1() {
        return country1;
    }

    public Country getCountry2() {
        return country2;
    }

    public String getId() {
        return branch.getId();
    }

    public boolean isInternalBranch() {
        return Objects.equals(country1, country2);
    }

    public double getAllocatedFlow() {
        return allocatedFlow;
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

    public double getInternalFlow() {
        return internalFlow;
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
        return getAllocatedFlow() + getPstFlow() + getTotalLoopFlow() + getInternalFlow();
    }

    private double getTotalLoopFlow() {
        return loopFlowsMap.values().stream().reduce(0., Double::sum);
    }

    void setLoopFlow(Map<String, Double> loopFlowsMap) {
        this.loopFlowsMap.putAll(loopFlowsMap);
    }

    public void setInternalFlow(double internalFlow) {
        this.internalFlow = internalFlow;
    }

    public void setAllocatedFlow(double allocatedFlow) {
        this.allocatedFlow = allocatedFlow;
    }

    public void setPstFlow(double pstFlow) {
        this.pstFlow = pstFlow;
    }

    public void setAcReferenceFlow(double acReferenceFlow) {
        this.acReferenceFlow = acReferenceFlow;
    }

    public void setDcReferenceFlow(double dcReferenceFlow) {
        this.dcReferenceFlow = dcReferenceFlow;
    }

    @Override
    public String toString() {
        return getAllKeyMap().toString();
    }

    private TreeMap<String, Double> getAllKeyMap() {
        TreeMap<String, Double> localDecomposedFlowMap = new TreeMap<>(loopFlowsMap);
        localDecomposedFlowMap.put(INTERNAL_COLUMN_NAME, getInternalFlow());
        localDecomposedFlowMap.put(ALLOCATED_COLUMN_NAME, getAllocatedFlow());
        localDecomposedFlowMap.put(PST_COLUMN_NAME, getPstFlow());
        localDecomposedFlowMap.put(AC_REFERENCE_FLOW_COLUMN_NAME, getAcReferenceFlow());
        localDecomposedFlowMap.put(DC_REFERENCE_FLOW_COLUMN_NAME, getDcReferenceFlow());
        return localDecomposedFlowMap;
    }
}
