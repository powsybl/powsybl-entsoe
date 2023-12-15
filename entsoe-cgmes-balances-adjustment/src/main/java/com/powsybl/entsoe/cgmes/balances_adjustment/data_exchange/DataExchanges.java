/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.entsoe.cgmes.balances_adjustment.data_exchange;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.powsybl.commons.PowsyblException;
import com.powsybl.timeseries.*;
import org.threeten.extra.Interval;

import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 *  Pan European Verification Function (PEVF) &
 *  Common Grid Model Alignment (CGMA)
 *  data.
 *
 * @author Thomas Adam {@literal <tadam at silicom.fr>}
 */
public class DataExchanges {

    // RequireNonNull messages
    private static final String INSTANT_CANNOT_BE_NULL = "Instant cannot be null";
    private static final String ID_CANNOT_BE_NULL = "TimeSeriesId cannot be null";
    private static final String IDS_CANNOT_BE_NULL = "TimeSeriesIds cannot be null";

    /** Document identification. */
    private final String mRID;
    /** Version of the document. */
    private final int revisionNumber;
    /** The coded type of a document. The document type describes the principal characteristic of the document. */
    private final StandardMessageType type;
    /** The identification of the nature of process that the
     document addresses. */
    private final StandardProcessType processType;
    /** The identification of the sender */
    private final String senderId;
    private final StandardCodingSchemeType senderCodingScheme;
    /** The identification of the role played by a market player. */
    private final StandardRoleType senderMarketRole;
    /** The identification of a party in the energy market. */
    private final String receiverId;
    private final StandardCodingSchemeType receiverCodingScheme;
    /** The identification of the role played by the a market player. */
    private final StandardRoleType receiverMarketRole;
    /** The date and time of the creation of the document. */
    private final ZonedDateTime creationDate;
    /** This information provides the start and end date and time of the period covered by the document. */
    private final Interval period;

    // Optional data
    /** The identification of an individually predefined dataset in a
     data base system (e. g. Verification Platform). */
    private final String datasetMarketDocumentMRId;
    /** The identification of the condition or position of the document with regard to its standing. A document may be intermediate or final. */
    private final StandardStatusType docStatus;
    /** The optimisation area of concern. */
    private final String domainId;
    private final StandardCodingSchemeType domainCodingScheme;

    // Time Series
    private final BiMap<String, DoubleTimeSeries> timeSeriesById = HashBiMap.create();

    DataExchanges(String mRID, int revisionNumber, StandardMessageType type, StandardProcessType processType,
                  String senderId, StandardCodingSchemeType senderCodingScheme, StandardRoleType senderMarketRole,
                  String receiverId, StandardCodingSchemeType receiverCodingScheme, StandardRoleType receiverMarketRole,
                  ZonedDateTime creationDate, Interval period, String datasetMarketDocumentMRId, StandardStatusType docStatus, Map<String, StoredDoubleTimeSeries> timeSeriesById,
                  String domainId, StandardCodingSchemeType domainCodingScheme) {
        this.mRID = Objects.requireNonNull(mRID, "mRID is missing");
        this.revisionNumber = checkRevisionNumber(revisionNumber);
        this.type = Objects.requireNonNull(type, "StandardMessageType is missing");
        this.processType = Objects.requireNonNull(processType, "StandardMessageType is missing");
        this.senderId = Objects.requireNonNull(senderId, "Sender mRID is missing");
        this.senderCodingScheme = Objects.requireNonNull(senderCodingScheme, "Sender codingScheme is missing");
        this.senderMarketRole = Objects.requireNonNull(senderMarketRole, "Sender role is missing");
        this.receiverId = Objects.requireNonNull(receiverId, "Receiver mRID is missing");
        this.receiverCodingScheme = Objects.requireNonNull(receiverCodingScheme, "Receiver codingScheme is missing");
        this.receiverMarketRole = Objects.requireNonNull(receiverMarketRole, "Receiver role is missing");
        this.creationDate = Objects.requireNonNull(creationDate, "Creation DateTime is missing");
        this.period = Objects.requireNonNull(period, "Time interval is missing");
        this.timeSeriesById.putAll(Objects.requireNonNull(timeSeriesById));
        // Optional data
        this.datasetMarketDocumentMRId = datasetMarketDocumentMRId;
        this.docStatus = docStatus;
        this.domainId = domainId;
        this.domainCodingScheme = domainCodingScheme;
    }

