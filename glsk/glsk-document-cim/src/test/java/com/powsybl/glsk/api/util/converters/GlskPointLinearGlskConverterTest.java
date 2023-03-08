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
import com.google.common.math.DoubleMath;
import com.powsybl.iidm.network.Network;
import com.powsybl.sensitivity.SensitivityVariableSet;
import com.powsybl.sensitivity.WeightedSensitivityVariable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Pengbo Wang {@literal <pengbo.wang@rte-international.com>}
 * @author Sebastien Murgey {@literal <sebastien.murgey@rte-france.com>}
 */
class GlskPointLinearGlskConverterTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(GlskPointLinearGlskConverterTest.class);
    private static final String GLSKB42COUNTRYIIDM = "/GlskB42CountryIIDM.xml";
    private static final String GLSKB42COUNTRYQUANTITY = "/GlskB42CountryQuantity.xml";
    private static final String GLSKB42EXPLICITGSKLSK = "/GlskB42ExplicitGskLsk.xml";
    private static final String GLSKB43GSKLSK = "/GlskB43ParticipationFactorGskLsk.xml";
    private static final String GLSKB43GSKZERO = "/GlskB43ParticipationFactorGskZero.xml";
    private static final String GLSKB43LSKZERO = "/GlskB43ParticipationFactorLskZero.xml";

    private Network testNetwork;
    private GlskPoint glskPointCountry;
    private GlskPoint glskPointCountryQuantity;
    private GlskPoint glskPointExplicitGskLsk;
    private GlskPoint glskPointParticipationFactorGskLsk;
    private GlskPoint glskPointParticipationFactorGskZero;
    private GlskPoint glskPointParticipationFactorLskZero;

    private InputStream getResourceAsStream(String resource) {
        return getClass().getResourceAsStream(resource);
    }

    @BeforeEach
    void setUp() {
        testNetwork = Network.read("testCase.xiidm", getClass().getResourceAsStream("/testCase.xiidm"));

        glskPointCountry = CimGlskDocument.importGlsk(getResourceAsStream(GLSKB42COUNTRYIIDM)).getGlskPoints().get(0);
        glskPointCountryQuantity = CimGlskDocument.importGlsk(getResourceAsStream(GLSKB42COUNTRYQUANTITY)).getGlskPoints().get(0);
        glskPointExplicitGskLsk = CimGlskDocument.importGlsk(getResourceAsStream(GLSKB42EXPLICITGSKLSK)).getGlskPoints().get(0);
        glskPointParticipationFactorGskLsk = CimGlskDocument.importGlsk(getResourceAsStream(GLSKB43GSKLSK)).getGlskPoints().get(0);
        glskPointParticipationFactorGskZero = CimGlskDocument.importGlsk(getResourceAsStream(GLSKB43GSKZERO)).getGlskPoints().get(0);
        glskPointParticipationFactorLskZero = CimGlskDocument.importGlsk(getResourceAsStream(GLSKB43LSKZERO)).getGlskPoints().get(0);
    }

    /**
     *  tests for LinearGlsk
     */
    @Test
    void testConvertGlskPointToLinearGlskB42Country() {

        SensitivityVariableSet linearGlsk = GlskPointLinearGlskConverter.convert(testNetwork, glskPointCountry);
        linearGlsk.getVariables().forEach(variable -> LOGGER.info("GenCountry: " + variable.getId() + "; factor = " + variable.getWeight())); //log
        double totalfactor = linearGlsk.getVariables().stream().mapToDouble(WeightedSensitivityVariable::getWeight).sum();
        Assertions.assertTrue(DoubleMath.fuzzyEquals(1.0, totalfactor, 0.0001));
    }

    @Test
    void testConvertGlskPointToLinearGlskB42CountryQuantity() {

        SensitivityVariableSet linearGlsk = GlskPointLinearGlskConverter.convert(testNetwork, glskPointCountryQuantity);
        linearGlsk.getVariables().forEach(variable -> LOGGER.info("Country: " + variable.getId() + "; factor = " + variable.getWeight())); //log
        double totalfactor = linearGlsk.getVariables().stream().mapToDouble(WeightedSensitivityVariable::getWeight).sum();
        Assertions.assertTrue(DoubleMath.fuzzyEquals(1.0, totalfactor, 0.0001));
    }

    @Test
    void testConvertGlskPointToLinearGlskB42Explicit() {
        SensitivityVariableSet linearGlsk = GlskPointLinearGlskConverter.convert(testNetwork, glskPointExplicitGskLsk);
        linearGlsk.getVariables().forEach(variable -> LOGGER.info("Explicit: " + variable.getId() + "; factor = " + variable.getWeight())); //log
        double totalfactor = linearGlsk.getVariables().stream().mapToDouble(WeightedSensitivityVariable::getWeight).sum();
        Assertions.assertTrue(DoubleMath.fuzzyEquals(1.0, totalfactor, 0.0001));
    }

    @Test
    void testConvertGlskPointToLinearGlskB43() {
        SensitivityVariableSet linearGlsk = GlskPointLinearGlskConverter.convert(testNetwork, glskPointParticipationFactorGskLsk);
        linearGlsk.getVariables().forEach(variable -> LOGGER.info("Factor: " + variable.getId() + "; factor = " + variable.getWeight())); //log
        double totalfactor = linearGlsk.getVariables().stream().mapToDouble(WeightedSensitivityVariable::getWeight).sum();
        Assertions.assertTrue(DoubleMath.fuzzyEquals(1.0, totalfactor, 0.0001));
    }

    @Test
    void testConvertGlskPointToLinearGlskB43Zero() {
        assertThrows(GlskException.class, () -> GlskPointLinearGlskConverter.convert(testNetwork, glskPointParticipationFactorGskZero));
        assertThrows(GlskException.class, () -> GlskPointLinearGlskConverter.convert(testNetwork, glskPointParticipationFactorLskZero));
    }

}
