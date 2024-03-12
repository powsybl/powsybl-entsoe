/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.balances_adjustment.balance_computation;

import com.powsybl.balances_adjustment.util.BalanceComputationAssert;
import com.powsybl.balances_adjustment.util.CountryAreaFactory;
import com.powsybl.commons.report.ReportNode;
import com.powsybl.computation.ComputationManager;
import com.powsybl.computation.local.LocalComputationManager;
import com.powsybl.iidm.modification.scalable.Scalable;
import com.powsybl.iidm.network.*;
import com.powsybl.loadflow.*;
import com.powsybl.math.matrix.DenseMatrixFactory;
import com.powsybl.openloadflow.OpenLoadFlowProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;

/**
 * @author Ameni Walha {@literal <ameni.walha at rte-france.com>}
 */
class BalanceComputationSimpleDcTest {
    private Network simpleNetwork;
    private ComputationManager computationManager;
    private CountryAreaFactory countryAreaFR;
    private CountryAreaFactory countryAreaBE;
    private Scalable scalableFR;
    private Scalable scalableBE;

    private BalanceComputationParameters parameters;
    private BalanceComputationFactory balanceComputationFactory;
    private LoadFlow.Runner loadFlowRunner;
    private Generator generatorFr;
    private Load loadFr;
    private Branch<?> branchFrBe1;
    private Branch<?> branchFrBe2;
    private final String initialState = "InitialState";
    private final String initialVariantNew = "InitialVariantNew";

    @BeforeEach
    void setUp() {
        simpleNetwork = Network.read("testSimpleNetwork.xiidm", getClass().getResourceAsStream("/testSimpleNetwork.xiidm"));

        countryAreaFR = new CountryAreaFactory(Country.FR);
        countryAreaBE = new CountryAreaFactory(Country.BE);

        computationManager = LocalComputationManager.getDefault();

        parameters = new BalanceComputationParameters();
        parameters.getLoadFlowParameters().setDc(true);
        parameters.getLoadFlowParameters().setWriteSlackBus(false);

        balanceComputationFactory = new BalanceComputationFactoryImpl();

        loadFlowRunner = new LoadFlow.Runner(new OpenLoadFlowProvider(new DenseMatrixFactory()));

        scalableFR = Scalable.proportional(Arrays.asList(60.0, 40.0),
                Arrays.asList(Scalable.onGenerator("GENERATOR_FR"), Scalable.onLoad("LOAD_FR")));

        scalableBE = Scalable.proportional(Arrays.asList(60.0, 40.0),
                Arrays.asList(Scalable.onGenerator("GENERATOR_BE"), Scalable.onLoad("LOAD_BE")));

        generatorFr = simpleNetwork.getGenerator("GENERATOR_FR");
        loadFr = simpleNetwork.getLoad("LOAD_FR");
        branchFrBe1 = simpleNetwork.getBranch("FRANCE_BELGIUM_1");
        branchFrBe2 = simpleNetwork.getBranch("FRANCE_BELGIUM_2");
    }

    @Test
    void testDivergentLoadFlowOnMainSynchronousComponent() {
        List<BalanceComputationArea> areas = new ArrayList<>();
        areas.add(new BalanceComputationArea("FR", countryAreaFR, scalableFR, 1200.));
        areas.add(new BalanceComputationArea("BE", countryAreaBE, scalableBE, -1200.));

        LoadFlow.Runner loadFlowRunnerMock = Mockito.mock(LoadFlow.Runner.class);

        BalanceComputationImpl balanceComputation = Mockito.spy(new BalanceComputationImpl(areas, computationManager, loadFlowRunnerMock));
        LoadFlowResult loadFlowResult = new LoadFlowResultImpl(true, Collections.emptyMap(), "logs",
                List.of(
                        new LoadFlowResultImpl.ComponentResultImpl(0, 0, LoadFlowResult.ComponentResult.Status.MAX_ITERATION_REACHED, 50, "dummy", 0.0, 0.0),
                        new LoadFlowResultImpl.ComponentResultImpl(0, 1, LoadFlowResult.ComponentResult.Status.CONVERGED, 5, "dummy", 0.0, 0.0)
                )
        );
        doReturn(loadFlowResult).when(loadFlowRunnerMock).run(Mockito.any(), Mockito.anyString(), Mockito.any(), Mockito.any(), Mockito.any());

        BalanceComputationResult result = balanceComputation.run(simpleNetwork, simpleNetwork.getVariantManager().getWorkingVariantId(), parameters).join();

        assertEquals(BalanceComputationResult.Status.FAILED, result.getStatus());
        assertEquals(0, result.getIterationCount());
    }

