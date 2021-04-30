/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.cne.converter;

import com.powsybl.cne.converter.model.ContingencySeries;
import com.powsybl.cne.converter.model.Measurement;
import com.powsybl.cne.converter.model.MonitoredRegisteredResource;
import com.powsybl.cne.converter.model.RegisteredResource;
import com.powsybl.commons.exceptions.UncheckedXmlStreamException;
import com.powsybl.commons.xml.XmlUtil;
import com.powsybl.security.SecurityAnalysisResult;
import org.apache.commons.io.output.WriterOutputStream;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

/**
 * @author Thomas Adam <tadam at silicom.fr>
 */
final class SecurityAnalysisResultXml {

    private SecurityAnalysisResultXml() {
    }

    private static XMLStreamWriter initializeWriter(WriterOutputStream os) throws XMLStreamException {
        XMLStreamWriter writer = XmlUtil.initializeWriter(true, CneConstants.INDENT, os);
        writer.writeStartElement(CneConstants.ROOT_ELEMENT);
        writer.writeAttribute("xsi:schemaLocation", "iec62325-451-n-cne_v2_0.xsd");
        writer.writeDefaultNamespace("urn:iec62325.351:tc57wg16:451-n:cnedocument:2:0");
        writer.writeNamespace("xsi", "http://www.w3.org/2001/XMLSchema-instance");
        return writer;
    }

    private static void writeMainAttributes(SecurityAnalysisResultXmlWriterContext context) throws XMLStreamException {
        XMLStreamWriter writer = context.getWriter();
        Properties parameters = context.getParameters();
        // mRID
        String mRID = parameters.getProperty(CneConstants.MRID);
        writer.writeComment(" mRID proposal including hour in UTC and local time, only constraint is the limit of 35 characters ");
        writer.writeStartElement(CneConstants.MRID);
        writer.writeCharacters(mRID);
        writer.writeEndElement(); // mRID
        // revisionNumber
        writer.writeStartElement(CneConstants.REVISION_NUMBER);
        writer.writeCharacters(Integer.toString(1));
        writer.writeEndElement(); // revisionNumber
        // type
        writer.writeComment(" type B15 for \"Network constraint document\" : \"A document providing the network constraint situations used for the load flow studies. A network constraint situation includes contingencies, monitored elements and remedial actions.\" ");
        writer.writeStartElement(CneConstants.TYPE);
        writer.writeCharacters("B15");
        writer.writeEndElement(); // type
        // process.processType
        writer.writeComment(" processType A01 for \"Day-ahead\" ");
        writer.writeComment(" processType A40 for \"Intraday\" ");
        writer.writeComment(" processType A45 for \"Two days ahead\" ");
        writer.writeComment(" processType A14 for \"Forecast\" : \"The data contained in the document are to be handled in short, medium, long term forecasting process\" ");
        writer.writeComment(" processType A16 for \"Realised\" : \"The process for the treatment of realised data as opposed to forecast data.\" ");
        writer.writeStartElement(CneConstants.PROCESS_PROCESS_TYPE);
        writer.writeCharacters("A01");
        writer.writeEndElement(); // process.processType
        // sender_MarketParticipant
        // mRID
        writer.writeComment(" A36 for \"Capacity Coordinator\" and A04 for \"System Operator\" ");
        String sender = parameters.getProperty(CneConstants.SENDER_MARKET_PARTICIPANT_MRID);
        writer.writeStartElement(CneConstants.SENDER_MARKET_PARTICIPANT_MRID);
        writer.writeAttribute(CneConstants.CODING_SCHEME, "A01");
        writer.writeCharacters(sender);
        writer.writeEndElement(); // sender_MarketParticipant.mRID
        // type
        writer.writeStartElement(CneConstants.SENDER_MARKET_PARTICIPANT_TYPE);
        writer.writeCharacters("A36");
        writer.writeEndElement(); // sender_MarketParticipant.marketRole.type
        // receiver_MarketParticipant
        // mRID
        String receiver = parameters.getProperty(CneConstants.RECEIVER_MARKET_PARTICIPANT_MRID);
        writer.writeStartElement(CneConstants.RECEIVER_MARKET_PARTICIPANT_MRID);
        writer.writeAttribute(CneConstants.CODING_SCHEME, "A01");
        writer.writeCharacters(receiver);
        writer.writeEndElement(); // receiver_MarketParticipant.mRID
        // type
        writer.writeStartElement(CneConstants.RECEIVER_MARKET_PARTICIPANT_TYPE);
        writer.writeCharacters("A36");
        writer.writeEndElement(); // receiver_MarketParticipant.marketRole.type
        // createdDateTime
        Optional<String> createdDateTime = Optional.ofNullable(parameters.getProperty(CneConstants.CREATED_DATETIME));
        writer.writeStartElement(CneConstants.CREATED_DATETIME);
        writer.writeCharacters(createdDateTime.orElseGet(() -> formatDateTime(Instant.now())));
        writer.writeEndElement(); // createdDateTime
        // time_Period.timeInterval
        writer.writeComment(" optional fields: \"docStatus\", \"Received_MarketDocument\", \"Related_MarketDocument\" -> we could add those additional fields if needed ");
        writer.writeComment(" \"Related_MarketDocument\" could contains the name of the grid model considered ? ");
        writer.writeComment(" \"Related_MarketDocument\" could contains the filename of the CRAC document containing the list of Contingencies ? ");
        writer.writeComment(" time_Period.timeInterval could be reduced to 15 min length for SN security analysis ");
        writer.writeStartElement(CneConstants.TIME_PERIOD + "." + CneConstants.TIME_INTERVAL);
        writeTimeInterval(writer, parameters);
        writer.writeEndElement(); // time_Period.timeInterval
        // Last comments
        writer.writeComment(" optional fields: \"domain.mRID\" used for CC to specify the region, not really needed for CSA ");
    }

