/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.glsk.nc;

import com.powsybl.glsk.api.GlskPoint;
import com.powsybl.glsk.commons.GlskException;
import com.powsybl.triplestore.api.PropertyBag;
import com.powsybl.triplestore.api.PropertyBags;
import com.powsybl.triplestore.api.QueryCatalog;
import com.powsybl.triplestore.api.TripleStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.threeten.extra.Interval;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Parser of the GLSK information carried by the NC datasets (RA and SIS profiles).
 * Executes SPARQL queries and builds GLSK data structures.
 *
 * @author Víctor Cardozo {@literal <victor.cardozo at artelys.com>}
 */
public class NcGlskDatasetParser {

    private static final Logger LOGGER = LoggerFactory.getLogger(NcGlskDatasetParser.class);

    private final TripleStore tripleStore;
    private final QueryCatalog queryCatalog;
    private final Map<String, CountertradeRAData> countertradeRAs;
    private Instant validityStart;
    private Instant validityEnd;

    public NcGlskDatasetParser(TripleStore tripleStore) {
        this.tripleStore = tripleStore;
        this.queryCatalog = new QueryCatalog(NcConstants.SPARQL_FILE_NC_GLSK);
        this.countertradeRAs = new HashMap<>();
    }

    /**
     * Runs a query of the NC GLSK query catalog on the triple store.
     *
     * @param queryName name of the query in the catalog (nc-glsk.sparql)
     */
    private PropertyBags query(String queryName) {
        String queryText = queryCatalog.get(queryName);
        if (queryText == null) {
            throw new GlskException("Query [" + queryName + "] not found in " + NcConstants.SPARQL_FILE_NC_GLSK);
        }
        return tripleStore.query(queryText);
    }

    /**
     * Parse all NC GLSK data from the triplestore.
     * Returns a map of zone EIC codes to GlskPoints.
     */
    public Map<String, List<GlskPoint>> parse() {
        // Step 1: Parse headers to get validity dates
        parseHeaders();

        // Step 2: Parse CountertradeRemedialActions
        parseCountertradeRemedialActions();

        // Step 3: Parse PowerBidSchedules
        Map<String, PowerBidScheduleData> powerBidSchedules = parsePowerBidSchedules();

        // Step 4: Parse GLSKSchedules with GeneratingUnits, and link them to their PowerBidSchedule
        Map<String, GlskScheduleData> glskSchedules = parseGlskSchedules(powerBidSchedules);

        // Step 5: Parse ParticipationFactorTimePoints
        parseParticipationFactorTimePoints(glskSchedules);

        // Step 6: Parse PowerBidScheduleTimePoints (price and power)
        parsePowerBidScheduleTimePoints(powerBidSchedules);

        // Step 7: Build GlskPoints grouped by zone
        return buildGlskPointsByZone(powerBidSchedules);
    }

    /**
     * Computes the global validity window of the loaded profiles: from the earliest start date to the latest end date
     * of their headers.
     */
    private void parseHeaders() {
        for (PropertyBag header : query(NcConstants.QUERY_HEADERS)) {
            Instant start = parseInstant(header.get(NcConstants.REQUEST_HEADER_START_DATE));
            Instant end = parseInstant(header.get(NcConstants.REQUEST_HEADER_END_DATE));
            if (validityStart == null || start.isBefore(validityStart)) {
                validityStart = start;
            }
            if (validityEnd == null || end.isAfter(validityEnd)) {
                validityEnd = end;
            }
        }
    }

    private void parseCountertradeRemedialActions() {
        for (PropertyBag bag : query(NcConstants.QUERY_COUNTERTRADE_REMEDIAL_ACTIONS)) {
            String uri = bag.get("ra");
            String glskStrategyValue = bag.get("glskStrategy");
            String shiftMethodValue = parseKindName(bag.get("shiftMethod"));
            countertradeRAs.put(uri, new CountertradeRAData(
                uri,
                bag.get("mRID"),
                extractEICCode(bag.get("biddingZone")),
                glskStrategyValue,
                parseGlskStrategy(glskStrategyValue),
                shiftMethodValue,
                ShiftMethodKind.fromValue(shiftMethodValue).orElse(null),
                parseNullableInstant(bag.get(NcConstants.REQUEST_HEADER_START_DATE)),
                parseNullableInstant(bag.get(NcConstants.REQUEST_HEADER_END_DATE))
            ));
        }
    }

    private Map<String, PowerBidScheduleData> parsePowerBidSchedules() {
        Map<String, PowerBidScheduleData> result = new HashMap<>();
        for (PropertyBag bag : query(NcConstants.QUERY_POWER_BID_SCHEDULES)) {
            String uri = bag.get("pbs");
            result.put(uri, new PowerBidScheduleData(
                uri,
                bag.get("pbsID"),
                bag.get("ra"),
                parseKindName(bag.get("direction")),
                parseNullableInstant(bag.get(NcConstants.REQUEST_HEADER_START_DATE)),
                parseNullableInstant(bag.get(NcConstants.REQUEST_HEADER_END_DATE)),
                new TreeMap<>(),
                new ArrayList<>()
            ));
        }
        return result;
    }