    @Test
    void testBalancedNetworkMockito() {
        List<BalanceComputationArea> areas = new ArrayList<>();
        areas.add(new BalanceComputationArea("FR", countryAreaFR, scalableFR, 1199.));
        areas.add(new BalanceComputationArea("BE", countryAreaBE, scalableBE, -1199.));

        LoadFlowProvider loadFlowProviderMock = new AbstractLoadFlowProviderMock() {

            @Override
            public CompletableFuture<LoadFlowResult> run(Network network, ComputationManager computationManager, String workingVariantId, LoadFlowParameters parameters, ReportNode reportNode) {
                generatorFr.getTerminal().setP(3000);
                loadFr.getTerminal().setP(1800);

                branchFrBe1.getTerminal1().setP(-516);
                branchFrBe1.getTerminal2().setP(516);

                branchFrBe2.getTerminal1().setP(-683);
                branchFrBe2.getTerminal2().setP(683);
                return CompletableFuture.completedFuture(new LoadFlowResultImpl(true, Collections.emptyMap(), null,
                        List.of(new LoadFlowResultImpl.ComponentResultImpl(0, 0, LoadFlowResult.ComponentResult.Status.CONVERGED, 5, "dummy", 0.0, 0.0)))
                );
            }
        };

        BalanceComputationImpl balanceComputation = new BalanceComputationImpl(areas, computationManager, new LoadFlow.Runner(loadFlowProviderMock));

        BalanceComputationResult result = balanceComputation.run(simpleNetwork, simpleNetwork.getVariantManager().getWorkingVariantId(), parameters).join();
        assertEquals(BalanceComputationResult.Status.SUCCESS, result.getStatus());
        assertEquals(1, result.getIterationCount());
        assertTrue(result.getBalancedScalingMap().values().stream().allMatch(v -> v == 0.));
    }

    @Test
    void testConvergedLoadFlowOnMainSynchronousComponent() {
        List<BalanceComputationArea> areas = new ArrayList<>();
        areas.add(new BalanceComputationArea("FR", countryAreaFR, scalableFR, 1199.));
        areas.add(new BalanceComputationArea("BE", countryAreaBE, scalableBE, -1199.));

        LoadFlowProvider loadFlowProviderMock = new AbstractLoadFlowProviderMock() {

            @Override
            public CompletableFuture<LoadFlowResult> run(Network network, ComputationManager computationManager, String workingVariantId, LoadFlowParameters parameters, ReportNode reportNode) {
                generatorFr.getTerminal().setP(3000);
                loadFr.getTerminal().setP(1800);

                branchFrBe1.getTerminal1().setP(-516);
                branchFrBe1.getTerminal2().setP(516);

                branchFrBe2.getTerminal1().setP(-683);
                branchFrBe2.getTerminal2().setP(683);
                return CompletableFuture.completedFuture(new LoadFlowResultImpl(true, Collections.emptyMap(), null,
                        List.of(
                                new LoadFlowResultImpl.ComponentResultImpl(0, 0, LoadFlowResult.ComponentResult.Status.CONVERGED, 5, "dummy", 0.0, 0.0),
                                new LoadFlowResultImpl.ComponentResultImpl(0, 1, LoadFlowResult.ComponentResult.Status.MAX_ITERATION_REACHED, 50, "dummy", 0.0, 0.0)
                        ))
                );
            }
        };

        BalanceComputationImpl balanceComputation = new BalanceComputationImpl(areas, computationManager, new LoadFlow.Runner(loadFlowProviderMock));

        BalanceComputationResult result = balanceComputation.run(simpleNetwork, simpleNetwork.getVariantManager().getWorkingVariantId(), parameters).join();
        assertEquals(BalanceComputationResult.Status.SUCCESS, result.getStatus());
        assertEquals(1, result.getIterationCount());
        assertTrue(result.getBalancedScalingMap().values().stream().allMatch(v -> v == 0.));
    }

