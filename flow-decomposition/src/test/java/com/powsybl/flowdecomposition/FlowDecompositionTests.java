/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.flowdecomposition;

import com.powsybl.commons.PowsyblException;
import com.powsybl.contingency.ContingencyContext;
import com.powsybl.flowdecomposition.xnecprovider.XnecProviderAllBranches;
import com.powsybl.flowdecomposition.xnecprovider.XnecProviderByIds;
import com.powsybl.flowdecomposition.xnecprovider.XnecProviderUnion;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Identifiable;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.sensitivity.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.powsybl.flowdecomposition.AbstractSensitivityAnalyser.CONTINGENCIES;
import static com.powsybl.flowdecomposition.AbstractSensitivityAnalyser.SENSITIVITY_FUNCTION_TYPE;
import static com.powsybl.flowdecomposition.DecomposedFlowAssert.assertThat;
import static com.powsybl.flowdecomposition.partitioners.SensitivityAnalyser.EMPTY_SENSITIVITY_VARIABLE_SETS;
import static java.lang.Double.NaN;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 */
class FlowDecompositionTests {

    public static final Set<FlowDecompositionParameters.FlowPartitionMode> FULL_LINE_DECOMPOSITION_MODES = Set.of(FlowDecompositionParameters.FlowPartitionMode.FULL_LINE_DECOMPOSITION,
        FlowDecompositionParameters.FlowPartitionMode.FAST_FULL_LINE_DECOMPOSITION);

    private static FlowDecompositionResults runFlowDecomposition(Network network, XnecProvider xnecProvider, FlowDecompositionParameters.FlowPartitionMode flowPartitionMode) {
        FlowDecompositionParameters flowDecompositionParameters = new FlowDecompositionParameters()
            .setEnableLossesCompensation(FlowDecompositionParameters.ENABLE_LOSSES_COMPENSATION)
            .setLossesCompensationEpsilon(FlowDecompositionParameters.DISABLE_LOSSES_COMPENSATION_EPSILON)
            .setSensitivityEpsilon(FlowDecompositionParameters.DISABLE_SENSITIVITY_EPSILON)
            .setRescaleMode(FlowDecompositionParameters.RescaleMode.NONE)
            .setFlowPartitioner(flowPartitionMode);
        FlowDecompositionComputer flowDecompositionComputer = new FlowDecompositionComputer(flowDecompositionParameters, LoadFlowParameters.load());
        FlowDecompositionResults flowDecompositionResults = flowDecompositionComputer.run(xnecProvider, network);
        TestUtils.assertNoInternalFlowOnTieLines(flowDecompositionResults);
        TestUtils.assertCoherenceTotalFlow(flowDecompositionParameters.getRescaleMode(), flowDecompositionResults);
        return flowDecompositionResults;
    }

    private static FlowDecompositionResults testFlowDecompositionOnHvdcNetwork(FlowDecompositionParameters.FlowPartitionMode flowPartitionMode) {
        Network network = TestUtils.importNetwork("TestCase16NodesWithHvdc.xiidm");

        Set<String> branchIds = network.getBranchStream().map(Identifiable::getId).collect(Collectors.toSet());
        XnecProvider xnecProviderContingency = XnecProviderByIds.builder()
            .addContingency("contingency_1", Set.of("DDE2AA11 NNL3AA11 1"))
            .addContingency("contingency_desync", Set.of("DDE2AA11 NNL3AA11 1", "FFR3AA11 FFR5AA11 1"))
            .addContingency("contingency_split_network", Set.of("DDE2AA11 NNL3AA11 1", "FFR3AA11 FFR5AA11 1", "NNL2AA11 BBE3AA11 1"))
            .addNetworkElementsAfterContingencies(branchIds, Set.of("contingency_1", "contingency_desync", "contingency_split_network"))
            .build();
        XnecProvider xnecProvider = new XnecProviderUnion(List.of(new XnecProviderAllBranches(), xnecProviderContingency));
        FlowDecompositionResults flowDecompositionResults = runFlowDecomposition(network, xnecProvider, flowPartitionMode);
        assertEquals(98, flowDecompositionResults.getDecomposedFlowMap().size());
        return flowDecompositionResults;
    }

    private static void validateFlowDecompositionResultsUsingPFCMethodology(FlowDecompositionResults flowDecompositionResults) throws IOException {
        InputStream inputStream = FlowDecompositionTests.class.getResourceAsStream("validateFlowDecompositionResultsUsingPFCMethodology.csv");
        FlowDecompositionAssertionsImporter.importFromResource(inputStream, "validateFlowDecompositionResultsUsingPFCMethodology.csv")
            .forEach(assertion -> assertion.validate(flowDecompositionResults));
        assertEquals(4, flowDecompositionResults.getZoneSet().size());
        assertTrue(flowDecompositionResults.getZoneSet().contains(Country.BE));
        assertTrue(flowDecompositionResults.getZoneSet().contains(Country.DE));
        assertTrue(flowDecompositionResults.getZoneSet().contains(Country.FR));
        assertTrue(flowDecompositionResults.getZoneSet().contains(Country.NL));
    }

    private static void validateFlowDecompositionResultsUsingFLDMethodology(FlowDecompositionResults flowDecompositionResults) throws IOException {
        InputStream inputStream = FlowDecompositionTests.class.getResourceAsStream("validateFlowDecompositionResultsUsingFLDMethodology.csv");
        FlowDecompositionAssertionsImporter.importFromResource(inputStream, "validateFlowDecompositionResultsUsingFLDMethodology.csv")
            .forEach(assertion -> assertion.validate(flowDecompositionResults));
        assertEquals(4, flowDecompositionResults.getZoneSet().size());
        assertTrue(flowDecompositionResults.getZoneSet().contains(Country.BE));
        assertTrue(flowDecompositionResults.getZoneSet().contains(Country.DE));
        assertTrue(flowDecompositionResults.getZoneSet().contains(Country.FR));
        assertTrue(flowDecompositionResults.getZoneSet().contains(Country.NL));
    }

    private static void validateFlowDecompositionResultsWithBusBarSectionOnly(FlowDecompositionResults flowDecompositionResults) {
        assertEquals(6, flowDecompositionResults.getDecomposedFlowMap().size());
        assertThat(flowDecompositionResults)
            .branchId("a708c3bc-465d-4fe7-b6ef-6fa6408a62b0")
            .country1(Country.BE)
            .country2(Country.BE)
            .acReferenceFlow(105.335)
            .dcReferenceFlow(115.129)
            .allocatedFlow(-8.896)
            .xNodeFlow(30.705)
            .pstFlow(33.030)
            .internalFlow(60.289)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("b58bf21a-096a-4dae-9a01-3f03b60c24c7")
            .country1(Country.BE)
            .country2(Country.BE)
            .acReferenceFlow(-116.324)
            .dcReferenceFlow(-118.550)
            .xNodeFlow(126.160)
            .internalFlow(-7.610)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("b94318f6-6d24-4f56-96b9-df2531ad6543")
            .country1(Country.BE)
            .country2(Country.BE)
            .acReferenceFlow(0.299)
            .dcReferenceFlow(0.289)
            .allocatedFlow(-5.505)
            .xNodeFlow(76.274)
            .pstFlow(-33.030)
            .internalFlow(-37.450)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("df16b3dd-c905-4a6f-84ee-f067be86f5da")
            .country1(Country.BE)
            .country2(Country.BE)
            .acReferenceFlow(-99.797)
            .dcReferenceFlow(-103.741)
            .xNodeFlow(103.741)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("e482b89a-fa84-4ea9-8e70-a83d44790957")
            .country1(Country.BE)
            .country2(Country.BE)
            .acReferenceFlow(-94.902)
            .dcReferenceFlow(-84.583)
            .allocatedFlow(14.401)
            .xNodeFlow(-106.980)
            .internalFlow(177.161)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("ffbabc27-1ccd-4fdc-b037-e341706c8d29")
            .country1(Country.BE)
            .country2(Country.BE)
            .acReferenceFlow(-53.551)
            .dcReferenceFlow(-57.104)
            .xNodeFlow(60.770)
            .internalFlow(-3.666)
            .validate();
        assertEquals(1, flowDecompositionResults.getZoneSet().size());
        assertTrue(flowDecompositionResults.getZoneSet().contains(Country.BE));
    }

