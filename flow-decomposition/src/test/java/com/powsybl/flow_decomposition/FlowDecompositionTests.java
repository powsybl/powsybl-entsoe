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
        FlowDecompositionComputer flowDecompositionComputer = new FlowDecompositionComputer(flowDecompositionParameters, new LoadFlowParameters());
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
        validateFlowDecomposition(flowDecompositionResults, "a708c3bc-465d-4fe7-b6ef-6fa6408a62b0", "a708c3bc-465d-4fe7-b6ef-6fa6408a62b0", "", Country.BE, Country.BE, 105.349165, 115.128500, -8.895784, 30.705093, 33.029749, 60.289442, 0.000000, 0.000000, 0.000000, 0.000000);
        validateFlowDecomposition(flowDecompositionResults, "b58bf21a-096a-4dae-9a01-3f03b60c24c7", "b58bf21a-096a-4dae-9a01-3f03b60c24c7", "", Country.BE, Country.BE, -116.324382, -118.550024, -0.000000, 126.160372, -0.000000, -7.610348, 0.000000, 0.000000, 0.000000, 0.000000);
        validateFlowDecomposition(flowDecompositionResults, "b94318f6-6d24-4f56-96b9-df2531ad6543", "b94318f6-6d24-4f56-96b9-df2531ad6543", "", Country.BE, Country.BE, 0.298603, 0.288925, -5.505314, 76.274497, -33.029749, -37.450509, 0.000000, 0.000000, 0.000000, 0.000000);
        validateFlowDecomposition(flowDecompositionResults, "df16b3dd-c905-4a6f-84ee-f067be86f5da", "df16b3dd-c905-4a6f-84ee-f067be86f5da", "", Country.BE, Country.BE, -99.797280, -103.741300, 0.000000, 103.741300, -0.000000, -0.000000, 0.000000, 0.000000, 0.000000, 0.000000);
        validateFlowDecomposition(flowDecompositionResults, "e482b89a-fa84-4ea9-8e70-a83d44790957", "e482b89a-fa84-4ea9-8e70-a83d44790957", "", Country.BE, Country.BE, -94.889551, -84.582575, 14.401099, -106.979590, -0.000000, 177.161067, 0.000000, 0.000000, 0.000000, 0.000000);
        validateFlowDecomposition(flowDecompositionResults, "ffbabc27-1ccd-4fdc-b037-e341706c8d29", "ffbabc27-1ccd-4fdc-b037-e341706c8d29", "", Country.BE, Country.BE, -53.551022, -57.104378, -0.000000, 60.770208, -0.000000, -3.665830, 0.000000, 0.000000, 0.000000, 0.000000);
        assertEquals(1, flowDecompositionResults.getZoneSet().size());
        assertTrue(flowDecompositionResults.getZoneSet().contains(Country.BE));

    }

    @Test
    void testFlowDecompositionOnNetworkWithShuntCompensatorOnly() {
        Network network = TestUtils.getMicroGridNetworkWithShuntCompensatorOnly();

        FlowDecompositionResults flowDecompositionResults = runFlowDecomposition(network, new XnecProviderAllBranches());
        assertEquals(6, flowDecompositionResults.getDecomposedFlowMap().size());
        validateFlowDecomposition(flowDecompositionResults, "a708c3bc-465d-4fe7-b6ef-6fa6408a62b0", "a708c3bc-465d-4fe7-b6ef-6fa6408a62b0", "", Country.BE, Country.BE, 105.203730, 115.160115, -8.895784, 30.705093, 33.029749, 60.321057, 0.000000, 0.000000, 0.000000, 0.000000);
        validateFlowDecomposition(flowDecompositionResults, "b58bf21a-096a-4dae-9a01-3f03b60c24c7", "b58bf21a-096a-4dae-9a01-3f03b60c24c7", "", Country.BE, Country.BE, -116.309968, -118.537339, -0.000000, 126.160372, -0.000000, -7.623033, 0.000000, 0.000000, 0.000000, 0.000000);
        validateFlowDecomposition(flowDecompositionResults, "b94318f6-6d24-4f56-96b9-df2531ad6543", "b94318f6-6d24-4f56-96b9-df2531ad6543", "", Country.BE, Country.BE, -0.732804, 0.247626, -5.505314, 76.274497, -33.029749, -37.491807, 0.000000, 0.000000, 0.000000, 0.000000);
        validateFlowDecomposition(flowDecompositionResults, "df16b3dd-c905-4a6f-84ee-f067be86f5da", "df16b3dd-c905-4a6f-84ee-f067be86f5da", "", Country.BE, Country.BE, -99.793176, -103.741300, 0.000000, 103.741300, -0.000000, -0.000000, 0.000000, 0.000000, 0.000000, 0.000000);
        validateFlowDecomposition(flowDecompositionResults, "e482b89a-fa84-4ea9-8e70-a83d44790957", "e482b89a-fa84-4ea9-8e70-a83d44790957", "", Country.BE, Country.BE, -95.900991, -84.604242, 14.401099, -106.979590, -0.000000, 177.182734, 0.000000, 0.000000, 0.000000, 0.000000);
        validateFlowDecomposition(flowDecompositionResults, "ffbabc27-1ccd-4fdc-b037-e341706c8d29", "ffbabc27-1ccd-4fdc-b037-e341706c8d29", "", Country.BE, Country.BE, -53.539743, -57.098268, -0.000000, 60.770208, -0.000000, -3.671940, 0.000000, 0.000000, 0.000000, 0.000000);
        assertEquals(1, flowDecompositionResults.getZoneSet().size());
        assertTrue(flowDecompositionResults.getZoneSet().contains(Country.BE));

    }

    @Test
    void testFlowDecompositionOnNetworkWithStaticVarCompensatorOnly() {
        Network network = TestUtils.getNetworkWithStaticVarCompensatorOnly();

        FlowDecompositionResults flowDecompositionResults = runFlowDecomposition(network, new XnecProviderAllBranches());
        assertEquals(1, flowDecompositionResults.getDecomposedFlowMap().size());
        validateFlowDecomposition(flowDecompositionResults, "L1", "L1", "", Country.FR, Country.FR, 100.260455, 100.000000, -0.000000, 0.000000, 0.000000, 100.000000, 0.000000, 0.000000, 0.000000, 0.000000);
        assertEquals(1, flowDecompositionResults.getZoneSet().size());
        assertTrue(flowDecompositionResults.getZoneSet().contains(Country.FR));
    }

    @Test
    void testFlowDecompositionOnHvdcNetwork() {
        testFlowDecompositionOnHvdcNetwork(FlowDecompositionParameters.FlowPartitionMode.MATRIX_BASED);
    }

    @Test
    void testFlowDecompositionOnHvdcNetworkUsingFastMode() {
        testFlowDecompositionOnHvdcNetwork(FlowDecompositionParameters.FlowPartitionMode.DIRECT_SENSITIVITY_BASED);
    }

    @Test
    void testConnectedComponentModeChangesFromAllToMain() {
        LoadFlowParameters loadFlowParameters = new LoadFlowParameters().setConnectedComponentMode(LoadFlowParameters.ConnectedComponentMode.ALL);
        FlowDecompositionComputer flowDecompositionComputer = new FlowDecompositionComputer(new FlowDecompositionParameters(), loadFlowParameters);
        // lfParameters inside flow decomposition changed from all to main
        assertEquals(FlowDecompositionComputer.MAIN_CONNECTED_COMPONENT, flowDecompositionComputer.getLoadFlowParameters().getConnectedComponentMode());
        // original lfParameters didn't change
        assertEquals(LoadFlowParameters.ConnectedComponentMode.ALL, loadFlowParameters.getConnectedComponentMode());
    }

    void testFlowDecompositionOnHvdcNetwork(FlowDecompositionParameters.FlowPartitionMode flowPartitionMode) {
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
        validateFlowDecomposition(flowDecompositionResults, "BBE1AA11 BBE3AA11 1_contingency_split_network", "BBE1AA11 BBE3AA11 1", "contingency_split_network", Country.BE, Country.BE, -571.575251, -571.428571, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000);
        validateFlowDecomposition(flowDecompositionResults, "FFR2AA11 FFR3AA11 2", "FFR2AA11 FFR3AA11 2", "", Country.FR, Country.FR, -1048.852694, -1044.697128, 418.391705, 0.000000, 38.815350, 611.949424, -16.125814, -4.610042, 0.000000, -3.723495);
        validateFlowDecomposition(flowDecompositionResults, "FFR2AA11 FFR3AA11 1", "FFR2AA11 FFR3AA11 1", "", Country.FR, Country.FR, -1048.852694, -1044.697128, 418.391705, 0.000000, 38.815350, 611.949424, -16.125814, -4.610042, 0.000000, -3.723495);
        validateFlowDecomposition(flowDecompositionResults, "FFR3AA11 FFR5AA11 1", "FFR3AA11 FFR5AA11 1", "", Country.FR, Country.FR, -1012.008034, -1009.730695, 1246.058874, 0.000000, 18.657301, -134.559417, -46.431224, -63.273741, 0.000000, -10.721099);
        validateFlowDecomposition(flowDecompositionResults, "DDE2AA11 DDE3AA11 1_contingency_1", "DDE2AA11 DDE3AA11 1", "contingency_1", Country.DE, Country.DE, -553.509702, -561.186194, 443.113772, 0.000000, -70.550332, 125.748503, 0.000000, 0.000000, 62.874251, 0.000000);
        validateFlowDecomposition(flowDecompositionResults, "FFR4AA11 DDE4AA11 1_contingency_1", "FFR4AA11 DDE4AA11 1", "contingency_1", Country.FR, Country.DE, 148.880291, 146.271269, 518.962076, 0.000000, 23.516777, 0.000000, 0.000000, -375.249501, -20.958084, 0.000000);
        validateFlowDecomposition(flowDecompositionResults, "FFR1AA11 FFR4AA11 1_contingency_split_network", "FFR1AA11 FFR4AA11 1", "contingency_split_network", Country.FR, Country.FR, 871.892782, 889.220358, 431.137725, 0.000000, -161.677845, 706.586826, 0.000000, -86.826347, 0.000000, 0.000000);
        validateFlowDecomposition(flowDecompositionResults, "FFR2AA11 FFR3AA11 2_contingency_split_network", "FFR2AA11 FFR3AA11 2", "contingency_split_network", Country.FR, Country.FR, -625.590192, -622.155928, -86.227545, 0.000000, 32.335569, 658.682635, 0.000000, 17.365269, 0.000000, 0.000000);
        validateFlowDecomposition(flowDecompositionResults, "DDE1AA11 DDE4AA11 1_contingency_1", "DDE1AA11 DDE4AA11 1", "contingency_1", Country.DE, Country.DE, -648.880291, -646.271269, 118.962076, 0.000000, 23.516777, 524.750499, 0.000000, 0.000000, -20.958084, 0.000000);
        validateFlowDecomposition(flowDecompositionResults, "NNL1AA11 NNL2AA11 1_contingency_1", "NNL1AA11 NNL2AA11 1", "contingency_1", Country.NL, Country.NL, 166.672696, 166.666667, -208.333333, 0.000000, -0.000000, 375.000000, 0.000000, 0.000000, 0.000000, 0.000000);
        validateFlowDecomposition(flowDecompositionResults, "NNL2AA11 BBE3AA11 1_contingency_1", "NNL2AA11 BBE3AA11 1", "contingency_1", Country.NL, Country.BE, -500.000000, -500.000000, 500.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000);
        validateFlowDecomposition(flowDecompositionResults, "NNL1AA11 NNL2AA11 1_contingency_desync", "NNL1AA11 NNL2AA11 1", "contingency_desync", Country.NL, Country.NL, 166.672696, 166.666667, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000);
        validateFlowDecomposition(flowDecompositionResults, "FFR1AA11 FFR4AA11 1", "FFR1AA11 FFR4AA11 1", "", Country.FR, Country.FR, 779.213147, 795.975751, 400.159224, 0.000000, -156.762149, 671.134046, -12.233376, -103.497273, 0.000000, -2.824721);
        validateFlowDecomposition(flowDecompositionResults, "FFR3AA11 FFR5AA11 1_contingency_1", "FFR3AA11 FFR5AA11 1", "contingency_1", Country.FR, Country.FR, -2000.000000, -2000.000000, 2000.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000);
        validateFlowDecomposition(flowDecompositionResults, "NNL1AA11 NNL2AA11 1", "NNL1AA11 NNL2AA11 1", "", Country.NL, Country.NL, -162.721962, -163.423102, 459.647042, 0.000000, -6.219100, -371.426300, 15.477075, 21.091247, 44.853139, 0.000000);
        validateFlowDecomposition(flowDecompositionResults, "FFR1AA11 FFR5AA11 1_contingency_desync", "FFR1AA11 FFR5AA11 1", "contingency_desync", Country.FR, Country.FR, NaN, NaN, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000);
        validateFlowDecomposition(flowDecompositionResults, "DDE1AA11 DDE4AA11 1_contingency_split_network", "DDE1AA11 DDE4AA11 1", "contingency_split_network", Country.DE, Country.DE, -636.877548, -634.295221, 106.986028, 0.000000, 23.516777, 524.750499, 0.000000, 0.000000, -20.958084, 0.000000);
        validateFlowDecomposition(flowDecompositionResults, "FFR4AA11 DDE1AA11 1", "FFR4AA11 DDE1AA11 1", "", Country.FR, Country.DE, 478.144120, 472.335696, 394.134806, 0.000000, 53.066455, 0.000000, -15.013689, 129.041225, -85.426398, -3.466703);
        validateFlowDecomposition(flowDecompositionResults, "NNL2AA11 BBE3AA11 1", "NNL2AA11 BBE3AA11 1", "", Country.NL, Country.BE, -1487.991966, -1490.269305, 1253.941126, 0.000000, -18.657301, 0.000000, 46.431224, 63.273741, 134.559417, 10.721099);
        validateFlowDecomposition(flowDecompositionResults, "BBE2AA11 BBE3AA11 1", "BBE2AA11 BBE3AA11 1", "", Country.BE, Country.BE, 926.977948, 927.181329, 423.495349, 0.000000, -2.665329, 476.557844, 0.000000, 9.039106, 19.222774, 1.531586);
        validateFlowDecomposition(flowDecompositionResults, "DDE1AA11 DDE2AA11 1_contingency_desync", "DDE1AA11 DDE2AA11 1", "contingency_desync", Country.DE, Country.DE, 410.522825, 402.885662, 54.291417, 0.000000, 70.550332, 340.918164, 0.000000, 0.000000, -62.874251, 0.000000);
        validateFlowDecomposition(flowDecompositionResults, "NNL1AA11 NNL3AA11 1_contingency_1", "NNL1AA11 NNL3AA11 1", "contingency_1", Country.NL, Country.NL, 333.327304, 333.333333, 20.833333, 0.000000, -0.000000, 312.500000, 0.000000, 0.000000, 0.000000, 0.000000);
        validateFlowDecomposition(flowDecompositionResults, "FFR4AA11 DDE4AA11 1", "FFR4AA11 DDE4AA11 1", "", Country.FR, Country.DE, -10.930437, -13.832152, -397.067403, 0.000000, -26.533227, 0.000000, 7.506845, 385.479387, 42.713199, 1.733351);
        validateFlowDecomposition(flowDecompositionResults, "FFR1AA11 FFR5AA11 1_contingency_split_network", "FFR1AA11 FFR5AA11 1", "contingency_split_network", Country.FR, Country.FR, NaN, NaN, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000);
        validateFlowDecomposition(flowDecompositionResults, "NNL2AA11 NNL3AA11 1_contingency_desync", "NNL2AA11 NNL3AA11 1", "contingency_desync", Country.NL, Country.NL, 166.672696, 166.666667, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000);
        validateFlowDecomposition(flowDecompositionResults, "BBE1AA11 BBE2AA11 1", "BBE1AA11 BBE2AA11 1", "", Country.BE, Country.BE, -1073.022052, -1072.818671, 365.978335, 0.000000, 2.665329, 733.968472, 0.000000, -9.039106, -19.222774, -1.531586);
        validateFlowDecomposition(flowDecompositionResults, "FFR4AA11 DDE4AA11 1_contingency_split_network", "FFR4AA11 DDE4AA11 1", "contingency_split_network", Country.FR, Country.DE, 136.883741, 134.295221, 506.986028, 0.000000, 23.516777, 0.000000, 0.000000, -375.249501, -20.958084, 0.000000);
        validateFlowDecomposition(flowDecompositionResults, "NNL2AA11 BBE3AA11 1_contingency_desync", "NNL2AA11 BBE3AA11 1", "contingency_desync", Country.NL, Country.BE, -500.000000, -500.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000);
        validateFlowDecomposition(flowDecompositionResults, "FFR2AA11 DDE3AA11 1", "FFR2AA11 DDE3AA11 1", "", Country.FR, Country.DE, 544.794351, 551.227151, 454.856666, 0.000000, -60.942381, 0.000000, -23.910690, 193.164421, -6.419820, -5.521045);
        validateFlowDecomposition(flowDecompositionResults, "BBE1AA11 BBE2AA11 1_contingency_split_network", "BBE1AA11 BBE2AA11 1", "contingency_split_network", Country.BE, Country.BE, -1285.274247, -1285.714286, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000);
        validateFlowDecomposition(flowDecompositionResults, "DDE1AA11 DDE2AA11 1_contingency_1", "DDE1AA11 DDE2AA11 1", "contingency_1", Country.DE, Country.DE, 446.490298, 438.813806, 90.219561, 0.000000, 70.550332, 340.918164, 0.000000, 0.000000, -62.874251, 0.000000);
        validateFlowDecomposition(flowDecompositionResults, "FFR2AA11 FFR4AA11 1_contingency_split_network", "FFR2AA11 FFR4AA11 1", "contingency_split_network", Country.FR, Country.FR, 1038.609398, 1013.665303, 689.820359, 0.000000, 232.228178, 230.538922, 0.000000, -138.922156, 0.000000, 0.000000);
        validateFlowDecomposition(flowDecompositionResults, "FFR2AA11 DDE3AA11 1_contingency_desync", "FFR2AA11 DDE3AA11 1", "contingency_desync", Country.FR, Country.DE, 1089.444201, 1097.114338, 879.041916, 0.000000, -70.550332, 0.000000, 0.000000, 225.748503, 62.874251, 0.000000);
        validateFlowDecomposition(flowDecompositionResults, "FFR1AA11 FFR3AA11 1", "FFR1AA11 FFR3AA11 1", "", Country.FR, Country.FR, -414.302646, -420.336439, 409.275465, 0.000000, -58.973399, 141.541735, -14.179595, -54.053657, 0.000000, -3.274108);
        validateFlowDecomposition(flowDecompositionResults, "FFR2AA11 DDE3AA11 1_contingency_1", "FFR2AA11 DDE3AA11 1", "contingency_1", Country.FR, Country.DE, 1053.509702, 1061.186194, 843.113772, 0.000000, -70.550332, 0.000000, 0.000000, 225.748503, 62.874251, 0.000000);
        validateFlowDecomposition(flowDecompositionResults, "DDE1AA11 DDE4AA11 1", "DDE1AA11 DDE4AA11 1", "", Country.DE, Country.DE, -489.069563, -486.167848, -2.932597, 0.000000, 26.533227, 514.520613, -7.506845, 0.000000, -42.713199, -1.733351);
        validateFlowDecomposition(flowDecompositionResults, "NNL1AA11 NNL3AA11 1_contingency_desync", "NNL1AA11 NNL3AA11 1", "contingency_desync", Country.NL, Country.NL, 333.327304, 333.333333, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000);
        validateFlowDecomposition(flowDecompositionResults, "DDE2AA11 DDE3AA11 1_contingency_desync", "DDE2AA11 DDE3AA11 1", "contingency_desync", Country.DE, Country.DE, -589.470478, -597.114338, 479.041916, 0.000000, -70.550332, 125.748503, 0.000000, 0.000000, 62.874251, 0.000000);
        validateFlowDecomposition(flowDecompositionResults, "BBE2AA11 BBE3AA11 1_contingency_split_network", "BBE2AA11 BBE3AA11 1", "contingency_split_network", Country.BE, Country.BE, 714.725753, 714.285714, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000);
        validateFlowDecomposition(flowDecompositionResults, "NNL1AA11 NNL3AA11 1_contingency_split_network", "NNL1AA11 NNL3AA11 1", "contingency_split_network", Country.NL, Country.NL, NaN, NaN, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000);
        validateFlowDecomposition(flowDecompositionResults, "BBE1AA11 BBE3AA11 2_contingency_desync", "BBE1AA11 BBE3AA11 2", "contingency_desync", Country.BE, Country.BE, -428.685692, -428.571429, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000);
        validateFlowDecomposition(flowDecompositionResults, "BBE1AA11 BBE2AA11 1_contingency_desync", "BBE1AA11 BBE2AA11 1", "contingency_desync", Country.BE, Country.BE, -1213.942924, -1214.285714, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000);
        validateFlowDecomposition(flowDecompositionResults, "FFR2AA11 FFR3AA11 2_contingency_desync", "FFR2AA11 FFR3AA11 2", "contingency_desync", Country.FR, Country.FR, -625.590192, -622.155928, -86.227545, 0.000000, 32.335569, 658.682635, 0.000000, 17.365269, 0.000000, 0.000000);
        validateFlowDecomposition(flowDecompositionResults, "BBE1AA11 FFR5AA11 1_contingency_desync", "BBE1AA11 FFR5AA11 1", "contingency_desync", Country.BE, Country.FR, 0.000000, -0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000);
        validateFlowDecomposition(flowDecompositionResults, "BBE3AA11 BBE4AA11 1_contingency_1", "BBE3AA11 BBE4AA11 1", "contingency_1", Country.BE, Country.BE, 428.685692, 428.571429, 157.894737, 0.000000, 0.000000, 270.676692, 0.000000, 0.000000, 0.000000, -0.000000);
        validateFlowDecomposition(flowDecompositionResults, "BBE1AA11 BBE3AA11 1_contingency_desync", "BBE1AA11 BBE3AA11 1", "contingency_desync", Country.BE, Country.BE, -428.685692, -428.571429, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000);
        validateFlowDecomposition(flowDecompositionResults, "FFR2AA11 FFR4AA11 1_contingency_desync", "FFR2AA11 FFR4AA11 1", "contingency_desync", Country.FR, Country.FR, 1038.609398, 1013.665303, 689.820359, 0.000000, 232.228178, 230.538922, 0.000000, -138.922156, 0.000000, 0.000000);
        validateFlowDecomposition(flowDecompositionResults, "DDE1AA11 DDE2AA11 1_contingency_split_network", "DDE1AA11 DDE2AA11 1", "contingency_split_network", Country.DE, Country.DE, 410.522825, 402.885662, 54.291417, 0.000000, 70.550332, 340.918164, 0.000000, 0.000000, -62.874251, 0.000000);
        validateFlowDecomposition(flowDecompositionResults, "FFR2AA11 FFR3AA11 1_contingency_desync", "FFR2AA11 FFR3AA11 1", "contingency_desync", Country.FR, Country.FR, -625.590192, -622.155928, -86.227545, 0.000000, 32.335569, 658.682635, 0.000000, 17.365269, 0.000000, 0.000000);
        validateFlowDecomposition(flowDecompositionResults, "FFR2AA11 FFR3AA11 1_contingency_split_network", "FFR2AA11 FFR3AA11 1", "contingency_split_network", Country.FR, Country.FR, -625.590192, -622.155928, -86.227545, 0.000000, 32.335569, 658.682635, 0.000000, 17.365269, 0.000000, 0.000000);
        validateFlowDecomposition(flowDecompositionResults, "BBE3AA11 BBE4AA11 1", "BBE3AA11 BBE4AA11 1", "", Country.BE, Country.BE, 146.328661, 145.637341, -57.517013, 0.000000, 5.330657, 257.410628, 0.000000, -18.078212, -38.445548, -3.063171);
        validateFlowDecomposition(flowDecompositionResults, "FFR1AA11 FFR2AA11 1_contingency_1", "FFR1AA11 FFR2AA11 1", "contingency_1", Country.FR, Country.FR, 676.752713, 665.868983, 40.718563, 0.000000, 97.006707, 476.047904, 0.000000, 52.095808, 0.000000, 0.000000);
        validateFlowDecomposition(flowDecompositionResults, "BBE2AA11 BBE3AA11 1_contingency_1", "BBE2AA11 BBE3AA11 1", "contingency_1", Country.BE, Country.BE, 786.057076, 785.714286, 315.789474, 0.000000, 0.000000, 469.924812, 0.000000, 0.000000, 0.000000, 0.000000);
        validateFlowDecomposition(flowDecompositionResults, "FFR1AA11 FFR3AA11 1_contingency_desync", "FFR1AA11 FFR3AA11 1", "contingency_desync", Country.FR, Country.FR, -248.807353, -255.688143, 172.455090, 0.000000, -64.671138, 182.634731, 0.000000, -34.730539, 0.000000, 0.000000);
        validateFlowDecomposition(flowDecompositionResults, "BBE1AA11 BBE3AA11 2_contingency_split_network", "BBE1AA11 BBE3AA11 2", "contingency_split_network", Country.BE, Country.BE, -571.575251, -571.428571, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000);
        validateFlowDecomposition(flowDecompositionResults, "DDE2AA11 DDE3AA11 1_contingency_split_network", "DDE2AA11 DDE3AA11 1", "contingency_split_network", Country.DE, Country.DE, -589.470478, -597.114338, 479.041916, 0.000000, -70.550332, 125.748503, 0.000000, 0.000000, 62.874251, 0.000000);
        validateFlowDecomposition(flowDecompositionResults, "FFR2AA11 DDE3AA11 1_contingency_split_network", "FFR2AA11 DDE3AA11 1", "contingency_split_network", Country.FR, Country.DE, 1089.444201, 1097.114338, 879.041916, 0.000000, -70.550332, 0.000000, 0.000000, 225.748503, 62.874251, 0.000000);
        validateFlowDecomposition(flowDecompositionResults, "BBE1AA11 BBE3AA11 2", "BBE1AA11 BBE3AA11 2", "", Country.BE, Country.BE, -146.328661, -145.637341, -57.517013, 0.000000, 5.330657, 257.410628, 0.000000, -18.078212, -38.445548, -3.063171);
        validateFlowDecomposition(flowDecompositionResults, "BBE1AA11 FFR5AA11 1", "BBE1AA11 FFR5AA11 1", "", Country.BE, Country.FR, 506.004017, 504.865347, 623.029437, 0.000000, 9.328651, 0.000000, -23.215612, -31.636871, -67.279708, -5.360549);
        validateFlowDecomposition(flowDecompositionResults, "BBE1AA11 BBE3AA11 1", "BBE1AA11 BBE3AA11 1", "", Country.BE, Country.BE, -146.328661, -145.637341, -57.517013, 0.000000, 5.330657, 257.410628, 0.000000, -18.078212, -38.445548, -3.063171);
        validateFlowDecomposition(flowDecompositionResults, "NNL2AA11 NNL3AA11 1_contingency_split_network", "NNL2AA11 NNL3AA11 1", "contingency_split_network", Country.NL, Country.NL, NaN, NaN, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000);
        validateFlowDecomposition(flowDecompositionResults, "BBE4AA11 FFR5AA11 1_contingency_split_network", "BBE4AA11 FFR5AA11 1", "contingency_split_network", Country.BE, Country.FR, 0.000000, -0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000);
        validateFlowDecomposition(flowDecompositionResults, "BBE2AA11 BBE3AA11 1_contingency_desync", "BBE2AA11 BBE3AA11 1", "contingency_desync", Country.BE, Country.BE, 786.057076, 785.714286, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000);
        validateFlowDecomposition(flowDecompositionResults, "BBE1AA11 BBE3AA11 1_contingency_1", "BBE1AA11 BBE3AA11 1", "contingency_1", Country.BE, Country.BE, -428.685692, -428.571429, 157.894737, 0.000000, 0.000000, 270.676692, 0.000000, 0.000000, 0.000000, -0.000000);
        validateFlowDecomposition(flowDecompositionResults, "BBE1AA11 FFR5AA11 1_contingency_split_network", "BBE1AA11 FFR5AA11 1", "contingency_split_network", Country.BE, Country.FR, 0.000000, -0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000);
        validateFlowDecomposition(flowDecompositionResults, "FFR4AA11 DDE1AA11 1_contingency_desync", "FFR4AA11 DDE1AA11 1", "contingency_desync", Country.FR, Country.DE, 773.629497, 768.590441, 613.972056, 0.000000, 47.033555, 0.000000, 0.000000, 149.500998, -41.916168, 0.000000);
        validateFlowDecomposition(flowDecompositionResults, "BBE1AA11 BBE2AA11 1_contingency_1", "BBE1AA11 BBE2AA11 1", "contingency_1", Country.BE, Country.BE, -1213.942924, -1214.285714, 473.684211, 0.000000, 0.000000, 740.601504, 0.000000, 0.000000, 0.000000, -0.000000);
        validateFlowDecomposition(flowDecompositionResults, "FFR1AA11 FFR3AA11 1_contingency_1", "FFR1AA11 FFR3AA11 1", "contingency_1", Country.FR, Country.FR, -716.378043, -722.754012, 639.520958, 0.000000, -64.671138, 182.634731, 0.000000, -34.730539, 0.000000, 0.000000);
        validateFlowDecomposition(flowDecompositionResults, "DDE1AA11 DDE2AA11 1", "DDE1AA11 DDE2AA11 1", "", Country.DE, Country.DE, -32.786317, -41.496456, 275.464458, 0.000000, -79.599682, -310.228505, 22.520534, 0.000000, 128.139597, 5.200054);
        validateFlowDecomposition(flowDecompositionResults, "FFR1AA11 FFR2AA11 1", "FFR1AA11 FFR2AA11 1", "", Country.FR, Country.FR, 635.089500, 624.360688, 9.116240, 0.000000, 97.788750, 470.407689, -1.946219, 49.443616, 0.000000, -0.449387);
        validateFlowDecomposition(flowDecompositionResults, "FFR4AA11 DDE1AA11 1_contingency_split_network", "FFR4AA11 DDE1AA11 1", "contingency_split_network", Country.FR, Country.DE, 773.629497, 768.590441, 613.972056, 0.000000, 47.033555, 0.000000, 0.000000, 149.500998, -41.916168, 0.000000);
        validateFlowDecomposition(flowDecompositionResults, "NNL2AA11 NNL3AA11 1", "NNL2AA11 NNL3AA11 1", "", Country.NL, Country.NL, 825.270004, 826.846203, 731.794084, 0.000000, -12.438201, -55.352601, 30.954149, 42.182494, 89.706278, 0.000000);
        validateFlowDecomposition(flowDecompositionResults, "BBE4AA11 FFR5AA11 1_contingency_desync", "BBE4AA11 FFR5AA11 1", "contingency_desync", Country.BE, Country.FR, 0.000000, -0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000);
        validateFlowDecomposition(flowDecompositionResults, "DDE1AA11 DDE4AA11 1_contingency_desync", "DDE1AA11 DDE4AA11 1", "contingency_desync", Country.DE, Country.DE, -636.877548, -634.295221, 106.986028, 0.000000, 23.516777, 524.750499, 0.000000, 0.000000, -20.958084, 0.000000);
        validateFlowDecomposition(flowDecompositionResults, "NNL1AA11 NNL2AA11 1_contingency_split_network", "NNL1AA11 NNL2AA11 1", "contingency_split_network", Country.NL, Country.NL, NaN, NaN, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000);
        validateFlowDecomposition(flowDecompositionResults, "FFR4AA11 DDE4AA11 1_contingency_desync", "FFR4AA11 DDE4AA11 1", "contingency_desync", Country.FR, Country.DE, 136.883741, 134.295221, 506.986028, 0.000000, 23.516777, 0.000000, 0.000000, -375.249501, -20.958084, 0.000000);
        validateFlowDecomposition(flowDecompositionResults, "FFR1AA11 FFR5AA11 1", "FFR1AA11 FFR5AA11 1", "", Country.FR, Country.FR, NaN, NaN, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000);
        validateFlowDecomposition(flowDecompositionResults, "BBE3AA11 BBE4AA11 1_contingency_desync", "BBE3AA11 BBE4AA11 1", "contingency_desync", Country.BE, Country.BE, 428.685692, 428.571429, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000);
        validateFlowDecomposition(flowDecompositionResults, "BBE1AA11 FFR5AA11 1_contingency_1", "BBE1AA11 FFR5AA11 1", "contingency_1", Country.BE, Country.FR, 1000.000000, 1000.000000, 1000.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000);
        validateFlowDecomposition(flowDecompositionResults, "NNL1AA11 NNL3AA11 1", "NNL1AA11 NNL3AA11 1", "", Country.NL, Country.NL, 662.721962, 663.423102, 272.147042, 0.000000, -6.219100, 316.073700, 15.477075, 21.091247, 44.853139, 0.000000);
        validateFlowDecomposition(flowDecompositionResults, "FFR4AA11 DDE1AA11 1_contingency_1", "FFR4AA11 DDE1AA11 1", "contingency_1", Country.FR, Country.DE, 797.610007, 792.542537, 637.924152, 0.000000, 47.033555, 0.000000, 0.000000, 149.500998, -41.916168, 0.000000);
        validateFlowDecomposition(flowDecompositionResults, "FFR1AA11 FFR2AA11 1_contingency_desync", "FFR1AA11 FFR2AA11 1", "contingency_desync", Country.FR, Country.FR, 376.897452, 366.467785, -258.682635, 0.000000, 97.006707, 476.047904, 0.000000, 52.095808, 0.000000, 0.000000);
        validateFlowDecomposition(flowDecompositionResults, "FFR1AA11 FFR4AA11 1_contingency_desync", "FFR1AA11 FFR4AA11 1", "contingency_desync", Country.FR, Country.FR, 871.892782, 889.220358, 431.137725, 0.000000, -161.677845, 706.586826, 0.000000, -86.826347, 0.000000, 0.000000);
        validateFlowDecomposition(flowDecompositionResults, "FFR2AA11 FFR4AA11 1_contingency_1", "FFR2AA11 FFR4AA11 1", "contingency_1", Country.FR, Country.FR, 906.864968, 881.928776, 558.083832, 0.000000, 232.228178, 230.538922, 0.000000, -138.922156, 0.000000, 0.000000);
        validateFlowDecomposition(flowDecompositionResults, "FFR2AA11 FFR3AA11 2_contingency_1", "FFR2AA11 FFR3AA11 2", "contingency_1", Country.FR, Country.FR, -1391.810978, -1388.622994, 680.239521, 0.000000, 32.335569, 658.682635, 0.000000, 17.365269, 0.000000, 0.000000);
        validateFlowDecomposition(flowDecompositionResults, "NNL2AA11 NNL3AA11 1_contingency_1", "NNL2AA11 NNL3AA11 1", "contingency_1", Country.NL, Country.NL, 166.672696, 166.666667, 229.166667, 0.000000, -0.000000, -62.500000, -0.000000, 0.000000, 0.000000, 0.000000);
        validateFlowDecomposition(flowDecompositionResults, "FFR1AA11 FFR2AA11 1_contingency_split_network", "FFR1AA11 FFR2AA11 1", "contingency_split_network", Country.FR, Country.FR, 376.897452, 366.467785, -258.682635, 0.000000, 97.006707, 476.047904, 0.000000, 52.095808, 0.000000, 0.000000);
        validateFlowDecomposition(flowDecompositionResults, "FFR2AA11 FFR3AA11 1_contingency_1", "FFR2AA11 FFR3AA11 1", "contingency_1", Country.FR, Country.FR, -1391.810978, -1388.622994, 680.239521, 0.000000, 32.335569, 658.682635, 0.000000, 17.365269, 0.000000, 0.000000);
        validateFlowDecomposition(flowDecompositionResults, "FFR1AA11 FFR4AA11 1_contingency_1", "FFR1AA11 FFR4AA11 1", "contingency_1", Country.FR, Country.FR, 1039.625330, 1056.885029, 598.802395, 0.000000, -161.677845, 706.586826, 0.000000, -86.826347, 0.000000, 0.000000);
        validateFlowDecomposition(flowDecompositionResults, "BBE4AA11 FFR5AA11 1_contingency_1", "BBE4AA11 FFR5AA11 1", "contingency_1", Country.BE, Country.FR, 1000.000000, 1000.000000, 1000.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000);
        validateFlowDecomposition(flowDecompositionResults, "DDE2AA11 DDE3AA11 1", "DDE2AA11 DDE3AA11 1", "", Country.DE, Country.DE, -44.794351, -51.227151, 54.856666, 0.000000, -60.942381, 93.164421, -23.910690, 0.000000, -6.419820, -5.521045);
        validateFlowDecomposition(flowDecompositionResults, "BBE3AA11 BBE4AA11 1_contingency_split_network", "BBE3AA11 BBE4AA11 1", "contingency_split_network", Country.BE, Country.BE, 571.575251, 571.428571, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000);
        validateFlowDecomposition(flowDecompositionResults, "DDE2AA11 NNL3AA11 1", "DDE2AA11 NNL3AA11 1", "", Country.DE, Country.NL, -987.991966, -990.269305, 753.941126, 0.000000, -18.657301, 0.000000, 46.431224, 63.273741, 134.559417, 10.721099);
        validateFlowDecomposition(flowDecompositionResults, "FFR2AA11 FFR4AA11 1", "FFR2AA11 FFR4AA11 1", "", Country.FR, Country.FR, 688.000536, 662.527793, 391.042984, 0.000000, 236.361831, 200.726357, -10.287157, -152.940889, 0.000000, -2.375333);
        validateFlowDecomposition(flowDecompositionResults, "BBE4AA11 FFR5AA11 1", "BBE4AA11 FFR5AA11 1", "", Country.BE, Country.FR, 506.004017, 504.865347, 623.029437, 0.000000, 9.328651, 0.000000, -23.215612, -31.636871, -67.279708, -5.360549);
        validateFlowDecomposition(flowDecompositionResults, "FFR1AA11 FFR3AA11 1_contingency_split_network", "FFR1AA11 FFR3AA11 1", "contingency_split_network", Country.FR, Country.FR, -248.807353, -255.688143, 172.455090, 0.000000, -64.671138, 182.634731, 0.000000, -34.730539, 0.000000, 0.000000);
        validateFlowDecomposition(flowDecompositionResults, "BBE1AA11 BBE3AA11 2_contingency_1", "BBE1AA11 BBE3AA11 2", "contingency_1", Country.BE, Country.BE, -428.685692, -428.571429, 157.894737, 0.000000, 0.000000, 270.676692, 0.000000, 0.000000, 0.000000, -0.000000);
        validateFlowDecomposition(flowDecompositionResults, "FFR1AA11 FFR5AA11 1_contingency_1", "FFR1AA11 FFR5AA11 1", "contingency_1", Country.FR, Country.FR, NaN, NaN, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000, 0.000000);
        assertEquals(4, flowDecompositionResults.getZoneSet().size());
        assertTrue(flowDecompositionResults.getZoneSet().contains(Country.BE));
        assertTrue(flowDecompositionResults.getZoneSet().contains(Country.DE));
        assertTrue(flowDecompositionResults.getZoneSet().contains(Country.FR));
        assertTrue(flowDecompositionResults.getZoneSet().contains(Country.NL));
    }
}
