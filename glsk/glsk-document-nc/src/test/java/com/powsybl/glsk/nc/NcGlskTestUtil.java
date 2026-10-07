/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.glsk.nc;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.powsybl.iidm.modification.scalable.Scalable;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.test.EurostagTutorialExample1Factory;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Generation of minimal RA and SIS profiles to test the NC GLSK importer.
 *
 * @author Víctor Cardozo {@literal <victor.cardozo at artelys.com>}
 */
final class NcGlskTestUtil {

    static final String STRATEGY_PREFIX = "http://entsoe.eu/ns/nc#GLSKStrategyKind.";
    static final String PROPORTIONAL_FOR_GENERATOR = STRATEGY_PREFIX + "proportionalForGenerator";
    static final String EXPLICIT_SCHEDULE = STRATEGY_PREFIX + "explicitSchedule";

    static final String ES_ZONE = "10YES-REE------0";
    static final String START = "2029-08-12T00:00:00Z";
    static final String END = "2029-08-13T00:00:00Z";
    static final Instant INSTANT = Instant.parse("2029-08-12T12:00:00Z");

    private static final String RDF_HEADER = """
        <?xml version="1.0" encoding="UTF-8"?>
        <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
                 xmlns:nc="http://entsoe.eu/ns/nc#"
                 xmlns:md="http://iec.ch/TC57/61970-552/ModelDescription/1#"
                 xmlns:dcat="http://www.w3.org/ns/dcat#"
                 xmlns:cim="http://iec.ch/TC57/CIM100#"
                 xmlns:dcterms="http://purl.org/dc/terms/#">
        """;

    private static final String RA_PROFILE_TEMPLATE = RDF_HEADER + """
          <md:FullModel rdf:about="urn:uuid:%1$s">
            %2$s
            <dcat:keyword>RA</dcat:keyword>
          </md:FullModel>
          <nc:CountertradeRemedialAction rdf:ID="_%1$s">
            <cim:IdentifiedObject.mRID>%1$s</cim:IdentifiedObject.mRID>
            <cim:IdentifiedObject.name>Countertrade %1$s</cim:IdentifiedObject.name>
            <nc:RemedialAction.kind rdf:resource="http://entsoe.eu/ns/nc#RemedialActionKind.preventive"/>
            <nc:RemedialAction.normalAvailable>%3$s</nc:RemedialAction.normalAvailable>
            <nc:PowerRemedialAction.BiddingZone rdf:resource="http://energy.referencedata.eu/EIC/%4$s"/>
            <nc:CountertradeRemedialAction.gLSKStrategy rdf:resource="%5$s"/>
            %6$s
          </nc:CountertradeRemedialAction>
        </rdf:RDF>
        """;

    private NcGlskTestUtil() {
    }

    /**
     * Price and power of a bid, from the time they take effect. Any of them can be null to be missing in the profile.
     */
    record TimePoint(String atTime, String price, String power) {
    }

    /**
     * A power bid schedule of the SIS profile, with the generating unit(s) it is bound to.
     *
     * @param id                     identifies the bid and its GLSK schedule(s) in the profile
     * @param direction              value of BidDirectionKind
     * @param generatingUnits        mRIDs of the generating units: none, one (the normal case) or several
     * @param participationFactor    participation factor of the generating unit(s)
     * @param participationFactorAt  time from which the participation factor takes effect
     * @param glskScheduleMrid       mRID shared by all the GLSK schedules of the bid, or null for a mRID per schedule
     * @param timePoints             price and power time series
     */
    record Bid(String id, String direction, List<String> generatingUnits, String participationFactor,
               String participationFactorAt, String glskScheduleMrid, List<TimePoint> timePoints) {

        /**
         * A bid bound to a generating unit, with a participation factor of 1 in force long before the profiles.
         */
        static Bid of(String id, String direction, String generatingUnit, TimePoint... timePoints) {
            return new Bid(id, direction, List.of(generatingUnit), "1", "2029-01-01T00:00:00Z", null, Arrays.asList(timePoints));
        }

        Bid withGeneratingUnits(String... units) {
            return new Bid(id, direction, Arrays.asList(units), participationFactor, participationFactorAt, glskScheduleMrid, timePoints);
        }

        Bid withParticipationFactor(String factor, String at) {
            return new Bid(id, direction, generatingUnits, factor, at, glskScheduleMrid, timePoints);
        }

        Bid withGlskScheduleMrid(String mrid) {
            return new Bid(id, direction, generatingUnits, participationFactor, participationFactorAt, mrid, timePoints);
        }
    }

