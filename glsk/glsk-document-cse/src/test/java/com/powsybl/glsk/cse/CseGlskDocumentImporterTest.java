/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.glsk.cse;

import com.powsybl.glsk.api.GlskDocument;
import com.powsybl.glsk.api.GlskPoint;
import com.powsybl.glsk.api.GlskRegisteredResource;
import com.powsybl.glsk.api.GlskShiftKey;
import com.powsybl.glsk.api.io.GlskDocumentImporters;
import com.powsybl.glsk.commons.GlskException;
import com.powsybl.glsk.commons.ZonalData;
import com.powsybl.iidm.modification.scalable.Scalable;
import com.powsybl.iidm.network.Network;
import org.apache.commons.lang3.NotImplementedException;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
class CseGlskDocumentImporterTest {
    private static final double EPSILON = 1e-3;

    @Test
    void checkCseGlskDocumentImporterCorrectlyImportManualGskBlocks() {
        CseGlskDocument cseGlskDocument = CseGlskDocument.importGlsk(getClass().getResourceAsStream("/testGlsk.xml"), false);
        List<GlskPoint> list = cseGlskDocument.getGlskPoints("FR_MANUAL");
        assertFalse(list.isEmpty());
        assertEquals(1, list.size());
        assertEquals(1, list.get(0).getGlskShiftKeys().size());
        assertEquals(2, list.get(0).getGlskShiftKeys().get(0).getRegisteredResourceArrayList().size());
    }

    @Test
    void checkCseGlskDocumentImporterCorrectlyConvertManualGskBlocks() {
        Network network = Network.read("testCase.xiidm", getClass().getResourceAsStream("/testCase.xiidm"));
        GlskDocument glskDocument = GlskDocumentImporters.importGlsk(getClass().getResourceAsStream("/testGlsk.xml"));
        Scalable manualScalable = glskDocument.getZonalScalable(network).getData("FR_MANUAL");

        assertNotNull(manualScalable);
        assertEquals(2000., network.getGenerator("FFR1AA1 _generator").getTargetP(), EPSILON);
        assertEquals(3000., network.getGenerator("FFR3AA1 _generator").getTargetP(), EPSILON);

        manualScalable.scale(network, 1000.);
        assertEquals(2700., network.getGenerator("FFR1AA1 _generator").getTargetP(), EPSILON);
        assertEquals(3300., network.getGenerator("FFR3AA1 _generator").getTargetP(), EPSILON);
    }

    @Test
    void checkCseGlskDocumentImporterCorrectlyImportReserveGskBlocks() {
        CseGlskDocument cseGlskDocument = CseGlskDocument.importGlsk(getClass().getResourceAsStream("/testGlsk.xml"), false);
        List<GlskPoint> list = cseGlskDocument.getGlskPoints("FR_RESERVE");
        assertFalse(list.isEmpty());
        assertEquals(1, list.size());
        assertEquals(1, list.get(0).getGlskShiftKeys().size());
        assertEquals(2, list.get(0).getGlskShiftKeys().get(0).getRegisteredResourceArrayList().size());
    }

    @Test
    void checkCseGlskDocumentImporterCorrectlyConvertReserveGskBlocksDown() {
        Network network = Network.read("testCase.xiidm", getClass().getResourceAsStream("/testCase.xiidm"));
        GlskDocument glskDocument = GlskDocumentImporters.importGlsk(getClass().getResourceAsStream("/testGlsk.xml"));
        Scalable reserveScalable = glskDocument.getZonalScalable(network).getData("FR_RESERVE");

        assertNotNull(reserveScalable);
        assertEquals(2000, network.getGenerator("FFR1AA1 _generator").getTargetP(), EPSILON);
        assertEquals(2000, network.getGenerator("FFR2AA1 _generator").getTargetP(), EPSILON);

        assertEquals(-900, reserveScalable.scale(network, -900), EPSILON);
        assertEquals(1400, network.getGenerator("FFR1AA1 _generator").getTargetP(), EPSILON);
        assertEquals(1700, network.getGenerator("FFR2AA1 _generator").getTargetP(), EPSILON);
    }

