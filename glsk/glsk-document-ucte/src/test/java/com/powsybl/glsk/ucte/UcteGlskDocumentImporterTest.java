/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.glsk.ucte;

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
                assertEquals(1.0, ucteGlskDocument.getListGlskSeries().get(i).getUcteGlskBlocks().get(j).getGlskShiftKeys().stream().mapToDouble(GlskShiftKey::getQuantity).sum(), 1e-5);
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
    void testFileNotFound() {
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
                .filter(glskRegisteredResource -> glskRegisteredResource.getmRID().equals("IOJDAEQD"))
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

    @Test
    void testOverlappingPointsAreNormalizedPerArea() {
        UcteGlskDocument document = UcteGlskDocument.importGlsk(getResourceAsInputStream("/OverlappingPoints.xml"));
        List<UcteGlskPoint> areaAPoints = document.getUcteGlskPointsByCountry().get("AREA-A");

        assertEquals(List.of(
                interval("2020-01-01T00:00Z", "2020-01-01T02:00Z"),
                interval("2020-01-01T02:00Z", "2020-01-01T03:00Z"),
                interval("2020-01-01T03:00Z", "2020-01-01T04:00Z"),
                interval("2020-01-01T04:00Z", "2020-01-01T05:00Z"),
                interval("2020-01-01T05:00Z", "2020-01-01T06:00Z"),
                interval("2020-01-01T06:00Z", "2020-01-01T07:00Z"),
                interval("2020-01-01T07:00Z", "2020-01-01T08:00Z"),
                interval("2020-01-01T08:00Z", "2020-01-01T09:00Z"),
                interval("2020-01-01T09:00Z", "2020-01-01T10:00Z"),
                interval("2020-01-01T12:00Z", "2020-01-01T13:00Z"),
                interval("2020-01-01T14:00Z", "2020-01-01T15:00Z"),
                interval("2020-01-01T16:00Z", "2020-01-01T17:00Z"),
                interval("2020-01-01T17:00Z", "2020-01-01T18:00Z"),
                interval("2020-01-01T19:00Z", "2020-01-01T20:00Z"),
                interval("2020-01-01T20:00Z", "2020-01-01T21:00Z"),
                interval("2020-01-01T22:00Z", "2020-01-01T23:00Z")),
                areaAPoints.stream().map(UcteGlskPoint::getPointInterval).toList());

        assertResources(point(areaAPoints, "2020-01-01T00:00Z", "2020-01-01T02:00Z"), "A");
        assertResources(point(areaAPoints, "2020-01-01T02:00Z", "2020-01-01T03:00Z"), "A", "B");
        assertResources(point(areaAPoints, "2020-01-01T03:00Z", "2020-01-01T04:00Z"), "A", "B", "C");
        assertResources(point(areaAPoints, "2020-01-01T04:00Z", "2020-01-01T05:00Z"), "A", "C");
        assertResources(point(areaAPoints, "2020-01-01T05:00Z", "2020-01-01T06:00Z"), "A", "C", "O");
        assertResources(point(areaAPoints, "2020-01-01T06:00Z", "2020-01-01T07:00Z"), "C", "O");
        assertResources(point(areaAPoints, "2020-01-01T07:00Z", "2020-01-01T08:00Z"), "O");
        assertResources(point(areaAPoints, "2020-01-01T08:00Z", "2020-01-01T09:00Z"), "D");
        assertResources(point(areaAPoints, "2020-01-01T09:00Z", "2020-01-01T10:00Z"), "E");
        assertResources(point(areaAPoints, "2020-01-01T16:00Z", "2020-01-01T17:00Z"), "H", "I");
        assertResources(point(areaAPoints, "2020-01-01T17:00Z", "2020-01-01T18:00Z"), "H");
        assertResources(point(areaAPoints, "2020-01-01T20:00Z", "2020-01-01T21:00Z"), "J", "K");

        UcteGlskPoint equalIntervalPoint = point(areaAPoints, "2020-01-01T22:00Z", "2020-01-01T23:00Z");
        assertResources(equalIntervalPoint, "L", "M", "N");
        assertEquals(List.of(0.4, 0.4, 0.6), equalIntervalPoint.getGlskShiftKeys().stream()
                .map(GlskShiftKey::getQuantity).toList());
        assertEquals(1.3, equalIntervalPoint.getGlskShiftKeys().get(0).getRegisteredResourceArrayList().get(0).getParticipationFactor());
        assertEquals("AREA-A", equalIntervalPoint.getSubjectDomainmRID());
        assertEquals("A03", equalIntervalPoint.getCurveType());

        List<UcteGlskPoint> areaBPoints = document.getUcteGlskPointsByCountry().get("AREA-B");
        assertEquals(3, areaBPoints.size());
        assertResources(areaBPoints.get(0), "P");
        assertResources(areaBPoints.get(1), "P", "Q");
        assertResources(areaBPoints.get(2), "Q");

        UcteGlskSeries firstAreaSeries = document.getListGlskSeries().stream()
                .filter(series -> series.getArea().equals("AREA-A")).findFirst().orElseThrow();
        assertEquals("first-A", firstAreaSeries.getTimeSeriesID());
        assertEquals(16, firstAreaSeries.getUcteGlskBlocks().size());
    }

    @Test
    void testNormalizedSegmentsDoNotShareMutableKeysOrResources() {
        List<UcteGlskPoint> points = UcteGlskDocument.importGlsk(getResourceAsInputStream("/OverlappingPoints.xml"))
                .getUcteGlskPointsByCountry().get("AREA-A");
        UcteGlskPoint firstSegment = point(points, "2020-01-01T00:00Z", "2020-01-01T02:00Z");
        UcteGlskPoint nextSegment = point(points, "2020-01-01T02:00Z", "2020-01-01T03:00Z");
        GlskShiftKey firstKey = firstSegment.getGlskShiftKeys().get(0);
        GlskShiftKey nextKey = nextSegment.getGlskShiftKeys().get(0);
        GlskRegisteredResource firstResource = firstKey.getRegisteredResourceArrayList().get(0);
        GlskRegisteredResource nextResource = nextKey.getRegisteredResourceArrayList().get(0);

        assertNotSame(firstKey, nextKey);
        assertNotSame(firstResource, nextResource);
        firstSegment.setPosition(99);
        firstKey.setBusinessType("changed");
        firstResource.setmRID("changed");

        assertEquals(1, nextSegment.getPosition());
        assertEquals("B42", nextKey.getBusinessType());
        assertEquals("A", nextResource.getmRID());
    }

    private Interval interval(String start, String end) {
        return Interval.parse(start + "/" + end);
    }

    private UcteGlskPoint point(List<UcteGlskPoint> points, String start, String end) {
        return points.stream().filter(point -> point.getPointInterval().equals(interval(start, end)))
                .findFirst().orElseThrow();
    }

    private void assertResources(UcteGlskPoint point, String... resourceIds) {
        assertEquals(List.of(resourceIds), point.getGlskShiftKeys().stream()
                .flatMap(key -> key.getRegisteredResourceArrayList().stream())
                .map(GlskRegisteredResource::getmRID).toList());
    }
}
