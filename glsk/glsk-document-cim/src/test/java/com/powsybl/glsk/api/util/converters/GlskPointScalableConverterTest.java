/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.glsk.api.util.converters;

import com.powsybl.glsk.api.GlskPoint;
import com.powsybl.glsk.cim.CimGlskDocument;
import com.powsybl.iidm.modification.scalable.Scalable;
import com.powsybl.iidm.network.Network;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Pengbo Wang {@literal <pengbo.wang@rte-international.com>}
 * @author Peter Mitri {@literal <peter.mitri@rte-france.com>}
 */
class GlskPointScalableConverterTest {
    private static final double DOUBLE_TOLERANCE = 1e-3;
    private static final double FUZZY_TOLERANCE = 0.5;

    private Network testNetwork;

    private InputStream getResourceAsStream(String resource) {
        return getClass().getResourceAsStream(resource);
    }

    @BeforeEach
    void setUp() {
        testNetwork = Network.read("testCase.xiidm", getClass().getResourceAsStream("/testCase.xiidm"));
    }

    /**
     *  test for Scalable
     */
    @Test
    void testConvertGlskPointToScalableB45MeritOrder() {
        GlskPoint glsk = CimGlskDocument.importGlsk(getResourceAsStream("/GlskB45test.xml")).getGlskPoints().get(0);
        Scalable scalable = GlskPointScalableConverter.convert(testNetwork, glsk);

        assertNotNull(scalable);
        assertEquals(2000., testNetwork.getGenerator("FFR1AA1 _generator").getTargetP(), DOUBLE_TOLERANCE);
        assertEquals(2000., testNetwork.getGenerator("FFR2AA1 _generator").getTargetP(), DOUBLE_TOLERANCE);
        assertEquals(3000., testNetwork.getGenerator("FFR3AA1 _generator").getTargetP(), DOUBLE_TOLERANCE);
        assertEquals(1000., testNetwork.getLoad("FFR1AA1 _load").getP0(), DOUBLE_TOLERANCE);
        assertEquals(3500., testNetwork.getLoad("FFR2AA1 _load").getP0(), DOUBLE_TOLERANCE);
        assertEquals(1500., testNetwork.getLoad("FFR3AA1 _load").getP0(), DOUBLE_TOLERANCE);

        double done = scalable.scale(testNetwork, 100.);
        assertEquals(6., done, DOUBLE_TOLERANCE);
        assertEquals(2001., testNetwork.getGenerator("FFR1AA1 _generator").getTargetP(), DOUBLE_TOLERANCE);
        assertEquals(2002., testNetwork.getGenerator("FFR2AA1 _generator").getTargetP(), DOUBLE_TOLERANCE);
        assertEquals(3003., testNetwork.getGenerator("FFR3AA1 _generator").getTargetP(), DOUBLE_TOLERANCE);
        assertEquals(1000., testNetwork.getLoad("FFR1AA1 _load").getP0(), DOUBLE_TOLERANCE); // untouched
        assertEquals(3500., testNetwork.getLoad("FFR2AA1 _load").getP0(), DOUBLE_TOLERANCE); // untouched
        assertEquals(1500., testNetwork.getLoad("FFR3AA1 _load").getP0(), DOUBLE_TOLERANCE); // untouched

        done = scalable.scale(testNetwork, -3000.0);
        assertEquals(-3000., done, DOUBLE_TOLERANCE);
        assertEquals(0., testNetwork.getGenerator("FFR1AA1 _generator").getTargetP(), DOUBLE_TOLERANCE);
        assertEquals(1003., testNetwork.getGenerator("FFR2AA1 _generator").getTargetP(), DOUBLE_TOLERANCE);
        assertEquals(3003., testNetwork.getGenerator("FFR3AA1 _generator").getTargetP(), DOUBLE_TOLERANCE);
        assertEquals(1000., testNetwork.getLoad("FFR1AA1 _load").getP0(), DOUBLE_TOLERANCE); // untouched
        assertEquals(3500., testNetwork.getLoad("FFR2AA1 _load").getP0(), DOUBLE_TOLERANCE); // untouched
        assertEquals(1500., testNetwork.getLoad("FFR3AA1 _load").getP0(), DOUBLE_TOLERANCE); // untouched
    }

