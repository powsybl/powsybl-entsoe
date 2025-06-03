/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.flow_decomposition.partitioners;

import com.powsybl.flow_decomposition.FlowDecompositionParameters;
import com.powsybl.flow_decomposition.ZonalSensitivityAnalyser;
import com.powsybl.flow_decomposition.glsk_provider.AutoGlskProvider;
import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.sensitivity.SensitivityAnalysis;
import com.powsybl.sensitivity.SensitivityVariableType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.powsybl.flow_decomposition.partitioners.SensitivityAnalyser.respectFlowSignConvention;
import static com.powsybl.flow_decomposition.TestUtils.importNetwork;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 */
class SensitivityComputerTests {
    private static final double EPSILON = 1e-3;

    @Test
    void testThatNodalPtdfAreWellComputed() {
        String networkFileName = "NETWORK_SINGLE_LOAD_TWO_GENERATORS_WITH_COUNTRIES.uct";
        String genBe = "BGEN2 11_generator";
        String loadBe = "BLOAD 11_load";
        String genFr = "FGEN1 11_generator";
        String xnecFrBe = "FGEN1 11 BLOAD 11 1";
        String xnecBeBe = "BLOAD 11 BGEN2 11 1";
        Network network = importNetwork(networkFileName);
        LoadFlowParameters loadFlowParameters = LoadFlowParameters.load();
        FlowDecompositionParameters parameters = FlowDecompositionParameters.load();
        SensitivityAnalysis.Runner sensitivityAnalysisRunner = SensitivityAnalysis.find();
        List<Branch> xnecList = network.getBranchStream().collect(Collectors.toList());
        NetworkMatrixIndexes networkMatrixIndexes = new NetworkMatrixIndexes(network, xnecList);
        SensitivityAnalyser sensitivityAnalyser = new SensitivityAnalyser(loadFlowParameters, parameters, sensitivityAnalysisRunner, network, networkMatrixIndexes);
        SparseMatrixWithIndexesTriplet ptdfMatrix =
            sensitivityAnalyser.run(networkMatrixIndexes.getNodeIdList(),
                networkMatrixIndexes.getNodeIndex(),
                SensitivityVariableType.INJECTION_ACTIVE_POWER);
        Map<String, Map<String, Double>> nodalPtdfs = ptdfMatrix.toMap();
        assertEquals(-0.5, nodalPtdfs.get(xnecFrBe).get(loadBe), EPSILON);
        assertEquals(-0.5, nodalPtdfs.get(xnecFrBe).get(genBe), EPSILON);
        assertEquals(+0.5, nodalPtdfs.get(xnecFrBe).get(genFr), EPSILON);
        assertEquals(-0.5, nodalPtdfs.get(xnecBeBe).get(loadBe), EPSILON);
        assertEquals(+0.5, nodalPtdfs.get(xnecBeBe).get(genBe), EPSILON);
        assertEquals(-0.5, nodalPtdfs.get(xnecBeBe).get(genFr), EPSILON);
    }