    private static void validateFlowDecompositionResultsWithShuntCompensatorOnly(FlowDecompositionResults flowDecompositionResults) {
        assertEquals(6, flowDecompositionResults.getDecomposedFlowMap().size());
        assertThat(flowDecompositionResults)
            .branchId("a708c3bc-465d-4fe7-b6ef-6fa6408a62b0")
            .country1(Country.BE)
            .country2(Country.BE)
            .acReferenceFlow(105.189)
            .dcReferenceFlow(115.160)
            .allocatedFlow(-8.896)
            .xNodeFlow(30.705)
            .pstFlow(33.030)
            .internalFlow(60.321)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("b58bf21a-096a-4dae-9a01-3f03b60c24c7")
            .country1(Country.BE)
            .country2(Country.BE)
            .acReferenceFlow(-116.310)
            .dcReferenceFlow(-118.537)
            .xNodeFlow(126.160)
            .internalFlow(-7.623)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("b94318f6-6d24-4f56-96b9-df2531ad6543")
            .country1(Country.BE)
            .country2(Country.BE)
            .acReferenceFlow(-0.732)
            .dcReferenceFlow(0.248)
            .allocatedFlow(-5.505)
            .xNodeFlow(76.274)
            .pstFlow(-33.030)
            .internalFlow(-37.492)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("df16b3dd-c905-4a6f-84ee-f067be86f5da")
            .country1(Country.BE)
            .country2(Country.BE)
            .acReferenceFlow(-99.793)
            .dcReferenceFlow(-103.741)
            .xNodeFlow(103.741)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("e482b89a-fa84-4ea9-8e70-a83d44790957")
            .country1(Country.BE)
            .country2(Country.BE)
            .acReferenceFlow(-95.912)
            .dcReferenceFlow(-84.604)
            .allocatedFlow(14.401)
            .xNodeFlow(-106.980)
            .internalFlow(177.183)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("ffbabc27-1ccd-4fdc-b037-e341706c8d29")
            .country1(Country.BE)
            .country2(Country.BE)
            .acReferenceFlow(-53.540)
            .dcReferenceFlow(-57.098)
            .xNodeFlow(60.770)
            .internalFlow(-3.672)
            .validate();
        assertEquals(1, flowDecompositionResults.getZoneSet().size());
        assertTrue(flowDecompositionResults.getZoneSet().contains(Country.BE));
    }

    private static void validateFlowDecompositionResultsWith6Nodes(FlowDecompositionParameters.FlowPartitionMode flowPartitionMode, FlowDecompositionResults flowDecompositionResults) {
        if (FULL_LINE_DECOMPOSITION_MODES.contains(flowPartitionMode)) {
            assertThat(flowDecompositionResults)
                .branchId("BGEN  11 BLOAD 11 1")
                .country1(Country.BE)
                .country2(Country.BE)
                .acReferenceFlow(NaN)
                .dcReferenceFlow(300.000)
                .allocatedFlow(200.000)
                .internalFlow(33.333)
                .loopFlow(Country.FR, 33.333)
                .loopFlow(Country.ES, 33.333)
                .validate();
            assertThat(flowDecompositionResults)
                .branchId("BLOAD 11 FLOAD 11 1")
                .country1(Country.BE)
                .country2(Country.FR)
                .acReferenceFlow(NaN)
                .dcReferenceFlow(200.000)
                .allocatedFlow(133.333)
                .loopFlow(Country.FR, 33.333)
                .loopFlow(Country.ES, 33.333)
                .validate();
            assertThat(flowDecompositionResults)
                .branchId("EGEN  11 FGEN  11 1")
                .country1(Country.ES)
                .country2(Country.FR)
                .acReferenceFlow(NaN)
                .dcReferenceFlow(100.000)
                .allocatedFlow(66.667)
                .loopFlow(Country.ES, 33.333)
                .validate();
            assertThat(flowDecompositionResults)
                .branchId("FGEN  11 BGEN  11 1")
                .country1(Country.FR)
                .country2(Country.BE)
                .acReferenceFlow(NaN)
                .dcReferenceFlow(200.000)
                .allocatedFlow(133.333)
                .loopFlow(Country.FR, 33.333)
                .loopFlow(Country.ES, 33.333)
                .validate();
            assertThat(flowDecompositionResults)
                .branchId("FLOAD 11 ELOAD 11 1")
                .country1(Country.FR)
                .country2(Country.ES)
                .acReferenceFlow(NaN)
                .dcReferenceFlow(100.000)
                .allocatedFlow(66.667)
                .loopFlow(Country.ES, 33.333)
                .validate();
        } else {
            assertThat(flowDecompositionResults)
                .branchId("BGEN  11 BLOAD 11 1")
                .country1(Country.BE)
                .country2(Country.BE)
                .acReferenceFlow(NaN)
                .dcReferenceFlow(300.000)
                .internalFlow(100.000)
                .loopFlow(Country.ES, 100.000)
                .loopFlow(Country.FR, 100.000)
                .validate();
            assertThat(flowDecompositionResults)
                .branchId("BLOAD 11 FLOAD 11 1")
                .country1(Country.BE)
                .country2(Country.FR)
                .acReferenceFlow(NaN)
                .dcReferenceFlow(200.000)
                .loopFlow(Country.ES, 100.000)
                .loopFlow(Country.FR, 100.000)
                .validate();
            assertThat(flowDecompositionResults)
                .branchId("EGEN  11 FGEN  11 1")
                .country1(Country.ES)
                .country2(Country.FR)
                .acReferenceFlow(NaN)
                .dcReferenceFlow(100.000)
                .loopFlow(Country.ES, 100.000)
                .validate();
            assertThat(flowDecompositionResults)
                .branchId("FGEN  11 BGEN  11 1")
                .country1(Country.FR)
                .country2(Country.BE)
                .acReferenceFlow(NaN)
                .dcReferenceFlow(200.000)
                .loopFlow(Country.ES, 100.000)
                .loopFlow(Country.FR, 100.000)
                .validate();
            assertThat(flowDecompositionResults)
                .branchId("FLOAD 11 ELOAD 11 1")
                .country1(Country.FR)
                .country2(Country.ES)
                .acReferenceFlow(NaN)
                .dcReferenceFlow(100.000)
                .loopFlow(Country.ES, 100.000)
                .validate();
        }

        assertEquals(3, flowDecompositionResults.getZoneSet().size());
        assertTrue(flowDecompositionResults.getZoneSet().contains(Country.BE));
        assertTrue(flowDecompositionResults.getZoneSet().contains(Country.ES));
        assertTrue(flowDecompositionResults.getZoneSet().contains(Country.FR));
    }

    private static void validateFlowDecompositionResultsWith6NodesConnected(FlowDecompositionResults flowDecompositionResults) {
        assertEquals(7, flowDecompositionResults.getDecomposedFlowMap().size());

        assertThat(flowDecompositionResults)
            .branchId("BGEN  11 BLOAD 11 1")
            .country1(Country.BE)
            .country2(Country.BE)
            .acReferenceFlow(100.063)
            .dcReferenceFlow(100.000)
            .internalFlow(73.333)
            .loopFlow(Country.FR, 20.000)
            .loopFlow(Country.ES, 6.667)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("BLOAD 11 FLOAD 11 1")
            .country1(Country.BE)
            .country2(Country.FR)
            .loopFlow(Country.BE, -26.667)
            .loopFlow(Country.FR, 20.000)
            .loopFlow(Country.ES, 6.667)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("EGEN  11 ELOAD 11 1")
            .country1(Country.ES)
            .country2(Country.ES)
            .acReferenceFlow(100.063)
            .dcReferenceFlow(100.000)
            .internalFlow(73.333)
            .loopFlow(Country.BE, 6.667)
            .loopFlow(Country.FR, 20.000)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("EGEN  11 FGEN  11 1")
            .country1(Country.ES)
            .country2(Country.FR)
            .loopFlow(Country.BE, 6.667)
            .loopFlow(Country.FR, 20.000)
            .loopFlow(Country.ES, -26.667)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("FGEN  11 BGEN  11 1")
            .country1(Country.FR)
            .country2(Country.BE)
            .loopFlow(Country.BE, 26.667)
            .loopFlow(Country.FR, -20.000)
            .loopFlow(Country.ES, -6.667)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("FGEN  11 FLOAD 11 1")
            .country1(Country.FR)
            .country2(Country.FR)
            .acReferenceFlow(100.063)
            .dcReferenceFlow(100.000)
            .internalFlow(60.000)
            .loopFlow(Country.BE, 20.000)
            .loopFlow(Country.ES, 20.000)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("FLOAD 11 ELOAD 11 1")
            .country1(Country.FR)
            .country2(Country.ES)
            .loopFlow(Country.BE, -6.667)
            .loopFlow(Country.FR, -20.000)
            .loopFlow(Country.ES, 26.667)
            .validate();

        assertEquals(3, flowDecompositionResults.getZoneSet().size());
        assertTrue(flowDecompositionResults.getZoneSet().contains(Country.BE));
        assertTrue(flowDecompositionResults.getZoneSet().contains(Country.ES));
        assertTrue(flowDecompositionResults.getZoneSet().contains(Country.FR));
    }

