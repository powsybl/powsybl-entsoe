/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.glsk.nc;

import com.powsybl.glsk.api.GlskDocument;
import com.powsybl.glsk.api.io.GlskDocumentImporter;
import com.powsybl.glsk.api.io.GlskDocumentImporters;
import com.powsybl.glsk.commons.GlskException;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static com.powsybl.glsk.nc.NcGlskTestUtil.zipTestCase;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Smoke tests of the NC GLSK importer: it must be discovered through the ServiceLoader and be able to read the RA and
 * SIS profiles of the test resources without failing.
 *
 * @author Víctor Cardozo {@literal <victor.cardozo at artelys.com>}
 */
class NcGlskDocumentImporterTest {

    private static final String RA_PROFILE = "ProportionalFR_RA.xml";
    private static final String FR_BIDDING_ZONE_EIC = "10YFR-RTE------C";

    @Test
    void importerIsDiscoveredByServiceLoader() {
        GlskDocumentImporter importer = GlskDocumentImporters.findImporter(new ByteArrayInputStream(zipTestCase("proportional-fr")));
        assertInstanceOf(NcGlskDocumentImporter.class, importer);
    }

    @Test
    void canImportNcProfiles() {
        NcGlskDocumentImporter importer = new NcGlskDocumentImporter();
        assertTrue(importer.canImport(new ByteArrayInputStream(zipTestCase("proportional-fr", RA_PROFILE))));
        assertTrue(importer.canImport(new ByteArrayInputStream(zipTestCase("proportional-fr"))));
    }

    private static boolean canImport(String... profiles) {
        return new NcGlskDocumentImporter().canImport(new ByteArrayInputStream(zipTestCase("detection", profiles)));
    }

    @Test
    void cannotImportOtherContent() {
        NcGlskDocumentImporter importer = new NcGlskDocumentImporter();
        assertFalse(importer.canImport(new ByteArrayInputStream("not a zip".getBytes(StandardCharsets.UTF_8))));
        assertFalse(canImport("Detection_NotAnNcProfile.xml"));
    }

    @Test
    void canImportProfilesWhateverTheNameOfTheirFile() {
        assertTrue(canImport("Detection_RA.xml"));
        // The name of the file suggests an RA profile, but its header says otherwise
        assertFalse(canImport("Detection_AE_RA_001.xml"));
    }

    @Test
    void anRaProfileIsRequiredAndTheSisProfileIsOptional() {
        // A SIS profile alone has no remedial action to build a GLSK for
        assertFalse(canImport("Detection_SIS.xml"));
        assertTrue(canImport("Detection_RA.xml", "Detection_SIS.xml"));
    }

    @Test
    void headerKeywordAndCreatorAreRequired() {
        assertFalse(canImport("Detection_NoKeyword.xml"));
        assertFalse(canImport("Detection_NoCreator.xml"));
        assertFalse(canImport("Detection_BlankCreator.xml"));
    }

    @Test
    void profileMustCarryGlskData() {
        assertFalse(canImport("Detection_RA_WithoutRemedialAction.xml"));
        assertFalse(canImport("Detection_SIS_WithoutGlskSchedule.xml"));
    }

    @Test
    void importProportionalFrProfiles() {
        GlskDocument document = GlskDocumentImporters.importGlsk(new ByteArrayInputStream(zipTestCase("proportional-fr")));

        assertInstanceOf(NcGlskDocument.class, document);
        assertEquals(List.of(FR_BIDDING_ZONE_EIC), document.getZones());
        assertEquals(1, document.getGlskPoints(FR_BIDDING_ZONE_EIC).size());
    }

    @Test
    void importRaProfileOnly() {
        NcGlskDocument document = NcGlskDocument.importGlsk(new ByteArrayInputStream(zipTestCase("proportional-fr", RA_PROFILE)));

        assertEquals(List.of(FR_BIDDING_ZONE_EIC), document.getZones());
        assertEquals(1, document.getGlskPoints(FR_BIDDING_ZONE_EIC).size());
    }

    @Test
    void noImporterFoundForUnknownContent() {
        ByteArrayInputStream inputStream = new ByteArrayInputStream("not a zip".getBytes(StandardCharsets.UTF_8));
        GlskException exception = assertThrows(GlskException.class, () -> GlskDocumentImporters.importGlsk(inputStream));
        assertEquals(GlskDocumentImporters.NO_IMPORTER_FOUND_FOR_THIS_FILE, exception.getMessage());
    }
}
