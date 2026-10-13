/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.glsk.nc;

import com.powsybl.glsk.api.GlskDocument;
import com.powsybl.glsk.api.GlskPoint;
import com.powsybl.glsk.api.util.ZonalDataChronologyFromGlskDocument;
import com.powsybl.glsk.api.util.ZonalDataFromGlskDocument;
import com.powsybl.glsk.api.util.converters.GlskPointScalableConverter;
import com.powsybl.glsk.commons.GlskException;
import com.powsybl.glsk.commons.ZonalData;
import com.powsybl.glsk.commons.ZonalDataChronology;
import com.powsybl.iidm.modification.scalable.Scalable;
import com.powsybl.iidm.network.Network;
import com.powsybl.triplestore.api.TripleStore;
import com.powsybl.triplestore.api.TripleStoreFactory;
import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * NC Profile GLSK Document implementation.
 * Imports GLSK data from NC Profiles (RA + SIS) in ZIP format.
 *
 * @author Víctor Cardozo {@literal <victor.cardozo@artelys.com>}
 */
public final class NcGlskDocument implements GlskDocument {

    private static final Logger LOGGER = LoggerFactory.getLogger(NcGlskDocument.class);

    /**
     * GLSK points grouped by zone (EIC code)
     */
    private final Map<String, List<GlskPoint>> glskPointsByZone;

    /**
     * Document validity start instant
     */
    private final Instant instantStart;

    /**
     * Document validity end instant
     */
    private final Instant instantEnd;

    /**
     * Private constructor - use importGlsk() static method
     */
    private NcGlskDocument(Map<String, List<GlskPoint>> glskPointsByZone, Instant instantStart, Instant instantEnd) {
        this.glskPointsByZone = glskPointsByZone;
        this.instantStart = instantStart;
        this.instantEnd = instantEnd;
    }

    /**
     * Import GLSK document from NC Profiles ZIP file.
     *
     * @param zipInputStream Input stream of ZIP file containing RA and SIS profiles
     * @return NcGlskDocument instance
     */
    public static NcGlskDocument importGlsk(InputStream zipInputStream) {
        try {
            // Load ZIP contents into TripleStore
            TripleStore tripleStore = loadZipToTripleStore(zipInputStream);

            // Parse NC Profiles and build GLSK structure
            NcGlskDatasetParser parser = new NcGlskDatasetParser(tripleStore);
            Map<String, List<GlskPoint>> pointsByZone = parser.parse();
            Instant start = parser.getValidityStart();
            Instant end = parser.getValidityEnd();

            // Use current time as fallback if headers not found
            if (start == null) {
                start = Instant.now();
            }
            if (end == null) {
                end = start.plus(Duration.ofHours(24));
            }

            return new NcGlskDocument(pointsByZone, start, end);

        } catch (IOException e) {
            throw new GlskException("Failed to import NC GLSK document from ZIP", e);
        }
    }

    /**
     * Load ZIP file contents into TripleStore.
     *
     * @param zipInputStream Input stream of ZIP file
     * @return TripleStore with loaded RDF data
     * @throws IOException if ZIP reading fails
     */
    private static TripleStore loadZipToTripleStore(InputStream zipInputStream) throws IOException {
        TripleStore tripleStore = TripleStoreFactory.create(NcConstants.TRIPLESTORE_RDF4J_NAME);
        Map<String, Set<String>> keywordMap = new HashMap<>();
        Pattern keywordPattern = Pattern.compile("<dcat:keyword>([A-Z]{2,3})</dcat:keyword>");

        int maxNbEntries = 200;
        int maxSizeEntry = 1_000_000_000;
        int countEntries = 0;

        try (ZipInputStream zis = new ZipInputStream(zipInputStream)) {
            ZipEntry zipEntry;
            while ((zipEntry = zis.getNextEntry()) != null && countEntries < maxNbEntries) {
                countEntries++;
                if (!zipEntry.isDirectory()) {
                    loadZipEntry(zipEntry, zis, maxSizeEntry, keywordPattern, keywordMap, tripleStore);
                }
            }
        }

        return tripleStore;
    }

