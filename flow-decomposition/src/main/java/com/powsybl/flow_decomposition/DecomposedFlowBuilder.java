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
    protected double acTerminal1ReferenceFlow;
    protected double acTerminal2ReferenceFlow;
    protected double dcReferenceFlow;
    protected double allocatedFlow;
    protected double xNodeFlow;
    protected double pstFlow;
    protected double internalFlow;
    protected Map<String, Double> loopFlowsMap;
    protected double acCurrentTerminal1;
    protected double acCurrentTerminal2;

    public DecomposedFlowBuilder() {
        // empty constructor
    }

    public DecomposedFlowBuilder withBranchId(String branchId) {
        this.branchId = branchId;
        return this;
    }

    public DecomposedFlowBuilder withContingencyId(String contingencyId) {
        this.contingencyId = contingencyId;
        return this;
    }

    public DecomposedFlowBuilder withCountry1(Country country1) {
        this.country1 = country1;
        return this;
    }

    public DecomposedFlowBuilder withCountry2(Country country2) {
        this.country2 = country2;
        return this;
    }

    public DecomposedFlowBuilder withAcTerminal1ReferenceFlow(double acTerminal1ReferenceFlow) {
        this.acTerminal1ReferenceFlow = acTerminal1ReferenceFlow;
        return this;
    }

    public DecomposedFlowBuilder withAcTerminal2ReferenceFlow(double acTerminal2ReferenceFlow) {
        this.acTerminal2ReferenceFlow = acTerminal2ReferenceFlow;
        return this;
    }

    public DecomposedFlowBuilder withDcReferenceFlow(double dcReferenceFlow) {
        this.dcReferenceFlow = dcReferenceFlow;
        return this;
    }

    public DecomposedFlowBuilder withAllocatedFlow(double allocatedFlow) {
        this.allocatedFlow = allocatedFlow;
        return this;
    }

    public DecomposedFlowBuilder withXNodeFlow(double xNodeFlow) {
        this.xNodeFlow = xNodeFlow;
        return this;
    }

    public DecomposedFlowBuilder withPstFlow(double pstFlow) {
        this.pstFlow = pstFlow;
        return this;
    }

    public DecomposedFlowBuilder withInternalFlow(double internalFlow) {
        this.internalFlow = internalFlow;
        return this;
    }

    public DecomposedFlowBuilder withLoopFlowsMap(Map<String, Double> loopFlowsMap) {
        this.loopFlowsMap = loopFlowsMap;
        return this;
    }

    public DecomposedFlowBuilder withAcCurrentTerminal1(double acCurrentTerminal1) {
        this.acCurrentTerminal1 = acCurrentTerminal1;
        return this;
    }

    public DecomposedFlowBuilder withAcCurrentTerminal2(double acCurrentTerminal2) {
        this.acCurrentTerminal2 = acCurrentTerminal2;
        return this;
    }

    public DecomposedFlow build() {
        return new DecomposedFlow(this);
    }
}
