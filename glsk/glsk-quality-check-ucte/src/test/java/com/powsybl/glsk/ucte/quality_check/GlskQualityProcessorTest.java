/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.glsk.ucte.quality_check;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.glsk.ucte.UcteGlskDocument;
import com.powsybl.iidm.network.Network;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.time.Instant;

import static com.powsybl.glsk.commons.GlskReports.NODE_ID_KEY;
import static com.powsybl.glsk.commons.GlskReports.TSO_KEY;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Marc Erkol {@literal <marc.erkol at rte-france.com>}
 */
class GlskQualityProcessorTest {

    private static final String COUNTRYTEST = "/20170322_1844_SN3_FR2_GLSK_test.xml";
    private static final String FIRST_ERROR = "/20170322_1844_SN3_FR2_GLSK_error_1.xml";

    private InputStream getResourceAsInputStream(String resource) {
        return getClass().getResourceAsStream(resource);
    }

    @Test
    void qualityCheckWithCorrectValue() {
        UcteGlskDocument ucteGlskDocument = UcteGlskDocument.importGlsk(getResourceAsInputStream(COUNTRYTEST));
        Network network = Network.read("testCase.xiidm", getClass().getResourceAsStream("/testCase.xiidm"));
        ReportNode reportNode = ReportNode.newRootReportNode().withMessageTemplate("defaultTask", "defaultName").build();
        GlskQualityProcessor.process(ucteGlskDocument, network, Instant.parse("2016-07-28T23:30:00Z"), reportNode);

        assertTrue(reportNode.getChildren().isEmpty());
    }

    @Test
    void qualityCheckWithError1() {
        UcteGlskDocument ucteGlskDocument = UcteGlskDocument.importGlsk(getResourceAsInputStream(FIRST_ERROR));
        Network network = Network.read("testCase.xiidm", getClass().getResourceAsStream("/testCase.xiidm"));
        ReportNode reporter = ReportNode.newRootReportNode().withMessageTemplate("defaultTask", "defaultName").build();
        GlskQualityProcessor.process(ucteGlskDocument, network, Instant.parse("2016-07-28T23:30:00Z"), reporter);

        assertEquals(1, reporter.getChildren().size());
        ReportNode r = reporter.getChildren().stream().findFirst().get();
        assertEquals("GLSK node is not found in CGM", r.getMessage());
        assertEquals("FFR2AA2 ", r.getValue(NODE_ID_KEY).get().toString());
        assertEquals("10YFR-RTE------C", r.getValue(TSO_KEY).get().toString());
        //Get unique TSO count in logs
        assertEquals(1, reporter.getChildren().stream().filter(rep -> rep.getValue(TSO_KEY).get().toString().equals("10YFR-RTE------C")).count());
        //Get log count for RTE
        assertEquals(1, reporter.getChildren().stream().map(rep -> rep.getValue(TSO_KEY).get().toString()).distinct().count());

    }

    @Test
    void qualityCheckWithError2() {
        UcteGlskDocument ucteGlskDocument = UcteGlskDocument.importGlsk(getResourceAsInputStream(COUNTRYTEST));
        Network network = Network.read("testCase_error_2.xiidm", getClass().getResourceAsStream("/testCase_error_2.xiidm"));
        ReportNode reporter = ReportNode.newRootReportNode().withMessageTemplate("defaultTask", "defaultName").build();
        GlskQualityProcessor.process(ucteGlskDocument, network, Instant.parse("2016-07-28T23:30:00Z"), reporter);

        assertEquals(1, reporter.getChildren().size());
        ReportNode r = reporter.getChildren().stream().findFirst().get();
        assertEquals("GLSK node is present but has no running Generator or Load", r.getMessage());
        assertEquals("FFR2AA1 ", r.getValue(NODE_ID_KEY).get().toString());
        assertEquals("10YFR-RTE------C", r.getValue(TSO_KEY).get().toString());
    }

    @Test
    void qualityCheckWithError3() {
        UcteGlskDocument ucteGlskDocument = UcteGlskDocument.importGlsk(getResourceAsInputStream(COUNTRYTEST));
        Network network = Network.read("testCase_error_3.xiidm", getClass().getResourceAsStream("/testCase_error_3.xiidm"));
        ReportNode reporter = ReportNode.newRootReportNode().withMessageTemplate("defaultTask", "defaultName").build();
        GlskQualityProcessor.process(ucteGlskDocument, network, Instant.parse("2016-07-28T23:30:00Z"), reporter);

        assertEquals(1, reporter.getChildren().size());
        ReportNode r = reporter.getChildren().stream().findFirst().get();
        assertEquals("GLSK node is connected to an island", r.getMessage());
        assertEquals("FFR2AA1 ", r.getValue(NODE_ID_KEY).get().toString());
        assertEquals("10YFR-RTE------C", r.getValue(TSO_KEY).get().toString());
    }

    @Test
    void qualityCheckLoadNotConnected() {
        UcteGlskDocument ucteGlskDocument = UcteGlskDocument.importGlsk(getResourceAsInputStream(COUNTRYTEST));
        Network network = Network.read("testCase_error_load_not_connected.xiidm", getClass().getResourceAsStream("/testCase_error_load_not_connected.xiidm"));
        ReportNode reporter = ReportNode.newRootReportNode().withMessageTemplate("defaultTask", "defaultName").build();
        GlskQualityProcessor.process(ucteGlskDocument, network, Instant.parse("2016-07-28T23:30:00Z"), reporter);

        assertEquals(1, reporter.getChildren().size());
        ReportNode r = reporter.getChildren().stream().findFirst().get();
        assertEquals("GLSK node is connected to an island", r.getMessage());
        assertEquals("FFR2AA1 ", r.getValue(NODE_ID_KEY).get().toString());
        assertEquals("10YFR-RTE------C", r.getValue(TSO_KEY).get().toString());
    }

}
