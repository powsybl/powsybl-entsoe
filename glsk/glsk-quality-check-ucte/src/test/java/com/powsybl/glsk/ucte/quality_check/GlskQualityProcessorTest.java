/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.glsk.ucte.quality_check;

import com.powsybl.glsk.ucte.UcteGlskDocument;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Network;
import org.junit.Test;

import java.io.InputStream;
import java.time.Instant;

import static org.junit.Assert.*;

/**
 * @author Marc Erkol {@literal <marc.erkol at rte-france.com>}
 */
public class GlskQualityProcessorTest {

    private static final String COUNTRYTEST = "/20170322_1844_SN3_FR2_GLSK_test.xml";
    private static final String FIRST_ERROR = "/20170322_1844_SN3_FR2_GLSK_error_1.xml";

    private InputStream getResourceAsInputStream(String resource) {
        return getClass().getResourceAsStream(resource);
    }

    @Test
    public void qualityCheckWithCorrectValue() {
        UcteGlskDocument ucteGlskDocument = UcteGlskDocument.importGlsk(getResourceAsInputStream(COUNTRYTEST));
        Network network = Importers.loadNetwork("testCase.xiidm", getClass().getResourceAsStream("/testCase.xiidm"));
        QualityReport qualityReport = GlskQualityProcessor.process(ucteGlskDocument, network, Instant.parse("2016-07-28T23:30:00Z"));

        assertTrue(qualityReport.getQualityLogsByTso().isEmpty());
    }

    @Test
    public void qualityCheckWithError1() {
        UcteGlskDocument ucteGlskDocument = UcteGlskDocument.importGlsk(getResourceAsInputStream(FIRST_ERROR));
        Network network = Importers.loadNetwork("testCase.xiidm", getClass().getResourceAsStream("/testCase.xiidm"));
        QualityReport qualityReport = GlskQualityProcessor.process(ucteGlskDocument, network, Instant.parse("2016-07-28T23:30:00Z"));

        assertEquals(1, qualityReport.getAllQualityLogs().size());
        assertEquals("GLSK node is not found in CGM", qualityReport.getAllQualityLogs().get(0).getMessage());
        assertEquals("FFR2AA2 ", qualityReport.getAllQualityLogs().get(0).getNodeId());
        assertEquals("10YFR-RTE------C", qualityReport.getAllQualityLogs().get(0).getTso());
        assertEquals(1, qualityReport.getQualityLogsByTso().size());
        assertEquals(1, qualityReport.getQualityLogs("10YFR-RTE------C").size());

    }

    @Test
    public void qualityCheckWithError2() {
        UcteGlskDocument ucteGlskDocument = UcteGlskDocument.importGlsk(getResourceAsInputStream(COUNTRYTEST));
        Network network = Importers.loadNetwork("testCase_error_2.xiidm", getClass().getResourceAsStream("/testCase_error_2.xiidm"));
        QualityReport qualityReport = GlskQualityProcessor.process(ucteGlskDocument, network, Instant.parse("2016-07-28T23:30:00Z"));

        assertEquals(1, qualityReport.getAllQualityLogs().size());
        assertEquals("GLSK node is present but has no running Generator or Load", qualityReport.getAllQualityLogs().get(0).getMessage());
        assertEquals("FFR2AA1 ", qualityReport.getAllQualityLogs().get(0).getNodeId());
        assertEquals("10YFR-RTE------C", qualityReport.getAllQualityLogs().get(0).getTso());
    }

    @Test
    public void qualityCheckWithError3() {
        UcteGlskDocument ucteGlskDocument = UcteGlskDocument.importGlsk(getResourceAsInputStream(COUNTRYTEST));
        Network network = Importers.loadNetwork("testCase_error_3.xiidm", getClass().getResourceAsStream("/testCase_error_3.xiidm"));
        QualityReport qualityReport = GlskQualityProcessor.process(ucteGlskDocument, network, Instant.parse("2016-07-28T23:30:00Z"));

        assertEquals(1, qualityReport.getQualityLogsByTso().size());
        assertEquals("GLSK node is connected to an island", qualityReport.getAllQualityLogs().get(0).getMessage());
        assertEquals("FFR2AA1 ", qualityReport.getAllQualityLogs().get(0).getNodeId());
        assertEquals("10YFR-RTE------C", qualityReport.getAllQualityLogs().get(0).getTso());
    }

    @Test
    public void qualityCheckLoadNotConnected() {
        UcteGlskDocument ucteGlskDocument = UcteGlskDocument.importGlsk(getResourceAsInputStream(COUNTRYTEST));
        Network network = Importers.loadNetwork("testCase_error_load_not_connected.xiidm", getClass().getResourceAsStream("/testCase_error_load_not_connected.xiidm"));
        QualityReport qualityReport = GlskQualityProcessor.process(ucteGlskDocument, network, Instant.parse("2016-07-28T23:30:00Z"));

        assertEquals(1, qualityReport.getQualityLogsByTso().size());
        assertEquals("GLSK node is connected to an island", qualityReport.getAllQualityLogs().get(0).getMessage());
        assertEquals("FFR2AA1 ", qualityReport.getAllQualityLogs().get(0).getNodeId());
        assertEquals("10YFR-RTE------C", qualityReport.getAllQualityLogs().get(0).getTso());
    }

}
