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
public final class CneConstants {

    public static final String INDENT = "    ";

    public static final String ROOT_ELEMENT = "CriticalNetworkElement_MarketDocument";

    // Main token/attributes
    public static final String CODING_SCHEME = "codingScheme";
    public static final String MRID = "mRID";
    public static final String REVISION_NUMBER = "revisionNumber";
    public static final String TYPE = "type";
    public static final String PROCESS_PROCESS_TYPE = "process.processType";
    private static final String SENDER_MARKET_PARTICIPANT = "sender_MarketParticipant";
    public static final String SENDER_MARKET_PARTICIPANT_MRID = SENDER_MARKET_PARTICIPANT + "." + MRID;
    public static final String SENDER_MARKET_PARTICIPANT_TYPE = SENDER_MARKET_PARTICIPANT + "." + "marketRole" + "." + TYPE;
    private static final String RECEIVER_MARKET_PARTICIPANT = "receiver_MarketParticipant";
    public static final String RECEIVER_MARKET_PARTICIPANT_MRID = RECEIVER_MARKET_PARTICIPANT + "." + MRID;
    public static final String RECEIVER_MARKET_PARTICIPANT_TYPE = RECEIVER_MARKET_PARTICIPANT + "." + "marketRole" + "." + TYPE;
    public static final String CREATED_DATETIME = "createdDateTime";
    public static final String TIME_PERIOD = "time_Period";
    public static final String TIME_INTERVAL = "timeInterval";
    public static final String START = "start";
    public static final String END = "end";

    // TimeSeries
    public static final String PERIOD = "Period";
    public static final String TIME_SERIES = "TimeSeries";
    public static final String TIME_SERIES_MRID = CneConstants.TIME_SERIES + "." + CneConstants.MRID;
    public static final String BUSINESS_TYPE = "businessType";
    public static final String CURVE_TYPE = "curveType";

    public static final String RESOLUTION = "resolution";
    public static final String POINT = "Point";
    public static final String POSITION = "position";
    public static final String CONSTRAINT_SERIES = "Constraint_Series";

    // Monitored_RegisteredResource
    public static final String MONITORED_REGISTERED_RESOURCE = "Monitored_RegisteredResource";
    public static final String NAME = "name";
    public static final String IN_DOMAIN_MRID = "in_Domain" + "." + MRID;
    public static final String OUT_DOMAIN_MRID = "out_Domain" + "." + MRID;

    // Measurements
    public static final String MEASUREMENTS = "Measurements";
    public static final String MEASUREMENT_TYPE = "measurementType";
    public static final String UNIT_SYMBOL = "unitSymbol";
    public static final String ANALOG_VALUES_VALUE = "analogValues.value";

    // Contingency_Series
    public static final String CONTINGENCY_SERIES = "Contingency_Series";
    public static final String REGISTERED_RESOURCE = "RegisteredResource";

    private CneConstants() {
    }
}
