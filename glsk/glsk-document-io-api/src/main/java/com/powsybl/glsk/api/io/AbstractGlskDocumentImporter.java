/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.glsk.api.io;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public abstract class AbstractGlskDocumentImporter {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractGlskDocumentImporter.class);

    protected Document document;

    protected boolean setDocument(InputStream inputStream) {
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        documentBuilderFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        documentBuilderFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        documentBuilderFactory.setAttribute(XMLConstants.FEATURE_SECURE_PROCESSING, Boolean.TRUE);
        documentBuilderFactory.setNamespaceAware(true);

        try {
            document = documentBuilderFactory.newDocumentBuilder().parse(inputStream);
        } catch (ParserConfigurationException | SAXException | IOException e) {
            LOGGER.warn("An exception occurred trying to read GLSK document. It could not be imported.");
            document = null; // As something failed ensure document is null, in case import method is called afterwards
            return false;
        }
        document.getDocumentElement().normalize();
        return true;
    }
}