    @Test
    void testConvertGlskPointToScalableB42Country() {
        GlskPoint glsk = CimGlskDocument.importGlsk(getResourceAsStream("/GlskB42CountryIIDM.xml")).getGlskPoints().get(0);
        Scalable scalable = GlskPointScalableConverter.convert(testNetwork, glsk);

        assertNotNull(scalable);
        assertEquals(2000., testNetwork.getGenerator("FFR1AA1 _generator").getTargetP(), DOUBLE_TOLERANCE);
        assertEquals(2000., testNetwork.getGenerator("FFR2AA1 _generator").getTargetP(), DOUBLE_TOLERANCE);
        assertEquals(3000., testNetwork.getGenerator("FFR3AA1 _generator").getTargetP(), DOUBLE_TOLERANCE);
        assertEquals(1000., testNetwork.getLoad("FFR1AA1 _load").getP0(), DOUBLE_TOLERANCE);
        assertEquals(3500., testNetwork.getLoad("FFR2AA1 _load").getP0(), DOUBLE_TOLERANCE);
        assertEquals(1500., testNetwork.getLoad("FFR3AA1 _load").getP0(), DOUBLE_TOLERANCE);

        double done = scalable.scale(testNetwork, 700.);
        assertEquals(700., done, DOUBLE_TOLERANCE);
        assertEquals(2200., testNetwork.getGenerator("FFR1AA1 _generator").getTargetP(), DOUBLE_TOLERANCE); // 2000/7000 * 700 = +200
        assertEquals(2200., testNetwork.getGenerator("FFR2AA1 _generator").getTargetP(), DOUBLE_TOLERANCE); // 2000/7000 * 700 = +200
        assertEquals(3300., testNetwork.getGenerator("FFR3AA1 _generator").getTargetP(), DOUBLE_TOLERANCE); // 3000/7000 * 700 = +300
        assertEquals(1000., testNetwork.getLoad("FFR1AA1 _load").getP0(), DOUBLE_TOLERANCE); // untouched
        assertEquals(3500., testNetwork.getLoad("FFR2AA1 _load").getP0(), DOUBLE_TOLERANCE); // untouched
        assertEquals(1500., testNetwork.getLoad("FFR3AA1 _load").getP0(), DOUBLE_TOLERANCE); // untouched
    }

    @Test
    void testConvertGlskPointToScalableB42CountryGskLsk() {
        GlskPoint glsk = CimGlskDocument.importGlsk(getResourceAsStream("/GlskB42CountryGskLsk.xml")).getGlskPoints().get(0);
        Scalable scalable = GlskPointScalableConverter.convert(testNetwork, glsk);

        assertNotNull(scalable);
        assertEquals(2000., testNetwork.getGenerator("FFR1AA1 _generator").getTargetP(), DOUBLE_TOLERANCE);
        assertEquals(2000., testNetwork.getGenerator("FFR2AA1 _generator").getTargetP(), DOUBLE_TOLERANCE);
        assertEquals(3000., testNetwork.getGenerator("FFR3AA1 _generator").getTargetP(), DOUBLE_TOLERANCE);
        assertEquals(1000., testNetwork.getLoad("FFR1AA1 _load").getP0(), DOUBLE_TOLERANCE);
        assertEquals(3500., testNetwork.getLoad("FFR2AA1 _load").getP0(), DOUBLE_TOLERANCE);
        assertEquals(1500., testNetwork.getLoad("FFR3AA1 _load").getP0(), DOUBLE_TOLERANCE);

        double done = scalable.scale(testNetwork, -1000.);
        assertEquals(-1000., done, DOUBLE_TOLERANCE);
        assertEquals(1829., testNetwork.getGenerator("FFR1AA1 _generator").getTargetP(), FUZZY_TOLERANCE); // 2000/7000 * 0.6 = 17.1 %
        assertEquals(1829., testNetwork.getGenerator("FFR2AA1 _generator").getTargetP(), FUZZY_TOLERANCE); // 2000/7000 * 0.6 = 17.1 %
        assertEquals(2743., testNetwork.getGenerator("FFR3AA1 _generator").getTargetP(), FUZZY_TOLERANCE); // 3000/7000 * 0.6 = 25.7 %
        assertEquals(1067., testNetwork.getLoad("FFR1AA1 _load").getP0(), FUZZY_TOLERANCE); // 1000/6000 * 0.4 = 6.7 %
        assertEquals(3733., testNetwork.getLoad("FFR2AA1 _load").getP0(), FUZZY_TOLERANCE); // 3500/6000 * 0.4 = 23.3 %
        assertEquals(1600., testNetwork.getLoad("FFR3AA1 _load").getP0(), FUZZY_TOLERANCE); // 1500/6000 * 0.4 = 10 %
    }

