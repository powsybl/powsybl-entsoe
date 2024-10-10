/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.flow_decomposition;

import com.powsybl.commons.PowsyblException;
import com.powsybl.flow_decomposition.xnec_provider.XnecProviderByIds;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowParameters;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 */
class LoadFlowFallbackTests {

    public static final String FALLBACK_MESSAGE = "AC loadflow divergence without fallback procedure enabled.";

    @Test
    void testIntegrationOfDisabledFallbackOnNetworkThatDoesNotConvergeInAc() {
        String networkFileName = "NETWORK_LOOP_FLOW_WITH_COUNTRIES.uct";
        Network network = TestUtils.importNetwork(networkFileName);
        FlowDecompositionParameters flowDecompositionParameters = new FlowDecompositionParameters()
            .setDcFallbackEnabledAfterAcDivergence(FlowDecompositionParameters.DISABLE_DC_FALLBACK_AFTER_AC_DIVERGENCE);
        FlowDecompositionComputer flowComputer = new FlowDecompositionComputer(flowDecompositionParameters);
        XnecProvider xnecProvider = XnecProviderByIds.builder().addNetworkElementsOnBasecase(Set.of("UNUSED")).build();
        Executable flowComputerExecutable = () -> flowComputer.run(xnecProvider, network);
        Exception exception = assertThrows(PowsyblException.class, flowComputerExecutable, FALLBACK_MESSAGE);
        assertEquals(FALLBACK_MESSAGE, exception.getMessage());
    }

    @Test
    void testLoadFlowServiceWhenLoadFlowConvergeInACWithFallbackActivated() {
        String networkFileName = "NETWORK_SINGLE_LOAD_TWO_GENERATORS_WITH_COUNTRIES.uct";
        Network network = TestUtils.importNetwork(networkFileName);
        LoadFlowRunningService loadFlowRunningService = new LoadFlowRunningService(LoadFlow.find());
        LoadFlowRunningService.Result loadFlowResult = loadFlowRunningService.runAcLoadflow(
            network, new LoadFlowParameters(), LoadFlowRunningService.FALLBACK_HAS_BEEN_ACTIVATED);
        assertTrue(loadFlowResult.getLoadFlowResult().isFullyConverged());
        assertFalse(loadFlowResult.fallbackHasBeenActivated());
    }

    @Test
    void testLoadFlowServiceWhenLoadFlowConvergeInACWithoutFallbackActivated() {
        String networkFileName = "NETWORK_SINGLE_LOAD_TWO_GENERATORS_WITH_COUNTRIES.uct";
        Network network = TestUtils.importNetwork(networkFileName);
        LoadFlowRunningService loadFlowRunningService = new LoadFlowRunningService(LoadFlow.find());
        LoadFlowRunningService.Result loadFlowResult = loadFlowRunningService.runAcLoadflow(
            network, new LoadFlowParameters(), LoadFlowRunningService.FALLBACK_HAS_NOT_BEEN_ACTIVATED);
        assertTrue(loadFlowResult.getLoadFlowResult().isFullyConverged());
        assertFalse(loadFlowResult.fallbackHasBeenActivated());
    }

    @Test
    void testLoadFlowServiceWhenLoadFlowDoesNotConvergeInACWithFallbackActivated() {
        String networkFileName = "NETWORK_LOOP_FLOW_WITH_COUNTRIES.uct";
        Network network = TestUtils.importNetwork(networkFileName);
        LoadFlowRunningService loadFlowRunningService = new LoadFlowRunningService(LoadFlow.find());
        LoadFlowRunningService.Result loadFlowResult = loadFlowRunningService.runAcLoadflow(
            network, new LoadFlowParameters(), LoadFlowRunningService.FALLBACK_HAS_BEEN_ACTIVATED);
        assertTrue(loadFlowResult.getLoadFlowResult().isFullyConverged());
        assertTrue(loadFlowResult.fallbackHasBeenActivated());
    }

    @Test
    void testLoadFlowServiceWhenLoadFlowDoesNotConvergeInACWithoutFallback() {
        String networkFileName = "NETWORK_LOOP_FLOW_WITH_COUNTRIES.uct";
        Network network = TestUtils.importNetwork(networkFileName);
        LoadFlowRunningService loadFlowRunningService = new LoadFlowRunningService(LoadFlow.find());
        Executable loadFlowRunningServiceExecutable = () -> loadFlowRunningService.runAcLoadflow(
            network, new LoadFlowParameters(), LoadFlowRunningService.FALLBACK_HAS_NOT_BEEN_ACTIVATED);
        Exception exception = assertThrows(PowsyblException.class, loadFlowRunningServiceExecutable, FALLBACK_MESSAGE);
        assertEquals(FALLBACK_MESSAGE, exception.getMessage());
    }
}
