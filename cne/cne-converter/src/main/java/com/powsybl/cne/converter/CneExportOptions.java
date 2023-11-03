/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.cne.converter;

/**
 * @author Thomas Adam {@literal <tadam at silicom.fr>}
 */
class CneExportOptions {

    // String equivalent to cne.export.xml.mRID
    private String mRID;

    // String equivalent to cne.export.xml.sender_MarketParticipant
    private String senderMarketParticipantMRID;

    // String equivalent to cne.export.xml.receiver_MarketParticipant
    private String receiverMarketParticipantMRID;

    // String equivalent to cne.export.xml.TimeSeries.mRID
    private String timeSeriesMRID;

    // String equivalent to cne.export.xml.in_Domain.mRID
    private String inDomainMRID;

    // String equivalent to cne.export.xml.out_Domain.mRID
    private String outDomainMRID;

    // String equivalent to cne.export.xml.createdDateTime
    private String createdDatetime;

    // String equivalent to cne.export.xml.time_Period.timeInterval.start
    private String timePeriodStart;

    // String equivalent to cne.export.xml.time_Period.timeInterval.end
    private String timePeriodEnd;

    public String getMRID() {
        return mRID;
    }

    public CneExportOptions setMRID(String mRID) {
        this.mRID = mRID;
        return this;
    }

    public String getSenderMarketParticipantMRID() {
        return senderMarketParticipantMRID;
    }

    public CneExportOptions setSenderMarketParticipantMRID(String senderMarketParticipantMRID) {
        this.senderMarketParticipantMRID = senderMarketParticipantMRID;
        return this;
    }

    public String getReceiverMarketParticipantMRID() {
        return receiverMarketParticipantMRID;
    }

    public CneExportOptions setReceiverMarketParticipantMRID(String receiverMarketParticipantMRID) {
        this.receiverMarketParticipantMRID = receiverMarketParticipantMRID;
        return this;
    }

    public String getTimeSeriesMRID() {
        return timeSeriesMRID;
    }

    public CneExportOptions setTimeSeriesMRID(String timeSeriesMRID) {
        this.timeSeriesMRID = timeSeriesMRID;
        return this;
    }

    public String getInDomainMRID() {
        return inDomainMRID;
    }

    public CneExportOptions setInDomainMRID(String inDomainMRID) {
        this.inDomainMRID = inDomainMRID;
        return this;
    }

    public String getOutDomainMRID() {
        return outDomainMRID;
    }

    public CneExportOptions setOutDomainMRID(String outDomainMRID) {
        this.outDomainMRID = outDomainMRID;
        return this;
    }

    public String getCreatedDatetime() {
        return createdDatetime;
    }

    public CneExportOptions setCreatedDatetime(String createdDatetime) {
        this.createdDatetime = createdDatetime;
        return this;
    }

    public String getTimePeriodStart() {
        return timePeriodStart;
    }

    public CneExportOptions setTimePeriodStart(String timePeriodStart) {
        this.timePeriodStart = timePeriodStart;
        return this;
    }

    public String getTimePeriodEnd() {
        return timePeriodEnd;
    }

    public CneExportOptions setTimePeriodEnd(String timePeriodEnd) {
        this.timePeriodEnd = timePeriodEnd;
        return this;
    }
}