    /**
     * Load individual ZIP entry into TripleStore
     */
    private static void loadZipEntry(ZipEntry zipEntry,
                                     ZipInputStream zipInputStream,
                                     int maxSizeEntry,
                                     Pattern keywordPattern,
                                     Map<String, Set<String>> keywordMap,
                                     TripleStore tripleStore) throws IOException {
        File tempFile = createTempFile();
        int currentSizeEntry = 0;

        try {
            // Write ZIP entry to temporary file
            try (OutputStream out = new BufferedOutputStream(new FileOutputStream(tempFile))) {
                InputStream in = new BufferedInputStream(zipInputStream);
                byte[] buffer = new byte[2048];
                int nBytes;

                while ((nBytes = in.read(buffer)) > 0 && currentSizeEntry < maxSizeEntry) {
                    out.write(buffer, 0, nBytes);
                    currentSizeEntry += nBytes;

                    // Extract keyword from buffer
                    String stringBuffer = new String(buffer, StandardCharsets.UTF_8);
                    Matcher matcher = keywordPattern.matcher(stringBuffer);
                    if (matcher.find()) {
                        String keyword = matcher.group(1);
                        keywordMap.computeIfAbsent(keyword, k -> new HashSet<>())
                                  .add("contexts:" + zipEntry.getName());
                    }
                }
            }

            // Read RDF from temporary file into TripleStore
            try (FileInputStream fileInputStream = new FileInputStream(tempFile)) {
                // All the entries are read with the same base URL, so that an association of a profile (rdf:resource) to an object
                // declared in another one (rdf:ID) is resolved: it is what links the bids of the SIS profile to the RA of the RA profile
                tripleStore.read(fileInputStream, NcConstants.RDF_BASE_URL, zipEntry.getName());
            }

        } finally {
            // Cleanup temporary file
            try {
                Files.delete(tempFile.toPath());
            } catch (IOException e) {
                tempFile.deleteOnExit();
            }
        }
    }

    /**
     * Create temporary file (OS-specific permissions)
     */
    private static File createTempFile() throws IOException {
        if (SystemUtils.IS_OS_UNIX) {
            FileAttribute<Set<PosixFilePermission>> attr =
                PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwx------"));
            return Files.createTempFile("ncGlsk", ".tmp", attr).toFile();
        } else {
            File tempFile = Files.createTempFile("ncGlsk", ".tmp").toFile();

            boolean tempFileOk = tempFile.setReadable(true, true) &&
                    tempFile.setWritable(true, true) &&
                    tempFile.setExecutable(true, true);
            if (!tempFileOk) {
                LOGGER.warn("Failed to set permissions on temporary file: {}", tempFile.getAbsolutePath());
            }
            return tempFile;
        }
    }

    @Override
    public List<String> getZones() {
        return new ArrayList<>(glskPointsByZone.keySet());
    }

    @Override
    public List<GlskPoint> getGlskPoints(String zone) {
        return glskPointsByZone.getOrDefault(zone, Collections.emptyList());
    }

    @Override
    public ZonalData<Scalable> getZonalScalable(Network network) {
        return new ZonalDataFromGlskDocument<>(this, network, NcGlskDocument::toScalable);
    }

    @Override
    public ZonalData<Scalable> getZonalScalable(Network network, Instant instant) {
        return new ZonalDataFromGlskDocument<>(this, network, NcGlskDocument::toScalable, instant);
    }

    @Override
    public ZonalDataChronology<Scalable> getZonalScalableChronology(Network network) {
        return new ZonalDataChronologyFromGlskDocument<>(this, network, NcGlskDocument::toScalable);
    }

    /**
     * Converts a GLSK point to a scalable. The resources of a merit order are first resolved against the network, as the
     * generating units of the SIS profile are not necessarily the ids of the network elements. If none of them can be
     * resolved, there is nothing to scale: the scalable is empty.
     */
    private static Scalable toScalable(Network network, GlskPoint glskPoint) {
        return NcGlskResourceResolver.resolve(network, glskPoint)
            .map(resolvedPoint -> GlskPointScalableConverter.convert(network, resolvedPoint))
            .orElseGet(() -> {
                LOGGER.warn("No resource of the merit order of zone {} could be found in the network: there is nothing to scale",
                    glskPoint.getSubjectDomainmRID());
                Scalable[] noScalable = {};
                return Scalable.upDown(Scalable.stack(noScalable), Scalable.stack(noScalable));
            });
    }

    /**
     * @return Document validity start instant
     */
    public Instant getInstantStart() {
        return instantStart;
    }

    /**
     * @return Document validity end instant
     */
    public Instant getInstantEnd() {
        return instantEnd;
    }
}
