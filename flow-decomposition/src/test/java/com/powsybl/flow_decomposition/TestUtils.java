/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.flow_decomposition;

import com.powsybl.glsk.api.GlskDocument;
import com.powsybl.glsk.api.io.GlskDocumentImporters;
import com.powsybl.iidm.network.Bus;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.Terminal;

import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * This class contains helper functions for tests.
 *
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 */
public final class TestUtils {
    private static final double EPSILON = 1e-6;

    private TestUtils() {
    }

    public static Network importNetwork(String networkResourcePath) {
        String networkName = Paths.get(networkResourcePath).getFileName().toString();
        return Network.read(networkName, TestUtils.class.getResourceAsStream(networkResourcePath));
    }

    public static GlskDocument importGlskDocument(String glskFileName) {
        String glskName = Paths.get(glskFileName).getFileName().toString();
        return GlskDocumentImporters.importGlsk(TestUtils.class.getResourceAsStream(glskName));
    }

    public static void assertCoherenceTotalFlow(FlowDecompositionParameters.RescaleMode rescaleMode, FlowDecompositionResults flowDecompositionResults) {
        for (String xnec : flowDecompositionResults.getDecomposedFlowMap().keySet()) {
            DecomposedFlow decomposedFlow = flowDecompositionResults.getDecomposedFlowMap().get(xnec);
            switch (rescaleMode) {
                case ACER_METHODOLOGY -> assertEquals(Math.abs(decomposedFlow.getAcTerminal1ReferenceFlow()), decomposedFlow.getTotalFlow(), EPSILON);
                case PROPORTIONAL -> assertEquals(decomposedFlow.getMaxAbsAcFlow(), decomposedFlow.getTotalFlow(), EPSILON);
                default -> assertEquals(Math.abs(decomposedFlow.getDcReferenceFlow()), decomposedFlow.getTotalFlow(), EPSILON);
            }
        }
    }

    static double getLossOnBus(Network network, Bus bus) {
        return network.getBranchStream()
            .filter(branch -> terminalIsSendingPowerToBus(branch.getTerminal1(), bus) || terminalIsSendingPowerToBus(branch.getTerminal2(), bus))
            .mapToDouble(branch -> branch.getTerminal1().getP() + branch.getTerminal2().getP())
            .sum();
    }

    private static boolean terminalIsSendingPowerToBus(Terminal terminal, Bus bus) {
        return terminal.getBusBreakerView().getBus() == bus && terminal.getP() > 0;
    }
}
