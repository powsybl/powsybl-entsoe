/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.glsk.nc;

import com.google.auto.service.AutoService;
import com.powsybl.glsk.api.GlskDocument;
import com.powsybl.glsk.api.io.AbstractGlskDocumentImporter;
import com.powsybl.glsk.api.io.GlskDocumentImporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Importer for NC Profile GLSK documents.
 * Detects and imports GLSK data from NC Profile ZIP files containing an RA profile, and optionally SIS and SSI profiles.
 * The detection relies on the header of the profiles (keyword and creator).
 *
 * @author Víctor Cardozo {@literal <victor.cardozo@artelys.com>}
 */
@AutoService(GlskDocumentImporter.class)
public class NcGlskDocumentImporter extends AbstractGlskDocumentImporter implements GlskDocumentImporter {
    private static final Logger LOGGER = LoggerFactory.getLogger(NcGlskDocumentImporter.class);
    static final int HEADER_SIZE = 64 * 1024;
    private static final int MAX_NB_ENTRIES = 200;
    private static final int MAX_SIZE_ENTRY = 1_000_000_000;
    static final Pattern KEYWORD = Pattern.compile("<(?:[\\w.-]+:)?keyword>([^<]*)<");
    private static final Pattern CREATOR = Pattern.compile("<(?:[\\w.-]+:)?creator>([^<]*)<");

    @Override
    public GlskDocument importGlsk(InputStream inputStream) {
        return NcGlskDocument.importGlsk(inputStream);
    }

    @Override
    public boolean canImport(InputStream inputStream) {
        InputStream markableStream = inputStream.markSupported() ? inputStream : new BufferedInputStream(inputStream);
        markableStream.mark(Integer.MAX_VALUE);

        try {
            boolean isNcGlsk = isNcProfileZip(markableStream);
            markableStream.reset();

            if (isNcGlsk) {
                LOGGER.info("NC GLSK importer detected an NC Profile ZIP with an RA profile.");
            } else {
                LOGGER.debug("NC GLSK importer could not detect NC Profile format.");
            }
            return isNcGlsk;
        } catch (IOException e) {
            LOGGER.warn("NC GLSK importer failed to read stream: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Checks that the archive contains an RA profile: without it there is no remedial action to build a GLSK for. The SIS
     * profile (bids and availability schedules) and the SSI profile (availability) are optional: a proportionalForGenerator
     * remedial action needs neither. The profile is recognised by the metadata of its header, never by the name of its file:
     * keyword RA, a creator, and a CountertradeRemedialAction.
     */
    private static boolean isNcProfileZip(InputStream inputStream) {
        try (ZipInputStream zis = new ZipInputStream(inputStream)) {
            ZipEntry entry;
            int countEntries = 0;
            while ((entry = zis.getNextEntry()) != null && countEntries < MAX_NB_ENTRIES) {
                countEntries++;
                if (!entry.isDirectory() && isRaProfile(zis)) {
                    return true;
                }
            }
        } catch (IOException e) {
            // Not a valid ZIP or other error
            return false;
        }
        return false;
    }

    /**
     * @param entry the content of a ZIP entry; the header is read first, and the rest only if the profile is an RA one
     */
    private static boolean isRaProfile(InputStream entry) throws IOException {
        byte[] head = entry.readNBytes(HEADER_SIZE);
        String header = new String(head, StandardCharsets.UTF_8);
        Optional<NcKeyword> keyword = NcKeyword.fromValue(headerValue(KEYWORD, header));
        if (keyword.isEmpty() || keyword.get() != NcKeyword.REMEDIAL_ACTION || headerValue(CREATOR, header) == null) {
            return false;
        }
        String content = header + new String(entry.readNBytes(MAX_SIZE_ENTRY), StandardCharsets.UTF_8);
        return content.contains("CountertradeRemedialAction");
    }

    private static String headerValue(Pattern pattern, String header) {
        Matcher matcher = pattern.matcher(header);
        if (!matcher.find()) {
            return null;
        }
        String value = matcher.group(1).trim();
        return value.isEmpty() ? null : value;
    }
}