    private static void writeTimeInterval(XMLStreamWriter writer, Properties parameters) throws XMLStreamException {
        // start
        Optional<String> startDateTime = Optional.ofNullable(parameters.getProperty(CneConstants.TIME_PERIOD + "." + CneConstants.TIME_INTERVAL + "." + CneConstants.START));
        var now = Instant.now();
        writer.writeStartElement(CneConstants.START);
        writer.writeCharacters(startDateTime.orElseGet(() -> formatDateTime(now)));
        writer.writeEndElement(); // start
        // end
        Optional<String> endDateTime = Optional.ofNullable(parameters.getProperty(CneConstants.TIME_PERIOD + "." + CneConstants.TIME_INTERVAL + "." + CneConstants.END));
        writer.writeStartElement(CneConstants.END);
        writer.writeCharacters(endDateTime.orElseGet(() -> formatDateTime(now.plusMillis(3600L * 1000L))));
        writer.writeEndElement(); // end
    }

    private static void writeTimeSeries(SecurityAnalysisResultXmlWriterContext context) throws XMLStreamException {
        XMLStreamWriter writer = context.getWriter();
        Properties parameters = context.getParameters();
        writer.writeComment(" One timeseries to provide ");
        writer.writeStartElement(CneConstants.TIME_SERIES);
        writer.writeComment(" mRID convention to be agreed, could include additional information ");
        // mRID
        String mRID = parameters.getProperty(CneConstants.TIME_SERIES_MRID);
        writer.writeStartElement(CneConstants.MRID);
        writer.writeCharacters(mRID);
        writer.writeEndElement(); // mRID
        writer.writeComment(" B37 - Constraint situation : Constraint situation \"The timeseries describes the constraint situation for a given TimeInterval. A constraint situation can be: - composed of a list of network elements in outage associated for each outage to a list of network elements on which remedial actions have been carried out accordingly to contingency process  - or it can be an external constraint. ");
        writer.writeComment(" B54 - Network Constraint Situation : The TimeSeries describes the network elements to be taken into account to simulate a network constraint during the network load flow studies. The network situation includes the contingencies, the remedial actions, the monitored network elements and the potential additional constraints. ");
        // businessType
        writer.writeStartElement(CneConstants.BUSINESS_TYPE);
        writer.writeCharacters("B37");
        writer.writeEndElement(); // businessType
        writer.writeComment(" A01: sequential fixed size block. It maked no sense to use A03 ");
        // curveType
        writer.writeStartElement(CneConstants.CURVE_TYPE);
        writer.writeCharacters("A01");
        writer.writeEndElement(); // curveType
        // Period
        writer.writeStartElement(CneConstants.PERIOD);
        // timeInterval
        writer.writeStartElement(CneConstants.TIME_INTERVAL);
        writeTimeInterval(writer, parameters);
        writer.writeEndElement(); // timeInterval
        writer.writeComment(" resolutions should be reduced for security analysis of snapshot -> \"PT15M\" ");
        // resolution
        writer.writeStartElement(CneConstants.RESOLUTION);
        writer.writeCharacters("PT60M");
        writer.writeEndElement(); // resolution
        // Point
        writer.writeStartElement(CneConstants.POINT);
        // position
        writer.writeStartElement(CneConstants.POSITION);
        writer.writeCharacters(Integer.toString(1));
        writer.writeEndElement(); // position
        writer.writeComment(" One constraint series for N state flows and one per contingency with constraint ");
        // PreContingencies
        writePreContingencyResult(context);
        // PostContingencies
        writePostContingencyResult(context);
        writer.writeEndElement(); // Point
        writer.writeEndElement(); // Period
        writer.writeEndElement(); // TimeSeries
    }