    private Map<String, GlskScheduleData> parseGlskSchedules(Map<String, PowerBidScheduleData> powerBidSchedules) {
        Map<String, GlskScheduleData> result = new HashMap<>();
        for (PropertyBag bag : query(NcConstants.QUERY_GLSK_SCHEDULES)) {
            PowerBidScheduleData powerBidSchedule = powerBidSchedules.get(bag.get("pbs"));
            if (powerBidSchedule == null) {
                continue;
            }
            // The generating unit is identified by its mRID, as in the CGMES EQ profile
            GlskScheduleData glskSchedule = result.computeIfAbsent(bag.get("glskSchedule"),
                uri -> new GlskScheduleData(uri, bag.getId("generatingUnit"), new TreeMap<>()));
            powerBidSchedule.glskSchedules().add(glskSchedule);
        }
        return result;
    }

    private void parseParticipationFactorTimePoints(Map<String, GlskScheduleData> glskSchedules) {
        for (PropertyBag bag : query(NcConstants.QUERY_PARTICIPATION_FACTOR_TIME_POINTS)) {
            GlskScheduleData glskSchedule = glskSchedules.get(bag.get("glskSchedule"));
            if (glskSchedule != null) {
                glskSchedule.participationFactors().put(parseInstant(bag.get("atTime")), Double.parseDouble(bag.get("participationFactor")));
            }
        }
    }

    private void parsePowerBidScheduleTimePoints(Map<String, PowerBidScheduleData> powerBidSchedules) {
        for (PropertyBag bag : query(NcConstants.QUERY_POWER_BID_SCHEDULE_TIME_POINTS)) {
            PowerBidScheduleData powerBidSchedule = powerBidSchedules.get(bag.get("pbs"));
            if (powerBidSchedule != null) {
                powerBidSchedule.timePoints().put(parseInstant(bag.get("atTime")),
                    new PriceAndPower(parseNullableDouble(bag.get("price")), parseNullableDouble(bag.get("power"))));
            }
        }
    }

    private Map<String, List<GlskPoint>> buildGlskPointsByZone(Map<String, PowerBidScheduleData> powerBidSchedules) {
        Map<String, List<PowerBidScheduleData>> bidsByRa = powerBidSchedules.values().stream()
            .collect(Collectors.groupingBy(PowerBidScheduleData::remedialActionUri));
        Map<String, List<GlskPoint>> result = new HashMap<>();

        // The availability of the remedial action (normalAvailable, overridden by the SSI profile) is not checked here:
        // a GLSK only describes how a shift is distributed, whether the remedial action can be used is decided when the
        // remedial action itself is imported
        for (CountertradeRAData raData : countertradeRAs.values()) {
            if (raData.validityStart() == null || raData.validityEnd() == null
                || !raData.validityEnd().isAfter(raData.validityStart())) {
                LOGGER.warn("Countertrade remedial action {} is ignored: its profile has no valid validity window ({} - {})",
                    raData.mRID(), raData.validityStart(), raData.validityEnd());
                continue;
            }

            List<GlskPoint> points = buildGlskPoints(raData, bidsByRa.getOrDefault(raData.uri(), Collections.emptyList()));
            if (!points.isEmpty()) {
                result.computeIfAbsent(raData.biddingZone(), k -> new ArrayList<>()).addAll(points);
            }
        }

        return result;
    }

    /**
     * Builds the GLSK points of a countertrade remedial action according to its GLSK strategy. Only
     * proportionalForGenerator and explicitSchedule are supported, any other strategy is ignored.
     */
    private List<GlskPoint> buildGlskPoints(CountertradeRAData raData, List<PowerBidScheduleData> bids) {
        GlskStrategyKind strategy = raData.strategy();
        if (strategy == GlskStrategyKind.PROPORTIONAL_FOR_GENERATOR) {
            checkShiftMethod(raData, ShiftMethodKind.SHARED);
            return List.of(createProportionalPoint(raData));
        }
        if (strategy == GlskStrategyKind.EXPLICIT_SCHEDULE) {
            checkShiftMethod(raData, ShiftMethodKind.PRIORITY);
            List<GlskPoint> points = NcGlskMeritOrderBuilder.build(raData, bids);
            if (points.isEmpty()) {
                LOGGER.warn("Countertrade remedial action {} is ignored: no merit order could be built from its power bid schedules", raData.mRID());
            }
            return points;
        }
        LOGGER.warn("Countertrade remedial action {} is ignored: unsupported GLSK strategy {}", raData.mRID(), raData.glskStrategyValue());
        return Collections.emptyList();
    }

