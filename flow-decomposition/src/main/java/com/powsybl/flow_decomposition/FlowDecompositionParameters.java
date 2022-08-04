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
    public enum XnecSelectionStrategy {
        ONLY_INTERCONNECTIONS,
        ZONE_TO_ZONE_PTDF_CRITERIA,
    }

    public enum ContingencyStrategy  {
        ONLY_N_STATE,
        AUTO_CONTINGENCY,
    }

    public static final boolean SAVE_INTERMEDIATES = true;
    public static final boolean DO_NOT_SAVE_INTERMEDIATES = false;
    public static final boolean ENABLE_RESCALED_RESULTS = true;
    public static final boolean DISABLE_RESCALED_RESULTS = false;
    public static final double DISABLE_SENSITIVITY_EPSILON = -1;
    public static final boolean DISABLE_LOSSES_COMPENSATION = false;
    public static final boolean ENABLE_LOSSES_COMPENSATION = true;
    public static final double DISABLE_LOSSES_COMPENSATION_EPSILON = -1;
    public static final boolean DEFAULT_SAVE_INTERMEDIATES = DO_NOT_SAVE_INTERMEDIATES;
    public static final boolean DEFAULT_ENABLE_LOSSES_COMPENSATION = DISABLE_LOSSES_COMPENSATION;
    public static final double DEFAULT_LOSSES_COMPENSATION_EPSILON = 1e-5;
    public static final double DEFAULT_SENSITIVITY_EPSILON = 1e-5;
    public static final boolean DEFAULT_RESCALE_ENABLED = DISABLE_RESCALED_RESULTS;
    private static final XnecSelectionStrategy DEFAULT_COMPUTE_ZONAL_PTDF = XnecSelectionStrategy.ONLY_INTERCONNECTIONS;
    public static final ContingencyStrategy DEFAULT_CONTINGENCY_STRATEGY = ContingencyStrategy.ONLY_N_STATE;
    private boolean saveIntermediates;
    private boolean enableLossesCompensation;
    private double lossesCompensationEpsilon;
    private double sensitivityEpsilon;
    private boolean rescaleEnabled;
    private XnecSelectionStrategy xnecSelectionStrategy;
    private ContingencyStrategy contingencyStrategy;

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
            parameters.setXnecSelectionStrategy(moduleConfig.getEnumProperty("branch-selection-strategy", XnecSelectionStrategy.class, DEFAULT_COMPUTE_ZONAL_PTDF));
            parameters.setContingencyStrategy(moduleConfig.getEnumProperty("contingency-strategy", ContingencyStrategy.class, DEFAULT_CONTINGENCY_STRATEGY));
        });
    }

    public FlowDecompositionParameters() {
        this.saveIntermediates = DEFAULT_SAVE_INTERMEDIATES;
        this.enableLossesCompensation = DEFAULT_ENABLE_LOSSES_COMPENSATION;
        this.lossesCompensationEpsilon = DEFAULT_LOSSES_COMPENSATION_EPSILON;
        this.sensitivityEpsilon = DEFAULT_SENSITIVITY_EPSILON;
        this.rescaleEnabled = DEFAULT_RESCALE_ENABLED;
        this.xnecSelectionStrategy = DEFAULT_COMPUTE_ZONAL_PTDF;
        this.contingencyStrategy = DEFAULT_CONTINGENCY_STRATEGY;
    }

    public boolean doesSaveIntermediates() {
        return saveIntermediates;
    }

    public FlowDecompositionParameters setSaveIntermediates(boolean saveIntermediates) {
        this.saveIntermediates = saveIntermediates;
        return this;
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

    public boolean isRescaleEnabled() {
        return rescaleEnabled;
    }

    public FlowDecompositionParameters setRescaleEnabled(boolean rescaleEnabled) {
        this.rescaleEnabled = rescaleEnabled;
        return this;
    }

    public XnecSelectionStrategy getXnecSelectionStrategy() {
        return xnecSelectionStrategy;
    }

    public FlowDecompositionParameters setXnecSelectionStrategy(XnecSelectionStrategy xnecSelectionStrategy) {
        this.xnecSelectionStrategy = xnecSelectionStrategy;
        return this;
    }

    public ContingencyStrategy getContingencyStrategy() {
        return contingencyStrategy;
    }

    public FlowDecompositionParameters setContingencyStrategy(ContingencyStrategy contingencyStrategy) {
        this.contingencyStrategy = contingencyStrategy;
        return this;
    }
}
