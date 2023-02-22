/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.flow_decomposition;

import com.powsybl.flow_decomposition.glsk_provider.AutoGlskProvider;
import com.powsybl.flow_decomposition.xnec_provider.XnecProviderByIds;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
class CustomGlskProviderTests {
    private static final String NETWORK_FILE_FOR_CUSTOM_GLSK_TEST = "customGlskProviderTestNetwork.uct";
    private static final String NETWORK_ELEMENT_DECOMPOSED = "FGEN2 11 FINTER11 1";
    private static final double EPSILON = 1e-3;

    @Test
    void checkThatDefaultGlskProviderIsAutoCountryGlsk() {
        FlowDecompositionComputer computer = new FlowDecompositionComputer();
        XnecProvider xnecProvider = XnecProviderByIds.builder()
                .addNetworkElementsOnBasecase(Set.of(NETWORK_ELEMENT_DECOMPOSED))
                .build();
        Network network = TestUtils.importNetwork(NETWORK_FILE_FOR_CUSTOM_GLSK_TEST);
        FlowDecompositionResults results = computer.run(xnecProvider, network);
        assertEquals(0., results.getDecomposedFlowMap().get(NETWORK_ELEMENT_DECOMPOSED).getAllocatedFlow(), EPSILON);
    }

    @Test
    void checkThatDefaultGlskProviderCanBeProvided() {
        FlowDecompositionComputer computer = new FlowDecompositionComputer();
        XnecProvider xnecProvider = XnecProviderByIds.builder()
                .addNetworkElementsOnBasecase(Set.of(NETWORK_ELEMENT_DECOMPOSED))
                .build();
        GlskProvider glskProvider = new AutoGlskProvider();
        Network network = TestUtils.importNetwork(NETWORK_FILE_FOR_CUSTOM_GLSK_TEST);
        FlowDecompositionResults results = computer.run(xnecProvider, glskProvider, network);
        assertEquals(0., results.getDecomposedFlowMap().get(NETWORK_ELEMENT_DECOMPOSED).getAllocatedFlow(), EPSILON);
    }

    @Test
    void checkThatOtherGlskProviderGivesOtherCorrectResults() {
        FlowDecompositionComputer computer = new FlowDecompositionComputer();
        XnecProvider xnecProvider = XnecProviderByIds.builder()
                .addNetworkElementsOnBasecase(Set.of(NETWORK_ELEMENT_DECOMPOSED))
                .build();
        GlskProvider glskProvider = network -> Map.of(Country.FR, Map.of("FGEN2 11_generator", 1.0),
                                                      Country.BE, Map.of("BGEN  11_generator", 1.0));
        Network network = TestUtils.importNetwork(NETWORK_FILE_FOR_CUSTOM_GLSK_TEST);
        FlowDecompositionResults results = computer.run(xnecProvider, glskProvider, network);
        assertEquals(-100., results.getDecomposedFlowMap().get(NETWORK_ELEMENT_DECOMPOSED).getAllocatedFlow(), EPSILON);
    }

    @Test
    void checkThatLskInGlskProviderGivesOtherCorrectResults() {
        FlowDecompositionComputer computer = new FlowDecompositionComputer();
        XnecProvider xnecProvider = XnecProviderByIds.builder()
                .addNetworkElementsOnBasecase(Set.of(NETWORK_ELEMENT_DECOMPOSED))
                .build();
        GlskProvider glskProvider = network -> Map.of(Country.FR, Map.of("FGEN2 11_load", 1.0),
                                                      Country.BE, Map.of("BGEN  11_generator", 1.0));
        Network network = TestUtils.importNetwork(NETWORK_FILE_FOR_CUSTOM_GLSK_TEST);
        FlowDecompositionResults results = computer.run(xnecProvider, glskProvider, network);
        assertEquals(-100., results.getDecomposedFlowMap().get(NETWORK_ELEMENT_DECOMPOSED).getAllocatedFlow(), EPSILON);
    }
}
