/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.flow_decomposition;


import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.sensitivity.SensitivityAnalysis;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

/**
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 */
class SensitivityAnalysisRunnerTests {
    @Test
    void testSensitivityAnalysisRunnerIsDefaultByDefaultInsideAFlowDecompositionComputer() {
        FlowDecompositionComputer flowDecompositionComputer = new FlowDecompositionComputer();
        SensitivityAnalysis.Runner sensitivityAnalysisRunner = flowDecompositionComputer.getSensitivityAnalysisRunner();
        SensitivityAnalysis.Runner defaultSensitivityRunner = SensitivityAnalysis.find();
        assertEquals(defaultSensitivityRunner.getName(), sensitivityAnalysisRunner.getName());
        assertEquals(defaultSensitivityRunner.getVersion(), sensitivityAnalysisRunner.getVersion());
    }

    @Test
    void testNodalSensitivityAnalysisUseRunner() {
        FlowDecompositionComputer flowDecompositionComputer = new FlowDecompositionComputer();
        Network mockNetwork = mock(Network.class);
        NetworkMatrixIndexes mockNetworkMatrixIndexes = mock(NetworkMatrixIndexes.class);
        SensitivityAnalyser nodalSensitivityAnalyser = flowDecompositionComputer.getSensitivityAnalyser(mockNetwork, mockNetworkMatrixIndexes);
        SensitivityAnalysis.Runner nodalSensitivityAnalyserRunner = nodalSensitivityAnalyser.runner;
        SensitivityAnalysis.Runner sensitivityAnalysisRunner = flowDecompositionComputer.getSensitivityAnalysisRunner();
        assertEquals(sensitivityAnalysisRunner.getName(), nodalSensitivityAnalyserRunner.getName());
        assertEquals(sensitivityAnalysisRunner.getVersion(), nodalSensitivityAnalyserRunner.getVersion());
    }

    @Test
    void testZonalSensitivityAnalysisUseRunner() {
        FlowDecompositionComputer flowDecompositionComputer = new FlowDecompositionComputer();
        ZonalSensitivityAnalyser zonalSensitivityAnalyser = new ZonalSensitivityAnalyser(new LoadFlowParameters(), flowDecompositionComputer.getSensitivityAnalysisRunner());
        SensitivityAnalysis.Runner nodalSensitivityAnalyserRunner = zonalSensitivityAnalyser.runner;
        SensitivityAnalysis.Runner sensitivityAnalysisRunner = flowDecompositionComputer.getSensitivityAnalysisRunner();
        assertEquals(sensitivityAnalysisRunner.getName(), nodalSensitivityAnalyserRunner.getName());
        assertEquals(sensitivityAnalysisRunner.getVersion(), nodalSensitivityAnalyserRunner.getVersion());
    }

}
