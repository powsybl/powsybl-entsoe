/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.glsk.ucte;

import com.powsybl.glsk.api.GlskDocument;
import com.powsybl.glsk.api.GlskPoint;
import com.powsybl.glsk.commons.GlskException;
import org.threeten.extra.Interval;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.*;

/**
 * Ucte type GLSK document object: contains a list of GlskPoint
 * @author Pengbo Wang {@literal <pengbo.wang@rte-international.com>}
 * @author Amira Kahya {@literal <amira.kahya@rte-france.com>}
 */
public final class UcteGlskDocument implements GlskDocument {
    /**
     * list of GlskPoint in the give Glsk document
     */
    private final List<GlskPoint> listUcteGlskBlocks;
    /**
     * map of Country EIC and time series
     */
    private final Map<String, UcteGlskSeries> ucteGlskSeriesByCountry; //map<countryEICode, UcteGlskSeries>
    /**
     * List of Glsk point by country code
     */
    private final Map<String, List<UcteGlskPoint>> ucteGlskPointsByCountry; //map <CountryID, List<GlskPoint>>
    /**
     * document GSKTimeInterval
     */
    private Interval gSKTimeInterval; // GSKTimeInterval. ex: <GSKTimeInterval v="2016-07-28T22:00Z/2016-07-29T22:00Z"/>

    public static UcteGlskDocument importGlsk(InputStream inputStream) {
        try {
            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            documentBuilderFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            documentBuilderFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            documentBuilderFactory.setAttribute(XMLConstants.FEATURE_SECURE_PROCESSING, Boolean.TRUE);
            documentBuilderFactory.setNamespaceAware(true);

            Document document = documentBuilderFactory.newDocumentBuilder().parse(inputStream);
            document.getDocumentElement().normalize();
            return new UcteGlskDocument(document);
        } catch (IOException | SAXException | ParserConfigurationException e) {
            throw new GlskException("Unable to import CIM GLSK file.", e);
        }
    }

    public static UcteGlskDocument importGlsk(Document document) {
        return new UcteGlskDocument(document);
    }

    private UcteGlskDocument(Document document) {
        if (document.getElementsByTagName("GSKTimeInterval").getLength() > 0) {
            this.gSKTimeInterval = Interval.parse(((Element) document.getElementsByTagName("GSKTimeInterval").item(0)).getAttribute("v"));
        }

        List<UcteGlskSeries> rawlistUcteGlskSeries = new ArrayList<>();
        ucteGlskSeriesByCountry = new HashMap<>();
        NodeList glskSeriesNodeList = document.getElementsByTagName("GSKSeries");
        for (int i = 0; i < glskSeriesNodeList.getLength(); i++) {
            if (glskSeriesNodeList.item(i).getNodeType() == Node.ELEMENT_NODE) {
                Element glskSeriesElement = (Element) glskSeriesNodeList.item(i);
                UcteGlskSeries glskSeries = new UcteGlskSeries(glskSeriesElement);
                rawlistUcteGlskSeries.add(glskSeries);
            }
        }

        //construct ucteGlskSeriesByCountry, collecting all points for each area
        for (UcteGlskSeries glskSeries : rawlistUcteGlskSeries) {
            String currentArea = glskSeries.getArea();
            if (!ucteGlskSeriesByCountry.containsKey(currentArea)) {
                ucteGlskSeriesByCountry.put(currentArea, glskSeries);
            } else {
                ucteGlskSeriesByCountry.get(currentArea).getUcteGlskBlocks().addAll(glskSeries.getUcteGlskBlocks());
            }
        }

        ucteGlskSeriesByCountry.values().forEach(series -> {
            List<UcteGlskPoint> normalizedPoints = normalizePoints(series.getUcteGlskBlocks());
            series.getUcteGlskBlocks().clear();
            series.getUcteGlskBlocks().addAll(normalizedPoints);
        });

        //construct list gsk points
        listUcteGlskBlocks = new ArrayList<>();
        ucteGlskSeriesByCountry.keySet().forEach(id -> listUcteGlskBlocks.addAll(ucteGlskSeriesByCountry.get(id).getUcteGlskBlocks()));

        //construct map ucteGlskPointsByCountry
        ucteGlskPointsByCountry = new HashMap<>();
        ucteGlskSeriesByCountry.keySet().forEach(id -> {
            String country = ucteGlskSeriesByCountry.get(id).getArea();
            List<UcteGlskPoint> glskPointList = ucteGlskSeriesByCountry.get(id).getUcteGlskBlocks();
            if (ucteGlskPointsByCountry.containsKey(country)) {
                glskPointList.addAll(ucteGlskPointsByCountry.get(country));
            }
            ucteGlskPointsByCountry.put(country, glskPointList);
        });
    }

