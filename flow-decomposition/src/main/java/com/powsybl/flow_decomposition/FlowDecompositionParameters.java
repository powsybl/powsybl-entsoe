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
    static final boolean SAVE_INTERMEDIATES = true;
    static final boolean DO_NOT_SAVE_INTERMEDIATES = false;
    static final boolean ENABLE_RESCALED_RESULTS = true;
    static final boolean DISABLE_RESCALED_RESULTS = false;
    static final double DISABLE_SENSITIVITY_EPSILON = -1;
    static final boolean DISABLE_LOSSES_COMPENSATION = false;
    static final boolean ENABLE_LOSSES_COMPENSATION = true;
    static final double DISABLE_LOSSES_COMPENSATION_EPSILON = -1;
    private static final boolean DEFAULT_SAVE_INTERMEDIATES = DO_NOT_SAVE_INTERMEDIATES;
    private static final boolean DEFAULT_ENABLE_LOSSES_COMPENSATION = DISABLE_LOSSES_COMPENSATION;
    private static final double DEFAULT_LOSSES_COMPENSATION_EPSILON = 1e-5;
    private static final double DEFAULT_SENSITIVITY_EPSILON = 1e-5;
    private static final boolean DEFAULT_RESCALE_ENABLED = DISABLE_RESCALED_RESULTS;
    private boolean saveIntermediates;
    private boolean enableLossesCompensation;
    private double lossesCompensationEpsilon;
    private double sensitivityEpsilon;
    private boolean rescaleEnabled;

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
            parameters.setSaveIntermediates(moduleConfig.getBooleanProperty("save-intermediates", DEFAULT_SAVE_INTERMEDIATES));
            parameters.setEnableLossesCompensation(moduleConfig.getBooleanProperty("enable-losses-compensation", DEFAULT_ENABLE_LOSSES_COMPENSATION));
            parameters.setLossesCompensationEpsilon(moduleConfig.getDoubleProperty("losses-compensation-epsilon", DEFAULT_LOSSES_COMPENSATION_EPSILON));
            parameters.setSensitivityEpsilon(moduleConfig.getDoubleProperty("sensitivity-epsilon", DEFAULT_SENSITIVITY_EPSILON));
            parameters.setRescaleEnabled(moduleConfig.getBooleanProperty("rescale-enabled", DEFAULT_RESCALE_ENABLED));
        });
    }

    public FlowDecompositionParameters() {
        this.saveIntermediates = DEFAULT_SAVE_INTERMEDIATES;
        this.enableLossesCompensation = DEFAULT_ENABLE_LOSSES_COMPENSATION;
        this.lossesCompensationEpsilon = DEFAULT_LOSSES_COMPENSATION_EPSILON;
        this.sensitivityEpsilon = DEFAULT_SENSITIVITY_EPSILON;
        this.rescaleEnabled = DEFAULT_RESCALE_ENABLED;
    }

    public boolean doesSaveIntermediates() {
        return saveIntermediates;
    }

    public void setSaveIntermediates(boolean saveIntermediates) {
        this.saveIntermediates = saveIntermediates;
    }

    public void setEnableLossesCompensation(boolean enableLossesCompensation) {
        this.enableLossesCompensation = enableLossesCompensation;
    }

    public boolean isLossesCompensationEnabled() {
        return enableLossesCompensation;
    }

    public double getLossesCompensationEpsilon() {
        return lossesCompensationEpsilon;
    }

    public void setLossesCompensationEpsilon(double lossesCompensationEpsilon) {
        this.lossesCompensationEpsilon = lossesCompensationEpsilon;
    }

    public double getSensitivityEpsilon() {
        return sensitivityEpsilon;
    }

    public void setSensitivityEpsilon(double sensitivityEpsilon) {
        this.sensitivityEpsilon = sensitivityEpsilon;
    }

    public boolean isRescaleEnabled() {
        return rescaleEnabled;
    }

    public void setRescaleEnabled(boolean rescaleEnabled) {
        this.rescaleEnabled = rescaleEnabled;
    }
}