    @Test
    void testThatZonalPtdfAreWellComputed() {
        String networkFileName = "NETWORK_PARALLEL_LINES_PTDF.uct";
        Network network = importNetwork(networkFileName);
        String lineFrBe = "FLOAD 11 BLOAD 11 1";
        String lineBeFr = "FGEN  11 BLOAD 11 1";
        String line1 = "FGEN  11 FLOAD 11 1";
        String line2 = "FGEN  11 FLOAD 11 2";
        String line3 = "FGEN  11 FLOAD 11 3";
        String line4 = "FGEN  11 FLOAD 11 4";
        String line5 = "FGEN  11 FLOAD 11 5";
        String line6 = "FGEN  11 FLOAD 11 6";
        String line7 = "FGEN  11 FLOAD 11 7";
        String line8 = "FGEN  11 FLOAD 11 8";
        String line9 = "FGEN  11 FLOAD 11 9";
        AutoGlskProvider glskProvider = new AutoGlskProvider();
        Map<Country, Map<String, Double>> glsks = glskProvider.getGlsk(network);
        LoadFlowParameters loadFlowParameters = LoadFlowParameters.load();
        SensitivityAnalysis.Runner sensitivityAnalysisRunner = SensitivityAnalysis.find();
        ZonalSensitivityAnalyser zonalSensitivityAnalyser = new ZonalSensitivityAnalyser(loadFlowParameters, sensitivityAnalysisRunner);
        Map<String, Map<Country, Double>> zonalPtdfs = zonalSensitivityAnalyser.run(network,
            glsks, SensitivityVariableType.INJECTION_ACTIVE_POWER);
        assertEquals(-0.025, zonalPtdfs.get(line1).get(Country.BE), EPSILON);
        assertEquals(+0.025, zonalPtdfs.get(line1).get(Country.FR), EPSILON);
        assertEquals(-0.025, zonalPtdfs.get(line2).get(Country.BE), EPSILON);
        assertEquals(+0.025, zonalPtdfs.get(line2).get(Country.FR), EPSILON);
        assertEquals(-0.025, zonalPtdfs.get(line3).get(Country.BE), EPSILON);
        assertEquals(+0.025, zonalPtdfs.get(line3).get(Country.FR), EPSILON);
        assertEquals(-0.025, zonalPtdfs.get(line4).get(Country.BE), EPSILON);
        assertEquals(+0.025, zonalPtdfs.get(line4).get(Country.FR), EPSILON);
        assertEquals(-0.025, zonalPtdfs.get(line5).get(Country.BE), EPSILON);
        assertEquals(+0.025, zonalPtdfs.get(line5).get(Country.FR), EPSILON);
        assertEquals(-0.025, zonalPtdfs.get(line6).get(Country.BE), EPSILON);
        assertEquals(+0.025, zonalPtdfs.get(line6).get(Country.FR), EPSILON);
        assertEquals(-0.025, zonalPtdfs.get(line7).get(Country.BE), EPSILON);
        assertEquals(+0.025, zonalPtdfs.get(line7).get(Country.FR), EPSILON);
        assertEquals(-0.025, zonalPtdfs.get(line8).get(Country.BE), EPSILON);
        assertEquals(+0.025, zonalPtdfs.get(line8).get(Country.FR), EPSILON);
        assertEquals(-0.025, zonalPtdfs.get(line9).get(Country.BE), EPSILON);
        assertEquals(+0.025, zonalPtdfs.get(line9).get(Country.FR), EPSILON);
        assertEquals(-0.225, zonalPtdfs.get(lineFrBe).get(Country.BE), EPSILON);
        assertEquals(+0.225, zonalPtdfs.get(lineFrBe).get(Country.FR), EPSILON);
        assertEquals(-0.275, zonalPtdfs.get(lineBeFr).get(Country.BE), EPSILON);
        assertEquals(+0.275, zonalPtdfs.get(lineBeFr).get(Country.FR), EPSILON);
    }

    @Test
    void testThatPsdfAreWellComputed() {
        String networkFileName = "NETWORK_PST_FLOW_WITH_COUNTRIES.uct";
        Network network = importNetwork(networkFileName);
        String pst = "BLOAD 11 BLOAD 12 2";
        String x1 = "FGEN  11 BLOAD 11 1";
        String x2 = "FGEN  11 BLOAD 12 1";
        LoadFlowParameters loadFlowParameters = LoadFlowParameters.load();
        FlowDecompositionParameters parameters = FlowDecompositionParameters.load();
        SensitivityAnalysis.Runner sensitivityAnalysisRunner = SensitivityAnalysis.find();
        List<Branch> xnecList = network.getBranchStream().collect(Collectors.toList());
        NetworkMatrixIndexes networkMatrixIndexes = new NetworkMatrixIndexes(network, xnecList);
        SensitivityAnalyser sensitivityAnalyser = new SensitivityAnalyser(loadFlowParameters, parameters, sensitivityAnalysisRunner, network, networkMatrixIndexes);
        SparseMatrixWithIndexesTriplet psdfMatrix = sensitivityAnalyser.run(networkMatrixIndexes.getPstList(),
            networkMatrixIndexes.getPstIndex(), SensitivityVariableType.TRANSFORMER_PHASE);
        Map<String, Map<String, Double>> psdf = psdfMatrix.toMap();
        assertEquals(-420.042573, psdf.get(x1).get(pst), EPSILON);
        assertEquals(420.042573, psdf.get(x2).get(pst), EPSILON);
    }

    @Test
    void testRespectFlowSignConventionIsAnInvolution() {
        assertEquals(0.5, applyRespectFlowSignConventionTwice(0.5, 1.0));
        assertEquals(0.5, applyRespectFlowSignConventionTwice(0.5, 0.0));
        assertEquals(0.5, applyRespectFlowSignConventionTwice(0.5, -1.0));
        assertEquals(-0.5, applyRespectFlowSignConventionTwice(-0.5, 1.0));
        assertEquals(-0.5, applyRespectFlowSignConventionTwice(-0.5, 0.0));
        assertEquals(-0.5, applyRespectFlowSignConventionTwice(-0.5, -1.0));
    }

    private double applyRespectFlowSignConventionTwice(double ptdfValue, double referenceFlow) {
        return respectFlowSignConvention(respectFlowSignConvention(ptdfValue, referenceFlow), referenceFlow);
    }
}
