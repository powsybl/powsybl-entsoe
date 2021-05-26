/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.entsoe.cgmes.balances_adjustment.data_exchange;

/**
 * Pan European Verification Function (PEVF) &
 * Common Grid Model Alignment (CGMA)
 * XML parser tokens.
 *
 * @author Thomas Adam {@literal <tadam at silicom.fr>}
 */
public final class DataExchangesConstants {

    // Metadata
    static final String ROOT = "ReportingInformation_MarketDocument";
    static final String MRID = "mRID";
    static final String REVISION_NUMBER = "revisionNumber";
    static final String TYPE = "type";
    static final String PROCESS_TYPE = "process.processType";
    static final String SENDER_MARKET_PARTICIPANT = "sender_MarketParticipant";
    static final String CODING_SCHEME = "codingScheme";
    static final String MARKET_ROLE = "marketRole.type";
    static final String RECEIVER_MARKET_PARTICIPANT = "receiver_MarketParticipant";
    static final String CREATION_DATETIME = "createdDateTime";
    static final String TIME_PERIOD_INTERVAL = "time_Period.timeInterval";
    static final String START = "start";
    static final String END = "end";
    static final String DOMAIN = "domain";
    static final String DATASET_MARKET_DOCUMENT = "dataset_MarketDocument";
    static final String DOC_STATUS = "docStatus";
    static final String VALUE = "value";
    // TimeSeries
    static final String TIMESERIES = "TimeSeries";
    static final String BUSINESS_TYPE = "businessType";
    static final String PRODUCT = "product";
    static final String IN_DOMAIN = "in_Domain";
    static final String OUT_DOMAIN = "out_Domain";
    static final String CONNECTING_LINE_REGISTERED_RESOURCE = "connectingLine_RegisteredResource";
    static final String MEASUREMENT_UNIT = "measurement_Unit.name";
    static final String CURVE_TYPE = "curveType";
    static final String PERIOD = "Period";
    static final String RESOLUTION = "resolution";
    static final String TIME_INTERVAL = "timeInterval";
    static final String POINT = "Point";
    static final String POSITION = "position";
    static final String QUANTITY = "quantity";
    static final String REASON = "Reason";
    static final String CODE = "code";
    static final String TEXT = "text";
    // Not used
    static final String MARKET_OBJECT_STATUS = "marketObjectStatus.status";
    static final String POSFR_QUANTITY = "posFR_Quantity.quantity";
    static final String NEGFR_QUANTITY = "negFR_Quantity.quantity";

    private DataExchangesConstants() {

    }
}
