/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.cne.converter;

import com.powsybl.cne.model.*;
import com.powsybl.contingency.Contingency;
import com.powsybl.iidm.network.Branch;
import com.powsybl.security.LimitViolation;
import com.powsybl.security.LimitViolationType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Check cne-model module
 *
 * @author Thomas Adam {@literal <tadam at silicom.fr>}
 */
class CneModelTest {

    @Test
    void baseTestMonitoredRegisteredResource() {
        LimitViolation current = new LimitViolation("current", LimitViolationType.CURRENT, null, Integer.MAX_VALUE, 100, 0.95f, 110.0, Branch.Side.ONE);
        LimitViolation lowShortCircuitCurrent = new LimitViolation("lowShortCircuitCurrent", LimitViolationType.LOW_SHORT_CIRCUIT_CURRENT, null, Integer.MAX_VALUE, 100, 0.95f, 110.0, Branch.Side.ONE);
        LimitViolation highShortCircuitCurrent = new LimitViolation("highShortCircuitCurrent", LimitViolationType.LOW_SHORT_CIRCUIT_CURRENT, null, Integer.MAX_VALUE, 100, 0.95f, 110.0, Branch.Side.ONE);
        LimitViolation lowVoltage = new LimitViolation("lowVoltage", LimitViolationType.LOW_VOLTAGE, null, Integer.MAX_VALUE, 100, 0.95f, 110.0, Branch.Side.ONE);
        LimitViolation highVoltage = new LimitViolation("highVoltage", LimitViolationType.HIGH_VOLTAGE, null, Integer.MAX_VALUE, 100, 0.95f, 110.0, Branch.Side.ONE);
        List<LimitViolation> violations = Arrays.asList(current, lowShortCircuitCurrent, highShortCircuitCurrent, lowVoltage, highVoltage);

        MonitoredRegisteredResource currentResource = new MonitoredRegisteredResource(violations);

        assertEquals("current", currentResource.getEquipmentId());
        assertEquals("current", currentResource.getEquipmentName());
        assertEquals(MeasurementType.A01, currentResource.getMeasurementList().get(0).getMeasurementType());
        assertEquals(110.0, currentResource.getMeasurementList().get(0).getAnalogValue(), 0.0d);
        assertEquals(UnitSymbol.AMP, currentResource.getMeasurementList().get(0).getUnitSymbol());
        assertEquals(MeasurementType.A01, currentResource.getMeasurementList().get(1).getMeasurementType());
        assertEquals(UnitSymbol.AMP, currentResource.getMeasurementList().get(1).getUnitSymbol());
        assertEquals(MeasurementType.A01, currentResource.getMeasurementList().get(2).getMeasurementType());
        assertEquals(UnitSymbol.AMP, currentResource.getMeasurementList().get(2).getUnitSymbol());
        assertEquals(MeasurementType.A10, currentResource.getMeasurementList().get(3).getMeasurementType());
        assertEquals(UnitSymbol.KVT, currentResource.getMeasurementList().get(3).getUnitSymbol());
        assertEquals(MeasurementType.A11, currentResource.getMeasurementList().get(4).getMeasurementType());
        assertEquals(UnitSymbol.KVT, currentResource.getMeasurementList().get(4).getUnitSymbol());
    }

    @Test
    void baseTestContingencySeries() {
        Contingency contingency = Contingency.builder("contingency")
                .addBranch("NHV1_NHV2_2", "VLNHV1")
                .addBranch("NHV1_NHV2_1")
                .addGenerator("GEN")
                .addBusbarSection("BBS1")
                .build();

        ContingencySeries contingencies = new ContingencySeries(contingency);
        assertEquals("contingency", contingencies.getContingencyId());
        assertEquals("contingency", contingencies.getContingencyName());
        assertEquals("NHV1_NHV2_2", contingencies.getRegisteredResourceList().get(0).getId());
        assertEquals("NHV1_NHV2_2", contingencies.getRegisteredResourceList().get(0).getName());
    }

    @Test
    void emptyLimitViolationNotAllowed() {
        // Fail to create MonitoredRegisteredResource
        List<LimitViolation> violations = Collections.emptyList();
        try {
            new MonitoredRegisteredResource(violations);
            Assertions.fail("Expected an IllegalArgumentException to be thrown");
        } catch (IllegalArgumentException ex) {
            Assertions.assertEquals("LimitViolation list cannot be empty", ex.getMessage());
        }
    }

    @Test
    void otherLimitViolationNotAllowed() {
        // Fail to create MonitoredRegisteredResource with LimitViolation.OTHER
        List<LimitViolation> violations = Collections.singletonList(new LimitViolation("otherId", LimitViolationType.OTHER, null, Integer.MAX_VALUE, 100, 0.95f, 110.0, Branch.Side.ONE));
        try {
            new MonitoredRegisteredResource(violations);
            Assertions.fail("Expected an UnsupportedOperationException to be thrown");
        } catch (UnsupportedOperationException ex) {
            Assertions.assertEquals("OTHER is not managed", ex.getMessage());
        }
    }
}
