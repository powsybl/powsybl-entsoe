/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.cne.converter;

import com.google.auto.service.AutoService;
import com.powsybl.security.SecurityAnalysisResult;
import com.powsybl.security.converter.SecurityAnalysisResultExporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.util.Properties;

/**
 * @author Thomas Adam <tadam at silicom.fr>
 */
@AutoService(SecurityAnalysisResultExporter.class)
public class CneExporter implements SecurityAnalysisResultExporter {

    private static final Logger LOGGER = LoggerFactory.getLogger(CneExporter.class);

    @Override
    public String getFormat() {
        return "XML";
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
        try {
            long startTime = System.currentTimeMillis();
            SecurityAnalysisResultXml.write(result, parameters, writer);
            LOGGER.debug("CNE export done in {} ms", System.currentTimeMillis() - startTime);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void export(SecurityAnalysisResult result, Writer writer) {
        export(result, new Properties(), writer);
    }
}