    // MarketDocument metadata
    public String getMRId() {
        return mRID;
    }

    public int getRevisionNumber() {
        return revisionNumber;
    }

    public StandardMessageType getType() {
        return type;
    }

    public StandardProcessType getProcessType() {
        return processType;
    }

    public String getSenderId() {
        return senderId;
    }

    public StandardCodingSchemeType getSenderCodingScheme() {
        return senderCodingScheme;
    }

    public StandardRoleType getSenderMarketRole() {
        return senderMarketRole;
    }

    public String getReceiverId() {
        return receiverId;
    }

    public StandardCodingSchemeType getReceiverCodingScheme() {
        return receiverCodingScheme;
    }

    public StandardRoleType getReceiverMarketRole() {
        return receiverMarketRole;
    }

    public ZonedDateTime getCreationDate() {
        return creationDate;
    }

    public Interval getPeriod() {
        return period;
    }

    // Optional metadata
    public Optional<String> getDatasetMarketDocumentMRId() {
        return Optional.ofNullable(datasetMarketDocumentMRId);
    }

    Optional<StandardStatusType> getDocStatus() {
        return Optional.ofNullable(docStatus);
    }

    public Optional<String> getDomainId() {
        return Optional.ofNullable(domainId);
    }

    public Optional<StandardCodingSchemeType> getDomainCodingScheme() {
        return Optional.ofNullable(domainCodingScheme);
    }

    // Utilities
    public Collection<DoubleTimeSeries> getTimeSeries() {
        return Collections.unmodifiableCollection(timeSeriesById.values());
    }

    public DoubleTimeSeries getTimeSeries(String timeSeriesId) {
        Objects.requireNonNull(timeSeriesId, ID_CANNOT_BE_NULL);
        if (!timeSeriesById.containsKey(timeSeriesId)) {
            throw new PowsyblException(String.format("TimeSeries '%s' not found", timeSeriesId));
        }
        return timeSeriesById.get(timeSeriesId);
    }

    public Stream<DoubleTimeSeries> getTimeSeriesWithDomainId(String inOutDomainId) {
        Objects.requireNonNull(inOutDomainId);

        return getTimeSeries().stream().filter(t -> {
            Map<String, String> tags = t.getMetadata().getTags();
            return inOutDomainId.equalsIgnoreCase(tags.get(DataExchangesConstants.IN_DOMAIN + "." + DataExchangesConstants.MRID)) ||
                    inOutDomainId.equalsIgnoreCase(tags.get(DataExchangesConstants.OUT_DOMAIN + "." + DataExchangesConstants.MRID));
        });
    }

    public Stream<DoubleTimeSeries> getTimeSeriesStream(String inDomainId, String outDomainId) {
        Objects.requireNonNull(inDomainId);
        Objects.requireNonNull(outDomainId);

        return getTimeSeries().stream().filter(t -> {
            Map<String, String> tags = t.getMetadata().getTags();
            return inDomainId.equalsIgnoreCase(tags.get(DataExchangesConstants.IN_DOMAIN + "." + DataExchangesConstants.MRID)) &&
                    outDomainId.equalsIgnoreCase(tags.get(DataExchangesConstants.OUT_DOMAIN + "." + DataExchangesConstants.MRID));
        });
    }

    public Map<String, Double> getNetPositionsWithInDomainId(String inDomainId, Instant instant) {
        return getNetPositionsWithInDomainId(inDomainId, instant, true);
    }

    public Map<String, Double> getNetPositionsWithInDomainId(String inDomainId, Instant instant, boolean exceptionOutOfBound) {
        return getTimeSeriesWithDomainId(inDomainId)
                .map(doubleTimeSeries -> {
                    Map<String, String> tags = doubleTimeSeries.getMetadata().getTags();
                    String tmpInDomainId = tags.get(DataExchangesConstants.IN_DOMAIN + "." + DataExchangesConstants.MRID);
                    String tmpOutDomainId = tags.get(DataExchangesConstants.OUT_DOMAIN + "." + DataExchangesConstants.MRID);
                    if (tmpInDomainId.equals(inDomainId)) {
                        return tmpOutDomainId;
                    } else if (tmpOutDomainId.equals(inDomainId)) {
                        return tmpInDomainId;
                    }
                    throw new AssertionError();
                })
                .distinct()
                .collect(Collectors.toMap(Function.identity(), outDomainId -> getNetPosition(inDomainId, outDomainId, instant, exceptionOutOfBound)));
    }