    /**
     * Builds the content of an RA profile with a single countertrade remedial action.
     *
     * @param mRid      mRID of the remedial action (also used to identify the profile)
     * @param start     start of the validity window of the profile, or null for no validity window
     * @param end       end of the validity window of the profile, or null for no validity window
     * @param available value of normalAvailable
     * @param zoneEic   EIC code of the bidding zone
     * @param strategy  GLSK strategy kind (full URI)
     */
    static String raProfile(String mRid, String start, String end, boolean available, String zoneEic, String strategy) {
        return raProfile(mRid, start, end, available, zoneEic, strategy, shiftMethodOf(strategy));
    }

    /**
     * Same as above, with the given shift method (name of a ShiftMethodKind value, or null for no shift method).
     */
    static String raProfile(String mRid, String start, String end, boolean available, String zoneEic, String strategy, String shiftMethod) {
        String shiftMethodElement = shiftMethod == null ? "" :
            "<nc:CountertradeRemedialAction.shiftMethod rdf:resource=\"http://entsoe.eu/ns/nc#ShiftMethodKind." + shiftMethod + "\"/>";
        return RA_PROFILE_TEMPLATE.formatted(mRid, validityWindow(start, end), available, zoneEic, strategy, shiftMethodElement);
    }

    /**
     * @return the shift method that goes with a GLSK strategy, or null for the strategies the importer does not support
     */
    private static String shiftMethodOf(String strategy) {
        if (EXPLICIT_SCHEDULE.equals(strategy)) {
            return "priority";
        }
        return PROPORTIONAL_FOR_GENERATOR.equals(strategy) ? "shared" : null;
    }

    /**
     * Runs an action and returns the messages logged as warnings by the given class meanwhile.
     */
    static List<String> warningsLoggedBy(Class<?> loggingClass, Runnable action) {
        Logger logger = (Logger) LoggerFactory.getLogger(loggingClass);
        Level previousLevel = logger.getLevel();
        boolean previousAdditive = logger.isAdditive();
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        logger.setLevel(Level.WARN);
        logger.setAdditive(false);
        try {
            action.run();
        } finally {
            logger.detachAppender(appender);
            logger.setLevel(previousLevel);
            logger.setAdditive(previousAdditive);
        }
        return appender.list.stream().filter(event -> event.getLevel() == Level.WARN).map(ILoggingEvent::getFormattedMessage).toList();
    }

    /**
     * Builds the content of a SIS profile with the given bids of a countertrade remedial action.
     *
     * @param remedialActionMrid mRID of the countertrade remedial action the bids belong to
     * @param start              start of the validity window of the profile, or null for no validity window
     * @param end                end of the validity window of the profile, or null for no validity window
     */
    static String sisProfile(String remedialActionMrid, String start, String end, Bid... bids) {
        StringBuilder xml = new StringBuilder(RDF_HEADER);
        xml.append("  <md:FullModel rdf:about=\"urn:uuid:sis-").append(remedialActionMrid).append("\">\n")
            .append("    ").append(validityWindow(start, end)).append("\n")
            .append("    <dcat:keyword>SIS</dcat:keyword>\n  </md:FullModel>\n");
        for (Bid bid : bids) {
            appendBid(xml, remedialActionMrid, bid);
        }
        return xml.append("</rdf:RDF>\n").toString();
    }

