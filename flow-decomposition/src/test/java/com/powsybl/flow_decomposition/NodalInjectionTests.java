/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.flow_decomposition;

import com.powsybl.flow_decomposition.glsk_provider.AutoGlskProvider;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Identifiable;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.loadflow.LoadFlowResult;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.powsybl.flow_decomposition.TestUtils.importNetwork;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 */
class NodalInjectionTests {
    public static final String ALLOCATED = "Allocated Flow";
    private static final double EPSILON = 1e-3;

    @Test
    void testThatNodalInjectionsAreWellComputed() {
        String networkFileName = "NETWORK_SINGLE_LOAD_TWO_GENERATORS_WITH_COUNTRIES.uct";
        String genBe = "BGEN2 11_generator";
        String genFr = "FGEN1 11_generator";
        Map<String, Map<String, Double>> nodalInjections = getNodalInjections(networkFileName);
        assertEquals(-100.0935, nodalInjections.get(genBe).get(ALLOCATED), EPSILON);
        assertEquals(+100.0935, nodalInjections.get(genFr).get(ALLOCATED), EPSILON);
    }

    @Test
    void testThatNodalInjectionsAreWellComputedOnInvertedNetwork() {
        String networkFileName = "NETWORK_SINGLE_LOAD_TWO_GENERATORS_WITH_COUNTRIES_INVERTED.uct";
        String genBe = "BGEN2 11_generator";
        String genFr = "FGEN1 11_generator";
        Map<String, Map<String, Double>> nodalInjections = getNodalInjections(networkFileName);
        assertEquals(-100.0935, nodalInjections.get(genBe).get(ALLOCATED), EPSILON);
        assertEquals(+100.0935, nodalInjections.get(genFr).get(ALLOCATED), EPSILON);
    }

    @Test
    void testThatNodalInjectionsAreWellComputedOnLoopFlowNetwork() {
        String networkFileName = "NETWORK_LOOP_FLOW_WITH_COUNTRIES.uct";
        String gBe = "BGEN  11_generator";
        String lBe = "BLOAD 11_load";
        String gFr = "FGEN  11_generator";
        String lFr = "FLOAD 11_load";
        String gEs = "EGEN  11_generator";
        String lEs = "ELOAD 11_load";
        Map<String, Map<String, Double>> nodalInjections = getNodalInjections(networkFileName);
        assertEquals(0, nodalInjections.get(gBe).get(ALLOCATED), EPSILON);
        assertEquals(0, nodalInjections.get(gEs).get(ALLOCATED), EPSILON);
        assertEquals(0, nodalInjections.get(gFr).get(ALLOCATED), EPSILON);
        assertEquals(0, nodalInjections.get(lBe).get(ALLOCATED), EPSILON);
        assertEquals(0, nodalInjections.get(lEs).get(ALLOCATED), EPSILON);
        assertEquals(0, nodalInjections.get(lFr).get(ALLOCATED), EPSILON);
        assertEquals(100., nodalInjections.get(gBe).get(NetworkUtil.getLoopFlowIdFromCountry(Country.BE)), EPSILON);
        assertEquals(100., nodalInjections.get(gEs).get(NetworkUtil.getLoopFlowIdFromCountry(Country.ES)), EPSILON);
        assertEquals(100., nodalInjections.get(gFr).get(NetworkUtil.getLoopFlowIdFromCountry(Country.FR)), EPSILON);
        assertEquals(-100, nodalInjections.get(lBe).get(NetworkUtil.getLoopFlowIdFromCountry(Country.BE)), EPSILON);
        assertEquals(-100, nodalInjections.get(lEs).get(NetworkUtil.getLoopFlowIdFromCountry(Country.ES)), EPSILON);
        assertEquals(-100, nodalInjections.get(lFr).get(NetworkUtil.getLoopFlowIdFromCountry(Country.FR)), EPSILON);
    }

    @Test
    void testThatReferenceNodalInjectionsAreWellComputedOnLoopFlowNetwork() {
        String networkFileName = "NETWORK_LOOP_FLOW_WITH_COUNTRIES.uct";
        String gBe = "BGEN  11_generator";
        String lBe = "BLOAD 11_load";
        String gFr = "FGEN  11_generator";
        String lFr = "FLOAD 11_load";
        String gEs = "EGEN  11_generator";
        String lEs = "ELOAD 11_load";
        Map<String, Double> referenceNodalInjections = getReferenceNodalInjections(networkFileName);
        assertEquals(100, referenceNodalInjections.get(gBe));
        assertEquals(100, referenceNodalInjections.get(gEs));
        assertEquals(100, referenceNodalInjections.get(gFr));
        assertEquals(-100, referenceNodalInjections.get(lBe));
        assertEquals(-100, referenceNodalInjections.get(lEs));
        assertEquals(-100, referenceNodalInjections.get(lFr));
    }

    private static Map<String, Map<String, Double>> getNodalInjections(String networkFileName) {
        Network network = importNetwork(networkFileName);
        LoadFlowResult loadFlowResult = LoadFlow.run(network);
        if (!loadFlowResult.isFullyConverged()) {
            LoadFlow.run(network, LoadFlowParameters.load().setDc(true));
        }
        AutoGlskProvider glskProvider = new AutoGlskProvider();
        Map<Country, Map<String, Double>> glsks = glskProvider.getGlsk(network);
        Map<Country, Double> netPositions = NetPositionComputer.computeNetPositions(network);
        List<Identifiable<?>> xnecList = network.getBranchStream()
                .map(e -> (Identifiable<?>) e)
                .collect(Collectors.toList());
        NetworkMatrixIndexes networkMatrixIndexes = new NetworkMatrixIndexes(network, xnecList);
        NodalInjectionComputer nodalInjectionComputer = new NodalInjectionComputer(networkMatrixIndexes);
        SparseMatrixWithIndexesTriplet nodalInjectionsMatrix = nodalInjectionComputer.run(network,
            glsks, netPositions);
        return nodalInjectionsMatrix.toMap();
    }

    private static Map<String, Double> getReferenceNodalInjections(String networkFileName) {
        Network network = importNetwork(networkFileName);
        LoadFlowResult loadFlowResult = LoadFlow.run(network);
        if (!loadFlowResult.isFullyConverged()) {
            LoadFlow.run(network, LoadFlowParameters.load().setDc(true));
        }
        List<Identifiable<?>> xnecList = network.getBranchStream()
                .map(e -> (Identifiable<?>) e)
                .collect(Collectors.toList());
        NetworkMatrixIndexes networkMatrixIndexes = new NetworkMatrixIndexes(network, xnecList);
        ReferenceNodalInjectionComputer referenceNodalInjectionComputer = new ReferenceNodalInjectionComputer();
        return referenceNodalInjectionComputer.run(networkMatrixIndexes.getNodeList());
    }
}