    private List<UcteGlskPoint> normalizePoints(List<UcteGlskPoint> sourcePoints) {
        SortedSet<Instant> boundaries = new TreeSet<>();
        List<UcteGlskPoint> normalizedPoints = new ArrayList<>();
        for (UcteGlskPoint point : sourcePoints) {
            Interval interval = point.getPointInterval();
            if (!interval.getStart().equals(interval.getEnd())) {
                boundaries.add(interval.getStart());
                boundaries.add(interval.getEnd());
            }
        }

        List<Instant> sortedBoundaries = new ArrayList<>(boundaries);
        for (int i = 0; i < sortedBoundaries.size() - 1; i++) {
            Instant start = sortedBoundaries.get(i);
            Instant end = sortedBoundaries.get(i + 1);
            List<UcteGlskPoint> activePoints = new ArrayList<>();
            for (UcteGlskPoint point : sourcePoints) {
                Interval interval = point.getPointInterval();
                if (!interval.getStart().equals(interval.getEnd())
                        && !interval.getStart().isAfter(start)
                        && !interval.getEnd().isBefore(end)) {
                    activePoints.add(point);
                }
            }
            if (!activePoints.isEmpty()) {
                Interval segmentInterval = Interval.of(start, end);
                UcteGlskPoint segment = activePoints.get(0).copyForInterval(segmentInterval);
                segment.getGlskShiftKeys().clear();
                activePoints.forEach(point -> point.getGlskShiftKeys().forEach(key ->
                        segment.getGlskShiftKeys().add(((UcteGlskShiftKey) key).copyForInterval(segmentInterval))));
                normalizedPoints.add(segment);
            }
        }

        normalizedPoints.sort(Comparator.comparing(point -> point.getPointInterval().getStart()));
        return normalizedPoints;
    }

    /**
     * @return getter list of glsk point in the document
     */
    public List<GlskPoint> getListUcteGlskBlocks() {
        return listUcteGlskBlocks;
    }

    /**
     * @return getter list of time series
     */
    public List<UcteGlskSeries> getListGlskSeries() {
        return new ArrayList<>(ucteGlskSeriesByCountry.values());
    }

    /**
     * @return getter map of list glsk point
     */
    public Map<String, List<UcteGlskPoint>> getUcteGlskPointsByCountry() {
        return ucteGlskPointsByCountry;
    }

    /**
     * @return getter list of country
     */
    @Override
    public List<String> getZones() {
        return new ArrayList<>(ucteGlskPointsByCountry.keySet());
    }

    @Override
    public List<GlskPoint> getGlskPoints(String zone) {
        return new ArrayList<>(getUcteGlskPointsByCountry().get(zone));
    }

    public Map<String, UcteGlskPoint> getGlskPointsForInstant(Instant instant) {
        Map<String, UcteGlskPoint> glskPointInstant = new HashMap<>();
        ucteGlskPointsByCountry.forEach((key, glskPoints) -> {
            UcteGlskPoint glskPoint = glskPoints.stream()
                    .filter(p -> p.containsInstant(instant))
                    .findAny().orElseThrow(() -> new GlskException("Error during get glsk point by instant for " + key + " country"));
            glskPointInstant.put(key, glskPoint);
        });
        return glskPointInstant;
    }

    /**
     * @return document time interval
     */
    public Interval getGSKTimeInterval() {
        return gSKTimeInterval;
    }
}