    @Test
    void testConvertGlskPointToScalableB42ExplicitGskLsk() {
        GlskPoint glsk = CimGlskDocument.importGlsk(getResourceAsStream("/GlskB42ExplicitGskLsk.xml")).getGlskPoints().get(0);
        Scalable scalable = GlskPointScalableConverter.convert(testNetwork, glsk);

        assertNotNull(scalable);
        assertEquals(2000., testNetwork.getGenerator("FFR1AA1 _generator").getTargetP(), DOUBLE_TOLERANCE);
        assertEquals(2000., testNetwork.getGenerator("FFR2AA1 _generator").getTargetP(), DOUBLE_TOLERANCE);
        assertEquals(3000., testNetwork.getGenerator("FFR3AA1 _generator").getTargetP(), DOUBLE_TOLERANCE);
        assertEquals(1000., testNetwork.getLoad("FFR1AA1 _load").getP0(), DOUBLE_TOLERANCE);
        assertEquals(3500., testNetwork.getLoad("FFR2AA1 _load").getP0(), DOUBLE_TOLERANCE);
        assertEquals(1500., testNetwork.getLoad("FFR3AA1 _load").getP0(), DOUBLE_TOLERANCE);

        double done = scalable.scale(testNetwork, 1000.);
        assertEquals(1000., done, DOUBLE_TOLERANCE);
        assertEquals(2300., testNetwork.getGenerator("FFR1AA1 _generator").getTargetP(), DOUBLE_TOLERANCE); // 2000/4000 * 0.6 = 30 %
        assertEquals(2300., testNetwork.getGenerator("FFR2AA1 _generator").getTargetP(), DOUBLE_TOLERANCE); // 2000/4000 * 0.6 = 30 %
        assertEquals(3000., testNetwork.getGenerator("FFR3AA1 _generator").getTargetP(), DOUBLE_TOLERANCE); // excluded
        assertEquals(1000., testNetwork.getLoad("FFR1AA1 _load").getP0(), DOUBLE_TOLERANCE); // excluded
        assertEquals(3220., testNetwork.getLoad("FFR2AA1 _load").getP0(), DOUBLE_TOLERANCE); // 3500/5000 * 0.4 = 28 %
        assertEquals(1380., testNetwork.getLoad("FFR3AA1 _load").getP0(), DOUBLE_TOLERANCE); // 1500/5000 * 0.4 = 12 %
    }

    @Test
    void testConvertGlskPointToScalableB43GskLsk() {
        GlskPoint glsk = CimGlskDocument.importGlsk(getResourceAsStream("/GlskB43ParticipationFactorGskLsk.xml")).getGlskPoints().get(0);
        Scalable scalable = GlskPointScalableConverter.convert(testNetwork, glsk);

        assertNotNull(scalable);
        assertEquals(2000., testNetwork.getGenerator("FFR1AA1 _generator").getTargetP(), DOUBLE_TOLERANCE);
        assertEquals(2000., testNetwork.getGenerator("FFR2AA1 _generator").getTargetP(), DOUBLE_TOLERANCE);
        assertEquals(3000., testNetwork.getGenerator("FFR3AA1 _generator").getTargetP(), DOUBLE_TOLERANCE);
        assertEquals(1000., testNetwork.getLoad("FFR1AA1 _load").getP0(), DOUBLE_TOLERANCE);
        assertEquals(3500., testNetwork.getLoad("FFR2AA1 _load").getP0(), DOUBLE_TOLERANCE);
        assertEquals(1500., testNetwork.getLoad("FFR3AA1 _load").getP0(), DOUBLE_TOLERANCE);

        double done = scalable.scale(testNetwork, 500.);
        assertEquals(500., done, DOUBLE_TOLERANCE);
        assertEquals(2210., testNetwork.getGenerator("FFR1AA1 _generator").getTargetP(), DOUBLE_TOLERANCE); // 0.7 * 0.6 = 42 %
        assertEquals(2090., testNetwork.getGenerator("FFR2AA1 _generator").getTargetP(), DOUBLE_TOLERANCE); // 0.3 * 0.6 = 18 %
        assertEquals(3000., testNetwork.getGenerator("FFR3AA1 _generator").getTargetP(), DOUBLE_TOLERANCE); // untouched
        assertEquals(960., testNetwork.getLoad("FFR1AA1 _load").getP0(), DOUBLE_TOLERANCE); // 0.2 * 0.4 = 8 %
        assertEquals(3340., testNetwork.getLoad("FFR2AA1 _load").getP0(), DOUBLE_TOLERANCE); // 0.8 * 0.4 = 32 %
        assertEquals(1500., testNetwork.getLoad("FFR3AA1 _load").getP0(), DOUBLE_TOLERANCE); // untouched
    }