    @Test
    void checkCseGlskDocumentImporterCorrectlyConvertReserveGskBlocksDownWithReachingLimits() {
        Network network = Network.read("testCase.xiidm", getClass().getResourceAsStream("/testCase.xiidm"));
        GlskDocument glskDocument = GlskDocumentImporters.importGlsk(getClass().getResourceAsStream("/testGlsk.xml"));
        Scalable reserveScalable = glskDocument.getZonalScalable(network).getData("FR_RESERVE");

        assertNotNull(reserveScalable);
        assertEquals(2000, network.getGenerator("FFR1AA1 _generator").getTargetP(), EPSILON);
        assertEquals(2000, network.getGenerator("FFR2AA1 _generator").getTargetP(), EPSILON);

        // 1000 MW missing for down-scaling
        assertEquals(-3000, reserveScalable.scale(network, -4000), EPSILON);
        assertEquals(0, network.getGenerator("FFR1AA1 _generator").getTargetP(), EPSILON);
        assertEquals(1000, network.getGenerator("FFR2AA1 _generator").getTargetP(), EPSILON);
    }

    @Test
    void checkCseGlskDocumentImporterCorrectlyConvertReserveGskBlocksUp() {
        Network network = Network.read("testCase.xiidm", getClass().getResourceAsStream("/testCase.xiidm"));
        GlskDocument glskDocument = GlskDocumentImporters.importGlsk(getClass().getResourceAsStream("/testGlsk.xml"));
        Scalable reserveScalable = glskDocument.getZonalScalable(network).getData("FR_RESERVE");

        assertNotNull(reserveScalable);
        assertEquals(2000, network.getGenerator("FFR1AA1 _generator").getTargetP(), EPSILON);
        assertEquals(2000, network.getGenerator("FFR2AA1 _generator").getTargetP(), EPSILON);

        assertEquals(1000, reserveScalable.scale(network, 1000), EPSILON);
        assertEquals(2600, network.getGenerator("FFR1AA1 _generator").getTargetP(), EPSILON);
        assertEquals(2400, network.getGenerator("FFR2AA1 _generator").getTargetP(), EPSILON);
    }

    @Test
    void checkCseGlskDocumentImporterCorrectlyConvertReserveGskBlocksUpWithReachingLimits() {
        Network network = Network.read("testCase.xiidm", getClass().getResourceAsStream("/testCase.xiidm"));
        GlskDocument glskDocument = GlskDocumentImporters.importGlsk(getClass().getResourceAsStream("/testGlsk.xml"));
        Scalable reserveScalable = glskDocument.getZonalScalable(network).getData("FR_RESERVE");

        assertNotNull(reserveScalable);
        assertEquals(2000, network.getGenerator("FFR1AA1 _generator").getTargetP(), EPSILON);
        assertEquals(2000, network.getGenerator("FFR2AA1 _generator").getTargetP(), EPSILON);

        // 1000 MW missing for up-scaling
        assertEquals(5000, reserveScalable.scale(network, 6000), EPSILON);
        assertEquals(5000, network.getGenerator("FFR1AA1 _generator").getTargetP(), EPSILON);
        assertEquals(4000, network.getGenerator("FFR2AA1 _generator").getTargetP(), EPSILON);
    }

    @Test
    void checkCseGlskDocumentImporterCorrectlyImportPropGskBlocks() {
        CseGlskDocument cseGlskDocument = CseGlskDocument.importGlsk(getClass().getResourceAsStream("/testGlsk.xml"), false);
        List<GlskPoint> list = cseGlskDocument.getGlskPoints("FR_PROPGSK");
        assertFalse(list.isEmpty());
        assertEquals(1, list.size());
        assertEquals(1, list.get(0).getGlskShiftKeys().size());
        assertEquals(3, list.get(0).getGlskShiftKeys().get(0).getRegisteredResourceArrayList().size());
    }

