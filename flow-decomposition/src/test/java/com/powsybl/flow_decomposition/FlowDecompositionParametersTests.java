/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.flow_decomposition;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import com.powsybl.commons.config.InMemoryPlatformConfig;
import com.powsybl.commons.config.MapModuleConfig;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.FileSystem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
class FlowDecompositionParametersTests {
    private static final double EPSILON = 1e-5;
    private static FileSystem fileSystem;
    private InMemoryPlatformConfig platformConfig;

    @BeforeAll
    static void createFileSystem() {
        fileSystem = Jimfs.newFileSystem(Configuration.unix());
    }

    @BeforeEach
    void createInMemoryPlatformConfig() {
        platformConfig = new InMemoryPlatformConfig(fileSystem);
    }

    @AfterAll
    static void closeFileSystem() throws Exception {
        fileSystem.close();
    }

    @Test
    void checkDefaultParameters() {
        FlowDecompositionParameters parameters = FlowDecompositionParameters.load();
        assertFalse(parameters.isLossesCompensationEnabled());
        assertEquals(1e-5, parameters.getLossesCompensationEpsilon(), EPSILON);
        assertEquals(1e-5, parameters.getSensitivityEpsilon(), EPSILON);
        assertEquals(FlowDecompositionParameters.RescaleMode.NONE, parameters.getRescaleMode());
        assertTrue(parameters.isDcFallbackEnabledAfterAcDivergence());
        assertEquals(15000, parameters.getSensitivityVariableBatchSize());
    }

    @Test
    void checkCompleteConfigurationOfParameters() {
        MapModuleConfig mapModuleConfig = platformConfig.createModuleConfig("flow-decomposition-default-parameters");
        mapModuleConfig.setStringProperty("enable-losses-compensation", Boolean.toString(true));
        mapModuleConfig.setStringProperty("losses-compensation-epsilon", Double.toString(2e-5));
        mapModuleConfig.setStringProperty("sensitivity-epsilon", Double.toString(3e-3));
        mapModuleConfig.setStringProperty("rescale-mode", FlowDecompositionParameters.RescaleMode.ACER_METHODOLOGY.name());
        mapModuleConfig.setStringProperty("dc-fallback-enabled-after-ac-divergence", Boolean.toString(false));
        mapModuleConfig.setStringProperty("sensitivity-variable-batch-size", Integer.toString(1234));

        FlowDecompositionParameters parameters = FlowDecompositionParameters.load(platformConfig);
        assertTrue(parameters.isLossesCompensationEnabled());
        assertEquals(2e-5, parameters.getLossesCompensationEpsilon(), EPSILON);
        assertEquals(3e-3, parameters.getSensitivityEpsilon(), EPSILON);
        assertEquals(FlowDecompositionParameters.RescaleMode.ACER_METHODOLOGY, parameters.getRescaleMode());
        assertFalse(parameters.isDcFallbackEnabledAfterAcDivergence());
        assertEquals(1234, parameters.getSensitivityVariableBatchSize());
    }

    @Test
    void checkIncompleteConfigurationOfParameters() {
        MapModuleConfig mapModuleConfig = platformConfig.createModuleConfig("flow-decomposition-default-parameters");
        mapModuleConfig.setStringProperty("losses-compensation-epsilon", Double.toString(2e-5));

        FlowDecompositionParameters parameters = FlowDecompositionParameters.load(platformConfig);
        assertFalse(parameters.isLossesCompensationEnabled());
        assertEquals(2e-5, parameters.getLossesCompensationEpsilon(), EPSILON);
        assertEquals(1e-5, parameters.getSensitivityEpsilon(), EPSILON);
        assertEquals(FlowDecompositionParameters.RescaleMode.NONE, parameters.getRescaleMode());
        assertTrue(parameters.isDcFallbackEnabledAfterAcDivergence());
    }
}