    private static void writePreContingencyResult(SecurityAnalysisResultXmlWriterContext context) throws XMLStreamException {
        final XMLStreamWriter writer = context.getWriter();
        Properties parameters = context.getParameters();
        // Constraint_Series
        writer.writeStartElement(CneConstants.CONSTRAINT_SERIES);
        // mRID
        writer.writeComment(" this mRID will identify the CO ? only ");
        writer.writeStartElement(CneConstants.MRID);
        writer.writeCharacters("N State constraints");
        writer.writeEndElement(); // mRID
        writer.writeComment(" Mandatory - Check which business type is the most appropriate for reporting a constraint ");
        // businessType
        writer.writeStartElement(CneConstants.BUSINESS_TYPE);
        writer.writeCharacters("B56");
        writer.writeEndElement(); // businessType
        writer.writeComment(" No contingency in this one because N state constraints are reported ");
        // Monitored_RegisteredResource
        writeMonitoredRegisteredResource(writer, parameters, context.getPreMonitoredRegisteredResources());
        writer.writeEndElement(); // Constraint_Series
    }

    private static void writeMonitoredRegisteredResource(final XMLStreamWriter writer, Properties parameters, List<MonitoredRegisteredResource> equipments) throws XMLStreamException {
        // Monitored_RegisteredResource
        for (MonitoredRegisteredResource equipment : equipments) {
            writer.writeStartElement(CneConstants.MONITORED_REGISTERED_RESOURCE);
            // mRID
            writer.writeStartElement(CneConstants.MRID);
            writer.writeAttribute(CneConstants.CODING_SCHEME, "A02");
            writer.writeCharacters(equipment.getEquipmentId());
            writer.writeEndElement(); // mRID
            // Name
            writer.writeStartElement(CneConstants.NAME);
            writer.writeCharacters(equipment.getEquipmentName());
            writer.writeEndElement(); // Name
            writer.writeComment(" We can use in/out to be able to filter by constraint location ");
            // in_Domain.mRID
            writer.writeStartElement(CneConstants.IN_DOMAIN_MRID);
            writer.writeAttribute(CneConstants.CODING_SCHEME, "A01");
            String inDomainId = parameters.getProperty(CneConstants.IN_DOMAIN_MRID);
            writer.writeCharacters(inDomainId);
            writer.writeEndElement(); // in_Domain.mRID
            // out_Domain.mRID
            writer.writeStartElement(CneConstants.OUT_DOMAIN_MRID);
            writer.writeAttribute(CneConstants.CODING_SCHEME, "A01");
            String outDomainId = parameters.getProperty(CneConstants.OUT_DOMAIN_MRID);
            writer.writeCharacters(outDomainId);
            writer.writeEndElement(); // out_Domain.mRID
            writer.writeComment(" Some work to be done to decide which measurement and with which units to be included here : %, Amperes, ");
            // Measurements
            writeMeasurements(writer, equipment.getMeasurementList());
            writer.writeEndElement(); // Monitored_RegisteredResource
        }
    }