    @Test
    void checkCseGlskDocumentImporterCorrectlyConvertPropGskBlocks() {
        Network network = Network.read("testCase.xiidm", getClass().getResourceAsStream("/testCase.xiidm"));
        GlskDocument glskDocument = GlskDocumentImporters.importGlsk(getClass().getResourceAsStream("/testGlsk.xml"));
        Scalable propGskScalable = glskDocument.getZonalScalable(network).getData("FR_PROPGSK");

        assertNotNull(propGskScalable);
        assertEquals(2000., network.getGenerator("FFR1AA1 _generator").getTargetP(), EPSILON);
        assertEquals(2000., network.getGenerator("FFR2AA1 _generator").getTargetP(), EPSILON);
        assertEquals(3000., network.getGenerator("FFR3AA1 _generator").getTargetP(), EPSILON);

        propGskScalable.scale(network, 700.);
        assertEquals(2200., network.getGenerator("FFR1AA1 _generator").getTargetP(), EPSILON);
        assertEquals(2200., network.getGenerator("FFR2AA1 _generator").getTargetP(), EPSILON);
        assertEquals(3300., network.getGenerator("FFR3AA1 _generator").getTargetP(), EPSILON);
    }

    @Test
    void checkCseGlskDocumentImporterCorrectlyImportPropGlskBlocks() {
        CseGlskDocument cseGlskDocument = CseGlskDocument.importGlsk(getClass().getResourceAsStream("/testGlsk.xml"), false);
        List<GlskPoint> list = cseGlskDocument.getGlskPoints("FR_PROPGLSK");
        assertFalse(list.isEmpty());
        assertEquals(1, list.size());
        assertEquals(2, list.get(0).getGlskShiftKeys().size());
        assertEquals(2, list.get(0).getGlskShiftKeys().get(0).getRegisteredResourceArrayList().size());
        assertEquals(3, list.get(0).getGlskShiftKeys().get(1).getRegisteredResourceArrayList().size());
    }

    @Test
    void checkCseGlskDocumentImporterCorrectlyConvertPropGlskBlocks() {
        Network network = Network.read("testCase.xiidm", getClass().getResourceAsStream("/testCase.xiidm"));
        GlskDocument glskDocument = GlskDocumentImporters.importGlsk(getClass().getResourceAsStream("/testGlsk.xml"));
        Scalable propGlskScalable = glskDocument.getZonalScalable(network).getData("FR_PROPGLSK");

        assertNotNull(propGlskScalable);
        assertEquals(2000., network.getGenerator("FFR1AA1 _generator").getTargetP(), EPSILON);
        assertEquals(2000., network.getGenerator("FFR2AA1 _generator").getTargetP(), EPSILON);
        assertEquals(3000., network.getGenerator("FFR3AA1 _generator").getTargetP(), EPSILON);
        assertEquals(1000., network.getLoad("FFR1AA1 _load").getP0(), EPSILON);
        assertEquals(3500., network.getLoad("FFR2AA1 _load").getP0(), EPSILON);

        propGlskScalable.scale(network, 1000.);
        assertEquals(2200., network.getGenerator("FFR1AA1 _generator").getTargetP(), EPSILON);
        assertEquals(2200., network.getGenerator("FFR2AA1 _generator").getTargetP(), EPSILON);
        assertEquals(3300., network.getGenerator("FFR3AA1 _generator").getTargetP(), EPSILON);
        assertEquals(933.3334, network.getLoad("FFR1AA1 _load").getP0(), EPSILON);
        assertEquals(3266.6667, network.getLoad("FFR2AA1 _load").getP0(), EPSILON);
    }