    public List<DoubleTimeSeries> getTimeSeries(String inDomainId, String outDomainId) {
        return getTimeSeriesStream(inDomainId, outDomainId).collect(Collectors.toList());
    }

    public double getNetPosition(String inDomainId, String outDomainId, Instant instant) {
        return getNetPosition(inDomainId, outDomainId, instant, true);
    }

    public double getNetPosition(String inDomainId, String outDomainId, Instant instant, boolean exceptionOutOfBound) {
        return getValuesAt(inDomainId, outDomainId, instant, exceptionOutOfBound).values().stream().reduce(0d, Double::sum)
                - getValuesAt(outDomainId, inDomainId, instant, exceptionOutOfBound).values().stream().reduce(0d, Double::sum);
    }

    public Map<String, Double> getValuesAt(Instant instant) {
        Objects.requireNonNull(instant, INSTANT_CANNOT_BE_NULL);

        return timeSeriesById.keySet().stream()
                .collect(Collectors.toMap(id -> id, id -> getValueAt(getTimeSeries(id), instant, true)));
    }

    public double getValueAt(String timeSeriesId, Instant instant) {
        return getValueAt(timeSeriesId, instant, true);
    }

    public double getValueAt(String timeSeriesId, Instant instant, boolean exceptionOutOfBound) {
        Objects.requireNonNull(instant, INSTANT_CANNOT_BE_NULL);

        final DoubleTimeSeries timeSeries = getTimeSeries(timeSeriesId);
        return getValueAt(timeSeries, instant, exceptionOutOfBound);
    }

    public Map<String, Double> getValuesAt(List<String> timeSeriesIds, Instant instant) {
        Objects.requireNonNull(timeSeriesIds, IDS_CANNOT_BE_NULL);
        Objects.requireNonNull(instant, INSTANT_CANNOT_BE_NULL);

        return timeSeriesIds.stream()
                .collect(Collectors.toMap(id -> id, id -> getValueAt(getTimeSeries(id), instant, true)));
    }

    public Map<String, Double> getValuesAt(String inDomainId, String outDomainId, Instant instant) {
        return getValuesAt(inDomainId, outDomainId, instant, true);
    }

    private Map<String, Double> getValuesAt(String inDomainId, String outDomainId, Instant instant, boolean exceptionOutOfBound) {
        Objects.requireNonNull(inDomainId);
        Objects.requireNonNull(outDomainId);
        Objects.requireNonNull(instant, INSTANT_CANNOT_BE_NULL);

        return getTimeSeriesStream(inDomainId, outDomainId).collect(Collectors.toMap(t -> timeSeriesById.inverse().get(t), t -> getValueAt(t, instant, exceptionOutOfBound)));
    }

    /**
     * @deprecated Use {@link #getValuesAt(String[], Instant)} instead.
     */
    @Deprecated
    public Map<String, Double> getValueAt(String[] timeSeriesIds, Instant instant) {
        return getValuesAt(timeSeriesIds, instant);
    }

    public Map<String, Double> getValuesAt(String[] timeSeriesIds, Instant instant) {
        return getValuesAt(Arrays.asList(timeSeriesIds), instant);
    }

    private double getValueAt(DoubleTimeSeries timeSeries, Instant instant, boolean exceptionOutOfBound) {
        RegularTimeSeriesIndex index = (RegularTimeSeriesIndex) timeSeries.getMetadata().getIndex();
        var start = Instant.ofEpochMilli(index.getStartTime());
        var end = Instant.ofEpochMilli(index.getEndTime());

        if (instant.isBefore(start) || instant.isAfter(end) || instant.equals(end)) {
            if (exceptionOutOfBound) {
                throw new PowsyblException(String.format("%s '%s' is out of bound [%s, %s[", timeSeries.getMetadata().getName(), instant, start, end));
            }
            return 0;
        } else {
            long spacing = index.getSpacing();
            var elapsed = Duration.between(start, instant);
            long point = elapsed.toMillis() / spacing;
            return timeSeries.toArray()[(int) point];
        }
    }

    private static int checkRevisionNumber(int revisionNumber) {
        if (revisionNumber < 0 || revisionNumber > 100) {
            throw new IllegalArgumentException("Bad revision number value " + revisionNumber);
        }
        return revisionNumber;
    }
}
