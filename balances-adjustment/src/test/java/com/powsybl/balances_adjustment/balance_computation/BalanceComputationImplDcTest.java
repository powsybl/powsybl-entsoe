/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.balances_adjustment.balance_computation;

import com.powsybl.balances_adjustment.util.CountryAreaFactory;
import com.powsybl.computation.ComputationManager;
import com.powsybl.computation.local.LocalComputationManager;
import com.powsybl.iidm.modification.scalable.Scalable;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.math.matrix.DenseMatrixFactory;
import com.powsybl.openloadflow.OpenLoadFlowProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Ameni Walha {@literal <ameni.walha at rte-france.com>}
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
class BalanceComputationImplDcTest {
    private Network testNetwork1;
    private ComputationManager computationManager;
    private CountryAreaFactory countryAreaFR;
    private CountryAreaFactory countryAreaBE;
    private Scalable scalableFR;
    private Scalable scalableBE;

    private BalanceComputationParameters parameters;
    private BalanceComputationFactory balanceComputationFactory;
    private LoadFlow.Runner loadFlowRunner;

    @BeforeEach
    void setUp() {
        testNetwork1 = Network.read("testCase.xiidm", getClass().getResourceAsStream("/testCase.xiidm"));

        countryAreaFR = new CountryAreaFactory(Country.FR);
        countryAreaBE = new CountryAreaFactory(Country.BE);

        computationManager = LocalComputationManager.getDefault();

        parameters = new BalanceComputationParameters();
        parameters.getLoadFlowParameters().setDc(true);
        parameters.getLoadFlowParameters().setDistributedSlack(false);
        parameters.getLoadFlowParameters().setWriteSlackBus(false);
        balanceComputationFactory = new BalanceComputationFactoryImpl();

        loadFlowRunner = new LoadFlow.Runner(new OpenLoadFlowProvider(new DenseMatrixFactory()));

        scalableFR = Scalable.proportional(Arrays.asList(60.0, 30.0, 10.0),
                Arrays.asList(Scalable.onGenerator("FFR1AA1 _generator"), Scalable.onGenerator("FFR2AA1 _generator"), Scalable.onGenerator("FFR3AA1 _generator")));
        scalableBE = Scalable.proportional(Arrays.asList(60.0, 30.0, 10.0),
                Arrays.asList(Scalable.onGenerator("BBE1AA1 _generator"), Scalable.onGenerator("BBE3AA1 _generator"), Scalable.onGenerator("BBE2AA1 _generator")));
    }

    @Test
    void testBalancedNetwork() {
        List<BalanceComputationArea> areas = new ArrayList<>();
        areas.add(new BalanceComputationArea("FR", countryAreaFR, scalableFR, 1000.));
        areas.add(new BalanceComputationArea("BE", countryAreaBE, scalableBE, 1500.));

        BalanceComputation balanceComputation = balanceComputationFactory.create(areas, loadFlowRunner, computationManager);

        BalanceComputationResult result = balanceComputation.run(testNetwork1, testNetwork1.getVariantManager().getWorkingVariantId(), parameters).join();

        assertEquals(BalanceComputationResult.Status.SUCCESS, result.getStatus());
        assertEquals(1, result.getIterationCount());
    }

    @Test
    void testBalancedNetworkAfter1Scaling() {
        List<BalanceComputationArea> areas = new ArrayList<>();
        areas.add(new BalanceComputationArea("FR", countryAreaFR, scalableFR, 1200.));
        areas.add(new BalanceComputationArea("BE", countryAreaBE, scalableBE, 1300.));

        BalanceComputation balanceComputation = balanceComputationFactory.create(areas, loadFlowRunner, computationManager);

        BalanceComputationResult result = balanceComputation.run(testNetwork1, testNetwork1.getVariantManager().getWorkingVariantId(), parameters).join();

        assertEquals(BalanceComputationResult.Status.SUCCESS, result.getStatus());
        assertEquals(2, result.getIterationCount());
    }

    @Test
    void testBalancesAdjustmentWithDifferentStateId() {
        String newStateId = "NewStateId";
        testNetwork1.getVariantManager().cloneVariant(testNetwork1.getVariantManager().getWorkingVariantId(), newStateId);

        Scalable scalable1 = Scalable.onGenerator("FFR1AA1 _generator");
        Scalable scalable2 = Scalable.onGenerator("FFR2AA1 _generator");
        Scalable scalable3 = Scalable.onGenerator("FFR3AA1 _generator");
        Scalable newScalableFr = Scalable.proportional(28.f, scalable1, 28f, scalable2, 44.f, scalable3);
        List<BalanceComputationArea> areas = Collections.singletonList(new BalanceComputationArea("FR", countryAreaFR, newScalableFr, 1100.));

        BalanceComputation balanceComputation = balanceComputationFactory.create(areas, loadFlowRunner, computationManager);

        BalanceComputationResult result = balanceComputation.run(testNetwork1, newStateId, parameters).join();

        assertEquals(BalanceComputationResult.Status.SUCCESS, result.getStatus());
        assertEquals(2, result.getIterationCount());
        // Check net position does not change with the initial state id after balances
        assertEquals(1000., countryAreaFR.create(testNetwork1).getNetPosition(), 1.);
        // Check target net position after balances with the new state id
        testNetwork1.getVariantManager().setWorkingVariant(newStateId);
        assertEquals(1100., countryAreaFR.create(testNetwork1).getNetPosition(), 1.);
    }

    @Test
    void testUnBalancedNetwork() {
        List<BalanceComputationArea> areas = new ArrayList<>();
        areas.add(new BalanceComputationArea("FR", countryAreaFR, scalableFR, 1200.));
        areas.add(new BalanceComputationArea("BE", countryAreaBE, scalableBE, 1500.));

        BalanceComputation balanceComputation = balanceComputationFactory.create(areas, loadFlowRunner, computationManager);

        BalanceComputationResult result = balanceComputation.run(testNetwork1, testNetwork1.getVariantManager().getWorkingVariantId(), parameters).join();

        assertEquals(BalanceComputationResult.Status.FAILED, result.getStatus());
        assertEquals(5, result.getIterationCount());
        assertEquals(1000, countryAreaFR.create(testNetwork1).getNetPosition(), 1e-3);
        assertEquals(1500, countryAreaBE.create(testNetwork1).getNetPosition(), 1e-3);
    }

    @Test
    void testConstantPowerFactor() {
        parameters.getScalingParameters().setConstantPowerFactor(true);
        List<BalanceComputationArea> areas = new ArrayList<>();
        areas.add(new BalanceComputationArea("FR", countryAreaFR, scalableFR, 1200.));
        areas.add(new BalanceComputationArea("BE", countryAreaBE, scalableBE, 1300.));

        BalanceComputation balanceComputation = balanceComputationFactory.create(areas, loadFlowRunner, computationManager);

        BalanceComputationResult result = balanceComputation.run(testNetwork1, testNetwork1.getVariantManager().getWorkingVariantId(), parameters).join();

        assertEquals(BalanceComputationResult.Status.SUCCESS, result.getStatus());
        assertEquals(2, result.getIterationCount());
    }
}
