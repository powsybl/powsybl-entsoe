/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.glsk.ucte;

import com.powsybl.glsk.api.GlskDocument;
import com.powsybl.glsk.api.io.AbstractGlskDocumentImporter;
import com.powsybl.glsk.api.io.GlskDocumentImporter;
import com.google.auto.service.AutoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
@AutoService(GlskDocumentImporter.class)
public class UcteGlskDocumentImporter extends AbstractGlskDocumentImporter implements GlskDocumentImporter {
    private static final Logger LOGGER = LoggerFactory.getLogger(UcteGlskDocumentImporter.class);

    @Override
    public GlskDocument importGlsk(InputStream inputStream) {
        if (document != null) {
            return UcteGlskDocument.importGlsk(document);
        }
        return UcteGlskDocument.importGlsk(inputStream);
    }

    @Override
    public boolean canImport(InputStream inputStream) {
        if (!setDocument(inputStream)) {
            return false;
        }

        if ("GSKDocument".equals(document.getDocumentElement().getTagName()) &&
                document.getDocumentElement().getElementsByTagName("GSKSeries").getLength() > 0) {
            LOGGER.info("UCTE GLSK importer could import this document.");
            return true;
        } else {
            LOGGER.info("UCTE GLSK importer could not import this document.");
            document = null; // As document is not recognized ensure document is null, in case import method is called afterwards
            return false;
        }
    }
}
