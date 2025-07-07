/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.entsoe.cgmes.balances_adjustment.data_exchange;

import com.powsybl.commons.PowsyblException;
import com.powsybl.commons.exceptions.UncheckedXmlStreamException;
import com.powsybl.commons.xml.XmlUtil;
import com.powsybl.timeseries.*;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.threeten.extra.Interval;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.time.Duration;
import java.util.*;

/**
 * @deprecated This module has not either been maintained nor used. We will remove it soon. Please report on Slack if you are using it.
 * Pan European Verification Function (PEVF) &
 * Common Grid Model Alignment (CGMA)
 * XML parser.
 *
 * @author Thomas Adam {@literal <tadam at silicom.fr>}
 */
@Deprecated(since = "2.14", forRemoval = true)
public final class DataExchangesXml {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataExchangesXml.class);

    // Log messages
    private static final String UNEXPECTED_TOKEN = "Unexpected token: ";

    private static class ParsingContext {

        private final Map<String, StoredDoubleTimeSeries> timeSeriesById = new HashMap<>();

        private StandardStatusType docStatus;

        private String datasetMarketDocumentMRId;

        private Interval period;

        private ZonedDateTime creationDate;

        private StandardRoleType receiverMarketRole;

        private StandardCodingSchemeType receiverCodingScheme;

        private String receiverId;

        private StandardRoleType senderMarketRole;

        private StandardCodingSchemeType senderCodingScheme;

        private String senderId;

        private StandardCodingSchemeType domainCodingScheme;

        private String domainId;

        private StandardProcessType processType;

        private StandardMessageType type;

        private int revisionNumber;

        private String mRID;
    }

    private static class ParsingTimeSeriesContext {

        private String mRID;

        private Interval period;

        private Duration spacing;

        private final List<Integer> positions = new ArrayList<>();

        private final List<Double> quantities = new ArrayList<>();

        private final Map<String, String> tags = new HashMap<>();

        private String code;

        private String text;
    }

    public static DataExchanges parse(InputStream stream) {
        return parse(new InputStreamReader(stream));
    }

    public static DataExchanges parse(Reader reader) {
        Objects.requireNonNull(reader);
        var context = new ParsingContext();
        try {
            var factory = XmlUtil.getXMLInputFactory();
            var xmlReader = factory.createXMLStreamReader(reader);
            try {
                XmlUtil.readSubElements(xmlReader, subElementName -> {
                    try {
                        switch (xmlReader.getLocalName()) {

                            case DataExchangesConstants.MRID:
                                context.mRID = xmlReader.getElementText();
                                break;

                            case DataExchangesConstants.REVISION_NUMBER:
                                context.revisionNumber = Integer.parseInt(xmlReader.getElementText());
                                break;

                            case DataExchangesConstants.TYPE:
                                context.type = StandardMessageType.valueOf(xmlReader.getElementText());
                                break;

                            case DataExchangesConstants.PROCESS_TYPE:
                                context.processType = StandardProcessType.valueOf(xmlReader.getElementText());
                                break;

                            case DataExchangesConstants.SENDER_MARKET_PARTICIPANT + "." + DataExchangesConstants.MRID:
                                context.senderCodingScheme = StandardCodingSchemeType.valueOf(xmlReader.getAttributeValue(null, DataExchangesConstants.CODING_SCHEME));
                                context.senderId = xmlReader.getElementText();
                                break;

                            case DataExchangesConstants.SENDER_MARKET_PARTICIPANT + "." + DataExchangesConstants.MARKET_ROLE:
                                context.senderMarketRole = StandardRoleType.valueOf(xmlReader.getElementText());
                                break;

                            case DataExchangesConstants.RECEIVER_MARKET_PARTICIPANT + "." + DataExchangesConstants.MRID:
                                context.receiverCodingScheme = StandardCodingSchemeType.valueOf(xmlReader.getAttributeValue(null, DataExchangesConstants.CODING_SCHEME));
                                context.receiverId = xmlReader.getElementText();
                                break;

                            case DataExchangesConstants.RECEIVER_MARKET_PARTICIPANT + "." + DataExchangesConstants.MARKET_ROLE:
                                context.receiverMarketRole = StandardRoleType.valueOf(xmlReader.getElementText());
                                break;

                            case DataExchangesConstants.CREATION_DATETIME:
                                context.creationDate = ZonedDateTime.parse(xmlReader.getElementText());
                                break;

                            case DataExchangesConstants.TIME_PERIOD_INTERVAL:
                                context.period = readTimeInterval(xmlReader);
                                break;

                            case DataExchangesConstants.DOMAIN + "." + DataExchangesConstants.MRID:
                                context.domainCodingScheme = StandardCodingSchemeType.valueOf(xmlReader.getAttributeValue(null, DataExchangesConstants.CODING_SCHEME));
                                context.domainId = xmlReader.getElementText();
                                break;

                            case DataExchangesConstants.DATASET_MARKET_DOCUMENT + "." + DataExchangesConstants.MRID:
                                context.datasetMarketDocumentMRId = xmlReader.getElementText();
                                break;

                            case DataExchangesConstants.DOC_STATUS:
                                XmlUtil.readSubElements(xmlReader, subElementName1 -> {
                                    if (subElementName1.equals(DataExchangesConstants.VALUE)) {
                                        try {
                                            context.docStatus = StandardStatusType.valueOf(XmlUtil.readText(xmlReader));
                                        } catch (XMLStreamException e) {
                                            throw new UncheckedXmlStreamException(e);
                                        }
                                    }
                                });
                                break;

                            case DataExchangesConstants.TIMESERIES:
                                StoredDoubleTimeSeries timeSeries = readTimeSeries(xmlReader);
                                context.timeSeriesById.put(timeSeries.getMetadata().getName(), timeSeries);
                                break;

                            case DataExchangesConstants.ROOT:
                                // Explicit skip
                                break;

                            default:
                                throw new PowsyblException(UNEXPECTED_TOKEN + xmlReader.getLocalName());
                        }
                    } catch (XMLStreamException e) {
                        throw new UncheckedXmlStreamException(e);
                    }
                });
            } finally {
                xmlReader.close();
            }
        } catch (XMLStreamException e) {
            throw new UncheckedXmlStreamException(e);
        }
        // the attributes are checked in the constructor
        return new DataExchanges(context.mRID, context.revisionNumber, context.type, context.processType,
                                 context.senderId, context.senderCodingScheme, context.senderMarketRole,
                                 context.receiverId, context.receiverCodingScheme, context.receiverMarketRole,
                                 context.creationDate, context.period, context.datasetMarketDocumentMRId, context.docStatus, context.timeSeriesById,
                                 context.domainId, context.domainCodingScheme);
    }

    private static StoredDoubleTimeSeries readTimeSeries(XMLStreamReader xmlReader) throws UncheckedXmlStreamException {
        var context = new ParsingTimeSeriesContext();

        XmlUtil.readSubElements(xmlReader, subElementName -> {
            try {
                switch (subElementName) {
                    case DataExchangesConstants.MRID:
                        context.mRID = xmlReader.getElementText();
                        break;

                    case DataExchangesConstants.BUSINESS_TYPE:
                    case DataExchangesConstants.PRODUCT:
                    case DataExchangesConstants.CONNECTING_LINE_REGISTERED_RESOURCE + "." + DataExchangesConstants.MRID:
                    case DataExchangesConstants.MEASUREMENT_UNIT:
                    case DataExchangesConstants.CURVE_TYPE:
                    case DataExchangesConstants.MARKET_OBJECT_STATUS: // See CGMA Implementation Guide v2 : Not used
                        context.tags.put(xmlReader.getLocalName(), xmlReader.getElementText());
                        break;

                    case DataExchangesConstants.IN_DOMAIN + "." + DataExchangesConstants.MRID:
                    case DataExchangesConstants.OUT_DOMAIN + "." + DataExchangesConstants.MRID:
                        String codingScheme = xmlReader.getAttributeValue(null, DataExchangesConstants.CODING_SCHEME);
                        context.tags.put(xmlReader.getLocalName() + "." + DataExchangesConstants.CODING_SCHEME, codingScheme);
                        context.tags.put(xmlReader.getLocalName(), xmlReader.getElementText());
                        break;

                    case DataExchangesConstants.PERIOD:
                        readPeriod(xmlReader, context);
                        break;

                    case DataExchangesConstants.REASON:
                        readReason(xmlReader, context);
                        break;

                    default:
                        throw new PowsyblException(UNEXPECTED_TOKEN + xmlReader.getLocalName());
                }
            } catch (XMLStreamException e) {
                throw new UncheckedXmlStreamException(e);
            }
        });

        // Log TimeSeries Reason
        if (LOGGER.isInfoEnabled() && Objects.nonNull(context.code) && Objects.nonNull(context.text)) {
            LOGGER.info("TimeSeries '{}' [{}, {}] - {} ({}) : {}", context.mRID,
                                                                    context.period.getStart(), context.period.getEnd(),
                                                                    context.code,
                                                                    StandardReasonCodeType.valueOf(context.code).getDescription(),
                                                                    context.text);
        }

        // Create DataChunk
        DoubleDataChunk dataChunk;
        // Computed number of steps
        int nbSteps = (int) (context.period.toDuration().toNanos() / context.spacing.toNanos());
        // Check if all steps are defined or not
        if (context.positions.size() == nbSteps) {
            // Uncompressed chunk
            dataChunk = new UncompressedDoubleDataChunk(0, context.quantities.stream().mapToDouble(d -> d).toArray());
        } else {
            // Compressed chunk
            var stepLengths = new int[context.positions.size()];
            if (context.positions.size() > 1) {
                for (var i = 1; i < context.positions.size(); i++) {
                    int lastPosition = context.positions.get(i - 1);
                    int newPosition = context.positions.get(i);
                    stepLengths[i - 1] = newPosition - lastPosition;
                }
                // Last step is computed from nbSteps and last position value
                stepLengths[stepLengths.length - 1] = 1 + (nbSteps - context.positions.get(context.positions.size() - 1));
            } else {
                stepLengths[0] = nbSteps;
            }
            dataChunk = new CompressedDoubleDataChunk(0, nbSteps, context.quantities.stream().mapToDouble(d -> d).toArray(), stepLengths);
        }

        // Instantiate new time series
        TimeSeriesIndex index = RegularTimeSeriesIndex.create(context.period.getStart(), context.period.getEnd(), context.spacing);
        var metadata = new TimeSeriesMetadata(context.mRID, TimeSeriesDataType.DOUBLE, context.tags, index);
        // Add new time series into DataExchanges
        return new StoredDoubleTimeSeries(metadata, dataChunk);
    }

    private static void readPeriod(XMLStreamReader xmlReader, ParsingTimeSeriesContext context) throws UncheckedXmlStreamException {
        XmlUtil.readSubElements(xmlReader, subElementName -> {
            try {
                switch (subElementName) {
                    case DataExchangesConstants.RESOLUTION:
                        String resolution = xmlReader.getElementText();
                        context.spacing = Duration.parse(resolution);
                        break;

                    case DataExchangesConstants.TIME_INTERVAL:
                        context.period = readTimeInterval(xmlReader);
                        break;

                    case DataExchangesConstants.POINT:
                        readPoint(xmlReader, context);
                        break;

                    default:
                        throw new PowsyblException(UNEXPECTED_TOKEN + xmlReader.getLocalName());
                }
            } catch (XMLStreamException e) {
                throw new UncheckedXmlStreamException(e);
            }
        });
    }

    private static Interval readTimeInterval(XMLStreamReader xmlReader) throws UncheckedXmlStreamException {
        var interval = new Instant[2];
        XmlUtil.readSubElements(xmlReader, subElementName -> {
            try {
                switch (subElementName) {
                    case DataExchangesConstants.START :
                        interval[0] = OffsetDateTime.parse(xmlReader.getElementText()).toInstant();
                        break;

                    case DataExchangesConstants.END :
                        interval[1] = OffsetDateTime.parse(xmlReader.getElementText()).toInstant();
                        break;

                    default:
                        throw new PowsyblException(UNEXPECTED_TOKEN + xmlReader.getLocalName());
                }
            } catch (XMLStreamException e) {
                throw new UncheckedXmlStreamException(e);
            }
        });
        return Interval.of(interval[0], interval[1]);
    }

    private static void readPoint(XMLStreamReader xmlReader, ParsingTimeSeriesContext context) throws UncheckedXmlStreamException {
        XmlUtil.readSubElements(xmlReader, subElementName -> {
            try {
                switch (xmlReader.getLocalName()) {
                    case DataExchangesConstants.POSITION:
                        context.positions.add(Integer.parseInt(xmlReader.getElementText()));
                        break;

                    case DataExchangesConstants.QUANTITY:
                        context.quantities.add(Double.parseDouble(xmlReader.getElementText()));
                        break;

                    case DataExchangesConstants.POSFR_QUANTITY:
                    case DataExchangesConstants.NEGFR_QUANTITY:
                        // See CGMA Implementation Guide v2 : Not used
                        xmlReader.getElementText();
                        break;

                    default:
                        throw new PowsyblException(UNEXPECTED_TOKEN + xmlReader.getLocalName());
                }
            } catch (XMLStreamException e) {
                throw new UncheckedXmlStreamException(e);
            }
        });
    }

    private static void readReason(XMLStreamReader xmlReader, ParsingTimeSeriesContext context) throws UncheckedXmlStreamException {
        XmlUtil.readSubElements(xmlReader, subElementName -> {
            try {
                switch (xmlReader.getLocalName()) {
                    case DataExchangesConstants.CODE:
                        context.code = xmlReader.getElementText();
                        break;

                    case DataExchangesConstants.TEXT:
                        context.text = xmlReader.getElementText();
                        break;

                    default:
                        throw new PowsyblException(UNEXPECTED_TOKEN + xmlReader.getLocalName());
                }
            } catch (XMLStreamException e) {
                throw new UncheckedXmlStreamException(e);
            }
        });
    }

    private DataExchangesXml() {
    }
}