    private static void validateFlowDecompositionResultsWith6NodesConnectedAsymmetrical(FlowDecompositionResults flowDecompositionResults,
                                                                                        FlowDecompositionParameters.FlowPartitionMode flowPartitionMode) {
        assertEquals(7, flowDecompositionResults.getDecomposedFlowMap().size());

        if (FULL_LINE_DECOMPOSITION_MODES.contains(flowPartitionMode)) {
            assertThat(flowDecompositionResults)
                .branchId("BGEN  11 BLOAD 11 1")
                .country1(Country.BE)
                .country2(Country.BE)
                .acReferenceFlow(110.820)
                .dcReferenceFlow(110.737)
                .allocatedFlow(15.152)
                .internalFlow(67.348)
                .loopFlow(Country.FR, 13.583)
                .loopFlow(Country.ES, 14.654)
                .validate();
            assertThat(flowDecompositionResults)
                .branchId("BLOAD 11 FLOAD 11 1")
                .country1(Country.BE)
                .country2(Country.FR)
                .acReferenceFlow(10.589)
                .dcReferenceFlow(10.581)
                .allocatedFlow(6.834)
                .loopFlow(Country.BE, -24.490)
                .loopFlow(Country.FR, 13.583)
                .loopFlow(Country.ES, 14.654)
                .validate();
            assertThat(flowDecompositionResults)
                .branchId("EGEN  11 ELOAD 11 1")
                .country1(Country.ES)
                .country2(Country.ES)
                .acReferenceFlow(210.652)
                .dcReferenceFlow(209.439)
                .allocatedFlow(28.539)
                .internalFlow(161.195)
                .loopFlow(Country.BE, 6.123)
                .loopFlow(Country.FR, 13.583)
                .validate();
            assertThat(flowDecompositionResults)
                .branchId("EGEN  11 FGEN  11 1")
                .country1(Country.ES)
                .country2(Country.FR)
                .acReferenceFlow(41.120)
                .dcReferenceFlow(38.364)
                .allocatedFlow(-0.547)
                .loopFlow(Country.BE, -6.123)
                .loopFlow(Country.FR, -13.583)
                .loopFlow(Country.ES, 58.616)
                .validate();
            assertThat(flowDecompositionResults)
                .branchId("FGEN  11 BGEN  11 1")
                .country1(Country.FR)
                .country2(Country.BE)
                .acReferenceFlow(9.258)
                .dcReferenceFlow(9.196)
                .allocatedFlow(5.450)
                .loopFlow(Country.BE, -24.490)
                .loopFlow(Country.FR, 13.583)
                .loopFlow(Country.ES, 14.654)
                .validate();
            assertThat(flowDecompositionResults)
                .branchId("FGEN  11 FLOAD 11 1")
                .country1(Country.FR)
                .country2(Country.FR)
                .acReferenceFlow(130.463)
                .dcReferenceFlow(130.514)
                .allocatedFlow(27.436)
                .internalFlow(40.748)
                .loopFlow(Country.BE, 18.368)
                .loopFlow(Country.ES, 43.962)
                .validate();
            assertThat(flowDecompositionResults)
                .branchId("FLOAD 11 ELOAD 11 1")
                .country1(Country.FR)
                .country2(Country.ES)
                .acReferenceFlow(40.681)
                .dcReferenceFlow(40.561)
                .allocatedFlow(1.650)
                .loopFlow(Country.BE, -6.123)
                .loopFlow(Country.FR, -13.583)
                .loopFlow(Country.ES, 58.616)
                .validate();
        } else {
            assertThat(flowDecompositionResults)
                .branchId("BGEN  11 BLOAD 11 1")
                .country1(Country.BE)
                .country2(Country.BE)
                .acReferenceFlow(110.820)
                .dcReferenceFlow(110.737)
                .allocatedFlow(0.421)
                .internalFlow(73.453)
                .loopFlow(Country.FR, 20.021)
                .loopFlow(Country.ES, 16.842)
                .validate();
            assertThat(flowDecompositionResults)
                .branchId("BLOAD 11 FLOAD 11 1")
                .country1(Country.BE)
                .country2(Country.FR)
                .acReferenceFlow(10.589)
                .dcReferenceFlow(10.581)
                .allocatedFlow(0.421)
                .loopFlow(Country.BE, -26.703)
                .loopFlow(Country.FR, 20.021)
                .loopFlow(Country.ES, 16.842)
                .validate();
            assertThat(flowDecompositionResults)
                .branchId("EGEN  11 ELOAD 11 1")
                .country1(Country.ES)
                .country2(Country.ES)
                .acReferenceFlow(210.652)
                .dcReferenceFlow(209.439)
                .allocatedFlow(-0.325)
                .internalFlow(183.070)
                .loopFlow(Country.BE, 6.673)
                .loopFlow(Country.FR, 20.021)
                .validate();
            assertThat(flowDecompositionResults)
                .branchId("EGEN  11 FGEN  11 1")
                .country1(Country.ES)
                .country2(Country.FR)
                .acReferenceFlow(41.120)
                .dcReferenceFlow(38.364)
                .allocatedFlow(-0.554)
                .loopFlow(Country.BE, -6.682)
                .loopFlow(Country.FR, -20.451)
                .loopFlow(Country.ES, 66.052)
                .validate();
            assertThat(flowDecompositionResults)
                .branchId("FGEN  11 BGEN  11 1")
                .country1(Country.FR)
                .country2(Country.BE)
                .acReferenceFlow(9.258)
                .dcReferenceFlow(9.196)
                .allocatedFlow(-0.937)
                .loopFlow(Country.BE, -26.721)
                .loopFlow(Country.FR, 20.451)
                .loopFlow(Country.ES, 16.403)
                .validate();
            assertThat(flowDecompositionResults)
                .branchId("FGEN  11 FLOAD 11 1")
                .country1(Country.FR)
                .country2(Country.FR)
                .acReferenceFlow(130.463)
                .dcReferenceFlow(130.514)
                .allocatedFlow(-0.096)
                .internalFlow(60.492)
                .loopFlow(Country.BE, 20.029)
                .loopFlow(Country.ES, 50.088)
                .validate();
            assertThat(flowDecompositionResults)
                .branchId("FLOAD 11 ELOAD 11 1")
                .country1(Country.FR)
                .country2(Country.ES)
                .acReferenceFlow(40.681)
                .dcReferenceFlow(40.561)
                .allocatedFlow(0.325)
                .loopFlow(Country.BE, -6.673)
                .loopFlow(Country.FR, -20.021)
                .loopFlow(Country.ES, 66.930)
                .validate();
        }

        assertEquals(3, flowDecompositionResults.getZoneSet().size());
        assertTrue(flowDecompositionResults.getZoneSet().contains(Country.BE));
        assertTrue(flowDecompositionResults.getZoneSet().contains(Country.ES));
        assertTrue(flowDecompositionResults.getZoneSet().contains(Country.FR));
    }

    private static void validateFlowDecompositionResultsWithPst(FlowDecompositionResults flowDecompositionResults, FlowDecompositionParameters.FlowPartitionMode flowPartitionMode) {
        assertEquals(3, flowDecompositionResults.getDecomposedFlowMap().size());

        if (FULL_LINE_DECOMPOSITION_MODES.contains(flowPartitionMode)) {
            assertThat(flowDecompositionResults)
                .branchId("BLOAD 11 BLOAD 12 2")
                .country1(Country.BE)
                .country2(Country.BE)
                .acReferenceFlow(-160.006)
                .dcReferenceFlow(-168.543)
                .allocatedFlow(4.890)
                .pstFlow(163.653)
                .validate();
            assertThat(flowDecompositionResults)
                .branchId("FGEN  11 BLOAD 11 1")
                .country1(Country.FR)
                .country2(Country.BE)
                .acReferenceFlow(192.391)
                .dcReferenceFlow(200.671)
                .allocatedFlow(37.019)
                .pstFlow(163.653)
                .validate();
            assertThat(flowDecompositionResults)
                .branchId("FGEN  11 BLOAD 12 1")
                .country1(Country.FR)
                .country2(Country.BE)
                .acReferenceFlow(-76.188)
                .dcReferenceFlow(-84.725)
                .allocatedFlow(-78.927)
                .pstFlow(163.653)
                .validate();
        } else {
            assertThat(flowDecompositionResults)
                .branchId("BLOAD 11 BLOAD 12 2")
                .country1(Country.BE)
                .country2(Country.BE)
                .acReferenceFlow(-160.006)
                .dcReferenceFlow(-168.543)
                .allocatedFlow(29.016)
                .pstFlow(163.653)
                .internalFlow(-24.111)
                .loopFlow(Country.FR, -0.015)
                .validate();
            assertThat(flowDecompositionResults)
                .branchId("FGEN  11 BLOAD 11 1")
                .country1(Country.FR)
                .country2(Country.BE)
                .acReferenceFlow(192.391)
                .dcReferenceFlow(200.671)
                .allocatedFlow(29.016)
                .pstFlow(163.653)
                .loopFlow(Country.BE, 8.017)
                .loopFlow(Country.FR, -0.015)
                .validate();
            assertThat(flowDecompositionResults)
                .branchId("FGEN  11 BLOAD 12 1")
                .country1(Country.FR)
                .country2(Country.BE)
                .acReferenceFlow(-76.188)
                .dcReferenceFlow(-84.725)
                .allocatedFlow(-87.048)
                .pstFlow(163.653)
                .loopFlow(Country.BE, 8.076)
                .loopFlow(Country.FR, 0.044)
                .validate();
        }
        assertEquals(2, flowDecompositionResults.getZoneSet().size());
        assertTrue(flowDecompositionResults.getZoneSet().contains(Country.BE));
        assertTrue(flowDecompositionResults.getZoneSet().contains(Country.FR));
    }

