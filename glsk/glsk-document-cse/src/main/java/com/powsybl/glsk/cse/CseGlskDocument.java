/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.glsk.cse;

import com.powsybl.action.util.Scalable;
import com.powsybl.glsk.api.AbstractGlskPoint;
import com.powsybl.glsk.api.AbstractGlskShiftKey;
import com.powsybl.glsk.api.GlskDocument;
import com.powsybl.glsk.api.util.converters.GlskPointScalableConverter;
import com.powsybl.glsk.commons.GlskException;
import com.powsybl.glsk.commons.ZonalData;
import com.powsybl.glsk.commons.ZonalDataChronology;
import com.powsybl.glsk.commons.ZonalDataImpl;
import com.powsybl.iidm.network.Network;
import com.powsybl.sensitivity.factors.variables.LinearGlsk;
import org.apache.commons.lang3.NotImplementedException;
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
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
public final class CseGlskDocument implements GlskDocument {
    private static final String LINEAR_GLSK_NOT_HANDLED = "CSE GLSK document does not handle Linear GLSK conversion";
    /**
     * list of GlskPoint in the given Glsk document
     */
    private final Map<String, List<AbstractGlskPoint>> cseGlskPoints = new TreeMap<>();

    public static CseGlskDocument importGlsk(Document document) {
        return new CseGlskDocument(document);
    }

    public static CseGlskDocument importGlsk(InputStream inputStream) {
        try {
            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            documentBuilderFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            documentBuilderFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            documentBuilderFactory.setAttribute(XMLConstants.FEATURE_SECURE_PROCESSING, Boolean.TRUE);
            documentBuilderFactory.setNamespaceAware(true);

            Document document = documentBuilderFactory.newDocumentBuilder().parse(inputStream);
            document.getDocumentElement().normalize();
            return new CseGlskDocument(document);
        } catch (IOException | SAXException | ParserConfigurationException e) {
            throw new GlskException("Unable to import CSE GLSK file.", e);
        }
    }

    private CseGlskDocument(Document document) {
        NodeList timeSeriesNodeList = document.getElementsByTagName("TimeSeries");
        for (int i = 0; i < timeSeriesNodeList.getLength(); i++) {
            if (timeSeriesNodeList.item(i).getNodeType() == Node.ELEMENT_NODE) {
                Element timeSeriesElement = (Element) timeSeriesNodeList.item(i);
                AbstractGlskPoint glskPoint = new CseGlskPoint(timeSeriesElement);
                cseGlskPoints.computeIfAbsent(glskPoint.getSubjectDomainmRID(), area -> cseGlskPoints.put(area, new ArrayList<>()));
                cseGlskPoints.get(glskPoint.getSubjectDomainmRID()).add(glskPoint);
            }
        }
    }

    @Override
    public List<String> getZones() {
        return new ArrayList<>(cseGlskPoints.keySet());
    }

    @Override
    public List<AbstractGlskPoint> getGlskPoints(String zone) {
        return cseGlskPoints.getOrDefault(zone, Collections.emptyList());
    }

    @Override
    public ZonalData<LinearGlsk> getZonalGlsks(Network network) {
        throw new NotImplementedException(LINEAR_GLSK_NOT_HANDLED);
    }

    @Override
    public ZonalData<LinearGlsk> getZonalGlsks(Network network, Instant instant) {
        throw new NotImplementedException(LINEAR_GLSK_NOT_HANDLED);
    }

    @Override
    public ZonalDataChronology<LinearGlsk> getZonalGlsksChronology(Network network) {
        throw new NotImplementedException(LINEAR_GLSK_NOT_HANDLED);
    }

    @Override
    public ZonalData<Scalable> getZonalScalable(Network network) {
        Map<String, Scalable> zonalData = new HashMap<>();
        for (Map.Entry<String, List<AbstractGlskPoint>> entry : cseGlskPoints.entrySet()) {
            String area = entry.getKey();
            List<AbstractGlskPoint> glskPoints = entry.getValue();
            if (isHybridCseGlskPoint(glskPoints)) {
                List<AbstractGlskShiftKey> shiftKeys = glskPoints.get(0).getGlskShiftKeys();
                Scalable scalable1 = GlskPointScalableConverter.convert(network, List.of(shiftKeys.get(0)));
                Scalable scalable2 = GlskPointScalableConverter.convert(network, List.of(shiftKeys.get(1)));
                zonalData.put(area, Scalable.upDown(Scalable.stack(scalable1, scalable2), scalable2));
            } else {
                zonalData.put(area, GlskPointScalableConverter.convert(network, glskPoints.get(0)));
            }
        }
        return new ZonalDataImpl<>(zonalData);
    }

    private boolean isHybridCseGlskPoint(List<AbstractGlskPoint> glskPointList) {
        // if 2 shift keys have different orders, this is a hybrid glsk for Swiss's ID CSE GSK.
        // Note: in CIM glsk format, there can be 2 shift keys, a GSK and a LSK, defined for a same zone,
        // these 2 shift keys (GSK + LSK) should be merged into one single Scalable
        return glskPointList.get(0).getGlskShiftKeys().size() == 2 &&
            ((CseGlskShiftKey) glskPointList.get(0).getGlskShiftKeys().get(0)).getOrder() !=
                ((CseGlskShiftKey) glskPointList.get(0).getGlskShiftKeys().get(1)).getOrder();
    }

    @Override
    public ZonalData<Scalable> getZonalScalable(Network network, Instant instant) {
        throw new NotImplementedException("CSE GLSK document does only support hourly data");
    }

    @Override
    public ZonalDataChronology<Scalable> getZonalScalableChronology(Network network) {
        throw new NotImplementedException("CSE GLSK document does only support hourly data");
    }
}
