/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.glsk.cse;

import com.powsybl.glsk.api.GlskDocument;
import com.powsybl.glsk.api.io.AbstractGlskDocumentImporter;
import com.powsybl.glsk.api.io.GlskDocumentImporter;
import com.google.auto.service.AutoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
@AutoService(GlskDocumentImporter.class)
public class CseGlskDocumentImporter extends AbstractGlskDocumentImporter implements GlskDocumentImporter {
    private static final Logger LOGGER = LoggerFactory.getLogger(CseGlskDocumentImporter.class);

    @Override
    public GlskDocument importGlsk(InputStream inputStream) {
        return importGlsk(inputStream, false);
    }

    @Override
    public GlskDocument importGlsk(InputStream inputStream, boolean useCalculationDirections) {
        return CseGlskDocument.importGlsk(inputStream, useCalculationDirections, false);
    }

    @Override
    public GlskDocument importAndValidateGlsk(InputStream inputStream, boolean useCalculationDirections) {
        return CseGlskDocument.importGlsk(inputStream, useCalculationDirections, true);
    }

    @Override
    public boolean canImport(InputStream inputStream) {
        if (!setDocument(inputStream)) {
            return false;
        }

        if ("GSKDocument".equals(document.getDocumentElement().getTagName()) &&
                document.getDocumentElement().getElementsByTagName("TimeSeries").getLength() > 0) {
            LOGGER.info("CSE GLSK importer could import this document.");
            return true;
        } else {
            LOGGER.info("CSE GLSK importer could not import this document.");
            document = null; // As document is not recognized ensure document is null, in case import method is called afterwards
            return false;
        }
    }
}