    private static void validateFlowDecompositionResultsWithSubStation(FlowDecompositionResults flowDecompositionResults, FlowDecompositionParameters.FlowPartitionMode flowPartitionMode) {
        assertEquals(3, flowDecompositionResults.getDecomposedFlowMap().size());

        if (FULL_LINE_DECOMPOSITION_MODES.contains(flowPartitionMode)) {
            assertThat(flowDecompositionResults)
                .branchId("BLOAD 11 BGEN2 11 1")
                .country1(Country.BE)
                .country2(Country.BE)
                .acReferenceFlow(-101.421)
                .dcReferenceFlow(-101.420)
                .internalFlow(101.420)
                .validate();
            assertThat(flowDecompositionResults)
                .branchId("FGEN1 11 FLOAD 11 1")
                .country1(Country.FR)
                .country2(Country.FR)
                .acReferenceFlow(104.268)
                .dcReferenceFlow(101.424)
                .allocatedFlow(98.580)
                .internalFlow(2.844)
                .validate();
            assertThat(flowDecompositionResults)
                .branchId("FLOAD 11 BLOAD 11 1")
                .country1(Country.FR)
                .country2(Country.BE)
                .acReferenceFlow(101.424)
                .dcReferenceFlow(98.580)
                .allocatedFlow(98.580)
                .validate();
        } else {
            assertThat(flowDecompositionResults)
                .branchId("BLOAD 11 BGEN2 11 1")
                .country1(Country.BE)
                .country2(Country.BE)
                .acReferenceFlow(-101.421)
                .dcReferenceFlow(-101.420)
                .allocatedFlow(-100.001)
                .internalFlow(200.711)
                .loopFlow(Country.FR, 0.711)
                .validate();
            assertThat(flowDecompositionResults)
                .branchId("FGEN1 11 FLOAD 11 1")
                .country1(Country.FR)
                .country2(Country.FR)
                .acReferenceFlow(104.268)
                .dcReferenceFlow(101.424)
                .allocatedFlow(100.001)
                .internalFlow(2.133)
                .loopFlow(Country.BE, -0.711)
                .validate();
            assertThat(flowDecompositionResults)
                .branchId("FLOAD 11 BLOAD 11 1")
                .country1(Country.FR)
                .country2(Country.BE)
                .acReferenceFlow(101.424)
                .dcReferenceFlow(98.580)
                .allocatedFlow(100.001)
                .loopFlow(Country.FR, -0.711)
                .loopFlow(Country.BE, -0.711)
                .validate();
        }

        assertEquals(2, flowDecompositionResults.getZoneSet().size());
        assertTrue(flowDecompositionResults.getZoneSet().contains(Country.BE));
        assertTrue(flowDecompositionResults.getZoneSet().contains(Country.FR));
    }

    private static void validateFlowDecompositionResultsWithUnpairedXNodeLoad(FlowDecompositionResults flowDecompositionResults, FlowDecompositionParameters.FlowPartitionMode flowPartitionMode) {
        assertEquals(2, flowDecompositionResults.getDecomposedFlowMap().size());

        if (FULL_LINE_DECOMPOSITION_MODES.contains(flowPartitionMode)) {
            assertThat(flowDecompositionResults)
                .branchId("BLOAD 11 BGEN2 11 1")
                .country1(Country.BE)
                .country2(Country.BE)
                .acReferenceFlow(-100.070)
                .dcReferenceFlow(-100.000)
                .xNodeFlow(75.000)
                .internalFlow(25.000)
                .validate();
            assertThat(flowDecompositionResults)
                .branchId("FGEN1 11 BLOAD 11 1")
                .country1(Country.FR)
                .country2(Country.BE)
                .acReferenceFlow(100.133)
                .dcReferenceFlow(100.000)
                .allocatedFlow(25.000)
                .xNodeFlow(75.000)
                .validate();
        } else {
            assertThat(flowDecompositionResults)
                .branchId("BLOAD 11 BGEN2 11 1")
                .country1(Country.BE)
                .country2(Country.BE)
                .acReferenceFlow(-100.070)
                .dcReferenceFlow(-100.000)
                .allocatedFlow(-25.102)
                .xNodeFlow(75.000)
                .internalFlow(50.051)
                .loopFlow(Country.FR, 0.051)
                .validate();
            assertThat(flowDecompositionResults)
                .branchId("FGEN1 11 BLOAD 11 1")
                .country1(Country.FR)
                .country2(Country.BE)
                .acReferenceFlow(100.133)
                .dcReferenceFlow(100.000)
                .allocatedFlow(25.102)
                .xNodeFlow(75.000)
                .loopFlow(Country.FR, -0.051)
                .loopFlow(Country.BE, -0.051)
                .validate();
        }

        assertEquals(2, flowDecompositionResults.getZoneSet().size());
        assertTrue(flowDecompositionResults.getZoneSet().contains(Country.BE));
        assertTrue(flowDecompositionResults.getZoneSet().contains(Country.FR));
    }

    private static void validateFlowDecompositionResultsWithUnpairedXNodeGen(FlowDecompositionResults flowDecompositionResults, FlowDecompositionParameters.FlowPartitionMode flowPartitionMode) {
        assertEquals(2, flowDecompositionResults.getDecomposedFlowMap().size());

        if (FULL_LINE_DECOMPOSITION_MODES.contains(flowPartitionMode)) {
            assertThat(flowDecompositionResults)
                .branchId("BGEN1 11 BLOAD 11 1")
                .country1(Country.BE)
                .country2(Country.BE)
                .acReferenceFlow(-49.844)
                .dcReferenceFlow(-49.984)
                .xNodeFlow(49.984)
                .validate();
            assertThat(flowDecompositionResults)
                .branchId("FLOAD 11 BGEN1 11 1")
                .country1(Country.FR)
                .country2(Country.BE)
                .acReferenceFlow(-100.000)
                .dcReferenceFlow(-100.000)
                .allocatedFlow(50.016)
                .xNodeFlow(49.984)
                .validate();
        } else {
            assertThat(flowDecompositionResults)
                .branchId("BGEN1 11 BLOAD 11 1")
                .country1(Country.BE)
                .country2(Country.BE)
                .acReferenceFlow(-49.844)
                .dcReferenceFlow(-49.984)
                .xNodeFlow(150.000)
                .internalFlow(-100.016)
                .validate();
            assertThat(flowDecompositionResults)
                .branchId("FLOAD 11 BGEN1 11 1")
                .country1(Country.FR)
                .country2(Country.BE)
                .acReferenceFlow(-100.000)
                .dcReferenceFlow(-100.000)
                .loopFlow(Country.FR, 100.000)
                .validate();
        }

        assertEquals(2, flowDecompositionResults.getZoneSet().size());
        assertTrue(flowDecompositionResults.getZoneSet().contains(Country.BE));
        assertTrue(flowDecompositionResults.getZoneSet().contains(Country.FR));
    }

    private static void validateFlowDecompositionResultsWithPairedXNode(FlowDecompositionResults flowDecompositionResults, FlowDecompositionParameters.FlowPartitionMode flowPartitionMode) {
        assertEquals(2, flowDecompositionResults.getDecomposedFlowMap().size());

        if (FULL_LINE_DECOMPOSITION_MODES.contains(flowPartitionMode)) {
            assertThat(flowDecompositionResults)
                .branchId("BLOAD 11 BGEN2 11 1")
                .country1(Country.BE)
                .country2(Country.BE)
                .acReferenceFlow(-100.000)
                .dcReferenceFlow(-100.000)
                .internalFlow(100.000)
                .validate();
            assertThat(flowDecompositionResults)
                .branchId("FGEN1 11 X     11 1 + X     11 BLOAD 11 1")
                .country1(Country.FR)
                .country2(Country.BE)
                .acReferenceFlow(100.063)
                .dcReferenceFlow(100.047)
                .allocatedFlow(100.047)
                .validate();
        } else {
            assertThat(flowDecompositionResults)
                .branchId("BLOAD 11 BGEN2 11 1")
                .country1(Country.BE)
                .country2(Country.BE)
                .acReferenceFlow(-100.000)
                .dcReferenceFlow(-100.000)
                .allocatedFlow(-100.047)
                .internalFlow(200.047)
                .validate();
            assertThat(flowDecompositionResults)
                .branchId("FGEN1 11 X     11 1 + X     11 BLOAD 11 1")
                .country1(Country.FR)
                .country2(Country.BE)
                .acReferenceFlow(100.063)
                .dcReferenceFlow(100.047)
                .allocatedFlow(100.047)
                .validate();
        }

        assertEquals(2, flowDecompositionResults.getZoneSet().size());
        assertTrue(flowDecompositionResults.getZoneSet().contains(Country.BE));
        assertTrue(flowDecompositionResults.getZoneSet().contains(Country.FR));
    }

