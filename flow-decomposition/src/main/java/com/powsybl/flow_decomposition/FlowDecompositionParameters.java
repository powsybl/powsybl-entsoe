/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.flow_decomposition;

import com.powsybl.commons.config.PlatformConfig;

import java.util.Objects;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 */
public class FlowDecompositionParameters {
    public static final double DISABLE_SENSITIVITY_EPSILON = -1;
    public static final boolean DISABLE_LOSSES_COMPENSATION = false;
    public static final boolean ENABLE_LOSSES_COMPENSATION = true;
    public static final double DISABLE_LOSSES_COMPENSATION_EPSILON = -1;
    public static final boolean DEFAULT_ENABLE_LOSSES_COMPENSATION = DISABLE_LOSSES_COMPENSATION;
    public static final double DEFAULT_LOSSES_COMPENSATION_EPSILON = 1e-5;
    public static final double DEFAULT_SENSITIVITY_EPSILON = 1e-5;
    public static final boolean DISABLE_DC_FALLBACK_AFTER_AC_DIVERGENCE = false;
    public static final boolean ENABLE_DC_FALLBACK_AFTER_AC_DIVERGENCE = true;
    public static final boolean DEFAULT_DC_FALLBACK_ENABLED_AFTER_AC_DIVERGENCE = ENABLE_DC_FALLBACK_AFTER_AC_DIVERGENCE;
    private static final int DEFAULT_SENSITIVITY_VARIABLE_BATCH_SIZE = 15000;

    public enum RescaleMode {
        NONE,
        RELU,
        PROPORTIONAL
    }

    public static final RescaleMode DEFAULT_RESCALE_MODE = RescaleMode.NONE;
    private boolean enableLossesCompensation;
    private double lossesCompensationEpsilon;
    private double sensitivityEpsilon;
    private RescaleMode rescaleMode;
    private boolean dcFallbackEnabledAfterAcDivergence;
    private int sensitivityVariableBatchSize;

    public static FlowDecompositionParameters load() {
        return load(PlatformConfig.defaultConfig());
    }

    public static FlowDecompositionParameters load(PlatformConfig platformConfig) {
        FlowDecompositionParameters parameters = new FlowDecompositionParameters();
        load(parameters, platformConfig);
        return parameters;
    }

    private static void load(FlowDecompositionParameters parameters, PlatformConfig platformConfig) {
        Objects.requireNonNull(parameters);
        Objects.requireNonNull(platformConfig);
        platformConfig.getOptionalModuleConfig("flow-decomposition-default-parameters").ifPresent(moduleConfig -> {
            parameters.setEnableLossesCompensation(moduleConfig.getBooleanProperty("enable-losses-compensation", DEFAULT_ENABLE_LOSSES_COMPENSATION));
            parameters.setLossesCompensationEpsilon(moduleConfig.getDoubleProperty("losses-compensation-epsilon", DEFAULT_LOSSES_COMPENSATION_EPSILON));
            parameters.setSensitivityEpsilon(moduleConfig.getDoubleProperty("sensitivity-epsilon", DEFAULT_SENSITIVITY_EPSILON));
            parameters.setRescaleMode(moduleConfig.getEnumProperty("rescale-mode", RescaleMode.class, DEFAULT_RESCALE_MODE));
            parameters.setDcFallbackEnabledAfterAcDivergence(moduleConfig.getBooleanProperty("dc-fallback-enabled-after-ac-divergence", DEFAULT_DC_FALLBACK_ENABLED_AFTER_AC_DIVERGENCE));
            parameters.setSensitivityVariableBatchSize(moduleConfig.getIntProperty("sensitivity-variable-batch-size", DEFAULT_SENSITIVITY_VARIABLE_BATCH_SIZE));
        });
    }

    public FlowDecompositionParameters() {
        this.enableLossesCompensation = DEFAULT_ENABLE_LOSSES_COMPENSATION;
        this.lossesCompensationEpsilon = DEFAULT_LOSSES_COMPENSATION_EPSILON;
        this.sensitivityEpsilon = DEFAULT_SENSITIVITY_EPSILON;
        this.rescaleMode = DEFAULT_RESCALE_MODE;
        this.dcFallbackEnabledAfterAcDivergence = DEFAULT_DC_FALLBACK_ENABLED_AFTER_AC_DIVERGENCE;
        this.sensitivityVariableBatchSize = DEFAULT_SENSITIVITY_VARIABLE_BATCH_SIZE;
    }

    public FlowDecompositionParameters setEnableLossesCompensation(boolean enableLossesCompensation) {
        this.enableLossesCompensation = enableLossesCompensation;
        return this;
    }

    public boolean isLossesCompensationEnabled() {
        return enableLossesCompensation;
    }

    public double getLossesCompensationEpsilon() {
        return lossesCompensationEpsilon;
    }

    public FlowDecompositionParameters setLossesCompensationEpsilon(double lossesCompensationEpsilon) {
        this.lossesCompensationEpsilon = lossesCompensationEpsilon;
        return this;
    }

    public double getSensitivityEpsilon() {
        return sensitivityEpsilon;
    }

    public FlowDecompositionParameters setSensitivityEpsilon(double sensitivityEpsilon) {
        this.sensitivityEpsilon = sensitivityEpsilon;
        return this;
    }

    public RescaleMode getRescaleMode() {
        return rescaleMode;
    }

    public FlowDecompositionParameters setRescaleMode(RescaleMode rescaleMode) {
        this.rescaleMode = rescaleMode;
        return this;
    }

    public boolean isDcFallbackEnabledAfterAcDivergence() {
        return this.dcFallbackEnabledAfterAcDivergence;
    }

    public FlowDecompositionParameters setDcFallbackEnabledAfterAcDivergence(boolean dcFallbackEnabledAfterAcDivergence) {
        this.dcFallbackEnabledAfterAcDivergence = dcFallbackEnabledAfterAcDivergence;
        return this;
    }

    public int getSensitivityVariableBatchSize() {
        return sensitivityVariableBatchSize;
    }

    public FlowDecompositionParameters setSensitivityVariableBatchSize(int sensitivityVariableBatchSize) {
        this.sensitivityVariableBatchSize = sensitivityVariableBatchSize;
        return this;
    }
}
