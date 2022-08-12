/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.flow_decomposition;

import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 */
class InternalFlowTests {

    public static final String NETWORK_FILE_NAME = "NETWORK_PARALLEL_LINES_PTDF.uct";
    private static final double EPSILON = 1e-3;
    public static final String VARIANT_ID = "InitialState";
    public static final String X_INTERNAL_FR_FORMAT = Xnec.createId("FGEN  11 FLOAD 11 %d", VARIANT_ID);

    public static final String X_GFR_LBE = Xnec.createId("FGEN  11 BLOAD 11 1", VARIANT_ID);
    public static final String X_LFR_LBE = Xnec.createId("FLOAD 11 BLOAD 11 1", VARIANT_ID);

    @Test
    void testNetworkWithoutInternalFlow() {
        Network network = AllocatedFlowTests.importNetwork(NETWORK_FILE_NAME);

        FlowDecompositionResults flowDecompositionResults = getFlowDecompositionResults(network);

        List<XnecWithDecomposition> xnecWithDecomposition = flowDecompositionResults.getXnecsWithDecomposition();
        assertEquals(2, xnecWithDecomposition.size());
        assertEquals(0.0, flowDecompositionResults.get(X_GFR_LBE).getInternalFlow());
        assertEquals(Country.FR, flowDecompositionResults.get(X_GFR_LBE).getCountryTerminal1());
        assertEquals(Country.BE, flowDecompositionResults.get(X_GFR_LBE).getCountryTerminal2());
        assertEquals(0.0, flowDecompositionResults.get(X_LFR_LBE).getInternalFlow());
        assertEquals(Country.FR, flowDecompositionResults.get(X_LFR_LBE).getCountryTerminal1());
        assertEquals(Country.BE, flowDecompositionResults.get(X_LFR_LBE).getCountryTerminal2());
    }

    @Test
    void testNetworkWithInternalFlow() {
        Network network = getHighPtdfNetwork();

        FlowDecompositionResults flowDecompositionResults = getFlowDecompositionResults(network);

        List<XnecWithDecomposition> xnecWithDecomposition = flowDecompositionResults.getXnecsWithDecomposition();
        assertEquals(2 + 10 - 1, xnecWithDecomposition.size());
        assertEquals(0.0, flowDecompositionResults.get(X_GFR_LBE).getInternalFlow());
        assertEquals(Country.FR, flowDecompositionResults.get(X_GFR_LBE).getCountryTerminal1());
        assertEquals(Country.BE, flowDecompositionResults.get(X_GFR_LBE).getCountryTerminal2());
        assertEquals(0.0, flowDecompositionResults.get(X_LFR_LBE).getInternalFlow());
        assertEquals(Country.FR, flowDecompositionResults.get(X_LFR_LBE).getCountryTerminal1());
        assertEquals(Country.BE, flowDecompositionResults.get(X_LFR_LBE).getCountryTerminal2());
        for (int i = 1; i < 10; i++) {
            String lineId = String.format(X_INTERNAL_FR_FORMAT, i);
            assertEquals(10., flowDecompositionResults.get(lineId).getInternalFlow(), EPSILON);
            assertEquals(Country.FR, flowDecompositionResults.get(lineId).getCountryTerminal1());
            assertEquals(Country.FR, flowDecompositionResults.get(lineId).getCountryTerminal2());
        }

    }

    private static Network getHighPtdfNetwork() {
        String line = "FGEN  11 FLOAD 11 A";
        Network network = AllocatedFlowTests.importNetwork(NETWORK_FILE_NAME);
        network.getLine(line).getTerminal1().disconnect();
        return network;
    }

    private static FlowDecompositionResults getFlowDecompositionResults(Network network) {
        FlowDecompositionParameters flowDecompositionParameters = new FlowDecompositionParameters();
        flowDecompositionParameters.setSaveIntermediates(FlowDecompositionParameters.SAVE_INTERMEDIATES);
        flowDecompositionParameters.setEnableLossesCompensation(FlowDecompositionParameters.DISABLE_LOSSES_COMPENSATION);
        flowDecompositionParameters.setBranchSelectionStrategy(FlowDecompositionParameters.BranchSelectionStrategy.INTERCONNECTION_OR_ZONE_TO_ZONE_PTDF_GT_5PC);
        FlowDecompositionComputer flowComputer = new FlowDecompositionComputer(flowDecompositionParameters);
        return flowComputer.run(network);
    }
}