    private static void validatePowerFlowColouringResultsWithXnodeGen(FlowDecompositionResults flowDecompositionResults) throws IOException {
        InputStream inputStream = FlowDecompositionTests.class.getResourceAsStream("validatePowerFlowColouringResultsWithXnodeGen.csv");
        FlowDecompositionAssertionsImporter.importFromResource(inputStream, "validatePowerFlowColouringResultsWithXnodeGen.csv")
            .forEach(assertion -> assertion.validate(flowDecompositionResults));
    }

    private static void validateFullLineDecompositionResultsWithXnodeGen(FlowDecompositionResults flowDecompositionResults) {
        assertThat(flowDecompositionResults)
            .branchId("BBE1AA1  BBE2AA1  1")
            .country1(Country.BE)
            .country2(Country.BE)
            .acReferenceFlow(-878.370)
            .dcReferenceFlow(-878.333)
            .allocatedFlow(191.529)
            .xNodeFlow(107.446)
            .internalFlow(550.311)
            .loopFlow(Country.FR, 31.596)
            .loopFlow(Country.NL, -2.548)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("BBE1AA1  BBE3AA1  1")
            .country1(Country.BE)
            .country2(Country.BE)
            .acReferenceFlow(-121.630)
            .dcReferenceFlow(-121.667)
            .allocatedFlow(-191.529)
            .xNodeFlow(13.955)
            .internalFlow(328.288)
            .loopFlow(Country.FR, -31.596)
            .loopFlow(Country.NL, 2.548)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("BBE2AA1  BBE3AA1  1")
            .country1(Country.BE)
            .country2(Country.BE)
            .acReferenceFlow(-756.898)
            .dcReferenceFlow(-756.667)
            .allocatedFlow(383.058)
            .xNodeFlow(93.491)
            .internalFlow(222.024)
            .loopFlow(Country.FR, 63.191)
            .loopFlow(Country.NL, -5.097)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("BBE2AA1  FFR3AA1  1")
            .country1(Country.BE)
            .country2(Country.FR)
            .acReferenceFlow(664.731)
            .dcReferenceFlow(665.000)
            .allocatedFlow(616.074)
            .xNodeFlow(99.063)
            .loopFlow(Country.BE, 37.004)
            .loopFlow(Country.FR, -94.787)
            .loopFlow(Country.NL, 7.645)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("DDE1AA1  DDE2AA1  1")
            .country1(Country.DE)
            .country2(Country.DE)
            .acReferenceFlow(-478.511)
            .dcReferenceFlow(-478.333)
            .allocatedFlow(449.781)
            .xNodeFlow(11.840)
            .loopFlow(Country.BE, -12.335)
            .loopFlow(Country.FR, 31.596)
            .loopFlow(Country.NL, -2.548)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("DDE1AA1  DDE3AA1  1")
            .country1(Country.DE)
            .country2(Country.DE)
            .acReferenceFlow(-821.489)
            .dcReferenceFlow(-821.667)
            .allocatedFlow(810.333)
            .xNodeFlow(28.046)
            .loopFlow(Country.BE, 12.335)
            .loopFlow(Country.FR, -31.596)
            .loopFlow(Country.NL, 2.548)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("DDE2AA1  DDE3AA1  1")
            .country1(Country.DE)
            .country2(Country.DE)
            .acReferenceFlow(-343.242)
            .dcReferenceFlow(-343.333)
            .allocatedFlow(360.553)
            .xNodeFlow(16.206)
            .loopFlow(Country.BE, 24.669)
            .loopFlow(Country.FR, -63.191)
            .loopFlow(Country.NL, 5.097)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("DDE2AA1  NNL3AA1  1")
            .country1(Country.DE)
            .country2(Country.NL)
            .acReferenceFlow(-1135.269)
            .dcReferenceFlow(-1135.000)
            .allocatedFlow(1052.919)
            .xNodeFlow(31.943)
            .loopFlow(Country.BE, -37.004)
            .loopFlow(Country.FR, 94.787)
            .loopFlow(Country.NL, -7.645)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("FFR1AA1  FFR2AA1  1")
            .country1(Country.FR)
            .country2(Country.FR)
            .acReferenceFlow(1388.870)
            .dcReferenceFlow(1388.333)
            .allocatedFlow(819.102)
            .xNodeFlow(33.021)
            .internalFlow(521.327)
            .loopFlow(Country.BE, 12.335)
            .loopFlow(Country.NL, 2.548)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("FFR1AA1  FFR3AA1  1")
            .country1(Country.FR)
            .country2(Country.FR)
            .acReferenceFlow(-388.870)
            .dcReferenceFlow(-388.333)
            .allocatedFlow(293.036)
            .xNodeFlow(33.021)
            .internalFlow(47.393)
            .loopFlow(Country.BE, 12.335)
            .loopFlow(Country.NL, 2.548)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("FFR2AA1  DDE3AA1  1")
            .country1(Country.FR)
            .country2(Country.DE)
            .acReferenceFlow(1664.731)
            .dcReferenceFlow(1665.000)
            .allocatedFlow(1657.183)
            .xNodeFlow(57.955)
            .loopFlow(Country.BE, 37.004)
            .loopFlow(Country.FR, -94.787)
            .loopFlow(Country.NL, 7.645)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("FFR2AA1  FFR3AA1  1")
            .country1(Country.FR)
            .country2(Country.FR)
            .acReferenceFlow(-1775.862)
            .dcReferenceFlow(-1776.667)
            .allocatedFlow(1112.138)
            .xNodeFlow(66.042)
            .internalFlow(568.720)
            .loopFlow(Country.BE, 24.669)
            .loopFlow(Country.NL, 5.097)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("NNL1AA1  NNL2AA1  1")
            .country1(Country.NL)
            .country2(Country.NL)
            .acReferenceFlow(-211.847)
            .dcReferenceFlow(-211.667)
            .allocatedFlow(228.775)
            .xNodeFlow(17.148)
            .internalFlow(-53.517)
            .loopFlow(Country.BE, -12.335)
            .loopFlow(Country.FR, 31.596)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("NNL1AA1  NNL3AA1  1")
            .country1(Country.NL)
            .country2(Country.NL)
            .acReferenceFlow(711.847)
            .dcReferenceFlow(711.667)
            .allocatedFlow(575.869)
            .xNodeFlow(17.148)
            .internalFlow(99.388)
            .loopFlow(Country.BE, -12.335)
            .loopFlow(Country.FR, 31.596)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("NNL2AA1  BBE3AA1  1")
            .country1(Country.NL)
            .country2(Country.BE)
            .acReferenceFlow(-1635.269)
            .dcReferenceFlow(-1635.000)
            .allocatedFlow(1505.327)
            .xNodeFlow(79.536)
            .loopFlow(Country.BE, -37.004)
            .loopFlow(Country.FR, 94.787)
            .loopFlow(Country.NL, -7.645)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("NNL2AA1  NNL3AA1  1")
            .country1(Country.NL)
            .country2(Country.NL)
            .acReferenceFlow(923.422)
            .dcReferenceFlow(923.333)
            .allocatedFlow(804.644)
            .xNodeFlow(34.296)
            .internalFlow(45.872)
            .loopFlow(Country.BE, -24.669)
            .loopFlow(Country.FR, 63.191)
            .validate();
    }

    private static void validatePowerFlowColouringResultsWithXnodeLoad(FlowDecompositionResults flowDecompositionResults) throws IOException {
        InputStream inputStream = FlowDecompositionTests.class.getResourceAsStream("validatePowerFlowColouringResultsWithXnodeLoad.csv");
        FlowDecompositionAssertionsImporter.importFromResource(inputStream, "validatePowerFlowColouringResultsWithXnodeLoad.csv")
            .forEach(assertion -> assertion.validate(flowDecompositionResults));
    }

