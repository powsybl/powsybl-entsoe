/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.flow_decomposition.computers.network_modificators;

import com.powsybl.commons.PowsyblException;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.loadflow.LoadFlowResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
public class LoadFlowRunningService {
    private static final boolean AC_LOAD_FLOW = false;
    private static final boolean DC_LOAD_FLOW = true;
    private static final Logger LOGGER = LoggerFactory.getLogger(LoadFlowRunningService.class);
    public static final boolean FALLBACK_HAS_BEEN_ACTIVATED = true;
    public static final boolean FALLBACK_HAS_NOT_BEEN_ACTIVATED = false;
    protected final LoadFlow.Runner runner;

    public LoadFlowRunningService(LoadFlow.Runner runner) {
        this.runner = runner;
    }

    public Result runAcLoadflow(Network network, LoadFlowParameters loadFlowParameters, boolean isDcFallbackEnabledAfterAcDivergence) {
        LoadFlowParameters acEnforcedParameters = enforceAcLoadFlowCalculation(loadFlowParameters);
        LoadFlowResult acLoadFlowResult = runner.run(network, acEnforcedParameters);
        if (!acLoadFlowResult.isOk() && isDcFallbackEnabledAfterAcDivergence) {
            LOGGER.warn("AC loadflow divergence. Running DC loadflow as fallback procedure.");
            return runDcLoadflow(network, loadFlowParameters)
                .setFallbackHasBeenActivated(FALLBACK_HAS_BEEN_ACTIVATED);
        }
        if (!acLoadFlowResult.isOk()) {
            throw new PowsyblException("AC loadfow divergence without fallback procedure enabled.");
        }
        return new Result(acLoadFlowResult, FALLBACK_HAS_NOT_BEEN_ACTIVATED);
    }

    public Result runDcLoadflow(Network network, LoadFlowParameters loadFlowParameters) {
        LoadFlowParameters dcEnforcedParameters = enforceDcLoadFlowCalculation(loadFlowParameters);
        LoadFlowResult dcLoadFlowResult = runner.run(network, dcEnforcedParameters);
        if (!dcLoadFlowResult.isOk()) {
            throw new PowsyblException("DC loadfow divergence.");
        }
        return new Result(dcLoadFlowResult, FALLBACK_HAS_NOT_BEEN_ACTIVATED);
    }

    public static class Result {
        private final LoadFlowResult loadFlowResult;
        private boolean fallbackHasBeenActivated;

        public Result(LoadFlowResult loadFlowResult, boolean fallbackHasBeenActivated) {
            this.loadFlowResult = loadFlowResult;
            this.fallbackHasBeenActivated = fallbackHasBeenActivated;
        }

        public LoadFlowResult getLoadFlowResult() {
            return loadFlowResult;
        }

        public boolean fallbackHasBeenActivated() {
            return fallbackHasBeenActivated;
        }

        public Result setFallbackHasBeenActivated(boolean fallbackHasBeenActivated) {
            this.fallbackHasBeenActivated = fallbackHasBeenActivated;
            return this;
        }
    }

    private LoadFlowParameters enforceAcLoadFlowCalculation(LoadFlowParameters initialLoadFlowParameters) {
        LoadFlowParameters acEnforcedParameters = initialLoadFlowParameters.copy();
        acEnforcedParameters.setDc(AC_LOAD_FLOW);
        return acEnforcedParameters;
    }

    private LoadFlowParameters enforceDcLoadFlowCalculation(LoadFlowParameters initialLoadFlowParameters) {
        LoadFlowParameters dcEnforcedParameters = initialLoadFlowParameters.copy();
        dcEnforcedParameters.setDc(DC_LOAD_FLOW);
        return dcEnforcedParameters;
    }
}
