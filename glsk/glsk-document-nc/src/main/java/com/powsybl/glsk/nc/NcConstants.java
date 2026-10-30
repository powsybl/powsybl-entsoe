/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.glsk.nc;

/**
 * @author Víctor Cardozo {@literal <victor.cardozo at artelys.com>}
 */
public final class NcConstants {

    private NcConstants() {
    }

    /**
     * constants to read rdf files
     */

    public static final String RDF_BASE_URL = "http://entsoe.eu";

    /**
     * namespace of the NC profiles, which the URL of the literals of its enumerations starts with
     */
    public static final String ENTSOE_NS_NC_URL = "http://entsoe.eu/ns/nc";

    public static final String TRIPLESTORE_RDF4J_NAME = "rdf4j";

    /**
     * Property of the generators created by the CGMES import, holding the mRID of their GeneratingUnit. The
     * GeneratingUnit referenced by the SIS profile is the one of the CGMES EQ profile, whereas the id of an IIDM
     * generator is the one of its SynchronousMachine.
     */
    public static final String PROPERTY_GENERATING_UNIT = "CGMES.GeneratingUnit";

    /**
     * constants to access triplestore data
     */

    public static final String SPARQL_FILE_NC_PROFILE = "nc.sparql";

    /**
     * names of the queries of the file
     */

    public static final String QUERY_COUNTERTRADE_REMEDIAL_ACTIONS = "countertradeRemedialActions";
    public static final String QUERY_POWER_BID_SCHEDULES = "powerBidSchedules";
    public static final String QUERY_GLSK_SCHEDULES = "glskSchedules";
    public static final String QUERY_PARTICIPATION_FACTOR_TIME_POINTS = "participationFactorTimePoints";
    public static final String QUERY_POWER_BID_SCHEDULE_TIME_POINTS = "powerBidScheduleTimePoints";
    public static final String QUERY_HEADERS = "headers";
    public static final String QUERY_AVAILABILITY_TIME_POINTS = "availabilityTimePoints";
    public static final String QUERY_REMEDIAL_ACTION_AVAILABLE = "remedialActionAvailable";

    /**
     * attributes of the results of the queries
     */

    public static final String REMEDIAL_ACTION = "ra";
    public static final String POWER_BID_SCHEDULE = "pbs";
    public static final String POWER_BID_SCHEDULE_ID = "pbsID";
    public static final String AT_TIME = "atTime";
    public static final String MRID = "mRID";
    public static final String INTERPOLATION_KIND = "interpolationKind";
    public static final String GLSK_SCHEDULE = "glskSchedule";
    public static final String OVERRIDE_AVAILABLE = "available";
    public static final String NORMAL_AVAILABLE = "normalAvailable";
    public static final String RESOURCE = "resource";
    public static final String SHIFT_METHOD = "shiftMethod";
    public static final String GLSK_STRATEGY = "glskStrategy";
    public static final String SCHEDULE = "schedule";
    public static final String PRICE = "price";
    public static final String POWER = "power";
    public static final String PARTICIPATION_FACTOR = "participationFactor";
    public static final String OPERATOR = "operator";
    public static final String CREATOR = "creator";
    public static final String BIDDING_ZONE = "biddingZone";
    public static final String DIRECTION = "direction";

    /**
     * constants to request file headers
     */

    public static final String REQUEST_HEADER_START_DATE = "startDate";

    public static final String REQUEST_HEADER_END_DATE = "endDate";

}