    @Test
    void testConvertGlskPointToScalableB42Explicit() {
        GlskPoint glsk = CimGlskDocument.importGlsk(getResourceAsStream("/GlskB42ExplicitIIDM.xml")).getGlskPoints().get(0);
        Scalable scalable = GlskPointScalableConverter.convert(testNetwork, glsk);

        assertNotNull(scalable);
        assertEquals(2000., testNetwork.getGenerator("FFR1AA1 _generator").getTargetP(), DOUBLE_TOLERANCE);
        assertEquals(2000., testNetwork.getGenerator("FFR2AA1 _generator").getTargetP(), DOUBLE_TOLERANCE);
        assertEquals(3000., testNetwork.getGenerator("FFR3AA1 _generator").getTargetP(), DOUBLE_TOLERANCE);
        assertEquals(1000., testNetwork.getLoad("FFR1AA1 _load").getP0(), DOUBLE_TOLERANCE);
        assertEquals(3500., testNetwork.getLoad("FFR2AA1 _load").getP0(), DOUBLE_TOLERANCE);
        assertEquals(1500., testNetwork.getLoad("FFR3AA1 _load").getP0(), DOUBLE_TOLERANCE);

        double done = scalable.scale(testNetwork, -500.);
        assertEquals(-500., done, DOUBLE_TOLERANCE);
        assertEquals(1800., testNetwork.getGenerator("FFR1AA1 _generator").getTargetP(), DOUBLE_TOLERANCE); // 2000/5000 * 500 = -200
        assertEquals(2000., testNetwork.getGenerator("FFR2AA1 _generator").getTargetP(), DOUBLE_TOLERANCE); // untouched
        assertEquals(2700., testNetwork.getGenerator("FFR3AA1 _generator").getTargetP(), DOUBLE_TOLERANCE); // 3000/5000 * 500 = -300
        assertEquals(1000., testNetwork.getLoad("FFR1AA1 _load").getP0(), DOUBLE_TOLERANCE); // untouched
        assertEquals(3500., testNetwork.getLoad("FFR2AA1 _load").getP0(), DOUBLE_TOLERANCE); // untouched
        assertEquals(1500., testNetwork.getLoad("FFR3AA1 _load").getP0(), DOUBLE_TOLERANCE); // untouched
    }

    @Test
    void testConvertGlskPointToScalableB43() {
        GlskPoint glsk = CimGlskDocument.importGlsk(getResourceAsStream("/GlskB43ParticipationFactorIIDM.xml")).getGlskPoints().get(0);
        Scalable scalable = GlskPointScalableConverter.convert(testNetwork, glsk);

        assertNotNull(scalable);
        assertEquals(2000., testNetwork.getGenerator("FFR1AA1 _generator").getTargetP(), DOUBLE_TOLERANCE);
        assertEquals(2000., testNetwork.getGenerator("FFR2AA1 _generator").getTargetP(), DOUBLE_TOLERANCE);
        assertEquals(3000., testNetwork.getGenerator("FFR3AA1 _generator").getTargetP(), DOUBLE_TOLERANCE);
        assertEquals(1000., testNetwork.getLoad("FFR1AA1 _load").getP0(), DOUBLE_TOLERANCE);
        assertEquals(3500., testNetwork.getLoad("FFR2AA1 _load").getP0(), DOUBLE_TOLERANCE);
        assertEquals(1500., testNetwork.getLoad("FFR3AA1 _load").getP0(), DOUBLE_TOLERANCE);

        double done = scalable.scale(testNetwork, 1000.);
        assertEquals(1000., done, DOUBLE_TOLERANCE);
        assertEquals(2382., testNetwork.getGenerator("FFR1AA1 _generator").getTargetP(), FUZZY_TOLERANCE); // 0.618/(0.618+0.618+0.382) = 38.2 %
        assertEquals(2382., testNetwork.getGenerator("FFR2AA1 _generator").getTargetP(), FUZZY_TOLERANCE); // 0.618/(0.618+0.618+0.382) = 38.2 %
        assertEquals(3236., testNetwork.getGenerator("FFR3AA1 _generator").getTargetP(), FUZZY_TOLERANCE); // 0.382/(0.618+0.618+0.382) = 23.6 %
        assertEquals(1000., testNetwork.getLoad("FFR1AA1 _load").getP0(), DOUBLE_TOLERANCE); // untouched
        assertEquals(3500., testNetwork.getLoad("FFR2AA1 _load").getP0(), DOUBLE_TOLERANCE); // untouched
        assertEquals(1500., testNetwork.getLoad("FFR3AA1 _load").getP0(), DOUBLE_TOLERANCE); // untouched
    }

