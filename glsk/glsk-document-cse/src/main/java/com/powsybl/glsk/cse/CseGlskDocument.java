/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.glsk.cse;

import com.powsybl.glsk.api.GlskDocument;
import com.powsybl.glsk.api.GlskPoint;
import com.powsybl.glsk.api.util.converters.GlskPointScalableConverter;
import com.powsybl.glsk.commons.CountryEICode;
import com.powsybl.glsk.commons.GlskException;
import com.powsybl.glsk.commons.ZonalData;
import com.powsybl.glsk.commons.ZonalDataChronology;
import com.powsybl.glsk.commons.ZonalDataImpl;
import com.powsybl.iidm.modification.scalable.Scalable;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import com.powsybl.sensitivity.SensitivityVariableSet;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.JAXBIntrospector;
import jakarta.xml.bind.Unmarshaller;
import org.apache.commons.lang3.NotImplementedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import java.io.InputStream;
import java.net.URL;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 * @author Vincent BOCHET {@literal <vincent.bochet at rte-france.com>}
 */
public final class CseGlskDocument implements GlskDocument {
    private static final Logger LOGGER = LoggerFactory.getLogger(CseGlskDocument.class);
    private static final String DATA_CHRONOLOGY_NOT_HANDLED = "CSE GLSK document does only support hourly data";
    private static final String COUNTRIES_IN_AREA_KEY = "countriesInArea";
    private static final String COUNTRIES_OUT_AREA_KEY = "countriesOutArea";

    /**
     * list of GlskPoint in the given Glsk document
     */
    private final Map<String, List<GlskPoint>> cseGlskPoints = new TreeMap<>();

