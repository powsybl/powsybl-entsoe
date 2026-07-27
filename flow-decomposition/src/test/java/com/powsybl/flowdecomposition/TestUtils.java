/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.flowdecomposition;

import com.powsybl.cgmes.conformity.CgmesConformity3ModifiedCatalog;
import com.powsybl.glsk.api.GlskDocument;
import com.powsybl.glsk.api.io.GlskDocumentImporters;
import com.powsybl.iidm.network.*;
import com.powsybl.iidm.network.test.SvcTestCaseFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * This class contains helper functions for tests.
 *
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 */
public final class TestUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(TestUtils.class);
    private static final double EPSILON = 1e-3;

    private TestUtils() {
        // Utility class
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
                case ACER_METHODOLOGY ->
                    assertEquals(Math.abs(decomposedFlow.getAcTerminal1ReferenceFlow()), decomposedFlow.getTotalFlow(), EPSILON);
                case PROPORTIONAL ->
                    assertEquals(decomposedFlow.getMaxAbsAcFlow(), decomposedFlow.getTotalFlow(), EPSILON);
                case MAX_CURRENT_OVERLOAD ->
                    throw new IllegalArgumentException("Rescaling method not tested by this method");
                default -> assertEqualsWithoutRescaling(xnec, decomposedFlow);
            }
        }
    }

    private static void assertEqualsWithoutRescaling(String xnec, DecomposedFlow decomposedFlow) {
        if (Double.isNaN(decomposedFlow.getDcReferenceFlow())) {
            LOGGER.error("XNEC \"{}\" is probably not connected", xnec); // TODO: should we decompose such xnecs ?
        } else if (decomposedFlow.getTotalFlow() == 0) {
            LOGGER.error("XNEC \"{}\" is outside main synchronous component", xnec); // TODO: should we decompose such xnecs ?
        } else {
            assertEquals(Math.abs(decomposedFlow.getDcReferenceFlow()), decomposedFlow.getTotalFlow(), EPSILON);
        }
    }

    public static double getLossOnBus(Network network, Bus bus) {
        return network.getBranchStream()
            .filter(branch -> terminalIsSendingPowerToBus(branch.getTerminal1(), bus) || terminalIsSendingPowerToBus(branch.getTerminal2(), bus))
            .mapToDouble(branch -> branch.getTerminal1().getP() + branch.getTerminal2().getP())
            .sum();
    }

    private static boolean terminalIsSendingPowerToBus(Terminal terminal, Bus bus) {
        return terminal.getBusBreakerView().getBus() == bus && terminal.getP() > 0;
    }

    public static Network getMicroGridNetworkWithBusBarSectionOnly() {
        Network network = Importers.importData("CGMES", CgmesConformity3ModifiedCatalog.microGridBE3BoundaryLinesSameBoundary1Disconnected().dataSource(), null);
        network.getShuntCompensator("d771118f-36e9-4115-a128-cc3d9ce3e3da").remove();
        network.getShuntCompensator("002b0a40-3957-46db-b84a-30420083558f").remove();
        network.getStaticVarCompensator("3c69652c-ff14-4550-9a87-b6fdaccbb5f4").remove();
        assertEquals(5, network.getBusbarSectionCount());
        assertEquals(0, network.getShuntCompensatorCount());
        assertEquals(0, network.getStaticVarCompensatorCount());
        return network;
    }

    public static Network getMicroGridNetworkWithShuntCompensatorOnly() {
        Network network = Importers.importData("CGMES", CgmesConformity3ModifiedCatalog.microGridBE3BoundaryLinesSameBoundary1Disconnected().dataSource(), null);
        network.getBusbarSection("5caf27ed-d2f8-458a-834a-6b3193a982e6").remove();
        network.getBusbarSection("64901aec-5a8a-4bcb-8ca7-a3ddbfcd0e6c").remove();
        network.getBusbarSection("364c9ca2-0d1d-4363-8f46-e586f8f66a8c").remove();
        network.getBusbarSection("ef45b632-3028-4afe-bc4c-a4fa323d83fe").remove();
        network.getBusbarSection("fd649fe1-bdf5-4062-98ea-bbb66f50402d").remove();
        network.getStaticVarCompensator("3c69652c-ff14-4550-9a87-b6fdaccbb5f4").remove();
        assertEquals(0, network.getBusbarSectionCount());
        assertEquals(2, network.getShuntCompensatorCount());
        assertEquals(0, network.getStaticVarCompensatorCount());
        return network;
    }

    public static Network getNetworkWithStaticVarCompensatorOnly() {
        Network network = SvcTestCaseFactory.create();
        assertEquals(0, network.getBusbarSectionCount());
        assertEquals(0, network.getShuntCompensatorCount());
        assertEquals(1, network.getStaticVarCompensatorCount());
        return network;
    }

    public static void assertNoInternalFlowOnTieLines(FlowDecompositionResults flowDecompositionResults) {
        List<DecomposedFlow> decomposedFlowsWithInternalIssues = flowDecompositionResults.getDecomposedFlowMap().values().stream()
            .filter(decomposedFlow -> decomposedFlow.getCountry1() != decomposedFlow.getCountry2())
            .filter(decomposedFlow -> decomposedFlow.getInternalFlow() != 0)
            .toList();
        if (!decomposedFlowsWithInternalIssues.isEmpty()) {
            LOGGER.error("Found {} flows with internal flow on tie lines", decomposedFlowsWithInternalIssues.size());
            decomposedFlowsWithInternalIssues.forEach(decomposedFlow -> LOGGER.error("Flow {}: Internal flow = {}", decomposedFlow.getId(), decomposedFlow.getInternalFlow()));
        }
        assertEquals(0, decomposedFlowsWithInternalIssues.size());
    }
}
