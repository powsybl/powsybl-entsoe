/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.flow_decomposition;

import com.powsybl.commons.PowsyblException;
import com.powsybl.flow_decomposition.xnec_provider.XnecProviderAllBranches;
import com.powsybl.flow_decomposition.xnec_provider.XnecProviderByIds;
import com.powsybl.flow_decomposition.xnec_provider.XnecProviderUnion;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Identifiable;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlowParameters;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.powsybl.flow_decomposition.TestUtils.validateFlowDecomposition;
import static com.powsybl.flow_decomposition.TestUtils.validateFlowDecompositionWithMap;
import static java.lang.Double.NaN;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 */
class FlowDecompositionTests {
    static void generateTestString(FlowDecompositionResults flowDecompositionResults) {
        // TODO remove this
        StringBuilder sb = new StringBuilder();
        flowDecompositionResults.getDecomposedFlowMap().entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .limit(100)
            .forEach(stringDecomposedFlowEntry -> {
                DecomposedFlow decomposedFlow = stringDecomposedFlowEntry.getValue();
                sb.append("validateFlowDecompositionWithMap(flowDecompositionResults, \"");
                sb.append(decomposedFlow.getId());
                sb.append("\", \"");
                sb.append(decomposedFlow.getBranchId());
                sb.append("\", \"");
                sb.append(decomposedFlow.getContingencyId());
                sb.append("\", Country.");
                sb.append(decomposedFlow.getCountry1());
                sb.append(", Country.");
                sb.append(decomposedFlow.getCountry2());
                sb.append(", ");
                sb.append(String.format("%.3f", decomposedFlow.getAcTerminal1ReferenceFlow()));
                sb.append(", ");
                sb.append(String.format("%.3f", decomposedFlow.getDcReferenceFlow()));
                sb.append(", ");
                sb.append(String.format("%.3f", decomposedFlow.getAllocatedFlow()));
                sb.append(", ");
                sb.append(String.format("%.3f", decomposedFlow.getXNodeFlow()));
                sb.append(", ");
                sb.append(String.format("%.3f", decomposedFlow.getPstFlow()));
                sb.append(", ");
                sb.append(String.format("%.3f", decomposedFlow.getInternalFlow()));
                boolean first = true;
                for (Country country : decomposedFlow.getLoopFlows().keySet().stream().sorted().toList()) {
                    if (Math.abs(decomposedFlow.getLoopFlows().get(country)) > 1e-3) {
                        if (first) {
                            sb.append(", Map.of(");
                        } else {
                            sb.append(", ");
                        }
                        sb.append(String.format("Country.%s, %.3f", country.name(), decomposedFlow.getLoopFlow(country)));
                        first = false;
                    }
                }
                if (first) {
                    sb.append(", Collections.emptyMap()");
                } else {
                    sb.append(")");
                }
                sb.append(");\n");

            });
        String s = sb.toString();
        System.out.println(s);
    }