    public static CseGlskDocument importGlsk(InputStream inputStream, boolean useCalculationDirections, boolean validateSchema) {
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(ObjectFactory.class);
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();

            if (validateSchema) {
                // Setup schema validator
                SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
                URL glskSchemaResource = CseGlskDocument.class.getResource("/xsd/gsk-document.xsd");
                if (glskSchemaResource != null) {
                    Schema glskSchema = sf.newSchema(glskSchemaResource);
                    unmarshaller.setSchema(glskSchema);
                } else {
                    LOGGER.warn("Unable to find GLSK Schema definition file. GLSK file will be imported without schema validation.");
                }
            }
            // Unmarshal xml file
            GSKDocument nativeGskDocument = (GSKDocument) JAXBIntrospector.getValue(unmarshaller.unmarshal(inputStream));
            return new CseGlskDocument(nativeGskDocument, useCalculationDirections);
        } catch (SAXException e) {
            throw new GlskException("Unable to import CSE GLSK file: Schema validation failed.", e);
        } catch (JAXBException e) {
            throw new GlskException("Unable to import CSE GLSK file.", e);
        }
    }

    private CseGlskDocument(GSKDocument nativeGskDocument, boolean useCalculationDirections) {
        // Computation of "standard" and "export" timeseries
        Map<String, List<GlskPoint>> cseGlskPointsStandard = getGlskPointsFromTimeSeries(nativeGskDocument.getTimeSeries());
        Map<String, List<GlskPoint>> cseGlskPointsExport = getGlskPointsFromTimeSeries(nativeGskDocument.getTimeSeriesExport());

        if (!useCalculationDirections || calculationDirectionsAbsent(nativeGskDocument)) {
            // "default" mode : all countries are considered in full import (use TimeSeries for all)
            this.cseGlskPoints.putAll(cseGlskPointsStandard);
        } else {
            // Extract CalculationDirections
            List<CalculationDirectionType> calculationDirections = nativeGskDocument.getCalculationDirections().get(0).getCalculationDirection();
            Map<String, List<String>> countriesInAndOutArea = getCountriesInAndOutArea(calculationDirections);

            // Use data from cseGlskPointsStandard or cseGlskPointsExport depending on CalculationDirections
            fillGlskPointsForExportCorner(cseGlskPointsStandard, cseGlskPointsExport, countriesInAndOutArea);
        }
    }

    private static Map<String, List<GlskPoint>> getGlskPointsFromTimeSeries(List<TimeSeriesType> timeSeries) {
        Map<String, List<GlskPoint>> cseGlskPointsPerArea = new TreeMap<>();
        if (timeSeries == null) {
            return cseGlskPointsPerArea;
        }

        timeSeries.stream()
            .map(CseGlskPoint::new)
            .forEach(glskPoint -> {
                cseGlskPointsPerArea.computeIfAbsent(glskPoint.getSubjectDomainmRID(), area -> new ArrayList<>());
                cseGlskPointsPerArea.get(glskPoint.getSubjectDomainmRID()).add(glskPoint);
            });
        return cseGlskPointsPerArea;
    }

    private static boolean calculationDirectionsAbsent(GSKDocument nativeGskDocument) {
        return nativeGskDocument.getCalculationDirections() == null
            || nativeGskDocument.getCalculationDirections().isEmpty()
            || nativeGskDocument.getCalculationDirections().get(0).getCalculationDirection() == null
            || nativeGskDocument.getCalculationDirections().get(0).getCalculationDirection().isEmpty();
    }

    private static Map<String, List<String>> getCountriesInAndOutArea(List<CalculationDirectionType> calculationDirections) {
        String italyEIC = new CountryEICode(Country.IT).getCode();
        List<String> countriesInArea = new ArrayList<>();
        List<String> countriesOutArea = new ArrayList<>();

        calculationDirections.stream()
            .map(cd -> cd.getInArea().getV())
            .filter(countryEIC -> !countryEIC.equals(italyEIC))
            .forEach(countriesInArea::add);

        countriesInArea.add(italyEIC);

        calculationDirections.stream()
            .map(cd -> cd.getOutArea().getV())
            .filter(countryEIC -> !countryEIC.equals(italyEIC))
            .forEach(countriesOutArea::add);

        return Map.of(COUNTRIES_IN_AREA_KEY, countriesInArea,
            COUNTRIES_OUT_AREA_KEY, countriesOutArea);
    }

    private void fillGlskPointsForExportCorner(Map<String, List<GlskPoint>> cseGlskPointsStandard,
                                               Map<String, List<GlskPoint>> cseGlskPointsExport,
                                               Map<String, List<String>> countriesInAndOutArea) {
        countriesInAndOutArea.get(COUNTRIES_IN_AREA_KEY).forEach(eic -> {
            List<GlskPoint> cseGlskPoint = cseGlskPointsExport.getOrDefault(eic, cseGlskPointsStandard.get(eic));
            this.cseGlskPoints.computeIfAbsent(eic, area -> new ArrayList<>());
            this.cseGlskPoints.get(eic).addAll(cseGlskPoint);
        });

        countriesInAndOutArea.get(COUNTRIES_OUT_AREA_KEY).forEach(eic -> {
            List<GlskPoint> cseGlskPoint = cseGlskPointsStandard.get(eic);
            this.cseGlskPoints.computeIfAbsent(eic, area -> new ArrayList<>());
            this.cseGlskPoints.get(eic).addAll(cseGlskPoint);
        });
    }

    @Override
    public List<String> getZones() {
        return new ArrayList<>(cseGlskPoints.keySet());
    }

    @Override
    public List<GlskPoint> getGlskPoints(String zone) {
        return cseGlskPoints.getOrDefault(zone, Collections.emptyList());
    }

    @Override
    public ZonalData<SensitivityVariableSet> getZonalGlsks(Network network, Instant instant) {
        throw new NotImplementedException(DATA_CHRONOLOGY_NOT_HANDLED);
    }

    @Override
    public ZonalDataChronology<SensitivityVariableSet> getZonalGlsksChronology(Network network) {
        throw new NotImplementedException(DATA_CHRONOLOGY_NOT_HANDLED);
    }

    @Override
    public ZonalData<Scalable> getZonalScalable(Network network) {
        Map<String, Scalable> zonalData = new HashMap<>();
        for (Map.Entry<String, List<GlskPoint>> entry : cseGlskPoints.entrySet()) {
            String area = entry.getKey();
            // There is always only one GlskPoint for a zone
            GlskPoint zonalGlskPoint = entry.getValue().get(0);
            if (isHybridCseGlskPoint(zonalGlskPoint)) {
                //if we are here then there are two glskPoints, and we want to order them by putting the one with "order = 1" first
                List<Scalable> scalables = zonalGlskPoint.getGlskShiftKeys().stream()
                    .sorted(Comparator.comparingInt(sk -> ((CseGlskShiftKey) sk).getOrder() == 1 ? 0 : 1))
                    .map(sk -> GlskPointScalableConverter.convert(network, List.of(sk)))
                    .collect(Collectors.toList());
                zonalData.put(area, Scalable.upDown(Scalable.stack(scalables.get(0), scalables.get(1)), scalables.get(1)));
            } else {
                zonalData.put(area, GlskPointScalableConverter.convert(network, zonalGlskPoint));
            }
        }
        return new ZonalDataImpl<>(zonalData);
    }

    private boolean isHybridCseGlskPoint(GlskPoint zonalGlskPoint) {
        // If 2 shift are present, and one has order 1 and a maximum shift and is of type PropGlsk
        return zonalGlskPoint.getGlskShiftKeys().size() == 2 && zonalGlskPoint.getGlskShiftKeys().stream().filter(CseGlskShiftKey.class::isInstance).map(CseGlskShiftKey.class::cast)
                .anyMatch(cseGlskShiftKey -> cseGlskShiftKey.getOrder() == 1 && !Double.isNaN(cseGlskShiftKey.getMaximumShift()) && cseGlskShiftKey.getBusinessType().equals("B42"));
    }

    @Override
    public ZonalData<Scalable> getZonalScalable(Network network, Instant instant) {
        throw new NotImplementedException(DATA_CHRONOLOGY_NOT_HANDLED);
    }

    @Override
    public ZonalDataChronology<Scalable> getZonalScalableChronology(Network network) {
        throw new NotImplementedException(DATA_CHRONOLOGY_NOT_HANDLED);
    }
}
