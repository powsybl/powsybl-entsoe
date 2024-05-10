/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.flow_decomposition;

import com.powsybl.flow_decomposition.glsk_provider.AutoGlskProvider;
import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.sensitivity.SensitivityAnalysis;
import com.powsybl.sensitivity.SensitivityVariableType;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

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
    private final LossesCompensator lossesCompensator;

    private final FlowDecompositionObserverList observers;

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
        this.lossesCompensator = parameters.isLossesCompensationEnabled() ? new LossesCompensator(parameters) : null;
        this.observers = new FlowDecompositionObserverList();
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
        return run(xnecProvider, new AutoGlskProvider(), network);
    }

    public FlowDecompositionResults run(XnecProvider xnecProvider, GlskProvider glskProvider, Network network) {
        observers.runStart();
        try {
            NetworkStateManager networkStateManager = new NetworkStateManager(network, xnecProvider);

            LoadFlowRunningService.Result loadFlowServiceAcResult = runAcLoadFlow(network);

            Map<Country, Map<String, Double>> glsks = glskProvider.getGlsk(network);
            observers.computedGlsk(glsks);

            Map<Country, Double> netPositions = getZonesNetPosition(network);
            observers.computedNetPositions(netPositions);

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
            networkStateManager.deleteAllContingencyVariants();
            return flowDecompositionResults;
        } finally {
            observers.runDone();
        }
    }

    private void decomposeFlowForNState(Network network,
                                        FlowDecompositionResults flowDecompositionResults,
                                        Set<Branch> xnecList,
                                        Map<Country, Double> netPositions,
                                        Map<Country, Map<String, Double>> glsks,
                                        LoadFlowRunningService.Result loadFlowServiceAcResult) {
        if (!xnecList.isEmpty()) {
            observers.computingBaseCase();
            FlowDecompositionResults.PerStateBuilder flowDecompositionResultsBuilder = flowDecompositionResults.getBuilder(xnecList);
            decomposeFlowForState(network, xnecList, flowDecompositionResultsBuilder, netPositions, glsks, loadFlowServiceAcResult);
        }
    }

    private void decomposeFlowForContingencyState(Network network,
                                                  FlowDecompositionResults flowDecompositionResults,
                                                  NetworkStateManager networkStateManager,
                                                  String contingencyId,
                                                  Set<Branch> xnecList,
                                                  Map<Country, Double> netPositions,
                                                  Map<Country, Map<String, Double>> glsks) {
        if (!xnecList.isEmpty()) {
            observers.computingContingency(contingencyId);
            networkStateManager.setNetworkVariant(contingencyId);
            LoadFlowRunningService.Result loadFlowServiceAcResult = runAcLoadFlow(network);
            FlowDecompositionResults.PerStateBuilder flowDecompositionResultsBuilder = flowDecompositionResults.getBuilder(contingencyId, xnecList);
            decomposeFlowForState(network, xnecList, flowDecompositionResultsBuilder, netPositions, glsks, loadFlowServiceAcResult);
        }
    }

    private void decomposeFlowForState(Network network,
                                       Set<Branch> xnecList,
                                       FlowDecompositionResults.PerStateBuilder flowDecompositionResultsBuilder,
                                       Map<Country, Double> netPositions,
                                       Map<Country, Map<String, Double>> glsks,
                                       LoadFlowRunningService.Result loadFlowServiceAcResult) {
        saveAcReferenceFlow(flowDecompositionResultsBuilder, xnecList, loadFlowServiceAcResult);
        saveAcMaxFlow(flowDecompositionResultsBuilder, xnecList, loadFlowServiceAcResult);
        compensateLosses(network);
        observers.computedAcFlows(network, loadFlowServiceAcResult);

        // None
        NetworkMatrixIndexes networkMatrixIndexes = new NetworkMatrixIndexes(network, new ArrayList<>(xnecList));
        observers.computedAcNodalInjections(networkMatrixIndexes, loadFlowServiceAcResult.fallbackHasBeenActivated());

        runDcLoadFlow(network);
        observers.computedDcFlows(network);
        observers.computedDcNodalInjections(networkMatrixIndexes);

        SparseMatrixWithIndexesTriplet nodalInjectionsMatrix = getNodalInjectionsMatrix(network,
            netPositions, networkMatrixIndexes, glsks);
        saveDcReferenceFlow(flowDecompositionResultsBuilder, xnecList);
        observers.computedNodalInjectionsMatrix(nodalInjectionsMatrix);

        // DC Sensi
        SensitivityAnalyser sensitivityAnalyser = getSensitivityAnalyser(network, networkMatrixIndexes);
        SparseMatrixWithIndexesTriplet ptdfMatrix = getPtdfMatrix(networkMatrixIndexes, sensitivityAnalyser);
        observers.computedPtdfMatrix(ptdfMatrix);
        SparseMatrixWithIndexesTriplet psdfMatrix = getPsdfMatrix(networkMatrixIndexes, sensitivityAnalyser);
        observers.computedPsdfMatrix(psdfMatrix);

        // None
        computeAllocatedAndLoopFlows(flowDecompositionResultsBuilder, nodalInjectionsMatrix, ptdfMatrix);
        computePstFlows(network, flowDecompositionResultsBuilder, networkMatrixIndexes, psdfMatrix);

        flowDecompositionResultsBuilder.build(parameters.getRescaleMode());
    }

    public void addObserver(FlowDecompositionObserver observer) {
        this.observers.addObserver(observer);
    }

    public void removeObserver(FlowDecompositionObserver observer) {
        this.observers.removeObserver(observer);
    }

    private LoadFlowRunningService.Result runAcLoadFlow(Network network) {
        return loadFlowRunningService.runAcLoadflow(network, loadFlowParameters, parameters.isDcFallbackEnabledAfterAcDivergence());
    }

    private void saveAcReferenceFlow(FlowDecompositionResults.PerStateBuilder flowDecompositionResultBuilder, Set<Branch> xnecList, LoadFlowRunningService.Result loadFlowServiceAcResult) {
        Map<String, Double> acReferenceFlows = FlowComputerUtils.calculateAcReferenceFlows(xnecList, loadFlowServiceAcResult);
        flowDecompositionResultBuilder.saveAcReferenceFlow(acReferenceFlows);
    }

    private void saveAcMaxFlow(FlowDecompositionResults.PerStateBuilder flowDecompositionResultBuilder, Set<Branch> xnecList, LoadFlowRunningService.Result loadFlowServiceAcResult) {
        Map<String, Double> acMaxFlow = FlowComputerUtils.calculateAcMaxFlows(xnecList, loadFlowServiceAcResult);
        flowDecompositionResultBuilder.saveAcMaxFlow(acMaxFlow);
    }

    private Map<Country, Double> getZonesNetPosition(Network network) {
        NetPositionComputer netPositionComputer = new NetPositionComputer();
        return netPositionComputer.run(network);
    }

    private void compensateLosses(Network network) {
        if (parameters.isLossesCompensationEnabled()) {
            lossesCompensator.run(network);
        }
    }

    private LoadFlowRunningService.Result runDcLoadFlow(Network network) {
        return loadFlowRunningService.runDcLoadflow(network, loadFlowParameters, parameters.getEnableSlackCompensationBeforeDcLf());
    }

    private SparseMatrixWithIndexesTriplet getNodalInjectionsMatrix(Network network,
                                                                    Map<Country, Double> netPositions,
                                                                    NetworkMatrixIndexes networkMatrixIndexes,
                                                                    Map<Country, Map<String, Double>> glsks) {
        NodalInjectionComputer nodalInjectionComputer = new NodalInjectionComputer(networkMatrixIndexes);
        return nodalInjectionComputer.run(network, glsks, netPositions);
    }

    private void saveDcReferenceFlow(FlowDecompositionResults.PerStateBuilder flowDecompositionResultBuilder, Set<Branch> xnecList) {
        flowDecompositionResultBuilder.saveDcReferenceFlow(FlowComputerUtils.getReferenceFlow(xnecList));
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