    private static void appendBid(StringBuilder xml, String remedialActionMrid, Bid bid) {
        String pbs = "pbs-" + bid.id();
        xml.append("  <nc:PowerBidSchedule rdf:ID=\"_").append(pbs).append("\">\n")
            .append("    <cim:IdentifiedObject.mRID>").append(pbs).append("</cim:IdentifiedObject.mRID>\n")
            .append("    <nc:PowerBidSchedule.direction rdf:resource=\"http://entsoe.eu/ns/nc#BidDirectionKind.").append(bid.direction()).append("\"/>\n")
            .append("    <nc:PowerBidSchedule.PowerRemedialAction rdf:resource=\"#_").append(remedialActionMrid).append("\"/>\n")
            .append("  </nc:PowerBidSchedule>\n");
        for (int i = 0; i < bid.timePoints().size(); i++) {
            TimePoint timePoint = bid.timePoints().get(i);
            xml.append("  <nc:PowerBidScheduleTimePoint rdf:ID=\"_tp-").append(bid.id()).append("-").append(i).append("\">\n")
                .append("    <nc:PowerBidScheduleTimePoint.PowerBidSchedule rdf:resource=\"#_").append(pbs).append("\"/>\n")
                .append("    <nc:PowerBidScheduleTimePoint.atTime>").append(timePoint.atTime()).append("</nc:PowerBidScheduleTimePoint.atTime>\n");
            if (timePoint.price() != null) {
                xml.append("    <nc:PowerBidScheduleTimePoint.price>").append(timePoint.price()).append("</nc:PowerBidScheduleTimePoint.price>\n");
            }
            if (timePoint.power() != null) {
                xml.append("    <nc:PowerBidScheduleTimePoint.p>").append(timePoint.power()).append("</nc:PowerBidScheduleTimePoint.p>\n");
            }
            xml.append("  </nc:PowerBidScheduleTimePoint>\n");
        }
        for (int i = 0; i < bid.generatingUnits().size(); i++) {
            String schedule = "glsk-" + bid.id() + "-" + i;
            String scheduleMrid = bid.glskScheduleMrid() != null ? bid.glskScheduleMrid() : schedule;
            xml.append("  <nc:GLSKSchedule rdf:ID=\"_").append(schedule).append("\">\n")
                .append("    <cim:IdentifiedObject.mRID>").append(scheduleMrid).append("</cim:IdentifiedObject.mRID>\n")
                .append("    <nc:GLSKSchedule.GeneratingUnit rdf:resource=\"#_").append(bid.generatingUnits().get(i)).append("\"/>\n")
                .append("  </nc:GLSKSchedule>\n")
                .append("  <nc:GLSKBidActionDistribution rdf:ID=\"_dist-").append(bid.id()).append("-").append(i).append("\">\n")
                .append("    <nc:GLSKBidActionDistribution.PowerBidSchedule rdf:resource=\"#_").append(pbs).append("\"/>\n")
                .append("    <nc:GLSKBidActionDistribution.GLSKSchedule rdf:resource=\"#_").append(schedule).append("\"/>\n")
                .append("  </nc:GLSKBidActionDistribution>\n")
                .append("  <nc:ParticipationFactorTimePoint rdf:ID=\"_pftp-").append(bid.id()).append("-").append(i).append("\">\n")
                .append("    <nc:ParticipationFactorTimePoint.GLSKSchedule rdf:resource=\"#_").append(schedule).append("\"/>\n")
                .append("    <nc:ParticipationFactorTimePoint.atTime>").append(bid.participationFactorAt()).append("</nc:ParticipationFactorTimePoint.atTime>\n")
                .append("    <nc:ParticipationFactorTimePoint.participationFactor>").append(bid.participationFactor()).append("</nc:ParticipationFactorTimePoint.participationFactor>\n")
                .append("  </nc:ParticipationFactorTimePoint>\n");
        }
    }

    private static String validityWindow(String start, String end) {
        return start == null || end == null ? "" :
            "<dcat:startDate>" + start + "</dcat:startDate><dcat:endDate>" + end + "</dcat:endDate>";
    }

    /**
     * Zips the given profiles, one entry each, and imports them as an NC GLSK document.
     */
    static NcGlskDocument importProfiles(String... profiles) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(bytes)) {
            for (int i = 0; i < profiles.length; i++) {
                zip.putNextEntry(new ZipEntry("20290812T0030Z_1D_TSO" + i + "_RA_001.xml"));
                zip.write(profiles[i].getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return NcGlskDocument.importGlsk(new ByteArrayInputStream(bytes.toByteArray()));
    }

    /**
     * Network with two generators: GEN produces 30 MW and GEN2 produces 25 MW, both can reach 1000 MW.
     */
    static Network twoGeneratorsNetwork() {
        Network network = EurostagTutorialExample1Factory.createWithMoreGenerators();
        network.getGenerator("GEN").setTargetP(30.).setMaxP(1000.).setMinP(0.);
        network.getGenerator("GEN2").setTargetP(25.).setMaxP(1000.).setMinP(0.);
        return network;
    }

    /**
     * A bid whose price and power are in force from START.
     */
    static Bid bid(String id, String direction, String generatingUnit, double price, double power) {
        return Bid.of(id, direction, generatingUnit, new TimePoint(START, String.valueOf(price), String.valueOf(power)));
    }

    static Bid up(String id, String generatingUnit, double price, double power) {
        return bid(id, "up", generatingUnit, price, power);
    }

    static Bid down(String id, String generatingUnit, double price, double power) {
        return bid(id, "down", generatingUnit, price, power);
    }

    /**
     * Imports an explicitSchedule remedial action of the zone of Spain with the given bids, valid from START to END, and
     * converts its merit order to a scalable for the network, at INSTANT.
     */
    static Scalable meritOrderScalable(Network network, Bid... bids) {
        NcGlskDocument document = importProfiles(
            raProfile("ra-es", START, END, true, ES_ZONE, EXPLICIT_SCHEDULE),
            sisProfile("ra-es", START, END, bids));
        return document.getZonalScalable(network, INSTANT).getData(ES_ZONE);
    }
}
