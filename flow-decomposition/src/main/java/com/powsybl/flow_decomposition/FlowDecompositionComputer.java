/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.flow_decomposition;

import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Identifiable;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.sensitivity.SensitivityAnalysis;
import com.powsybl.sensitivity.SensitivityVariableType;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 */
public class FlowDecompositionComputer {
    static final String DEFAULT_LOAD_FLOW_PROVIDER = null;
    static final String DEFAULT_SENSITIVITY_ANALYSIS_PROVIDER = null;
    private final LoadFlowParameters loadFlowParameters;
    private final FlowDecompositionParameters parameters;
    private final LoadFlowRunningService loadFlowRunningService;
    private final SensitivityAnalysis.Runner sensitivityAnalysisRunner;

    public FlowDecompositionComputer() {
        this(new FlowDecompositionParameters());
    }

    public FlowDecompositionComputer(FlowDecompositionParameters flowDecompositionParameters,
                                     LoadFlowParameters loadFlowParameters,
                                     String loadFlowProvider, String sensitivityAnalysisProvider) {
        this.parameters = flowDecompositionParameters;
        this.loadFlowParameters = loadFlowParameters;
        this.loadFlowRunningService = new LoadFlowRunningService(LoadFlow.find(loadFlowProvider));
        this.sensitivityAnalysisRunner = SensitivityAnalysis.find(sensitivityAnalysisProvider);
    }

    public FlowDecompositionComputer(FlowDecompositionParameters flowDecompositionParameters,
                                     LoadFlowParameters loadFlowParameters) {
        this(flowDecompositionParameters, loadFlowParameters,
            DEFAULT_LOAD_FLOW_PROVIDER, DEFAULT_SENSITIVITY_ANALYSIS_PROVIDER);
    }

    public FlowDecompositionComputer(FlowDecompositionParameters flowDecompositionParameters) {
        this(flowDecompositionParameters, LoadFlowParameters.load());
    }

    public FlowDecompositionResults run(XnecProvider xnecProvider, Network network) {
        NetworkStateManager networkStateManager = new NetworkStateManager(network, xnecProvider);

        LoadFlowRunningService.Result loadFlowServiceAcResult = runAcLoadFlow(network);

        Map<Country, Map<String, Double>> glsks = getGlsks(network);
        Map<Country, Double> netPositions = getZonesNetPosition(network);

        FlowDecompositionResults flowDecompositionResults = new FlowDecompositionResults(network);
        decomposeFlowForNState(network,
            flowDecompositionResults,
            xnecProvider.getNetworkElements(network),
            netPositions,
            glsks,
            loadFlowServiceAcResult);
        xnecProvider.getNetworkElementsPerContingency(network)
            .forEach((contingencyId, xnecList) -> decomposeFlowForContingencyState(network,
                flowDecompositionResults,
                networkStateManager,
                contingencyId,
                xnecList,
                netPositions,
                glsks));
        return flowDecompositionResults;
    }

    private void decomposeFlowForNState(Network network,
                                        FlowDecompositionResults flowDecompositionResults,
                                        List<Branch> xnecList,
                                        Map<Country, Double> netPositions,
                                        Map<Country, Map<String, Double>> glsks,
                                        LoadFlowRunningService.Result loadFlowServiceAcResult) {
        if (!xnecList.isEmpty()) {
            FlowDecompositionResults.PerStateBuilder flowDecompositionResultsBuilder = flowDecompositionResults.getBuilder(xnecList);
            decomposeFlowForState(network, xnecList, flowDecompositionResultsBuilder, netPositions, glsks, loadFlowServiceAcResult);
        }
    }

    private void decomposeFlowForContingencyState(Network network,
                                                  FlowDecompositionResults flowDecompositionResults,
                                                  NetworkStateManager networkStateManager,
                                                  String contingencyId,
                                                  List<Branch> xnecList,
                                                  Map<Country, Double> netPositions,
                                                  Map<Country, Map<String, Double>> glsks) {
        if (!xnecList.isEmpty()) {
            networkStateManager.setNetworkVariant(contingencyId);
            LoadFlowRunningService.Result loadFlowServiceAcResult = runAcLoadFlow(network);
            FlowDecompositionResults.PerStateBuilder flowDecompositionResultsBuilder = flowDecompositionResults.getBuilder(contingencyId, xnecList);
            decomposeFlowForState(network, xnecList, flowDecompositionResultsBuilder, netPositions, glsks, loadFlowServiceAcResult);
        }
    }