    @Test
    void testUnBalancedNetworkMockito() {
        List<BalanceComputationArea> areas = new ArrayList<>();
        areas.add(new BalanceComputationArea("FR", countryAreaFR, scalableFR, 1300.));
        areas.add(new BalanceComputationArea("BE", countryAreaBE, scalableBE, -1400.));

        LoadFlowProvider loadFlowProviderMock = new AbstractLoadFlowProviderMock() {

            @Override
            public CompletableFuture<LoadFlowResult> run(Network network, ComputationManager computationManager, String workingVariantId, LoadFlowParameters parameters, ReportNode reportNode) {
                generatorFr.getTerminal().setP(3000);
                loadFr.getTerminal().setP(1800);
                branchFrBe1.getTerminal1().setP(-516);
                branchFrBe1.getTerminal2().setP(516);
                branchFrBe2.getTerminal1().setP(-683);
                return CompletableFuture.completedFuture(new LoadFlowResultImpl(true, Collections.emptyMap(), null,
                        List.of(new LoadFlowResultImpl.ComponentResultImpl(0, 0, LoadFlowResult.ComponentResult.Status.CONVERGED, 5, "dummy", 0.0, 0.0)))
                );
            }
        };

        BalanceComputationImpl balanceComputation = new BalanceComputationImpl(areas, computationManager, new LoadFlow.Runner(loadFlowProviderMock));
        BalanceComputationImpl balanceComputationSpy = Mockito.spy(balanceComputation);

        BalanceComputationResult result = balanceComputationSpy.run(simpleNetwork, simpleNetwork.getVariantManager().getWorkingVariantId(), parameters).join();

        assertEquals(BalanceComputationResult.Status.FAILED, result.getStatus());
        assertEquals(5, result.getIterationCount());

        assertEquals(initialState, simpleNetwork.getVariantManager().getWorkingVariantId());

    }

    @Test
    void testBalancedNetworkAfter1Scaling() {
        List<BalanceComputationArea> areas = new ArrayList<>();
        areas.add(new BalanceComputationArea("FR", countryAreaFR, scalableFR, 1300.));
        areas.add(new BalanceComputationArea("BE", countryAreaBE, scalableBE, -1300.));

        BalanceComputation balanceComputation = balanceComputationFactory.create(areas, loadFlowRunner, computationManager);

        BalanceComputationResult result = balanceComputation.run(simpleNetwork, simpleNetwork.getVariantManager().getWorkingVariantId(), parameters).join();

        assertEquals(BalanceComputationResult.Status.SUCCESS, result.getStatus());
        assertEquals(2, result.getIterationCount());
    }

    @Test
    void testUnBalancedNetwork() {
        List<BalanceComputationArea> areas = new ArrayList<>();
        areas.add(new BalanceComputationArea("FR", countryAreaFR, scalableFR, 1300.));
        areas.add(new BalanceComputationArea("BE", countryAreaBE, scalableBE, -1400.));

        BalanceComputation balanceComputation = balanceComputationFactory.create(areas, loadFlowRunner, computationManager);

        BalanceComputationResult result = balanceComputation.run(simpleNetwork, simpleNetwork.getVariantManager().getWorkingVariantId(), parameters).join();

        assertEquals(BalanceComputationResult.Status.FAILED, result.getStatus());
        assertEquals(5, result.getIterationCount());
        assertEquals(initialState, simpleNetwork.getVariantManager().getWorkingVariantId());

    }

    @Test
    void testDifferentStateId() {
        List<BalanceComputationArea> areas = new ArrayList<>();
        areas.add(new BalanceComputationArea("FR", countryAreaFR, scalableFR, 1300.));
        areas.add(new BalanceComputationArea("BE", countryAreaBE, scalableBE, -1300.));

        BalanceComputation balanceComputation = balanceComputationFactory.create(areas, loadFlowRunner, computationManager);

        String initialVariant = simpleNetwork.getVariantManager().getWorkingVariantId();
        simpleNetwork.getVariantManager().cloneVariant(initialVariant, initialVariantNew);
        BalanceComputationResult result = balanceComputation.run(simpleNetwork, initialVariantNew, parameters).join();

        assertEquals(BalanceComputationResult.Status.SUCCESS, result.getStatus());
        assertEquals(2, result.getIterationCount());
        assertEquals(initialState, simpleNetwork.getVariantManager().getWorkingVariantId());

        loadFlowRunner.run(simpleNetwork, initialVariantNew, computationManager, parameters.getLoadFlowParameters());
        assertEquals(1300, countryAreaFR.create(simpleNetwork).getNetPosition(), 0.0001);
        assertEquals(-1300, countryAreaBE.create(simpleNetwork).getNetPosition(), 0.0001);

        loadFlowRunner.run(simpleNetwork, initialState, computationManager, parameters.getLoadFlowParameters());
        assertEquals(1200, countryAreaFR.create(simpleNetwork).getNetPosition(), 0.0001);
        assertEquals(-1200, countryAreaBE.create(simpleNetwork).getNetPosition(), 0.0001);
    }

