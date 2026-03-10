/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.flow_decomposition;

import com.powsybl.flow_decomposition.xnec_provider.XnecProviderAllBranches;
import com.powsybl.flow_decomposition.xnec_provider.XnecProviderByIds;
import com.powsybl.flow_decomposition.xnec_provider.XnecProviderUnion;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Identifiable;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlowParameters;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.powsybl.flow_decomposition.TestUtils.validateFlowDecomposition;
import static java.lang.Double.NaN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 */
class FlowDecompositionTests {

    private static FlowDecompositionResults runFlowDecomposition(Network network, XnecProvider xnecProvider, FlowDecompositionParameters.FlowPartitionMode flowPartitionMode) {
        FlowDecompositionParameters flowDecompositionParameters = new FlowDecompositionParameters()
            .setEnableLossesCompensation(FlowDecompositionParameters.ENABLE_LOSSES_COMPENSATION)
            .setLossesCompensationEpsilon(FlowDecompositionParameters.DISABLE_LOSSES_COMPENSATION_EPSILON)
            .setSensitivityEpsilon(FlowDecompositionParameters.DISABLE_SENSITIVITY_EPSILON)
            .setRescaleMode(FlowDecompositionParameters.RescaleMode.NONE)
            .setFlowPartitioner(flowPartitionMode);
        FlowDecompositionComputer flowDecompositionComputer = new FlowDecompositionComputer(flowDecompositionParameters, LoadFlowParameters.load());
        FlowDecompositionResults flowDecompositionResults = flowDecompositionComputer.run(xnecProvider, network);
        TestUtils.assertCoherenceTotalFlow(flowDecompositionParameters.getRescaleMode(), flowDecompositionResults);
        return flowDecompositionResults;
    }

    private static FlowDecompositionResults runFlowDecomposition(Network network, XnecProvider xnecProvider) {
        return runFlowDecomposition(network, xnecProvider, FlowDecompositionParameters.FlowPartitionMode.MATRIX_BASED);
    }

    @Test
    void testFlowDecompositionOnNetworkWithBusBarSectionOnly() {
        Network network = TestUtils.getMicroGridNetworkWithBusBarSectionOnly();

        FlowDecompositionResults flowDecompositionResults = runFlowDecomposition(network, new XnecProviderAllBranches());
        assertEquals(6, flowDecompositionResults.getDecomposedFlowMap().size());
        validateFlowDecomposition(flowDecompositionResults, "a708c3bc-465d-4fe7-b6ef-6fa6408a62b0", "a708c3bc-465d-4fe7-b6ef-6fa6408a62b0", "", Country.BE, Country.BE, 105.335, 115.129, -8.896, 30.705, 33.030, 60.289, 0.000, 0.000, 0.000, 0.000);
        validateFlowDecomposition(flowDecompositionResults, "b58bf21a-096a-4dae-9a01-3f03b60c24c7", "b58bf21a-096a-4dae-9a01-3f03b60c24c7", "", Country.BE, Country.BE, -116.324, -118.550, -0.000, 126.160, -0.000, -7.610, 0.000, 0.000, 0.000, 0.000);
        validateFlowDecomposition(flowDecompositionResults, "b94318f6-6d24-4f56-96b9-df2531ad6543", "b94318f6-6d24-4f56-96b9-df2531ad6543", "", Country.BE, Country.BE, 0.299, 0.289, -5.505, 76.274, -33.030, -37.450, 0.000, 0.000, 0.000, 0.000);
        validateFlowDecomposition(flowDecompositionResults, "df16b3dd-c905-4a6f-84ee-f067be86f5da", "df16b3dd-c905-4a6f-84ee-f067be86f5da", "", Country.BE, Country.BE, -99.797, -103.741, 0.000, 103.741, -0.000, -0.000, 0.000, 0.000, 0.000, 0.000);
        validateFlowDecomposition(flowDecompositionResults, "e482b89a-fa84-4ea9-8e70-a83d44790957", "e482b89a-fa84-4ea9-8e70-a83d44790957", "", Country.BE, Country.BE, -94.902, -84.583, 14.401, -106.980, -0.000, 177.161, 0.000, 0.000, 0.000, 0.000);
        validateFlowDecomposition(flowDecompositionResults, "ffbabc27-1ccd-4fdc-b037-e341706c8d29", "ffbabc27-1ccd-4fdc-b037-e341706c8d29", "", Country.BE, Country.BE, -53.551, -57.104, -0.000, 60.770, -0.000, -3.666, 0.000, 0.000, 0.000, 0.000);
        assertEquals(1, flowDecompositionResults.getZoneSet().size());
        assertTrue(flowDecompositionResults.getZoneSet().contains(Country.BE));

    }

    @Test
    void testFlowDecompositionOnNetworkWithShuntCompensatorOnly() {
        Network network = TestUtils.getMicroGridNetworkWithShuntCompensatorOnly();

        FlowDecompositionResults flowDecompositionResults = runFlowDecomposition(network, new XnecProviderAllBranches());
        assertEquals(6, flowDecompositionResults.getDecomposedFlowMap().size());
        validateFlowDecomposition(flowDecompositionResults, "a708c3bc-465d-4fe7-b6ef-6fa6408a62b0", "a708c3bc-465d-4fe7-b6ef-6fa6408a62b0", "", Country.BE, Country.BE, 105.189, 115.160, -8.896, 30.705, 33.030, 60.321, 0.000, 0.000, 0.000, 0.000);
        validateFlowDecomposition(flowDecompositionResults, "b58bf21a-096a-4dae-9a01-3f03b60c24c7", "b58bf21a-096a-4dae-9a01-3f03b60c24c7", "", Country.BE, Country.BE, -116.310, -118.537, -0.000, 126.160, -0.000, -7.623, 0.000, 0.000, 0.000, 0.000);
        validateFlowDecomposition(flowDecompositionResults, "b94318f6-6d24-4f56-96b9-df2531ad6543", "b94318f6-6d24-4f56-96b9-df2531ad6543", "", Country.BE, Country.BE, -0.732, 0.248, -5.505, 76.274, -33.030, -37.492, 0.000, 0.000, 0.000, 0.000);
        validateFlowDecomposition(flowDecompositionResults, "df16b3dd-c905-4a6f-84ee-f067be86f5da", "df16b3dd-c905-4a6f-84ee-f067be86f5da", "", Country.BE, Country.BE, -99.793, -103.741, 0.000, 103.741, -0.000, -0.000, 0.000, 0.000, 0.000, 0.000);
        validateFlowDecomposition(flowDecompositionResults, "e482b89a-fa84-4ea9-8e70-a83d44790957", "e482b89a-fa84-4ea9-8e70-a83d44790957", "", Country.BE, Country.BE, -95.912, -84.604, 14.401, -106.980, -0.000, 177.183, 0.000, 0.000, 0.000, 0.000);
        validateFlowDecomposition(flowDecompositionResults, "ffbabc27-1ccd-4fdc-b037-e341706c8d29", "ffbabc27-1ccd-4fdc-b037-e341706c8d29", "", Country.BE, Country.BE, -53.540, -57.098, -0.000, 60.770, -0.000, -3.672, 0.000, 0.000, 0.000, 0.000);
        assertEquals(1, flowDecompositionResults.getZoneSet().size());
        assertTrue(flowDecompositionResults.getZoneSet().contains(Country.BE));

    }

    @Test
    void testFlowDecompositionOnNetworkWithStaticVarCompensatorOnly() {
        Network network = TestUtils.getNetworkWithStaticVarCompensatorOnly();

        FlowDecompositionResults flowDecompositionResults = runFlowDecomposition(network, new XnecProviderAllBranches());
        assertEquals(1, flowDecompositionResults.getDecomposedFlowMap().size());
        validateFlowDecomposition(flowDecompositionResults, "L1", "L1", "", Country.FR, Country.FR, 100.266, 100.000, 0.000, 0.000, 0.000, 100.000, 0.000, 0.000, 0.000, 0.000);
        assertEquals(1, flowDecompositionResults.getZoneSet().size());
        assertTrue(flowDecompositionResults.getZoneSet().contains(Country.FR));
    }

    @Test
    void testFlowDecompositionOnHvdcNetwork() {
        FlowDecompositionResults flowDecompositionResults = testFlowDecompositionOnHvdcNetwork(FlowDecompositionParameters.FlowPartitionMode.MATRIX_BASED);
        validateFlowDecompositionResultsUsingPFCMethodology(flowDecompositionResults);
    }

    @Test
    void testFlowDecompositionOnHvdcNetworkUsingFastMode() {
        FlowDecompositionResults flowDecompositionResults = testFlowDecompositionOnHvdcNetwork(FlowDecompositionParameters.FlowPartitionMode.DIRECT_SENSITIVITY_BASED);
        validateFlowDecompositionResultsUsingPFCMethodology(flowDecompositionResults);
    }