    private void decomposeFlowForState(Network network,
                                       List<Branch> xnecList,
                                       FlowDecompositionResults.PerStateBuilder flowDecompositionResultsBuilder,
                                       Map<Country, Double> netPositions,
                                       Map<Country, Map<String, Double>> glsks,
                                       LoadFlowRunningService.Result loadFlowServiceAcResult) {
        saveAcReferenceFlow(flowDecompositionResultsBuilder, xnecList, loadFlowServiceAcResult);
        compensateLosses(network);

        // None
        NetworkMatrixIndexes networkMatrixIndexes = new NetworkMatrixIndexes(network, xnecList);

        runDcLoadFlow(network);

        SparseMatrixWithIndexesTriplet nodalInjectionsMatrix = getNodalInjectionsMatrix(network,
            netPositions, networkMatrixIndexes, glsks);
        saveDcReferenceFlow(flowDecompositionResultsBuilder, xnecList);

        // DC Sensi
        SensitivityAnalyser sensitivityAnalyser = getSensitivityAnalyser(network, networkMatrixIndexes);
        SparseMatrixWithIndexesTriplet ptdfMatrix = getPtdfMatrix(networkMatrixIndexes, sensitivityAnalyser);
        SparseMatrixWithIndexesTriplet psdfMatrix = getPsdfMatrix(networkMatrixIndexes, sensitivityAnalyser);

        // None
        computeAllocatedAndLoopFlows(flowDecompositionResultsBuilder, nodalInjectionsMatrix, ptdfMatrix);
        computePstFlows(network, flowDecompositionResultsBuilder, networkMatrixIndexes, psdfMatrix);

        flowDecompositionResultsBuilder.build(parameters.isRescaleEnabled());
    }

    private LoadFlowRunningService.Result runAcLoadFlow(Network network) {
        return loadFlowRunningService.runAcLoadflow(network, loadFlowParameters, parameters.isDcFallbackEnabledAfterAcDivergence());
    }

    private void saveAcReferenceFlow(FlowDecompositionResults.PerStateBuilder flowDecompositionResultBuilder, List<Branch> xnecList, LoadFlowRunningService.Result loadFlowServiceAcResult) {
        Map<String, Double> acReferenceFlows;
        if (loadFlowServiceAcResult.fallbackHasBeenActivated()) {
            acReferenceFlows = xnecList.stream().collect(Collectors.toMap(Identifiable::getId, branch -> Double.NaN));
        } else {
            acReferenceFlows = getXnecReferenceFlows(xnecList);
        }
        flowDecompositionResultBuilder.saveAcReferenceFlow(acReferenceFlows);
    }

    private Map<Country, Map<String, Double>> getGlsks(Network network) {
        GlskComputer glskComputer = new GlskComputer();
        return glskComputer.run(network);
    }

    private Map<Country, Double> getZonesNetPosition(Network network) {
        NetPositionComputer netPositionComputer = new NetPositionComputer();
        return netPositionComputer.run(network);
    }

    private Map<String, Double> getXnecReferenceFlows(List<Branch> xnecList) {
        ReferenceFlowComputer referenceFlowComputer = new ReferenceFlowComputer();
        return referenceFlowComputer.run(xnecList);
    }

    private void compensateLosses(Network network) {
        if (parameters.isLossesCompensationEnabled()) {
            LossesCompensator lossesCompensator = new LossesCompensator(parameters);
            lossesCompensator.compensateLossesOnBranches(network);
        }
    }

    private LoadFlowRunningService.Result runDcLoadFlow(Network network) {
        return loadFlowRunningService.runDcLoadflow(network, loadFlowParameters);
    }