    @Test
    void testUnBalancedNetworkDifferentState() {
        List<BalanceComputationArea> areas = new ArrayList<>();
        areas.add(new BalanceComputationArea("FR", countryAreaFR, scalableFR, 1300.));
        areas.add(new BalanceComputationArea("BE", countryAreaBE, scalableBE, -1400.));

        BalanceComputation balanceComputation = balanceComputationFactory.create(areas, loadFlowRunner, computationManager);

        String initialVariant = simpleNetwork.getVariantManager().getWorkingVariantId();

        simpleNetwork.getVariantManager().cloneVariant(initialVariant, initialVariantNew);
        BalanceComputationResult result = balanceComputation.run(simpleNetwork, initialVariantNew, parameters).join();

        assertEquals(BalanceComputationResult.Status.FAILED, result.getStatus());
        assertEquals(5, result.getIterationCount());
        assertEquals(initialState, simpleNetwork.getVariantManager().getWorkingVariantId());

        loadFlowRunner.run(simpleNetwork, initialState, computationManager, parameters.getLoadFlowParameters());
        assertEquals(1200, countryAreaFR.create(simpleNetwork).getNetPosition(), 0.0001);
        assertEquals(-1200, countryAreaBE.create(simpleNetwork).getNetPosition(), 0.0001);

        loadFlowRunner.run(simpleNetwork, initialVariantNew, computationManager, new LoadFlowParameters());
        assertEquals(1200, countryAreaFR.create(simpleNetwork).getNetPosition(), 0.0001);
        assertEquals(-1200, countryAreaBE.create(simpleNetwork).getNetPosition(), 0.0001);

    }

    @Test
    void testBalancedNetworkAfter1ScalingReport() throws IOException {
        List<BalanceComputationArea> areas = new ArrayList<>();
        areas.add(new BalanceComputationArea("FR", countryAreaFR, scalableFR, 1300.));
        areas.add(new BalanceComputationArea("BE", countryAreaBE, scalableBE, -1300.));

        BalanceComputation balanceComputation = balanceComputationFactory.create(areas, loadFlowRunner, computationManager);

        ReportNode reportNode = ReportNode.newRootReportNode().withMessageTemplate("testBalancedNetworkReport", "Test balanced network report").build();
        balanceComputation.run(simpleNetwork, simpleNetwork.getVariantManager().getWorkingVariantId(), parameters, reportNode).join();
        BalanceComputationAssert.assertReportEquals("/balancedNetworkReport.txt", reportNode);
    }

    @Test
    void testUnBalancedNetworkReport() throws IOException {
        List<BalanceComputationArea> areas = new ArrayList<>();
        areas.add(new BalanceComputationArea("FR", countryAreaFR, scalableFR, 1300.));
        areas.add(new BalanceComputationArea("BE", countryAreaBE, scalableBE, -1400.));

        BalanceComputation balanceComputation = balanceComputationFactory.create(areas, loadFlowRunner, computationManager);

        ReportNode reportNode = ReportNode.newRootReportNode().withMessageTemplate("testUnbalancedNetworkReport", "Test unbalanced network report").build();
        balanceComputation.run(simpleNetwork, simpleNetwork.getVariantManager().getWorkingVariantId(), parameters, reportNode).join();
        BalanceComputationAssert.assertReportEquals("/unbalancedNetworkReport.txt", reportNode);
    }

    private static abstract class AbstractLoadFlowProviderMock extends AbstractNoSpecificParametersLoadFlowProvider {
        @Override
        public String getName() {
            return "test load flow";
        }

        @Override
        public String getVersion() {
            return "1.0";
        }

    }

}
