/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.flow_decomposition;

import com.powsybl.commons.PowsyblException;
import com.powsybl.flow_decomposition.glsk_provider.AutoGlskProvider;
import com.powsybl.flow_decomposition.rescaler.*;
import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.TwoSides;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.sensitivity.SensitivityAnalysis;
import com.powsybl.sensitivity.SensitivityVariableType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 * @author Caio Luke {@literal <caio.luke at artelys.com>}
 */
public class FlowDecompositionComputer {

    private static final Logger LOGGER = LoggerFactory.getLogger(FlowDecompositionComputer.class);
    static final String DEFAULT_LOAD_FLOW_PROVIDER = "OpenLoadFlow";
    static final String DEFAULT_SENSITIVITY_ANALYSIS_PROVIDER = "OpenLoadFlow";
    public static final LoadFlowParameters.ConnectedComponentMode MAIN_CONNECTED_COMPONENT = LoadFlowParameters.ConnectedComponentMode.MAIN;
    private final LoadFlowParameters loadFlowParameters;
    private final FlowDecompositionParameters parameters;
    private final LoadFlowRunningService loadFlowRunningService;
    private final SensitivityAnalysis.Runner sensitivityAnalysisRunner;
    private final LossesCompensator lossesCompensator;
    private final DecomposedFlowRescaler decomposedFlowRescaler;
    private final FlowDecompositionObserverList observers;

    public FlowDecompositionComputer() {
        this(new FlowDecompositionParameters());
    }

