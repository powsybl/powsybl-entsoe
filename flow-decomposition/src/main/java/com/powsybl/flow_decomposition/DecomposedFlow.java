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
    private static final double NO_FLOW = 0.;
    private final Map<String, Double> loopFlowsMap = new TreeMap<>();
    private final double allocatedFlow;
    private final double pstFlow;
    private final double acReferenceFlow;
    private final double dcReferenceFlow;
    private final Country country1;
    private final Country country2;
    private final double internalFlow;
    public static final String INTERNAL_COLUMN_NAME = "Internal Flow";
    public static final String ALLOCATED_COLUMN_NAME = "Allocated Flow";
    public static final String PST_COLUMN_NAME = "PST Flow";
    public static final String AC_REFERENCE_FLOW_COLUMN_NAME = "Reference AC Flow";
    public static final String DC_REFERENCE_FLOW_COLUMN_NAME = "Reference DC Flow";

    public DecomposedFlow(Map<String, Double> loopFlowsMap, double internalFlow, double allocatedFlow, double pstFlow, double acReferenceFlow, double dcReferenceFlow, Country country1, Country country2) {
        this.loopFlowsMap.putAll(loopFlowsMap);
        this.internalFlow = internalFlow;
        this.allocatedFlow = allocatedFlow;
        this.pstFlow = pstFlow;
        this.acReferenceFlow = acReferenceFlow;
        this.dcReferenceFlow = dcReferenceFlow;
        this.country1 = country1;
        this.country2 = country2;
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

    public double getPstFlow() {
        return pstFlow;
    }

    public double getAcReferenceFlow() {
        return acReferenceFlow;
    }

    public double getDcReferenceFlow() {
        return dcReferenceFlow;
    }

    public Country getCountry1() {
        return country1;
    }

    public Country getCountry2() {
        return country2;
    }

    public double getTotalFlow() {
        return getAllocatedFlow() + getPstFlow() + getTotalLoopFlow() + getInternalFlow();
    }

    private double getTotalLoopFlow() {
        return loopFlowsMap.values().stream().reduce(0., Double::sum);
    }

    public double getInternalFlow() {
        return internalFlow;
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
