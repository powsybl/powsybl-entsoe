/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.balances_adjustment.balance_computation;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.powsybl.commons.config.PlatformConfig;
import com.powsybl.commons.extensions.*;
import com.powsybl.loadflow.LoadFlowParameters;

import java.util.Objects;

/**
 * parameters for balance computation.
 *
 * @author Ameni Walha {@literal <ameni.walha at rte-france.com>}
 * @author Mohamed Ben Rejeb {@literal <mohamed.benrejeb at rte-france.com>}
 */
public class BalanceComputationParameters extends AbstractExtendable<BalanceComputationParameters> {

    public static final double DEFAULT_THRESHOLD_NET_POSITION = 1;
    public static final int DEFAULT_MAX_NUMBER_ITERATIONS = 5;
    public static final boolean DEFAULT_LOAD_POWER_FACTOR_CONSTANT = false;

    /**
     * Threshold for comparing net positions (given in MW).
     * Under this threshold, the network area is balanced
     */
    private double thresholdNetPosition;

    /**
     * Maximum iteration number for balances adjustment
     */
    private int maxNumberIterations;

    private boolean loadPowerFactorConstant;

    /**
     * Constructor with default parameters
     */
    public BalanceComputationParameters() {
        this(DEFAULT_THRESHOLD_NET_POSITION, DEFAULT_MAX_NUMBER_ITERATIONS, DEFAULT_LOAD_POWER_FACTOR_CONSTANT);
    }

    /**
     * Constructor with given parameters
     * @param threshold Threshold for comparing net positions (given in MW)
     * @param maxNumberIterations Maximum iteration number for balances adjustment
     */
    public BalanceComputationParameters(double threshold, int maxNumberIterations) {
        this(threshold, maxNumberIterations, DEFAULT_LOAD_POWER_FACTOR_CONSTANT);
    }

    public BalanceComputationParameters(double threshold, int maxNumberIterations, boolean loadPowerFactorConstant) {
        this.thresholdNetPosition = checkThresholdNetPosition(threshold);
        this.maxNumberIterations = checkMaxNumberIterations(maxNumberIterations);
        this.loadPowerFactorConstant = loadPowerFactorConstant;
    }

    private static final double checkThresholdNetPosition(double threshold) {
        if (threshold < 0) {
            throw new IllegalArgumentException("Threshold must be positive");
        }
        return threshold;
    }

    private static final int checkMaxNumberIterations(int maxNumberIterations) {
        if (maxNumberIterations < 0) {
            throw new IllegalArgumentException("The maximum number of iterations must be positive");
        }
        return maxNumberIterations;
    }

    public boolean isLoadPowerFactorConstant() {
        return loadPowerFactorConstant;
    }

    public void setLoadPowerFactorConstant(boolean loadPowerFactorConstant) {
        this.loadPowerFactorConstant = loadPowerFactorConstant;
    }

    /**
     * A configuration loader interface for the RaoComputationParameters extensions loaded from the platform configuration
     *
     * @param <E> The extension class
     */
    public static interface ConfigLoader<E extends Extension<BalanceComputationParameters>> extends ExtensionConfigLoader<BalanceComputationParameters, E> {
    }

    private static final Supplier<ExtensionProviders<ConfigLoader>> SUPPLIER =
            Suppliers.memoize(() -> ExtensionProviders.createProvider(ConfigLoader.class, "balance-computation-parameters"));

    private LoadFlowParameters loadFlowParameters = new LoadFlowParameters();

    /**
     * Load parameters from platform default config.
     */
    public static BalanceComputationParameters load() {
        return load(PlatformConfig.defaultConfig());
    }

    /**
     * Load parameters from a provided platform config.
     */
    public static BalanceComputationParameters load(PlatformConfig platformConfig) {
        Objects.requireNonNull(platformConfig);

        BalanceComputationParameters parameters = new BalanceComputationParameters();
        parameters.readExtensions(platformConfig);

        parameters.setLoadFlowParameters(LoadFlowParameters.load(platformConfig));
        return parameters;
    }

    private void readExtensions(PlatformConfig platformConfig) {
        for (ExtensionConfigLoader provider : SUPPLIER.get().getProviders()) {
            addExtension(provider.getExtensionClass(), provider.load(platformConfig));
        }
    }

    public LoadFlowParameters getLoadFlowParameters() {
        return loadFlowParameters;
    }

    public BalanceComputationParameters setLoadFlowParameters(LoadFlowParameters loadFlowParameters) {
        this.loadFlowParameters = Objects.requireNonNull(loadFlowParameters);
        return this;
    }

    public double getThresholdNetPosition() {
        return thresholdNetPosition;
    }

    public BalanceComputationParameters setThresholdNetPosition(double thresholdNetPosition) {
        this.thresholdNetPosition = checkThresholdNetPosition(thresholdNetPosition);
        return this;
    }

    public int getMaxNumberIterations() {
        return maxNumberIterations;
    }

    public BalanceComputationParameters setMaxNumberIterations(int maxNumberIterations) {
        this.maxNumberIterations = checkMaxNumberIterations(maxNumberIterations);
        return this;
    }
}
