/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.glsk.ucte;

import com.google.common.math.DoubleMath;
import com.powsybl.glsk.api.GlskRegisteredResource;
import com.powsybl.glsk.api.GlskShiftKey;
import com.powsybl.glsk.api.io.GlskDocumentImporters;
import com.powsybl.glsk.commons.GlskException;
import org.junit.jupiter.api.Test;
import org.threeten.extra.Interval;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Pengbo Wang {@literal <pengbo.wang@rte-international.com>}
 */
class UcteGlskDocumentImporterTest {

    private static final String COUNTRYFULL = "/20160729_0000_GSK_allday_full.xml";
    private static final String COUNTRYTEST = "/20160729_0000_GSK_allday_test.xml";

    private InputStream getResourceAsInputStream(String resource) {
        return getClass().getResourceAsStream(resource);
    }

    @Test
    void testUcteGlskDocumentImporterTest() {
        UcteGlskDocument ucteGlskDocument = UcteGlskDocument.importGlsk(getResourceAsInputStream(COUNTRYTEST));

        List<UcteGlskSeries> list = ucteGlskDocument.getListGlskSeries();
        assertFalse(list.isEmpty());
        assertEquals(1, ucteGlskDocument.getListGlskSeries().size());
        assertEquals(1, ucteGlskDocument.getListUcteGlskBlocks().size());
        assertFalse(ucteGlskDocument.getListGlskSeries().get(0).getUcteGlskBlocks().get(0).toString().isEmpty());
    }

    @Test
    void testImportUcteGlskDocumentWithFilePathString() {
        assertFalse(UcteGlskDocument.importGlsk(getResourceAsInputStream(COUNTRYTEST)).getListGlskSeries().isEmpty());
    }

    @Test
    void testImportUcteGlskDocumentWithFilePath() {
        assertFalse(UcteGlskDocument.importGlsk(getResourceAsInputStream(COUNTRYTEST)).getListGlskSeries().isEmpty());
    }

    @Test
    void testUcteGlskDocumentImporterFull() {
        UcteGlskDocument ucteGlskDocument = UcteGlskDocument.importGlsk(getResourceAsInputStream(COUNTRYFULL));

        //test Merged List GlskSeries
        assertEquals(12, ucteGlskDocument.getListGlskSeries().size());
        //test merged List GlskPoints
        assertEquals(67, ucteGlskDocument.getListUcteGlskBlocks().size());

        //test factor LSK + GSK = 1
        for (int i = 0; i < ucteGlskDocument.getListGlskSeries().size(); i++) {
            for (int j = 0; j < ucteGlskDocument.getListGlskSeries().get(i).getUcteGlskBlocks().size(); j++) {
                assertTrue(DoubleMath.fuzzyEquals(1.0,
                        ucteGlskDocument.getListGlskSeries().get(i).getUcteGlskBlocks().get(j).getGlskShiftKeys().stream().mapToDouble(GlskShiftKey::getQuantity).sum(),
                        1e-5));
            }
        }

        //test map <CountryID, List<Point>>
        Map<String, List<UcteGlskPoint>> listPoints = ucteGlskDocument.getUcteGlskPointsByCountry();
        int sum = 0;
        for (String s : listPoints.keySet()) {
            sum += listPoints.get(s).size();
        }
        assertEquals(67, sum);

        //test countries list
        List<String> countries = ucteGlskDocument.getZones();
        assertEquals(12, countries.size());

        Interval documentGSKTimeInterval = ucteGlskDocument.getGSKTimeInterval(); // <GSKTimeInterval v="2016-07-28T22:00Z/2016-07-29T22:00Z"/>
        assertEquals("2016-07-28T22:00:00Z", documentGSKTimeInterval.getStart().toString());
    }

    @Test
    void testExceptionCases() {
        byte[] nonXmlBytes = "{ should not be imported }".getBytes();
        InputStream inputStream = new ByteArrayInputStream(nonXmlBytes);
        assertThrows(GlskException.class, () -> UcteGlskDocument.importGlsk(inputStream));
    }

    @Test
    void testFileNotFound() throws FileNotFoundException {
        assertThrows(FileNotFoundException.class, () -> GlskDocumentImporters.importGlsk("/nonExistingFile.xml"));
    }

    @Test
    void testGetGlskPointForInstant() {
        UcteGlskDocument ucteGlskDocument = UcteGlskDocument.importGlsk(getResourceAsInputStream(COUNTRYFULL));
        Map<String, UcteGlskPoint> result = ucteGlskDocument.getGlskPointsForInstant(Instant.parse("2016-07-28T23:30:00Z"));
        Instant instant = Instant.parse("2016-07-28T23:30:00Z");
        assertTrue(result.get("10YNL----------L").getPointInterval().getStart().isBefore(instant));
        assertTrue(result.get("10YNL----------L").getPointInterval().getEnd().isAfter(instant));
        assertEquals(1, result.get("10YNL----------L").getGlskShiftKeys().size());

        double factor = result.get("10YNL----------L").getGlskShiftKeys().get(0)
                .getRegisteredResourceArrayList()
                .stream()
                .filter(glskRegisteredResource -> glskRegisteredResource.getmRID().equals("N_EC-42 "))
                .mapToDouble(GlskRegisteredResource::getParticipationFactor)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Not good"));

        assertEquals(0.009878, factor, 0.);
    }

    @Test
    void testGetGlskPointForIncorrectInstant() {
        UcteGlskDocument ucteGlskDocument = UcteGlskDocument.importGlsk(getResourceAsInputStream(COUNTRYFULL));
        Instant instant = Instant.parse("2016-07-29T22:00:00Z");
        assertThrows(GlskException.class, () -> ucteGlskDocument.getGlskPointsForInstant(instant));
    }

    @Test
    void existsTrue() {
        UcteGlskDocumentImporter importer = new UcteGlskDocumentImporter();
        assertTrue(importer.canImport(getResourceAsInputStream("/20160729_0000_GSK_allday_full.xml")));
    }

    @Test
    void existsFalse() {
        UcteGlskDocumentImporter importer = new UcteGlskDocumentImporter();
        assertFalse(importer.canImport(getResourceAsInputStream("/20160729_0000_GSK_allday_wrong.xml")));
    }

    @Test
    void testUcteGlskDocumentImporterTest2() {
        UcteGlskDocument ucteGlskDocument = UcteGlskDocument.importGlsk(getResourceAsInputStream("/TestMultiShare1.xml"));
        assertEquals(24, ucteGlskDocument.getUcteGlskPointsByCountry().get("10YFR-RTE------C").size());
    }
}
