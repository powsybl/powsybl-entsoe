/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.flowdecomposition;

import com.powsybl.iidm.network.Country;

import java.util.EnumMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Fluent builder for asserting flow decomposition results in tests.
 *
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 */
public final class DecomposedFlowAssert {
    private static final double EPSILON = 1e-3;

    private final FlowDecompositionResults flowDecompositionResults;
    private String branchId;
    private String contingencyId = "";
    private Country country1;
    private Country country2;
    private double acReferenceFlow = 0.0;
    private double dcReferenceFlow = 0.0;
    private double allocatedFlow = 0.0;
    private double xNodeFlow = 0.0;
    private double pstFlow = 0.0;
    private double internalFlow = 0.0;
    private final Map<Country, Double> loopFlows = new EnumMap<>(Country.class);

    private DecomposedFlowAssert(FlowDecompositionResults flowDecompositionResults) {
        this.flowDecompositionResults = flowDecompositionResults;
    }

    public static DecomposedFlowAssert assertThat(FlowDecompositionResults flowDecompositionResults) {
        return new DecomposedFlowAssert(flowDecompositionResults);
    }

    public static DecomposedFlowAssert newAssert() {
        return new DecomposedFlowAssert(null);
    }

    public DecomposedFlowAssert branchId(String branchId) {
        this.branchId = branchId;
        return this;
    }

    public DecomposedFlowAssert contingencyId(String contingencyId) {
        this.contingencyId = contingencyId;
        return this;
    }

    public DecomposedFlowAssert country1(Country country1) {
        this.country1 = country1;
        return this;
    }

    public DecomposedFlowAssert country2(Country country2) {
        this.country2 = country2;
        return this;
    }

    public DecomposedFlowAssert acReferenceFlow(double acReferenceFlow) {
        this.acReferenceFlow = acReferenceFlow;
        return this;
    }

    public DecomposedFlowAssert dcReferenceFlow(double dcReferenceFlow) {
        this.dcReferenceFlow = dcReferenceFlow;
        return this;
    }

    public DecomposedFlowAssert allocatedFlow(double allocatedFlow) {
        this.allocatedFlow = allocatedFlow;
        return this;
    }

    public DecomposedFlowAssert xNodeFlow(double xNodeFlow) {
        this.xNodeFlow = xNodeFlow;
        return this;
    }

    public DecomposedFlowAssert pstFlow(double pstFlow) {
        this.pstFlow = pstFlow;
        return this;
    }

    public DecomposedFlowAssert internalFlow(double internalFlow) {
        this.internalFlow = internalFlow;
        return this;
    }

    public DecomposedFlowAssert loopFlow(Country country, double value) {
        this.loopFlows.put(country, value);
        return this;
    }

    public void validate() {
        validate(flowDecompositionResults);
    }

    public void validate(FlowDecompositionResults results) {
        String id = contingencyId.isEmpty() ? branchId : branchId + "_" + contingencyId;
        DecomposedFlow decomposedFlow = results.getDecomposedFlowMap().get(id);
        assertNotNull(decomposedFlow);
        assertEquals(id, decomposedFlow.getId());
        assertEquals(branchId, decomposedFlow.getBranchId());
        assertEquals(contingencyId, decomposedFlow.getContingencyId());
        assertEquals(country1, decomposedFlow.getCountry1());
        assertEquals(country2, decomposedFlow.getCountry2());
        assertEquals(acReferenceFlow, decomposedFlow.getAcTerminal1ReferenceFlow(), EPSILON);
        assertEquals(dcReferenceFlow, decomposedFlow.getDcReferenceFlow(), EPSILON);
        assertEquals(allocatedFlow, decomposedFlow.getAllocatedFlow(), EPSILON);
        assertEquals(xNodeFlow, decomposedFlow.getXNodeFlow(), EPSILON);
        assertEquals(pstFlow, decomposedFlow.getPstFlow(), EPSILON);
        assertEquals(internalFlow, decomposedFlow.getInternalFlow(), EPSILON);
        assert decomposedFlow.getLoopFlows().values().stream().filter(d -> Math.abs(d) > 1e-3).count() == loopFlows.size();
        loopFlows.forEach((country, loopFlowValue) -> assertEquals(loopFlowValue, decomposedFlow.getLoopFlow(country), EPSILON));
    }
}
