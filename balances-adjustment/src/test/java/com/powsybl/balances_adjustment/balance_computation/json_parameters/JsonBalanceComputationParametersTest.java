/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.balances_adjustment.balance_computation.json_parameters;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.google.auto.service.AutoService;
import com.powsybl.balances_adjustment.balance_computation.BalanceComputationParameters;
import com.powsybl.commons.test.AbstractSerDeTest;
import com.powsybl.commons.test.ComparisonUtils;
import com.powsybl.commons.extensions.AbstractExtension;
import com.powsybl.loadflow.LoadFlowParameters;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Mohamed Ben Rejeb {@literal <mohamed.benrejeb at rte-france.com>}
 */
class JsonBalanceComputationParametersTest extends AbstractSerDeTest {

    @Test
    void testDefaultBalanceComputationConfig() {
        BalanceComputationParameters parameters = new BalanceComputationParameters();
        BalanceComputationParameters.load();
        assertEquals(BalanceComputationParameters.DEFAULT_MAX_NUMBER_ITERATIONS, parameters.getMaxNumberIterations());
        assertEquals(BalanceComputationParameters.DEFAULT_THRESHOLD_NET_POSITION, parameters.getThresholdNetPosition(), .01);
        assertEquals(BalanceComputationParameters.DEFAULT_MISMATCH_MODE, parameters.getMismatchMode());
        assertEquals(BalanceComputationParameters.DEFAULT_WITH_LOAD_FLOW, parameters.isWithLoadFlow());
    }

    @Test
    void readError() {
        InputStream is = getClass().getResourceAsStream("/balanceComputationParametersError.json");
        assertThrows(AssertionError.class, () -> JsonBalanceComputationParameters.read(is));
    }

    @Test
    void readSuccessful() {
        BalanceComputationParameters parameters = JsonBalanceComputationParameters.read(getClass().getResourceAsStream("/balanceComputationParameters.json"));
        assertEquals(11, parameters.getMaxNumberIterations());
        assertEquals(2, parameters.getThresholdNetPosition(), .01);
        LoadFlowParameters actualLoadflowParams = parameters.getLoadFlowParameters();
        assertEquals("DC_VALUES", actualLoadflowParams.getVoltageInitMode().toString());
        assertTrue(actualLoadflowParams.isTransformerVoltageControlOn());
        assertTrue(actualLoadflowParams.isPhaseShifterRegulationOn());
        assertTrue(actualLoadflowParams.isUseReactiveLimits());
        assertTrue(parameters.isWithLoadFlow());
    }

    @Test
    void readExtension() {
        BalanceComputationParameters parameters = JsonBalanceComputationParameters.read(getClass().getResourceAsStream("/balanceComputationParametersWithExtension.json"));
        assertEquals(1, parameters.getExtensions().size());
        assertNotNull(parameters.getExtension(DummyExtension.class));
        assertNotNull(parameters.getExtensionByName("dummy-extension"));
    }

    @Test
    void roundTrip() throws IOException {
        BalanceComputationParameters parameters = JsonBalanceComputationParameters.read(getClass().getResourceAsStream("/balanceComputationParameters.json"));
        roundTripTest(parameters, JsonBalanceComputationParameters::write, JsonBalanceComputationParameters::read, "/balanceComputationParameters.json");
    }

    @Test
    void writeExtension() throws IOException {
        BalanceComputationParameters parameters = new BalanceComputationParameters();
        parameters.addExtension(DummyExtension.class, new DummyExtension());
        writeTest(parameters, JsonBalanceComputationParameters::write, ComparisonUtils::assertTxtEquals, "/balanceComputationParametersWithExtension.json");
    }

    @Test
    void updateLoadFlowParameters() {
        BalanceComputationParameters parameters = new BalanceComputationParameters();
        JsonBalanceComputationParameters.update(parameters, getClass().getResourceAsStream("/balanceComputationParameters.json"));

        assertEquals("DC_VALUES", parameters.getLoadFlowParameters().getVoltageInitMode().toString());
        assertTrue(parameters.getLoadFlowParameters().isTransformerVoltageControlOn());
        assertTrue(parameters.getLoadFlowParameters().isPhaseShifterRegulationOn());
        assertTrue(parameters.getLoadFlowParameters().isUseReactiveLimits());
    }

    public static class DummyExtension extends AbstractExtension<BalanceComputationParameters> {

        DummyExtension() {
            super();
        }

        @Override
        public String getName() {
            return "dummy-extension";
        }
    }

    @AutoService(JsonBalanceComputationParameters.ExtensionSerializer.class)
    public static class DummySerializer implements JsonBalanceComputationParameters.ExtensionSerializer<DummyExtension> {

        @Override
        public void serialize(DummyExtension extension, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
            jsonGenerator.writeStartObject();
            jsonGenerator.writeEndObject();
        }

        @Override
        public DummyExtension deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) {
            return new DummyExtension();
        }

        @Override
        public String getExtensionName() {
            return "dummy-extension";
        }

        @Override
        public String getCategoryName() {
            return "balance-computation-parameters";
        }

        @Override
        public Class<? super DummyExtension> getExtensionClass() {
            return DummyExtension.class;
        }
    }
}
