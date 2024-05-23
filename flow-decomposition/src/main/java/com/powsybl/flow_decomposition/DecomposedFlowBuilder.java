/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.flow_decomposition;

import com.powsybl.iidm.network.Country;

import java.util.Map;

/**
 * @author Caio Luke {@literal <caio.luke at artelys.com>}
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 */
public class DecomposedFlowBuilder {
    protected String branchId;
    protected String contingencyId;
    protected Country country1;
    protected Country country2;
    protected double acReferenceFlow;
    protected double acMaxFlow;
    protected double dcReferenceFlow;
    protected double allocatedFlow;
    protected double xNodeFlow;
    protected double pstFlow;
    protected double internalFlow;
    protected Map<String, Double> loopFlowsMap;

    public DecomposedFlowBuilder() {
        // empty constructor
    }

    public DecomposedFlowBuilder addBranchId(String branchId) {
        this.branchId = branchId;
        return this;
    }

    public DecomposedFlowBuilder addContingencyId(String contingencyId) {
        this.contingencyId = contingencyId;
        return this;
    }

    public DecomposedFlowBuilder addCountry1(Country country1) {
        this.country1 = country1;
        return this;
    }

    public DecomposedFlowBuilder addCountry2(Country country2) {
        this.country2 = country2;
        return this;
    }

    public DecomposedFlowBuilder addAcReferenceFlow(double acReferenceFlow) {
        this.acReferenceFlow = acReferenceFlow;
        return this;
    }

    public DecomposedFlowBuilder addAcMaxFlow(double acMaxFlow) {
        this.acMaxFlow = acMaxFlow;
        return this;
    }

    public DecomposedFlowBuilder addDcReferenceFlow(double dcReferenceFlow) {
        this.dcReferenceFlow = dcReferenceFlow;
        return this;
    }

    public DecomposedFlowBuilder addAllocatedFlow(double allocatedFlow) {
        this.allocatedFlow = allocatedFlow;
        return this;
    }

    public DecomposedFlowBuilder addXNodeFlow(double xNodeFlow) {
        this.xNodeFlow = xNodeFlow;
        return this;
    }

    public DecomposedFlowBuilder addPstFlow(double pstFlow) {
        this.pstFlow = pstFlow;
        return this;
    }

    public DecomposedFlowBuilder addInternalFlow(double internalFlow) {
        this.internalFlow = internalFlow;
        return this;
    }

    public DecomposedFlowBuilder addLoopFlowsMap(Map<String, Double> loopFlowsMap) {
        this.loopFlowsMap = loopFlowsMap;
        return this;
    }

    public DecomposedFlow build() {
        return new DecomposedFlow(this);
    }
}
