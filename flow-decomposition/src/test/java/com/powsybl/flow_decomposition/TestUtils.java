/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.flow_decomposition;

import com.powsybl.cgmes.conformity.CgmesConformity3ModifiedCatalog;
import com.powsybl.glsk.api.GlskDocument;
import com.powsybl.glsk.api.io.GlskDocumentImporters;
import com.powsybl.iidm.network.*;
import com.powsybl.iidm.network.test.SvcTestCaseFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * This class contains helper functions for tests.
 *
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 */
public final class TestUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(TestUtils.class);
    private static final double EPSILON = 1e-6;

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
                case ACER_METHODOLOGY -> assertEquals(Math.abs(decomposedFlow.getAcTerminal1ReferenceFlow()), decomposedFlow.getTotalFlow(), EPSILON);
                case PROPORTIONAL -> assertEquals(decomposedFlow.getMaxAbsAcFlow(), decomposedFlow.getTotalFlow(), EPSILON);
                default -> {
                    if (Double.isNaN(decomposedFlow.getDcReferenceFlow())) {
                        LOGGER.error("XNEC \"{}\" is probably not connected", xnec); // TODO: should we decompose such xnecs ?
                    } else if (decomposedFlow.getTotalFlow() == 0) {
                        LOGGER.error("XNEC \"{}\" is outside main synchronous component", xnec); // TODO: should we decompose such xnecs ?
                    } else {
                        assertEquals(Math.abs(decomposedFlow.getDcReferenceFlow()), Math.abs(decomposedFlow.getTotalFlow()), EPSILON);
                    }
                }
            }
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
        Network network = Importers.importData("CGMES", CgmesConformity3ModifiedCatalog.microGridBE3DanglingLinesSameBoundary1Disconnected().dataSource(), null);
        network.getShuntCompensator("d771118f-36e9-4115-a128-cc3d9ce3e3da").remove();
        network.getShuntCompensator("002b0a40-3957-46db-b84a-30420083558f").remove();
        assertEquals(5, network.getBusbarSectionCount());
        assertEquals(0, network.getShuntCompensatorCount());
        assertEquals(0, network.getStaticVarCompensatorCount());
        return network;
    }

    public static Network getMicroGridNetworkWithShuntCompensatorOnly() {
        Network network = Importers.importData("CGMES", CgmesConformity3ModifiedCatalog.microGridBE3DanglingLinesSameBoundary1Disconnected().dataSource(), null);
        network.getBusbarSection("5caf27ed-d2f8-458a-834a-6b3193a982e6").remove();
        network.getBusbarSection("64901aec-5a8a-4bcb-8ca7-a3ddbfcd0e6c").remove();
        network.getBusbarSection("364c9ca2-0d1d-4363-8f46-e586f8f66a8c").remove();
        network.getBusbarSection("ef45b632-3028-4afe-bc4c-a4fa323d83fe").remove();
        network.getBusbarSection("fd649fe1-bdf5-4062-98ea-bbb66f50402d").remove();
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

    public static void validateFlowDecomposition(FlowDecompositionResults flowDecompositionResults,
                                          String id,
                                          String branchId,
                                          String contingencyId,
                                          Country country1,
                                          Country country2,
                                          double acReferenceFlow,
                                          double dcReferenceFlow,
                                          double allocatedFlow,
                                          double xNodeFlow,
                                          double pstFlow,
                                          double internalFlow,
                                          double loopFlowBe,
                                          double loopFlowDe,
                                          double loopFLowFr,
                                          double loopFlowNl) {
        DecomposedFlow l1 = flowDecompositionResults.getDecomposedFlowMap().get(id);
        assertNotNull(l1);
        assertEquals(id, l1.getId());
        assertEquals(branchId, l1.getBranchId());
        assertEquals(contingencyId, l1.getContingencyId());
        assertEquals(country1, l1.getCountry1());
        assertEquals(country2, l1.getCountry2());
        assertEquals(acReferenceFlow, l1.getAcReferenceFlow(), EPSILON);
        assertEquals(dcReferenceFlow, l1.getDcReferenceFlow(), EPSILON);
        assertEquals(allocatedFlow, l1.getAllocatedFlow(), EPSILON);
        assertEquals(xNodeFlow, l1.getXNodeFlow(), EPSILON);
        assertEquals(pstFlow, l1.getPstFlow(), EPSILON);
        assertEquals(internalFlow, l1.getInternalFlow(), EPSILON);
        assertEquals(loopFlowBe, l1.getLoopFlow(Country.BE), EPSILON);
        assertEquals(loopFlowDe, l1.getLoopFlow(Country.DE), EPSILON);
        assertEquals(loopFLowFr, l1.getLoopFlow(Country.FR), EPSILON);
        assertEquals(loopFlowNl, l1.getLoopFlow(Country.NL), EPSILON);
    }
}
