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

import java.util.*;
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

    @SuppressWarnings("checkstyle:MethodLength")
    private static void validateFlowDecompositionResultsUsingPFCMethodology(FlowDecompositionResults flowDecompositionResults) {
        assertThat(flowDecompositionResults)
            .branchId("BBE1AA11 BBE3AA11 1")
            .contingencyId("contingency_split_network")
            .country1(Country.BE)
            .country2(Country.BE)
            .acReferenceFlow(-571.575251)
            .dcReferenceFlow(-571.428571)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("FFR2AA11 FFR3AA11 2")
            .country1(Country.FR)
            .country2(Country.FR)
            .acReferenceFlow(-1048.852694)
            .dcReferenceFlow(-1044.697128)
            .allocatedFlow(418.391705)
            .pstFlow(38.815350)
            .internalFlow(611.949424)
            .loopFlow(Country.BE, -16.125814)
            .loopFlow(Country.DE, -4.610042)
            .loopFlow(Country.NL, -3.723495)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("FFR2AA11 FFR3AA11 1")
            .country1(Country.FR)
            .country2(Country.FR)
            .acReferenceFlow(-1048.852694)
            .dcReferenceFlow(-1044.697128)
            .allocatedFlow(418.391705)
            .pstFlow(38.815350)
            .internalFlow(611.949424)
            .loopFlow(Country.BE, -16.125814)
            .loopFlow(Country.DE, -4.610042)
            .loopFlow(Country.NL, -3.723495)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("FFR3AA11 FFR5AA11 1")
            .country1(Country.FR)
            .country2(Country.FR)
            .acReferenceFlow(-1012.008034)
            .dcReferenceFlow(-1009.730695)
            .allocatedFlow(1246.058874)
            .pstFlow(18.657301)
            .internalFlow(-134.559417)
            .loopFlow(Country.BE, -46.431224)
            .loopFlow(Country.DE, -63.273741)
            .loopFlow(Country.NL, -10.721099)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("DDE2AA11 DDE3AA11 1")
            .contingencyId("contingency_1")
            .country1(Country.DE)
            .country2(Country.DE)
            .acReferenceFlow(-553.509702)
            .dcReferenceFlow(-561.186194)
            .allocatedFlow(443.113772)
            .pstFlow(-70.550332)
            .internalFlow(125.748503)
            .loopFlow(Country.FR, 62.874251)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("FFR4AA11 DDE4AA11 1")
            .contingencyId("contingency_1")
            .country1(Country.FR)
            .country2(Country.DE)
            .acReferenceFlow(148.880291)
            .dcReferenceFlow(146.271269)
            .allocatedFlow(518.962076)
            .pstFlow(23.516777)
            .loopFlow(Country.DE, -375.249501)
            .loopFlow(Country.FR, -20.958084)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("FFR1AA11 FFR4AA11 1")
            .contingencyId("contingency_split_network")
            .country1(Country.FR)
            .country2(Country.FR)
            .acReferenceFlow(871.907722)
            .dcReferenceFlow(889.220358)
            .allocatedFlow(431.137725)
            .pstFlow(-161.677845)
            .internalFlow(706.586826)
            .loopFlow(Country.DE, -86.826347)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("FFR2AA11 FFR3AA11 2")
            .contingencyId("contingency_split_network")
            .country1(Country.FR)
            .country2(Country.FR)
            .acReferenceFlow(-625.595532)
            .dcReferenceFlow(-622.155928)
            .allocatedFlow(-86.227545)
            .pstFlow(32.335569)
            .internalFlow(658.682635)
            .loopFlow(Country.DE, 17.365269)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("DDE1AA11 DDE4AA11 1")
            .contingencyId("contingency_1")
            .country1(Country.DE)
            .country2(Country.DE)
            .acReferenceFlow(-648.880291)
            .dcReferenceFlow(-646.271269)
            .allocatedFlow(118.962076)
            .pstFlow(23.516777)
            .internalFlow(524.750499)
            .loopFlow(Country.FR, -20.958084)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("NNL1AA11 NNL2AA11 1")
            .contingencyId("contingency_1")
            .country1(Country.NL)
            .country2(Country.NL)
            .acReferenceFlow(166.672696)
            .dcReferenceFlow(166.666667)
            .allocatedFlow(-208.333333)
            .internalFlow(375.000000)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("NNL2AA11 BBE3AA11 1")
            .contingencyId("contingency_1")
            .country1(Country.NL)
            .country2(Country.BE)
            .acReferenceFlow(-500.000000)
            .dcReferenceFlow(-500.000000)
            .allocatedFlow(500.000000)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("NNL1AA11 NNL2AA11 1")
            .contingencyId("contingency_desync")
            .country1(Country.NL)
            .country2(Country.NL)
            .acReferenceFlow(166.672696)
            .dcReferenceFlow(166.666667)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("FFR1AA11 FFR4AA11 1")
            .country1(Country.FR)
            .country2(Country.FR)
            .acReferenceFlow(779.213147)
            .dcReferenceFlow(795.975751)
            .allocatedFlow(400.159224)
            .pstFlow(-156.762149)
            .internalFlow(671.134046)
            .loopFlow(Country.BE, -12.233376)
            .loopFlow(Country.DE, -103.497273)
            .loopFlow(Country.NL, -2.824721)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("FFR3AA11 FFR5AA11 1")
            .contingencyId("contingency_1")
            .country1(Country.FR)
            .country2(Country.FR)
            .acReferenceFlow(-2000.000000)
            .dcReferenceFlow(-2000.000000)
            .allocatedFlow(2000.000000)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("NNL1AA11 NNL2AA11 1")
            .country1(Country.NL)
            .country2(Country.NL)
            .acReferenceFlow(-162.721962)
            .dcReferenceFlow(-163.423102)
            .allocatedFlow(459.647042)
            .pstFlow(-6.219100)
            .internalFlow(-371.426300)
            .loopFlow(Country.BE, 15.477075)
            .loopFlow(Country.DE, 21.091247)
            .loopFlow(Country.FR, 44.853139)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("FFR1AA11 FFR5AA11 1")
            .contingencyId("contingency_desync")
            .country1(Country.FR)
            .country2(Country.FR)
            .acReferenceFlow(NaN)
            .dcReferenceFlow(NaN)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("DDE1AA11 DDE4AA11 1")
            .contingencyId("contingency_split_network")
            .country1(Country.DE)
            .country2(Country.DE)
            .acReferenceFlow(-636.886698)
            .dcReferenceFlow(-634.295221)
            .allocatedFlow(106.986028)
            .pstFlow(23.516777)
            .internalFlow(524.750499)
            .loopFlow(Country.FR, -20.958084)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("FFR4AA11 DDE1AA11 1")
            .country1(Country.FR)
            .country2(Country.DE)
            .acReferenceFlow(478.144120)
            .dcReferenceFlow(472.335696)
            .allocatedFlow(394.134806)
            .pstFlow(53.066455)
            .loopFlow(Country.BE, -15.013689)
            .loopFlow(Country.DE, 129.041225)
            .loopFlow(Country.FR, -85.426398)
            .loopFlow(Country.NL, -3.466703)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("NNL2AA11 BBE3AA11 1")
            .country1(Country.NL)
            .country2(Country.BE)
            .acReferenceFlow(-1487.991966)
            .dcReferenceFlow(-1490.269305)
            .allocatedFlow(1253.941126)
            .pstFlow(-18.657301)
            .loopFlow(Country.BE, 46.431224)
            .loopFlow(Country.DE, 63.273741)
            .loopFlow(Country.FR, 134.559417)
            .loopFlow(Country.NL, 10.721099)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("BBE2AA11 BBE3AA11 1")
            .country1(Country.BE)
            .country2(Country.BE)
            .acReferenceFlow(926.977948)
            .dcReferenceFlow(927.181329)
            .allocatedFlow(423.495349)
            .pstFlow(-2.665329)
            .internalFlow(476.557844)
            .loopFlow(Country.DE, 9.039106)
            .loopFlow(Country.FR, 19.222774)
            .loopFlow(Country.NL, 1.531586)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("DDE1AA11 DDE2AA11 1")
            .contingencyId("contingency_desync")
            .country1(Country.DE)
            .country2(Country.DE)
            .acReferenceFlow(410.528297)
            .dcReferenceFlow(402.885662)
            .allocatedFlow(54.291417)
            .pstFlow(70.550332)
            .internalFlow(340.918164)
            .loopFlow(Country.FR, -62.874251)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("NNL1AA11 NNL3AA11 1")
            .contingencyId("contingency_1")
            .country1(Country.NL)
            .country2(Country.NL)
            .acReferenceFlow(333.327304)
            .dcReferenceFlow(333.333333)
            .allocatedFlow(20.833333)
            .internalFlow(312.500000)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("FFR4AA11 DDE4AA11 1")
            .country1(Country.FR)
            .country2(Country.DE)
            .acReferenceFlow(-10.930437)
            .dcReferenceFlow(-13.832152)
            .allocatedFlow(-397.067403)
            .pstFlow(-26.533227)
            .loopFlow(Country.BE, 7.506845)
            .loopFlow(Country.DE, 385.479387)
            .loopFlow(Country.FR, 42.713199)
            .loopFlow(Country.NL, 1.733351)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("FFR1AA11 FFR5AA11 1")
            .contingencyId("contingency_split_network")
            .country1(Country.FR)
            .country2(Country.FR)
            .acReferenceFlow(NaN)
            .dcReferenceFlow(NaN)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("NNL2AA11 NNL3AA11 1")
            .contingencyId("contingency_desync")
            .country1(Country.NL)
            .country2(Country.NL)
            .acReferenceFlow(166.672696)
            .dcReferenceFlow(166.666667)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("BBE1AA11 BBE2AA11 1")
            .country1(Country.BE)
            .country2(Country.BE)
            .acReferenceFlow(-1073.022052)
            .dcReferenceFlow(-1072.818671)
            .allocatedFlow(365.978335)
            .pstFlow(2.665329)
            .internalFlow(733.968472)
            .loopFlow(Country.DE, -9.039106)
            .loopFlow(Country.FR, -19.222774)
            .loopFlow(Country.NL, -1.531586)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("FFR4AA11 DDE4AA11 1")
            .contingencyId("contingency_split_network")
            .country1(Country.FR)
            .country2(Country.DE)
            .acReferenceFlow(136.886698)
            .dcReferenceFlow(134.295221)
            .allocatedFlow(506.986028)
            .pstFlow(23.516777)
            .loopFlow(Country.DE, -375.249501)
            .loopFlow(Country.FR, -20.958084)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("NNL2AA11 BBE3AA11 1")
            .contingencyId("contingency_desync")
            .country1(Country.NL)
            .country2(Country.BE)
            .acReferenceFlow(-500.000000)
            .dcReferenceFlow(-500.000000)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("FFR2AA11 DDE3AA11 1")
            .country1(Country.FR)
            .country2(Country.DE)
            .acReferenceFlow(544.794351)
            .dcReferenceFlow(551.227151)
            .allocatedFlow(454.856666)
            .pstFlow(-60.942381)
            .loopFlow(Country.BE, -23.910690)
            .loopFlow(Country.DE, 193.164421)
            .loopFlow(Country.FR, -6.419820)
            .loopFlow(Country.NL, -5.521045)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("BBE1AA11 BBE2AA11 1")
            .contingencyId("contingency_split_network")
            .country1(Country.BE)
            .country2(Country.BE)
            .acReferenceFlow(-1285.274247)
            .dcReferenceFlow(-1285.714286)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("DDE1AA11 DDE2AA11 1")
            .contingencyId("contingency_1")
            .country1(Country.DE)
            .country2(Country.DE)
            .acReferenceFlow(446.490298)
            .dcReferenceFlow(438.813806)
            .allocatedFlow(90.219561)
            .pstFlow(70.550332)
            .internalFlow(340.918164)
            .loopFlow(Country.FR, -62.874251)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("FFR2AA11 FFR4AA11 1")
            .contingencyId("contingency_split_network")
            .country1(Country.FR)
            .country2(Country.FR)
            .acReferenceFlow(1038.620574)
            .dcReferenceFlow(1013.665303)
            .allocatedFlow(689.820359)
            .pstFlow(232.228178)
            .internalFlow(230.538922)
            .loopFlow(Country.DE, -138.922156)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("FFR2AA11 DDE3AA11 1")
            .contingencyId("contingency_desync")
            .country1(Country.FR)
            .country2(Country.DE)
            .acReferenceFlow(1089.471702)
            .dcReferenceFlow(1097.114338)
            .allocatedFlow(879.041916)
            .pstFlow(-70.550332)
            .loopFlow(Country.DE, 225.748503)
            .loopFlow(Country.FR, 62.874251)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("FFR1AA11 FFR3AA11 1")
            .country1(Country.FR)
            .country2(Country.FR)
            .acReferenceFlow(-414.302646)
            .dcReferenceFlow(-420.336439)
            .allocatedFlow(409.275465)
            .pstFlow(-58.973399)
            .internalFlow(141.541735)
            .loopFlow(Country.BE, -14.179595)
            .loopFlow(Country.DE, -54.053657)
            .loopFlow(Country.NL, -3.274108)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("FFR2AA11 DDE3AA11 1")
            .contingencyId("contingency_1")
            .country1(Country.FR)
            .country2(Country.DE)
            .acReferenceFlow(1053.509702)
            .dcReferenceFlow(1061.186194)
            .allocatedFlow(843.113772)
            .pstFlow(-70.550332)
            .loopFlow(Country.DE, 225.748503)
            .loopFlow(Country.FR, 62.874251)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("DDE1AA11 DDE4AA11 1")
            .country1(Country.DE)
            .country2(Country.DE)
            .acReferenceFlow(-489.069563)
            .dcReferenceFlow(-486.167848)
            .allocatedFlow(-2.932597)
            .pstFlow(26.533227)
            .internalFlow(514.520613)
            .loopFlow(Country.BE, -7.506845)
            .loopFlow(Country.FR, -42.713199)
            .loopFlow(Country.NL, -1.733351)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("NNL1AA11 NNL3AA11 1")
            .contingencyId("contingency_desync")
            .country1(Country.NL)
            .country2(Country.NL)
            .acReferenceFlow(333.327304)
            .dcReferenceFlow(333.333333)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("DDE2AA11 DDE3AA11 1")
            .contingencyId("contingency_desync")
            .country1(Country.DE)
            .country2(Country.DE)
            .acReferenceFlow(-589.471702)
            .dcReferenceFlow(-597.114338)
            .allocatedFlow(479.041916)
            .pstFlow(-70.550332)
            .internalFlow(125.748503)
            .loopFlow(Country.FR, 62.874251)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("BBE2AA11 BBE3AA11 1")
            .contingencyId("contingency_split_network")
            .country1(Country.BE)
            .country2(Country.BE)
            .acReferenceFlow(714.725753)
            .dcReferenceFlow(714.285714)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("NNL1AA11 NNL3AA11 1")
            .contingencyId("contingency_split_network")
            .country1(Country.NL)
            .country2(Country.NL)
            .acReferenceFlow(NaN)
            .dcReferenceFlow(NaN)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("BBE1AA11 BBE3AA11 2")
            .contingencyId("contingency_desync")
            .country1(Country.BE)
            .country2(Country.BE)
            .acReferenceFlow(-428.685692)
            .dcReferenceFlow(-428.571429)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("BBE1AA11 BBE2AA11 1")
            .contingencyId("contingency_desync")
            .country1(Country.BE)
            .country2(Country.BE)
            .acReferenceFlow(-1213.942924)
            .dcReferenceFlow(-1214.285714)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("FFR2AA11 FFR3AA11 2")
            .contingencyId("contingency_desync")
            .country1(Country.FR)
            .country2(Country.FR)
            .acReferenceFlow(-625.595532)
            .dcReferenceFlow(-622.155928)
            .allocatedFlow(-86.227545)
            .pstFlow(32.335569)
            .internalFlow(658.682635)
            .loopFlow(Country.DE, 17.365269)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("BBE1AA11 FFR5AA11 1")
            .contingencyId("contingency_desync")
            .country1(Country.BE)
            .country2(Country.FR)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("BBE3AA11 BBE4AA11 1")
            .contingencyId("contingency_1")
            .country1(Country.BE)
            .country2(Country.BE)
            .acReferenceFlow(428.685692)
            .dcReferenceFlow(428.571429)
            .allocatedFlow(157.894737)
            .internalFlow(270.676692)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("BBE1AA11 BBE3AA11 1")
            .contingencyId("contingency_desync")
            .country1(Country.BE)
            .country2(Country.BE)
            .acReferenceFlow(-428.685692)
            .dcReferenceFlow(-428.571429)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("FFR2AA11 FFR4AA11 1")
            .contingencyId("contingency_desync")
            .country1(Country.FR)
            .country2(Country.FR)
            .acReferenceFlow(1038.620574)
            .dcReferenceFlow(1013.665303)
            .allocatedFlow(689.820359)
            .pstFlow(232.228178)
            .internalFlow(230.538922)
            .loopFlow(Country.DE, -138.922156)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("DDE1AA11 DDE2AA11 1")
            .contingencyId("contingency_split_network")
            .country1(Country.DE)
            .country2(Country.DE)
            .acReferenceFlow(410.528297)
            .dcReferenceFlow(402.885662)
            .allocatedFlow(54.291417)
            .pstFlow(70.550332)
            .internalFlow(340.918164)
            .loopFlow(Country.FR, -62.874251)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("FFR2AA11 FFR3AA11 1")
            .contingencyId("contingency_desync")
            .country1(Country.FR)
            .country2(Country.FR)
            .acReferenceFlow(-625.595532)
            .dcReferenceFlow(-622.155928)
            .allocatedFlow(-86.227545)
            .pstFlow(32.335569)
            .internalFlow(658.682635)
            .loopFlow(Country.DE, 17.365269)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("FFR2AA11 FFR3AA11 1")
            .contingencyId("contingency_split_network")
            .country1(Country.FR)
            .country2(Country.FR)
            .acReferenceFlow(-625.595532)
            .dcReferenceFlow(-622.155928)
            .allocatedFlow(-86.227545)
            .pstFlow(32.335569)
            .internalFlow(658.682635)
            .loopFlow(Country.DE, 17.365269)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("BBE3AA11 BBE4AA11 1")
            .country1(Country.BE)
            .country2(Country.BE)
            .acReferenceFlow(146.328661)
            .dcReferenceFlow(145.637341)
            .allocatedFlow(-57.517013)
            .pstFlow(5.330657)
            .internalFlow(257.410628)
            .loopFlow(Country.DE, -18.078212)
            .loopFlow(Country.FR, -38.445548)
            .loopFlow(Country.NL, -3.063171)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("FFR1AA11 FFR2AA11 1")
            .contingencyId("contingency_1")
            .country1(Country.FR)
            .country2(Country.FR)
            .acReferenceFlow(676.752713)
            .dcReferenceFlow(665.868983)
            .allocatedFlow(40.718563)
            .pstFlow(97.006707)
            .internalFlow(476.047904)
            .loopFlow(Country.DE, 52.095808)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("BBE2AA11 BBE3AA11 1")
            .contingencyId("contingency_1")
            .country1(Country.BE)
            .country2(Country.BE)
            .acReferenceFlow(786.057076)
            .dcReferenceFlow(785.714286)
            .allocatedFlow(315.789474)
            .internalFlow(469.924812)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("FFR1AA11 FFR3AA11 1")
            .contingencyId("contingency_desync")
            .country1(Country.FR)
            .country2(Country.FR)
            .acReferenceFlow(-248.808935)
            .dcReferenceFlow(-255.688143)
            .allocatedFlow(172.455090)
            .pstFlow(-64.671138)
            .internalFlow(182.634731)
            .loopFlow(Country.DE, -34.730539)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("BBE1AA11 BBE3AA11 2")
            .contingencyId("contingency_split_network")
            .country1(Country.BE)
            .country2(Country.BE)
            .acReferenceFlow(-571.575251)
            .dcReferenceFlow(-571.428571)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("DDE2AA11 DDE3AA11 1")
            .contingencyId("contingency_split_network")
            .country1(Country.DE)
            .country2(Country.DE)
            .acReferenceFlow(-589.471702)
            .dcReferenceFlow(-597.114338)
            .allocatedFlow(479.041916)
            .pstFlow(-70.550332)
            .internalFlow(125.748503)
            .loopFlow(Country.FR, 62.874251)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("FFR2AA11 DDE3AA11 1")
            .contingencyId("contingency_split_network")
            .country1(Country.FR)
            .country2(Country.DE)
            .acReferenceFlow(1089.471702)
            .dcReferenceFlow(1097.114338)
            .allocatedFlow(879.041916)
            .pstFlow(-70.550332)
            .loopFlow(Country.DE, 225.748503)
            .loopFlow(Country.FR, 62.874251)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("BBE1AA11 BBE3AA11 2")
            .country1(Country.BE)
            .country2(Country.BE)
            .acReferenceFlow(-146.328661)
            .dcReferenceFlow(-145.637341)
            .allocatedFlow(-57.517013)
            .pstFlow(5.330657)
            .internalFlow(257.410628)
            .loopFlow(Country.DE, -18.078212)
            .loopFlow(Country.FR, -38.445548)
            .loopFlow(Country.NL, -3.063171)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("BBE1AA11 FFR5AA11 1")
            .country1(Country.BE)
            .country2(Country.FR)
            .acReferenceFlow(506.004017)
            .dcReferenceFlow(504.865347)
            .allocatedFlow(623.029437)
            .pstFlow(9.328651)
            .loopFlow(Country.BE, -23.215612)
            .loopFlow(Country.DE, -31.636871)
            .loopFlow(Country.FR, -67.279708)
            .loopFlow(Country.NL, -5.360549)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("BBE1AA11 BBE3AA11 1")
            .country1(Country.BE)
            .country2(Country.BE)
            .acReferenceFlow(-146.328661)
            .dcReferenceFlow(-145.637341)
            .allocatedFlow(-57.517013)
            .pstFlow(5.330657)
            .internalFlow(257.410628)
            .loopFlow(Country.DE, -18.078212)
            .loopFlow(Country.FR, -38.445548)
            .loopFlow(Country.NL, -3.063171)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("NNL2AA11 NNL3AA11 1")
            .contingencyId("contingency_split_network")
            .country1(Country.NL)
            .country2(Country.NL)
            .acReferenceFlow(NaN)
            .dcReferenceFlow(NaN)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("BBE4AA11 FFR5AA11 1")
            .contingencyId("contingency_split_network")
            .country1(Country.BE)
            .country2(Country.FR)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("BBE2AA11 BBE3AA11 1")
            .contingencyId("contingency_desync")
            .country1(Country.BE)
            .country2(Country.BE)
            .acReferenceFlow(786.057076)
            .dcReferenceFlow(785.714286)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("BBE1AA11 BBE3AA11 1")
            .contingencyId("contingency_1")
            .country1(Country.BE)
            .country2(Country.BE)
            .acReferenceFlow(-428.685692)
            .dcReferenceFlow(-428.571429)
            .allocatedFlow(157.894737)
            .internalFlow(270.676692)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("BBE1AA11 FFR5AA11 1")
            .contingencyId("contingency_split_network")
            .country1(Country.BE)
            .country2(Country.FR)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("FFR4AA11 DDE1AA11 1")
            .contingencyId("contingency_desync")
            .country1(Country.FR)
            .country2(Country.DE)
            .acReferenceFlow(773.641598)
            .dcReferenceFlow(768.590441)
            .allocatedFlow(613.972056)
            .pstFlow(47.033555)
            .loopFlow(Country.DE, 149.500998)
            .loopFlow(Country.FR, -41.916168)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("BBE1AA11 BBE2AA11 1")
            .contingencyId("contingency_1")
            .country1(Country.BE)
            .country2(Country.BE)
            .acReferenceFlow(-1213.942924)
            .dcReferenceFlow(-1214.285714)
            .allocatedFlow(473.684211)
            .internalFlow(740.601504)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("FFR1AA11 FFR3AA11 1")
            .contingencyId("contingency_1")
            .country1(Country.FR)
            .country2(Country.FR)
            .acReferenceFlow(-716.378043)
            .dcReferenceFlow(-722.754012)
            .allocatedFlow(639.520958)
            .pstFlow(-64.671138)
            .internalFlow(182.634731)
            .loopFlow(Country.DE, -34.730539)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("DDE1AA11 DDE2AA11 1")
            .country1(Country.DE)
            .country2(Country.DE)
            .acReferenceFlow(-32.786317)
            .dcReferenceFlow(-41.496456)
            .allocatedFlow(275.464458)
            .pstFlow(-79.599682)
            .internalFlow(-310.228505)
            .loopFlow(Country.BE, 22.520534)
            .loopFlow(Country.FR, 128.139597)
            .loopFlow(Country.NL, 5.200054)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("FFR1AA11 FFR2AA11 1")
            .country1(Country.FR)
            .country2(Country.FR)
            .acReferenceFlow(635.089500)
            .dcReferenceFlow(624.360688)
            .allocatedFlow(9.116240)
            .pstFlow(97.788750)
            .internalFlow(470.407689)
            .loopFlow(Country.BE, -1.946219)
            .loopFlow(Country.DE, 49.443616)
            .loopFlow(Country.NL, -0.449387)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("FFR4AA11 DDE1AA11 1")
            .contingencyId("contingency_split_network")
            .country1(Country.FR)
            .country2(Country.DE)
            .acReferenceFlow(773.641598)
            .dcReferenceFlow(768.590441)
            .allocatedFlow(613.972056)
            .pstFlow(47.033555)
            .loopFlow(Country.DE, 149.500998)
            .loopFlow(Country.FR, -41.916168)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("NNL2AA11 NNL3AA11 1")
            .country1(Country.NL)
            .country2(Country.NL)
            .acReferenceFlow(825.270004)
            .dcReferenceFlow(826.846203)
            .allocatedFlow(731.794084)
            .pstFlow(-12.438201)
            .internalFlow(-55.352601)
            .loopFlow(Country.BE, 30.954149)
            .loopFlow(Country.DE, 42.182494)
            .loopFlow(Country.FR, 89.706278)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("BBE4AA11 FFR5AA11 1")
            .contingencyId("contingency_desync")
            .country1(Country.BE)
            .country2(Country.FR)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("DDE1AA11 DDE4AA11 1")
            .contingencyId("contingency_desync")
            .country1(Country.DE)
            .country2(Country.DE)
            .acReferenceFlow(-636.886698)
            .dcReferenceFlow(-634.295221)
            .allocatedFlow(106.986028)
            .pstFlow(23.516777)
            .internalFlow(524.750499)
            .loopFlow(Country.FR, -20.958084)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("NNL1AA11 NNL2AA11 1")
            .contingencyId("contingency_split_network")
            .country1(Country.NL)
            .country2(Country.NL)
            .acReferenceFlow(NaN)
            .dcReferenceFlow(NaN)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("FFR4AA11 DDE4AA11 1")
            .contingencyId("contingency_desync")
            .country1(Country.FR)
            .country2(Country.DE)
            .acReferenceFlow(136.886698)
            .dcReferenceFlow(134.295221)
            .allocatedFlow(506.986028)
            .pstFlow(23.516777)
            .loopFlow(Country.DE, -375.249501)
            .loopFlow(Country.FR, -20.958084)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("FFR1AA11 FFR5AA11 1")
            .country1(Country.FR)
            .country2(Country.FR)
            .acReferenceFlow(NaN)
            .dcReferenceFlow(NaN)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("BBE3AA11 BBE4AA11 1")
            .contingencyId("contingency_desync")
            .country1(Country.BE)
            .country2(Country.BE)
            .acReferenceFlow(428.685692)
            .dcReferenceFlow(428.571429)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("BBE1AA11 FFR5AA11 1")
            .contingencyId("contingency_1")
            .country1(Country.BE)
            .country2(Country.FR)
            .acReferenceFlow(1000.000000)
            .dcReferenceFlow(1000.000000)
            .allocatedFlow(1000.000000)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("NNL1AA11 NNL3AA11 1")
            .country1(Country.NL)
            .country2(Country.NL)
            .acReferenceFlow(662.721962)
            .dcReferenceFlow(663.423102)
            .allocatedFlow(272.147042)
            .pstFlow(-6.219100)
            .internalFlow(316.073700)
            .loopFlow(Country.BE, 15.477075)
            .loopFlow(Country.DE, 21.091247)
            .loopFlow(Country.FR, 44.853139)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("FFR4AA11 DDE1AA11 1")
            .contingencyId("contingency_1")
            .country1(Country.FR)
            .country2(Country.DE)
            .acReferenceFlow(797.610007)
            .dcReferenceFlow(792.542537)
            .allocatedFlow(637.924152)
            .pstFlow(47.033555)
            .loopFlow(Country.DE, 149.500998)
            .loopFlow(Country.FR, -41.916168)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("FFR1AA11 FFR2AA11 1")
            .contingencyId("contingency_desync")
            .country1(Country.FR)
            .country2(Country.FR)
            .acReferenceFlow(376.901212)
            .dcReferenceFlow(366.467785)
            .allocatedFlow(-258.682635)
            .pstFlow(97.006707)
            .internalFlow(476.047904)
            .loopFlow(Country.DE, 52.095808)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("FFR1AA11 FFR4AA11 1")
            .contingencyId("contingency_desync")
            .country1(Country.FR)
            .country2(Country.FR)
            .acReferenceFlow(871.907722)
            .dcReferenceFlow(889.220358)
            .allocatedFlow(431.137725)
            .pstFlow(-161.677845)
            .internalFlow(706.586826)
            .loopFlow(Country.DE, -86.826347)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("FFR2AA11 FFR4AA11 1")
            .contingencyId("contingency_1")
            .country1(Country.FR)
            .country2(Country.FR)
            .acReferenceFlow(906.864968)
            .dcReferenceFlow(881.928776)
            .allocatedFlow(558.083832)
            .pstFlow(232.228178)
            .internalFlow(230.538922)
            .loopFlow(Country.DE, -138.922156)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("FFR2AA11 FFR3AA11 2")
            .contingencyId("contingency_1")
            .country1(Country.FR)
            .country2(Country.FR)
            .acReferenceFlow(-1391.810978)
            .dcReferenceFlow(-1388.622994)
            .allocatedFlow(680.239521)
            .pstFlow(32.335569)
            .internalFlow(658.682635)
            .loopFlow(Country.DE, 17.365269)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("NNL2AA11 NNL3AA11 1")
            .contingencyId("contingency_1")
            .country1(Country.NL)
            .country2(Country.NL)
            .acReferenceFlow(166.672696)
            .dcReferenceFlow(166.666667)
            .allocatedFlow(229.166667)
            .internalFlow(-62.500000)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("FFR1AA11 FFR2AA11 1")
            .contingencyId("contingency_split_network")
            .country1(Country.FR)
            .country2(Country.FR)
            .acReferenceFlow(376.901212)
            .dcReferenceFlow(366.467785)
            .allocatedFlow(-258.682635)
            .pstFlow(97.006707)
            .internalFlow(476.047904)
            .loopFlow(Country.DE, 52.095808)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("FFR2AA11 FFR3AA11 1")
            .contingencyId("contingency_1")
            .country1(Country.FR)
            .country2(Country.FR)
            .acReferenceFlow(-1391.810978)
            .dcReferenceFlow(-1388.622994)
            .allocatedFlow(680.239521)
            .pstFlow(32.335569)
            .internalFlow(658.682635)
            .loopFlow(Country.DE, 17.365269)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("FFR1AA11 FFR4AA11 1")
            .contingencyId("contingency_1")
            .country1(Country.FR)
            .country2(Country.FR)
            .acReferenceFlow(1039.625330)
            .dcReferenceFlow(1056.885029)
            .allocatedFlow(598.802395)
            .pstFlow(-161.677845)
            .internalFlow(706.586826)
            .loopFlow(Country.DE, -86.826347)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("BBE4AA11 FFR5AA11 1")
            .contingencyId("contingency_1")
            .country1(Country.BE)
            .country2(Country.FR)
            .acReferenceFlow(1000.000000)
            .dcReferenceFlow(1000.000000)
            .allocatedFlow(1000.000000)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("DDE2AA11 DDE3AA11 1")
            .country1(Country.DE)
            .country2(Country.DE)
            .acReferenceFlow(-44.794351)
            .dcReferenceFlow(-51.227151)
            .allocatedFlow(54.856666)
            .pstFlow(-60.942381)
            .internalFlow(93.164421)
            .loopFlow(Country.BE, -23.910690)
            .loopFlow(Country.FR, -6.419820)
            .loopFlow(Country.NL, -5.521045)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("BBE3AA11 BBE4AA11 1")
            .contingencyId("contingency_split_network")
            .country1(Country.BE)
            .country2(Country.BE)
            .acReferenceFlow(571.575251)
            .dcReferenceFlow(571.428571)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("DDE2AA11 NNL3AA11 1")
            .country1(Country.DE)
            .country2(Country.NL)
            .acReferenceFlow(-987.991966)
            .dcReferenceFlow(-990.269305)
            .allocatedFlow(753.941126)
            .pstFlow(-18.657301)
            .loopFlow(Country.BE, 46.431224)
            .loopFlow(Country.DE, 63.273741)
            .loopFlow(Country.FR, 134.559417)
            .loopFlow(Country.NL, 10.721099)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("FFR2AA11 FFR4AA11 1")
            .country1(Country.FR)
            .country2(Country.FR)
            .acReferenceFlow(688.000536)
            .dcReferenceFlow(662.527793)
            .allocatedFlow(391.042984)
            .pstFlow(236.361831)
            .internalFlow(200.726357)
            .loopFlow(Country.BE, -10.287157)
            .loopFlow(Country.DE, -152.940889)
            .loopFlow(Country.NL, -2.375333)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("BBE4AA11 FFR5AA11 1")
            .country1(Country.BE)
            .country2(Country.FR)
            .acReferenceFlow(506.004017)
            .dcReferenceFlow(504.865347)
            .allocatedFlow(623.029437)
            .pstFlow(9.328651)
            .loopFlow(Country.BE, -23.215612)
            .loopFlow(Country.DE, -31.636871)
            .loopFlow(Country.FR, -67.279708)
            .loopFlow(Country.NL, -5.360549)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("FFR1AA11 FFR3AA11 1")
            .contingencyId("contingency_split_network")
            .country1(Country.FR)
            .country2(Country.FR)
            .acReferenceFlow(-248.808935)
            .dcReferenceFlow(-255.688143)
            .allocatedFlow(172.455090)
            .pstFlow(-64.671138)
            .internalFlow(182.634731)
            .loopFlow(Country.DE, -34.730539)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("BBE1AA11 BBE3AA11 2")
            .contingencyId("contingency_1")
            .country1(Country.BE)
            .country2(Country.BE)
            .acReferenceFlow(-428.685692)
            .dcReferenceFlow(-428.571429)
            .allocatedFlow(157.894737)
            .internalFlow(270.676692)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("FFR1AA11 FFR5AA11 1")
            .contingencyId("contingency_1")
            .country1(Country.FR)
            .country2(Country.FR)
            .acReferenceFlow(NaN)
            .dcReferenceFlow(NaN)
            .validate();
        assertEquals(4, flowDecompositionResults.getZoneSet().size());
        assertTrue(flowDecompositionResults.getZoneSet().contains(Country.BE));
        assertTrue(flowDecompositionResults.getZoneSet().contains(Country.DE));
        assertTrue(flowDecompositionResults.getZoneSet().contains(Country.FR));
        assertTrue(flowDecompositionResults.getZoneSet().contains(Country.NL));
    }

    @SuppressWarnings("checkstyle:MethodLength")
    private static void validateFlowDecompositionResultsUsingFLDMethodology(FlowDecompositionResults flowDecompositionResults) {
        assertThat(flowDecompositionResults)
            .branchId("BBE1AA11 BBE2AA11 1")
            .country1(Country.BE)
            .country2(Country.BE)
            .acReferenceFlow(-1073.022)
            .dcReferenceFlow(-1072.819)
            .allocatedFlow(833.251)
            .pstFlow(2.665)
            .internalFlow(251.523)
            .loopFlow(Country.FR, -14.047)
            .loopFlow(Country.DE, -1.944)
            .loopFlow(Country.NL, 1.370)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("BBE1AA11 BBE2AA11 1")
            .contingencyId("contingency_1")
            .country1(Country.BE)
            .country2(Country.BE)
            .acReferenceFlow(-1213.943)
            .dcReferenceFlow(-1214.286)
            .allocatedFlow(990.286)
            .internalFlow(224.000)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("BBE1AA11 BBE2AA11 1")
            .contingencyId("contingency_desync")
            .country1(Country.BE)
            .country2(Country.BE)
            .acReferenceFlow(-1213.943)
            .dcReferenceFlow(-1214.286)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("BBE1AA11 BBE2AA11 1")
            .contingencyId("contingency_split_network")
            .country1(Country.BE)
            .country2(Country.BE)
            .acReferenceFlow(-1285.274)
            .dcReferenceFlow(-1285.714)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("BBE1AA11 BBE3AA11 1")
            .country1(Country.BE)
            .country2(Country.BE)
            .acReferenceFlow(-146.329)
            .dcReferenceFlow(-145.637)
            .allocatedFlow(91.419)
            .pstFlow(5.331)
            .internalFlow(78.128)
            .loopFlow(Country.FR, -28.093)
            .loopFlow(Country.DE, -3.888)
            .loopFlow(Country.NL, 2.741)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("BBE1AA11 BBE3AA11 1")
            .contingencyId("contingency_1")
            .country1(Country.BE)
            .country2(Country.BE)
            .acReferenceFlow(-428.686)
            .dcReferenceFlow(-428.571)
            .allocatedFlow(336.571)
            .internalFlow(92.000)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("BBE1AA11 BBE3AA11 1")
            .contingencyId("contingency_desync")
            .country1(Country.BE)
            .country2(Country.BE)
            .acReferenceFlow(-428.686)
            .dcReferenceFlow(-428.571)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("BBE1AA11 BBE3AA11 1")
            .contingencyId("contingency_split_network")
            .country1(Country.BE)
            .country2(Country.BE)
            .acReferenceFlow(-571.575)
            .dcReferenceFlow(-571.429)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("BBE1AA11 BBE3AA11 2")
            .country1(Country.BE)
            .country2(Country.BE)
            .acReferenceFlow(-146.329)
            .dcReferenceFlow(-145.637)
            .allocatedFlow(91.419)
            .pstFlow(5.331)
            .internalFlow(78.128)
            .loopFlow(Country.FR, -28.093)
            .loopFlow(Country.DE, -3.888)
            .loopFlow(Country.NL, 2.741)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("BBE1AA11 BBE3AA11 2")
            .contingencyId("contingency_1")
            .country1(Country.BE)
            .country2(Country.BE)
            .acReferenceFlow(-428.686)
            .dcReferenceFlow(-428.571)
            .allocatedFlow(336.571)
            .internalFlow(92.000)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("BBE1AA11 BBE3AA11 2")
            .contingencyId("contingency_desync")
            .country1(Country.BE)
            .country2(Country.BE)
            .acReferenceFlow(-428.686)
            .dcReferenceFlow(-428.571)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("BBE1AA11 BBE3AA11 2")
            .contingencyId("contingency_split_network")
            .country1(Country.BE)
            .country2(Country.BE)
            .acReferenceFlow(-571.575)
            .dcReferenceFlow(-571.429)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("BBE1AA11 FFR5AA11 1")
            .country1(Country.BE)
            .country2(Country.FR)
            .acReferenceFlow(506.004)
            .dcReferenceFlow(504.865)
            .allocatedFlow(553.753)
            .pstFlow(9.329)
            .loopFlow(Country.BE, -7.046)
            .loopFlow(Country.FR, -49.163)
            .loopFlow(Country.DE, -6.803)
            .loopFlow(Country.NL, 4.796)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("BBE1AA11 FFR5AA11 1")
            .contingencyId("contingency_1")
            .country1(Country.BE)
            .country2(Country.FR)
            .acReferenceFlow(1000.000)
            .dcReferenceFlow(1000.000)
            .allocatedFlow(1000.000)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("BBE1AA11 FFR5AA11 1")
            .contingencyId("contingency_desync")
            .country1(Country.BE)
            .country2(Country.FR)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("BBE1AA11 FFR5AA11 1")
            .contingencyId("contingency_split_network")
            .country1(Country.BE)
            .country2(Country.FR)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("BBE2AA11 BBE3AA11 1")
            .country1(Country.BE)
            .country2(Country.BE)
            .acReferenceFlow(926.978)
            .dcReferenceFlow(927.181)
            .allocatedFlow(741.832)
            .pstFlow(-2.665)
            .internalFlow(173.394)
            .loopFlow(Country.FR, 14.047)
            .loopFlow(Country.DE, 1.944)
            .loopFlow(Country.NL, -1.370)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("BBE2AA11 BBE3AA11 1")
            .contingencyId("contingency_1")
            .country1(Country.BE)
            .country2(Country.BE)
            .acReferenceFlow(786.057)
            .dcReferenceFlow(785.714)
            .allocatedFlow(653.714)
            .internalFlow(132.000)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("BBE2AA11 BBE3AA11 1")
            .contingencyId("contingency_desync")
            .country1(Country.BE)
            .country2(Country.BE)
            .acReferenceFlow(786.057)
            .dcReferenceFlow(785.714)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("BBE2AA11 BBE3AA11 1")
            .contingencyId("contingency_split_network")
            .country1(Country.BE)
            .country2(Country.BE)
            .acReferenceFlow(714.726)
            .dcReferenceFlow(714.286)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("BBE3AA11 BBE4AA11 1")
            .country1(Country.BE)
            .country2(Country.BE)
            .acReferenceFlow(146.329)
            .dcReferenceFlow(145.637)
            .allocatedFlow(91.419)
            .pstFlow(5.331)
            .internalFlow(78.128)
            .loopFlow(Country.FR, -28.093)
            .loopFlow(Country.DE, -3.888)
            .loopFlow(Country.NL, 2.741)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("BBE3AA11 BBE4AA11 1")
            .contingencyId("contingency_1")
            .country1(Country.BE)
            .country2(Country.BE)
            .acReferenceFlow(428.686)
            .dcReferenceFlow(428.571)
            .allocatedFlow(336.571)
            .internalFlow(92.000)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("BBE3AA11 BBE4AA11 1")
            .contingencyId("contingency_desync")
            .country1(Country.BE)
            .country2(Country.BE)
            .acReferenceFlow(428.686)
            .dcReferenceFlow(428.571)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("BBE3AA11 BBE4AA11 1")
            .contingencyId("contingency_split_network")
            .country1(Country.BE)
            .country2(Country.BE)
            .acReferenceFlow(571.575)
            .dcReferenceFlow(571.429)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("BBE4AA11 FFR5AA11 1")
            .country1(Country.BE)
            .country2(Country.FR)
            .acReferenceFlow(506.004)
            .dcReferenceFlow(504.865)
            .allocatedFlow(553.753)
            .pstFlow(9.329)
            .loopFlow(Country.BE, -7.046)
            .loopFlow(Country.FR, -49.163)
            .loopFlow(Country.DE, -6.803)
            .loopFlow(Country.NL, 4.796)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("BBE4AA11 FFR5AA11 1")
            .contingencyId("contingency_1")
            .country1(Country.BE)
            .country2(Country.FR)
            .acReferenceFlow(1000.000)
            .dcReferenceFlow(1000.000)
            .allocatedFlow(1000.000)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("BBE4AA11 FFR5AA11 1")
            .contingencyId("contingency_desync")
            .country1(Country.BE)
            .country2(Country.FR)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("BBE4AA11 FFR5AA11 1")
            .contingencyId("contingency_split_network")
            .country1(Country.BE)
            .country2(Country.FR)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("DDE1AA11 DDE2AA11 1")
            .country1(Country.DE)
            .country2(Country.DE)
            .acReferenceFlow(-32.786)
            .dcReferenceFlow(-41.496)
            .allocatedFlow(-28.078)
            .pstFlow(-79.600)
            .internalFlow(44.790)
            .loopFlow(Country.BE, 6.835)
            .loopFlow(Country.FR, 102.201)
            .loopFlow(Country.NL, -4.652)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("DDE1AA11 DDE2AA11 1")
            .contingencyId("contingency_1")
            .country1(Country.DE)
            .country2(Country.DE)
            .acReferenceFlow(446.490)
            .dcReferenceFlow(438.814)
            .allocatedFlow(338.075)
            .pstFlow(70.550)
            .internalFlow(77.958)
            .loopFlow(Country.FR, -47.769)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("DDE1AA11 DDE2AA11 1")
            .contingencyId("contingency_desync")
            .country1(Country.DE)
            .country2(Country.DE)
            .acReferenceFlow(410.528)
            .dcReferenceFlow(402.886)
            .allocatedFlow(373.198)
            .pstFlow(70.550)
            .internalFlow(71.136)
            .loopFlow(Country.FR, -111.999)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("DDE1AA11 DDE2AA11 1")
            .contingencyId("contingency_split_network")
            .country1(Country.DE)
            .country2(Country.DE)
            .acReferenceFlow(410.528)
            .dcReferenceFlow(402.886)
            .allocatedFlow(373.198)
            .pstFlow(70.550)
            .internalFlow(71.136)
            .loopFlow(Country.FR, -111.999)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("DDE1AA11 DDE4AA11 1")
            .country1(Country.DE)
            .country2(Country.DE)
            .acReferenceFlow(-489.070)
            .dcReferenceFlow(-486.168)
            .allocatedFlow(182.289)
            .pstFlow(26.533)
            .internalFlow(312.140)
            .loopFlow(Country.BE, -2.278)
            .loopFlow(Country.FR, -34.067)
            .loopFlow(Country.NL, 1.551)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("DDE1AA11 DDE4AA11 1")
            .contingencyId("contingency_1")
            .country1(Country.DE)
            .country2(Country.DE)
            .acReferenceFlow(-648.880)
            .dcReferenceFlow(-646.271)
            .allocatedFlow(330.189)
            .pstFlow(23.517)
            .internalFlow(308.489)
            .loopFlow(Country.FR, -15.923)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("DDE1AA11 DDE4AA11 1")
            .contingencyId("contingency_desync")
            .country1(Country.DE)
            .country2(Country.DE)
            .acReferenceFlow(-636.887)
            .dcReferenceFlow(-634.295)
            .allocatedFlow(338.930)
            .pstFlow(23.517)
            .internalFlow(309.181)
            .loopFlow(Country.FR, -37.333)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("DDE1AA11 DDE4AA11 1")
            .contingencyId("contingency_split_network")
            .country1(Country.DE)
            .country2(Country.DE)
            .acReferenceFlow(-636.887)
            .dcReferenceFlow(-634.295)
            .allocatedFlow(338.930)
            .pstFlow(23.517)
            .internalFlow(309.181)
            .loopFlow(Country.FR, -37.333)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("DDE2AA11 DDE3AA11 1")
            .country1(Country.DE)
            .country2(Country.DE)
            .acReferenceFlow(-44.794)
            .dcReferenceFlow(-51.227)
            .allocatedFlow(79.428)
            .pstFlow(-60.942)
            .internalFlow(31.184)
            .loopFlow(Country.BE, -7.257)
            .loopFlow(Country.FR, 3.875)
            .loopFlow(Country.NL, 4.940)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("DDE2AA11 DDE3AA11 1")
            .contingencyId("contingency_1")
            .country1(Country.DE)
            .country2(Country.DE)
            .acReferenceFlow(-553.510)
            .dcReferenceFlow(-561.186)
            .allocatedFlow(509.434)
            .pstFlow(-70.550)
            .internalFlow(74.534)
            .loopFlow(Country.FR, 47.769)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("DDE2AA11 DDE3AA11 1")
            .contingencyId("contingency_desync")
            .country1(Country.DE)
            .country2(Country.DE)
            .acReferenceFlow(-589.472)
            .dcReferenceFlow(-597.114)
            .allocatedFlow(483.211)
            .pstFlow(-70.550)
            .internalFlow(72.456)
            .loopFlow(Country.FR, 111.999)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("DDE2AA11 DDE3AA11 1")
            .contingencyId("contingency_split_network")
            .country1(Country.DE)
            .country2(Country.DE)
            .acReferenceFlow(-589.472)
            .dcReferenceFlow(-597.114)
            .allocatedFlow(483.211)
            .pstFlow(-70.550)
            .internalFlow(72.456)
            .loopFlow(Country.FR, 111.999)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("DDE2AA11 NNL3AA11 1")
            .country1(Country.DE)
            .country2(Country.NL)
            .acReferenceFlow(-987.992)
            .dcReferenceFlow(-990.269)
            .allocatedFlow(892.493)
            .pstFlow(-18.657)
            .loopFlow(Country.BE, 14.093)
            .loopFlow(Country.FR, 98.327)
            .loopFlow(Country.DE, 13.606)
            .loopFlow(Country.NL, -9.592)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("FFR1AA11 FFR2AA11 1")
            .country1(Country.FR)
            .country2(Country.FR)
            .acReferenceFlow(635.089)
            .dcReferenceFlow(624.361)
            .allocatedFlow(202.368)
            .pstFlow(97.789)
            .internalFlow(316.150)
            .loopFlow(Country.BE, -0.591)
            .loopFlow(Country.DE, 8.243)
            .loopFlow(Country.NL, 0.402)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("FFR1AA11 FFR2AA11 1")
            .contingencyId("contingency_1")
            .country1(Country.FR)
            .country2(Country.FR)
            .acReferenceFlow(676.753)
            .dcReferenceFlow(665.869)
            .allocatedFlow(311.171)
            .pstFlow(97.007)
            .internalFlow(240.491)
            .loopFlow(Country.DE, 17.200)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("FFR1AA11 FFR2AA11 1")
            .contingencyId("contingency_desync")
            .country1(Country.FR)
            .country2(Country.FR)
            .acReferenceFlow(376.901)
            .dcReferenceFlow(366.468)
            .allocatedFlow(186.061)
            .pstFlow(97.007)
            .internalFlow(66.679)
            .loopFlow(Country.DE, 16.721)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("FFR1AA11 FFR2AA11 1")
            .contingencyId("contingency_split_network")
            .country1(Country.FR)
            .country2(Country.FR)
            .acReferenceFlow(376.901)
            .dcReferenceFlow(366.468)
            .allocatedFlow(186.061)
            .pstFlow(97.007)
            .internalFlow(66.679)
            .loopFlow(Country.DE, 16.721)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("FFR1AA11 FFR3AA11 1")
            .country1(Country.FR)
            .country2(Country.FR)
            .acReferenceFlow(-414.303)
            .dcReferenceFlow(-420.336)
            .allocatedFlow(376.826)
            .pstFlow(-58.973)
            .internalFlow(113.888)
            .loopFlow(Country.BE, -4.304)
            .loopFlow(Country.DE, -10.031)
            .loopFlow(Country.NL, 2.929)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("FFR1AA11 FFR3AA11 1")
            .contingencyId("contingency_1")
            .country1(Country.FR)
            .country2(Country.FR)
            .acReferenceFlow(-716.378)
            .dcReferenceFlow(-722.754)
            .allocatedFlow(678.630)
            .pstFlow(-64.671)
            .internalFlow(120.262)
            .loopFlow(Country.DE, -11.467)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("FFR1AA11 FFR3AA11 1")
            .contingencyId("contingency_desync")
            .country1(Country.FR)
            .country2(Country.FR)
            .acReferenceFlow(-248.809)
            .dcReferenceFlow(-255.688)
            .allocatedFlow(233.288)
            .pstFlow(-64.671)
            .internalFlow(98.219)
            .loopFlow(Country.DE, -11.147)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("FFR1AA11 FFR3AA11 1")
            .contingencyId("contingency_split_network")
            .country1(Country.FR)
            .country2(Country.FR)
            .acReferenceFlow(-248.809)
            .dcReferenceFlow(-255.688)
            .allocatedFlow(233.288)
            .pstFlow(-64.671)
            .internalFlow(98.219)
            .loopFlow(Country.DE, -11.147)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("FFR1AA11 FFR4AA11 1")
            .country1(Country.FR)
            .country2(Country.FR)
            .acReferenceFlow(779.213)
            .dcReferenceFlow(795.976)
            .allocatedFlow(477.962)
            .pstFlow(-156.762)
            .internalFlow(494.235)
            .loopFlow(Country.BE, -3.713)
            .loopFlow(Country.DE, -18.274)
            .loopFlow(Country.NL, 2.527)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("FFR1AA11 FFR4AA11 1")
            .contingencyId("contingency_1")
            .country1(Country.FR)
            .country2(Country.FR)
            .acReferenceFlow(1039.625)
            .dcReferenceFlow(1056.885)
            .allocatedFlow(831.587)
            .pstFlow(-161.678)
            .internalFlow(415.643)
            .loopFlow(Country.DE, -28.667)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("FFR1AA11 FFR4AA11 1")
            .contingencyId("contingency_desync")
            .country1(Country.FR)
            .country2(Country.FR)
            .acReferenceFlow(871.908)
            .dcReferenceFlow(889.220)
            .allocatedFlow(601.425)
            .pstFlow(-161.678)
            .internalFlow(477.340)
            .loopFlow(Country.DE, -27.868)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("FFR1AA11 FFR4AA11 1")
            .contingencyId("contingency_split_network")
            .country1(Country.FR)
            .country2(Country.FR)
            .acReferenceFlow(871.908)
            .dcReferenceFlow(889.220)
            .allocatedFlow(601.425)
            .pstFlow(-161.678)
            .internalFlow(477.340)
            .loopFlow(Country.DE, -27.868)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("FFR1AA11 FFR5AA11 1")
            .country1(Country.FR)
            .country2(Country.FR)
            .acReferenceFlow(NaN)
            .dcReferenceFlow(NaN)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("FFR1AA11 FFR5AA11 1")
            .contingencyId("contingency_1")
            .country1(Country.FR)
            .country2(Country.FR)
            .acReferenceFlow(NaN)
            .dcReferenceFlow(NaN)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("FFR1AA11 FFR5AA11 1")
            .contingencyId("contingency_desync")
            .country1(Country.FR)
            .country2(Country.FR)
            .acReferenceFlow(NaN)
            .dcReferenceFlow(NaN)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("FFR1AA11 FFR5AA11 1")
            .contingencyId("contingency_split_network")
            .country1(Country.FR)
            .country2(Country.FR)
            .acReferenceFlow(NaN)
            .dcReferenceFlow(NaN)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("FFR2AA11 DDE3AA11 1")
            .country1(Country.FR)
            .country2(Country.DE)
            .acReferenceFlow(544.794)
            .dcReferenceFlow(551.227)
            .allocatedFlow(579.428)
            .pstFlow(-60.942)
            .loopFlow(Country.BE, -7.257)
            .loopFlow(Country.FR, 3.875)
            .loopFlow(Country.DE, 31.184)
            .loopFlow(Country.NL, 4.940)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("FFR2AA11 DDE3AA11 1")
            .contingencyId("contingency_1")
            .country1(Country.FR)
            .country2(Country.DE)
            .acReferenceFlow(1053.510)
            .dcReferenceFlow(1061.186)
            .allocatedFlow(1009.434)
            .pstFlow(-70.550)
            .loopFlow(Country.FR, 47.769)
            .loopFlow(Country.DE, 74.534)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("FFR2AA11 DDE3AA11 1")
            .contingencyId("contingency_desync")
            .country1(Country.FR)
            .country2(Country.DE)
            .acReferenceFlow(1089.472)
            .dcReferenceFlow(1097.114)
            .allocatedFlow(983.211)
            .pstFlow(-70.550)
            .loopFlow(Country.FR, 111.999)
            .loopFlow(Country.DE, 72.456)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("FFR2AA11 DDE3AA11 1")
            .contingencyId("contingency_split_network")
            .country1(Country.FR)
            .country2(Country.DE)
            .acReferenceFlow(1089.472)
            .dcReferenceFlow(1097.114)
            .allocatedFlow(983.211)
            .pstFlow(-70.550)
            .loopFlow(Country.FR, 111.999)
            .loopFlow(Country.DE, 72.456)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("FFR2AA11 FFR3AA11 1")
            .country1(Country.FR)
            .country2(Country.FR)
            .acReferenceFlow(-1048.853)
            .dcReferenceFlow(-1044.697)
            .allocatedFlow(579.194)
            .pstFlow(38.815)
            .internalFlow(430.038)
            .loopFlow(Country.BE, -4.894)
            .loopFlow(Country.DE, -1.788)
            .loopFlow(Country.NL, 3.331)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("FFR2AA11 FFR3AA11 1")
            .contingencyId("contingency_1")
            .country1(Country.FR)
            .country2(Country.FR)
            .acReferenceFlow(-1391.811)
            .dcReferenceFlow(-1388.623)
            .allocatedFlow(989.801)
            .pstFlow(32.336)
            .internalFlow(360.753)
            .loopFlow(Country.DE, 5.733)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("FFR2AA11 FFR3AA11 1")
            .contingencyId("contingency_desync")
            .country1(Country.FR)
            .country2(Country.FR)
            .acReferenceFlow(-625.596)
            .dcReferenceFlow(-622.156)
            .allocatedFlow(419.349)
            .pstFlow(32.336)
            .internalFlow(164.898)
            .loopFlow(Country.DE, 5.574)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("FFR2AA11 FFR3AA11 1")
            .contingencyId("contingency_split_network")
            .country1(Country.FR)
            .country2(Country.FR)
            .acReferenceFlow(-625.596)
            .dcReferenceFlow(-622.156)
            .allocatedFlow(419.349)
            .pstFlow(32.336)
            .internalFlow(164.898)
            .loopFlow(Country.DE, 5.574)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("FFR2AA11 FFR3AA11 2")
            .country1(Country.FR)
            .country2(Country.FR)
            .acReferenceFlow(-1048.853)
            .dcReferenceFlow(-1044.697)
            .allocatedFlow(579.194)
            .pstFlow(38.815)
            .internalFlow(430.038)
            .loopFlow(Country.BE, -4.894)
            .loopFlow(Country.DE, -1.788)
            .loopFlow(Country.NL, 3.331)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("FFR2AA11 FFR3AA11 2")
            .contingencyId("contingency_1")
            .country1(Country.FR)
            .country2(Country.FR)
            .acReferenceFlow(-1391.811)
            .dcReferenceFlow(-1388.623)
            .allocatedFlow(989.801)
            .pstFlow(32.336)
            .internalFlow(360.753)
            .loopFlow(Country.DE, 5.733)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("FFR2AA11 FFR3AA11 2")
            .contingencyId("contingency_desync")
            .country1(Country.FR)
            .country2(Country.FR)
            .acReferenceFlow(-625.596)
            .dcReferenceFlow(-622.156)
            .allocatedFlow(419.349)
            .pstFlow(32.336)
            .internalFlow(164.898)
            .loopFlow(Country.DE, 5.574)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("FFR2AA11 FFR3AA11 2")
            .contingencyId("contingency_split_network")
            .country1(Country.FR)
            .country2(Country.FR)
            .acReferenceFlow(-625.596)
            .dcReferenceFlow(-622.156)
            .allocatedFlow(419.349)
            .pstFlow(32.336)
            .internalFlow(164.898)
            .loopFlow(Country.DE, 5.574)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("FFR2AA11 FFR4AA11 1")
            .country1(Country.FR)
            .country2(Country.FR)
            .acReferenceFlow(688.001)
            .dcReferenceFlow(662.528)
            .allocatedFlow(275.595)
            .pstFlow(236.362)
            .internalFlow(178.085)
            .loopFlow(Country.BE, -3.122)
            .loopFlow(Country.DE, -26.517)
            .loopFlow(Country.NL, 2.125)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("FFR2AA11 FFR4AA11 1")
            .contingencyId("contingency_1")
            .country1(Country.FR)
            .country2(Country.FR)
            .acReferenceFlow(906.865)
            .dcReferenceFlow(881.929)
            .allocatedFlow(520.416)
            .pstFlow(232.228)
            .internalFlow(175.152)
            .loopFlow(Country.DE, -45.867)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("FFR2AA11 FFR4AA11 1")
            .contingencyId("contingency_desync")
            .country1(Country.FR)
            .country2(Country.FR)
            .acReferenceFlow(1038.621)
            .dcReferenceFlow(1013.665)
            .allocatedFlow(415.364)
            .pstFlow(232.228)
            .internalFlow(410.661)
            .loopFlow(Country.DE, -44.588)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("FFR2AA11 FFR4AA11 1")
            .contingencyId("contingency_split_network")
            .country1(Country.FR)
            .country2(Country.FR)
            .acReferenceFlow(1038.621)
            .dcReferenceFlow(1013.665)
            .allocatedFlow(415.364)
            .pstFlow(232.228)
            .internalFlow(410.661)
            .loopFlow(Country.DE, -44.588)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("FFR3AA11 FFR5AA11 1")
            .country1(Country.FR)
            .country2(Country.FR)
            .acReferenceFlow(-1012.008)
            .dcReferenceFlow(-1009.731)
            .allocatedFlow(1107.507)
            .pstFlow(18.657)
            .internalFlow(-98.327)
            .loopFlow(Country.BE, -14.093)
            .loopFlow(Country.DE, -13.606)
            .loopFlow(Country.NL, 9.592)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("FFR3AA11 FFR5AA11 1")
            .contingencyId("contingency_1")
            .country1(Country.FR)
            .country2(Country.FR)
            .acReferenceFlow(-2000.000)
            .dcReferenceFlow(-2000.000)
            .allocatedFlow(2000.000)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("FFR4AA11 DDE1AA11 1")
            .country1(Country.FR)
            .country2(Country.DE)
            .acReferenceFlow(478.144)
            .dcReferenceFlow(472.336)
            .allocatedFlow(355.184)
            .pstFlow(53.066)
            .loopFlow(Country.BE, -4.557)
            .loopFlow(Country.FR, -68.134)
            .loopFlow(Country.DE, 133.675)
            .loopFlow(Country.NL, 3.102)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("FFR4AA11 DDE1AA11 1")
            .contingencyId("contingency_1")
            .country1(Country.FR)
            .country2(Country.DE)
            .acReferenceFlow(797.610)
            .dcReferenceFlow(792.543)
            .allocatedFlow(660.377)
            .pstFlow(47.034)
            .loopFlow(Country.FR, -31.846)
            .loopFlow(Country.DE, 116.977)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("FFR4AA11 DDE1AA11 1")
            .contingencyId("contingency_desync")
            .country1(Country.FR)
            .country2(Country.DE)
            .acReferenceFlow(773.642)
            .dcReferenceFlow(768.590)
            .allocatedFlow(677.860)
            .pstFlow(47.034)
            .loopFlow(Country.FR, -74.666)
            .loopFlow(Country.DE, 118.363)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("FFR4AA11 DDE1AA11 1")
            .contingencyId("contingency_split_network")
            .country1(Country.FR)
            .country2(Country.DE)
            .acReferenceFlow(773.642)
            .dcReferenceFlow(768.590)
            .allocatedFlow(677.860)
            .pstFlow(47.034)
            .loopFlow(Country.FR, -74.666)
            .loopFlow(Country.DE, 118.363)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("FFR4AA11 DDE4AA11 1")
            .country1(Country.FR)
            .country2(Country.DE)
            .acReferenceFlow(-10.930)
            .dcReferenceFlow(-13.832)
            .allocatedFlow(-172.895)
            .pstFlow(-26.533)
            .loopFlow(Country.BE, 2.278)
            .loopFlow(Country.FR, 34.067)
            .loopFlow(Country.DE, 178.465)
            .loopFlow(Country.NL, -1.551)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("FFR4AA11 DDE4AA11 1")
            .contingencyId("contingency_1")
            .country1(Country.FR)
            .country2(Country.DE)
            .acReferenceFlow(148.880)
            .dcReferenceFlow(146.271)
            .allocatedFlow(330.189)
            .pstFlow(23.517)
            .loopFlow(Country.FR, -15.923)
            .loopFlow(Country.DE, -191.511)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("FFR4AA11 DDE4AA11 1")
            .contingencyId("contingency_desync")
            .country1(Country.FR)
            .country2(Country.DE)
            .acReferenceFlow(136.887)
            .dcReferenceFlow(134.295)
            .allocatedFlow(338.930)
            .pstFlow(23.517)
            .loopFlow(Country.FR, -37.333)
            .loopFlow(Country.DE, -190.819)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("FFR4AA11 DDE4AA11 1")
            .contingencyId("contingency_split_network")
            .country1(Country.FR)
            .country2(Country.DE)
            .acReferenceFlow(136.887)
            .dcReferenceFlow(134.295)
            .allocatedFlow(338.930)
            .pstFlow(23.517)
            .loopFlow(Country.FR, -37.333)
            .loopFlow(Country.DE, -190.819)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("NNL1AA11 NNL2AA11 1")
            .country1(Country.NL)
            .country2(Country.NL)
            .acReferenceFlow(-162.722)
            .dcReferenceFlow(-163.423)
            .allocatedFlow(186.749)
            .pstFlow(-6.219)
            .internalFlow(-59.116)
            .loopFlow(Country.BE, 4.698)
            .loopFlow(Country.FR, 32.776)
            .loopFlow(Country.DE, 4.535)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("NNL1AA11 NNL2AA11 1")
            .contingencyId("contingency_1")
            .country1(Country.NL)
            .country2(Country.NL)
            .acReferenceFlow(166.673)
            .dcReferenceFlow(166.667)
            .allocatedFlow(-41.667)
            .internalFlow(208.333)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("NNL1AA11 NNL2AA11 1")
            .contingencyId("contingency_desync")
            .country1(Country.NL)
            .country2(Country.NL)
            .acReferenceFlow(166.673)
            .dcReferenceFlow(166.667)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("NNL1AA11 NNL2AA11 1")
            .contingencyId("contingency_split_network")
            .country1(Country.NL)
            .country2(Country.NL)
            .acReferenceFlow(NaN)
            .dcReferenceFlow(NaN)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("NNL1AA11 NNL3AA11 1")
            .country1(Country.NL)
            .country2(Country.NL)
            .acReferenceFlow(662.722)
            .dcReferenceFlow(663.423)
            .allocatedFlow(518.994)
            .pstFlow(-6.219)
            .internalFlow(108.639)
            .loopFlow(Country.BE, 4.698)
            .loopFlow(Country.FR, 32.776)
            .loopFlow(Country.DE, 4.535)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("NNL1AA11 NNL3AA11 1")
            .contingencyId("contingency_1")
            .country1(Country.NL)
            .country2(Country.NL)
            .acReferenceFlow(333.327)
            .dcReferenceFlow(333.333)
            .allocatedFlow(41.667)
            .internalFlow(291.667)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("NNL1AA11 NNL3AA11 1")
            .contingencyId("contingency_desync")
            .country1(Country.NL)
            .country2(Country.NL)
            .acReferenceFlow(333.327)
            .dcReferenceFlow(333.333)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("NNL1AA11 NNL3AA11 1")
            .contingencyId("contingency_split_network")
            .country1(Country.NL)
            .country2(Country.NL)
            .acReferenceFlow(NaN)
            .dcReferenceFlow(NaN)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("NNL2AA11 BBE3AA11 1")
            .country1(Country.NL)
            .country2(Country.BE)
            .acReferenceFlow(-1487.992)
            .dcReferenceFlow(-1490.269)
            .allocatedFlow(1392.493)
            .pstFlow(-18.657)
            .loopFlow(Country.BE, 14.093)
            .loopFlow(Country.FR, 98.327)
            .loopFlow(Country.DE, 13.606)
            .loopFlow(Country.NL, -9.592)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("NNL2AA11 BBE3AA11 1")
            .contingencyId("contingency_1")
            .country1(Country.NL)
            .country2(Country.BE)
            .acReferenceFlow(-500.000)
            .dcReferenceFlow(-500.000)
            .allocatedFlow(500.000)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("NNL2AA11 BBE3AA11 1")
            .contingencyId("contingency_desync")
            .country1(Country.NL)
            .country2(Country.BE)
            .acReferenceFlow(-500.000)
            .dcReferenceFlow(-500.000)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("NNL2AA11 NNL3AA11 1")
            .country1(Country.NL)
            .country2(Country.NL)
            .acReferenceFlow(825.270)
            .dcReferenceFlow(826.846)
            .allocatedFlow(705.744)
            .pstFlow(-12.438)
            .internalFlow(49.524)
            .loopFlow(Country.BE, 9.395)
            .loopFlow(Country.FR, 65.551)
            .loopFlow(Country.DE, 9.071)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("NNL2AA11 NNL3AA11 1")
            .contingencyId("contingency_1")
            .country1(Country.NL)
            .country2(Country.NL)
            .acReferenceFlow(166.673)
            .dcReferenceFlow(166.667)
            .allocatedFlow(83.333)
            .internalFlow(83.333)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("NNL2AA11 NNL3AA11 1")
            .contingencyId("contingency_desync")
            .country1(Country.NL)
            .country2(Country.NL)
            .acReferenceFlow(166.673)
            .dcReferenceFlow(166.667)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("NNL2AA11 NNL3AA11 1")
            .contingencyId("contingency_split_network")
            .country1(Country.NL)
            .country2(Country.NL)
            .acReferenceFlow(NaN)
            .dcReferenceFlow(NaN)
            .validate();
        assertEquals(4, flowDecompositionResults.getZoneSet().size());
        assertTrue(flowDecompositionResults.getZoneSet().contains(Country.BE));
        assertTrue(flowDecompositionResults.getZoneSet().contains(Country.DE));
        assertTrue(flowDecompositionResults.getZoneSet().contains(Country.FR));
        assertTrue(flowDecompositionResults.getZoneSet().contains(Country.NL));
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
    void testFlowDecompositionOnHvdcNetworkUsingMode(FlowDecompositionParameters.FlowPartitionMode flowPartitionMode) {
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

    @ParameterizedTest(name = "Mode={0}")
    @EnumSource(value = FlowDecompositionParameters.FlowPartitionMode.class, names = {
        "MATRIX_BASED",
        "DIRECT_SENSITIVITY_BASED",
        "FULL_LINE_DECOMPOSITION",
        "FAST_FULL_LINE_DECOMPOSITION"
    })
    void testSimpleNetworkWithXNodeGen(FlowDecompositionParameters.FlowPartitionMode flowPartitionMode) {
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

    @SuppressWarnings("checkstyle:MethodLength")
    private static void validatePowerFlowColouringResultsWithXnodeGen(FlowDecompositionResults flowDecompositionResults) {
        assertThat(flowDecompositionResults)
            .branchId("BBE1AA1  BBE2AA1  1")
            .country1(Country.BE)
            .country2(Country.BE)
            .acReferenceFlow(-878.370)
            .dcReferenceFlow(-878.333)
            .allocatedFlow(-48.390)
            .xNodeFlow(57.500)
            .internalFlow(814.035)
            .loopFlow(Country.DE, 4.444)
            .loopFlow(Country.NL, 3.125)
            .loopFlow(Country.FR, 47.619)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("BBE1AA1  BBE3AA1  1")
            .country1(Country.BE)
            .country2(Country.BE)
            .acReferenceFlow(-121.630)
            .dcReferenceFlow(-121.667)
            .allocatedFlow(-336.259)
            .xNodeFlow(-32.500)
            .internalFlow(545.614)
            .loopFlow(Country.DE, -4.444)
            .loopFlow(Country.NL, -3.125)
            .loopFlow(Country.FR, -47.619)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("BBE2AA1  BBE3AA1  1")
            .country1(Country.BE)
            .country2(Country.BE)
            .acReferenceFlow(-756.898)
            .dcReferenceFlow(-756.667)
            .allocatedFlow(287.869)
            .xNodeFlow(90.000)
            .internalFlow(268.421)
            .loopFlow(Country.DE, 8.889)
            .loopFlow(Country.NL, 6.250)
            .loopFlow(Country.FR, 95.238)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("BBE2AA1  FFR3AA1  1")
            .country1(Country.BE)
            .country2(Country.FR)
            .acReferenceFlow(664.731)
            .dcReferenceFlow(665.000)
            .allocatedFlow(460.960)
            .xNodeFlow(127.500)
            .loopFlow(Country.DE, -13.333)
            .loopFlow(Country.BE, 242.105)
            .loopFlow(Country.NL, -9.375)
            .loopFlow(Country.FR, -142.857)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("DDE1AA1  DDE2AA1  1")
            .country1(Country.DE)
            .country2(Country.DE)
            .acReferenceFlow(-478.511)
            .dcReferenceFlow(-478.333)
            .allocatedFlow(386.405)
            .xNodeFlow(7.500)
            .internalFlow(26.667)
            .loopFlow(Country.BE, 7.018)
            .loopFlow(Country.NL, 3.125)
            .loopFlow(Country.FR, 47.619)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("DDE1AA1  DDE3AA1  1")
            .country1(Country.DE)
            .country2(Country.DE)
            .acReferenceFlow(-821.489)
            .dcReferenceFlow(-821.667)
            .allocatedFlow(711.402)
            .xNodeFlow(17.500)
            .internalFlow(106.667)
            .loopFlow(Country.BE, 36.842)
            .loopFlow(Country.NL, -3.125)
            .loopFlow(Country.FR, -47.619)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("DDE2AA1  DDE3AA1  1")
            .country1(Country.DE)
            .country2(Country.DE)
            .acReferenceFlow(-343.242)
            .dcReferenceFlow(-343.333)
            .allocatedFlow(324.997)
            .xNodeFlow(10.000)
            .internalFlow(80.000)
            .loopFlow(Country.BE, 29.825)
            .loopFlow(Country.NL, -6.250)
            .loopFlow(Country.FR, -95.238)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("DDE2AA1  NNL3AA1  1")
            .country1(Country.DE)
            .country2(Country.NL)
            .acReferenceFlow(-1135.269)
            .dcReferenceFlow(-1135.000)
            .allocatedFlow(925.882)
            .xNodeFlow(22.500)
            .loopFlow(Country.DE, 13.333)
            .loopFlow(Country.BE, 21.053)
            .loopFlow(Country.NL, 9.375)
            .loopFlow(Country.FR, 142.857)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("FFR1AA1  FFR2AA1  1")
            .country1(Country.FR)
            .country2(Country.FR)
            .acReferenceFlow(1388.870)
            .dcReferenceFlow(1388.333)
            .allocatedFlow(555.846)
            .xNodeFlow(17.500)
            .internalFlow(785.714)
            .loopFlow(Country.DE, -4.444)
            .loopFlow(Country.BE, 36.842)
            .loopFlow(Country.NL, -3.125)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("FFR1AA1  FFR3AA1  1")
            .country1(Country.FR)
            .country2(Country.FR)
            .acReferenceFlow(-388.870)
            .dcReferenceFlow(-388.333)
            .allocatedFlow(201.272)
            .xNodeFlow(42.500)
            .internalFlow(71.429)
            .loopFlow(Country.DE, -4.444)
            .loopFlow(Country.BE, 80.702)
            .loopFlow(Country.NL, -3.125)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("FFR2AA1  DDE3AA1  1")
            .country1(Country.FR)
            .country2(Country.DE)
            .acReferenceFlow(1664.731)
            .dcReferenceFlow(1665.000)
            .allocatedFlow(1667.539)
            .xNodeFlow(52.500)
            .loopFlow(Country.DE, -13.333)
            .loopFlow(Country.BE, 110.526)
            .loopFlow(Country.NL, -9.375)
            .loopFlow(Country.FR, -142.857)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("FFR2AA1  FFR3AA1  1")
            .country1(Country.FR)
            .country2(Country.FR)
            .acReferenceFlow(-1775.862)
            .dcReferenceFlow(-1776.667)
            .allocatedFlow(757.119)
            .xNodeFlow(60.000)
            .internalFlow(857.143)
            .loopFlow(Country.DE, -8.889)
            .loopFlow(Country.BE, 117.544)
            .loopFlow(Country.NL, -6.250)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("NNL1AA1  NNL2AA1  1")
            .country1(Country.NL)
            .country2(Country.NL)
            .acReferenceFlow(-211.847)
            .dcReferenceFlow(-211.667)
            .allocatedFlow(448.101)
            .xNodeFlow(32.500)
            .internalFlow(-371.875)
            .loopFlow(Country.DE, 4.444)
            .loopFlow(Country.BE, 50.877)
            .loopFlow(Country.FR, 47.619)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("NNL1AA1  NNL3AA1  1")
            .country1(Country.NL)
            .country2(Country.NL)
            .acReferenceFlow(711.847)
            .dcReferenceFlow(711.667)
            .allocatedFlow(329.461)
            .xNodeFlow(7.500)
            .internalFlow(315.625)
            .loopFlow(Country.DE, 4.444)
            .loopFlow(Country.BE, 7.018)
            .loopFlow(Country.FR, 47.619)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("NNL2AA1  BBE3AA1  1")
            .country1(Country.NL)
            .country2(Country.BE)
            .acReferenceFlow(-1635.269)
            .dcReferenceFlow(-1635.000)
            .allocatedFlow(1219.303)
            .xNodeFlow(97.500)
            .loopFlow(Country.DE, 13.333)
            .loopFlow(Country.BE, 152.632)
            .loopFlow(Country.NL, 9.375)
            .loopFlow(Country.FR, 142.857)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("NNL2AA1  NNL3AA1  1")
            .country1(Country.NL)
            .country2(Country.NL)
            .acReferenceFlow(923.422)
            .dcReferenceFlow(923.333)
            .allocatedFlow(777.562)
            .xNodeFlow(40.000)
            .internalFlow(-56.250)
            .loopFlow(Country.DE, 8.889)
            .loopFlow(Country.BE, 57.895)
            .loopFlow(Country.FR, 95.238)
            .validate();
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

    @ParameterizedTest(name = "Mode={0}")
    @EnumSource(value = FlowDecompositionParameters.FlowPartitionMode.class, names = {
        "MATRIX_BASED",
        "DIRECT_SENSITIVITY_BASED",
        "FULL_LINE_DECOMPOSITION",
        "FAST_FULL_LINE_DECOMPOSITION"
    })
    void testSimpleNetworkWithXNodeLoad(FlowDecompositionParameters.FlowPartitionMode flowPartitionMode) {
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

    @SuppressWarnings("checkstyle:MethodLength")
    private static void validatePowerFlowColouringResultsWithXnodeLoad(FlowDecompositionResults flowDecompositionResults) {
        assertThat(flowDecompositionResults)
            .branchId("BBE1AA1  BBE2AA1  1")
            .country1(Country.BE)
            .country2(Country.BE)
            .acReferenceFlow(-878.370)
            .dcReferenceFlow(-878.333)
            .allocatedFlow(-36.764)
            .xNodeFlow(-57.500)
            .internalFlow(917.409)
            .loopFlow(Country.FR, 47.619)
            .loopFlow(Country.NL, 3.125)
            .loopFlow(Country.DE, 4.444)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("BBE1AA1  BBE3AA1  1")
            .country1(Country.BE)
            .country2(Country.BE)
            .acReferenceFlow(-121.630)
            .dcReferenceFlow(-121.667)
            .allocatedFlow(-378.005)
            .xNodeFlow(32.500)
            .internalFlow(522.360)
            .loopFlow(Country.FR, -47.619)
            .loopFlow(Country.NL, -3.125)
            .loopFlow(Country.DE, -4.444)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("BBE2AA1  BBE3AA1  1")
            .country1(Country.BE)
            .country2(Country.BE)
            .acReferenceFlow(-756.898)
            .dcReferenceFlow(-756.667)
            .allocatedFlow(341.240)
            .xNodeFlow(-90.000)
            .internalFlow(395.050)
            .loopFlow(Country.FR, 95.238)
            .loopFlow(Country.NL, 6.250)
            .loopFlow(Country.DE, 8.889)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("BBE2AA1  FFR3AA1  1")
            .country1(Country.BE)
            .country2(Country.FR)
            .acReferenceFlow(664.731)
            .dcReferenceFlow(665.000)
            .allocatedFlow(650.887)
            .xNodeFlow(-127.500)
            .loopFlow(Country.BE, 307.178)
            .loopFlow(Country.FR, -142.857)
            .loopFlow(Country.NL, -9.375)
            .loopFlow(Country.DE, -13.333)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("DDE1AA1  DDE2AA1  1")
            .country1(Country.DE)
            .country2(Country.DE)
            .acReferenceFlow(-478.511)
            .dcReferenceFlow(-478.333)
            .allocatedFlow(403.555)
            .xNodeFlow(-7.500)
            .internalFlow(26.667)
            .loopFlow(Country.BE, 4.868)
            .loopFlow(Country.FR, 47.619)
            .loopFlow(Country.NL, 3.125)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("DDE1AA1  DDE3AA1  1")
            .country1(Country.DE)
            .country2(Country.DE)
            .acReferenceFlow(-821.489)
            .dcReferenceFlow(-821.667)
            .allocatedFlow(734.482)
            .xNodeFlow(-17.500)
            .internalFlow(106.667)
            .loopFlow(Country.BE, 48.762)
            .loopFlow(Country.FR, -47.619)
            .loopFlow(Country.NL, -3.125)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("DDE2AA1  DDE3AA1  1")
            .country1(Country.DE)
            .country2(Country.DE)
            .acReferenceFlow(-343.242)
            .dcReferenceFlow(-343.333)
            .allocatedFlow(330.927)
            .xNodeFlow(-10.000)
            .internalFlow(80.000)
            .loopFlow(Country.BE, 43.894)
            .loopFlow(Country.FR, -95.238)
            .loopFlow(Country.NL, -6.250)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("DDE2AA1  NNL3AA1  1")
            .country1(Country.DE)
            .country2(Country.NL)
            .acReferenceFlow(-1135.269)
            .dcReferenceFlow(-1135.000)
            .allocatedFlow(977.331)
            .xNodeFlow(-22.500)
            .loopFlow(Country.BE, 14.604)
            .loopFlow(Country.FR, 142.857)
            .loopFlow(Country.NL, 9.375)
            .loopFlow(Country.DE, 13.333)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("FFR1AA1  FFR2AA1  1")
            .country1(Country.FR)
            .country2(Country.FR)
            .acReferenceFlow(1388.870)
            .dcReferenceFlow(1388.333)
            .allocatedFlow(578.926)
            .xNodeFlow(-17.500)
            .internalFlow(785.714)
            .loopFlow(Country.BE, 48.762)
            .loopFlow(Country.NL, -3.125)
            .loopFlow(Country.DE, -4.444)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("FFR1AA1  FFR3AA1  1")
            .country1(Country.FR)
            .country2(Country.FR)
            .acReferenceFlow(-388.870)
            .dcReferenceFlow(-388.333)
            .allocatedFlow(264.581)
            .xNodeFlow(-42.500)
            .internalFlow(71.429)
            .loopFlow(Country.BE, 102.393)
            .loopFlow(Country.NL, -3.125)
            .loopFlow(Country.DE, -4.444)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("FFR2AA1  DDE3AA1  1")
            .country1(Country.FR)
            .country2(Country.DE)
            .acReferenceFlow(1664.731)
            .dcReferenceFlow(1665.000)
            .allocatedFlow(1736.778)
            .xNodeFlow(-52.500)
            .loopFlow(Country.BE, 146.287)
            .loopFlow(Country.FR, -142.857)
            .loopFlow(Country.NL, -9.375)
            .loopFlow(Country.DE, -13.333)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("FFR2AA1  FFR3AA1  1")
            .country1(Country.FR)
            .country2(Country.FR)
            .acReferenceFlow(-1775.862)
            .dcReferenceFlow(-1776.667)
            .allocatedFlow(843.508)
            .xNodeFlow(-60.000)
            .internalFlow(857.143)
            .loopFlow(Country.BE, 151.155)
            .loopFlow(Country.NL, -6.250)
            .loopFlow(Country.DE, -8.889)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("NNL1AA1  NNL2AA1  1")
            .country1(Country.NL)
            .country2(Country.NL)
            .acReferenceFlow(-211.847)
            .dcReferenceFlow(-211.667)
            .allocatedFlow(505.480)
            .xNodeFlow(-32.500)
            .internalFlow(-371.875)
            .loopFlow(Country.BE, 58.498)
            .loopFlow(Country.FR, 47.619)
            .loopFlow(Country.DE, 4.444)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("NNL1AA1  NNL3AA1  1")
            .country1(Country.NL)
            .country2(Country.NL)
            .acReferenceFlow(711.847)
            .dcReferenceFlow(711.667)
            .allocatedFlow(346.610)
            .xNodeFlow(-7.500)
            .internalFlow(315.625)
            .loopFlow(Country.BE, 4.868)
            .loopFlow(Country.FR, 47.619)
            .loopFlow(Country.DE, 4.444)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("NNL2AA1  BBE3AA1  1")
            .country1(Country.NL)
            .country2(Country.BE)
            .acReferenceFlow(-1635.269)
            .dcReferenceFlow(-1635.000)
            .allocatedFlow(1391.439)
            .xNodeFlow(-97.500)
            .loopFlow(Country.BE, 175.495)
            .loopFlow(Country.FR, 142.857)
            .loopFlow(Country.NL, 9.375)
            .loopFlow(Country.DE, 13.333)
            .validate();
        assertThat(flowDecompositionResults)
            .branchId("NNL2AA1  NNL3AA1  1")
            .country1(Country.NL)
            .country2(Country.NL)
            .acReferenceFlow(923.422)
            .dcReferenceFlow(923.333)
            .allocatedFlow(852.090)
            .xNodeFlow(-40.000)
            .internalFlow(-56.250)
            .loopFlow(Country.BE, 63.366)
            .loopFlow(Country.FR, 95.238)
            .loopFlow(Country.DE, 8.889)
            .validate();
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
}