    private static void validateFullLineDecompositionResultsWithXnodeLoad(FlowDecompositionResults flowDecompositionResults) {
        assertThat(flowDecompositionResults)
            .branchId("BBE1AA1  BBE2AA1  1")
            .country1(Country.BE)
            .country2(Country.BE)
            .acReferenceFlow(-878.370)
            .dcReferenceFlow(-878.333)
            .allocatedFlow(220.064)
            .internalFlow(629.222)
            .loopFlow(Country.FR, 31.596)
            .loopFlow(Country.NL, -2.548)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("BBE1AA1  BBE3AA1  1")
            .country1(Country.BE)
            .country2(Country.BE)
            .acReferenceFlow(-121.630)
            .dcReferenceFlow(-121.667)
            .allocatedFlow(-220.064)
            .internalFlow(370.778)
            .loopFlow(Country.FR, -31.596)
            .loopFlow(Country.NL, 2.548)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("BBE2AA1  BBE3AA1  1")
            .country1(Country.BE)
            .country2(Country.BE)
            .acReferenceFlow(-756.898)
            .dcReferenceFlow(-756.667)
            .allocatedFlow(440.128)
            .internalFlow(258.444)
            .loopFlow(Country.FR, 63.191)
            .loopFlow(Country.NL, -5.097)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("BBE2AA1  FFR3AA1  1")
            .country1(Country.BE)
            .country2(Country.FR)
            .acReferenceFlow(664.731)
            .dcReferenceFlow(665.000)
            .allocatedFlow(709.067)
            .loopFlow(Country.BE, 43.074)
            .loopFlow(Country.FR, -94.787)
            .loopFlow(Country.NL, 7.645)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("DDE1AA1  DDE2AA1  1")
            .country1(Country.DE)
            .country2(Country.DE)
            .acReferenceFlow(-478.511)
            .dcReferenceFlow(-478.333)
            .allocatedFlow(463.644)
            .loopFlow(Country.BE, -14.358)
            .loopFlow(Country.FR, 31.596)
            .loopFlow(Country.NL, -2.548)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("DDE1AA1  DDE3AA1  1")
            .country1(Country.DE)
            .country2(Country.DE)
            .acReferenceFlow(-821.489)
            .dcReferenceFlow(-821.667)
            .allocatedFlow(836.356)
            .loopFlow(Country.BE, 14.358)
            .loopFlow(Country.FR, -31.596)
            .loopFlow(Country.NL, 2.548)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("DDE2AA1  DDE3AA1  1")
            .country1(Country.DE)
            .country2(Country.DE)
            .acReferenceFlow(-343.242)
            .dcReferenceFlow(-343.333)
            .allocatedFlow(372.712)
            .loopFlow(Country.BE, 28.716)
            .loopFlow(Country.FR, -63.191)
            .loopFlow(Country.NL, 5.097)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("DDE2AA1  NNL3AA1  1")
            .country1(Country.DE)
            .country2(Country.NL)
            .acReferenceFlow(-1135.269)
            .dcReferenceFlow(-1135.000)
            .allocatedFlow(1090.933)
            .loopFlow(Country.BE, -43.074)
            .loopFlow(Country.FR, 94.787)
            .loopFlow(Country.NL, -7.645)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("FFR1AA1  FFR2AA1  1")
            .country1(Country.FR)
            .country2(Country.FR)
            .acReferenceFlow(1388.870)
            .dcReferenceFlow(1388.333)
            .allocatedFlow(850.100)
            .internalFlow(521.327)
            .loopFlow(Country.BE, 14.358)
            .loopFlow(Country.NL, 2.548)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("FFR1AA1  FFR3AA1  1")
            .country1(Country.FR)
            .country2(Country.FR)
            .acReferenceFlow(-388.870)
            .dcReferenceFlow(-388.333)
            .allocatedFlow(324.034)
            .internalFlow(47.393)
            .loopFlow(Country.BE, 14.358)
            .loopFlow(Country.NL, 2.548)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("FFR2AA1  DDE3AA1  1")
            .country1(Country.FR)
            .country2(Country.DE)
            .acReferenceFlow(1664.731)
            .dcReferenceFlow(1665.000)
            .allocatedFlow(1709.067)
            .loopFlow(Country.BE, 43.074)
            .loopFlow(Country.FR, -94.787)
            .loopFlow(Country.NL, 7.645)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("FFR2AA1  FFR3AA1  1")
            .country1(Country.FR)
            .country2(Country.FR)
            .acReferenceFlow(-1775.862)
            .dcReferenceFlow(-1776.667)
            .allocatedFlow(1174.133)
            .internalFlow(568.720)
            .loopFlow(Country.BE, 28.716)
            .loopFlow(Country.NL, 5.097)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("NNL1AA1  NNL2AA1  1")
            .country1(Country.NL)
            .country2(Country.NL)
            .acReferenceFlow(-211.847)
            .dcReferenceFlow(-211.667)
            .allocatedFlow(247.946)
            .internalFlow(-53.517)
            .loopFlow(Country.BE, -14.358)
            .loopFlow(Country.FR, 31.596)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("NNL1AA1  NNL3AA1  1")
            .country1(Country.NL)
            .country2(Country.NL)
            .acReferenceFlow(711.847)
            .dcReferenceFlow(711.667)
            .allocatedFlow(595.041)
            .internalFlow(99.388)
            .loopFlow(Country.BE, -14.358)
            .loopFlow(Country.FR, 31.596)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("NNL2AA1  BBE3AA1  1")
            .country1(Country.NL)
            .country2(Country.BE)
            .acReferenceFlow(-1635.269)
            .dcReferenceFlow(-1635.000)
            .allocatedFlow(1590.933)
            .loopFlow(Country.BE, -43.074)
            .loopFlow(Country.FR, 94.787)
            .loopFlow(Country.NL, -7.645)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("NNL2AA1  NNL3AA1  1")
            .country1(Country.NL)
            .country2(Country.NL)
            .acReferenceFlow(923.422)
            .dcReferenceFlow(923.333)
            .allocatedFlow(842.987)
            .internalFlow(45.872)
            .loopFlow(Country.BE, -28.716)
            .loopFlow(Country.FR, 63.191)
            .validate();
    }

    @Test
    void shouldComputeVariableSetSensitivityAsWeightedSumOfElementarySensitivities() {
        Network network = TestUtils.importNetwork("TestCase16NodesWithHvdc.xiidm");
        SensitivityAnalysis.Runner runner = SensitivityAnalysis.find("OpenLoadFlow");
        SensitivityAnalysisParameters parameters = SensitivityAnalysisParameters.load();

        List<String> xnecIds = network.getLineStream().map(Identifiable::getId).toList();
        List<String> variableIds = List.of("FFR5AA11_generator", "DDE3AA11_load", "NNL2AA11_load", "NNL3AA11_generator");
        List<Double> weights = List.of(2.0, 0.5, 3.0, 1.0);
        double sum = weights.stream().reduce(0.0, Double::sum);

        SensitivityVariableSet variableSet = new SensitivityVariableSet(
            "TEST_SET",
            IntStream.range(0, variableIds.size()).mapToObj(i -> new WeightedSensitivityVariable(variableIds.get(i), weights.get(i) / sum)).toList()
        );

        Map<String, Map<String, Double>> elementary = runElementarySensitivities(
            network, parameters, runner, xnecIds, variableIds
        );

        Map<String, Double> grouped = runGroupedSensitivity(
            network, parameters, runner, xnecIds, variableSet
        );

        double epsilon = 1e-8;

        for (String xnecId : xnecIds) {
            double expected = IntStream.range(0, variableIds.size()).mapToDouble(i -> weights.get(i) * elementary.get(xnecId).get(variableIds.get(i)) / sum).sum();

            double actual = grouped.get(xnecId);

            assertEquals(expected, actual, epsilon, "Mismatch for xnec " + xnecId);
        }
    }

    private Map<String, Map<String, Double>> runElementarySensitivities(Network network,
                                                                        SensitivityAnalysisParameters sensitivityAnalysisParameters,
                                                                        SensitivityAnalysis.Runner runner,
                                                                        List<String> xnecIds,
                                                                        List<String> variableIds) {
        List<SensitivityFactor> sensitivityFactors = new ArrayList<>();
        xnecIds.forEach(
            xnecId -> variableIds.forEach(
                variableId -> {
                    SensitivityFactor sensitivityFactor = new SensitivityFactor(SENSITIVITY_FUNCTION_TYPE,
                        xnecId,
                        SensitivityVariableType.INJECTION_ACTIVE_POWER,
                        variableId,
                        false,
                        ContingencyContext.none());
                    sensitivityFactors.add(sensitivityFactor);
                }));
        SensitivityAnalysisRunParameters runParameters = new SensitivityAnalysisRunParameters()
            .setContingencies(CONTINGENCIES)
            .setVariableSets(EMPTY_SENSITIVITY_VARIABLE_SETS)
            .setParameters(sensitivityAnalysisParameters);
        SensitivityAnalysisResult analysisResult = runner.run(network, sensitivityFactors, runParameters);

        return xnecIds.stream().collect(Collectors.toMap(
            xnecId -> xnecId,
            xnecId -> variableIds.stream().collect(Collectors.toMap(
                variableId -> variableId,
                variableId -> analysisResult.getSensitivityValue(variableId, xnecId, SENSITIVITY_FUNCTION_TYPE, SensitivityVariableType.INJECTION_ACTIVE_POWER)
            ))
        ));
    }

