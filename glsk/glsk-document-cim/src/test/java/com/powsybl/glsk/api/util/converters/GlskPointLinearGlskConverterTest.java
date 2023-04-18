/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.glsk.api.util.converters;

import com.powsybl.glsk.api.GlskPoint;
import com.powsybl.glsk.commons.GlskException;
import com.powsybl.glsk.cim.CimGlskDocument;
import com.powsybl.iidm.network.Network;
import com.powsybl.sensitivity.SensitivityVariableSet;
import com.powsybl.sensitivity.WeightedSensitivityVariable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Pengbo Wang {@literal <pengbo.wang@rte-international.com>}
 * @author Sebastien Murgey {@literal <sebastien.murgey@rte-france.com>}
 * @author Peter Mitri {@literal <peter.mitri@rte-france.com>}
 */
class GlskPointLinearGlskConverterTest {
    // TODO : detail existing tests
    private static final Logger LOGGER = LoggerFactory.getLogger(GlskPointLinearGlskConverterTest.class);
    private static final double DOUBLE_TOLERANCE = 1e-4;

    private Network testNetwork;

    private InputStream getResourceAsStream(String resource) {
        return getClass().getResourceAsStream(resource);
    }

    @BeforeEach
    void setUp() {
        testNetwork = Network.read("testCase.xiidm", getClass().getResourceAsStream("/testCase.xiidm"));
    }

    /**
     *  tests for LinearGlsk
     */
    @Test
    void testConvertGlskPointToLinearGlskB42Country() {
        GlskPoint glsk = CimGlskDocument.importGlsk(getResourceAsStream("/GlskB42CountryIIDM.xml")).getGlskPoints().get(0);
        SensitivityVariableSet linearGlsk = GlskPointLinearGlskConverter.convert(testNetwork, glsk);

        assertEquals(3, linearGlsk.getVariables().size());
        assertEquals(0.2857, linearGlsk.getVariable("FFR1AA1 _generator").getWeight(), DOUBLE_TOLERANCE); // 2000/7000 = 0.2857
        assertEquals(0.2857, linearGlsk.getVariable("FFR2AA1 _generator").getWeight(), DOUBLE_TOLERANCE); // 2000/7000 = 0.2857
        assertEquals(0.4286, linearGlsk.getVariable("FFR3AA1 _generator").getWeight(), DOUBLE_TOLERANCE); // 3000/7000 = 0.4286

        double totalFactor = linearGlsk.getVariables().stream().mapToDouble(WeightedSensitivityVariable::getWeight).sum();
        assertEquals(1.0, totalFactor, DOUBLE_TOLERANCE);
    }

    @Test
    void testConvertGlskPointToLinearGlskB42CountryQuantity() {
        GlskPoint glsk = CimGlskDocument.importGlsk(getResourceAsStream("/GlskB42CountryQuantity.xml")).getGlskPoints().get(0);
        SensitivityVariableSet linearGlsk = GlskPointLinearGlskConverter.convert(testNetwork, glsk);

        assertEquals(6, linearGlsk.getVariables().size());
        assertEquals(0.1714, linearGlsk.getVariable("FFR1AA1 _generator").getWeight(), DOUBLE_TOLERANCE); // 2000/7000 * 0.6 = 0.1714
        assertEquals(0.1714, linearGlsk.getVariable("FFR2AA1 _generator").getWeight(), DOUBLE_TOLERANCE); // 2000/7000 * 0.6 = 0.1714
        assertEquals(0.2571, linearGlsk.getVariable("FFR3AA1 _generator").getWeight(), DOUBLE_TOLERANCE); // 3000/7000 * 0.6 = 0.2571
        assertEquals(0.0667, linearGlsk.getVariable("FFR1AA1 _load").getWeight(), DOUBLE_TOLERANCE); // 1000/6000 * 0.4 = 0.0667
        assertEquals(0.2333, linearGlsk.getVariable("FFR2AA1 _load").getWeight(), DOUBLE_TOLERANCE); // 3500/6000 * 0.4 = 0.2333
        assertEquals(0.1, linearGlsk.getVariable("FFR3AA1 _load").getWeight(), DOUBLE_TOLERANCE); // 1500/6000 * 0.4 = 0.1

        double totalFactor = linearGlsk.getVariables().stream().mapToDouble(WeightedSensitivityVariable::getWeight).sum();
        assertEquals(1.0, totalFactor, DOUBLE_TOLERANCE);
    }