    @Test
    void checkCseGlskDocumentImporterCorrectlyConvertPropLskBlocksWithPassingLoadsAsGenerators() {
        Network network = Network.read("testCase.xiidm", getClass().getResourceAsStream("/testCase.xiidm"));
        GlskDocument glskDocument = GlskDocumentImporters.importGlsk(getClass().getResourceAsStream("/testGlsk.xml"));
        Scalable propGlskScalable = glskDocument.getZonalScalable(network).getData("FR_PROPGLSK");

        propGlskScalable.scale(network, 20000);
        assertEquals(-333.3334, network.getLoad("FFR1AA1 _load").getP0(), EPSILON);
        assertEquals(-1166.6667, network.getLoad("FFR2AA1 _load").getP0(), EPSILON);
    }

    @Test
    void checkCseGlskDocumentImporterCorrectlyImportMeritOrderGskBlocks() {
        CseGlskDocument cseGlskDocument = CseGlskDocument.importGlsk(getClass().getResourceAsStream("/testGlsk.xml"), false);
        List<GlskPoint> list = cseGlskDocument.getGlskPoints("FR_MERITORDER");
        assertFalse(list.isEmpty());
        assertEquals(1, list.size());
        assertEquals(7, list.get(0).getGlskShiftKeys().size());
        assertEquals(1, list.get(0).getGlskShiftKeys().get(0).getRegisteredResourceArrayList().size());
        assertEquals(1, list.get(0).getGlskShiftKeys().get(1).getRegisteredResourceArrayList().size());
        assertEquals(1, list.get(0).getGlskShiftKeys().get(2).getRegisteredResourceArrayList().size());
        assertEquals(1, list.get(0).getGlskShiftKeys().get(3).getRegisteredResourceArrayList().size());
        assertEquals(1, list.get(0).getGlskShiftKeys().get(1).getRegisteredResourceArrayList().size());
        assertEquals(1, list.get(0).getGlskShiftKeys().get(2).getRegisteredResourceArrayList().size());
        assertEquals(1, list.get(0).getGlskShiftKeys().get(3).getRegisteredResourceArrayList().size());
    }

    @Test
    void checkCseGlskDocumentImporterCorrectlyConvertMeritOrderGskBlocksDown() {
        Network network = Network.read("testCase.xiidm", getClass().getResourceAsStream("/testCase.xiidm"));
        GlskDocument glskDocument = GlskDocumentImporters.importGlsk(getClass().getResourceAsStream("/testGlsk.xml"));
        Scalable meritOrderGskScalable = glskDocument.getZonalScalable(network).getData("FR_MERITORDER");

        assertNotNull(meritOrderGskScalable);
        assertEquals(2000., network.getGenerator("FFR1AA1 _generator").getTargetP(), EPSILON);
        assertEquals(2000., network.getGenerator("FFR2AA1 _generator").getTargetP(), EPSILON);
        assertEquals(3000., network.getGenerator("FFR3AA1 _generator").getTargetP(), EPSILON);

        meritOrderGskScalable.scale(network, -4000.);
        assertEquals(2000., network.getGenerator("FFR1AA1 _generator").getTargetP(), EPSILON);
        assertEquals(1000., network.getGenerator("FFR2AA1 _generator").getTargetP(), EPSILON);
        assertEquals(0., network.getGenerator("FFR3AA1 _generator").getTargetP(), EPSILON);
    }

    @Test
    void checkCseGlskDocumentImporterCorrectlyConvertMeritOrderGskBlocksUp() {
        Network network = Network.read("testCase.xiidm", getClass().getResourceAsStream("/testCase.xiidm"));
        GlskDocument glskDocument = GlskDocumentImporters.importGlsk(getClass().getResourceAsStream("/testGlsk.xml"));
        Scalable meritOrderGskScalable = glskDocument.getZonalScalable(network).getData("FR_MERITORDER");

        assertNotNull(meritOrderGskScalable);
        assertEquals(2000., network.getGenerator("FFR1AA1 _generator").getTargetP(), EPSILON);
        assertEquals(2000., network.getGenerator("FFR2AA1 _generator").getTargetP(), EPSILON);
        assertEquals(3000., network.getGenerator("FFR3AA1 _generator").getTargetP(), EPSILON);

        meritOrderGskScalable.scale(network, 5000.);
        assertEquals(5000., network.getGenerator("FFR1AA1 _generator").getTargetP(), EPSILON);
        assertEquals(4000., network.getGenerator("FFR2AA1 _generator").getTargetP(), EPSILON);
        assertEquals(3000., network.getGenerator("FFR3AA1 _generator").getTargetP(), EPSILON);
    }

