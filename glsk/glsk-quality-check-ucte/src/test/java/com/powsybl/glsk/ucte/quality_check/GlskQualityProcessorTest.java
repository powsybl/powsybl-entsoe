/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.glsk.ucte.quality_check;

import com.powsybl.commons.reporter.Report;
import com.powsybl.commons.reporter.ReporterModel;
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
        ReporterModel reporter = new ReporterModel("defaultTask", "defaultName");
        GlskQualityProcessor.process(ucteGlskDocument, network, Instant.parse("2016-07-28T23:30:00Z"), reporter);

        assertTrue(reporter.getReports().isEmpty());
    }

    @Test
    public void qualityCheckWithError1() {
        UcteGlskDocument ucteGlskDocument = UcteGlskDocument.importGlsk(getResourceAsInputStream(FIRST_ERROR));
        Network network = Importers.loadNetwork("testCase.xiidm", getClass().getResourceAsStream("/testCase.xiidm"));
        ReporterModel reporter = new ReporterModel("defaultTask", "defaultName");
        GlskQualityProcessor.process(ucteGlskDocument, network, Instant.parse("2016-07-28T23:30:00Z"), reporter);

        assertEquals(1, reporter.getReports().size());
        Report r = reporter.getReports().stream().findFirst().get();
        assertEquals("GLSK node is not found in CGM", r.getDefaultMessage());
        assertEquals("FFR2AA2 ", r.getValue(GlskQualityCheck.NODE_ID_KEY).toString());
        assertEquals("10YFR-RTE------C", r.getValue(GlskQualityCheck.TSO_KEY).toString());
        //Get unique TSO count in logs
        assertEquals(1, reporter.getReports().stream().filter(rep -> rep.getValue(GlskQualityCheck.TSO_KEY).toString().equals("10YFR-RTE------C")).count());
        //Get log count for RTE
        assertEquals(1, reporter.getReports().stream().map(rep -> rep.getValue(GlskQualityCheck.TSO_KEY).toString()).distinct().count());

    }

    @Test
    public void qualityCheckWithError2() {
        UcteGlskDocument ucteGlskDocument = UcteGlskDocument.importGlsk(getResourceAsInputStream(COUNTRYTEST));
        Network network = Importers.loadNetwork("testCase_error_2.xiidm", getClass().getResourceAsStream("/testCase_error_2.xiidm"));
        ReporterModel reporter = new ReporterModel("defaultTask", "defaultName");
        GlskQualityProcessor.process(ucteGlskDocument, network, Instant.parse("2016-07-28T23:30:00Z"), reporter);

        assertEquals(1, reporter.getReports().size());
        Report r = reporter.getReports().stream().findFirst().get();
        assertEquals("GLSK node is present but has no running Generator or Load", r.getDefaultMessage());
        assertEquals("FFR2AA1 ", r.getValue(GlskQualityCheck.NODE_ID_KEY).toString());
        assertEquals("10YFR-RTE------C", r.getValue(GlskQualityCheck.TSO_KEY).toString());
    }

    @Test
    public void qualityCheckWithError3() {
        UcteGlskDocument ucteGlskDocument = UcteGlskDocument.importGlsk(getResourceAsInputStream(COUNTRYTEST));
        Network network = Importers.loadNetwork("testCase_error_3.xiidm", getClass().getResourceAsStream("/testCase_error_3.xiidm"));
        ReporterModel reporter = new ReporterModel("defaultTask", "defaultName");
        GlskQualityProcessor.process(ucteGlskDocument, network, Instant.parse("2016-07-28T23:30:00Z"), reporter);

        assertEquals(1, reporter.getReports().size());
        Report r = reporter.getReports().stream().findFirst().get();
        assertEquals("GLSK node is connected to an island", r.getDefaultMessage());
        assertEquals("FFR2AA1 ", r.getValue(GlskQualityCheck.NODE_ID_KEY).toString());
        assertEquals("10YFR-RTE------C", r.getValue(GlskQualityCheck.TSO_KEY).toString());
    }

    @Test
    public void qualityCheckLoadNotConnected() {
        UcteGlskDocument ucteGlskDocument = UcteGlskDocument.importGlsk(getResourceAsInputStream(COUNTRYTEST));
        Network network = Importers.loadNetwork("testCase_error_load_not_connected.xiidm", getClass().getResourceAsStream("/testCase_error_load_not_connected.xiidm"));
        ReporterModel reporter = new ReporterModel("defaultTask", "defaultName");
        GlskQualityProcessor.process(ucteGlskDocument, network, Instant.parse("2016-07-28T23:30:00Z"), reporter);

        assertEquals(1, reporter.getReports().size());
        Report r = reporter.getReports().stream().findFirst().get();
        assertEquals("GLSK node is connected to an island", r.getDefaultMessage());
        assertEquals("FFR2AA1 ", r.getValue(GlskQualityCheck.NODE_ID_KEY).toString());
        assertEquals("10YFR-RTE------C", r.getValue(GlskQualityCheck.TSO_KEY).toString());
    }

}