    /**
     * The shift method goes with the GLSK strategy: shared with proportionalForGenerator, priority with
     * explicitSchedule. The GLSK strategy prevails, so a shift method that does not go with it (or is missing) is only
     * reported.
     */
    private static void checkShiftMethod(CountertradeRAData raData, ShiftMethodKind expected) {
        if (raData.shiftMethod() == expected) {
            return;
        }
        if (raData.shiftMethodValue() == null) {
            LOGGER.warn("Countertrade remedial action {} has no shift method but its GLSK strategy {} goes with {}",
                raData.mRID(), raData.strategy().value(), expected.value());
        } else {
            LOGGER.warn("Countertrade remedial action {} has shift method {} but its GLSK strategy {} goes with {}: the GLSK strategy prevails",
                raData.mRID(), raData.shiftMethodValue(), raData.strategy().value(), expected.value());
        }
    }

    /**
     * proportionalForGenerator needs no data from the SIS profile: a single point, valid over the validity window of the
     * RA profile, which applies to all the generators of the country of the bidding zone.
     */
    private GlskPoint createProportionalPoint(CountertradeRAData raData) {
        return NcGlskPoint.proportionalForGenerator(raData.biddingZone(), Interval.of(raData.validityStart(), raData.validityEnd()));
    }

    /**
     * Extracts EIC code from URI.
     * Example: "http://energy.referencedata.eu/EIC/10YFR-RTE------C" → "10YFR-RTE------C"
     */
    private String extractEICCode(String uri) {
        if (uri == null) {
            return null;
        }
        int lastSlash = uri.lastIndexOf('/');
        if (lastSlash >= 0 && lastSlash < uri.length() - 1) {
            return uri.substring(lastSlash + 1);
        }
        return uri;
    }

    /**
     * Parses GLSK strategy from URI.
     * Example: "http://entsoe.eu/ns/nc#GLSKStrategyKind.proportionalForGenerator" → PROPORTIONAL_FOR_GENERATOR
     *
     * @return the strategy, or null if it is missing or not a known GLSKStrategyKind value
     */
    private GlskStrategyKind parseGlskStrategy(String uri) {
        if (uri == null) {
            return null;
        }
        try {
            return GlskStrategyKind.fromValue(uri.substring(uri.lastIndexOf('.') + 1));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Parses the name of a value of a CIM enumeration from its URI.
     * Example: "http://entsoe.eu/ns/nc#BidDirectionKind.up" → "up"
     */
    private static String parseKindName(String uri) {
        return uri == null ? null : uri.substring(uri.lastIndexOf('.') + 1);
    }

    private static Instant parseInstant(String dateTime) {
        return OffsetDateTime.parse(dateTime).toInstant();
    }

    private static Instant parseNullableInstant(String dateTime) {
        return dateTime == null ? null : parseInstant(dateTime);
    }

    private static Double parseNullableDouble(String value) {
        return value == null ? null : Double.valueOf(value);
    }

    public Instant getValidityStart() {
        return validityStart;
    }

    public Instant getValidityEnd() {
        return validityEnd;
    }

    /**
     * Intermediate data of a CountertradeRemedialAction (RA profile).
     *
     * @param uri               RDF identity of the RA, which the power bid schedules of the SIS profile refer to
     * @param glskStrategyValue raw value of the GLSK strategy, as found in the profile
     * @param strategy          GLSK strategy, or null if it is not a known GLSKStrategyKind value
     * @param shiftMethodValue  name of the shift method, as found in the profile, or null if there is none
     * @param shiftMethod       shift method, or null if it is missing or not a known ShiftMethodKind value
     * @param validityStart     start of the validity window declared in the header of the RA profile
     * @param validityEnd       end of the validity window declared in the header of the RA profile
     */
    record CountertradeRAData(String uri, String mRID, String biddingZone, String glskStrategyValue,
                              GlskStrategyKind strategy, String shiftMethodValue, ShiftMethodKind shiftMethod,
                              Instant validityStart, Instant validityEnd) {
    }

    /**
     * Intermediate data of a PowerBidSchedule (SIS profile), i.e. one bid of a merit order.
     *
     * @param uri                RDF identity of the bid
     * @param remedialActionUri  RDF identity of the countertrade remedial action the bid belongs to
     * @param direction          direction of the bid (up or down)
     * @param validityStart      start of the validity window declared in the header of the SIS profile
     * @param validityEnd        end of the validity window declared in the header of the SIS profile
     * @param timePoints         price and power of the bid, from the time they take effect
     * @param glskSchedules      GLSK schedules binding the bid to its generating unit
     */
    record PowerBidScheduleData(String uri, String mRID, String remedialActionUri, String direction,
                                Instant validityStart, Instant validityEnd,
                                NavigableMap<Instant, PriceAndPower> timePoints, List<GlskScheduleData> glskSchedules) {
    }

    /**
     * Intermediate data of a GLSKSchedule (SIS profile).
     *
     * @param uri                  RDF identity of the GLSK schedule
     * @param generatingUnit       mRID of the generating unit the schedule refers to
     * @param participationFactors participation factor of the generating unit, from the time it takes effect
     */
    record GlskScheduleData(String uri, String generatingUnit, NavigableMap<Instant, Double> participationFactors) {
    }

    /**
     * Price and power of a bid time point; any of them can be missing in the profile.
     */
    record PriceAndPower(Double price, Double power) {
    }
}