    @Test
    void checkCseGlskDocumentImporterCorrectlyConvertMeritOrderGskBlocksWithTargetPIssueDown() {
        Network network = Network.read("testCase.xiidm", getClass().getResourceAsStream("/testCase.xiidm"));
        GlskDocument glskDocument = GlskDocumentImporters.importGlsk(getClass().getResourceAsStream("/testGlsk.xml"));
        Scalable meritOrderGskScalable = glskDocument.getZonalScalable(network).getData("FR_MERIT_ISSUE_PC");

        assertNotNull(meritOrderGskScalable);
        assertEquals(2000., network.getGenerator("FFR1AA1 _generator").getTargetP(), EPSILON);
        assertEquals(2000., network.getGenerator("FFR2AA1 _generator").getTargetP(), EPSILON);
        assertEquals(3000., network.getGenerator("FFR3AA1 _generator").getTargetP(), EPSILON);

        meritOrderGskScalable.scale(network, -4000.);
        assertEquals(2000., network.getGenerator("FFR1AA1 _generator").getTargetP(), EPSILON);
        assertEquals(1000., network.getGenerator("FFR2AA1 _generator").getTargetP(), EPSILON);
        assertEquals(0., network.getGenerator("FFR3AA1 _generator").getTargetP(), EPSILON);
    }

    @Test
    void checkCseGlskDocumentImporterCorrectlyConvertMeritOrderGskBlocksWithTargetPIssueUp() {
        Network network = Network.read("testCase.xiidm", getClass().getResourceAsStream("/testCase.xiidm"));
        GlskDocument glskDocument = GlskDocumentImporters.importGlsk(getClass().getResourceAsStream("/testGlsk.xml"));
        Scalable meritOrderGskScalable = glskDocument.getZonalScalable(network).getData("FR_MERIT_ISSUE_PC");

        assertNotNull(meritOrderGskScalable);
        assertEquals(2000., network.getGenerator("FFR1AA1 _generator").getTargetP(), EPSILON);
        assertEquals(2000., network.getGenerator("FFR2AA1 _generator").getTargetP(), EPSILON);
        assertEquals(3000., network.getGenerator("FFR3AA1 _generator").getTargetP(), EPSILON);

        meritOrderGskScalable.scale(network, 5000.);
        assertEquals(2000., network.getGenerator("FFR1AA1 _generator").getTargetP(), EPSILON);
        assertEquals(4000., network.getGenerator("FFR2AA1 _generator").getTargetP(), EPSILON);
        assertEquals(6000., network.getGenerator("FFR3AA1 _generator").getTargetP(), EPSILON);
    }

    @Test
    void checkCseGlskDocumentImporterWithHybridGskBelowMaximumShift() {
        Network network = Network.read("testCase.xiidm", getClass().getResourceAsStream("/testCase.xiidm"));
        GlskDocument glskDocument = GlskDocumentImporters.importGlsk(getClass().getResourceAsStream("/testGlskHybrid.xml"));
        Scalable meritOrderGskScalable = glskDocument.getZonalScalable(network).getData("10YCH-SWISSGRIDZ");

        assertNotNull(meritOrderGskScalable);
        assertEquals(2000., network.getGenerator("FFR1AA1 _generator").getTargetP(), EPSILON);
        assertEquals(2000., network.getGenerator("FFR2AA1 _generator").getTargetP(), EPSILON);
        assertEquals(3000., network.getGenerator("FFR3AA1 _generator").getTargetP(), EPSILON);

        meritOrderGskScalable.scale(network, 1000.);
        assertEquals(2500., network.getGenerator("FFR1AA1 _generator").getTargetP(), EPSILON);
        assertEquals(2500., network.getGenerator("FFR2AA1 _generator").getTargetP(), EPSILON);
        assertEquals(3000., network.getGenerator("FFR3AA1 _generator").getTargetP(), EPSILON);
    }

