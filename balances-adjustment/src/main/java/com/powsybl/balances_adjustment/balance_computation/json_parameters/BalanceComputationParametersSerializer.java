/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.balances_adjustment.balance_computation.json_parameters;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.powsybl.balances_adjustment.balance_computation.BalanceComputationParameters;
import com.powsybl.commons.json.JsonUtil;
import com.powsybl.loadflow.json.JsonLoadFlowParameters;

import java.io.IOException;

/**
 * @author Mohamed Ben Rejeb {@literal <mohamed.benrejeb at rte-france.com>}
 */
public class BalanceComputationParametersSerializer extends StdSerializer<BalanceComputationParameters> {
    BalanceComputationParametersSerializer() {
        super(BalanceComputationParameters.class);
    }

    @Override
    public void serialize(BalanceComputationParameters parameters, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {

        jsonGenerator.writeStartObject();

        jsonGenerator.writeNumberField("maxNumberIterations", parameters.getMaxNumberIterations());
        jsonGenerator.writeNumberField("thresholdNetPosition", parameters.getThresholdNetPosition());
        jsonGenerator.writeFieldName("load-flow-parameters");
        JsonLoadFlowParameters.serialize(parameters.getLoadFlowParameters(), jsonGenerator, serializerProvider);

        JsonUtil.writeExtensions(parameters, jsonGenerator, serializerProvider, JsonBalanceComputationParameters.getExtensionSerializers());

        jsonGenerator.writeEndObject();
    }
}
