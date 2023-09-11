/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.flow_decomposition.glsk_provider;

import com.powsybl.flow_decomposition.GlskProvider;
import com.powsybl.glsk.api.GlskDocument;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static com.powsybl.flow_decomposition.TestUtils.importGlskDocument;
import static com.powsybl.flow_decomposition.TestUtils.importNetwork;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
class GlskDocumentBasedGlskProviderTest {
    private static final double EPSILON = 1e-3;

    @Test
    void testThatGlskProviderWithFirstInstantGetsCorrectFactors() {
        String networkFileName = "testCase.xiidm";
        String glskFileName = "GlskUcteFrUniqueInstant.xml";
        Network network = importNetwork(networkFileName);
        GlskDocument glskDocument = importGlskDocument(glskFileName);
        GlskProvider glskProvider = GlskDocumentBasedGlskProvider.notTimeSpecific(glskDocument);
        Map<Country, Map<String, Double>> glsks = glskProvider.getGlsk(network);
        assertEquals(0.5, glsks.get(Country.FR).get("FFR1AA1 _generator"), EPSILON);
        assertEquals(0.5, glsks.get(Country.FR).get("FFR2AA1 _generator"), EPSILON);
    }

    @Test
    void testThatGlskProviderWithNetworkInstantGetsCorrectFactors() {
        String networkFileName = "testCase.xiidm";
        String glskFileName = "GlskUcteFrMultipleInstants.xml";
        Network network = importNetwork(networkFileName);
        GlskDocument glskDocument = importGlskDocument(glskFileName);
        GlskProvider glskProvider = GlskDocumentBasedGlskProvider.basedOnNetworkInstant(glskDocument);
        Map<Country, Map<String, Double>> glsks = glskProvider.getGlsk(network);
        assertEquals(1, glsks.get(Country.FR).get("FFR1AA1 _generator"), EPSILON);
    }

    @Test
    void testThatGlskProviderWithGiovenInstantGetsCorrectFactors() {
        String networkFileName = "testCase.xiidm";
        String glskFileName = "GlskUcteFrMultipleInstants.xml";
        Network network = importNetwork(networkFileName);
        GlskDocument glskDocument = importGlskDocument(glskFileName);
        GlskProvider glskProvider = GlskDocumentBasedGlskProvider.basedOnGivenInstant(glskDocument, Instant.parse("2014-01-16T21:00:00Z"));
        Map<Country, Map<String, Double>> glsks = glskProvider.getGlsk(network);
        assertEquals(1, glsks.get(Country.FR).get("FFR2AA1 _generator"), EPSILON);
    }

    @Test
    void testThatCountriesNotIncludedInGlskDocumentHaveAutoGlskFactorsCalculated() {
        String networkFileName = "testCase.xiidm";
        String glskFileName = "GlskUcteFrMultipleInstants.xml";
        Network network = importNetwork(networkFileName);
        GlskDocument glskDocument = importGlskDocument(glskFileName);
        GlskProvider glskProvider = GlskDocumentBasedGlskProvider.basedOnNetworkInstant(glskDocument);
        Map<Country, Map<String, Double>> glsks = glskProvider.getGlsk(network);
        assertEquals(0.214, glsks.get(Country.BE).get("BBE1AA1 _generator"), EPSILON);
        assertEquals(0.429, glsks.get(Country.BE).get("BBE2AA1 _generator"), EPSILON);
        assertEquals(0.357, glsks.get(Country.BE).get("BBE3AA1 _generator"), EPSILON);
    }
}
