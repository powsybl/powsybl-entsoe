/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.balances_adjustment.balance_computation.json_parameters;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.powsybl.balances_adjustment.balance_computation.BalanceComputationParameters;
import com.powsybl.commons.extensions.Extension;
import com.powsybl.commons.json.JsonUtil;
import com.powsybl.loadflow.json.JsonLoadFlowParameters;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * @author Mohamed Ben Rejeb {@literal <mohamed.benrejeb at rte-france.com>}
 */
public class BalanceComputationParametersDeserializer extends StdDeserializer<BalanceComputationParameters> {

    BalanceComputationParametersDeserializer() {
        super(BalanceComputationParameters.class);
    }

    @Override
    public BalanceComputationParameters deserialize(JsonParser parser, DeserializationContext deserializationContext) throws IOException {
        return deserialize(parser, deserializationContext, new BalanceComputationParameters());
    }

    @Override
    public BalanceComputationParameters deserialize(JsonParser parser, DeserializationContext deserializationContext, BalanceComputationParameters parameters) throws IOException {

        List<Extension<BalanceComputationParameters>> extensions = Collections.emptyList();
        while (parser.nextToken() != JsonToken.END_OBJECT) {
            switch (parser.getCurrentName()) {
                case "maxNumberIterations":
                    parser.nextToken();
                    parameters.setMaxNumberIterations(parser.readValueAs(int.class));
                    break;

                case "thresholdNetPosition":
                    parser.nextToken();
                    parameters.setThresholdNetPosition(parser.readValueAs(double.class));
                    break;

                case "load-flow-parameters":
                    parser.nextToken();
                    JsonLoadFlowParameters.deserialize(parser, deserializationContext, parameters.getLoadFlowParameters());
                    break;

                case "extensions":
                    parser.nextToken();
                    extensions = JsonUtil.readExtensions(parser, deserializationContext, JsonBalanceComputationParameters.getExtensionSerializers());
                    break;

                default:
                    throw new AssertionError("Unexpected field: " + parser.getCurrentName());
            }
        }

        JsonBalanceComputationParameters.getExtensionSerializers().addExtensions(parameters, extensions);

        return parameters;
    }

}
