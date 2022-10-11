/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.flow_decomposition;

import com.powsybl.commons.PowsyblException;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowParameters;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 */
class LoadFlowFallbackTests {

    public static final String FALLBACK_MESSAGE = "AC loadfow divergence without fallback procedure enabled";

    @Test
    void testIntegrationOfDisabledFallbackOnNetworkThatDoesNotConvergeInAc() {
        String networkFileName = "NETWORK_LOOP_FLOW_WITH_COUNTRIES.uct";
        Network network = TestUtil.importNetwork(networkFileName);
        FlowDecompositionParameters flowDecompositionParameters = new FlowDecompositionParameters()
            .setDcFallbackEnabledAfterAcDivergence(FlowDecompositionParameters.DISABLE_DC_FALLBACK_AFTER_AC_DIVERGENCE);
        FlowDecompositionComputer flowComputer = new FlowDecompositionComputer(flowDecompositionParameters);
        Executable flowComputerExecutable = () -> flowComputer.run(network);
        assertThrows(PowsyblException.class, flowComputerExecutable, FALLBACK_MESSAGE);
    }

    @Test
    void testLoadFlowServiceWhenLoadFlowConvergeInACWithFallbackActivated() {
        String networkFileName = "NETWORK_SINGLE_LOAD_TWO_GENERATORS_WITH_COUNTRIES.uct";
        Network network = TestUtil.importNetwork(networkFileName);
        LoadFlowRunningService loadFlowRunningService = new LoadFlowRunningService(LoadFlow.find());
        LoadFlowRunningService.Result loadFlowResult = loadFlowRunningService.runAcLoadflow(
            network, new LoadFlowParameters(), LoadFlowRunningService.FALLBACK_HAS_BEEN_ACTIVATED);
        assertTrue(loadFlowResult.getLoadFlowResult().isOk());
        assertFalse(loadFlowResult.fallbackHasBeenActivated());
    }

    @Test
    void testLoadFlowServiceWhenLoadFlowConvergeInACWithoutFallbackActivated() {
        String networkFileName = "NETWORK_SINGLE_LOAD_TWO_GENERATORS_WITH_COUNTRIES.uct";
        Network network = TestUtil.importNetwork(networkFileName);
        LoadFlowRunningService loadFlowRunningService = new LoadFlowRunningService(LoadFlow.find());
        LoadFlowRunningService.Result loadFlowResult = loadFlowRunningService.runAcLoadflow(
            network, new LoadFlowParameters(), LoadFlowRunningService.FALLBACK_HAS_NOT_BEEN_ACTIVATED);
        assertTrue(loadFlowResult.getLoadFlowResult().isOk());
        assertFalse(loadFlowResult.fallbackHasBeenActivated());
    }

    @Test
    void testLoadFlowServiceWhenLoadFlowDoesNotConvergeInACWithFallbackActivated() {
        String networkFileName = "NETWORK_LOOP_FLOW_WITH_COUNTRIES.uct";
        Network network = TestUtil.importNetwork(networkFileName);
        LoadFlowRunningService loadFlowRunningService = new LoadFlowRunningService(LoadFlow.find());
        LoadFlowRunningService.Result loadFlowResult = loadFlowRunningService.runAcLoadflow(
            network, new LoadFlowParameters(), LoadFlowRunningService.FALLBACK_HAS_BEEN_ACTIVATED);
        assertTrue(loadFlowResult.getLoadFlowResult().isOk());
        assertTrue(loadFlowResult.fallbackHasBeenActivated());
    }

    @Test
    void testLoadFlowServiceWhenLoadFlowDoesNotConvergeInACWithoutFallback() {
        String networkFileName = "NETWORK_LOOP_FLOW_WITH_COUNTRIES.uct";
        Network network = TestUtil.importNetwork(networkFileName);
        LoadFlowRunningService loadFlowRunningService = new LoadFlowRunningService(LoadFlow.find());
        Executable loadFlowRunningServiceExecutable = () -> loadFlowRunningService.runAcLoadflow(
            network, new LoadFlowParameters(), LoadFlowRunningService.FALLBACK_HAS_NOT_BEEN_ACTIVATED);
        assertThrows(PowsyblException.class, loadFlowRunningServiceExecutable, FALLBACK_MESSAGE);
    }
}
