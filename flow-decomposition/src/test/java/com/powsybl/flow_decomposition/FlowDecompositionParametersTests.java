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

import static com.powsybl.flow_decomposition.FlowDecompositionParameters.XnecSelectionStrategy.INTERCONNECTION_OR_ZONE_TO_ZONE_PTDF_GT_5PC;
import static com.powsybl.flow_decomposition.FlowDecompositionParameters.XnecSelectionStrategy.ONLY_INTERCONNECTIONS;
import static org.junit.jupiter.api.Assertions.*;

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
        assertFalse(parameters.isRescaleEnabled());
        assertEquals(ONLY_INTERCONNECTIONS, parameters.getXnecSelectionStrategy());
        assertTrue(parameters.isDcFallbackEnabledAfterAcDivergence());
    }

    @Test
    void checkCompleteConfigurationOfParameters() {
        MapModuleConfig mapModuleConfig = platformConfig.createModuleConfig("flow-decomposition-default-parameters");
        mapModuleConfig.setStringProperty("enable-losses-compensation", Boolean.toString(true));
        mapModuleConfig.setStringProperty("losses-compensation-epsilon", Double.toString(2e-5));
        mapModuleConfig.setStringProperty("sensitivity-epsilon", Double.toString(3e-3));
        mapModuleConfig.setStringProperty("rescale-enabled", Boolean.toString(true));
        mapModuleConfig.setStringProperty("xnec-selection-strategy", INTERCONNECTION_OR_ZONE_TO_ZONE_PTDF_GT_5PC.name());
        mapModuleConfig.setStringProperty("dc-fallback-enabled-after-ac-divergence", Boolean.toString(false));

        FlowDecompositionParameters parameters = FlowDecompositionParameters.load(platformConfig);
        assertTrue(parameters.isLossesCompensationEnabled());
        assertEquals(2e-5, parameters.getLossesCompensationEpsilon(), EPSILON);
        assertEquals(3e-3, parameters.getSensitivityEpsilon(), EPSILON);
        assertTrue(parameters.isRescaleEnabled());
        assertEquals(INTERCONNECTION_OR_ZONE_TO_ZONE_PTDF_GT_5PC, parameters.getXnecSelectionStrategy());
        assertFalse(parameters.isDcFallbackEnabledAfterAcDivergence());
    }

    @Test
    void checkIncompleteConfigurationOfParameters() {
        MapModuleConfig mapModuleConfig = platformConfig.createModuleConfig("flow-decomposition-default-parameters");
        mapModuleConfig.setStringProperty("losses-compensation-epsilon", Double.toString(2e-5));

        FlowDecompositionParameters parameters = FlowDecompositionParameters.load(platformConfig);
        assertFalse(parameters.isLossesCompensationEnabled());
        assertEquals(2e-5, parameters.getLossesCompensationEpsilon(), EPSILON);
        assertEquals(1e-5, parameters.getSensitivityEpsilon(), EPSILON);
        assertFalse(parameters.isRescaleEnabled());
        assertEquals(ONLY_INTERCONNECTIONS, parameters.getXnecSelectionStrategy());
        assertTrue(parameters.isDcFallbackEnabledAfterAcDivergence());
    }
}
