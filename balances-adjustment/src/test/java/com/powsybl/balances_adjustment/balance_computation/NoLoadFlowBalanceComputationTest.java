/*
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.balances_adjustment.balance_computation;

import com.powsybl.balances_adjustment.util.CountryAreaFactory;
import com.powsybl.iidm.modification.scalable.Scalable;
import com.powsybl.iidm.network.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Joris Mancini <joris.mancini_externe at rte-france.com>
 */
class NoLoadFlowBalanceComputationTest {
    private Network simpleNetwork;
    private CountryAreaFactory countryAreaFR;
    private CountryAreaFactory countryAreaBE;
    private Scalable scalableFR;
    private Scalable scalableBE;

    private BalanceComputationParameters parameters;

    @BeforeEach
    void setUp() {
        simpleNetwork = Network.read("testSimpleNetwork.xiidm", getClass().getResourceAsStream("/testSimpleNetwork.xiidm"));

        countryAreaFR = new CountryAreaFactory(true, Country.FR);
        countryAreaBE = new CountryAreaFactory(true, Country.BE);

        parameters = new BalanceComputationParameters();

        scalableFR = Scalable.proportional(Arrays.asList(60.0, 40.0),
            Arrays.asList(Scalable.onGenerator("GENERATOR_FR"), Scalable.onLoad("LOAD_FR")));

        scalableBE = Scalable.proportional(Arrays.asList(60.0, 40.0),
            Arrays.asList(Scalable.onGenerator("GENERATOR_BE"), Scalable.onLoad("LOAD_BE")));
    }

    @Test
    void testWithNoScaling() {
        List<BalanceComputationArea> areas = new ArrayList<>();
        areas.add(new BalanceComputationArea("FR", countryAreaFR, scalableFR, 1200));
        areas.add(new BalanceComputationArea("BE", countryAreaBE, scalableBE, -1200));

        NoLoadFlowBalanceComputation balanceComputation = new NoLoadFlowBalanceComputation(areas);

        BalanceComputationResult result = balanceComputation.run(simpleNetwork, simpleNetwork.getVariantManager().getWorkingVariantId(), parameters).join();
        assertEquals(BalanceComputationResult.Status.SUCCESS, result.getStatus());
        assertEquals(1, result.getIterationCount());
        assertTrue(result.getBalancedScalingMap().values().stream().allMatch(v -> v == 0.));
    }

    @Test
    void testWithScaling() {
        List<BalanceComputationArea> areas = new ArrayList<>();
        areas.add(new BalanceComputationArea("FR", countryAreaFR, scalableFR, 1500));
        areas.add(new BalanceComputationArea("BE", countryAreaBE, scalableBE, -1500));

        NoLoadFlowBalanceComputation balanceComputation = new NoLoadFlowBalanceComputation(areas);

        BalanceComputationResult result = balanceComputation.run(simpleNetwork, simpleNetwork.getVariantManager().getWorkingVariantId(), parameters).join();
        assertEquals(BalanceComputationResult.Status.SUCCESS, result.getStatus());
        assertEquals(1, result.getIterationCount());
        assertTrue(result.getBalancedScalingMap().values().stream().allMatch(v -> Math.abs(v) == 300.));
    }

    @Test
    void testSuccessEvenIfUnbalanced() {
        List<BalanceComputationArea> areas = new ArrayList<>();
        areas.add(new BalanceComputationArea("FR", countryAreaFR, scalableFR, 3000));
        areas.add(new BalanceComputationArea("BE", countryAreaBE, scalableBE, -1500));

        NoLoadFlowBalanceComputation balanceComputation = new NoLoadFlowBalanceComputation(areas);

        BalanceComputationResult result = balanceComputation.run(simpleNetwork, simpleNetwork.getVariantManager().getWorkingVariantId(), parameters).join();
        assertEquals(BalanceComputationResult.Status.SUCCESS, result.getStatus());
        assertEquals(1, result.getIterationCount());
    }
}