    private SparseMatrixWithIndexesTriplet getNodalInjectionsMatrix(Network network,
                                                                    Map<Country, Double> netPositions,
                                                                    NetworkMatrixIndexes networkMatrixIndexes,
                                                                    Map<Country, Map<String, Double>> glsks) {
        NodalInjectionComputer nodalInjectionComputer = new NodalInjectionComputer(networkMatrixIndexes);
        Map<String, Double> dcNodalInjection = getDcNodalInjection(networkMatrixIndexes);

        return getNodalInjectionsMatrix(network, netPositions, glsks,
            nodalInjectionComputer, dcNodalInjection);
    }

    private Map<String, Double> getDcNodalInjection(NetworkMatrixIndexes networkMatrixIndexes) {
        ReferenceNodalInjectionComputer referenceNodalInjectionComputer = new ReferenceNodalInjectionComputer(networkMatrixIndexes);
        return referenceNodalInjectionComputer.run();
    }

    private SparseMatrixWithIndexesTriplet getNodalInjectionsMatrix(Network network,
                                                                    Map<Country, Double> netPositions,
                                                                    Map<Country, Map<String, Double>> glsks,
                                                                    NodalInjectionComputer nodalInjectionComputer,
                                                                    Map<String, Double> dcNodalInjection) {
        return nodalInjectionComputer.run(network, glsks, netPositions, dcNodalInjection);
    }

    private void saveDcReferenceFlow(FlowDecompositionResults.PerStateBuilder flowDecompositionResultBuilder, List<Branch> xnecList) {
        flowDecompositionResultBuilder.saveDcReferenceFlow(getXnecReferenceFlows(xnecList));
    }

    private SensitivityAnalyser getSensitivityAnalyser(Network network, NetworkMatrixIndexes networkMatrixIndexes) {
        return new SensitivityAnalyser(loadFlowParameters, parameters, sensitivityAnalysisRunner, network, networkMatrixIndexes);
    }

    private SparseMatrixWithIndexesTriplet getPtdfMatrix(NetworkMatrixIndexes networkMatrixIndexes,
                                                         SensitivityAnalyser sensitivityAnalyser) {
        return sensitivityAnalyser.run(networkMatrixIndexes.getNodeIdList(),
            networkMatrixIndexes.getNodeIndex(),
            SensitivityVariableType.INJECTION_ACTIVE_POWER);
    }

    private void computeAllocatedAndLoopFlows(FlowDecompositionResults.PerStateBuilder flowDecompositionResultBuilder,
                                              SparseMatrixWithIndexesTriplet nodalInjectionsMatrix,
                                              SparseMatrixWithIndexesTriplet ptdfMatrix) {
        SparseMatrixWithIndexesCSC allocatedLoopFlowsMatrix =
            SparseMatrixWithIndexesCSC.mult(ptdfMatrix.toCSCMatrix(), nodalInjectionsMatrix.toCSCMatrix());
        flowDecompositionResultBuilder.saveAllocatedAndLoopFlowsMatrix(allocatedLoopFlowsMatrix);
    }

    private SparseMatrixWithIndexesTriplet getPsdfMatrix(NetworkMatrixIndexes networkMatrixIndexes,
                                                         SensitivityAnalyser sensitivityAnalyser) {
        return sensitivityAnalyser.run(networkMatrixIndexes.getPstList(),
            networkMatrixIndexes.getPstIndex(), SensitivityVariableType.TRANSFORMER_PHASE);
    }

    private void computePstFlows(Network network,
                                 FlowDecompositionResults.PerStateBuilder flowDecompositionResultBuilder,
                                 NetworkMatrixIndexes networkMatrixIndexes,
                                 SparseMatrixWithIndexesTriplet psdfMatrix) {
        PstFlowComputer pstFlowComputer = new PstFlowComputer();
        SparseMatrixWithIndexesCSC pstFlowMatrix = pstFlowComputer.run(network, networkMatrixIndexes, psdfMatrix);
        flowDecompositionResultBuilder.savePstFlowMatrix(pstFlowMatrix);
    }
}