    @Test
    void testConvertGlskPointToScalableB42ExplicitGskLskWithDanglingLines() {
        testNetwork = Network.read("testCaseWithDanglingLines.xiidm", getClass().getResourceAsStream("/testCaseWithDanglingLines.xiidm"));
        GlskPoint glsk = CimGlskDocument.importGlsk(getResourceAsStream("/GlskB42ExplicitWithDanglingLines.xml")).getGlskPoints().get(0);
        Scalable scalable = GlskPointScalableConverter.convert(testNetwork, glsk);

        assertNotNull(scalable);
        assertEquals(2000., testNetwork.getGenerator("FFR1AA1 _generator").getTargetP(), DOUBLE_TOLERANCE);
        assertEquals(2000., testNetwork.getGenerator("FFR2AA1 _generator").getTargetP(), DOUBLE_TOLERANCE);
        assertEquals(3000., testNetwork.getGenerator("FFR3AA1 _generator").getTargetP(), DOUBLE_TOLERANCE);
        assertEquals(-1000., testNetwork.getDanglingLine("DDE3AA1  XNODE_1A 1").getP0(), DOUBLE_TOLERANCE);
        assertEquals(1000., testNetwork.getLoad("FFR1AA1 _load").getP0(), DOUBLE_TOLERANCE);
        assertEquals(3500., testNetwork.getLoad("FFR2AA1 _load").getP0(), DOUBLE_TOLERANCE);
        assertEquals(1500., testNetwork.getLoad("FFR3AA1 _load").getP0(), DOUBLE_TOLERANCE);
        assertEquals(1000., testNetwork.getDanglingLine("BBE2AA1  XNODE_1B 1").getP0(), DOUBLE_TOLERANCE);

        double done = scalable.scale(testNetwork, -1000.);
        assertEquals(-1000., done, DOUBLE_TOLERANCE);
        assertEquals(1850., testNetwork.getGenerator("FFR1AA1 _generator").getTargetP(), FUZZY_TOLERANCE); // 2/8 * 0.6 = 15 %
        assertEquals(1850., testNetwork.getGenerator("FFR2AA1 _generator").getTargetP(), FUZZY_TOLERANCE); // 2/8 * 0.6 = 15 %
        assertEquals(2775., testNetwork.getGenerator("FFR3AA1 _generator").getTargetP(), FUZZY_TOLERANCE); // 3/8 * 0.6 = 22.5 %
        assertEquals(-925., testNetwork.getDanglingLine("DDE3AA1  XNODE_1A 1").getP0(), FUZZY_TOLERANCE); // 1/8 * 0.6 = 7.5 %
        assertEquals(1057., testNetwork.getLoad("FFR1AA1 _load").getP0(), FUZZY_TOLERANCE); // 1/7 * 0.4 = 5.7 %
        assertEquals(3700., testNetwork.getLoad("FFR2AA1 _load").getP0(), FUZZY_TOLERANCE); // 3.5/7 * 0.4 = 20 %
        assertEquals(1586., testNetwork.getLoad("FFR3AA1 _load").getP0(), FUZZY_TOLERANCE); // 1.5/7 * 0.4 = 8.6 %
        assertEquals(1057., testNetwork.getDanglingLine("BBE2AA1  XNODE_1B 1").getP0(), FUZZY_TOLERANCE); // 1/7 * 0.4 = 5.7 %
    }