    @Test
    void testConvertGlskPointToLinearGlskB42Explicit() {
        GlskPoint glsk = CimGlskDocument.importGlsk(getResourceAsStream("/GlskB42ExplicitGskLsk.xml")).getGlskPoints().get(0);
        SensitivityVariableSet linearGlsk = GlskPointLinearGlskConverter.convert(testNetwork, glsk);

        assertEquals(4, linearGlsk.getVariables().size());
        assertEquals(0.3, linearGlsk.getVariable("FFR1AA1 _generator").getWeight(), DOUBLE_TOLERANCE); // 2000/4000 * 0.6 = 30 %
        assertEquals(0.3, linearGlsk.getVariable("FFR2AA1 _generator").getWeight(), DOUBLE_TOLERANCE); // 2000/4000 * 0.6 = 30 %
        assertEquals(0.28, linearGlsk.getVariable("FFR2AA1 _load").getWeight(), DOUBLE_TOLERANCE); // 3500/5000 * 0.4 = 28 %
        assertEquals(0.12, linearGlsk.getVariable("FFR3AA1 _load").getWeight(), DOUBLE_TOLERANCE); // 1500/5000 * 0.4 = 12 %

        double totalFactor = linearGlsk.getVariables().stream().mapToDouble(WeightedSensitivityVariable::getWeight).sum();
        assertEquals(1.0, totalFactor, DOUBLE_TOLERANCE);
    }

    @Test
    void testConvertGlskPointToLinearGlskB43() {
        GlskPoint glsk = CimGlskDocument.importGlsk(getResourceAsStream("/GlskB43ParticipationFactorGskLsk.xml")).getGlskPoints().get(0);
        SensitivityVariableSet linearGlsk = GlskPointLinearGlskConverter.convert(testNetwork, glsk);

        assertEquals(4, linearGlsk.getVariables().size());
        assertEquals(0.42, linearGlsk.getVariable("FFR1AA1 _generator").getWeight(), DOUBLE_TOLERANCE); // 0.7 * 0.6 = 42 %
        assertEquals(0.18, linearGlsk.getVariable("FFR2AA1 _generator").getWeight(), DOUBLE_TOLERANCE); // 0.3 * 0.6 = 18 %
        assertEquals(0.08, linearGlsk.getVariable("FFR1AA1 _load").getWeight(), DOUBLE_TOLERANCE); // 0.2 * 0.4 = 8 %
        assertEquals(0.32, linearGlsk.getVariable("FFR2AA1 _load").getWeight(), DOUBLE_TOLERANCE); // 0.8 * 0.4 = 32 %

        double totalFactor = linearGlsk.getVariables().stream().mapToDouble(WeightedSensitivityVariable::getWeight).sum();
        assertEquals(1.0, totalFactor, DOUBLE_TOLERANCE);
    }

    @Test
    void testConvertGlskPointToLinearGlskB43Zero() {
        GlskPoint gsk = CimGlskDocument.importGlsk(getResourceAsStream("/GlskB43ParticipationFactorGskZero.xml")).getGlskPoints().get(0);
        assertThrows(GlskException.class, () -> GlskPointLinearGlskConverter.convert(testNetwork, gsk));
        GlskPoint lsk = CimGlskDocument.importGlsk(getResourceAsStream("/GlskB43ParticipationFactorLskZero.xml")).getGlskPoints().get(0);
        assertThrows(GlskException.class, () -> GlskPointLinearGlskConverter.convert(testNetwork, lsk));
    }

