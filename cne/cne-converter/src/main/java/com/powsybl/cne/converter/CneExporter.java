/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.cne.converter;

import com.google.auto.service.AutoService;
import com.powsybl.commons.config.PlatformConfig;
import com.powsybl.commons.parameters.Parameter;
import com.powsybl.commons.parameters.ParameterDefaultValueConfig;
import com.powsybl.commons.parameters.ParameterType;
import com.powsybl.security.SecurityAnalysisResult;
import com.powsybl.security.converter.SecurityAnalysisResultExporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.util.Properties;

import static com.powsybl.cne.converter.CneConstants.*;

/**
 * CNE XML format export of an SecurityAnalysisResult.<p>
 *
 * @author Thomas Adam <tadam at silicom.fr>
 */
@AutoService(SecurityAnalysisResultExporter.class)
public class CneExporter implements SecurityAnalysisResultExporter {

    private static final Logger LOGGER = LoggerFactory.getLogger(CneExporter.class);

    public static final String PREFIX = "cne.export.xml.";

    // Required parameters
    private static final Parameter MRID_PARAMETER = new Parameter(PREFIX + MRID, ParameterType.STRING, "mRID", null);
    private static final Parameter SENDER_MARKET_PARTICIPANT_MRID_PARAMETER = new Parameter(PREFIX + SENDER_MARKET_PARTICIPANT_MRID, ParameterType.STRING, "", null);
    private static final Parameter RECEIVER_MARKET_PARTICIPANT_MRID_PARAMETER = new Parameter(PREFIX + RECEIVER_MARKET_PARTICIPANT_MRID, ParameterType.STRING, "", null);
    private static final Parameter TIME_SERIES_MRID_PARAMETER = new Parameter(PREFIX + TIME_SERIES_MRID, ParameterType.STRING, "", null);
    private static final Parameter IN_DOMAIN_PARAMETER = new Parameter(PREFIX + IN_DOMAIN_MRID, ParameterType.STRING, "", null);
    private static final Parameter OUT_DOMAIN_PARAMETER = new Parameter(PREFIX + OUT_DOMAIN_MRID, ParameterType.STRING, "", null);
    // Optional parameters
    // Current datetime (now) is used, if no created datetime is given
    private static final Parameter CREATED_DATETIME_PARAMETER = new Parameter(PREFIX + CREATED_DATETIME, ParameterType.STRING, "", formatDateTime(Instant.now()));
    // Current datetime (now) is used, if no start datetime is given
    private static final Parameter TIME_PERIOD_START_PARAMETER = new Parameter(PREFIX + TIME_PERIOD + "." + TIME_INTERVAL + "." + START, ParameterType.STRING, "", formatDateTime(Instant.now()));
    // Current datetime (now) + 1 hour is used, if no end datetime is given
    private static final Parameter TIME_PERIOD_END_PARAMETER = new Parameter(PREFIX + TIME_PERIOD + "." + TIME_INTERVAL + "." + END, ParameterType.STRING, "", formatDateTime(Instant.now().plusMillis(3600L * 1000L)));

    private final ParameterDefaultValueConfig defaultValueConfig;

    public CneExporter() {
        this(PlatformConfig.defaultConfig());
    }

    public CneExporter(PlatformConfig platformConfig) {
        defaultValueConfig = new ParameterDefaultValueConfig(platformConfig);
    }

    @Override
    public String getFormat() {
        return "CNE-XML";
    }

    @Override
    public String getComment() {
        return "Export a security analysis result in CNE format";
    }

    /**
     * Required parameters properties key
     *   cne.export.xml.mRID
     *   cne.export.xml.sender_MarketParticipant.mRID
     *   cne.export.xml.receiver_MarketParticipant.mRID
     *   cne.export.xml.TimeSeries.mRID
     *   cne.export.xml.in_Domain.mRID
     *   cne.export.xml.out_Domain.mRID
     *
     * Optional parameters properties key
     *   cne.export.xml.createdDateTime
     *   cne.export.xml.time_Period.timeInterval.start
     *   cne.export.xml.time_Period.timeInterval.end
     */
    @Override
    public void export(SecurityAnalysisResult result, Properties parameters, Writer writer) {
        var options = createExportOptions(parameters);

        try {
            long startTime = System.currentTimeMillis();
            SecurityAnalysisResultXml.write(result, options, writer);
            LOGGER.debug("CNE export done in {} ms", System.currentTimeMillis() - startTime);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private CneExportOptions createExportOptions(Properties parameters) {
        return new CneExportOptions()
                .setMRID(Parameter.readString(getFormat(), parameters, MRID_PARAMETER, defaultValueConfig))
                .setSenderMarketParticipantMRID(Parameter.readString(getFormat(), parameters, SENDER_MARKET_PARTICIPANT_MRID_PARAMETER, defaultValueConfig))
                .setReceiverMarketParticipantMRID(Parameter.readString(getFormat(), parameters, RECEIVER_MARKET_PARTICIPANT_MRID_PARAMETER, defaultValueConfig))
                .setTimeSeriesMRID(Parameter.readString(getFormat(), parameters, TIME_SERIES_MRID_PARAMETER, defaultValueConfig))
                .setInDomainMRID(Parameter.readString(getFormat(), parameters, IN_DOMAIN_PARAMETER, defaultValueConfig))
                .setOutDomainMRID(Parameter.readString(getFormat(), parameters, OUT_DOMAIN_PARAMETER, defaultValueConfig))
                .setCreatedDatetime(Parameter.readString(getFormat(), parameters, CREATED_DATETIME_PARAMETER, defaultValueConfig))
                .setTimePeriodStart(Parameter.readString(getFormat(), parameters, TIME_PERIOD_START_PARAMETER, defaultValueConfig))
                .setTimePeriodEnd(Parameter.readString(getFormat(), parameters, TIME_PERIOD_END_PARAMETER, defaultValueConfig));
    }

    private static String formatDateTime(Instant instant) {
        DateTimeFormatter formatter = DateTimeFormatter.ISO_INSTANT.withZone(ZoneId.from(ZoneOffset.UTC));
        // Remove milliseconds
        return formatter.format(instant.with(ChronoField.MILLI_OF_SECOND, 0));
    }
}