    public FlowDecompositionComputer(FlowDecompositionParameters flowDecompositionParameters,
                                     LoadFlowParameters loadFlowParameters,
                                     String loadFlowProvider, String sensitivityAnalysisProvider) {
        this.parameters = flowDecompositionParameters;
        this.loadFlowParameters = loadFlowParameters.copy();
        if (!MAIN_CONNECTED_COMPONENT.equals(this.loadFlowParameters.getConnectedComponentMode())) {
            LOGGER.warn("Flow decomposition is currently available only on the main synchronous component. Changing connected component mode from {} to MAIN.",
                    this.loadFlowParameters.getConnectedComponentMode());
            this.loadFlowParameters.setConnectedComponentMode(MAIN_CONNECTED_COMPONENT);
        }
        this.loadFlowRunningService = new LoadFlowRunningService(LoadFlow.find(loadFlowProvider));
        this.sensitivityAnalysisRunner = SensitivityAnalysis.find(sensitivityAnalysisProvider);
        this.lossesCompensator = parameters.isLossesCompensationEnabled() ? new LossesCompensator(parameters) : null;
        this.decomposedFlowRescaler = getDecomposedFlowRescaler();
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
        // AC load flow
        saveAcLoadFlowResults(flowDecompositionResultsBuilder, network, xnecList, loadFlowServiceAcResult);

        // Losses compensation
        compensateLosses(network);

        // DC load flow
        LoadFlowRunningService.Result loadFlowServiceDcResult = runDcLoadFlow(network);
        saveDcLoadFlowResults(flowDecompositionResultsBuilder, network, xnecList, loadFlowServiceDcResult);

        // Nodal injections
        NetworkMatrixIndexes networkMatrixIndexes = new NetworkMatrixIndexes(network, new ArrayList<>(xnecList));
        SparseMatrixWithIndexesTriplet nodalInjectionsMatrix = getNodalInjectionsMatrix(network, netPositions,
            networkMatrixIndexes, glsks);

        // Sensitivity
        SensitivityAnalyser sensitivityAnalyser = getSensitivityAnalyser(network, networkMatrixIndexes);
        SparseMatrixWithIndexesTriplet ptdfMatrix = getPtdfMatrix(networkMatrixIndexes, sensitivityAnalyser);
        SparseMatrixWithIndexesTriplet psdfMatrix = getPsdfMatrix(networkMatrixIndexes, sensitivityAnalyser);

        // Flows
        computeAllocatedAndLoopFlows(flowDecompositionResultsBuilder, nodalInjectionsMatrix, ptdfMatrix);
        computePstFlows(network, flowDecompositionResultsBuilder, networkMatrixIndexes, psdfMatrix);

        // Add the observes to keep the decomposed flows before rescaling
        flowDecompositionResultsBuilder.addObserversList(observers);
        flowDecompositionResultsBuilder.build(decomposedFlowRescaler, network);
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

    private void saveAcLoadFlowResults(FlowDecompositionResults.PerStateBuilder flowDecompositionResultsBuilder, Network network, Set<Branch> xnecList, LoadFlowRunningService.Result loadFlowServiceAcResult) {
        saveAcReferenceFlows(flowDecompositionResultsBuilder, xnecList, loadFlowServiceAcResult.fallbackHasBeenActivated());
        saveAcCurrents(flowDecompositionResultsBuilder, xnecList, loadFlowServiceAcResult.fallbackHasBeenActivated());
        observers.computedAcLoadFlowResults(network, loadFlowServiceAcResult);
    }

    private void saveAcReferenceFlows(FlowDecompositionResults.PerStateBuilder flowDecompositionResultsBuilder, Set<Branch> xnecList, boolean fallbackHasBeenActivated) {
        Map<String, Double> acTerminal1ReferenceFlows = FlowComputerUtils.calculateAcTerminalReferenceFlows(xnecList, fallbackHasBeenActivated, TwoSides.ONE);
        Map<String, Double> acTerminal2ReferenceFlows = FlowComputerUtils.calculateAcTerminalReferenceFlows(xnecList, fallbackHasBeenActivated, TwoSides.TWO);
        flowDecompositionResultsBuilder.saveAcTerminal1ReferenceFlow(acTerminal1ReferenceFlows);
        flowDecompositionResultsBuilder.saveAcTerminal2ReferenceFlow(acTerminal2ReferenceFlows);
    }

    private void saveAcCurrents(FlowDecompositionResults.PerStateBuilder flowDecompositionResultBuilder, Set<Branch> xnecList, boolean fallbackHasBeenActivated) {
        Map<String, Double> acTerminal1Currents = FlowComputerUtils.calculateAcTerminalCurrents(xnecList, fallbackHasBeenActivated, TwoSides.ONE);
        Map<String, Double> acTerminal2Currents = FlowComputerUtils.calculateAcTerminalCurrents(xnecList, fallbackHasBeenActivated, TwoSides.TWO);
        flowDecompositionResultBuilder.saveAcCurrentTerminal1(acTerminal1Currents);
        flowDecompositionResultBuilder.saveAcCurrentTerminal2(acTerminal2Currents);
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

    private DecomposedFlowRescaler getDecomposedFlowRescaler() {
        return switch (parameters.getRescaleMode()) {
            case NONE -> new DecomposedFlowRescalerNoOp();
            case ACER_METHODOLOGY -> new DecomposedFlowRescalerAcerMethodology();
            case PROPORTIONAL -> new DecomposedFlowRescalerProportional(parameters.getProportionalRescalerMinFlowTolerance());
            case MAX_CURRENT_OVERLOAD -> new DecomposedFlowRescalerMaxCurrentOverload(parameters.getProportionalRescalerMinFlowTolerance());
            default -> throw new PowsyblException("DecomposedFlowRescaler not defined for mode: " + parameters.getRescaleMode());
        };
    }

    private LoadFlowRunningService.Result runDcLoadFlow(Network network) {
        return loadFlowRunningService.runDcLoadflow(network, loadFlowParameters);
    }

    private SparseMatrixWithIndexesTriplet getNodalInjectionsMatrix(Network network,
                                                                    Map<Country, Double> netPositions,
                                                                    NetworkMatrixIndexes networkMatrixIndexes,
                                                                    Map<Country, Map<String, Double>> glsks) {
        NodalInjectionComputer nodalInjectionComputer = new NodalInjectionComputer(networkMatrixIndexes);
        SparseMatrixWithIndexesTriplet nodalInjectionsMatrix = nodalInjectionComputer.run(network, glsks, netPositions);
        observers.computedNodalInjectionsMatrix(nodalInjectionsMatrix);
        return nodalInjectionsMatrix;
    }

    private void saveDcLoadFlowResults(FlowDecompositionResults.PerStateBuilder flowDecompositionResultBuilder, Network network, Set<Branch> xnecList, LoadFlowRunningService.Result loadFlowServiceDcResult) {
        flowDecompositionResultBuilder.saveDcReferenceFlow(FlowComputerUtils.getTerminalReferenceFlow(xnecList, TwoSides.ONE));
        observers.computedDcLoadFlowResults(network, loadFlowServiceDcResult);
    }

    private SensitivityAnalyser getSensitivityAnalyser(Network network, NetworkMatrixIndexes networkMatrixIndexes) {
        return new SensitivityAnalyser(loadFlowParameters, parameters, sensitivityAnalysisRunner, network, networkMatrixIndexes);
    }

    private SparseMatrixWithIndexesTriplet getPtdfMatrix(NetworkMatrixIndexes networkMatrixIndexes,
                                                         SensitivityAnalyser sensitivityAnalyser) {
        SparseMatrixWithIndexesTriplet ptdfMatrix = sensitivityAnalyser.run(networkMatrixIndexes.getNodeIdList(),
            networkMatrixIndexes.getNodeIndex(),
            SensitivityVariableType.INJECTION_ACTIVE_POWER);
        observers.computedPtdfMatrix(ptdfMatrix);
        return ptdfMatrix;
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
        SparseMatrixWithIndexesTriplet psdfMatrix = sensitivityAnalyser.run(networkMatrixIndexes.getPstList(),
            networkMatrixIndexes.getPstIndex(), SensitivityVariableType.TRANSFORMER_PHASE);
        observers.computedPsdfMatrix(psdfMatrix);
        return psdfMatrix;
    }

    private void computePstFlows(Network network,
                                 FlowDecompositionResults.PerStateBuilder flowDecompositionResultBuilder,
                                 NetworkMatrixIndexes networkMatrixIndexes,
                                 SparseMatrixWithIndexesTriplet psdfMatrix) {
        PstFlowComputer pstFlowComputer = new PstFlowComputer();
        SparseMatrixWithIndexesCSC pstFlowMatrix = pstFlowComputer.run(network, networkMatrixIndexes, psdfMatrix);
        flowDecompositionResultBuilder.savePstFlowMatrix(pstFlowMatrix);
    }

    protected LoadFlowParameters getLoadFlowParameters() {
        return this.loadFlowParameters;
    }
}