    private Map<String, Double> runGroupedSensitivity(Network network,
                                                      SensitivityAnalysisParameters sensitivityAnalysisParameters,
                                                      SensitivityAnalysis.Runner runner,
                                                      List<String> xnecIds,
                                                      SensitivityVariableSet variableSet) {
        List<SensitivityFactor> sensitivityFactors = xnecIds.stream().map(
            xnecId -> new SensitivityFactor(SENSITIVITY_FUNCTION_TYPE, xnecId, SensitivityVariableType.INJECTION_ACTIVE_POWER, variableSet.getId(), true, ContingencyContext.none())).toList();
        SensitivityAnalysisRunParameters runParameters = new SensitivityAnalysisRunParameters()
            .setContingencies(CONTINGENCIES)
            .setVariableSets(List.of(variableSet))
            .setParameters(sensitivityAnalysisParameters);
        SensitivityAnalysisResult analysisResult = runner.run(network, sensitivityFactors, runParameters);

        return xnecIds.stream().collect(Collectors.toMap(
            xnecId -> xnecId,
            xnecId -> analysisResult.getSensitivityValue(variableSet.getId(), xnecId, SENSITIVITY_FUNCTION_TYPE, SensitivityVariableType.INJECTION_ACTIVE_POWER)
        ));
    }

    @ParameterizedTest(name = "Mode={0}")
    @EnumSource(value = FlowDecompositionParameters.FlowPartitionMode.class, names = {
        "MATRIX_BASED",
        "DIRECT_SENSITIVITY_BASED",
        "FULL_LINE_DECOMPOSITION",
        "FAST_FULL_LINE_DECOMPOSITION"
    })
    void testFlowDecompositionOnNetworkWithBusBarSectionOnly(FlowDecompositionParameters.FlowPartitionMode flowPartitionMode) {
        Network network = TestUtils.getMicroGridNetworkWithBusBarSectionOnly();

        XnecProviderAllBranches xnecProvider = new XnecProviderAllBranches();
        if (FULL_LINE_DECOMPOSITION_MODES.contains(flowPartitionMode)) {
            // TODO fix this test, FLD does not support three winding transformers
            PowsyblException exception = assertThrows(PowsyblException.class, () -> runFlowDecomposition(network, xnecProvider, flowPartitionMode));
            assertEquals("Nodal generation and load do not match for vertex associated with bus: b10b171b-3bc5-4849-bb1f-61ed9ea1ec7c_0", exception.getMessage());
        } else {
            FlowDecompositionResults flowDecompositionResults = runFlowDecomposition(network, xnecProvider, flowPartitionMode);
            validateFlowDecompositionResultsWithBusBarSectionOnly(flowDecompositionResults);
        }
    }

    @ParameterizedTest(name = "Mode={0}")
    @EnumSource(value = FlowDecompositionParameters.FlowPartitionMode.class, names = {
        "MATRIX_BASED",
        "DIRECT_SENSITIVITY_BASED",
        "FULL_LINE_DECOMPOSITION",
        "FAST_FULL_LINE_DECOMPOSITION"
    })
    void testFlowDecompositionOnNetworkWithShuntCompensatorOnly(FlowDecompositionParameters.FlowPartitionMode flowPartitionMode) {
        Network network = TestUtils.getMicroGridNetworkWithShuntCompensatorOnly();

        XnecProviderAllBranches xnecProvider = new XnecProviderAllBranches();
        if (FULL_LINE_DECOMPOSITION_MODES.contains(flowPartitionMode)) {
            // TODO fix this test, FLD does not support three winding transformers
            PowsyblException exception = assertThrows(PowsyblException.class, () -> runFlowDecomposition(network, xnecProvider, flowPartitionMode));
            assertEquals("Nodal generation and load do not match for vertex associated with bus: b10b171b-3bc5-4849-bb1f-61ed9ea1ec7c_0", exception.getMessage());
        } else {
            FlowDecompositionResults flowDecompositionResults = runFlowDecomposition(network, xnecProvider, flowPartitionMode);
            validateFlowDecompositionResultsWithShuntCompensatorOnly(flowDecompositionResults);
        }
    }

    @ParameterizedTest(name = "Mode={0}")
    @EnumSource(value = FlowDecompositionParameters.FlowPartitionMode.class, names = {
        "MATRIX_BASED",
        "DIRECT_SENSITIVITY_BASED",
        "FULL_LINE_DECOMPOSITION",
        "FAST_FULL_LINE_DECOMPOSITION"
    })
    void testFlowDecompositionOnNetworkWithStaticVarCompensatorOnly(FlowDecompositionParameters.FlowPartitionMode flowPartitionMode) {
        Network network = TestUtils.getNetworkWithStaticVarCompensatorOnly();

        FlowDecompositionResults flowDecompositionResults = runFlowDecomposition(network, new XnecProviderAllBranches(), flowPartitionMode);
        assertEquals(1, flowDecompositionResults.getDecomposedFlowMap().size());
        assertThat(flowDecompositionResults)
            .branchId("L1")
            .country1(Country.FR)
            .country2(Country.FR)
            .acReferenceFlow(100.266)
            .dcReferenceFlow(100.000)
            .internalFlow(100.000)
            .validate();
        assertEquals(1, flowDecompositionResults.getZoneSet().size());
        assertTrue(flowDecompositionResults.getZoneSet().contains(Country.FR));
    }

    @ParameterizedTest(name = "Mode={0}")
    @EnumSource(value = FlowDecompositionParameters.FlowPartitionMode.class, names = {
        "MATRIX_BASED",
        "DIRECT_SENSITIVITY_BASED",
        "FULL_LINE_DECOMPOSITION",
        "FAST_FULL_LINE_DECOMPOSITION"
    })
    void testFlowDecompositionOnHvdcNetworkUsingMode(FlowDecompositionParameters.FlowPartitionMode flowPartitionMode) throws IOException {
        FlowDecompositionResults flowDecompositionResults = testFlowDecompositionOnHvdcNetwork(flowPartitionMode);
        if (FULL_LINE_DECOMPOSITION_MODES.contains(flowPartitionMode)) {
            validateFlowDecompositionResultsUsingFLDMethodology(flowDecompositionResults);
        } else {
            validateFlowDecompositionResultsUsingPFCMethodology(flowDecompositionResults);
        }
    }

    @Test
    void testComponentModeChangesFromAllToMain() {
        LoadFlowParameters loadFlowParameters = LoadFlowParameters.load().setComponentMode(LoadFlowParameters.ComponentMode.ALL_CONNECTED);
        FlowDecompositionComputer flowDecompositionComputer = new FlowDecompositionComputer(new FlowDecompositionParameters(), loadFlowParameters);
        // lfParameters inside flow decomposition changed from all to main
        assertEquals(FlowDecompositionComputer.MAIN_CONNECTED_COMPONENT, flowDecompositionComputer.getLoadFlowParameters().getComponentMode());
        // original lfParameters didn't change
        assertEquals(LoadFlowParameters.ComponentMode.ALL_CONNECTED, loadFlowParameters.getComponentMode());
    }

    @ParameterizedTest(name = "Mode={0}")
    @EnumSource(value = FlowDecompositionParameters.FlowPartitionMode.class, names = {
        "MATRIX_BASED",
        "DIRECT_SENSITIVITY_BASED",
        "FULL_LINE_DECOMPOSITION",
        "FAST_FULL_LINE_DECOMPOSITION"
    })
    void testSimpleNetworkWith6Nodes(FlowDecompositionParameters.FlowPartitionMode flowPartitionMode) {
        String networkFileName = "NETWORK_LOOP_FLOW_WITH_COUNTRIES.uct";

        Network network = TestUtils.importNetwork(networkFileName);

        FlowDecompositionResults flowDecompositionResults = runFlowDecomposition(network, new XnecProviderAllBranches(), flowPartitionMode);
        assertEquals(5, flowDecompositionResults.getDecomposedFlowMap().size());

        validateFlowDecompositionResultsWith6Nodes(flowPartitionMode, flowDecompositionResults);
    }

    @ParameterizedTest(name = "Mode={0}")
    @EnumSource(value = FlowDecompositionParameters.FlowPartitionMode.class, names = {
        "MATRIX_BASED",
        "DIRECT_SENSITIVITY_BASED",
        "FULL_LINE_DECOMPOSITION",
        "FAST_FULL_LINE_DECOMPOSITION"
    })
    void testSimpleNetworkWith6NodesConnected(FlowDecompositionParameters.FlowPartitionMode flowPartitionMode) {
        String networkFileName = "NETWORK_6_NODES_CONNECTED.uct";

        Network network = TestUtils.importNetwork(networkFileName);

        FlowDecompositionResults flowDecompositionResults = runFlowDecomposition(network, new XnecProviderAllBranches(), flowPartitionMode);
        validateFlowDecompositionResultsWith6NodesConnected(flowDecompositionResults);
    }

