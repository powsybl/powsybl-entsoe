/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.cne.converter;

/**
 * @author Thomas Adam <tadam at silicom.fr>
 */
class CneExportOptions {

    // String equivalent to cne.export.xml.mRID
    private String mRID;

    private String senderMarketParticipantMRID;

    private String receiverMarketParticipantMRID;

    private String timeSeriesMRID;

    private String inDomainMRID;

    private String outDomainMRID;

    private String createdDatetime;

    private String timePeriodStart;

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
