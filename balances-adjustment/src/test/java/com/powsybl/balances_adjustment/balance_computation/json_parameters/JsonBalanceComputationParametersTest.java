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
import com.powsybl.commons.AbstractConverterTest;
import com.powsybl.commons.ComparisonUtils;
import com.powsybl.commons.extensions.AbstractExtension;
import com.powsybl.loadflow.LoadFlowParameters;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.*;

/**
 * @author Mohamed Ben Rejeb {@literal <mohamed.benrejeb at rte-france.com>}
 */
public class JsonBalanceComputationParametersTest extends AbstractConverterTest {

    @Test
    public void testDefaultBalanceComputationConfig() {
        BalanceComputationParameters parameters = new BalanceComputationParameters();
        BalanceComputationParameters.load();
        assertEquals(BalanceComputationParameters.DEFAULT_MAX_NUMBER_ITERATIONS, parameters.getMaxNumberIterations());
        assertEquals(BalanceComputationParameters.DEFAULT_THRESHOLD_NET_POSITION, parameters.getThresholdNetPosition(), .01);
    }

    @Test
    public void readError() {
        InputStream is = getClass().getResourceAsStream("/balanceComputationParametersError.json");
        assertThrows(AssertionError.class, () -> JsonBalanceComputationParameters.read(is));
    }

    @Test
    public void readSuccessful() {
        BalanceComputationParameters parameters = JsonBalanceComputationParameters.read(getClass().getResourceAsStream("/balanceComputationParameters.json"));
        assertEquals(11, parameters.getMaxNumberIterations());
        assertEquals(2, parameters.getThresholdNetPosition(), .01);
        LoadFlowParameters actualLoadflowParams =  parameters.getLoadFlowParameters();
        assertEquals("DC_VALUES", actualLoadflowParams.getVoltageInitMode().toString());
        assertTrue(actualLoadflowParams.isTransformerVoltageControlOn());
        assertTrue(actualLoadflowParams.isPhaseShifterRegulationOn());
        assertFalse(actualLoadflowParams.isNoGeneratorReactiveLimits());
    }

    @Test
    public void readExtension() throws IOException {
        BalanceComputationParameters parameters = JsonBalanceComputationParameters.read(getClass().getResourceAsStream("/balanceComputationParametersWithExtension.json"));
        assertEquals(1, parameters.getExtensions().size());
        assertNotNull(parameters.getExtension(DummyExtension.class));
        assertNotNull(parameters.getExtensionByName("dummy-extension"));
    }

    @Test
    public void roundTrip() throws IOException {
        BalanceComputationParameters parameters = JsonBalanceComputationParameters.read(getClass().getResourceAsStream("/balanceComputationParameters.json"));
        roundTripTest(parameters, JsonBalanceComputationParameters::write, JsonBalanceComputationParameters::read, "/balanceComputationParameters.json");
    }

    @Test
    public void writeExtension() throws IOException {
        BalanceComputationParameters parameters = new BalanceComputationParameters();
        parameters.addExtension(DummyExtension.class, new DummyExtension());
        writeTest(parameters, JsonBalanceComputationParameters::write, ComparisonUtils::compareTxt, "/balanceComputationParametersWithExtension.json");
    }

    @Test
    public void updateLoadFlowParameters() {
        BalanceComputationParameters parameters = new BalanceComputationParameters();
        JsonBalanceComputationParameters.update(parameters, getClass().getResourceAsStream("/balanceComputationParameters.json"));

        assertEquals("DC_VALUES", parameters.getLoadFlowParameters().getVoltageInitMode().toString());
        assertTrue(parameters.getLoadFlowParameters().isTransformerVoltageControlOn());
        assertTrue(parameters.getLoadFlowParameters().isPhaseShifterRegulationOn());
        assertFalse(parameters.getLoadFlowParameters().isNoGeneratorReactiveLimits());
    }

    static class DummyExtension extends AbstractExtension<BalanceComputationParameters> {

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
        public DummyExtension deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
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