    @ParameterizedTest(name = "Mode={0}")
    @EnumSource(value = FlowDecompositionParameters.FlowPartitionMode.class, names = {
        "MATRIX_BASED",
        "DIRECT_SENSITIVITY_BASED",
        "FULL_LINE_DECOMPOSITION",
        "FAST_FULL_LINE_DECOMPOSITION"
    })
    void testSimpleNetworkWith6NodesConnectedAsymmetrical(FlowDecompositionParameters.FlowPartitionMode flowPartitionMode) {
        String networkFileName = "NETWORK_6_NODES_CONNECTED.uct";

        Network network = TestUtils.importNetwork(networkFileName);
        network.getGenerator("EGEN  11_generator").setTargetP(250.0);
        network.getLoad("ELOAD 11_load").setP0(250.0);

        FlowDecompositionResults flowDecompositionResults = runFlowDecomposition(network, new XnecProviderAllBranches(), flowPartitionMode);
        validateFlowDecompositionResultsWith6NodesConnectedAsymmetrical(flowDecompositionResults, flowPartitionMode);
    }

    @ParameterizedTest(name = "Mode={0}")
    @EnumSource(value = FlowDecompositionParameters.FlowPartitionMode.class, names = {
        "MATRIX_BASED",
        "DIRECT_SENSITIVITY_BASED",
        "FULL_LINE_DECOMPOSITION",
        "FAST_FULL_LINE_DECOMPOSITION"
    })
    void testSimpleNetworkWithPst(FlowDecompositionParameters.FlowPartitionMode flowPartitionMode) {
        String networkFileName = "NETWORK_PST_FLOW_WITH_COUNTRIES_NON_NEUTRAL.uct";

        Network network = TestUtils.importNetwork(networkFileName);

        FlowDecompositionResults flowDecompositionResults = runFlowDecomposition(network, new XnecProviderAllBranches(), flowPartitionMode);
        validateFlowDecompositionResultsWithPst(flowDecompositionResults, flowPartitionMode);
    }

    @ParameterizedTest(name = "Mode={0}")
    @EnumSource(value = FlowDecompositionParameters.FlowPartitionMode.class, names = {
        "MATRIX_BASED",
        "DIRECT_SENSITIVITY_BASED",
        "FULL_LINE_DECOMPOSITION",
        "FAST_FULL_LINE_DECOMPOSITION"
    })
    void testSimpleNetworkWithSubStation(FlowDecompositionParameters.FlowPartitionMode flowPartitionMode) {
        String networkFileName = "NETWORK_SINGLE_LOAD_TWO_GENERATORS_WITH_COUNTRIES_EXTRA_SUBSTATION.uct";

        Network network = TestUtils.importNetwork(networkFileName);

        FlowDecompositionResults flowDecompositionResults = runFlowDecomposition(network, new XnecProviderAllBranches(), flowPartitionMode);
        validateFlowDecompositionResultsWithSubStation(flowDecompositionResults, flowPartitionMode);
    }

    @ParameterizedTest(name = "Mode={0}")
    @EnumSource(value = FlowDecompositionParameters.FlowPartitionMode.class, names = {
        "MATRIX_BASED",
        "DIRECT_SENSITIVITY_BASED",
        "FULL_LINE_DECOMPOSITION",
        "FAST_FULL_LINE_DECOMPOSITION"
    })
    void testSimpleNetworkWithUnpairedXNodeLoad(FlowDecompositionParameters.FlowPartitionMode flowPartitionMode) {
        String networkFileName = "NETWORK_SINGLE_LOAD_TWO_GENERATORS_WITH_UNBOUNDED_XNODE_LOAD.uct";

        Network network = TestUtils.importNetwork(networkFileName);

        FlowDecompositionResults flowDecompositionResults = runFlowDecomposition(network, new XnecProviderAllBranches(), flowPartitionMode);
        validateFlowDecompositionResultsWithUnpairedXNodeLoad(flowDecompositionResults, flowPartitionMode);
    }

    @ParameterizedTest(name = "Mode={0}")
    @EnumSource(value = FlowDecompositionParameters.FlowPartitionMode.class, names = {
        "MATRIX_BASED",
        "DIRECT_SENSITIVITY_BASED",
        "FULL_LINE_DECOMPOSITION",
        "FAST_FULL_LINE_DECOMPOSITION"
    })
    void testSimpleNetworkWithUnpairedXNodeGen(FlowDecompositionParameters.FlowPartitionMode flowPartitionMode) {
        String networkFileName = "NETWORK_SINGLE_LOAD_TWO_GENERATORS_WITH_UNBOUNDED_XNODE_GEN.uct";

        Network network = TestUtils.importNetwork(networkFileName);

        FlowDecompositionResults flowDecompositionResults = runFlowDecomposition(network, new XnecProviderAllBranches(), flowPartitionMode);
        validateFlowDecompositionResultsWithUnpairedXNodeGen(flowDecompositionResults, flowPartitionMode);
    }

    @ParameterizedTest(name = "Mode={0}")
    @EnumSource(value = FlowDecompositionParameters.FlowPartitionMode.class, names = {
        "MATRIX_BASED",
        "DIRECT_SENSITIVITY_BASED",
        "FULL_LINE_DECOMPOSITION",
        "FAST_FULL_LINE_DECOMPOSITION"
    })
    void testSimpleNetworkWithPairedXNode(FlowDecompositionParameters.FlowPartitionMode flowPartitionMode) {
        String networkFileName = "NETWORK_SINGLE_LOAD_TWO_GENERATORS_WITH_XNODE.uct";

        Network network = TestUtils.importNetwork(networkFileName);

        FlowDecompositionResults flowDecompositionResults = runFlowDecomposition(network, new XnecProviderAllBranches(), flowPartitionMode);
        validateFlowDecompositionResultsWithPairedXNode(flowDecompositionResults, flowPartitionMode);
    }

    @ParameterizedTest(name = "Mode={0}")
    @EnumSource(value = FlowDecompositionParameters.FlowPartitionMode.class, names = {
        "MATRIX_BASED",
        "DIRECT_SENSITIVITY_BASED",
        "FULL_LINE_DECOMPOSITION",
        "FAST_FULL_LINE_DECOMPOSITION"
    })
    void testSimpleNetworkWithXNodeGen(FlowDecompositionParameters.FlowPartitionMode flowPartitionMode) throws IOException {
        String networkFileName = "TestCaseDangling.xiidm";

        Network network = TestUtils.importNetwork(networkFileName);

        FlowDecompositionResults flowDecompositionResults = runFlowDecomposition(network, new XnecProviderAllBranches(), flowPartitionMode);
        assertEquals(16, flowDecompositionResults.getDecomposedFlowMap().size());

        if (FULL_LINE_DECOMPOSITION_MODES.contains(flowPartitionMode)) {
            validateFullLineDecompositionResultsWithXnodeGen(flowDecompositionResults);
        } else {
            validatePowerFlowColouringResultsWithXnodeGen(flowDecompositionResults);
        }

        assertEquals(4, flowDecompositionResults.getZoneSet().size());
        assertTrue(flowDecompositionResults.getZoneSet().contains(Country.BE));
        assertTrue(flowDecompositionResults.getZoneSet().contains(Country.DE));
        assertTrue(flowDecompositionResults.getZoneSet().contains(Country.FR));
        assertTrue(flowDecompositionResults.getZoneSet().contains(Country.NL));
    }

    @ParameterizedTest(name = "Mode={0}")
    @EnumSource(value = FlowDecompositionParameters.FlowPartitionMode.class, names = {
        "MATRIX_BASED",
        "DIRECT_SENSITIVITY_BASED",
        "FULL_LINE_DECOMPOSITION",
        "FAST_FULL_LINE_DECOMPOSITION"
    })
    void testSimpleNetworkWithXNodeLoad(FlowDecompositionParameters.FlowPartitionMode flowPartitionMode) throws IOException {
        String networkFileName = "TestCaseDangling.xiidm";

        Network network = TestUtils.importNetwork(networkFileName);
        network.getBoundaryLine("BBE2AA1  X_BEFR1  1").setP0(300);
        network.getGenerator("BBE2AA1 _generator").setTargetP(3600);

        FlowDecompositionResults flowDecompositionResults = runFlowDecomposition(network, new XnecProviderAllBranches(), flowPartitionMode);
        assertEquals(16, flowDecompositionResults.getDecomposedFlowMap().size());

        if (FULL_LINE_DECOMPOSITION_MODES.contains(flowPartitionMode)) {
            validateFullLineDecompositionResultsWithXnodeLoad(flowDecompositionResults);
        } else {
            validatePowerFlowColouringResultsWithXnodeLoad(flowDecompositionResults);
        }

        assertEquals(4, flowDecompositionResults.getZoneSet().size());
        assertTrue(flowDecompositionResults.getZoneSet().contains(Country.BE));
        assertTrue(flowDecompositionResults.getZoneSet().contains(Country.DE));
        assertTrue(flowDecompositionResults.getZoneSet().contains(Country.FR));
        assertTrue(flowDecompositionResults.getZoneSet().contains(Country.NL));
    }
}