    @Test
    void checkCseGlskDocumentImporterWithHybridGskAboveMaximumShift() {
        Network network = Network.read("testCase.xiidm", getClass().getResourceAsStream("/testCase.xiidm"));
        GlskDocument glskDocument = GlskDocumentImporters.importGlsk(getClass().getResourceAsStream("/testGlskHybrid.xml"));
        Scalable meritOrderGskScalable = glskDocument.getZonalScalable(network).getData("10YCH-SWISSGRIDZ");

        assertNotNull(meritOrderGskScalable);
        assertEquals(2000., network.getGenerator("FFR1AA1 _generator").getTargetP(), EPSILON);
        assertEquals(2000., network.getGenerator("FFR2AA1 _generator").getTargetP(), EPSILON);
        assertEquals(3000., network.getGenerator("FFR3AA1 _generator").getTargetP(), EPSILON);

        meritOrderGskScalable.scale(network, 1500.);
        assertEquals(2500., network.getGenerator("FFR1AA1 _generator").getTargetP(), EPSILON);
        assertEquals(2500., network.getGenerator("FFR2AA1 _generator").getTargetP(), EPSILON);
        assertEquals(3500., network.getGenerator("FFR3AA1 _generator").getTargetP(), EPSILON);
    }

    @Test
    void checkCseGlskDocumentImporterWithHybridGskGoingDown() {
        Network network = Network.read("testCase.xiidm", getClass().getResourceAsStream("/testCase.xiidm"));
        GlskDocument glskDocument = GlskDocumentImporters.importGlsk(getClass().getResourceAsStream("/testGlskHybrid.xml"));
        Scalable meritOrderGskScalable = glskDocument.getZonalScalable(network).getData("10YCH-SWISSGRIDZ");

        assertNotNull(meritOrderGskScalable);
        assertEquals(2000., network.getGenerator("FFR1AA1 _generator").getTargetP(), EPSILON);
        assertEquals(2000., network.getGenerator("FFR2AA1 _generator").getTargetP(), EPSILON);
        assertEquals(3000., network.getGenerator("FFR3AA1 _generator").getTargetP(), EPSILON);

        meritOrderGskScalable.scale(network, -500.);
        assertEquals(2000., network.getGenerator("FFR1AA1 _generator").getTargetP(), EPSILON);
        assertEquals(2000., network.getGenerator("FFR2AA1 _generator").getTargetP(), EPSILON);
        assertEquals(2500., network.getGenerator("FFR3AA1 _generator").getTargetP(), EPSILON);
    }

    @Test
    void checkRemainingCapacityWithDifferentKindOfInitialLimitations() {
        // BBE1 has initially no remaining up capacity due to network limitation but BBE3 has remaining up capacity
        // NNL1 has initially no remaining up capacity due to GLSK limits but NNL2 has remaining up capacity
        // FFR1 has initially no remaining up capacity due to network limitation and FFR2 has initially no remaining up capacity due to GLSK limits
        Network network = Network.read("testCaseWithInitialLimits.xiidm", getClass().getResourceAsStream("/testCaseWithInitialLimits.xiidm"));
        GlskDocument glskDocument = GlskDocumentImporters.importGlsk(getClass().getResourceAsStream("/testGlskWithInitialLimits.xml"));

        assertEquals(1500, network.getGenerator("BBE1AA1 _generator").getTargetP(), EPSILON);
        assertEquals(2500, network.getGenerator("BBE3AA1 _generator").getTargetP(), EPSILON);
        assertEquals(6500, glskDocument.getZonalScalable(network).getData("BE_RESERVE").scale(network, 10000), EPSILON);
        assertEquals(1500, network.getGenerator("BBE1AA1 _generator").getTargetP(), EPSILON);
        assertEquals(9000, network.getGenerator("BBE3AA1 _generator").getTargetP(), EPSILON);

        assertEquals(1500, network.getGenerator("NNL1AA1 _generator").getTargetP(), EPSILON);
        assertEquals(500, network.getGenerator("NNL2AA1 _generator").getTargetP(), EPSILON);
        assertEquals(500, glskDocument.getZonalScalable(network).getData("NL_RESERVE").scale(network, 500), EPSILON);
        assertEquals(1500, network.getGenerator("NNL1AA1 _generator").getTargetP(), EPSILON);
        assertEquals(1000, network.getGenerator("NNL2AA1 _generator").getTargetP(), EPSILON);

        assertEquals(0, glskDocument.getZonalScalable(network).getData("FR_RESERVE").scale(network, 6000), EPSILON);
    }