    @Test
    void testConvertGlskPointToScalableB43GskLskWithDanglingLines() {
        testNetwork = Network.read("testCaseWithDanglingLines.xiidm", getClass().getResourceAsStream("/testCaseWithDanglingLines.xiidm"));
        GlskPoint glsk = CimGlskDocument.importGlsk(getResourceAsStream("/GlskB43ParticipationFactorGskLskWithDanglingLines.xml")).getGlskPoints().get(0);
        Scalable scalable = GlskPointScalableConverter.convert(testNetwork, glsk);

        assertNotNull(scalable);
        assertEquals(2000., testNetwork.getGenerator("FFR1AA1 _generator").getTargetP(), DOUBLE_TOLERANCE);
        assertEquals(-1000., testNetwork.getDanglingLine("DDE3AA1  XNODE_1A 1").getP0(), DOUBLE_TOLERANCE);
        assertEquals(1000., testNetwork.getLoad("FFR1AA1 _load").getP0(), DOUBLE_TOLERANCE);
        assertEquals(1000., testNetwork.getDanglingLine("BBE2AA1  XNODE_1B 1").getP0(), DOUBLE_TOLERANCE);

        double done = scalable.scale(testNetwork, 1000.);
        assertEquals(1000., done, DOUBLE_TOLERANCE);
        assertEquals(2420., testNetwork.getGenerator("FFR1AA1 _generator").getTargetP(), DOUBLE_TOLERANCE); // 0.6 * 0.7 = 42 %
        assertEquals(-1180., testNetwork.getDanglingLine("DDE3AA1  XNODE_1A 1").getP0(), DOUBLE_TOLERANCE); // 0.6 * 0.3 = 18 %
        assertEquals(840., testNetwork.getLoad("FFR1AA1 _load").getP0(), DOUBLE_TOLERANCE); // 0.4 * 0.4 = 16 %
        assertEquals(760., testNetwork.getDanglingLine("BBE2AA1  XNODE_1B 1").getP0(), DOUBLE_TOLERANCE); // 0.4 * 0.6 = 24 %
    }

    @Test
    void testNullPercentageSum() {
        GlskPoint glsk = CimGlskDocument.importGlsk(getResourceAsStream("/GlskB43ParticipationFactorGskLskZeroSum.xml")).getGlskPoints().get(0);
        Scalable scalable = GlskPointScalableConverter.convert(testNetwork, glsk);

        assertNotNull(scalable);
        assertEquals(2000., testNetwork.getGenerator("FFR1AA1 _generator").getTargetP(), DOUBLE_TOLERANCE);
        assertEquals(2000., testNetwork.getGenerator("FFR2AA1 _generator").getTargetP(), DOUBLE_TOLERANCE);
        assertEquals(3000., testNetwork.getGenerator("FFR3AA1 _generator").getTargetP(), DOUBLE_TOLERANCE);
        assertEquals(1000., testNetwork.getLoad("FFR1AA1 _load").getP0(), DOUBLE_TOLERANCE);
        assertEquals(3500., testNetwork.getLoad("FFR2AA1 _load").getP0(), DOUBLE_TOLERANCE);
        assertEquals(1500., testNetwork.getLoad("FFR3AA1 _load").getP0(), DOUBLE_TOLERANCE);

        double done = scalable.scale(testNetwork, 500.);
        assertEquals(500., done, DOUBLE_TOLERANCE);
        assertEquals(2350., testNetwork.getGenerator("FFR1AA1 _generator").getTargetP(), DOUBLE_TOLERANCE); // 0.7 * 1.0 = 70 %
        assertEquals(2150., testNetwork.getGenerator("FFR2AA1 _generator").getTargetP(), DOUBLE_TOLERANCE); // 0.3 * 1.0 = 30 %
        assertEquals(3000., testNetwork.getGenerator("FFR3AA1 _generator").getTargetP(), DOUBLE_TOLERANCE); // untouched
        assertEquals(1000., testNetwork.getLoad("FFR1AA1 _load").getP0(), DOUBLE_TOLERANCE); // 0.0 * 0.0 = 0 %
        assertEquals(3500., testNetwork.getLoad("FFR2AA1 _load").getP0(), DOUBLE_TOLERANCE); // 0.0 * 0.0 = 0 %
        assertEquals(1500., testNetwork.getLoad("FFR3AA1 _load").getP0(), DOUBLE_TOLERANCE); // untouched
    }
}
