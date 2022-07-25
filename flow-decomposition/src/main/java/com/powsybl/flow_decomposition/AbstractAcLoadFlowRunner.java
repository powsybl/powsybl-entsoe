/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.flow_decomposition;

import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlowParameters;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
abstract class AbstractAcLoadFlowRunner<T> {
    private static final boolean AC_LOAD_FLOW = false;
    protected final LoadFlowParameters loadFlowParameters;

    protected AbstractAcLoadFlowRunner(LoadFlowParameters initialLoadFlowParameters) {
        this.loadFlowParameters = enforceAcLoadFlowCalculation(initialLoadFlowParameters);
    }

    protected LoadFlowParameters enforceAcLoadFlowCalculation(LoadFlowParameters initialLoadFlowParameters) {
        LoadFlowParameters acEnforcedParameters = LoadFlowParameters.load(); // WARNING we want to copy but there is a bug with Graal VM ! initialLoadFlowParameters.copy();
        acEnforcedParameters.setDc(AC_LOAD_FLOW);
        return acEnforcedParameters;
    }

    abstract T run(Network network);
}
