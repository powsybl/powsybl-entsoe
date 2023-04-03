/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.glsk.cse;

import com.powsybl.glsk.api.GlskDocument;
import com.powsybl.glsk.api.io.GlskDocumentImporters;
import com.powsybl.iidm.network.Network;
import com.powsybl.sensitivity.SensitivityVariableSet;
import org.apache.commons.lang3.NotImplementedException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
class ZonalGlsksTest {
    private static final double EPSILON = 1e-3;

    @Test
    void checkZonalGlskFromManualGskBlocks() {
        Network network = Network.read("testCase.xiidm", getClass().getResourceAsStream("/testCase.xiidm"));
        CseGlskDocument cseGlskDocument = CseGlskDocument.importGlsk(getClass().getResourceAsStream("/testGlskOnlyLinear.xml"), false);
        SensitivityVariableSet manualGlskSensi = cseGlskDocument.getZonalGlsks(network).getData("FR_MANUAL");

        assertNotNull(manualGlskSensi);
        assertEquals(0.7, manualGlskSensi.getVariable("FFR1AA1 _generator").getWeight(), EPSILON);
        assertEquals(0.3, manualGlskSensi.getVariable("FFR3AA1 _generator").getWeight(), EPSILON);
    }

    @Test
    void checkZonalGlskFromProportionalGskBlocks() {
        Network network = Network.read("testCase.xiidm", getClass().getResourceAsStream("/testCase.xiidm"));
        GlskDocument cseGlskDocument = GlskDocumentImporters.importGlsk(getClass().getResourceAsStream("/testGlskOnlyLinear.xml"));
        SensitivityVariableSet glskSensi = cseGlskDocument.getZonalGlsks(network).getData("FR_PROPGSK");

        assertNotNull(glskSensi);
        assertEquals(0.286, glskSensi.getVariable("FFR1AA1 _generator").getWeight(), EPSILON);
        assertEquals(0.286, glskSensi.getVariable("FFR2AA1 _generator").getWeight(), EPSILON);
        assertEquals(0.428, glskSensi.getVariable("FFR3AA1 _generator").getWeight(), EPSILON);
    }

    @Test
    void checkZonalGlskFromProportionalGlskBlocks() {
        Network network = Network.read("testCase.xiidm", getClass().getResourceAsStream("/testCase.xiidm"));
        CseGlskDocument cseGlskDocument = CseGlskDocument.importGlsk(getClass().getResourceAsStream("/testGlskOnlyLinear.xml"), false);
        SensitivityVariableSet glskSensi = cseGlskDocument.getZonalGlsks(network).getData("FR_PROPGLSK");

        assertNotNull(glskSensi);
        assertEquals(0.2, glskSensi.getVariable("FFR1AA1 _generator").getWeight(), EPSILON);
        assertEquals(0.2, glskSensi.getVariable("FFR2AA1 _generator").getWeight(), EPSILON);
        assertEquals(0.3, glskSensi.getVariable("FFR3AA1 _generator").getWeight(), EPSILON);
        assertEquals(0.067, glskSensi.getVariable("FFR1AA1 _load").getWeight(), EPSILON);
        assertEquals(0.233, glskSensi.getVariable("FFR2AA1 _load").getWeight(), EPSILON);
    }

    @Test
    void checkZonalGlskFailsWithNonLinearGlskBlocks() {
        Network network = Network.read("testCase.xiidm", getClass().getResourceAsStream("/testCase.xiidm"));
        CseGlskDocument cseGlskDocument = CseGlskDocument.importGlsk(getClass().getResourceAsStream("/testGlsk.xml"), false);
        assertThrows(NotImplementedException.class, () -> cseGlskDocument.getZonalGlsks(network));
    }
}
