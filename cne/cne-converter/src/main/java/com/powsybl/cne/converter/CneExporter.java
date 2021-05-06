/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.cne.converter;

import com.google.auto.service.AutoService;
import com.powsybl.commons.config.PlatformConfig;
import com.powsybl.iidm.ConversionParameters;
import com.powsybl.iidm.parameters.Parameter;
import com.powsybl.iidm.parameters.ParameterDefaultValueConfig;
import com.powsybl.iidm.parameters.ParameterType;
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
 * @author Thomas Adam <tadam at silicom.fr>
 */
@AutoService(SecurityAnalysisResultExporter.class)
public class CneExporter implements SecurityAnalysisResultExporter {

    private static final Logger LOGGER = LoggerFactory.getLogger(CneExporter.class);

    public static final String PREFIX = "cne.export.xml.";

    private static final Parameter MRID_PARAMETER = new Parameter(PREFIX + MRID, ParameterType.STRING, "mRID", null);
    private static final Parameter SENDER_MARKET_PARTICIPANT_MRID_PARAMETER = new Parameter(PREFIX + SENDER_MARKET_PARTICIPANT_MRID, ParameterType.STRING, "", null);
    private static final Parameter RECEIVER_MARKET_PARTICIPANT_MRID_PARAMETER = new Parameter(PREFIX + RECEIVER_MARKET_PARTICIPANT_MRID, ParameterType.STRING, "", null);
    private static final Parameter TIME_SERIES_MRID_PARAMETER = new Parameter(PREFIX + TIME_SERIES_MRID, ParameterType.STRING, "", null);
    private static final Parameter IN_DOMAIN_PARAMETER = new Parameter(PREFIX + IN_DOMAIN_MRID, ParameterType.STRING, "", null);
    private static final Parameter OUT_DOMAIN_PARAMETER = new Parameter(PREFIX + OUT_DOMAIN_MRID, ParameterType.STRING, "", null);
    private static final Parameter CREATED_DATETIME_PARAMETER = new Parameter(PREFIX + CREATED_DATETIME, ParameterType.STRING, "", formatDateTime(Instant.now()));
    private static final Parameter TIME_PERIOD_START_PARAMETER = new Parameter(PREFIX + TIME_PERIOD + "." + TIME_INTERVAL + "." + START, ParameterType.STRING, "", formatDateTime(Instant.now()));
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
     * - CneConstants.MRID
     * - CneConstants.SENDER_MARKET_PARTICIPANT_MRID
     * - CneConstants.RECEIVER_MARKET_PARTICIPANT_MRID
     * - CneConstants.TIME_SERIES + "." + CneConstants.MRID
     * - CneConstants.IN_DOMAIN_MRID
     * - CneConstants.OUT_DOMAIN_MRID
     *
     * Optional parameters properties key
     * - CneConstants.CREATED_DATETIME
     * - CneConstants.TIME_PERIOD + "." + CneConstants.TIME_INTERVAL + "." + CneConstants.START
     * - CneConstants.TIME_PERIOD + "." + CneConstants.TIME_INTERVAL + "." + CneConstants.END
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

    private ExportOptions createExportOptions(Properties parameters) {
        return new ExportOptions()
                .setMRID(ConversionParameters.readStringParameter(getFormat(), parameters, MRID_PARAMETER, defaultValueConfig))
                .setSenderMarketParticipantMRID(ConversionParameters.readStringParameter(getFormat(), parameters, SENDER_MARKET_PARTICIPANT_MRID_PARAMETER, defaultValueConfig))
                .setReceiverMarketParticipantMRID(ConversionParameters.readStringParameter(getFormat(), parameters, RECEIVER_MARKET_PARTICIPANT_MRID_PARAMETER, defaultValueConfig))
                .setTimeSeriesMRID(ConversionParameters.readStringParameter(getFormat(), parameters, TIME_SERIES_MRID_PARAMETER, defaultValueConfig))
                .setInDomainMRID(ConversionParameters.readStringParameter(getFormat(), parameters, IN_DOMAIN_PARAMETER, defaultValueConfig))
                .setOutDomainMRID(ConversionParameters.readStringParameter(getFormat(), parameters, OUT_DOMAIN_PARAMETER, defaultValueConfig))
                .setCreatedDatetime(ConversionParameters.readStringParameter(getFormat(), parameters, CREATED_DATETIME_PARAMETER, defaultValueConfig))
                .setTimePeriodStart(ConversionParameters.readStringParameter(getFormat(), parameters, TIME_PERIOD_START_PARAMETER, defaultValueConfig))
                .setTimePeriodEnd(ConversionParameters.readStringParameter(getFormat(), parameters, TIME_PERIOD_END_PARAMETER, defaultValueConfig));
    }

    private static String formatDateTime(Instant instant) {
        DateTimeFormatter formatter = DateTimeFormatter.ISO_INSTANT.withZone(ZoneId.from(ZoneOffset.UTC));
        // Remove milliseconds
        return formatter.format(instant.with(ChronoField.MILLI_OF_SECOND, 0));
    }
}
