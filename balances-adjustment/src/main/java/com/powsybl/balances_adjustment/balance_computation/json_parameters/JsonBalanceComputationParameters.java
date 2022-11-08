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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.powsybl.balances_adjustment.balance_computation.BalanceComputationParameters;
import com.powsybl.commons.extensions.Extension;
import com.powsybl.commons.extensions.ExtensionJsonSerializer;
import com.powsybl.commons.extensions.ExtensionProviders;
import com.powsybl.commons.json.JsonUtil;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Provides methods to read and write BalanceComputationParameters from and to JSON.
 *
 * @author Mohamed Ben Rejeb {@literal <mohamed.benrejeb at rte-france.com>}
 */
public final class JsonBalanceComputationParameters {

    /**
     * A configuration loader interface for the LoadFlowParameters extensions loaded from the platform configuration
     *
     * @param <E> The extension class
     */
    public interface ExtensionSerializer<E extends Extension<BalanceComputationParameters>> extends ExtensionJsonSerializer<BalanceComputationParameters, E> {
    }

    /**
     * Lazily initialized list of extension serializers.
     */
    private static final Supplier<ExtensionProviders<ExtensionSerializer>> SUPPLIER =
            Suppliers.memoize(() -> ExtensionProviders.createProvider(ExtensionSerializer.class, "balance-computation-parameters"));

    /**
     * Gets the known extension serializers.
     */
    public static ExtensionProviders<ExtensionSerializer> getExtensionSerializers() {
        return SUPPLIER.get();
    }

    private JsonBalanceComputationParameters() {
    }

    /**
     * Reads parameters from a JSON file (will NOT rely on platform config).
     */
    public static BalanceComputationParameters read(Path jsonFile) {
        return update(new BalanceComputationParameters(), jsonFile);
    }

    /**
     * Reads parameters from a JSON file (will NOT rely on platform config).
     */
    public static BalanceComputationParameters read(InputStream jsonStream) {
        return update(new BalanceComputationParameters(), jsonStream);
    }

    /**
     * Updates parameters by reading the content of a JSON file.
     */
    public static BalanceComputationParameters update(BalanceComputationParameters parameters, Path jsonFile) {
        Objects.requireNonNull(jsonFile);
        try (InputStream is = Files.newInputStream(jsonFile)) {
            return update(parameters, is);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Updates parameters by reading the content of a JSON stream.
     */
    public static BalanceComputationParameters update(BalanceComputationParameters parameters, InputStream jsonStream) {
        try {
            ObjectMapper objectMapper = createObjectMapper();
            return objectMapper.readerForUpdating(parameters).readValue(jsonStream);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Writes parameters as JSON to a file.
     */
    public static void write(BalanceComputationParameters parameters, Path jsonFile) {
        Objects.requireNonNull(jsonFile);

        try (OutputStream outputStream = Files.newOutputStream(jsonFile)) {
            write(parameters, outputStream);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Writes parameters as JSON to an output stream.
     */
    public static void write(BalanceComputationParameters parameters, OutputStream outputStream) {
        try {
            ObjectMapper objectMapper = createObjectMapper();
            ObjectWriter writer = objectMapper.writerWithDefaultPrettyPrinter();
            writer.writeValue(outputStream, parameters);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Low level deserialization method, to be used for instance for reading balance computation parameters nested in another object.
     */
    public static BalanceComputationParameters deserialize(JsonParser parser, DeserializationContext context, BalanceComputationParameters parameters) throws IOException {
        return new BalanceComputationParametersDeserializer().deserialize(parser, context, parameters);
    }

    /**
     * Low level deserialization method, to be used for instance for updating balance computation parameters nested in another object.
     */
    public static BalanceComputationParameters deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        return new BalanceComputationParametersDeserializer().deserialize(parser, context);
    }

    /**
     * Low level serialization method, to be used for instance for writing balance computation parameters nested in another object.
     */
    public static void serialize(BalanceComputationParameters parameters, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        new BalanceComputationParametersSerializer().serialize(parameters, jsonGenerator, serializerProvider);
    }

    private static ObjectMapper createObjectMapper() {
        return JsonUtil.createObjectMapper()
                .registerModule(new BalanceComputationParametersJsonModule());
    }
}