    private static FlowDecompositionResults runFlowDecomposition(Network network, XnecProvider xnecProvider, FlowDecompositionParameters.FlowPartitionMode flowPartitionMode) {
        FlowDecompositionParameters flowDecompositionParameters = new FlowDecompositionParameters()
            .setEnableLossesCompensation(FlowDecompositionParameters.ENABLE_LOSSES_COMPENSATION)
            .setLossesCompensationEpsilon(FlowDecompositionParameters.DISABLE_LOSSES_COMPENSATION_EPSILON)
            .setSensitivityEpsilon(FlowDecompositionParameters.DISABLE_SENSITIVITY_EPSILON)
            .setRescaleMode(FlowDecompositionParameters.RescaleMode.NONE)
            .setFlowPartitioner(flowPartitionMode);
        FlowDecompositionComputer flowDecompositionComputer = new FlowDecompositionComputer(flowDecompositionParameters, LoadFlowParameters.load());
        FlowDecompositionResults flowDecompositionResults = flowDecompositionComputer.run(xnecProvider, network);
        generateTestString(flowDecompositionResults);
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

    private static void validateFlowDecompositionResultsUsingFLDMethodology(FlowDecompositionResults flowDecompositionResults) {
        validateFlowDecompositionWithMap(flowDecompositionResults, "BBE1AA11 BBE2AA11 1", "BBE1AA11 BBE2AA11 1", "", Country.BE, Country.BE, -1073.022, -1072.819, 833.251, 0.000, 2.665, 251.523, Map.of(Country.FR, -14.047, Country.DE, -1.944, Country.NL, 1.370));
        validateFlowDecompositionWithMap(flowDecompositionResults, "BBE1AA11 BBE2AA11 1_contingency_1", "BBE1AA11 BBE2AA11 1", "contingency_1", Country.BE, Country.BE, -1213.943, -1214.286, 990.286, 0.000, 0.000, 224.000, Collections.emptyMap());
        validateFlowDecompositionWithMap(flowDecompositionResults, "BBE1AA11 BBE2AA11 1_contingency_desync", "BBE1AA11 BBE2AA11 1", "contingency_desync", Country.BE, Country.BE, -1213.943, -1214.286, 0.000, 0.000, 0.000, 0.000, Collections.emptyMap());
        validateFlowDecompositionWithMap(flowDecompositionResults, "BBE1AA11 BBE2AA11 1_contingency_split_network", "BBE1AA11 BBE2AA11 1", "contingency_split_network", Country.BE, Country.BE, -1285.274, -1285.714, 0.000, 0.000, 0.000, 0.000, Collections.emptyMap());
        validateFlowDecompositionWithMap(flowDecompositionResults, "BBE1AA11 BBE3AA11 1", "BBE1AA11 BBE3AA11 1", "", Country.BE, Country.BE, -146.329, -145.637, 91.419, 0.000, 5.331, 78.128, Map.of(Country.FR, -28.093, Country.DE, -3.888, Country.NL, 2.741));
        validateFlowDecompositionWithMap(flowDecompositionResults, "BBE1AA11 BBE3AA11 1_contingency_1", "BBE1AA11 BBE3AA11 1", "contingency_1", Country.BE, Country.BE, -428.686, -428.571, 336.571, 0.000, 0.000, 92.000, Collections.emptyMap());
        validateFlowDecompositionWithMap(flowDecompositionResults, "BBE1AA11 BBE3AA11 1_contingency_desync", "BBE1AA11 BBE3AA11 1", "contingency_desync", Country.BE, Country.BE, -428.686, -428.571, 0.000, 0.000, 0.000, 0.000, Collections.emptyMap());
        validateFlowDecompositionWithMap(flowDecompositionResults, "BBE1AA11 BBE3AA11 1_contingency_split_network", "BBE1AA11 BBE3AA11 1", "contingency_split_network", Country.BE, Country.BE, -571.575, -571.429, 0.000, 0.000, 0.000, 0.000, Collections.emptyMap());
        validateFlowDecompositionWithMap(flowDecompositionResults, "BBE1AA11 BBE3AA11 2", "BBE1AA11 BBE3AA11 2", "", Country.BE, Country.BE, -146.329, -145.637, 91.419, 0.000, 5.331, 78.128, Map.of(Country.FR, -28.093, Country.DE, -3.888, Country.NL, 2.741));
        validateFlowDecompositionWithMap(flowDecompositionResults, "BBE1AA11 BBE3AA11 2_contingency_1", "BBE1AA11 BBE3AA11 2", "contingency_1", Country.BE, Country.BE, -428.686, -428.571, 336.571, 0.000, 0.000, 92.000, Collections.emptyMap());
        validateFlowDecompositionWithMap(flowDecompositionResults, "BBE1AA11 BBE3AA11 2_contingency_desync", "BBE1AA11 BBE3AA11 2", "contingency_desync", Country.BE, Country.BE, -428.686, -428.571, 0.000, 0.000, 0.000, 0.000, Collections.emptyMap());
        validateFlowDecompositionWithMap(flowDecompositionResults, "BBE1AA11 BBE3AA11 2_contingency_split_network", "BBE1AA11 BBE3AA11 2", "contingency_split_network", Country.BE, Country.BE, -571.575, -571.429, 0.000, 0.000, 0.000, 0.000, Collections.emptyMap());
        validateFlowDecompositionWithMap(flowDecompositionResults, "BBE1AA11 FFR5AA11 1", "BBE1AA11 FFR5AA11 1", "", Country.BE, Country.FR, 506.004, 504.865, 553.753, 0.000, 9.329, 0.000, Map.of(Country.BE, -7.046, Country.FR, -49.163, Country.DE, -6.803, Country.NL, 4.796));
        validateFlowDecompositionWithMap(flowDecompositionResults, "BBE1AA11 FFR5AA11 1_contingency_1", "BBE1AA11 FFR5AA11 1", "contingency_1", Country.BE, Country.FR, 1000.000, 1000.000, 1000.000, 0.000, 0.000, 0.000, Collections.emptyMap());
        validateFlowDecompositionWithMap(flowDecompositionResults, "BBE1AA11 FFR5AA11 1_contingency_desync", "BBE1AA11 FFR5AA11 1", "contingency_desync", Country.BE, Country.FR, 0.000, -0.000, 0.000, 0.000, 0.000, 0.000, Collections.emptyMap());
        validateFlowDecompositionWithMap(flowDecompositionResults, "BBE1AA11 FFR5AA11 1_contingency_split_network", "BBE1AA11 FFR5AA11 1", "contingency_split_network", Country.BE, Country.FR, 0.000, -0.000, 0.000, 0.000, 0.000, 0.000, Collections.emptyMap());
        validateFlowDecompositionWithMap(flowDecompositionResults, "BBE2AA11 BBE3AA11 1", "BBE2AA11 BBE3AA11 1", "", Country.BE, Country.BE, 926.978, 927.181, 741.832, 0.000, -2.665, 173.394, Map.of(Country.FR, 14.047, Country.DE, 1.944, Country.NL, -1.370));
        validateFlowDecompositionWithMap(flowDecompositionResults, "BBE2AA11 BBE3AA11 1_contingency_1", "BBE2AA11 BBE3AA11 1", "contingency_1", Country.BE, Country.BE, 786.057, 785.714, 653.714, 0.000, 0.000, 132.000, Collections.emptyMap());
        validateFlowDecompositionWithMap(flowDecompositionResults, "BBE2AA11 BBE3AA11 1_contingency_desync", "BBE2AA11 BBE3AA11 1", "contingency_desync", Country.BE, Country.BE, 786.057, 785.714, 0.000, 0.000, 0.000, 0.000, Collections.emptyMap());
        validateFlowDecompositionWithMap(flowDecompositionResults, "BBE2AA11 BBE3AA11 1_contingency_split_network", "BBE2AA11 BBE3AA11 1", "contingency_split_network", Country.BE, Country.BE, 714.726, 714.286, 0.000, 0.000, 0.000, 0.000, Collections.emptyMap());
        validateFlowDecompositionWithMap(flowDecompositionResults, "BBE3AA11 BBE4AA11 1", "BBE3AA11 BBE4AA11 1", "", Country.BE, Country.BE, 146.329, 145.637, 91.419, 0.000, 5.331, 78.128, Map.of(Country.FR, -28.093, Country.DE, -3.888, Country.NL, 2.741));
        validateFlowDecompositionWithMap(flowDecompositionResults, "BBE3AA11 BBE4AA11 1_contingency_1", "BBE3AA11 BBE4AA11 1", "contingency_1", Country.BE, Country.BE, 428.686, 428.571, 336.571, 0.000, 0.000, 92.000, Collections.emptyMap());
        validateFlowDecompositionWithMap(flowDecompositionResults, "BBE3AA11 BBE4AA11 1_contingency_desync", "BBE3AA11 BBE4AA11 1", "contingency_desync", Country.BE, Country.BE, 428.686, 428.571, 0.000, 0.000, 0.000, 0.000, Collections.emptyMap());
        validateFlowDecompositionWithMap(flowDecompositionResults, "BBE3AA11 BBE4AA11 1_contingency_split_network", "BBE3AA11 BBE4AA11 1", "contingency_split_network", Country.BE, Country.BE, 571.575, 571.429, 0.000, 0.000, 0.000, 0.000, Collections.emptyMap());
        validateFlowDecompositionWithMap(flowDecompositionResults, "BBE4AA11 FFR5AA11 1", "BBE4AA11 FFR5AA11 1", "", Country.BE, Country.FR, 506.004, 504.865, 553.753, 0.000, 9.329, 0.000, Map.of(Country.BE, -7.046, Country.FR, -49.163, Country.DE, -6.803, Country.NL, 4.796));
        validateFlowDecompositionWithMap(flowDecompositionResults, "BBE4AA11 FFR5AA11 1_contingency_1", "BBE4AA11 FFR5AA11 1", "contingency_1", Country.BE, Country.FR, 1000.000, 1000.000, 1000.000, 0.000, 0.000, 0.000, Collections.emptyMap());
        validateFlowDecompositionWithMap(flowDecompositionResults, "BBE4AA11 FFR5AA11 1_contingency_desync", "BBE4AA11 FFR5AA11 1", "contingency_desync", Country.BE, Country.FR, 0.000, -0.000, 0.000, 0.000, 0.000, 0.000, Collections.emptyMap());
        validateFlowDecompositionWithMap(flowDecompositionResults, "BBE4AA11 FFR5AA11 1_contingency_split_network", "BBE4AA11 FFR5AA11 1", "contingency_split_network", Country.BE, Country.FR, 0.000, -0.000, 0.000, 0.000, 0.000, 0.000, Collections.emptyMap());
        validateFlowDecompositionWithMap(flowDecompositionResults, "DDE1AA11 DDE2AA11 1", "DDE1AA11 DDE2AA11 1", "", Country.DE, Country.DE, -32.786, -41.496, -28.078, 0.000, -79.600, 44.790, Map.of(Country.BE, 6.835, Country.FR, 102.201, Country.NL, -4.652));
        validateFlowDecompositionWithMap(flowDecompositionResults, "DDE1AA11 DDE2AA11 1_contingency_1", "DDE1AA11 DDE2AA11 1", "contingency_1", Country.DE, Country.DE, 446.490, 438.814, 338.075, 0.000, 70.550, 77.958, Map.of(Country.FR, -47.769));
        validateFlowDecompositionWithMap(flowDecompositionResults, "DDE1AA11 DDE2AA11 1_contingency_desync", "DDE1AA11 DDE2AA11 1", "contingency_desync", Country.DE, Country.DE, 410.528, 402.886, 373.198, 0.000, 70.550, 71.136, Map.of(Country.FR, -111.999));
        validateFlowDecompositionWithMap(flowDecompositionResults, "DDE1AA11 DDE2AA11 1_contingency_split_network", "DDE1AA11 DDE2AA11 1", "contingency_split_network", Country.DE, Country.DE, 410.528, 402.886, 373.198, 0.000, 70.550, 71.136, Map.of(Country.FR, -111.999));
        validateFlowDecompositionWithMap(flowDecompositionResults, "DDE1AA11 DDE4AA11 1", "DDE1AA11 DDE4AA11 1", "", Country.DE, Country.DE, -489.070, -486.168, 182.289, 0.000, 26.533, 312.140, Map.of(Country.BE, -2.278, Country.FR, -34.067, Country.NL, 1.551));
        validateFlowDecompositionWithMap(flowDecompositionResults, "DDE1AA11 DDE4AA11 1_contingency_1", "DDE1AA11 DDE4AA11 1", "contingency_1", Country.DE, Country.DE, -648.880, -646.271, 330.189, 0.000, 23.517, 308.489, Map.of(Country.FR, -15.923));
        validateFlowDecompositionWithMap(flowDecompositionResults, "DDE1AA11 DDE4AA11 1_contingency_desync", "DDE1AA11 DDE4AA11 1", "contingency_desync", Country.DE, Country.DE, -636.887, -634.295, 338.930, 0.000, 23.517, 309.181, Map.of(Country.FR, -37.333));
        validateFlowDecompositionWithMap(flowDecompositionResults, "DDE1AA11 DDE4AA11 1_contingency_split_network", "DDE1AA11 DDE4AA11 1", "contingency_split_network", Country.DE, Country.DE, -636.887, -634.295, 338.930, 0.000, 23.517, 309.181, Map.of(Country.FR, -37.333));
        validateFlowDecompositionWithMap(flowDecompositionResults, "DDE2AA11 DDE3AA11 1", "DDE2AA11 DDE3AA11 1", "", Country.DE, Country.DE, -44.794, -51.227, 79.428, 0.000, -60.942, 31.184, Map.of(Country.BE, -7.257, Country.FR, 3.875, Country.NL, 4.940));
        validateFlowDecompositionWithMap(flowDecompositionResults, "DDE2AA11 DDE3AA11 1_contingency_1", "DDE2AA11 DDE3AA11 1", "contingency_1", Country.DE, Country.DE, -553.510, -561.186, 509.434, 0.000, -70.550, 74.534, Map.of(Country.FR, 47.769));
        validateFlowDecompositionWithMap(flowDecompositionResults, "DDE2AA11 DDE3AA11 1_contingency_desync", "DDE2AA11 DDE3AA11 1", "contingency_desync", Country.DE, Country.DE, -589.472, -597.114, 483.211, 0.000, -70.550, 72.456, Map.of(Country.FR, 111.999));
        validateFlowDecompositionWithMap(flowDecompositionResults, "DDE2AA11 DDE3AA11 1_contingency_split_network", "DDE2AA11 DDE3AA11 1", "contingency_split_network", Country.DE, Country.DE, -589.472, -597.114, 483.211, 0.000, -70.550, 72.456, Map.of(Country.FR, 111.999));
        validateFlowDecompositionWithMap(flowDecompositionResults, "DDE2AA11 NNL3AA11 1", "DDE2AA11 NNL3AA11 1", "", Country.DE, Country.NL, -987.992, -990.269, 892.493, 0.000, -18.657, 0.000, Map.of(Country.BE, 14.093, Country.FR, 98.327, Country.DE, 13.606, Country.NL, -9.592));
        validateFlowDecompositionWithMap(flowDecompositionResults, "FFR1AA11 FFR2AA11 1", "FFR1AA11 FFR2AA11 1", "", Country.FR, Country.FR, 635.089, 624.361, 202.368, 0.000, 97.789, 316.150, Map.of(Country.BE, -0.591, Country.DE, 8.243, Country.NL, 0.402));
        validateFlowDecompositionWithMap(flowDecompositionResults, "FFR1AA11 FFR2AA11 1_contingency_1", "FFR1AA11 FFR2AA11 1", "contingency_1", Country.FR, Country.FR, 676.753, 665.869, 311.171, 0.000, 97.007, 240.491, Map.of(Country.DE, 17.200));
        validateFlowDecompositionWithMap(flowDecompositionResults, "FFR1AA11 FFR2AA11 1_contingency_desync", "FFR1AA11 FFR2AA11 1", "contingency_desync", Country.FR, Country.FR, 376.901, 366.468, 186.061, 0.000, 97.007, 66.679, Map.of(Country.DE, 16.721));
        validateFlowDecompositionWithMap(flowDecompositionResults, "FFR1AA11 FFR2AA11 1_contingency_split_network", "FFR1AA11 FFR2AA11 1", "contingency_split_network", Country.FR, Country.FR, 376.901, 366.468, 186.061, 0.000, 97.007, 66.679, Map.of(Country.DE, 16.721));
        validateFlowDecompositionWithMap(flowDecompositionResults, "FFR1AA11 FFR3AA11 1", "FFR1AA11 FFR3AA11 1", "", Country.FR, Country.FR, -414.303, -420.336, 376.826, 0.000, -58.973, 113.888, Map.of(Country.BE, -4.304, Country.DE, -10.031, Country.NL, 2.929));
        validateFlowDecompositionWithMap(flowDecompositionResults, "FFR1AA11 FFR3AA11 1_contingency_1", "FFR1AA11 FFR3AA11 1", "contingency_1", Country.FR, Country.FR, -716.378, -722.754, 678.630, 0.000, -64.671, 120.262, Map.of(Country.DE, -11.467));
        validateFlowDecompositionWithMap(flowDecompositionResults, "FFR1AA11 FFR3AA11 1_contingency_desync", "FFR1AA11 FFR3AA11 1", "contingency_desync", Country.FR, Country.FR, -248.809, -255.688, 233.288, 0.000, -64.671, 98.219, Map.of(Country.DE, -11.147));
        validateFlowDecompositionWithMap(flowDecompositionResults, "FFR1AA11 FFR3AA11 1_contingency_split_network", "FFR1AA11 FFR3AA11 1", "contingency_split_network", Country.FR, Country.FR, -248.809, -255.688, 233.288, 0.000, -64.671, 98.219, Map.of(Country.DE, -11.147));
        validateFlowDecompositionWithMap(flowDecompositionResults, "FFR1AA11 FFR4AA11 1", "FFR1AA11 FFR4AA11 1", "", Country.FR, Country.FR, 779.213, 795.976, 477.962, 0.000, -156.762, 494.235, Map.of(Country.BE, -3.713, Country.DE, -18.274, Country.NL, 2.527));
        validateFlowDecompositionWithMap(flowDecompositionResults, "FFR1AA11 FFR4AA11 1_contingency_1", "FFR1AA11 FFR4AA11 1", "contingency_1", Country.FR, Country.FR, 1039.625, 1056.885, 831.587, 0.000, -161.678, 415.643, Map.of(Country.DE, -28.667));
        validateFlowDecompositionWithMap(flowDecompositionResults, "FFR1AA11 FFR4AA11 1_contingency_desync", "FFR1AA11 FFR4AA11 1", "contingency_desync", Country.FR, Country.FR, 871.908, 889.220, 601.425, 0.000, -161.678, 477.340, Map.of(Country.DE, -27.868));
        validateFlowDecompositionWithMap(flowDecompositionResults, "FFR1AA11 FFR4AA11 1_contingency_split_network", "FFR1AA11 FFR4AA11 1", "contingency_split_network", Country.FR, Country.FR, 871.908, 889.220, 601.425, 0.000, -161.678, 477.340, Map.of(Country.DE, -27.868));
        validateFlowDecompositionWithMap(flowDecompositionResults, "FFR1AA11 FFR5AA11 1", "FFR1AA11 FFR5AA11 1", "", Country.FR, Country.FR, NaN, NaN, 0.000, 0.000, 0.000, 0.000, Collections.emptyMap());
        validateFlowDecompositionWithMap(flowDecompositionResults, "FFR1AA11 FFR5AA11 1_contingency_1", "FFR1AA11 FFR5AA11 1", "contingency_1", Country.FR, Country.FR, NaN, NaN, 0.000, 0.000, 0.000, 0.000, Collections.emptyMap());
        validateFlowDecompositionWithMap(flowDecompositionResults, "FFR1AA11 FFR5AA11 1_contingency_desync", "FFR1AA11 FFR5AA11 1", "contingency_desync", Country.FR, Country.FR, NaN, NaN, 0.000, 0.000, 0.000, 0.000, Collections.emptyMap());
        validateFlowDecompositionWithMap(flowDecompositionResults, "FFR1AA11 FFR5AA11 1_contingency_split_network", "FFR1AA11 FFR5AA11 1", "contingency_split_network", Country.FR, Country.FR, NaN, NaN, 0.000, 0.000, 0.000, 0.000, Collections.emptyMap());
        validateFlowDecompositionWithMap(flowDecompositionResults, "FFR2AA11 DDE3AA11 1", "FFR2AA11 DDE3AA11 1", "", Country.FR, Country.DE, 544.794, 551.227, 579.428, 0.000, -60.942, 0.000, Map.of(Country.BE, -7.257, Country.FR, 3.875, Country.DE, 31.184, Country.NL, 4.940));
        validateFlowDecompositionWithMap(flowDecompositionResults, "FFR2AA11 DDE3AA11 1_contingency_1", "FFR2AA11 DDE3AA11 1", "contingency_1", Country.FR, Country.DE, 1053.510, 1061.186, 1009.434, 0.000, -70.550, 0.000, Map.of(Country.FR, 47.769, Country.DE, 74.534));
        validateFlowDecompositionWithMap(flowDecompositionResults, "FFR2AA11 DDE3AA11 1_contingency_desync", "FFR2AA11 DDE3AA11 1", "contingency_desync", Country.FR, Country.DE, 1089.472, 1097.114, 983.211, 0.000, -70.550, 0.000, Map.of(Country.FR, 111.999, Country.DE, 72.456));
        validateFlowDecompositionWithMap(flowDecompositionResults, "FFR2AA11 DDE3AA11 1_contingency_split_network", "FFR2AA11 DDE3AA11 1", "contingency_split_network", Country.FR, Country.DE, 1089.472, 1097.114, 983.211, 0.000, -70.550, 0.000, Map.of(Country.FR, 111.999, Country.DE, 72.456));
        validateFlowDecompositionWithMap(flowDecompositionResults, "FFR2AA11 FFR3AA11 1", "FFR2AA11 FFR3AA11 1", "", Country.FR, Country.FR, -1048.853, -1044.697, 579.194, 0.000, 38.815, 430.038, Map.of(Country.BE, -4.894, Country.DE, -1.788, Country.NL, 3.331));
        validateFlowDecompositionWithMap(flowDecompositionResults, "FFR2AA11 FFR3AA11 1_contingency_1", "FFR2AA11 FFR3AA11 1", "contingency_1", Country.FR, Country.FR, -1391.811, -1388.623, 989.801, 0.000, 32.336, 360.753, Map.of(Country.DE, 5.733));
        validateFlowDecompositionWithMap(flowDecompositionResults, "FFR2AA11 FFR3AA11 1_contingency_desync", "FFR2AA11 FFR3AA11 1", "contingency_desync", Country.FR, Country.FR, -625.596, -622.156, 419.349, 0.000, 32.336, 164.898, Map.of(Country.DE, 5.574));
        validateFlowDecompositionWithMap(flowDecompositionResults, "FFR2AA11 FFR3AA11 1_contingency_split_network", "FFR2AA11 FFR3AA11 1", "contingency_split_network", Country.FR, Country.FR, -625.596, -622.156, 419.349, 0.000, 32.336, 164.898, Map.of(Country.DE, 5.574));
        validateFlowDecompositionWithMap(flowDecompositionResults, "FFR2AA11 FFR3AA11 2", "FFR2AA11 FFR3AA11 2", "", Country.FR, Country.FR, -1048.853, -1044.697, 579.194, 0.000, 38.815, 430.038, Map.of(Country.BE, -4.894, Country.DE, -1.788, Country.NL, 3.331));
        validateFlowDecompositionWithMap(flowDecompositionResults, "FFR2AA11 FFR3AA11 2_contingency_1", "FFR2AA11 FFR3AA11 2", "contingency_1", Country.FR, Country.FR, -1391.811, -1388.623, 989.801, 0.000, 32.336, 360.753, Map.of(Country.DE, 5.733));
        validateFlowDecompositionWithMap(flowDecompositionResults, "FFR2AA11 FFR3AA11 2_contingency_desync", "FFR2AA11 FFR3AA11 2", "contingency_desync", Country.FR, Country.FR, -625.596, -622.156, 419.349, 0.000, 32.336, 164.898, Map.of(Country.DE, 5.574));
        validateFlowDecompositionWithMap(flowDecompositionResults, "FFR2AA11 FFR3AA11 2_contingency_split_network", "FFR2AA11 FFR3AA11 2", "contingency_split_network", Country.FR, Country.FR, -625.596, -622.156, 419.349, 0.000, 32.336, 164.898, Map.of(Country.DE, 5.574));
        validateFlowDecompositionWithMap(flowDecompositionResults, "FFR2AA11 FFR4AA11 1", "FFR2AA11 FFR4AA11 1", "", Country.FR, Country.FR, 688.001, 662.528, 275.595, 0.000, 236.362, 178.085, Map.of(Country.BE, -3.122, Country.DE, -26.517, Country.NL, 2.125));
        validateFlowDecompositionWithMap(flowDecompositionResults, "FFR2AA11 FFR4AA11 1_contingency_1", "FFR2AA11 FFR4AA11 1", "contingency_1", Country.FR, Country.FR, 906.865, 881.929, 520.416, 0.000, 232.228, 175.152, Map.of(Country.DE, -45.867));
        validateFlowDecompositionWithMap(flowDecompositionResults, "FFR2AA11 FFR4AA11 1_contingency_desync", "FFR2AA11 FFR4AA11 1", "contingency_desync", Country.FR, Country.FR, 1038.621, 1013.665, 415.364, 0.000, 232.228, 410.661, Map.of(Country.DE, -44.588));
        validateFlowDecompositionWithMap(flowDecompositionResults, "FFR2AA11 FFR4AA11 1_contingency_split_network", "FFR2AA11 FFR4AA11 1", "contingency_split_network", Country.FR, Country.FR, 1038.621, 1013.665, 415.364, 0.000, 232.228, 410.661, Map.of(Country.DE, -44.588));
        validateFlowDecompositionWithMap(flowDecompositionResults, "FFR3AA11 FFR5AA11 1", "FFR3AA11 FFR5AA11 1", "", Country.FR, Country.FR, -1012.008, -1009.731, 1107.507, 0.000, 18.657, -98.327, Map.of(Country.BE, -14.093, Country.DE, -13.606, Country.NL, 9.592));
        validateFlowDecompositionWithMap(flowDecompositionResults, "FFR3AA11 FFR5AA11 1_contingency_1", "FFR3AA11 FFR5AA11 1", "contingency_1", Country.FR, Country.FR, -2000.000, -2000.000, 2000.000, 0.000, 0.000, 0.000, Collections.emptyMap());
        validateFlowDecompositionWithMap(flowDecompositionResults, "FFR4AA11 DDE1AA11 1", "FFR4AA11 DDE1AA11 1", "", Country.FR, Country.DE, 478.144, 472.336, 355.184, 0.000, 53.066, 0.000, Map.of(Country.BE, -4.557, Country.FR, -68.134, Country.DE, 133.675, Country.NL, 3.102));
        validateFlowDecompositionWithMap(flowDecompositionResults, "FFR4AA11 DDE1AA11 1_contingency_1", "FFR4AA11 DDE1AA11 1", "contingency_1", Country.FR, Country.DE, 797.610, 792.543, 660.377, 0.000, 47.034, 0.000, Map.of(Country.FR, -31.846, Country.DE, 116.977));
        validateFlowDecompositionWithMap(flowDecompositionResults, "FFR4AA11 DDE1AA11 1_contingency_desync", "FFR4AA11 DDE1AA11 1", "contingency_desync", Country.FR, Country.DE, 773.642, 768.590, 677.860, 0.000, 47.034, 0.000, Map.of(Country.FR, -74.666, Country.DE, 118.363));
        validateFlowDecompositionWithMap(flowDecompositionResults, "FFR4AA11 DDE1AA11 1_contingency_split_network", "FFR4AA11 DDE1AA11 1", "contingency_split_network", Country.FR, Country.DE, 773.642, 768.590, 677.860, 0.000, 47.034, 0.000, Map.of(Country.FR, -74.666, Country.DE, 118.363));
        validateFlowDecompositionWithMap(flowDecompositionResults, "FFR4AA11 DDE4AA11 1", "FFR4AA11 DDE4AA11 1", "", Country.FR, Country.DE, -10.930, -13.832, -172.895, 0.000, -26.533, 0.000, Map.of(Country.BE, 2.278, Country.FR, 34.067, Country.DE, 178.465, Country.NL, -1.551));
        validateFlowDecompositionWithMap(flowDecompositionResults, "FFR4AA11 DDE4AA11 1_contingency_1", "FFR4AA11 DDE4AA11 1", "contingency_1", Country.FR, Country.DE, 148.880, 146.271, 330.189, 0.000, 23.517, 0.000, Map.of(Country.FR, -15.923, Country.DE, -191.511));
        validateFlowDecompositionWithMap(flowDecompositionResults, "FFR4AA11 DDE4AA11 1_contingency_desync", "FFR4AA11 DDE4AA11 1", "contingency_desync", Country.FR, Country.DE, 136.887, 134.295, 338.930, 0.000, 23.517, 0.000, Map.of(Country.FR, -37.333, Country.DE, -190.819));
        validateFlowDecompositionWithMap(flowDecompositionResults, "FFR4AA11 DDE4AA11 1_contingency_split_network", "FFR4AA11 DDE4AA11 1", "contingency_split_network", Country.FR, Country.DE, 136.887, 134.295, 338.930, 0.000, 23.517, 0.000, Map.of(Country.FR, -37.333, Country.DE, -190.819));
        validateFlowDecompositionWithMap(flowDecompositionResults, "NNL1AA11 NNL2AA11 1", "NNL1AA11 NNL2AA11 1", "", Country.NL, Country.NL, -162.722, -163.423, 186.749, 0.000, -6.219, -59.116, Map.of(Country.BE, 4.698, Country.FR, 32.776, Country.DE, 4.535));
        validateFlowDecompositionWithMap(flowDecompositionResults, "NNL1AA11 NNL2AA11 1_contingency_1", "NNL1AA11 NNL2AA11 1", "contingency_1", Country.NL, Country.NL, 166.673, 166.667, -41.667, 0.000, 0.000, 208.333, Collections.emptyMap());
        validateFlowDecompositionWithMap(flowDecompositionResults, "NNL1AA11 NNL2AA11 1_contingency_desync", "NNL1AA11 NNL2AA11 1", "contingency_desync", Country.NL, Country.NL, 166.673, 166.667, 0.000, 0.000, 0.000, 0.000, Collections.emptyMap());
        validateFlowDecompositionWithMap(flowDecompositionResults, "NNL1AA11 NNL2AA11 1_contingency_split_network", "NNL1AA11 NNL2AA11 1", "contingency_split_network", Country.NL, Country.NL, NaN, NaN, 0.000, 0.000, 0.000, 0.000, Collections.emptyMap());
        validateFlowDecompositionWithMap(flowDecompositionResults, "NNL1AA11 NNL3AA11 1", "NNL1AA11 NNL3AA11 1", "", Country.NL, Country.NL, 662.722, 663.423, 518.994, 0.000, -6.219, 108.639, Map.of(Country.BE, 4.698, Country.FR, 32.776, Country.DE, 4.535));
        validateFlowDecompositionWithMap(flowDecompositionResults, "NNL1AA11 NNL3AA11 1_contingency_1", "NNL1AA11 NNL3AA11 1", "contingency_1", Country.NL, Country.NL, 333.327, 333.333, 41.667, 0.000, 0.000, 291.667, Collections.emptyMap());
        validateFlowDecompositionWithMap(flowDecompositionResults, "NNL1AA11 NNL3AA11 1_contingency_desync", "NNL1AA11 NNL3AA11 1", "contingency_desync", Country.NL, Country.NL, 333.327, 333.333, 0.000, 0.000, 0.000, 0.000, Collections.emptyMap());
        validateFlowDecompositionWithMap(flowDecompositionResults, "NNL1AA11 NNL3AA11 1_contingency_split_network", "NNL1AA11 NNL3AA11 1", "contingency_split_network", Country.NL, Country.NL, NaN, NaN, 0.000, 0.000, 0.000, 0.000, Collections.emptyMap());
        validateFlowDecompositionWithMap(flowDecompositionResults, "NNL2AA11 BBE3AA11 1", "NNL2AA11 BBE3AA11 1", "", Country.NL, Country.BE, -1487.992, -1490.269, 1392.493, 0.000, -18.657, 0.000, Map.of(Country.BE, 14.093, Country.FR, 98.327, Country.DE, 13.606, Country.NL, -9.592));
        validateFlowDecompositionWithMap(flowDecompositionResults, "NNL2AA11 BBE3AA11 1_contingency_1", "NNL2AA11 BBE3AA11 1", "contingency_1", Country.NL, Country.BE, -500.000, -500.000, 500.000, 0.000, 0.000, 0.000, Collections.emptyMap());
        validateFlowDecompositionWithMap(flowDecompositionResults, "NNL2AA11 BBE3AA11 1_contingency_desync", "NNL2AA11 BBE3AA11 1", "contingency_desync", Country.NL, Country.BE, -500.000, -500.000, 0.000, 0.000, 0.000, 0.000, Collections.emptyMap());
        validateFlowDecompositionWithMap(flowDecompositionResults, "NNL2AA11 NNL3AA11 1", "NNL2AA11 NNL3AA11 1", "", Country.NL, Country.NL, 825.270, 826.846, 705.744, 0.000, -12.438, 49.524, Map.of(Country.BE, 9.395, Country.FR, 65.551, Country.DE, 9.071));
        validateFlowDecompositionWithMap(flowDecompositionResults, "NNL2AA11 NNL3AA11 1_contingency_1", "NNL2AA11 NNL3AA11 1", "contingency_1", Country.NL, Country.NL, 166.673, 166.667, 83.333, 0.000, 0.000, 83.333, Collections.emptyMap());
        validateFlowDecompositionWithMap(flowDecompositionResults, "NNL2AA11 NNL3AA11 1_contingency_desync", "NNL2AA11 NNL3AA11 1", "contingency_desync", Country.NL, Country.NL, 166.673, 166.667, 0.000, 0.000, 0.000, 0.000, Collections.emptyMap());
        validateFlowDecompositionWithMap(flowDecompositionResults, "NNL2AA11 NNL3AA11 1_contingency_split_network", "NNL2AA11 NNL3AA11 1", "contingency_split_network", Country.NL, Country.NL, NaN, NaN, 0.000, 0.000, 0.000, 0.000, Collections.emptyMap());
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
    void testFlowDecompositionOnNetworkWithBusBarSectionOnly(FlowDecompositionParameters.FlowPartitionMode flowPartitionMode) {
        Network network = TestUtils.getMicroGridNetworkWithBusBarSectionOnly();

        if (Set.of(FlowDecompositionParameters.FlowPartitionMode.FULL_LINE_DECOMPOSITION, FlowDecompositionParameters.FlowPartitionMode.FAST_FULL_LINE_DECOMPOSITION).contains(flowPartitionMode)) {
            // TODO fix this test, FLD does not support three winding transformers
            PowsyblException exception = assertThrows(PowsyblException.class, () -> runFlowDecomposition(network, new XnecProviderAllBranches(), flowPartitionMode));
            assertEquals("Nodal generation and load do not match for vertex associated with bus: b10b171b-3bc5-4849-bb1f-61ed9ea1ec7c_0", exception.getMessage());
        } else {
            FlowDecompositionResults flowDecompositionResults = runFlowDecomposition(network, new XnecProviderAllBranches(), flowPartitionMode);
            assertEquals(6, flowDecompositionResults.getDecomposedFlowMap().size());
            validateFlowDecompositionWithMap(flowDecompositionResults, "a708c3bc-465d-4fe7-b6ef-6fa6408a62b0", "a708c3bc-465d-4fe7-b6ef-6fa6408a62b0", "", Country.BE, Country.BE, 105.335, 115.129, -8.896, 30.705, 33.030, 60.289, Collections.emptyMap());
            validateFlowDecompositionWithMap(flowDecompositionResults, "b58bf21a-096a-4dae-9a01-3f03b60c24c7", "b58bf21a-096a-4dae-9a01-3f03b60c24c7", "", Country.BE, Country.BE, -116.324, -118.550, -0.000, 126.160, -0.000, -7.610, Collections.emptyMap());
            validateFlowDecompositionWithMap(flowDecompositionResults, "b94318f6-6d24-4f56-96b9-df2531ad6543", "b94318f6-6d24-4f56-96b9-df2531ad6543", "", Country.BE, Country.BE, 0.299, 0.289, -5.505, 76.274, -33.030, -37.450, Collections.emptyMap());
            validateFlowDecompositionWithMap(flowDecompositionResults, "df16b3dd-c905-4a6f-84ee-f067be86f5da", "df16b3dd-c905-4a6f-84ee-f067be86f5da", "", Country.BE, Country.BE, -99.797, -103.741, 0.000, 103.741, -0.000, -0.000, Collections.emptyMap());
            validateFlowDecompositionWithMap(flowDecompositionResults, "e482b89a-fa84-4ea9-8e70-a83d44790957", "e482b89a-fa84-4ea9-8e70-a83d44790957", "", Country.BE, Country.BE, -94.902, -84.583, 14.401, -106.980, -0.000, 177.161, Collections.emptyMap());
            validateFlowDecompositionWithMap(flowDecompositionResults, "ffbabc27-1ccd-4fdc-b037-e341706c8d29", "ffbabc27-1ccd-4fdc-b037-e341706c8d29", "", Country.BE, Country.BE, -53.551, -57.104, -0.000, 60.770, -0.000, -3.666, Collections.emptyMap());
            assertEquals(1, flowDecompositionResults.getZoneSet().size());
            assertTrue(flowDecompositionResults.getZoneSet().contains(Country.BE));
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

        if (Set.of(FlowDecompositionParameters.FlowPartitionMode.FULL_LINE_DECOMPOSITION, FlowDecompositionParameters.FlowPartitionMode.FAST_FULL_LINE_DECOMPOSITION).contains(flowPartitionMode)) {
            // TODO fix this test, FLD does not support three winding transformers
            PowsyblException exception = assertThrows(PowsyblException.class, () -> runFlowDecomposition(network, new XnecProviderAllBranches(), flowPartitionMode));
            assertEquals("Nodal generation and load do not match for vertex associated with bus: b10b171b-3bc5-4849-bb1f-61ed9ea1ec7c_0", exception.getMessage());
        } else {
            FlowDecompositionResults flowDecompositionResults = runFlowDecomposition(network, new XnecProviderAllBranches(), flowPartitionMode);
            assertEquals(6, flowDecompositionResults.getDecomposedFlowMap().size());
            validateFlowDecompositionWithMap(flowDecompositionResults, "a708c3bc-465d-4fe7-b6ef-6fa6408a62b0", "a708c3bc-465d-4fe7-b6ef-6fa6408a62b0", "", Country.BE, Country.BE, 105.189, 115.160, -8.896, 30.705, 33.030, 60.321, Collections.emptyMap());
            validateFlowDecompositionWithMap(flowDecompositionResults, "b58bf21a-096a-4dae-9a01-3f03b60c24c7", "b58bf21a-096a-4dae-9a01-3f03b60c24c7", "", Country.BE, Country.BE, -116.310, -118.537, -0.000, 126.160, -0.000, -7.623, Collections.emptyMap());
            validateFlowDecompositionWithMap(flowDecompositionResults, "b94318f6-6d24-4f56-96b9-df2531ad6543", "b94318f6-6d24-4f56-96b9-df2531ad6543", "", Country.BE, Country.BE, -0.732, 0.248, -5.505, 76.274, -33.030, -37.492, Collections.emptyMap());
            validateFlowDecompositionWithMap(flowDecompositionResults, "df16b3dd-c905-4a6f-84ee-f067be86f5da", "df16b3dd-c905-4a6f-84ee-f067be86f5da", "", Country.BE, Country.BE, -99.793, -103.741, 0.000, 103.741, -0.000, -0.000, Collections.emptyMap());
            validateFlowDecompositionWithMap(flowDecompositionResults, "e482b89a-fa84-4ea9-8e70-a83d44790957", "e482b89a-fa84-4ea9-8e70-a83d44790957", "", Country.BE, Country.BE, -95.912, -84.604, 14.401, -106.980, -0.000, 177.183, Collections.emptyMap());
            validateFlowDecompositionWithMap(flowDecompositionResults, "ffbabc27-1ccd-4fdc-b037-e341706c8d29", "ffbabc27-1ccd-4fdc-b037-e341706c8d29", "", Country.BE, Country.BE, -53.540, -57.098, -0.000, 60.770, -0.000, -3.672, Collections.emptyMap());
            assertEquals(1, flowDecompositionResults.getZoneSet().size());
            assertTrue(flowDecompositionResults.getZoneSet().contains(Country.BE));
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
        validateFlowDecompositionWithMap(flowDecompositionResults, "L1", "L1", "", Country.FR, Country.FR, 100.266, 100.000, 0.000, 0.000, 0.000, 100.000, Collections.emptyMap());
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
    void testFlowDecompositionOnHvdcNetworkUsingMode(FlowDecompositionParameters.FlowPartitionMode flowPartitionMode) {
        FlowDecompositionResults flowDecompositionResults = testFlowDecompositionOnHvdcNetwork(flowPartitionMode);
        if (Set.of(FlowDecompositionParameters.FlowPartitionMode.FULL_LINE_DECOMPOSITION, FlowDecompositionParameters.FlowPartitionMode.FAST_FULL_LINE_DECOMPOSITION).contains(flowPartitionMode)) {
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

        if (Set.of(FlowDecompositionParameters.FlowPartitionMode.FULL_LINE_DECOMPOSITION, FlowDecompositionParameters.FlowPartitionMode.FAST_FULL_LINE_DECOMPOSITION).contains(flowPartitionMode)) {
            validateFlowDecompositionWithMap(flowDecompositionResults, "BGEN  11 BLOAD 11 1", "BGEN  11 BLOAD 11 1", "", Country.BE, Country.BE, NaN, 300.000, 200.000, 0.000, 0.000, 33.333, Map.of(Country.FR, 33.333, Country.ES, 33.333));
            validateFlowDecompositionWithMap(flowDecompositionResults, "BLOAD 11 FLOAD 11 1", "BLOAD 11 FLOAD 11 1", "", Country.BE, Country.FR, NaN, 200.000, 133.333, 0.000, 0.000, 0.000, Map.of(Country.FR, 33.333, Country.ES, 33.333));
            validateFlowDecompositionWithMap(flowDecompositionResults, "EGEN  11 FGEN  11 1", "EGEN  11 FGEN  11 1", "", Country.ES, Country.FR, NaN, 100.000, 66.667, 0.000, 0.000, 0.000, Map.of(Country.ES, 33.333));
            validateFlowDecompositionWithMap(flowDecompositionResults, "FGEN  11 BGEN  11 1", "FGEN  11 BGEN  11 1", "", Country.FR, Country.BE, NaN, 200.000, 133.333, 0.000, 0.000, 0.000, Map.of(Country.FR, 33.333, Country.ES, 33.333));
            validateFlowDecompositionWithMap(flowDecompositionResults, "FLOAD 11 ELOAD 11 1", "FLOAD 11 ELOAD 11 1", "", Country.FR, Country.ES, NaN, 100.000, 66.667, 0.000, 0.000, 0.000, Map.of(Country.ES, 33.333));
        } else {
            validateFlowDecompositionWithMap(flowDecompositionResults, "BGEN  11 BLOAD 11 1", "BGEN  11 BLOAD 11 1", "", Country.BE, Country.BE, NaN, 300.000, 0.000, 0.000, 0.000, 100.000, Map.of(Country.ES, 100.000, Country.FR, 100.000));
            validateFlowDecompositionWithMap(flowDecompositionResults, "BLOAD 11 FLOAD 11 1", "BLOAD 11 FLOAD 11 1", "", Country.BE, Country.FR, NaN, 200.000, -0.000, 0.000, 0.000, 0.000, Map.of(Country.ES, 100.000, Country.FR, 100.000));
            validateFlowDecompositionWithMap(flowDecompositionResults, "EGEN  11 FGEN  11 1", "EGEN  11 FGEN  11 1", "", Country.ES, Country.FR, NaN, 100.000, -0.000, 0.000, 0.000, 0.000, Map.of(Country.ES, 100.000));
            validateFlowDecompositionWithMap(flowDecompositionResults, "FGEN  11 BGEN  11 1", "FGEN  11 BGEN  11 1", "", Country.FR, Country.BE, NaN, 200.000, -0.000, 0.000, 0.000, 0.000, Map.of(Country.ES, 100.000, Country.FR, 100.000));
            validateFlowDecompositionWithMap(flowDecompositionResults, "FLOAD 11 ELOAD 11 1", "FLOAD 11 ELOAD 11 1", "", Country.FR, Country.ES, NaN, 100.000, 0.000, 0.000, 0.000, 0.000, Map.of(Country.ES, 100.000));
        }

        assertEquals(3, flowDecompositionResults.getZoneSet().size());
        assertTrue(flowDecompositionResults.getZoneSet().contains(Country.BE));
        assertTrue(flowDecompositionResults.getZoneSet().contains(Country.ES));
        assertTrue(flowDecompositionResults.getZoneSet().contains(Country.FR));
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
        generateTestString(flowDecompositionResults);
        assertEquals(3, flowDecompositionResults.getDecomposedFlowMap().size());

        if (Set.of(FlowDecompositionParameters.FlowPartitionMode.FULL_LINE_DECOMPOSITION, FlowDecompositionParameters.FlowPartitionMode.FAST_FULL_LINE_DECOMPOSITION).contains(flowPartitionMode)) {
            validateFlowDecompositionWithMap(flowDecompositionResults, "BLOAD 11 BLOAD 12 2", "BLOAD 11 BLOAD 12 2", "", Country.BE, Country.BE, -160.006, -168.543, 4.890, 0.000, 163.653, 0.000, Collections.emptyMap());
            validateFlowDecompositionWithMap(flowDecompositionResults, "FGEN  11 BLOAD 11 1", "FGEN  11 BLOAD 11 1", "", Country.FR, Country.BE, 192.391, 200.671, 37.019, 0.000, 163.653, 0.000, Collections.emptyMap());
            validateFlowDecompositionWithMap(flowDecompositionResults, "FGEN  11 BLOAD 12 1", "FGEN  11 BLOAD 12 1", "", Country.FR, Country.BE, -76.188, -84.725, -78.927, 0.000, 163.653, 0.000, Collections.emptyMap());
        } else {
            validateFlowDecompositionWithMap(flowDecompositionResults, "BLOAD 11 BLOAD 12 2", "BLOAD 11 BLOAD 12 2", "", Country.BE, Country.BE, -160.006, -168.543, 29.016, 0.000, 163.653, -24.111, Map.of(Country.FR, -0.015));
            validateFlowDecompositionWithMap(flowDecompositionResults, "FGEN  11 BLOAD 11 1", "FGEN  11 BLOAD 11 1", "", Country.FR, Country.BE, 192.391, 200.671, 29.016, 0.000, 163.653, 0.000, Map.of(Country.BE, 8.017, Country.FR, -0.015));
            validateFlowDecompositionWithMap(flowDecompositionResults, "FGEN  11 BLOAD 12 1", "FGEN  11 BLOAD 12 1", "", Country.FR, Country.BE, -76.188, -84.725, -87.048, 0.000, 163.653, 0.000, Map.of(Country.BE, 8.076, Country.FR, 0.044));
        }
        assertEquals(2, flowDecompositionResults.getZoneSet().size());
        assertTrue(flowDecompositionResults.getZoneSet().contains(Country.BE));
        assertTrue(flowDecompositionResults.getZoneSet().contains(Country.FR));
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
        assertEquals(3, flowDecompositionResults.getDecomposedFlowMap().size());

        if (Set.of(FlowDecompositionParameters.FlowPartitionMode.FULL_LINE_DECOMPOSITION, FlowDecompositionParameters.FlowPartitionMode.FAST_FULL_LINE_DECOMPOSITION).contains(flowPartitionMode)) {
            validateFlowDecompositionWithMap(flowDecompositionResults, "BLOAD 11 BGEN2 11 1", "BLOAD 11 BGEN2 11 1", "", Country.BE, Country.BE, -101.421, -101.420, 0.000, 0.000, 0.000, 101.420, Collections.emptyMap());
            validateFlowDecompositionWithMap(flowDecompositionResults, "FGEN1 11 FLOAD 11 1", "FGEN1 11 FLOAD 11 1", "", Country.FR, Country.FR, 104.268, 101.424, 98.580, 0.000, 0.000, 2.844, Collections.emptyMap());
            validateFlowDecompositionWithMap(flowDecompositionResults, "FLOAD 11 BLOAD 11 1", "FLOAD 11 BLOAD 11 1", "", Country.FR, Country.BE, 101.424, 98.580, 98.580, 0.000, 0.000, 0.000, Collections.emptyMap());
        } else {
            validateFlowDecompositionWithMap(flowDecompositionResults, "BLOAD 11 BGEN2 11 1", "BLOAD 11 BGEN2 11 1", "", Country.BE, Country.BE, -101.421, -101.420, -100.001, 0.000, 0.000, 200.711, Map.of(Country.FR, 0.711));
            validateFlowDecompositionWithMap(flowDecompositionResults, "FGEN1 11 FLOAD 11 1", "FGEN1 11 FLOAD 11 1", "", Country.FR, Country.FR, 104.268, 101.424, 100.001, 0.000, 0.000, 2.133, Map.of(Country.BE, -0.711));
            validateFlowDecompositionWithMap(flowDecompositionResults, "FLOAD 11 BLOAD 11 1", "FLOAD 11 BLOAD 11 1", "", Country.FR, Country.BE, 101.424, 98.580, 100.001, 0.000, 0.000, 0.000, Map.of(Country.FR, -0.711, Country.BE, -0.711));
        }

        assertEquals(2, flowDecompositionResults.getZoneSet().size());
        assertTrue(flowDecompositionResults.getZoneSet().contains(Country.BE));
        assertTrue(flowDecompositionResults.getZoneSet().contains(Country.FR));
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
        generateTestString(flowDecompositionResults);
        assertEquals(2, flowDecompositionResults.getDecomposedFlowMap().size());

        if (Set.of(FlowDecompositionParameters.FlowPartitionMode.FULL_LINE_DECOMPOSITION, FlowDecompositionParameters.FlowPartitionMode.FAST_FULL_LINE_DECOMPOSITION).contains(flowPartitionMode)) {
            // TODO fix this test, xnode flow is not supported
            validateFlowDecompositionWithMap(flowDecompositionResults, "BLOAD 11 BGEN2 11 1", "BLOAD 11 BGEN2 11 1", "", Country.BE, Country.BE, -100.070, -100.000, 0.000, 75.000, 0.000, 25.000, Collections.emptyMap());
            validateFlowDecompositionWithMap(flowDecompositionResults, "FGEN1 11 BLOAD 11 1", "FGEN1 11 BLOAD 11 1", "", Country.FR, Country.BE, 100.133, 100.000, 25.000, 75.000, 0.000, 0.000, Collections.emptyMap());
        } else {
            validateFlowDecompositionWithMap(flowDecompositionResults, "BLOAD 11 BGEN2 11 1", "BLOAD 11 BGEN2 11 1", "", Country.BE, Country.BE, -100.070, -100.000, -25.102, 75.000, 0.000, 50.051, Map.of(Country.FR, 0.051));
            validateFlowDecompositionWithMap(flowDecompositionResults, "FGEN1 11 BLOAD 11 1", "FGEN1 11 BLOAD 11 1", "", Country.FR, Country.BE, 100.133, 100.000, 25.102, 75.000, 0.000, 0.000, Map.of(Country.FR, -0.051, Country.BE, -0.051));
        }

        assertEquals(2, flowDecompositionResults.getZoneSet().size());
        assertTrue(flowDecompositionResults.getZoneSet().contains(Country.BE));
        assertTrue(flowDecompositionResults.getZoneSet().contains(Country.FR));
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
        generateTestString(flowDecompositionResults);
        assertEquals(2, flowDecompositionResults.getDecomposedFlowMap().size());

        if (Set.of(FlowDecompositionParameters.FlowPartitionMode.FULL_LINE_DECOMPOSITION, FlowDecompositionParameters.FlowPartitionMode.FAST_FULL_LINE_DECOMPOSITION).contains(flowPartitionMode)) {
            // TODO fix this test, xnode flow is not supported
            validateFlowDecompositionWithMap(flowDecompositionResults, "BGEN1 11 BLOAD 11 1", "BGEN1 11 BLOAD 11 1", "", Country.BE, Country.BE, -49.844, -49.984, 0.000, 49.984, 0.000, 0.000, Collections.emptyMap());
            validateFlowDecompositionWithMap(flowDecompositionResults, "FLOAD 11 BGEN1 11 1", "FLOAD 11 BGEN1 11 1", "", Country.FR, Country.BE, -100.000, -100.000, 50.016, 49.984, 0.000, 0.000, Collections.emptyMap());
        } else {
            validateFlowDecompositionWithMap(flowDecompositionResults, "BGEN1 11 BLOAD 11 1", "BGEN1 11 BLOAD 11 1", "", Country.BE, Country.BE, -49.844, -49.984, 0.000, 150.000, 0.000, -100.016, Collections.emptyMap());
            validateFlowDecompositionWithMap(flowDecompositionResults, "FLOAD 11 BGEN1 11 1", "FLOAD 11 BGEN1 11 1", "", Country.FR, Country.BE, -100.000, -100.000, 0.000, -0.000, 0.000, 0.000, Map.of(Country.FR, 100.000));
        }

        assertEquals(2, flowDecompositionResults.getZoneSet().size());
        assertTrue(flowDecompositionResults.getZoneSet().contains(Country.BE));
        assertTrue(flowDecompositionResults.getZoneSet().contains(Country.FR));
    }

    @ParameterizedTest(name = "Mode={0}")
    @EnumSource(value = FlowDecompositionParameters.FlowPartitionMode.class, names = {
        "MATRIX_BASED",
        "DIRECT_SENSITIVITY_BASED",
        "FULL_LINE_DECOMPOSITION",
        "FAST_FULL_LINE_DECOMPOSITION"
    })
    void testSimpleNetworkWithUnpairedXNodeGen2(FlowDecompositionParameters.FlowPartitionMode flowPartitionMode) {
        String networkFileName = "NETWORK_SINGLE_LOAD_TWO_GENERATORS_WITH_UNBOUNDED_XNODE_GEN_2.uct";

        Network network = TestUtils.importNetwork(networkFileName);

        FlowDecompositionResults flowDecompositionResults = runFlowDecomposition(network, new XnecProviderAllBranches(), flowPartitionMode);
        generateTestString(flowDecompositionResults);
        assertEquals(2, flowDecompositionResults.getDecomposedFlowMap().size());

        if (Set.of(FlowDecompositionParameters.FlowPartitionMode.FULL_LINE_DECOMPOSITION, FlowDecompositionParameters.FlowPartitionMode.FAST_FULL_LINE_DECOMPOSITION).contains(flowPartitionMode)) {
            // TODO fix this test, xnode flow is not supported
            validateFlowDecompositionWithMap(flowDecompositionResults, "BGEN1 11 BGEN2 11 1", "BGEN1 11 BGEN2 11 1", "", Country.BE, Country.BE, -86.928, -86.920, 86.920, 0.000, 0.000, 0.000, Collections.emptyMap());
            validateFlowDecompositionWithMap(flowDecompositionResults, "FLOAD 11 BGEN1 11 1", "FLOAD 11 BGEN1 11 1", "", Country.FR, Country.BE, -150.000, -150.000, 100.000, 50.000, 0.000, 0.000, Collections.emptyMap());
        } else {
            validateFlowDecompositionWithMap(flowDecompositionResults, "BGEN1 11 BGEN2 11 1", "BGEN1 11 BGEN2 11 1", "", Country.BE, Country.BE, -86.928, -86.920, 40.028, -25.000, 0.000, -3.108, Map.of(Country.FR, 75.000));
            validateFlowDecompositionWithMap(flowDecompositionResults, "FLOAD 11 BGEN1 11 1", "FLOAD 11 BGEN1 11 1", "", Country.FR, Country.BE, -150.000, -150.000, 0.000, -0.000, 0.000, 0.000, Map.of(Country.FR, 150.000));
        }

        assertEquals(2, flowDecompositionResults.getZoneSet().size());
        assertTrue(flowDecompositionResults.getZoneSet().contains(Country.BE));
        assertTrue(flowDecompositionResults.getZoneSet().contains(Country.FR));
    }

    @ParameterizedTest(name = "Mode={0}")
    @EnumSource(value = FlowDecompositionParameters.FlowPartitionMode.class, names = {
        "MATRIX_BASED",
        "DIRECT_SENSITIVITY_BASED",
        "FULL_LINE_DECOMPOSITION",
        "FAST_FULL_LINE_DECOMPOSITION"
    })
    void testSimpleNetworkWithUnpairedXNodeGen3(FlowDecompositionParameters.FlowPartitionMode flowPartitionMode) {
        String networkFileName = "NETWORK_SINGLE_LOAD_TWO_GENERATORS_WITH_UNBOUNDED_XNODE_GEN_3.uct";

        Network network = TestUtils.importNetwork(networkFileName);

        FlowDecompositionResults flowDecompositionResults = runFlowDecomposition(network, new XnecProviderAllBranches(), flowPartitionMode);
        generateTestString(flowDecompositionResults);
        assertEquals(2, flowDecompositionResults.getDecomposedFlowMap().size());

        if (Set.of(FlowDecompositionParameters.FlowPartitionMode.FULL_LINE_DECOMPOSITION, FlowDecompositionParameters.FlowPartitionMode.FAST_FULL_LINE_DECOMPOSITION).contains(flowPartitionMode)) {
            // TODO fix this test, xnode flow is not supported
            validateFlowDecompositionWithMap(flowDecompositionResults, "BGEN1 11 BLOAD 11 1", "BGEN1 11 BLOAD 11 1", "", Country.BE, Country.BE, -49.844, -49.984, 0.000, 49.984, 0.000, 0.000, Collections.emptyMap());
            validateFlowDecompositionWithMap(flowDecompositionResults, "FLOAD 11 BGEN1 11 1", "FLOAD 11 BGEN1 11 1", "", Country.FR, Country.BE, -100.000, -100.000, 50.016, 49.984, 0.000, 0.000, Collections.emptyMap());
        } else {
            validateFlowDecompositionWithMap(flowDecompositionResults, "BGEN1 11 BLOAD 11 1", "BGEN1 11 BLOAD 11 1", "", Country.BE, Country.BE, -49.844, -49.984, 0.000, 150.000, 0.000, -100.016, Collections.emptyMap());
            validateFlowDecompositionWithMap(flowDecompositionResults, "FLOAD 11 BGEN1 11 1", "FLOAD 11 BGEN1 11 1", "", Country.FR, Country.BE, -100.000, -100.000, 0.000, 0.000, 0.000, 0.000, Map.of(Country.FR, 100.000));
        }

        assertEquals(2, flowDecompositionResults.getZoneSet().size());
        assertTrue(flowDecompositionResults.getZoneSet().contains(Country.BE));
        assertTrue(flowDecompositionResults.getZoneSet().contains(Country.FR));
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
        assertEquals(2, flowDecompositionResults.getDecomposedFlowMap().size());

        if (Set.of(FlowDecompositionParameters.FlowPartitionMode.FULL_LINE_DECOMPOSITION, FlowDecompositionParameters.FlowPartitionMode.FAST_FULL_LINE_DECOMPOSITION).contains(flowPartitionMode)) {
            validateFlowDecompositionWithMap(flowDecompositionResults, "BLOAD 11 BGEN2 11 1", "BLOAD 11 BGEN2 11 1", "", Country.BE, Country.BE, -100.000, -100.000, 0.000, 0.000, 0.000, 100.000, Collections.emptyMap());
            validateFlowDecompositionWithMap(flowDecompositionResults, "FGEN1 11 X     11 1 + X     11 BLOAD 11 1", "FGEN1 11 X     11 1 + X     11 BLOAD 11 1", "", Country.FR, Country.BE, 100.063, 100.047, 100.047, 0.000, 0.000, 0.000, Collections.emptyMap());
        } else {
            validateFlowDecompositionWithMap(flowDecompositionResults, "BLOAD 11 BGEN2 11 1", "BLOAD 11 BGEN2 11 1", "", Country.BE, Country.BE, -100.000, -100.000, -100.047, 0.000, 0.000, 200.047, Collections.emptyMap());
            validateFlowDecompositionWithMap(flowDecompositionResults, "FGEN1 11 X     11 1 + X     11 BLOAD 11 1", "FGEN1 11 X     11 1 + X     11 BLOAD 11 1", "", Country.FR, Country.BE, 100.063, 100.047, 100.047, 0.000, 0.000, 0.000, Collections.emptyMap());
        }

        assertEquals(2, flowDecompositionResults.getZoneSet().size());
        assertTrue(flowDecompositionResults.getZoneSet().contains(Country.BE));
        assertTrue(flowDecompositionResults.getZoneSet().contains(Country.FR));
    }

    @ParameterizedTest(name = "Mode={0}")
    @EnumSource(value = FlowDecompositionParameters.FlowPartitionMode.class, names = {
        "MATRIX_BASED",
        "DIRECT_SENSITIVITY_BASED",
        "FULL_LINE_DECOMPOSITION",
        "FAST_FULL_LINE_DECOMPOSITION" // TODO fix this test, paired xnode are not supported
    })
    void testSimpleNetworkWithXNodeGen(FlowDecompositionParameters.FlowPartitionMode flowPartitionMode) {
        String networkFileName = "TestCaseDangling.xiidm";

        Network network = TestUtils.importNetwork(networkFileName);

        FlowDecompositionResults flowDecompositionResults = runFlowDecomposition(network, new XnecProviderAllBranches(), flowPartitionMode);
        assertEquals(16, flowDecompositionResults.getDecomposedFlowMap().size());

        if (Set.of(FlowDecompositionParameters.FlowPartitionMode.FULL_LINE_DECOMPOSITION, FlowDecompositionParameters.FlowPartitionMode.FAST_FULL_LINE_DECOMPOSITION).contains(flowPartitionMode)) {
            validateFlowDecompositionWithMap(flowDecompositionResults, "BBE1AA1  BBE2AA1  1", "BBE1AA1  BBE2AA1  1", "", Country.BE, Country.BE, -878.370, -878.333, 191.529, 107.446, 0.000, 550.311, Map.of(Country.FR, 31.596, Country.NL, -2.548));
            validateFlowDecompositionWithMap(flowDecompositionResults, "BBE1AA1  BBE3AA1  1", "BBE1AA1  BBE3AA1  1", "", Country.BE, Country.BE, -121.630, -121.667, -191.529, 13.955, 0.000, 328.288, Map.of(Country.FR, -31.596, Country.NL, 2.548));
            validateFlowDecompositionWithMap(flowDecompositionResults, "BBE2AA1  BBE3AA1  1", "BBE2AA1  BBE3AA1  1", "", Country.BE, Country.BE, -756.898, -756.667, 383.058, 93.491, 0.000, 222.024, Map.of(Country.FR, 63.191, Country.NL, -5.097));
            validateFlowDecompositionWithMap(flowDecompositionResults, "BBE2AA1  FFR3AA1  1", "BBE2AA1  FFR3AA1  1", "", Country.BE, Country.FR, 664.731, 665.000, 616.074, 99.063, 0.000, 0.000, Map.of(Country.BE, 37.004, Country.FR, -94.787, Country.NL, 7.645));
            validateFlowDecompositionWithMap(flowDecompositionResults, "DDE1AA1  DDE2AA1  1", "DDE1AA1  DDE2AA1  1", "", Country.DE, Country.DE, -478.511, -478.333, 449.781, 11.840, 0.000, 0.000, Map.of(Country.BE, -12.335, Country.FR, 31.596, Country.NL, -2.548));
            validateFlowDecompositionWithMap(flowDecompositionResults, "DDE1AA1  DDE3AA1  1", "DDE1AA1  DDE3AA1  1", "", Country.DE, Country.DE, -821.489, -821.667, 810.333, 28.046, 0.000, 0.000, Map.of(Country.BE, 12.335, Country.FR, -31.596, Country.NL, 2.548));
            validateFlowDecompositionWithMap(flowDecompositionResults, "DDE2AA1  DDE3AA1  1", "DDE2AA1  DDE3AA1  1", "", Country.DE, Country.DE, -343.242, -343.333, 360.553, 16.206, 0.000, 0.000, Map.of(Country.BE, 24.669, Country.FR, -63.191, Country.NL, 5.097));
            validateFlowDecompositionWithMap(flowDecompositionResults, "DDE2AA1  NNL3AA1  1", "DDE2AA1  NNL3AA1  1", "", Country.DE, Country.NL, -1135.269, -1135.000, 1052.919, 31.943, 0.000, 0.000, Map.of(Country.BE, -37.004, Country.FR, 94.787, Country.NL, -7.645));
            validateFlowDecompositionWithMap(flowDecompositionResults, "FFR1AA1  FFR2AA1  1", "FFR1AA1  FFR2AA1  1", "", Country.FR, Country.FR, 1388.870, 1388.333, 819.102, 33.021, 0.000, 521.327, Map.of(Country.BE, 12.335, Country.NL, 2.548));
            validateFlowDecompositionWithMap(flowDecompositionResults, "FFR1AA1  FFR3AA1  1", "FFR1AA1  FFR3AA1  1", "", Country.FR, Country.FR, -388.870, -388.333, 293.036, 33.021, 0.000, 47.393, Map.of(Country.BE, 12.335, Country.NL, 2.548));
            validateFlowDecompositionWithMap(flowDecompositionResults, "FFR2AA1  DDE3AA1  1", "FFR2AA1  DDE3AA1  1", "", Country.FR, Country.DE, 1664.731, 1665.000, 1657.183, 57.955, 0.000, 0.000, Map.of(Country.BE, 37.004, Country.FR, -94.787, Country.NL, 7.645));
            validateFlowDecompositionWithMap(flowDecompositionResults, "FFR2AA1  FFR3AA1  1", "FFR2AA1  FFR3AA1  1", "", Country.FR, Country.FR, -1775.862, -1776.667, 1112.138, 66.042, 0.000, 568.720, Map.of(Country.BE, 24.669, Country.NL, 5.097));
            validateFlowDecompositionWithMap(flowDecompositionResults, "NNL1AA1  NNL2AA1  1", "NNL1AA1  NNL2AA1  1", "", Country.NL, Country.NL, -211.847, -211.667, 228.775, 17.148, 0.000, -53.517, Map.of(Country.BE, -12.335, Country.FR, 31.596));
            validateFlowDecompositionWithMap(flowDecompositionResults, "NNL1AA1  NNL3AA1  1", "NNL1AA1  NNL3AA1  1", "", Country.NL, Country.NL, 711.847, 711.667, 575.869, 17.148, 0.000, 99.388, Map.of(Country.BE, -12.335, Country.FR, 31.596));
            validateFlowDecompositionWithMap(flowDecompositionResults, "NNL2AA1  BBE3AA1  1", "NNL2AA1  BBE3AA1  1", "", Country.NL, Country.BE, -1635.269, -1635.000, 1505.327, 79.536, 0.000, 0.000, Map.of(Country.BE, -37.004, Country.FR, 94.787, Country.NL, -7.645));
            validateFlowDecompositionWithMap(flowDecompositionResults, "NNL2AA1  NNL3AA1  1", "NNL2AA1  NNL3AA1  1", "", Country.NL, Country.NL, 923.422, 923.333, 804.644, 34.296, 0.000, 45.872, Map.of(Country.BE, -24.669, Country.FR, 63.191));
        } else {
            validateFlowDecompositionWithMap(flowDecompositionResults, "BBE1AA1  BBE2AA1  1", "BBE1AA1  BBE2AA1  1", "", Country.BE, Country.BE, -878.370, -878.333, -48.390, 57.500, 0.000, 814.035, Map.of(Country.DE, 4.444, Country.NL, 3.125, Country.FR, 47.619));
            validateFlowDecompositionWithMap(flowDecompositionResults, "BBE1AA1  BBE3AA1  1", "BBE1AA1  BBE3AA1  1", "", Country.BE, Country.BE, -121.630, -121.667, -336.259, -32.500, 0.000, 545.614, Map.of(Country.DE, -4.444, Country.NL, -3.125, Country.FR, -47.619));
            validateFlowDecompositionWithMap(flowDecompositionResults, "BBE2AA1  BBE3AA1  1", "BBE2AA1  BBE3AA1  1", "", Country.BE, Country.BE, -756.898, -756.667, 287.869, 90.000, 0.000, 268.421, Map.of(Country.DE, 8.889, Country.NL, 6.250, Country.FR, 95.238));
            validateFlowDecompositionWithMap(flowDecompositionResults, "BBE2AA1  FFR3AA1  1", "BBE2AA1  FFR3AA1  1", "", Country.BE, Country.FR, 664.731, 665.000, 460.960, 127.500, 0.000, 0.000, Map.of(Country.DE, -13.333, Country.BE, 242.105, Country.NL, -9.375, Country.FR, -142.857));
            validateFlowDecompositionWithMap(flowDecompositionResults, "DDE1AA1  DDE2AA1  1", "DDE1AA1  DDE2AA1  1", "", Country.DE, Country.DE, -478.511, -478.333, 386.405, 7.500, 0.000, 26.667, Map.of(Country.BE, 7.018, Country.NL, 3.125, Country.FR, 47.619));
            validateFlowDecompositionWithMap(flowDecompositionResults, "DDE1AA1  DDE3AA1  1", "DDE1AA1  DDE3AA1  1", "", Country.DE, Country.DE, -821.489, -821.667, 711.402, 17.500, 0.000, 106.667, Map.of(Country.BE, 36.842, Country.NL, -3.125, Country.FR, -47.619));
            validateFlowDecompositionWithMap(flowDecompositionResults, "DDE2AA1  DDE3AA1  1", "DDE2AA1  DDE3AA1  1", "", Country.DE, Country.DE, -343.242, -343.333, 324.997, 10.000, 0.000, 80.000, Map.of(Country.BE, 29.825, Country.NL, -6.250, Country.FR, -95.238));
            validateFlowDecompositionWithMap(flowDecompositionResults, "DDE2AA1  NNL3AA1  1", "DDE2AA1  NNL3AA1  1", "", Country.DE, Country.NL, -1135.269, -1135.000, 925.882, 22.500, 0.000, 0.000, Map.of(Country.DE, 13.333, Country.BE, 21.053, Country.NL, 9.375, Country.FR, 142.857));
            validateFlowDecompositionWithMap(flowDecompositionResults, "FFR1AA1  FFR2AA1  1", "FFR1AA1  FFR2AA1  1", "", Country.FR, Country.FR, 1388.870, 1388.333, 555.846, 17.500, 0.000, 785.714, Map.of(Country.DE, -4.444, Country.BE, 36.842, Country.NL, -3.125));
            validateFlowDecompositionWithMap(flowDecompositionResults, "FFR1AA1  FFR3AA1  1", "FFR1AA1  FFR3AA1  1", "", Country.FR, Country.FR, -388.870, -388.333, 201.272, 42.500, 0.000, 71.429, Map.of(Country.DE, -4.444, Country.BE, 80.702, Country.NL, -3.125));
            validateFlowDecompositionWithMap(flowDecompositionResults, "FFR2AA1  DDE3AA1  1", "FFR2AA1  DDE3AA1  1", "", Country.FR, Country.DE, 1664.731, 1665.000, 1667.539, 52.500, 0.000, 0.000, Map.of(Country.DE, -13.333, Country.BE, 110.526, Country.NL, -9.375, Country.FR, -142.857));
            validateFlowDecompositionWithMap(flowDecompositionResults, "FFR2AA1  FFR3AA1  1", "FFR2AA1  FFR3AA1  1", "", Country.FR, Country.FR, -1775.862, -1776.667, 757.119, 60.000, 0.000, 857.143, Map.of(Country.DE, -8.889, Country.BE, 117.544, Country.NL, -6.250));
            validateFlowDecompositionWithMap(flowDecompositionResults, "NNL1AA1  NNL2AA1  1", "NNL1AA1  NNL2AA1  1", "", Country.NL, Country.NL, -211.847, -211.667, 448.101, 32.500, 0.000, -371.875, Map.of(Country.DE, 4.444, Country.BE, 50.877, Country.FR, 47.619));
            validateFlowDecompositionWithMap(flowDecompositionResults, "NNL1AA1  NNL3AA1  1", "NNL1AA1  NNL3AA1  1", "", Country.NL, Country.NL, 711.847, 711.667, 329.461, 7.500, 0.000, 315.625, Map.of(Country.DE, 4.444, Country.BE, 7.018, Country.FR, 47.619));
            validateFlowDecompositionWithMap(flowDecompositionResults, "NNL2AA1  BBE3AA1  1", "NNL2AA1  BBE3AA1  1", "", Country.NL, Country.BE, -1635.269, -1635.000, 1219.303, 97.500, 0.000, 0.000, Map.of(Country.DE, 13.333, Country.BE, 152.632, Country.NL, 9.375, Country.FR, 142.857));
            validateFlowDecompositionWithMap(flowDecompositionResults, "NNL2AA1  NNL3AA1  1", "NNL2AA1  NNL3AA1  1", "", Country.NL, Country.NL, 923.422, 923.333, 777.562, 40.000, 0.000, -56.250, Map.of(Country.DE, 8.889, Country.BE, 57.895, Country.FR, 95.238));
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
        "FAST_FULL_LINE_DECOMPOSITION" // TODO fix this test, paired xnode are not supported
    })
    void testSimpleNetworkWithXNodeLoad(FlowDecompositionParameters.FlowPartitionMode flowPartitionMode) {
        String networkFileName = "TestCaseDangling.xiidm";

        Network network = TestUtils.importNetwork(networkFileName);
        network.getDanglingLine("BBE2AA1  X_BEFR1  1").setP0(300);
        network.getGenerator("BBE2AA1 _generator").setTargetP(3600);

        FlowDecompositionResults flowDecompositionResults = runFlowDecomposition(network, new XnecProviderAllBranches(), flowPartitionMode);
        assertEquals(16, flowDecompositionResults.getDecomposedFlowMap().size());

        if (Set.of(FlowDecompositionParameters.FlowPartitionMode.FULL_LINE_DECOMPOSITION, FlowDecompositionParameters.FlowPartitionMode.FAST_FULL_LINE_DECOMPOSITION).contains(flowPartitionMode)) {
            validateFlowDecompositionWithMap(flowDecompositionResults, "BBE1AA1  BBE2AA1  1", "BBE1AA1  BBE2AA1  1", "", Country.BE, Country.BE, -878.370, -878.333, 220.064, 0.000, 0.000, 629.222, Map.of(Country.FR, 31.596, Country.NL, -2.548));
            validateFlowDecompositionWithMap(flowDecompositionResults, "BBE1AA1  BBE3AA1  1", "BBE1AA1  BBE3AA1  1", "", Country.BE, Country.BE, -121.630, -121.667, -220.064, 0.000, 0.000, 370.778, Map.of(Country.FR, -31.596, Country.NL, 2.548));
            validateFlowDecompositionWithMap(flowDecompositionResults, "BBE2AA1  BBE3AA1  1", "BBE2AA1  BBE3AA1  1", "", Country.BE, Country.BE, -756.898, -756.667, 440.128, 0.000, 0.000, 258.444, Map.of(Country.FR, 63.191, Country.NL, -5.097));
            validateFlowDecompositionWithMap(flowDecompositionResults, "BBE2AA1  FFR3AA1  1", "BBE2AA1  FFR3AA1  1", "", Country.BE, Country.FR, 664.731, 665.000, 709.067, 0.000, 0.000, 0.000, Map.of(Country.BE, 43.074, Country.FR, -94.787, Country.NL, 7.645));
            validateFlowDecompositionWithMap(flowDecompositionResults, "DDE1AA1  DDE2AA1  1", "DDE1AA1  DDE2AA1  1", "", Country.DE, Country.DE, -478.511, -478.333, 463.644, 0.000, 0.000, 0.000, Map.of(Country.BE, -14.358, Country.FR, 31.596, Country.NL, -2.548));
            validateFlowDecompositionWithMap(flowDecompositionResults, "DDE1AA1  DDE3AA1  1", "DDE1AA1  DDE3AA1  1", "", Country.DE, Country.DE, -821.489, -821.667, 836.356, 0.000, 0.000, 0.000, Map.of(Country.BE, 14.358, Country.FR, -31.596, Country.NL, 2.548));
            validateFlowDecompositionWithMap(flowDecompositionResults, "DDE2AA1  DDE3AA1  1", "DDE2AA1  DDE3AA1  1", "", Country.DE, Country.DE, -343.242, -343.333, 372.712, 0.000, 0.000, 0.000, Map.of(Country.BE, 28.716, Country.FR, -63.191, Country.NL, 5.097));
            validateFlowDecompositionWithMap(flowDecompositionResults, "DDE2AA1  NNL3AA1  1", "DDE2AA1  NNL3AA1  1", "", Country.DE, Country.NL, -1135.269, -1135.000, 1090.933, 0.000, 0.000, 0.000, Map.of(Country.BE, -43.074, Country.FR, 94.787, Country.NL, -7.645));
            validateFlowDecompositionWithMap(flowDecompositionResults, "FFR1AA1  FFR2AA1  1", "FFR1AA1  FFR2AA1  1", "", Country.FR, Country.FR, 1388.870, 1388.333, 850.100, 0.000, 0.000, 521.327, Map.of(Country.BE, 14.358, Country.NL, 2.548));
            validateFlowDecompositionWithMap(flowDecompositionResults, "FFR1AA1  FFR3AA1  1", "FFR1AA1  FFR3AA1  1", "", Country.FR, Country.FR, -388.870, -388.333, 324.034, 0.000, 0.000, 47.393, Map.of(Country.BE, 14.358, Country.NL, 2.548));
            validateFlowDecompositionWithMap(flowDecompositionResults, "FFR2AA1  DDE3AA1  1", "FFR2AA1  DDE3AA1  1", "", Country.FR, Country.DE, 1664.731, 1665.000, 1709.067, 0.000, 0.000, 0.000, Map.of(Country.BE, 43.074, Country.FR, -94.787, Country.NL, 7.645));
            validateFlowDecompositionWithMap(flowDecompositionResults, "FFR2AA1  FFR3AA1  1", "FFR2AA1  FFR3AA1  1", "", Country.FR, Country.FR, -1775.862, -1776.667, 1174.133, 0.000, 0.000, 568.720, Map.of(Country.BE, 28.716, Country.NL, 5.097));
            validateFlowDecompositionWithMap(flowDecompositionResults, "NNL1AA1  NNL2AA1  1", "NNL1AA1  NNL2AA1  1", "", Country.NL, Country.NL, -211.847, -211.667, 247.946, 0.000, 0.000, -53.517, Map.of(Country.BE, -14.358, Country.FR, 31.596));
            validateFlowDecompositionWithMap(flowDecompositionResults, "NNL1AA1  NNL3AA1  1", "NNL1AA1  NNL3AA1  1", "", Country.NL, Country.NL, 711.847, 711.667, 595.041, 0.000, 0.000, 99.388, Map.of(Country.BE, -14.358, Country.FR, 31.596));
            validateFlowDecompositionWithMap(flowDecompositionResults, "NNL2AA1  BBE3AA1  1", "NNL2AA1  BBE3AA1  1", "", Country.NL, Country.BE, -1635.269, -1635.000, 1590.933, 0.000, 0.000, 0.000, Map.of(Country.BE, -43.074, Country.FR, 94.787, Country.NL, -7.645));
            validateFlowDecompositionWithMap(flowDecompositionResults, "NNL2AA1  NNL3AA1  1", "NNL2AA1  NNL3AA1  1", "", Country.NL, Country.NL, 923.422, 923.333, 842.987, 0.000, 0.000, 45.872, Map.of(Country.BE, -28.716, Country.FR, 63.191));
        } else {
            validateFlowDecompositionWithMap(flowDecompositionResults, "BBE1AA1  BBE2AA1  1", "BBE1AA1  BBE2AA1  1", "", Country.BE, Country.BE, -878.370, -878.333, -36.764, -57.500, 0.000, 917.409, Map.of(Country.FR, 47.619, Country.NL, 3.125, Country.DE, 4.444));
            validateFlowDecompositionWithMap(flowDecompositionResults, "BBE1AA1  BBE3AA1  1", "BBE1AA1  BBE3AA1  1", "", Country.BE, Country.BE, -121.630, -121.667, -378.005, 32.500, 0.000, 522.360, Map.of(Country.FR, -47.619, Country.NL, -3.125, Country.DE, -4.444));
            validateFlowDecompositionWithMap(flowDecompositionResults, "BBE2AA1  BBE3AA1  1", "BBE2AA1  BBE3AA1  1", "", Country.BE, Country.BE, -756.898, -756.667, 341.240, -90.000, 0.000, 395.050, Map.of(Country.FR, 95.238, Country.NL, 6.250, Country.DE, 8.889));
            validateFlowDecompositionWithMap(flowDecompositionResults, "BBE2AA1  FFR3AA1  1", "BBE2AA1  FFR3AA1  1", "", Country.BE, Country.FR, 664.731, 665.000, 650.887, -127.500, 0.000, 0.000, Map.of(Country.BE, 307.178, Country.FR, -142.857, Country.NL, -9.375, Country.DE, -13.333));
            validateFlowDecompositionWithMap(flowDecompositionResults, "DDE1AA1  DDE2AA1  1", "DDE1AA1  DDE2AA1  1", "", Country.DE, Country.DE, -478.511, -478.333, 403.555, -7.500, 0.000, 26.667, Map.of(Country.BE, 4.868, Country.FR, 47.619, Country.NL, 3.125));
            validateFlowDecompositionWithMap(flowDecompositionResults, "DDE1AA1  DDE3AA1  1", "DDE1AA1  DDE3AA1  1", "", Country.DE, Country.DE, -821.489, -821.667, 734.482, -17.500, 0.000, 106.667, Map.of(Country.BE, 48.762, Country.FR, -47.619, Country.NL, -3.125));
            validateFlowDecompositionWithMap(flowDecompositionResults, "DDE2AA1  DDE3AA1  1", "DDE2AA1  DDE3AA1  1", "", Country.DE, Country.DE, -343.242, -343.333, 330.927, -10.000, 0.000, 80.000, Map.of(Country.BE, 43.894, Country.FR, -95.238, Country.NL, -6.250));
            validateFlowDecompositionWithMap(flowDecompositionResults, "DDE2AA1  NNL3AA1  1", "DDE2AA1  NNL3AA1  1", "", Country.DE, Country.NL, -1135.269, -1135.000, 977.331, -22.500, 0.000, 0.000, Map.of(Country.BE, 14.604, Country.FR, 142.857, Country.NL, 9.375, Country.DE, 13.333));
            validateFlowDecompositionWithMap(flowDecompositionResults, "FFR1AA1  FFR2AA1  1", "FFR1AA1  FFR2AA1  1", "", Country.FR, Country.FR, 1388.870, 1388.333, 578.926, -17.500, 0.000, 785.714, Map.of(Country.BE, 48.762, Country.NL, -3.125, Country.DE, -4.444));
            validateFlowDecompositionWithMap(flowDecompositionResults, "FFR1AA1  FFR3AA1  1", "FFR1AA1  FFR3AA1  1", "", Country.FR, Country.FR, -388.870, -388.333, 264.581, -42.500, 0.000, 71.429, Map.of(Country.BE, 102.393, Country.NL, -3.125, Country.DE, -4.444));
            validateFlowDecompositionWithMap(flowDecompositionResults, "FFR2AA1  DDE3AA1  1", "FFR2AA1  DDE3AA1  1", "", Country.FR, Country.DE, 1664.731, 1665.000, 1736.778, -52.500, 0.000, 0.000, Map.of(Country.BE, 146.287, Country.FR, -142.857, Country.NL, -9.375, Country.DE, -13.333));
            validateFlowDecompositionWithMap(flowDecompositionResults, "FFR2AA1  FFR3AA1  1", "FFR2AA1  FFR3AA1  1", "", Country.FR, Country.FR, -1775.862, -1776.667, 843.508, -60.000, 0.000, 857.143, Map.of(Country.BE, 151.155, Country.NL, -6.250, Country.DE, -8.889));
            validateFlowDecompositionWithMap(flowDecompositionResults, "NNL1AA1  NNL2AA1  1", "NNL1AA1  NNL2AA1  1", "", Country.NL, Country.NL, -211.847, -211.667, 505.480, -32.500, 0.000, -371.875, Map.of(Country.BE, 58.498, Country.FR, 47.619, Country.DE, 4.444));
            validateFlowDecompositionWithMap(flowDecompositionResults, "NNL1AA1  NNL3AA1  1", "NNL1AA1  NNL3AA1  1", "", Country.NL, Country.NL, 711.847, 711.667, 346.610, -7.500, 0.000, 315.625, Map.of(Country.BE, 4.868, Country.FR, 47.619, Country.DE, 4.444));
            validateFlowDecompositionWithMap(flowDecompositionResults, "NNL2AA1  BBE3AA1  1", "NNL2AA1  BBE3AA1  1", "", Country.NL, Country.BE, -1635.269, -1635.000, 1391.439, -97.500, 0.000, 0.000, Map.of(Country.BE, 175.495, Country.FR, 142.857, Country.NL, 9.375, Country.DE, 13.333));
            validateFlowDecompositionWithMap(flowDecompositionResults, "NNL2AA1  NNL3AA1  1", "NNL2AA1  NNL3AA1  1", "", Country.NL, Country.NL, 923.422, 923.333, 852.090, -40.000, 0.000, -56.250, Map.of(Country.BE, 63.366, Country.FR, 95.238, Country.DE, 8.889));
        }

        assertEquals(4, flowDecompositionResults.getZoneSet().size());
        assertTrue(flowDecompositionResults.getZoneSet().contains(Country.BE));
        assertTrue(flowDecompositionResults.getZoneSet().contains(Country.DE));
        assertTrue(flowDecompositionResults.getZoneSet().contains(Country.FR));
        assertTrue(flowDecompositionResults.getZoneSet().contains(Country.NL));
    }
}