    @Test
    void checkFactorTagIsOptional() {
        Network network = Network.read("testCase.xiidm", getClass().getResourceAsStream("/testCase.xiidm"));
        InputStream is = getClass().getResourceAsStream("/testGlskWithMissingFactorTag.xml");
        ZonalData<Scalable> zs = CseGlskDocument.importGlsk(is, false).getZonalScalable(network);

        assertEquals(4, zs.getDataPerZone().size());
    }

    @Test
    void checkGlskExceptionWhenMissingTag() {
        InputStream is = getClass().getResourceAsStream("/testGlskMissingTag.xml");
        assertThrows(GlskException.class, () -> CseGlskDocument.importGlsk(is, false));
    }

    @Test
    void checkCseGlskDocumentImporterCorrectlyImportMergedGlsk() {
        CseGlskDocument cseGlskDocument = CseGlskDocument.importGlsk(getClass().getResourceAsStream("/testGlskMerged.xml"), true);
        assertNotNull(cseGlskDocument);
        assertEquals(5, cseGlskDocument.getZones().size());

        List<GlskPoint> atPoints = cseGlskDocument.getGlskPoints("10YAT-APG------L");
        assertEquals(1, atPoints.size());
        List<GlskShiftKey> atGlskShiftKeys = atPoints.get(0).getGlskShiftKeys();
        assertEquals(1, atGlskShiftKeys.size());
        assertEquals(0.95, atGlskShiftKeys.get(0).getQuantity());
        List<GlskRegisteredResource> atRegisteredResources = atGlskShiftKeys.get(0).getRegisteredResourceArrayList();
        assertEquals(2, atRegisteredResources.size());
        assertEquals("AT01AA01", atRegisteredResources.get(0).getName());
        assertEquals(0.7, atRegisteredResources.get(0).getParticipationFactor());
        assertEquals("AT02AA01", atRegisteredResources.get(1).getName());
        assertEquals(0.3, atRegisteredResources.get(1).getParticipationFactor());

        List<GlskPoint> chPoints = cseGlskDocument.getGlskPoints("10YCH-SWISSGRIDZ");
        assertEquals(1, chPoints.size());
        List<GlskShiftKey> chGlskShiftKeys = chPoints.get(0).getGlskShiftKeys();
        assertEquals(1, chGlskShiftKeys.size());
        assertEquals(1.2, chGlskShiftKeys.get(0).getQuantity());
        List<GlskRegisteredResource> chRegisteredResources = chGlskShiftKeys.get(0).getRegisteredResourceArrayList();
        assertEquals(1, chRegisteredResources.size());
        assertEquals("CH01AA01", chRegisteredResources.get(0).getName());
        assertEquals(200.0, chRegisteredResources.get(0).getMaximumCapacity().get());
        assertEquals(-10.0, chRegisteredResources.get(0).getMinimumCapacity().get());

        List<GlskPoint> frPoints = cseGlskDocument.getGlskPoints("10YFR-RTE------C");
        assertEquals(1, frPoints.size());
        List<GlskShiftKey> frGlskShiftKeys = frPoints.get(0).getGlskShiftKeys();
        assertEquals(1, frGlskShiftKeys.size());
        assertEquals(1.05, frGlskShiftKeys.get(0).getQuantity());
        List<GlskRegisteredResource> frRegisteredResources = frGlskShiftKeys.get(0).getRegisteredResourceArrayList();
        assertEquals(3, frRegisteredResources.size());
        assertEquals("FR01AA01", frRegisteredResources.get(0).getName());
        assertEquals("FR02AA01", frRegisteredResources.get(1).getName());
        assertEquals("FR03AA01", frRegisteredResources.get(2).getName());

        List<GlskPoint> itPoints = cseGlskDocument.getGlskPoints("10YIT-GRTN-----B");
        assertEquals(1, itPoints.size());
        List<GlskShiftKey> itGlskShiftKeys = itPoints.get(0).getGlskShiftKeys();
        assertEquals(2, itGlskShiftKeys.size());
        assertEquals(1.15, itGlskShiftKeys.get(0).getQuantity());
        assertEquals(1, itGlskShiftKeys.get(0).getMeritOrderPosition());
        List<GlskRegisteredResource> itFirstRegisteredResources = itGlskShiftKeys.get(0).getRegisteredResourceArrayList();
        assertEquals(1, itFirstRegisteredResources.size());
        assertEquals("IT01AA01", itFirstRegisteredResources.get(0).getName());
        assertEquals(1000, itFirstRegisteredResources.get(0).getMaximumCapacity().get());
        assertEquals(1.15, itGlskShiftKeys.get(1).getQuantity());
        assertEquals(-1, itGlskShiftKeys.get(1).getMeritOrderPosition());
        List<GlskRegisteredResource> itSecondRegisteredResources = itGlskShiftKeys.get(1).getRegisteredResourceArrayList();
        assertEquals(1, itSecondRegisteredResources.size());
        assertEquals("IT01AA02", itSecondRegisteredResources.get(0).getName());
        assertEquals(0, itSecondRegisteredResources.get(0).getMinimumCapacity().get());

        List<GlskPoint> siPoints = cseGlskDocument.getGlskPoints("10YSI-ELES-----O");
        assertEquals(1, siPoints.size());
        List<GlskShiftKey> siGlskShiftKeys = siPoints.get(0).getGlskShiftKeys();
        assertEquals(2, siGlskShiftKeys.size());
        assertEquals(0.3, siGlskShiftKeys.get(0).getQuantity());
        List<GlskRegisteredResource> siFirstRegisteredResources = siGlskShiftKeys.get(0).getRegisteredResourceArrayList();
        assertEquals(2, siFirstRegisteredResources.size());
        assertEquals("SI02AA01", siFirstRegisteredResources.get(0).getName());
        assertEquals("SI02AA02", siFirstRegisteredResources.get(1).getName());
        assertEquals(0.7, siGlskShiftKeys.get(1).getQuantity());
        List<GlskRegisteredResource> siSecondRegisteredResources = siGlskShiftKeys.get(1).getRegisteredResourceArrayList();
        assertEquals(1, siSecondRegisteredResources.size());
        assertEquals("SI01AA01", siSecondRegisteredResources.get(0).getName());
    }

    @Test
    void checkExceptionWhenCallingNotImplementedFeatures() {
        Network network = Network.read("testCase.xiidm", getClass().getResourceAsStream("/testCase.xiidm"));
        GlskDocument glskDocument = GlskDocumentImporters.importGlsk(getClass().getResourceAsStream("/testGlsk.xml"));
        assertThrows(NotImplementedException.class, () -> glskDocument.getZonalScalableChronology(network));
        assertThrows(NotImplementedException.class, () -> glskDocument.getZonalGlsksChronology(network));
        assertThrows(NotImplementedException.class, () -> glskDocument.getZonalScalable(network, Instant.now()));
        assertThrows(NotImplementedException.class, () -> glskDocument.getZonalGlsks(network, Instant.now()));
    }
}