    private static void writeMeasurements(final XMLStreamWriter writer, List<Measurement> measurementList) throws XMLStreamException {
        for (Measurement m : measurementList) {
            // Measurements
            writer.writeStartElement(CneConstants.MEASUREMENTS);
            // measurementType
            writer.writeStartElement(CneConstants.MEASUREMENT_TYPE);
            writer.writeCharacters(m.getMeasurementType().name());
            writer.writeEndElement(); //measurementType
            // unitSymbol
            writer.writeStartElement(CneConstants.UNIT_SYMBOL);
            writer.writeCharacters(m.getUnitSymbol().name());
            writer.writeEndElement(); // unitSymbol
            // analogValues.value
            writer.writeStartElement(CneConstants.ANALOG_VALUES_VALUE);
            writer.writeCharacters(String.valueOf(m.getAnalogValue()));
            writer.writeEndElement(); // analogValues.value
            writer.writeEndElement(); // Measurements
        }
    }

    private static void writePostContingencyResult(SecurityAnalysisResultXmlWriterContext context) throws XMLStreamException {
        final XMLStreamWriter writer = context.getWriter();
        Properties parameters = context.getParameters();

        for (ContingencySeries key : context.getPostMonitoredRegisteredResources().keySet()) {
            // Constraint_Series
            writer.writeStartElement(CneConstants.CONSTRAINT_SERIES);
            // mRID
            writer.writeComment(" this mRID will identify the CO ? only ");
            writer.writeStartElement(CneConstants.MRID);
            writer.writeCharacters(key.getContingencyId());
            writer.writeEndElement(); // mRID
            writer.writeComment(" Mandatory - Check which business type is the most appropriate for reporting a constraint ");
            // businessType
            writer.writeStartElement(CneConstants.BUSINESS_TYPE);
            writer.writeCharacters("B56");
            writer.writeEndElement(); // businessType
            // Contingency_Series
            writer.writeStartElement(CneConstants.CONTINGENCY_SERIES);
            writer.writeComment(" Ensuring stability of mRID will be complex without an external Co Dictionary ");
            // mRID
            writer.writeStartElement(CneConstants.MRID);
            writer.writeCharacters(key.getContingencyId());
            writer.writeEndElement(); // mRID
            // Name
            writer.writeStartElement(CneConstants.NAME);
            writer.writeCharacters(key.getContingencyName());
            writer.writeEndElement(); // Name

            for (RegisteredResource registeredResource : key.getRegisteredResourceList()) {
                // RegisteredResource
                writer.writeStartElement(CneConstants.REGISTERED_RESOURCE);
                // mRID
                writer.writeStartElement(CneConstants.MRID);
                writer.writeAttribute(CneConstants.CODING_SCHEME, "A02");
                writer.writeCharacters(registeredResource.getId());
                writer.writeEndElement(); // mRID
                // Name
                writer.writeStartElement(CneConstants.NAME);
                writer.writeCharacters(registeredResource.getName());
                writer.writeEndElement(); // Name
                writer.writeEndElement(); // RegisteredResource
            }
            writer.writeEndElement(); // Contingency_Series
            // Monitored_RegisteredResource
            writeMonitoredRegisteredResource(writer, parameters, context.getPostMonitoredRegisteredResources().get(key));
            writer.writeEndElement(); // Constraint_Series
        }
    }

    public static void write(SecurityAnalysisResult result, Properties parameters, Writer writer) throws IOException {
        try (var os = new WriterOutputStream(writer, StandardCharsets.UTF_8)) {
            var context = new SecurityAnalysisResultXmlWriterContext(result, parameters, initializeWriter(os));
            // Write root metadata
            writeMainAttributes(context);
            // Write TimeSeries token
            writeTimeSeries(context);
            // Write last comments & closure tokens
            context.getWriter().writeComment(" optional class \"Reason\" could be used to provide additional information. Each reason node have a reason code and a reason text field (free text) ");
            context.getWriter().writeComment(" A95:Complementary information, B01:Incomplete document, B18:Failure, B27:Calculation process failed ");
            context.getWriter().writeEndElement();
            context.getWriter().writeEndDocument();
            context.getWriter().flush();
        } catch (XMLStreamException e) {
            throw new UncheckedXmlStreamException(e);
        }
    }

    private static String formatDateTime(Instant instant) {
        DateTimeFormatter formatter = DateTimeFormatter.ISO_INSTANT.withZone(ZoneId.from(ZoneOffset.UTC));
        // Remove milliseconds
        return formatter.format(instant.with(ChronoField.MILLI_OF_SECOND, 0));
    }
}
