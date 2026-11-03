/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.Set;
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
    private final Map<String, Set<String>> keywordMap;
    private final QueryCatalog queryCatalog;
    private final Map<String, CountertradeRAData> countertradeRAs;
    private final Map<String, NcRemedialActionAvailability> availabilities;
    private Instant validityStart;
    private Instant validityEnd;

    /**
     * @param tripleStore the triples of the profiles, one context per file
     * @param keywordMap  the contexts of the files of each profile keyword (RA, SIS, SSI)
     */
    public NcGlskDatasetParser(TripleStore tripleStore, Map<String, Set<String>> keywordMap) {
        this.tripleStore = tripleStore;
        this.keywordMap = keywordMap;
        this.queryCatalog = new QueryCatalog(NcConstants.SPARQL_FILE_NC_PROFILE);
        this.countertradeRAs = new HashMap<>();
        this.availabilities = new HashMap<>();
    }

    /**
     * Runs a query of the NC GLSK query catalog on the whole triple store.
     *
     * @param queryName name of the query in the catalog (nc.sparql)
     */
    private PropertyBags query(String queryName) {
        return tripleStore.query(queryText(queryName));
    }

    /**
     * Runs a query of the NC GLSK query catalog on each file of the profile of a keyword (the query is written for a single
     * context, which is its parameter), so that the data of a profile is only read from the files of that profile.
     *
     * @param queryName name of the query in the catalog (nc.sparql)
     * @param keyword   keyword of the profile the query reads
     */
    private PropertyBags query(String queryName, NcKeyword keyword) {
        String queryText = queryText(queryName);
        PropertyBags result = new PropertyBags();
        for (String context : keywordMap.getOrDefault(keyword.toString(), Set.of())) {
            result.addAll(tripleStore.query(String.format(queryText, context)));
        }
        return result;
    }

    private String queryText(String queryName) {
        String queryText = queryCatalog.get(queryName);
        if (queryText == null) {
            throw new GlskException("Query [" + queryName + "] not found in " + NcConstants.SPARQL_FILE_NC_PROFILE);
        }
        return queryText;
    }

    /**
     * Parse all NC GLSK data from the triplestore.
     * Returns a map of zone EIC codes to GlskPoints.
     */
    public Map<String, List<GlskPoint>> parse() {
        // Parse headers to get validity dates
        parseHeaders();

        // Parse CountertradeRemedialActions
        parseCountertradeRemedialActions();

        // Parse the availability of the remedial actions (RA profile, completed by the SIS and SSI profiles)
        parseAvailability();

        // Parse PowerBidSchedules
        Map<String, PowerBidScheduleData> powerBidSchedules = parsePowerBidSchedules();

        // Parse GLSKSchedules with their resource, and link them to their PowerBidSchedule
        Map<String, GlskScheduleData> glskSchedules = parseGlskSchedules(powerBidSchedules);

        // Parse ParticipationFactorTimePoints
        parseParticipationFactorTimePoints(glskSchedules);

        // Parse PowerBidScheduleTimePoints (price and power)
        parsePowerBidScheduleTimePoints(powerBidSchedules);

        // Build GlskPoints grouped by zone
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
        for (PropertyBag bag : query(NcConstants.QUERY_COUNTERTRADE_REMEDIAL_ACTIONS, NcKeyword.REMEDIAL_ACTION)) {
            String uri = bag.get(NcConstants.REMEDIAL_ACTION);
            String glskStrategyValue = bag.get(NcConstants.GLSK_STRATEGY);
            String shiftMethodUrl = bag.get(NcConstants.SHIFT_METHOD);
            // A remedial action without normalAvailable is considered available
            String normalAvailable = bag.get(NcConstants.NORMAL_AVAILABLE);
            availabilities.put(uri, new NcRemedialActionAvailability(normalAvailable == null || Boolean.parseBoolean(normalAvailable)));
            checkOperator(bag);
            countertradeRAs.put(uri, new CountertradeRAData(
                uri,
                bag.get(NcConstants.MRID),
                extractEICCode(bag.get(NcConstants.BIDDING_ZONE)),
                glskStrategyValue,
                GlskStrategyKind.fromUrl(glskStrategyValue).orElse(null),
                literalOf(shiftMethodUrl),
                ShiftMethodKind.fromUrl(shiftMethodUrl).orElse(null),
                parseNullableInstant(bag.get(NcConstants.REQUEST_HEADER_START_DATE)),
                parseNullableInstant(bag.get(NcConstants.REQUEST_HEADER_END_DATE))
            ));
        }
    }

    /**
     * The operator of the remedial action is the creator of its profile. If the remedial action declares an operator
     * (RemedialActionSystemOperator) that is not the creator, it is only reported: the creator prevails.
     */
    private void checkOperator(PropertyBag bag) {
        String operator = bag.get(NcConstants.OPERATOR);
        String creator = bag.get(NcConstants.CREATOR);
        if (operator != null && creator != null && !extractEICCode(operator).equals(extractEICCode(creator))) {
            String mrId = bag.get(NcConstants.MRID);
            LOGGER.warn("Countertrade remedial action {} has operator {} which is not the creator of its profile ({}): the creator prevails",
                    mrId, operator, creator);
        }
    }

    /**
     * Completes the availability of the remedial actions with the availability schedules of the SIS profile and with the
     * availability of the SSI profile. The SSI profile prevails over the SIS profile, which prevails over the RA profile.
     */
    private void parseAvailability() {
        Set<String> unsupportedSchedules = new HashSet<>();
        for (PropertyBag bag : query(NcConstants.QUERY_AVAILABILITY_TIME_POINTS, NcKeyword.STATE_INSTRUCTION_SCHEDULE)) {
            String ra = bag.get(NcConstants.REMEDIAL_ACTION);
            NcRemedialActionAvailability availability = availabilities.get(ra);
            // An availability schedule only supports the interpolation next (a missing kind is read as next), and any other one is ignored
            String interpolationUrl = bag.get(NcConstants.INTERPOLATION_KIND);
            boolean supported = interpolationUrl == null
                || TimeSeriesInterpolationKind.fromUrl(interpolationUrl).filter(kind -> kind == TimeSeriesInterpolationKind.NEXT).isPresent();
            if (!supported && unsupportedSchedules.add(bag.get(NcConstants.SCHEDULE))) {
                String interpolationValue = literalOf(interpolationUrl);
                LOGGER.warn("Availability schedule of countertrade remedial action {} is ignored: its interpolation kind {} is not supported (only next)",
                        ra, interpolationValue);
            }
            Optional<Interval> validity = validityOf(bag);
            if (availability != null && supported && validity.isPresent()) {
                availability.addAvailabilityTimePoint(parseInstant(bag.get(NcConstants.AT_TIME)), Boolean.parseBoolean(bag.get(NcConstants.OVERRIDE_AVAILABLE)), validity.get());
            }
        }
        for (PropertyBag bag : query(NcConstants.QUERY_REMEDIAL_ACTION_AVAILABLE, NcKeyword.STEADY_STATE_INSTRUCTION)) {
            NcRemedialActionAvailability availability = availabilities.get(bag.get(NcConstants.REMEDIAL_ACTION));
            Optional<Interval> validity = validityOf(bag);
            if (availability != null && validity.isPresent()) {
                availability.addSsiAvailability(Boolean.parseBoolean(bag.get(NcConstants.OVERRIDE_AVAILABLE)), validity.get());
            }
        }
    }

    /**
     * @return the validity window declared in the header of the profile a row belongs to, or empty if the window is empty
     * (a profile with such a window is never valid)
     */
    private static Optional<Interval> validityOf(PropertyBag bag) {
        Instant start = parseInstant(bag.get(NcConstants.REQUEST_HEADER_START_DATE));
        Instant end = parseInstant(bag.get(NcConstants.REQUEST_HEADER_END_DATE));
        return end.isAfter(start) ? Optional.of(Interval.of(start, end)) : Optional.empty();
    }

    private Map<String, PowerBidScheduleData> parsePowerBidSchedules() {
        Map<String, PowerBidScheduleData> result = new HashMap<>();
        for (PropertyBag bag : query(NcConstants.QUERY_POWER_BID_SCHEDULES, NcKeyword.STATE_INSTRUCTION_SCHEDULE)) {
            String uri = bag.get(NcConstants.POWER_BID_SCHEDULE);
            String directionUrl = bag.get(NcConstants.DIRECTION);
            String interpolationUrl = bag.get(NcConstants.INTERPOLATION_KIND);
            result.put(uri, new PowerBidScheduleData(
                uri,
                bag.get(NcConstants.POWER_BID_SCHEDULE_ID),
                bag.get(NcConstants.REMEDIAL_ACTION),
                literalOf(directionUrl),
                BidDirectionKind.fromUrl(directionUrl).orElse(null),
                literalOf(interpolationUrl),
                TimeSeriesInterpolationKind.fromUrl(interpolationUrl).orElse(null),
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
        for (PropertyBag bag : query(NcConstants.QUERY_GLSK_SCHEDULES, NcKeyword.STATE_INSTRUCTION_SCHEDULE)) {
            PowerBidScheduleData powerBidSchedule = powerBidSchedules.get(bag.get(NcConstants.POWER_BID_SCHEDULE));
            if (powerBidSchedule == null) {
                continue;
            }
            // The resource is identified by its mRID, as in the CGMES EQ profile
            GlskScheduleData glskSchedule = result.computeIfAbsent(bag.get(NcConstants.GLSK_SCHEDULE),
                uri -> new GlskScheduleData(uri, bag.getId(NcConstants.RESOURCE), new TreeMap<>()));
            powerBidSchedule.glskSchedules().add(glskSchedule);
        }
        return result;
    }

    private void parseParticipationFactorTimePoints(Map<String, GlskScheduleData> glskSchedules) {
        for (PropertyBag bag : query(NcConstants.QUERY_PARTICIPATION_FACTOR_TIME_POINTS, NcKeyword.STATE_INSTRUCTION_SCHEDULE)) {
            GlskScheduleData glskSchedule = glskSchedules.get(bag.get(NcConstants.GLSK_SCHEDULE));
            if (glskSchedule != null) {
                glskSchedule.participationFactors().put(parseInstant(bag.get(NcConstants.AT_TIME)), Double.parseDouble(bag.get(NcConstants.PARTICIPATION_FACTOR)));
            }
        }
    }

    private void parsePowerBidScheduleTimePoints(Map<String, PowerBidScheduleData> powerBidSchedules) {
        for (PropertyBag bag : query(NcConstants.QUERY_POWER_BID_SCHEDULE_TIME_POINTS, NcKeyword.STATE_INSTRUCTION_SCHEDULE)) {
            PowerBidScheduleData powerBidSchedule = powerBidSchedules.get(bag.get(NcConstants.POWER_BID_SCHEDULE));
            if (powerBidSchedule != null) {
                powerBidSchedule.timePoints().put(parseInstant(bag.get(NcConstants.AT_TIME)),
                    new PriceAndPower(parseNullableDouble(bag.get(NcConstants.PRICE)), parseNullableDouble(bag.get(NcConstants.POWER))));
            }
        }
    }

    private Map<String, List<GlskPoint>> buildGlskPointsByZone(Map<String, PowerBidScheduleData> powerBidSchedules) {
        Map<String, List<PowerBidScheduleData>> bidsByRa = powerBidSchedules.values().stream()
            .collect(Collectors.groupingBy(PowerBidScheduleData::remedialActionUri));
        Map<String, List<GlskPoint>> result = new HashMap<>();

        // For each RA, build its GLSK points according to its strategy and the bids of its SIS profile
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
        if (strategy != GlskStrategyKind.PROPORTIONAL_FOR_GENERATOR && strategy != GlskStrategyKind.EXPLICIT_SCHEDULE) {
            LOGGER.warn("Countertrade remedial action {} is ignored: unsupported GLSK strategy {}", raData.mRID(), raData.glskStrategyValue());
            return Collections.emptyList();
        }
        checkShiftMethod(raData, strategy == GlskStrategyKind.PROPORTIONAL_FOR_GENERATOR ? ShiftMethodKind.SHARED : ShiftMethodKind.PRIORITY);

        // A GLSK is only built for the periods during which the remedial action is available
        NcRemedialActionAvailability availability = availabilities.get(raData.uri());
        List<Interval> availablePeriods = availability.availablePeriods(raData.validityStart(), raData.validityEnd());
        if (availablePeriods.isEmpty()) {
            LOGGER.warn("Countertrade remedial action {} is ignored: it is not available during its validity window", raData.mRID());
            return Collections.emptyList();
        }

        if (strategy == GlskStrategyKind.PROPORTIONAL_FOR_GENERATOR) {
            return availablePeriods.stream().map(period -> createProportionalPoint(raData, period)).toList();
        }
        List<GlskPoint> points = NcGlskMeritOrderBuilder.build(raData, bids, availability);
        if (points.isEmpty()) {
            LOGGER.warn("Countertrade remedial action {} is ignored: no merit order could be built from its power bid schedules", raData.mRID());
        }
        return points;
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
     * proportionalForGenerator needs no data from the SIS profile: a point for each period during which the remedial
     * action is available, which applies to all the generators of the country of the bidding zone.
     */
    private GlskPoint createProportionalPoint(CountertradeRAData raData, Interval interval) {
        return NcGlskPoint.proportionalForGenerator(raData.biddingZone(), interval);
    }

    /**
     * Extracts EIC code from URI.
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
     * The name of the literal of an enumeration, from its URL, to report a value of a profile (the enumerations themselves
     * are read from the whole URL, see for example {@link BidDirectionKind#fromUrl(String)}).
     */
    private static String literalOf(String url) {
        return url == null ? null : url.substring(url.lastIndexOf('.') + 1);
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
     * @param shiftMethodValue  name of the shift method literal, as found in the profile, or null if there is none
     * @param shiftMethod       shift method, or null if it is missing or not a ShiftMethodKind literal
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
     * @param directionValue     name of the direction literal, as found in the profile
     * @param direction          direction of the bid, or null if it is missing or not a BidDirectionKind literal (up and down are supported)
     * @param interpolationKindValue name of the interpolation kind literal, as found in the profile, or null if there is none
     * @param interpolationKind  interpolation of the values between two time points, or null if it is missing or not a TimeSeriesInterpolationKind literal (none and next are supported)
     * @param validityStart      start of the validity window declared in the header of the SIS profile
     * @param validityEnd        end of the validity window declared in the header of the SIS profile
     * @param timePoints         price and power of the bid, from the time they take effect
     * @param glskSchedules      GLSK schedules binding the bid to its resource
     */
    record PowerBidScheduleData(String uri, String mRID, String remedialActionUri, String directionValue, BidDirectionKind direction,
                                String interpolationKindValue, TimeSeriesInterpolationKind interpolationKind, Instant validityStart, Instant validityEnd,
                                NavigableMap<Instant, PriceAndPower> timePoints, List<GlskScheduleData> glskSchedules) {
    }

    /**
     * Intermediate data of a GLSKSchedule (SIS profile).
     *
     * @param uri                  RDF identity of the GLSK schedule
     * @param resource             mRID of the resource (generating unit, load...) the schedule refers to
     * @param participationFactors participation factor of the resource, from the time it takes effect
     */
    record GlskScheduleData(String uri, String resource, NavigableMap<Instant, Double> participationFactors) {
    }

    /**
     * Price and power of a bid time point; any of them can be missing in the profile.
     */
    record PriceAndPower(Double price, Double power) {
    }
}