    @Test
    void testB42ExplicitWithDanglingLines() {
        testNetwork = Network.read("testCaseWithDanglingLines.xiidm", getClass().getResourceAsStream("/testCaseWithDanglingLines.xiidm"));
        GlskPoint glsk = CimGlskDocument.importGlsk(getResourceAsStream("/GlskB42ExplicitWithDanglingLines.xml")).getGlskPoints().get(0);
        SensitivityVariableSet linearGlsk = GlskPointLinearGlskConverter.convert(testNetwork, glsk);

        assertEquals(8, linearGlsk.getVariables().size());
        assertEquals(0.15, linearGlsk.getVariable("FFR1AA1 _generator").getWeight(), DOUBLE_TOLERANCE); // 2/8 * 0.6 = 15 %
        assertEquals(0.15, linearGlsk.getVariable("FFR2AA1 _generator").getWeight(), DOUBLE_TOLERANCE); // 2/8 * 0.6 = 15 %
        assertEquals(0.225, linearGlsk.getVariable("FFR3AA1 _generator").getWeight(), DOUBLE_TOLERANCE); // 3/8 * 0.6 = 22.5 %
        assertEquals(0.075, linearGlsk.getVariable("DDE3AA1  XNODE_1A 1").getWeight(), DOUBLE_TOLERANCE); // 1/8 * 0.6 = 7.5 %
        assertEquals(0.0571, linearGlsk.getVariable("FFR1AA1 _load").getWeight(), DOUBLE_TOLERANCE); // 1/7 * 0.4 = 5.7 %
        assertEquals(0.20, linearGlsk.getVariable("FFR2AA1 _load").getWeight(), DOUBLE_TOLERANCE); // 3.5/7 * 0.4 = 20 %
        assertEquals(0.0857, linearGlsk.getVariable("FFR3AA1 _load").getWeight(), DOUBLE_TOLERANCE); // 1.5/7 * 0.4 = 8.6 %
        assertEquals(0.0571, linearGlsk.getVariable("BBE2AA1  XNODE_1B 1").getWeight(), DOUBLE_TOLERANCE); // 1/7 * 0.4 = 5.7 %

        double totalFactor = linearGlsk.getVariables().stream().mapToDouble(WeightedSensitivityVariable::getWeight).sum();
        assertEquals(1.0, totalFactor, DOUBLE_TOLERANCE);
    }

    @Test
    void testConvertGlskPointToLinearGlskB43WithDanglingLines() {
        testNetwork = Network.read("testCaseWithDanglingLines.xiidm", getClass().getResourceAsStream("/testCaseWithDanglingLines.xiidm"));
        GlskPoint glsk = CimGlskDocument.importGlsk(getResourceAsStream("/GlskB43ParticipationFactorGskLskWithDanglingLines.xml")).getGlskPoints().get(0);
        SensitivityVariableSet linearGlsk = GlskPointLinearGlskConverter.convert(testNetwork, glsk);

        assertEquals(4, linearGlsk.getVariables().size());
        assertEquals(0.42, linearGlsk.getVariable("FFR1AA1 _generator").getWeight(), DOUBLE_TOLERANCE); // 0.6 * 0.7 = 42 %
        assertEquals(0.18, linearGlsk.getVariable("DDE3AA1  XNODE_1A 1").getWeight(), DOUBLE_TOLERANCE); // 0.6 * 0.3 = 18 %
        assertEquals(0.16, linearGlsk.getVariable("FFR1AA1 _load").getWeight(), DOUBLE_TOLERANCE); // 0.4 * 0.4 = 16 %
        assertEquals(0.24, linearGlsk.getVariable("BBE2AA1  XNODE_1B 1").getWeight(), DOUBLE_TOLERANCE); // 0.4 * 0.6 = 24 %

        double totalFactor = linearGlsk.getVariables().stream().mapToDouble(WeightedSensitivityVariable::getWeight).sum();
        assertEquals(1.0, totalFactor, DOUBLE_TOLERANCE);
    }
}
