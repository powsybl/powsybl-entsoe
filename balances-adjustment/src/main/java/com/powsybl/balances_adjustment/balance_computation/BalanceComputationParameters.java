/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.balances_adjustment.balance_computation;

import com.google.common.base.Suppliers;
import com.powsybl.commons.config.PlatformConfig;
import com.powsybl.commons.extensions.AbstractExtendable;
import com.powsybl.commons.extensions.Extension;
import com.powsybl.commons.extensions.ExtensionConfigLoader;
import com.powsybl.commons.extensions.ExtensionProviders;
import com.powsybl.iidm.modification.scalable.ScalingParameters;
import com.powsybl.loadflow.LoadFlowParameters;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * parameters for balance computation.
 *
 * @author Ameni Walha {@literal <ameni.walha at rte-france.com>}
 * @author Mohamed Ben Rejeb {@literal <mohamed.benrejeb at rte-france.com>}
 */
public class BalanceComputationParameters extends AbstractExtendable<BalanceComputationParameters> {

    public static final String VERSION = "1.1";

    public static final double DEFAULT_THRESHOLD_NET_POSITION = 1;
    public static final int DEFAULT_MAX_NUMBER_ITERATIONS = 5;
    public static final MismatchMode DEFAULT_MISMATCH_MODE = MismatchMode.SQUARED;

    /**
     * how overall mismatch is to be computed from individual area mismatches
     */
    public enum MismatchMode {
        /**
         * overall mismatch computed as sum of squared individual area mismatches
         */
        SQUARED,
        /**
         * overall mismatch computed as worst individual area mismatches (in absolute value)
         */
        MAX,
    }

    /**
     * Threshold for comparing net positions (given in MW).
     * Under this threshold, the network area is balanced
     */
    private double thresholdNetPosition;

    /**
     * Maximum iteration number for balances adjustment
     */
    private int maxNumberIterations;

    /**
     * Mode for overall mismatch calculation
     */
    private MismatchMode mismatchMode;

    /**
     * Constructor with default parameters
     */
    public BalanceComputationParameters() {
        this(DEFAULT_THRESHOLD_NET_POSITION, DEFAULT_MAX_NUMBER_ITERATIONS, DEFAULT_MISMATCH_MODE);
    }

    /**
     * Constructor with given parameters
     * @param threshold Threshold for comparing net positions (given in MW)
     * @param maxNumberIterations Maximum iteration number for balances adjustment
     * @param mismatchMode How overall mismatch is to be computed from individual area mismatches
     */
    public BalanceComputationParameters(double threshold, int maxNumberIterations, MismatchMode mismatchMode) {
        this.thresholdNetPosition = checkThresholdNetPosition(threshold);
        this.maxNumberIterations = checkMaxNumberIterations(maxNumberIterations);
        this.mismatchMode = mismatchMode;
    }

    /**
     * Constructor with given parameters
     * @param threshold Threshold for comparing net positions (given in MW)
     * @param maxNumberIterations Maximum iteration number for balances adjustment
     */
    public BalanceComputationParameters(double threshold, int maxNumberIterations) {
        this(threshold, maxNumberIterations, DEFAULT_MISMATCH_MODE);
    }

    /**
     * @deprecated Use {@link #BalanceComputationParameters()} or {@link #BalanceComputationParameters(double, int)} instead.
     */
    @Deprecated(since = "2.3.0")
    public BalanceComputationParameters(double threshold, int maxNumberIterations, boolean loadPowerFactorConstant) {
        this(threshold, maxNumberIterations, DEFAULT_MISMATCH_MODE);
        scalingParameters.setConstantPowerFactor(loadPowerFactorConstant);
    }

    private static double checkThresholdNetPosition(double threshold) {
        if (threshold < 0) {
            throw new IllegalArgumentException("Threshold must be positive");
        }
        return threshold;
    }

    private static int checkMaxNumberIterations(int maxNumberIterations) {
        if (maxNumberIterations < 0) {
            throw new IllegalArgumentException("The maximum number of iterations must be positive");
        }
        return maxNumberIterations;
    }

    public ScalingParameters getScalingParameters() {
        return scalingParameters;
    }

    public BalanceComputationParameters setScalingParameters(ScalingParameters scalingParameters) {
        this.scalingParameters = Objects.requireNonNull(scalingParameters);
        return this;
    }

    public MismatchMode getMismatchMode() {
        return mismatchMode;
    }

    public BalanceComputationParameters setMismatchMode(MismatchMode mismatchMode) {
        this.mismatchMode = mismatchMode;
        return this;
    }

    /**
     * @deprecated Use {@link #getScalingParameters()} and {@link ScalingParameters#isConstantPowerFactor()} instead.
     */
    @Deprecated(since = "2.3.0")
    public boolean isLoadPowerFactorConstant() {
        return scalingParameters.isConstantPowerFactor();
    }

    /**
     * @deprecated Use {@link #getScalingParameters()} and {@link ScalingParameters#setConstantPowerFactor(boolean)} instead.
     */
    @Deprecated(since = "2.3.0")
    public BalanceComputationParameters setLoadPowerFactorConstant(boolean loadPowerFactorConstant) {
        this.scalingParameters.setConstantPowerFactor(loadPowerFactorConstant);
        return this;
    }

    /**
     * A configuration loader interface for the RaoComputationParameters extensions loaded from the platform configuration
     *
     * @param <E> The extension class
     */
    public interface ConfigLoader<E extends Extension<BalanceComputationParameters>> extends ExtensionConfigLoader<BalanceComputationParameters, E> {
    }

    private static final Supplier<ExtensionProviders<ConfigLoader>> SUPPLIER =
            Suppliers.memoize(() -> ExtensionProviders.createProvider(ConfigLoader.class, "balance-computation-parameters"));

    private LoadFlowParameters loadFlowParameters = new LoadFlowParameters();

    private ScalingParameters scalingParameters = new ScalingParameters();

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
        platformConfig.getOptionalModuleConfig("balance-computation-parameters").ifPresent(config -> parameters
                .setMaxNumberIterations(config.getIntProperty("maxNumberIterations", DEFAULT_MAX_NUMBER_ITERATIONS))
                .setThresholdNetPosition(config.getDoubleProperty("thresholdNetPosition", DEFAULT_THRESHOLD_NET_POSITION))
                .setMismatchMode(config.getEnumProperty("mismatchMode", MismatchMode.class, DEFAULT_MISMATCH_MODE)));
        parameters.readExtensions(platformConfig);

        parameters.setLoadFlowParameters(LoadFlowParameters.load(platformConfig));
        parameters.setScalingParameters(ScalingParameters.load(platformConfig));
        return parameters;
    }

    private void readExtensions(PlatformConfig platformConfig) {
        for (ConfigLoader provider : SUPPLIER.get().getProviders()) {
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