    @Test
    void testFlowDecompositionOnHvdcNetworkUsingFullLineDecomposition() {
        FlowDecompositionResults flowDecompositionResults = testFlowDecompositionOnHvdcNetwork(FlowDecompositionParameters.FlowPartitionMode.FULL_LINE_DECOMPOSITION);
        validateFlowDecompositionResultsUsingFLDMethodology(flowDecompositionResults);
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

    FlowDecompositionResults testFlowDecompositionOnHvdcNetwork(FlowDecompositionParameters.FlowPartitionMode flowPartitionMode) {
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

    private static void validateFlowDecompositionResultsUsingPFCMethodology(FlowDecompositionResults flowDecompositionResults) {
        validateFlowDecomposition(flowDecompositionResults, "BBE1AA11 BBE3AA11 1_contingency_split_network", "BBE1AA11 BBE3AA11 1", "contingency_split_network", Country.BE, Country.BE, -571.575251, -571.428571, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000);
        validateFlowDecomposition(flowDecompositionResults, "FFR2AA11 FFR3AA11 2", "FFR2AA11 FFR3AA11 2", "", Country.FR, Country.FR, -1048.852694, -1044.697128, 418.391705, 0.000000, 38.815350, 611.949424, -16.125814, -4.610042, 0.000000, -3.723495);
        validateFlowDecomposition(flowDecompositionResults, "FFR2AA11 FFR3AA11 1", "FFR2AA11 FFR3AA11 1", "", Country.FR, Country.FR, -1048.852694, -1044.697128, 418.391705, 0.000000, 38.815350, 611.949424, -16.125814, -4.610042, 0.000000, -3.723495);
        validateFlowDecomposition(flowDecompositionResults, "FFR3AA11 FFR5AA11 1", "FFR3AA11 FFR5AA11 1", "", Country.FR, Country.FR, -1012.008034, -1009.730695, 1246.058874, 0.000000, 18.657301, -134.559417, -46.431224, -63.273741, 0.000000, -10.721099);
        validateFlowDecomposition(flowDecompositionResults, "DDE2AA11 DDE3AA11 1_contingency_1", "DDE2AA11 DDE3AA11 1", "contingency_1", Country.DE, Country.DE, -553.509702, -561.186194, 443.113772, 0.000000, -70.550332, 125.748503, 0.000000, 0.000000, 62.874251, 0.000000);
        validateFlowDecomposition(flowDecompositionResults, "FFR4AA11 DDE4AA11 1_contingency_1", "FFR4AA11 DDE4AA11 1", "contingency_1", Country.FR, Country.DE, 148.880291, 146.271269, 518.962076, 0.000000, 23.516777, 0.000000, 0.000000, -375.249501, -20.958084, 0.000000);
        validateFlowDecomposition(flowDecompositionResults, "FFR1AA11 FFR4AA11 1_contingency_split_network", "FFR1AA11 FFR4AA11 1", "contingency_split_network", Country.FR, Country.FR, 871.907722, 889.220358, 431.137725, 0.000000, -161.677845, 706.586826, 0.000000, -86.826347, 0.000000, 0.000000);
        validateFlowDecomposition(flowDecompositionResults, "FFR2AA11 FFR3AA11 2_contingency_split_network", "FFR2AA11 FFR3AA11 2", "contingency_split_network", Country.FR, Country.FR, -625.595532, -622.155928, -86.227545, 0.000000, 32.335569, 658.682635, 0.000000, 17.365269, 0.000000, 0.000000);
        validateFlowDecomposition(flowDecompositionResults, "DDE1AA11 DDE4AA11 1_contingency_1", "DDE1AA11 DDE4AA11 1", "contingency_1", Country.DE, Country.DE, -648.880291, -646.271269, 118.962076, 0.000000, 23.516777, 524.750499, 0.000000, 0.000000, -20.958084, 0.000000);
        validateFlowDecomposition(flowDecompositionResults, "NNL1AA11 NNL2AA11 1_contingency_1", "NNL1AA11 NNL2AA11 1", "contingency_1", Country.NL, Country.NL, 166.672696, 166.666667, -208.333333, 0.000000, -0.000000, 375.000000, 0.000000, 0.000000, 0.000000, 0.000000);
        validateFlowDecomposition(flowDecompositionResults, "NNL2AA11 BBE3AA11 1_contingency_1", "NNL2AA11 BBE3AA11 1", "contingency_1", Country.NL, Country.BE, -500.000000, -500.000000, 500.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000);
        validateFlowDecomposition(flowDecompositionResults, "NNL1AA11 NNL2AA11 1_contingency_desync", "NNL1AA11 NNL2AA11 1", "contingency_desync", Country.NL, Country.NL, 166.672696, 166.666667, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000);
        validateFlowDecomposition(flowDecompositionResults, "FFR1AA11 FFR4AA11 1", "FFR1AA11 FFR4AA11 1", "", Country.FR, Country.FR, 779.213147, 795.975751, 400.159224, 0.000000, -156.762149, 671.134046, -12.233376, -103.497273, 0.000000, -2.824721);
        validateFlowDecomposition(flowDecompositionResults, "FFR3AA11 FFR5AA11 1_contingency_1", "FFR3AA11 FFR5AA11 1", "contingency_1", Country.FR, Country.FR, -2000.000000, -2000.000000, 2000.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000);
        validateFlowDecomposition(flowDecompositionResults, "NNL1AA11 NNL2AA11 1", "NNL1AA11 NNL2AA11 1", "", Country.NL, Country.NL, -162.721962, -163.423102, 459.647042, 0.000000, -6.219100, -371.426300, 15.477075, 21.091247, 44.853139, 0.000000);
        validateFlowDecomposition(flowDecompositionResults, "FFR1AA11 FFR5AA11 1_contingency_desync", "FFR1AA11 FFR5AA11 1", "contingency_desync", Country.FR, Country.FR, NaN, NaN, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000);
        validateFlowDecomposition(flowDecompositionResults, "DDE1AA11 DDE4AA11 1_contingency_split_network", "DDE1AA11 DDE4AA11 1", "contingency_split_network", Country.DE, Country.DE, -636.886698, -634.295221, 106.986028, 0.000000, 23.516777, 524.750499, 0.000000, 0.000000, -20.958084, 0.000000);
        validateFlowDecomposition(flowDecompositionResults, "FFR4AA11 DDE1AA11 1", "FFR4AA11 DDE1AA11 1", "", Country.FR, Country.DE, 478.144120, 472.335696, 394.134806, 0.000000, 53.066455, 0.000000, -15.013689, 129.041225, -85.426398, -3.466703);
        validateFlowDecomposition(flowDecompositionResults, "NNL2AA11 BBE3AA11 1", "NNL2AA11 BBE3AA11 1", "", Country.NL, Country.BE, -1487.991966, -1490.269305, 1253.941126, 0.000000, -18.657301, 0.000000, 46.431224, 63.273741, 134.559417, 10.721099);
        validateFlowDecomposition(flowDecompositionResults, "BBE2AA11 BBE3AA11 1", "BBE2AA11 BBE3AA11 1", "", Country.BE, Country.BE, 926.977948, 927.181329, 423.495349, 0.000000, -2.665329, 476.557844, 0.000000, 9.039106, 19.222774, 1.531586);
        validateFlowDecomposition(flowDecompositionResults, "DDE1AA11 DDE2AA11 1_contingency_desync", "DDE1AA11 DDE2AA11 1", "contingency_desync", Country.DE, Country.DE, 410.528297, 402.885662, 54.291417, 0.000000, 70.550332, 340.918164, 0.000000, 0.000000, -62.874251, 0.000000);
        validateFlowDecomposition(flowDecompositionResults, "NNL1AA11 NNL3AA11 1_contingency_1", "NNL1AA11 NNL3AA11 1", "contingency_1", Country.NL, Country.NL, 333.327304, 333.333333, 20.833333, 0.000000, -0.000000, 312.500000, 0.000000, 0.000000, 0.000000, 0.000000);
        validateFlowDecomposition(flowDecompositionResults, "FFR4AA11 DDE4AA11 1", "FFR4AA11 DDE4AA11 1", "", Country.FR, Country.DE, -10.930437, -13.832152, -397.067403, 0.000000, -26.533227, 0.000000, 7.506845, 385.479387, 42.713199, 1.733351);
        validateFlowDecomposition(flowDecompositionResults, "FFR1AA11 FFR5AA11 1_contingency_split_network", "FFR1AA11 FFR5AA11 1", "contingency_split_network", Country.FR, Country.FR, NaN, NaN, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000);
        validateFlowDecomposition(flowDecompositionResults, "NNL2AA11 NNL3AA11 1_contingency_desync", "NNL2AA11 NNL3AA11 1", "contingency_desync", Country.NL, Country.NL, 166.672696, 166.666667, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000);
        validateFlowDecomposition(flowDecompositionResults, "BBE1AA11 BBE2AA11 1", "BBE1AA11 BBE2AA11 1", "", Country.BE, Country.BE, -1073.022052, -1072.818671, 365.978335, 0.000000, 2.665329, 733.968472, 0.000000, -9.039106, -19.222774, -1.531586);
        validateFlowDecomposition(flowDecompositionResults, "FFR4AA11 DDE4AA11 1_contingency_split_network", "FFR4AA11 DDE4AA11 1", "contingency_split_network", Country.FR, Country.DE, 136.886698, 134.295221, 506.986028, 0.000000, 23.516777, 0.000000, 0.000000, -375.249501, -20.958084, 0.000000);
        validateFlowDecomposition(flowDecompositionResults, "NNL2AA11 BBE3AA11 1_contingency_desync", "NNL2AA11 BBE3AA11 1", "contingency_desync", Country.NL, Country.BE, -500.000000, -500.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000);
        validateFlowDecomposition(flowDecompositionResults, "FFR2AA11 DDE3AA11 1", "FFR2AA11 DDE3AA11 1", "", Country.FR, Country.DE, 544.794351, 551.227151, 454.856666, 0.000000, -60.942381, 0.000000, -23.910690, 193.164421, -6.419820, -5.521045);
        validateFlowDecomposition(flowDecompositionResults, "BBE1AA11 BBE2AA11 1_contingency_split_network", "BBE1AA11 BBE2AA11 1", "contingency_split_network", Country.BE, Country.BE, -1285.274247, -1285.714286, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000);
        validateFlowDecomposition(flowDecompositionResults, "DDE1AA11 DDE2AA11 1_contingency_1", "DDE1AA11 DDE2AA11 1", "contingency_1", Country.DE, Country.DE, 446.490298, 438.813806, 90.219561, 0.000000, 70.550332, 340.918164, 0.000000, 0.000000, -62.874251, 0.000000);
        validateFlowDecomposition(flowDecompositionResults, "FFR2AA11 FFR4AA11 1_contingency_split_network", "FFR2AA11 FFR4AA11 1", "contingency_split_network", Country.FR, Country.FR, 1038.620574, 1013.665303, 689.820359, 0.000000, 232.228178, 230.538922, 0.000000, -138.922156, 0.000000, 0.000000);
        validateFlowDecomposition(flowDecompositionResults, "FFR2AA11 DDE3AA11 1_contingency_desync", "FFR2AA11 DDE3AA11 1", "contingency_desync", Country.FR, Country.DE, 1089.471702, 1097.114338, 879.041916, 0.000000, -70.550332, 0.000000, 0.000000, 225.748503, 62.874251, 0.000000);
        validateFlowDecomposition(flowDecompositionResults, "FFR1AA11 FFR3AA11 1", "FFR1AA11 FFR3AA11 1", "", Country.FR, Country.FR, -414.302646, -420.336439, 409.275465, 0.000000, -58.973399, 141.541735, -14.179595, -54.053657, 0.000000, -3.274108);
        validateFlowDecomposition(flowDecompositionResults, "FFR2AA11 DDE3AA11 1_contingency_1", "FFR2AA11 DDE3AA11 1", "contingency_1", Country.FR, Country.DE, 1053.509702, 1061.186194, 843.113772, 0.000000, -70.550332, 0.000000, 0.000000, 225.748503, 62.874251, 0.000000);
        validateFlowDecomposition(flowDecompositionResults, "DDE1AA11 DDE4AA11 1", "DDE1AA11 DDE4AA11 1", "", Country.DE, Country.DE, -489.069563, -486.167848, -2.932597, 0.000000, 26.533227, 514.520613, -7.506845, 0.000000, -42.713199, -1.733351);
        validateFlowDecomposition(flowDecompositionResults, "NNL1AA11 NNL3AA11 1_contingency_desync", "NNL1AA11 NNL3AA11 1", "contingency_desync", Country.NL, Country.NL, 333.327304, 333.333333, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000);
        validateFlowDecomposition(flowDecompositionResults, "DDE2AA11 DDE3AA11 1_contingency_desync", "DDE2AA11 DDE3AA11 1", "contingency_desync", Country.DE, Country.DE, -589.471702, -597.114338, 479.041916, 0.000000, -70.550332, 125.748503, 0.000000, 0.000000, 62.874251, 0.000000);
        validateFlowDecomposition(flowDecompositionResults, "BBE2AA11 BBE3AA11 1_contingency_split_network", "BBE2AA11 BBE3AA11 1", "contingency_split_network", Country.BE, Country.BE, 714.725753, 714.285714, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000);
        validateFlowDecomposition(flowDecompositionResults, "NNL1AA11 NNL3AA11 1_contingency_split_network", "NNL1AA11 NNL3AA11 1", "contingency_split_network", Country.NL, Country.NL, NaN, NaN, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000);
        validateFlowDecomposition(flowDecompositionResults, "BBE1AA11 BBE3AA11 2_contingency_desync", "BBE1AA11 BBE3AA11 2", "contingency_desync", Country.BE, Country.BE, -428.685692, -428.571429, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000);
        validateFlowDecomposition(flowDecompositionResults, "BBE1AA11 BBE2AA11 1_contingency_desync", "BBE1AA11 BBE2AA11 1", "contingency_desync", Country.BE, Country.BE, -1213.942924, -1214.285714, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000);
        validateFlowDecomposition(flowDecompositionResults, "FFR2AA11 FFR3AA11 2_contingency_desync", "FFR2AA11 FFR3AA11 2", "contingency_desync", Country.FR, Country.FR, -625.595532, -622.155928, -86.227545, 0.000000, 32.335569, 658.682635, 0.000000, 17.365269, 0.000000, 0.000000);
        validateFlowDecomposition(flowDecompositionResults, "BBE1AA11 FFR5AA11 1_contingency_desync", "BBE1AA11 FFR5AA11 1", "contingency_desync", Country.BE, Country.FR, 0.000000, -0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000);
        validateFlowDecomposition(flowDecompositionResults, "BBE3AA11 BBE4AA11 1_contingency_1", "BBE3AA11 BBE4AA11 1", "contingency_1", Country.BE, Country.BE, 428.685692, 428.571429, 157.894737, 0.000000, 0.000000, 270.676692, 0.000000, 0.000000, 0.000000, -0.000000);
        validateFlowDecomposition(flowDecompositionResults, "BBE1AA11 BBE3AA11 1_contingency_desync", "BBE1AA11 BBE3AA11 1", "contingency_desync", Country.BE, Country.BE, -428.685692, -428.571429, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000);
        validateFlowDecomposition(flowDecompositionResults, "FFR2AA11 FFR4AA11 1_contingency_desync", "FFR2AA11 FFR4AA11 1", "contingency_desync", Country.FR, Country.FR, 1038.620574, 1013.665303, 689.820359, 0.000000, 232.228178, 230.538922, 0.000000, -138.922156, 0.000000, 0.000000);
        validateFlowDecomposition(flowDecompositionResults, "DDE1AA11 DDE2AA11 1_contingency_split_network", "DDE1AA11 DDE2AA11 1", "contingency_split_network", Country.DE, Country.DE, 410.528297, 402.885662, 54.291417, 0.000000, 70.550332, 340.918164, 0.000000, 0.000000, -62.874251, 0.000000);
        validateFlowDecomposition(flowDecompositionResults, "FFR2AA11 FFR3AA11 1_contingency_desync", "FFR2AA11 FFR3AA11 1", "contingency_desync", Country.FR, Country.FR, -625.595532, -622.155928, -86.227545, 0.000000, 32.335569, 658.682635, 0.000000, 17.365269, 0.000000, 0.000000);
        validateFlowDecomposition(flowDecompositionResults, "FFR2AA11 FFR3AA11 1_contingency_split_network", "FFR2AA11 FFR3AA11 1", "contingency_split_network", Country.FR, Country.FR, -625.595532, -622.155928, -86.227545, 0.000000, 32.335569, 658.682635, 0.000000, 17.365269, 0.000000, 0.000000);
        validateFlowDecomposition(flowDecompositionResults, "BBE3AA11 BBE4AA11 1", "BBE3AA11 BBE4AA11 1", "", Country.BE, Country.BE, 146.328661, 145.637341, -57.517013, 0.000000, 5.330657, 257.410628, 0.000000, -18.078212, -38.445548, -3.063171);
        validateFlowDecomposition(flowDecompositionResults, "FFR1AA11 FFR2AA11 1_contingency_1", "FFR1AA11 FFR2AA11 1", "contingency_1", Country.FR, Country.FR, 676.752713, 665.868983, 40.718563, 0.000000, 97.006707, 476.047904, 0.000000, 52.095808, 0.000000, 0.000000);
        validateFlowDecomposition(flowDecompositionResults, "BBE2AA11 BBE3AA11 1_contingency_1", "BBE2AA11 BBE3AA11 1", "contingency_1", Country.BE, Country.BE, 786.057076, 785.714286, 315.789474, 0.000000, 0.000000, 469.924812, 0.000000, 0.000000, 0.000000, 0.000000);
        validateFlowDecomposition(flowDecompositionResults, "FFR1AA11 FFR3AA11 1_contingency_desync", "FFR1AA11 FFR3AA11 1", "contingency_desync", Country.FR, Country.FR, -248.808935, -255.688143, 172.455090, 0.000000, -64.671138, 182.634731, 0.000000, -34.730539, 0.000000, 0.000000);
        validateFlowDecomposition(flowDecompositionResults, "BBE1AA11 BBE3AA11 2_contingency_split_network", "BBE1AA11 BBE3AA11 2", "contingency_split_network", Country.BE, Country.BE, -571.575251, -571.428571, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000);
        validateFlowDecomposition(flowDecompositionResults, "DDE2AA11 DDE3AA11 1_contingency_split_network", "DDE2AA11 DDE3AA11 1", "contingency_split_network", Country.DE, Country.DE, -589.471702, -597.114338, 479.041916, 0.000000, -70.550332, 125.748503, 0.000000, 0.000000, 62.874251, 0.000000);
        validateFlowDecomposition(flowDecompositionResults, "FFR2AA11 DDE3AA11 1_contingency_split_network", "FFR2AA11 DDE3AA11 1", "contingency_split_network", Country.FR, Country.DE, 1089.471702, 1097.114338, 879.041916, 0.000000, -70.550332, 0.000000, 0.000000, 225.748503, 62.874251, 0.000000);
        validateFlowDecomposition(flowDecompositionResults, "BBE1AA11 BBE3AA11 2", "BBE1AA11 BBE3AA11 2", "", Country.BE, Country.BE, -146.328661, -145.637341, -57.517013, 0.000000, 5.330657, 257.410628, 0.000000, -18.078212, -38.445548, -3.063171);
        validateFlowDecomposition(flowDecompositionResults, "BBE1AA11 FFR5AA11 1", "BBE1AA11 FFR5AA11 1", "", Country.BE, Country.FR, 506.004017, 504.865347, 623.029437, 0.000000, 9.328651, 0.000000, -23.215612, -31.636871, -67.279708, -5.360549);
        validateFlowDecomposition(flowDecompositionResults, "BBE1AA11 BBE3AA11 1", "BBE1AA11 BBE3AA11 1", "", Country.BE, Country.BE, -146.328661, -145.637341, -57.517013, 0.000000, 5.330657, 257.410628, 0.000000, -18.078212, -38.445548, -3.063171);
        validateFlowDecomposition(flowDecompositionResults, "NNL2AA11 NNL3AA11 1_contingency_split_network", "NNL2AA11 NNL3AA11 1", "contingency_split_network", Country.NL, Country.NL, NaN, NaN, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000);
        validateFlowDecomposition(flowDecompositionResults, "BBE4AA11 FFR5AA11 1_contingency_split_network", "BBE4AA11 FFR5AA11 1", "contingency_split_network", Country.BE, Country.FR, 0.000000, -0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000);
        validateFlowDecomposition(flowDecompositionResults, "BBE2AA11 BBE3AA11 1_contingency_desync", "BBE2AA11 BBE3AA11 1", "contingency_desync", Country.BE, Country.BE, 786.057076, 785.714286, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000);
        validateFlowDecomposition(flowDecompositionResults, "BBE1AA11 BBE3AA11 1_contingency_1", "BBE1AA11 BBE3AA11 1", "contingency_1", Country.BE, Country.BE, -428.685692, -428.571429, 157.894737, 0.000000, 0.000000, 270.676692, 0.000000, 0.000000, 0.000000, -0.000000);
        validateFlowDecomposition(flowDecompositionResults, "BBE1AA11 FFR5AA11 1_contingency_split_network", "BBE1AA11 FFR5AA11 1", "contingency_split_network", Country.BE, Country.FR, 0.000000, -0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000);
        validateFlowDecomposition(flowDecompositionResults, "FFR4AA11 DDE1AA11 1_contingency_desync", "FFR4AA11 DDE1AA11 1", "contingency_desync", Country.FR, Country.DE, 773.641598, 768.590441, 613.972056, 0.000000, 47.033555, 0.000000, 0.000000, 149.500998, -41.916168, 0.000000);
        validateFlowDecomposition(flowDecompositionResults, "BBE1AA11 BBE2AA11 1_contingency_1", "BBE1AA11 BBE2AA11 1", "contingency_1", Country.BE, Country.BE, -1213.942924, -1214.285714, 473.684211, 0.000000, 0.000000, 740.601504, 0.000000, 0.000000, 0.000000, -0.000000);
        validateFlowDecomposition(flowDecompositionResults, "FFR1AA11 FFR3AA11 1_contingency_1", "FFR1AA11 FFR3AA11 1", "contingency_1", Country.FR, Country.FR, -716.378043, -722.754012, 639.520958, 0.000000, -64.671138, 182.634731, 0.000000, -34.730539, 0.000000, 0.000000);
        validateFlowDecomposition(flowDecompositionResults, "DDE1AA11 DDE2AA11 1", "DDE1AA11 DDE2AA11 1", "", Country.DE, Country.DE, -32.786317, -41.496456, 275.464458, 0.000000, -79.599682, -310.228505, 22.520534, 0.000000, 128.139597, 5.200054);
        validateFlowDecomposition(flowDecompositionResults, "FFR1AA11 FFR2AA11 1", "FFR1AA11 FFR2AA11 1", "", Country.FR, Country.FR, 635.089500, 624.360688, 9.116240, 0.000000, 97.788750, 470.407689, -1.946219, 49.443616, 0.000000, -0.449387);
        validateFlowDecomposition(flowDecompositionResults, "FFR4AA11 DDE1AA11 1_contingency_split_network", "FFR4AA11 DDE1AA11 1", "contingency_split_network", Country.FR, Country.DE, 773.641598, 768.590441, 613.972056, 0.000000, 47.033555, 0.000000, 0.000000, 149.500998, -41.916168, 0.000000);
        validateFlowDecomposition(flowDecompositionResults, "NNL2AA11 NNL3AA11 1", "NNL2AA11 NNL3AA11 1", "", Country.NL, Country.NL, 825.270004, 826.846203, 731.794084, 0.000000, -12.438201, -55.352601, 30.954149, 42.182494, 89.706278, 0.000000);
        validateFlowDecomposition(flowDecompositionResults, "BBE4AA11 FFR5AA11 1_contingency_desync", "BBE4AA11 FFR5AA11 1", "contingency_desync", Country.BE, Country.FR, 0.000000, -0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000);
        validateFlowDecomposition(flowDecompositionResults, "DDE1AA11 DDE4AA11 1_contingency_desync", "DDE1AA11 DDE4AA11 1", "contingency_desync", Country.DE, Country.DE, -636.886698, -634.295221, 106.986028, 0.000000, 23.516777, 524.750499, 0.000000, 0.000000, -20.958084, 0.000000);
        validateFlowDecomposition(flowDecompositionResults, "NNL1AA11 NNL2AA11 1_contingency_split_network", "NNL1AA11 NNL2AA11 1", "contingency_split_network", Country.NL, Country.NL, NaN, NaN, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000);
        validateFlowDecomposition(flowDecompositionResults, "FFR4AA11 DDE4AA11 1_contingency_desync", "FFR4AA11 DDE4AA11 1", "contingency_desync", Country.FR, Country.DE, 136.886698, 134.295221, 506.986028, 0.000000, 23.516777, 0.000000, 0.000000, -375.249501, -20.958084, 0.000000);
        validateFlowDecomposition(flowDecompositionResults, "FFR1AA11 FFR5AA11 1", "FFR1AA11 FFR5AA11 1", "", Country.FR, Country.FR, NaN, NaN, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000);
        validateFlowDecomposition(flowDecompositionResults, "BBE3AA11 BBE4AA11 1_contingency_desync", "BBE3AA11 BBE4AA11 1", "contingency_desync", Country.BE, Country.BE, 428.685692, 428.571429, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000);
        validateFlowDecomposition(flowDecompositionResults, "BBE1AA11 FFR5AA11 1_contingency_1", "BBE1AA11 FFR5AA11 1", "contingency_1", Country.BE, Country.FR, 1000.000000, 1000.000000, 1000.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000);
        validateFlowDecomposition(flowDecompositionResults, "NNL1AA11 NNL3AA11 1", "NNL1AA11 NNL3AA11 1", "", Country.NL, Country.NL, 662.721962, 663.423102, 272.147042, 0.000000, -6.219100, 316.073700, 15.477075, 21.091247, 44.853139, 0.000000);
        validateFlowDecomposition(flowDecompositionResults, "FFR4AA11 DDE1AA11 1_contingency_1", "FFR4AA11 DDE1AA11 1", "contingency_1", Country.FR, Country.DE, 797.610007, 792.542537, 637.924152, 0.000000, 47.033555, 0.000000, 0.000000, 149.500998, -41.916168, 0.000000);
        validateFlowDecomposition(flowDecompositionResults, "FFR1AA11 FFR2AA11 1_contingency_desync", "FFR1AA11 FFR2AA11 1", "contingency_desync", Country.FR, Country.FR, 376.901212, 366.467785, -258.682635, 0.000000, 97.006707, 476.047904, 0.000000, 52.095808, 0.000000, 0.000000);
        validateFlowDecomposition(flowDecompositionResults, "FFR1AA11 FFR4AA11 1_contingency_desync", "FFR1AA11 FFR4AA11 1", "contingency_desync", Country.FR, Country.FR, 871.907722, 889.220358, 431.137725, 0.000000, -161.677845, 706.586826, 0.000000, -86.826347, 0.000000, 0.000000);
        validateFlowDecomposition(flowDecompositionResults, "FFR2AA11 FFR4AA11 1_contingency_1", "FFR2AA11 FFR4AA11 1", "contingency_1", Country.FR, Country.FR, 906.864968, 881.928776, 558.083832, 0.000000, 232.228178, 230.538922, 0.000000, -138.922156, 0.000000, 0.000000);
        validateFlowDecomposition(flowDecompositionResults, "FFR2AA11 FFR3AA11 2_contingency_1", "FFR2AA11 FFR3AA11 2", "contingency_1", Country.FR, Country.FR, -1391.810978, -1388.622994, 680.239521, 0.000000, 32.335569, 658.682635, 0.000000, 17.365269, 0.000000, 0.000000);
        validateFlowDecomposition(flowDecompositionResults, "NNL2AA11 NNL3AA11 1_contingency_1", "NNL2AA11 NNL3AA11 1", "contingency_1", Country.NL, Country.NL, 166.672696, 166.666667, 229.166667, 0.000000, -0.000000, -62.500000, -0.000000, 0.000000, 0.000000, 0.000000);
        validateFlowDecomposition(flowDecompositionResults, "FFR1AA11 FFR2AA11 1_contingency_split_network", "FFR1AA11 FFR2AA11 1", "contingency_split_network", Country.FR, Country.FR, 376.901212, 366.467785, -258.682635, 0.000000, 97.006707, 476.047904, 0.000000, 52.095808, 0.000000, 0.000000);
        validateFlowDecomposition(flowDecompositionResults, "FFR2AA11 FFR3AA11 1_contingency_1", "FFR2AA11 FFR3AA11 1", "contingency_1", Country.FR, Country.FR, -1391.810978, -1388.622994, 680.239521, 0.000000, 32.335569, 658.682635, 0.000000, 17.365269, 0.000000, 0.000000);
        validateFlowDecomposition(flowDecompositionResults, "FFR1AA11 FFR4AA11 1_contingency_1", "FFR1AA11 FFR4AA11 1", "contingency_1", Country.FR, Country.FR, 1039.625330, 1056.885029, 598.802395, 0.000000, -161.677845, 706.586826, 0.000000, -86.826347, 0.000000, 0.000000);
        validateFlowDecomposition(flowDecompositionResults, "BBE4AA11 FFR5AA11 1_contingency_1", "BBE4AA11 FFR5AA11 1", "contingency_1", Country.BE, Country.FR, 1000.000000, 1000.000000, 1000.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000);
        validateFlowDecomposition(flowDecompositionResults, "DDE2AA11 DDE3AA11 1", "DDE2AA11 DDE3AA11 1", "", Country.DE, Country.DE, -44.794351, -51.227151, 54.856666, 0.000000, -60.942381, 93.164421, -23.910690, 0.000000, -6.419820, -5.521045);
        validateFlowDecomposition(flowDecompositionResults, "BBE3AA11 BBE4AA11 1_contingency_split_network", "BBE3AA11 BBE4AA11 1", "contingency_split_network", Country.BE, Country.BE, 571.575251, 571.428571, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000);
        validateFlowDecomposition(flowDecompositionResults, "DDE2AA11 NNL3AA11 1", "DDE2AA11 NNL3AA11 1", "", Country.DE, Country.NL, -987.991966, -990.269305, 753.941126, 0.000000, -18.657301, 0.000000, 46.431224, 63.273741, 134.559417, 10.721099);
        validateFlowDecomposition(flowDecompositionResults, "FFR2AA11 FFR4AA11 1", "FFR2AA11 FFR4AA11 1", "", Country.FR, Country.FR, 688.000536, 662.527793, 391.042984, 0.000000, 236.361831, 200.726357, -10.287157, -152.940889, 0.000000, -2.375333);
        validateFlowDecomposition(flowDecompositionResults, "BBE4AA11 FFR5AA11 1", "BBE4AA11 FFR5AA11 1", "", Country.BE, Country.FR, 506.004017, 504.865347, 623.029437, 0.000000, 9.328651, 0.000000, -23.215612, -31.636871, -67.279708, -5.360549);
        validateFlowDecomposition(flowDecompositionResults, "FFR1AA11 FFR3AA11 1_contingency_split_network", "FFR1AA11 FFR3AA11 1", "contingency_split_network", Country.FR, Country.FR, -248.808935, -255.688143, 172.455090, 0.000000, -64.671138, 182.634731, 0.000000, -34.730539, 0.000000, 0.000000);
        validateFlowDecomposition(flowDecompositionResults, "BBE1AA11 BBE3AA11 2_contingency_1", "BBE1AA11 BBE3AA11 2", "contingency_1", Country.BE, Country.BE, -428.685692, -428.571429, 157.894737, 0.000000, 0.000000, 270.676692, 0.000000, 0.000000, 0.000000, -0.000000);
        validateFlowDecomposition(flowDecompositionResults, "FFR1AA11 FFR5AA11 1_contingency_1", "FFR1AA11 FFR5AA11 1", "contingency_1", Country.FR, Country.FR, NaN, NaN, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000);
        assertEquals(4, flowDecompositionResults.getZoneSet().size());
        assertTrue(flowDecompositionResults.getZoneSet().contains(Country.BE));
        assertTrue(flowDecompositionResults.getZoneSet().contains(Country.DE));
        assertTrue(flowDecompositionResults.getZoneSet().contains(Country.FR));
        assertTrue(flowDecompositionResults.getZoneSet().contains(Country.NL));
    }

    private void validateFlowDecompositionResultsUsingFLDMethodology(FlowDecompositionResults flowDecompositionResults) {
        validateFlowDecomposition(flowDecompositionResults, "BBE1AA11 BBE3AA11 1_contingency_split_network", "BBE1AA11 BBE3AA11 1", "contingency_split_network", Country.BE, Country.BE, -571.5752509641023, -571.4285714285714, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
        validateFlowDecomposition(flowDecompositionResults, "FFR2AA11 FFR3AA11 2", "FFR2AA11 FFR3AA11 2", "", Country.FR, Country.FR, -1048.852693794893, -1044.697127740711, 579.1943093103146, 0.0, 38.815350245540955, 430.03829853807946, -4.894443572921734, -1.7877715206457416, 0.0, 3.331384740343645);
        validateFlowDecomposition(flowDecompositionResults, "FFR3AA11 FFR5AA11 1", "FFR3AA11 FFR5AA11 1", "", Country.FR, Country.FR, -1012.0080338734653, -1009.7306948682542, 1107.5068522689166, 0.0, 18.657301045954444, -98.32666587696284, -14.092622011688483, -13.606261103437806, 0.0, 9.592090545472212);
        validateFlowDecomposition(flowDecompositionResults, "FFR2AA11 FFR3AA11 1", "FFR2AA11 FFR3AA11 1", "", Country.FR, Country.FR, -1048.852693794893, -1044.697127740711, 579.1943093103146, 0.0, 38.815350245540955, 430.03829853807946, -4.894443572921734, -1.7877715206457416, 0.0, 3.331384740343645);
        validateFlowDecomposition(flowDecompositionResults, "DDE2AA11 DDE3AA11 1_contingency_1", "DDE2AA11 DDE3AA11 1", "contingency_1", Country.DE, Country.DE, -553.5097016453959, -561.1861944865753, 509.43387244587194, 0.0, -70.55033245953159, 74.53394755021665, 0.0, 0.0, 47.76870695001772, 0.0);
        validateFlowDecomposition(flowDecompositionResults, "FFR4AA11 DDE4AA11 1_contingency_1", "FFR4AA11 DDE4AA11 1", "contingency_1", Country.FR, Country.DE, 148.8802909596293, 146.2712685044741, 330.18870918470816, 0.0, 23.51677748651053, -15.922902316672761, 0.0, -191.51131585007226, 0.0, 0.0);
        validateFlowDecomposition(flowDecompositionResults, "FFR1AA11 FFR4AA11 1_contingency_split_network", "FFR1AA11 FFR4AA11 1", "contingency_split_network", Country.FR, Country.FR, 871.9077227112757, 889.2203583730545, 601.425450228772, 0.0, -161.6778452197599, 477.34027733477615, 0.0, -27.86752397073389, 0.0, 0.0);
        validateFlowDecomposition(flowDecompositionResults, "FFR2AA11 FFR3AA11 2_contingency_split_network", "FFR2AA11 FFR3AA11 2", "contingency_split_network", Country.FR, Country.FR, -625.5955323508144, -622.1559283253891, 419.3492099929259, 0.0, 32.33556904395198, 164.8976444943644, 0.0, 5.573504794146785, 0.0, 0.0);
        validateFlowDecomposition(flowDecompositionResults, "DDE1AA11 DDE4AA11 1_contingency_1", "DDE1AA11 DDE4AA11 1", "contingency_1", Country.DE, Country.DE, -648.8802909595084, -646.2712685044742, 330.18870918470816, 0.0, 23.516777486510552, 308.4886841499281, 0.0, 0.0, -15.922902316672761, 0.0);
        validateFlowDecomposition(flowDecompositionResults, "NNL1AA11 NNL2AA11 1_contingency_1", "NNL1AA11 NNL2AA11 1", "contingency_1", Country.NL, Country.NL, 166.67269564801944, 166.66666666666669, -41.66666666666657, 0.0, 0.0, 208.3333333333334, 2.0164772629449615E-14, 0.0, 0.0, 0.0);
        validateFlowDecomposition(flowDecompositionResults, "NNL2AA11 BBE3AA11 1_contingency_1", "NNL2AA11 BBE3AA11 1", "contingency_1", Country.NL, Country.BE, -499.99999999999096, -499.9999999999999, 499.99999999999994, 0.0, 0.0, 0.0, 2.0164772629449615E-14, 0.0, 0.0, 0.0);
        validateFlowDecomposition(flowDecompositionResults, "NNL1AA11 NNL2AA11 1_contingency_desync", "NNL1AA11 NNL2AA11 1", "contingency_desync", Country.NL, Country.NL, 166.67269564801956, 166.66666666666669, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
        validateFlowDecomposition(flowDecompositionResults, "FFR1AA11 FFR4AA11 1", "FFR1AA11 FFR4AA11 1", "", Country.FR, Country.FR, 779.2131467471844, 795.9757510329534, 477.9623712120201, 0.0, -156.76214913579582, 494.2349623299181, -3.713026158768246, -18.27366460364711, 0.0, 2.527257389226206);
        validateFlowDecomposition(flowDecompositionResults, "FFR3AA11 FFR5AA11 1_contingency_1", "FFR3AA11 FFR5AA11 1", "contingency_1", Country.FR, Country.FR, -1999.9999998626677, -1999.9999999999989, 1999.9999999999984, 0.0, 2.1625689497089173E-13, -1.6459173134991253E-13, 0.0, -6.023242382732863E-14, 0.0, 0.0);
        validateFlowDecomposition(flowDecompositionResults, "NNL1AA11 NNL2AA11 1", "NNL1AA11 NNL2AA11 1", "", Country.NL, Country.NL, -162.721961706112, -163.42310171058185, 186.74935410736904, 0.0, -6.2191003486514616, -59.11566837883191, 4.697540670562837, 4.535420367812604, 32.77555529232082, 0.0);
        validateFlowDecomposition(flowDecompositionResults, "FFR1AA11 FFR5AA11 1_contingency_desync", "FFR1AA11 FFR5AA11 1", "contingency_desync", Country.FR, Country.FR, NaN, NaN, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
        validateFlowDecomposition(flowDecompositionResults, "DDE1AA11 DDE4AA11 1_contingency_split_network", "DDE1AA11 DDE4AA11 1", "contingency_split_network", Country.DE, Country.DE, -636.8866986704637, -634.295220600283, 338.92980122163874, 0.0, 23.516777486510552, 309.18147922536394, 0.0, 0.0, -37.332837333230245, 0.0);
        validateFlowDecomposition(flowDecompositionResults, "FFR4AA11 DDE1AA11 1", "FFR4AA11 DDE1AA11 1", "", Country.FR, Country.DE, 478.1441198616001, 472.33569582862367, 355.183855762433, 0.0, 53.06645471243147, -68.13426915918674, -4.556895740306442, 133.67491618465672, 0.0, 3.1016340685958013);
        validateFlowDecomposition(flowDecompositionResults, "NNL2AA11 BBE3AA11 1", "NNL2AA11 BBE3AA11 1", "", Country.NL, Country.BE, -1487.9919661102651, -1490.2693051317456, 1392.4931477310834, 0.0, -18.657301045954384, -9.592090545472189, 14.092622011688515, 13.606261103437806, 98.32666587696255, 0.0);
        validateFlowDecomposition(flowDecompositionResults, "BBE2AA11 BBE3AA11 1", "BBE2AA11 BBE3AA11 1", "", Country.BE, Country.BE, 926.9779480958824, 927.181329304535, 741.8321955760288, 0.0, -2.6653287208506264, 173.3943429586529, 0.0, 1.943751586205397, 14.046666553851782, -1.370298649353168);
        validateFlowDecomposition(flowDecompositionResults, "DDE1AA11 DDE2AA11 1_contingency_desync", "DDE1AA11 DDE2AA11 1", "contingency_desync", Country.DE, Country.DE, 410.52829754570706, 402.885661800849, 373.19763679074543, 0.0, 70.55033245953157, 71.13620455026275, 0.0, 0.0, -111.99851199969083, 0.0);
        validateFlowDecomposition(flowDecompositionResults, "NNL1AA11 NNL3AA11 1_contingency_1", "NNL1AA11 NNL3AA11 1", "contingency_1", Country.NL, Country.NL, 333.3273043519803, 333.3333333333333, 41.66666666666667, 0.0, 0.0, 291.66666666666674, 0.0, 0.0, 0.0, 0.0);
        validateFlowDecomposition(flowDecompositionResults, "FFR4AA11 DDE4AA11 1", "FFR4AA11 DDE4AA11 1", "", Country.FR, Country.DE, -10.930436768705455, -13.832152085687788, -172.89457790699646, 0.0, -26.533227356215736, 34.067134579593436, 2.2784478701532223, 178.46519193345165, 0.0, -1.5508170342979006);
        validateFlowDecomposition(flowDecompositionResults, "FFR1AA11 FFR5AA11 1_contingency_split_network", "FFR1AA11 FFR5AA11 1", "contingency_split_network", Country.FR, Country.FR, NaN, NaN, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
        validateFlowDecomposition(flowDecompositionResults, "NNL2AA11 NNL3AA11 1_contingency_desync", "NNL2AA11 NNL3AA11 1", "contingency_desync", Country.NL, Country.NL, 166.67269564801956, 166.66666666666666, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
        validateFlowDecomposition(flowDecompositionResults, "BBE1AA11 BBE2AA11 1", "BBE1AA11 BBE2AA11 1", "", Country.BE, Country.BE, -1073.0220519017091, -1072.8186706954648, 833.2508597492509, 0.0, 2.6653287208506264, 251.52260171606758, 0.0, -1.943751586205397, -14.046666553851782, 1.370298649353168);
        validateFlowDecomposition(flowDecompositionResults, "FFR4AA11 DDE4AA11 1_contingency_split_network", "FFR4AA11 DDE4AA11 1", "contingency_split_network", Country.FR, Country.DE, 136.8866986704639, 134.29522060028296, 338.9298012216386, 0.0, 23.516777486510552, -37.33283733323031, 0.0, -190.81852077463606, 0.0, 0.0);
        validateFlowDecomposition(flowDecompositionResults, "NNL2AA11 BBE3AA11 1_contingency_desync", "NNL2AA11 BBE3AA11 1", "contingency_desync", Country.NL, Country.BE, -500.0, -499.9999999999999, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
        validateFlowDecomposition(flowDecompositionResults, "FFR2AA11 DDE3AA11 1", "FFR2AA11 DDE3AA11 1", "", Country.FR, Country.DE, 544.7943507828535, 551.2271511253169, 579.4284185994856, 0.0, -60.94238102269281, 3.8747378618174615, -7.257278401228766, 31.184014645357053, 0.0, 4.939639442578509);
        validateFlowDecomposition(flowDecompositionResults, "BBE1AA11 BBE2AA11 1_contingency_split_network", "BBE1AA11 BBE2AA11 1", "contingency_split_network", Country.BE, Country.BE, -1285.2742471076929, -1285.7142857142858, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
        validateFlowDecomposition(flowDecompositionResults, "DDE1AA11 DDE2AA11 1_contingency_1", "DDE1AA11 DDE2AA11 1", "contingency_1", Country.DE, Country.DE, 446.49029835456855, 438.8138055134247, 338.0746085570481, 0.0, 70.55033245953157, 77.95757144686435, 0.0, 0.0, -47.76870695001777, 0.0);
        validateFlowDecomposition(flowDecompositionResults, "FFR2AA11 FFR4AA11 1_contingency_split_network", "FFR2AA11 FFR4AA11 1", "contingency_split_network", Country.FR, Country.FR, 1038.620574834431, 1013.6653034277945, 415.36395343614413, 0.0, 232.22817767929158, 410.66121066553296, 0.0, -44.58803835317428, 0.0, 0.0);
        validateFlowDecomposition(flowDecompositionResults, "FFR2AA11 DDE3AA11 1_contingency_desync", "FFR2AA11 DDE3AA11 1", "contingency_desync", Country.FR, Country.DE, 1089.471702454293, 1097.114338199151, 983.2105963350834, 0.0, -70.55033245953157, 111.9985119996908, 0.0, 72.45556232390817, 0.0, 0.0);
        validateFlowDecomposition(flowDecompositionResults, "FFR1AA11 FFR3AA11 1", "FFR1AA11 FFR3AA11 1", "", Country.FR, Country.FR, -414.3026462829429, -420.33643938683224, 376.82648586540313, 0.0, -58.97339944512745, 113.88848482976327, -4.303734865844973, -10.030718062146432, 0.0, 2.9293210647849315);
        validateFlowDecomposition(flowDecompositionResults, "FFR2AA11 DDE3AA11 1_contingency_1", "FFR2AA11 DDE3AA11 1", "contingency_1", Country.FR, Country.DE, 1053.5097016441914, 1061.1861944865755, 1009.4338724458735, 0.0, -70.55033245953157, 47.76870695001824, 0.0, 74.53394755021641, 0.0, 0.0);
        validateFlowDecomposition(flowDecompositionResults, "DDE1AA11 DDE4AA11 1", "DDE1AA11 DDE4AA11 1", "", Country.DE, Country.DE, -489.0695632312748, -486.1678479143115, 182.2892778554365, 0.0, 26.533227356215736, 312.14010811810834, -2.278447870153219, 0.0, -34.06713457959329, 1.5508170342979006);
        validateFlowDecomposition(flowDecompositionResults, "NNL1AA11 NNL3AA11 1_contingency_desync", "NNL1AA11 NNL3AA11 1", "contingency_desync", Country.NL, Country.NL, 333.3273043519805, 333.3333333333333, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
        validateFlowDecomposition(flowDecompositionResults, "DDE2AA11 DDE3AA11 1_contingency_desync", "DDE2AA11 DDE3AA11 1", "contingency_desync", Country.DE, Country.DE, -589.4717024542933, -597.1143381991509, 483.2105963350835, 0.0, -70.55033245953157, 72.45556232390817, 0.0, 0.0, 111.9985119996908, 0.0);
        validateFlowDecomposition(flowDecompositionResults, "BBE2AA11 BBE3AA11 1_contingency_split_network", "BBE2AA11 BBE3AA11 1", "contingency_split_network", Country.BE, Country.BE, 714.7257528923074, 714.2857142857143, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
        validateFlowDecomposition(flowDecompositionResults, "NNL1AA11 NNL3AA11 1_contingency_split_network", "NNL1AA11 NNL3AA11 1", "contingency_split_network", Country.NL, Country.NL, NaN, NaN, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
        validateFlowDecomposition(flowDecompositionResults, "BBE1AA11 BBE3AA11 2_contingency_desync", "BBE1AA11 BBE3AA11 2", "contingency_desync", Country.BE, Country.BE, -428.68569183946556, -428.57142857142856, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
        validateFlowDecomposition(flowDecompositionResults, "BBE1AA11 BBE2AA11 1_contingency_desync", "BBE1AA11 BBE2AA11 1", "contingency_desync", Country.BE, Country.BE, -1213.9429244816033, -1214.2857142857142, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
        validateFlowDecomposition(flowDecompositionResults, "FFR2AA11 FFR3AA11 2_contingency_desync", "FFR2AA11 FFR3AA11 2", "contingency_desync", Country.FR, Country.FR, -625.5955323508144, -622.1559283253891, 419.3492099929259, 0.0, 32.33556904395198, 164.8976444943644, 0.0, 5.573504794146785, 0.0, 0.0);
        validateFlowDecomposition(flowDecompositionResults, "BBE1AA11 FFR5AA11 1_contingency_desync", "BBE1AA11 FFR5AA11 1", "contingency_desync", Country.BE, Country.FR, 0.0, -0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
        validateFlowDecomposition(flowDecompositionResults, "BBE3AA11 BBE4AA11 1_contingency_1", "BBE3AA11 BBE4AA11 1", "contingency_1", Country.BE, Country.BE, 428.68569183925223, 428.57142857142856, 336.5714285714285, 0.0, 0.0, 92.00000000000006, 0.0, 0.0, 0.0, 0.0);
        validateFlowDecomposition(flowDecompositionResults, "BBE1AA11 BBE3AA11 1_contingency_desync", "BBE1AA11 BBE3AA11 1", "contingency_desync", Country.BE, Country.BE, -428.68569183946556, -428.57142857142856, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
        validateFlowDecomposition(flowDecompositionResults, "FFR2AA11 FFR4AA11 1_contingency_desync", "FFR2AA11 FFR4AA11 1", "contingency_desync", Country.FR, Country.FR, 1038.620574834431, 1013.6653034277945, 415.36395343614413, 0.0, 232.22817767929158, 410.66121066553296, 0.0, -44.58803835317428, 0.0, 0.0);
        validateFlowDecomposition(flowDecompositionResults, "DDE1AA11 DDE2AA11 1_contingency_split_network", "DDE1AA11 DDE2AA11 1", "contingency_split_network", Country.DE, Country.DE, 410.52829754570706, 402.885661800849, 373.19763679074543, 0.0, 70.55033245953157, 71.13620455026275, 0.0, 0.0, -111.99851199969083, 0.0);
        validateFlowDecomposition(flowDecompositionResults, "FFR2AA11 FFR3AA11 1_contingency_desync", "FFR2AA11 FFR3AA11 1", "contingency_desync", Country.FR, Country.FR, -625.5955323508144, -622.1559283253891, 419.3492099929259, 0.0, 32.33556904395198, 164.8976444943644, 0.0, 5.573504794146785, 0.0, 0.0);
        validateFlowDecomposition(flowDecompositionResults, "FFR2AA11 FFR3AA11 1_contingency_split_network", "FFR2AA11 FFR3AA11 1", "contingency_split_network", Country.FR, Country.FR, -625.5955323508144, -622.1559283253891, 419.3492099929259, 0.0, 32.33556904395198, 164.8976444943644, 0.0, 5.573504794146785, 0.0, 0.0);
        validateFlowDecomposition(flowDecompositionResults, "BBE3AA11 BBE4AA11 1", "BBE3AA11 BBE4AA11 1", "", Country.BE, Country.BE, 146.3286606575447, 145.63734139092978, 91.41866417322206, 0.0, 5.330657441701253, 78.1282587574147, 0.0, -3.887503172410794, -28.093333107703565, 2.740597298706336);
        validateFlowDecomposition(flowDecompositionResults, "FFR1AA11 FFR2AA11 1_contingency_1", "FFR1AA11 FFR2AA11 1", "contingency_1", Country.FR, Country.FR, 676.7527126470246, 665.8689825809585, 311.17078519220183, 0.0, 97.00670713185596, 240.49134851454278, 0.0, 17.200141742357594, 0.0, 0.0);
        validateFlowDecomposition(flowDecompositionResults, "BBE2AA11 BBE3AA11 1_contingency_1", "BBE2AA11 BBE3AA11 1", "contingency_1", Country.BE, Country.BE, 786.0570755169368, 785.7142857142858, 653.7142857142854, 0.0, 0.0, 132.00000000000009, 0.0, 0.0, 0.0, 0.0);
        validateFlowDecomposition(flowDecompositionResults, "FFR1AA11 FFR3AA11 1_contingency_desync", "FFR1AA11 FFR3AA11 1", "contingency_desync", Country.FR, Country.FR, -248.80893529837147, -255.68814334922183, 233.28771320029807, 0.0, -64.67113808790394, 98.21857782512116, 0.0, -11.14700958829357, 0.0, 0.0);
        validateFlowDecomposition(flowDecompositionResults, "BBE1AA11 BBE3AA11 2_contingency_split_network", "BBE1AA11 BBE3AA11 2", "contingency_split_network", Country.BE, Country.BE, -571.5752509641023, -571.4285714285714, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
        validateFlowDecomposition(flowDecompositionResults, "FFR2AA11 DDE3AA11 1_contingency_split_network", "FFR2AA11 DDE3AA11 1", "contingency_split_network", Country.FR, Country.DE, 1089.471702454293, 1097.114338199151, 983.2105963350834, 0.0, -70.55033245953157, 111.9985119996908, 0.0, 72.45556232390817, 0.0, 0.0);
        validateFlowDecomposition(flowDecompositionResults, "DDE2AA11 DDE3AA11 1_contingency_split_network", "DDE2AA11 DDE3AA11 1", "contingency_split_network", Country.DE, Country.DE, -589.4717024542933, -597.1143381991509, 483.2105963350835, 0.0, -70.55033245953157, 72.45556232390817, 0.0, 0.0, 111.9985119996908, 0.0);
        validateFlowDecomposition(flowDecompositionResults, "BBE1AA11 BBE3AA11 2", "BBE1AA11 BBE3AA11 2", "", Country.BE, Country.BE, -146.3286606575447, -145.63734139092978, 91.41866417322206, 0.0, 5.330657441701253, 78.1282587574147, 0.0, -3.887503172410794, -28.093333107703565, 2.740597298706336);
        validateFlowDecomposition(flowDecompositionResults, "BBE1AA11 FFR5AA11 1", "BBE1AA11 FFR5AA11 1", "", Country.BE, Country.FR, 506.00401693786694, 504.865347434127, 553.7534261344583, 0.0, 9.32865052297722, -7.046311005844229, 0.0, -6.80313055171889, -49.16333293848134, 4.796045272736103);
        validateFlowDecomposition(flowDecompositionResults, "BBE1AA11 BBE3AA11 1", "BBE1AA11 BBE3AA11 1", "", Country.BE, Country.BE, -146.3286606575447, -145.63734139092978, 91.41866417322206, 0.0, 5.330657441701253, 78.1282587574147, 0.0, -3.887503172410794, -28.093333107703565, 2.740597298706336);
        validateFlowDecomposition(flowDecompositionResults, "NNL2AA11 NNL3AA11 1_contingency_split_network", "NNL2AA11 NNL3AA11 1", "contingency_split_network", Country.NL, Country.NL, NaN, NaN, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
        validateFlowDecomposition(flowDecompositionResults, "BBE4AA11 FFR5AA11 1_contingency_split_network", "BBE4AA11 FFR5AA11 1", "contingency_split_network", Country.BE, Country.FR, 0.0, -0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
        validateFlowDecomposition(flowDecompositionResults, "BBE2AA11 BBE3AA11 1_contingency_desync", "BBE2AA11 BBE3AA11 1", "contingency_desync", Country.BE, Country.BE, 786.0570755183962, 785.7142857142858, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
        validateFlowDecomposition(flowDecompositionResults, "BBE1AA11 BBE3AA11 1_contingency_1", "BBE1AA11 BBE3AA11 1", "contingency_1", Country.BE, Country.BE, -428.68569183925223, -428.57142857142856, 336.5714285714285, 0.0, 0.0, 92.00000000000006, 0.0, 0.0, 0.0, 0.0);
        validateFlowDecomposition(flowDecompositionResults, "BBE1AA11 FFR5AA11 1_contingency_split_network", "BBE1AA11 FFR5AA11 1", "contingency_split_network", Country.BE, Country.FR, 0.0, -0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
        validateFlowDecomposition(flowDecompositionResults, "FFR4AA11 DDE1AA11 1_contingency_desync", "FFR4AA11 DDE1AA11 1", "contingency_desync", Country.FR, Country.DE, 773.6415988752431, 768.5904412005659, 677.8596024432774, 0.0, 47.033554973021104, -74.66567466646057, 0.0, 118.36295845072787, 0.0, 0.0);
        validateFlowDecomposition(flowDecompositionResults, "BBE1AA11 BBE2AA11 1_contingency_1", "BBE1AA11 BBE2AA11 1", "contingency_1", Country.BE, Country.BE, -1213.9429244799337, -1214.2857142857142, 990.285714285714, 0.0, 0.0, 224.00000000000014, 0.0, 0.0, 0.0, 0.0);
        validateFlowDecomposition(flowDecompositionResults, "FFR1AA11 FFR3AA11 1_contingency_1", "FFR1AA11 FFR3AA11 1", "contingency_1", Country.FR, Country.FR, -716.3780430271256, -722.7540116126935, 678.6298412366724, 0.0, -64.6711380879039, 120.26206962549712, 0.0, -11.466761161571645, 0.0, 0.0);
        validateFlowDecomposition(flowDecompositionResults, "DDE1AA11 DDE2AA11 1", "DDE1AA11 DDE2AA11 1", "", Country.DE, Country.DE, -32.78631690706756, -41.496456257064565, -28.078433669428595, 0.0, -79.5996820686472, 44.790275748794826, 6.835343610459651, 0.0, 102.2014037387801, -4.6524511028937);
        validateFlowDecomposition(flowDecompositionResults, "FFR1AA11 FFR2AA11 1", "FFR1AA11 FFR2AA11 1", "", Country.FR, Country.FR, 635.0894995353001, 624.3606883538788, 202.36782344491135, 0.0, 97.78874969066842, 316.14981370831623, -0.5907087070767636, 8.242946541500697, 0.0, 0.40206367555871364);
        validateFlowDecomposition(flowDecompositionResults, "FFR4AA11 DDE1AA11 1_contingency_split_network", "FFR4AA11 DDE1AA11 1", "contingency_split_network", Country.FR, Country.DE, 773.6415988752431, 768.5904412005659, 677.8596024432774, 0.0, 47.033554973021104, -74.66567466646057, 0.0, 118.36295845072787, 0.0, 0.0);
        validateFlowDecomposition(flowDecompositionResults, "NNL2AA11 NNL3AA11 1", "NNL2AA11 NNL3AA11 1", "", Country.NL, Country.NL, 825.2700044179857, 826.846203421164, 705.7437936237146, 0.0, -12.438200697302925, 49.52357783335967, 9.395081341125659, 9.070840735625207, 65.55111058464168, 0.0);
        validateFlowDecomposition(flowDecompositionResults, "BBE4AA11 FFR5AA11 1_contingency_desync", "BBE4AA11 FFR5AA11 1", "contingency_desync", Country.BE, Country.FR, 0.0, -0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
        validateFlowDecomposition(flowDecompositionResults, "DDE1AA11 DDE4AA11 1_contingency_desync", "DDE1AA11 DDE4AA11 1", "contingency_desync", Country.DE, Country.DE, -636.8866986704637, -634.295220600283, 338.92980122163874, 0.0, 23.516777486510552, 309.18147922536394, 0.0, 0.0, -37.332837333230245, 0.0);
        validateFlowDecomposition(flowDecompositionResults, "NNL1AA11 NNL2AA11 1_contingency_split_network", "NNL1AA11 NNL2AA11 1", "contingency_split_network", Country.NL, Country.NL, NaN, NaN, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
        validateFlowDecomposition(flowDecompositionResults, "FFR4AA11 DDE4AA11 1_contingency_desync", "FFR4AA11 DDE4AA11 1", "contingency_desync", Country.FR, Country.DE, 136.8866986704639, 134.29522060028296, 338.9298012216386, 0.0, 23.516777486510552, -37.33283733323031, 0.0, -190.81852077463606, 0.0, 0.0);
        validateFlowDecomposition(flowDecompositionResults, "FFR1AA11 FFR5AA11 1", "FFR1AA11 FFR5AA11 1", "", Country.FR, Country.FR, NaN, NaN, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
        validateFlowDecomposition(flowDecompositionResults, "BBE3AA11 BBE4AA11 1_contingency_desync", "BBE3AA11 BBE4AA11 1", "contingency_desync", Country.BE, Country.BE, 428.68569183946556, 428.57142857142856, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
        validateFlowDecomposition(flowDecompositionResults, "BBE1AA11 FFR5AA11 1_contingency_1", "BBE1AA11 FFR5AA11 1", "contingency_1", Country.BE, Country.FR, 999.9999999988574, 999.9999999999993, 999.9999999999991, 0.0, 1.0812844748544581E-13, 0.0, 0.0, 2.5394939317593507E-14, -7.089483516146792E-14, 0.0);
        validateFlowDecomposition(flowDecompositionResults, "NNL1AA11 NNL3AA11 1", "NNL1AA11 NNL3AA11 1", "", Country.NL, Country.NL, 662.7219617060664, 663.4231017105822, 518.9944395163457, 0.0, -6.219100348651464, 108.6392462121916, 4.697540670562823, 4.535420367812604, 32.77555529232086, 0.0);
        validateFlowDecomposition(flowDecompositionResults, "FFR4AA11 DDE1AA11 1_contingency_1", "FFR4AA11 DDE1AA11 1", "contingency_1", Country.FR, Country.DE, 797.6100073946999, 792.5425370089482, 660.3774183694163, 0.0, 47.03355497302109, -31.845804633345523, 0.0, 116.97736829985581, 0.0, 0.0);
        validateFlowDecomposition(flowDecompositionResults, "FFR1AA11 FFR2AA11 1_contingency_desync", "FFR1AA11 FFR2AA11 1", "contingency_desync", Country.FR, Country.FR, 376.90121258709553, 366.4677849761673, 186.06149679262785, 0.0, 97.00670713185592, 66.67906666924317, 0.0, 16.72051438244036, 0.0, 0.0);
        validateFlowDecomposition(flowDecompositionResults, "FFR1AA11 FFR4AA11 1_contingency_desync", "FFR1AA11 FFR4AA11 1", "contingency_desync", Country.FR, Country.FR, 871.9077227112757, 889.2203583730545, 601.425450228772, 0.0, -161.6778452197599, 477.34027733477615, 0.0, -27.86752397073389, 0.0, 0.0);
        validateFlowDecomposition(flowDecompositionResults, "FFR2AA11 FFR4AA11 1_contingency_1", "FFR2AA11 FFR4AA11 1", "contingency_1", Country.FR, Country.FR, 906.8649679741118, 881.9287764816853, 520.4157179652809, 0.0, 232.22817767929158, 175.15192548339922, 0.0, -45.86704464628704, 0.0, 0.0);
        validateFlowDecomposition(flowDecompositionResults, "FFR2AA11 FFR3AA11 2_contingency_1", "FFR2AA11 FFR3AA11 2", "contingency_1", Country.FR, Country.FR, -1391.810978477755, -1388.622994193652, 989.8006264288742, 0.0, 32.33556904395206, 360.75341814003997, 0.0, 5.733380580785948, 0.0, 0.0);
        validateFlowDecomposition(flowDecompositionResults, "NNL2AA11 NNL3AA11 1_contingency_1", "NNL2AA11 NNL3AA11 1", "contingency_1", Country.NL, Country.NL, 166.67269564801944, 166.66666666666666, 83.33333333333323, 0.0, 0.0, 83.33333333333334, -2.0164772629449615E-14, 0.0, 0.0, 0.0);
        validateFlowDecomposition(flowDecompositionResults, "FFR1AA11 FFR2AA11 1_contingency_split_network", "FFR1AA11 FFR2AA11 1", "contingency_split_network", Country.FR, Country.FR, 376.90121258709553, 366.4677849761673, 186.06149679262785, 0.0, 97.00670713185592, 66.67906666924317, 0.0, 16.72051438244036, 0.0, 0.0);
        validateFlowDecomposition(flowDecompositionResults, "FFR2AA11 FFR3AA11 1_contingency_1", "FFR2AA11 FFR3AA11 1", "contingency_1", Country.FR, Country.FR, -1391.810978477755, -1388.622994193652, 989.8006264288742, 0.0, 32.33556904395206, 360.75341814003997, 0.0, 5.733380580785948, 0.0, 0.0);
        validateFlowDecomposition(flowDecompositionResults, "FFR1AA11 FFR4AA11 1_contingency_1", "FFR1AA11 FFR4AA11 1", "contingency_1", Country.FR, Country.FR, 1039.6253303786427, 1056.8850290317364, 831.5865031574828, 0.0, -161.67784521975986, 415.64327399794206, 0.0, -28.66690290392942, 0.0, 0.0);
        validateFlowDecomposition(flowDecompositionResults, "BBE4AA11 FFR5AA11 1_contingency_1", "BBE4AA11 FFR5AA11 1", "contingency_1", Country.BE, Country.FR, 999.9999999988574, 999.9999999999993, 999.9999999999991, 0.0, 1.0812844748544581E-13, 0.0, 0.0, 2.5394939317593507E-14, -7.089483516146792E-14, 0.0);
        validateFlowDecomposition(flowDecompositionResults, "DDE2AA11 DDE3AA11 1", "DDE2AA11 DDE3AA11 1", "", Country.DE, Country.DE, -44.794350782930344, -51.22715112531687, 79.42841859948483, 0.0, -60.94238102269279, 31.184014645357053, -7.257278401228807, 0.0, 3.8747378618175823, 4.9396394425784855);
        validateFlowDecomposition(flowDecompositionResults, "BBE3AA11 BBE4AA11 1_contingency_split_network", "BBE3AA11 BBE4AA11 1", "contingency_split_network", Country.BE, Country.BE, 571.5752509641023, 571.4285714285714, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
        validateFlowDecomposition(flowDecompositionResults, "DDE2AA11 NNL3AA11 1", "DDE2AA11 NNL3AA11 1", "", Country.DE, Country.NL, -987.9919661237825, -990.2693051317461, 892.493147731084, 0.0, -18.657301045954405, 13.60626110343786, 14.092622011688489, 0.0, 98.32666587696264, -9.5920905454722);
        validateFlowDecomposition(flowDecompositionResults, "FFR2AA11 FFR4AA11 1", "FFR2AA11 FFR4AA11 1", "", Country.FR, Country.FR, 688.0005363453773, 662.527792709982, 275.59454776710874, 0.0, 236.3618312044431, 178.08514862160195, -3.122317451691485, -26.516611145147813, 0.0, 2.125193713667491);
        validateFlowDecomposition(flowDecompositionResults, "BBE4AA11 FFR5AA11 1", "BBE4AA11 FFR5AA11 1", "", Country.BE, Country.FR, 506.00401693786694, 504.865347434127, 553.7534261344583, 0.0, 9.32865052297722, -7.046311005844229, 0.0, -6.80313055171889, -49.16333293848134, 4.796045272736103);
        validateFlowDecomposition(flowDecompositionResults, "FFR1AA11 FFR3AA11 1_contingency_split_network", "FFR1AA11 FFR3AA11 1", "contingency_split_network", Country.FR, Country.FR, -248.80893529837147, -255.68814334922183, 233.28771320029807, 0.0, -64.67113808790394, 98.21857782512116, 0.0, -11.14700958829357, 0.0, 0.0);
        validateFlowDecomposition(flowDecompositionResults, "BBE1AA11 BBE3AA11 2_contingency_1", "BBE1AA11 BBE3AA11 2", "contingency_1", Country.BE, Country.BE, -428.68569183925223, -428.57142857142856, 336.5714285714285, 0.0, 0.0, 92.00000000000006, 0.0, 0.0, 0.0, 0.0);
        validateFlowDecomposition(flowDecompositionResults, "FFR1AA11 FFR5AA11 1_contingency_1", "FFR1AA11 FFR5AA11 1", "contingency_1", Country.FR, Country.FR, NaN, NaN, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0);
        assertEquals(4, flowDecompositionResults.getZoneSet().size());
        assertTrue(flowDecompositionResults.getZoneSet().contains(Country.BE));
        assertTrue(flowDecompositionResults.getZoneSet().contains(Country.DE));
        assertTrue(flowDecompositionResults.getZoneSet().contains(Country.FR));
        assertTrue(flowDecompositionResults.getZoneSet().contains(Country.NL));
    }
}
