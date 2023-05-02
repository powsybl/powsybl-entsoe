/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.glsk.cim;

import com.powsybl.glsk.api.GlskDocument;
import com.powsybl.glsk.api.GlskPoint;
import com.powsybl.glsk.api.GlskShiftKey;
import com.powsybl.glsk.api.io.GlskDocumentImporters;
import com.powsybl.glsk.commons.GlskException;
import org.apache.commons.lang3.NotImplementedException;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.threeten.extra.Interval;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Pengbo Wang {@literal <pengbo.wang@rte-international.com>}
 * @author Sebastien Murgey {@literal <sebastien.murgey@rte-france.com>}
 */
class CimGlskDocumentImporterTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(CimGlskDocumentImporterTest.class);

    private static final String GLSKB42TEST = "/GlskB42test.xml";
    private static final String GLSKB42COUNTRY = "/GlskB42CountryIIDM.xml";
    private static final String GLSKMULTIPOINTSTEST = "/GlskMultiPoints.xml";
    private static final String GLSKB45TEST = "/GlskB45test.xml";
    private static final String GLSKB45A03TEST = "/GlskB45CurveTypeA03test.xml";

    private InputStream getResourceAsInputStream(String resource) {
        return getClass().getResourceAsStream(resource);
    }

    @Test
    void testGlskDocumentImporterWithFilePathString() {
        CimGlskDocument cimGlskDocument = CimGlskDocument.importGlsk(getResourceAsInputStream(GLSKB42COUNTRY));
        assertEquals("2018-08-28T22:00:00Z", cimGlskDocument.getInstantStart().toString());
        assertEquals("2018-08-29T22:00:00Z", cimGlskDocument.getInstantEnd().toString());
        assertFalse(cimGlskDocument.getZones().isEmpty());
    }

    @Test
    void testGlskDocumentImporterWithFilePath() {
        CimGlskDocument cimGlskDocument = CimGlskDocument.importGlsk(getResourceAsInputStream(GLSKB42COUNTRY));
        assertEquals("2018-08-28T22:00:00Z", cimGlskDocument.getInstantStart().toString());
        assertEquals("2018-08-29T22:00:00Z", cimGlskDocument.getInstantEnd().toString());
        assertFalse(cimGlskDocument.getZones().isEmpty());
    }

    @Test
    void testGlskDocumentImportB45() {
        CimGlskDocument cimGlskDocument = CimGlskDocument.importGlsk(getResourceAsInputStream(GLSKB45TEST));
        List<GlskShiftKey> glskShiftKeys = cimGlskDocument.getGlskPoints().get(0).getGlskShiftKeys();
        assertEquals(5, glskShiftKeys.size());
    }

    @Test
    void testGlskDocumentImportB45A03() {
        CimGlskDocument cimGlskDocument = CimGlskDocument.importGlsk(getResourceAsInputStream(GLSKB45A03TEST));
        assertEquals(2, cimGlskDocument.getGlskPoints().size());
        assertEquals("2017-04-12T22:00:00Z/2017-04-13T07:00:00Z", cimGlskDocument.getGlskPoints().get(0).getPointInterval().toString());
        assertEquals("2017-04-13T07:00:00Z/2017-04-13T22:00:00Z", cimGlskDocument.getGlskPoints().get(1).getPointInterval().toString());
    }

    @Test
    void testGlskDocumentImporterWithFileName() {
        CimGlskDocument cimGlskDocument = CimGlskDocument.importGlsk(getResourceAsInputStream(GLSKB42TEST));

        List<GlskPoint> glskPointList = cimGlskDocument.getGlskPoints();
        for (GlskPoint point : glskPointList) {
            assertEquals(Interval.parse("2018-08-28T22:00:00Z/2018-08-29T22:00:00Z"), point.getPointInterval());
            assertEquals(Integer.valueOf(1), point.getPosition());
        }

    }

    @Test
    void testGlskDocumentImporterGlskMultiPoints() {
        CimGlskDocument cimGlskDocument = CimGlskDocument.importGlsk(getResourceAsInputStream(GLSKMULTIPOINTSTEST));

        List<GlskPoint> glskPointList = cimGlskDocument.getGlskPoints();
        for (GlskPoint point : glskPointList) {
            LOGGER.info("Position: " + point.getPosition() + "; PointInterval: " + point.getPointInterval().toString());
        }
        assertFalse(glskPointList.isEmpty());
    }

    @Test
    void testExceptionCases() {
        byte[] nonXmlBytes = "{ should not be imported }".getBytes();
        CimGlskDocumentImporter importer = new CimGlskDocumentImporter();
        InputStream inputStream = new ByteArrayInputStream(nonXmlBytes);
        assertThrows(GlskException.class, () -> importer.importGlsk(inputStream));
    }

    @Test
    void testFileNotFound() throws FileNotFoundException {
        assertThrows(FileNotFoundException.class, () -> GlskDocumentImporters.importGlsk("/nonExistingFile.xml"));
    }

    @Test
    void existsTrue() {
        CimGlskDocumentImporter importer = new CimGlskDocumentImporter();
        assertTrue(importer.canImport(getResourceAsInputStream(GLSKB45TEST)));
    }

    @Test
    void existsFalse() {
        CimGlskDocumentImporter importer = new CimGlskDocumentImporter();
        assertFalse(importer.canImport(getResourceAsInputStream("/GlskB45wrong.xml")));
    }

    @Test
    void fullImport() {
        GlskDocument document = GlskDocumentImporters.importGlsk(getResourceAsInputStream(GLSKB42COUNTRY));
        assertEquals(1, document.getZones().size());
    }

    @Test
    void checkUnimplemented() {
        CimGlskDocumentImporter importer = new CimGlskDocumentImporter();
        assertThrows(NotImplementedException.class, () -> importer.importGlsk(getResourceAsInputStream(GLSKB42COUNTRY), false));
        assertThrows(NotImplementedException.class, () -> importer.importAndValidateGlsk(getResourceAsInputStream(GLSKB42COUNTRY), true));
    }
}
