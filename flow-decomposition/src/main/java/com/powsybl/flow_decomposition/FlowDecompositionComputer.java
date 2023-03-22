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
import com.powsybl.iidm.network.Identifiable;
import com.powsybl.iidm.network.Injection;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.sensitivity.SensitivityAnalysis;
import com.powsybl.sensitivity.SensitivityVariableType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    private final LossesCompensator lossesCompensator;

    public FlowDecompositionComputer() {
        this(new FlowDecompositionParameters());
    }

    public FlowDecompositionComputer(FlowDecompositionParameters flowDecompositionParameters,
                                     LoadFlowParameters loadFlowParameters,
                                     String loadFlowProvider,
                                     String sensitivityAnalysisProvider) {
        this.parameters = flowDecompositionParameters;
        this.loadFlowParameters = loadFlowParameters;
        this.loadFlowRunningService = new LoadFlowRunningService(LoadFlow.find(loadFlowProvider));
        this.sensitivityAnalysisRunner = SensitivityAnalysis.find(sensitivityAnalysisProvider);
        this.lossesCompensator = parameters.getLossesCompensationMode() == FlowDecompositionParameters.LossCompensationMode.NONE ? null : new LossesCompensator(parameters);
    }

    public FlowDecompositionComputer(FlowDecompositionParameters flowDecompositionParameters, LoadFlowParameters loadFlowParameters) {
        this(flowDecompositionParameters, loadFlowParameters, DEFAULT_LOAD_FLOW_PROVIDER, DEFAULT_SENSITIVITY_ANALYSIS_PROVIDER);
    }

    public FlowDecompositionComputer(FlowDecompositionParameters flowDecompositionParameters) {
        this(flowDecompositionParameters, LoadFlowParameters.load());
    }

    public FlowDecompositionResults run(XnecProvider xnecProvider, Network network) {
        return run(xnecProvider, new AutoGlskProvider(), network);
    }

    public FlowDecompositionResults run(XnecProvider xnecProvider, GlskProvider glskProvider, Network network) {
        Map<String, Map<String, Double>> acReferenceFlowPerVariant = new HashMap<>();
        String defaultVariantId = network.getVariantManager().getWorkingVariantId();
        Set<Branch> baseCaseXnecList = xnecProvider.getNetworkElements(network);
        LoadFlowRunningService.Result baseCaseLoadFlowServiceAcResultLocal = runAcLoadFlow(network);
        if (baseCaseLoadFlowServiceAcResultLocal.fallbackHasBeenActivated()) {
            acReferenceFlowPerVariant.put(defaultVariantId, baseCaseXnecList.stream().collect(Collectors.toMap(Identifiable::getId, branch -> Double.NaN)));
        } else {
            acReferenceFlowPerVariant.put(defaultVariantId, getXnecReferenceFlows(baseCaseXnecList));
        }

        if (parameters.getLossesCompensationMode() == FlowDecompositionParameters.LossCompensationMode.SHARED) {
            compensateLosses(network);
        }

        NetworkStateManager networkStateManager = new NetworkStateManager(network, xnecProvider);

        Map<Country, Map<String, Double>> glsks = glskProvider.getGlsk(network);
        Map<Country, Double> netPositions = getZonesNetPosition(network);

        FlowDecompositionResults flowDecompositionResults = new FlowDecompositionResults(network);

        Map<String, Map<String, Double>> nodalInjectionDcReferencePerVariant = new HashMap<>();
        Map<String, Map<String, Double>> nodalInjectionForXNodeFlowPerVariant = new HashMap<>();

        Map<String, Set<Branch>> variantIdXnecMap = new HashMap<>(xnecProvider.getNetworkElementsPerContingency(network));
        variantIdXnecMap.put(defaultVariantId, baseCaseXnecList);

        variantIdXnecMap.forEach((variantId, xnecList) -> {
            networkStateManager.setNetworkVariant(variantId);
            if (!acReferenceFlowPerVariant.containsKey(variantId)) {
                LoadFlowRunningService.Result loadFlowServiceAcResultLocal = runAcLoadFlow(network);
                if (loadFlowServiceAcResultLocal.fallbackHasBeenActivated()) {
                    acReferenceFlowPerVariant.put(variantId, xnecList.stream().collect(Collectors.toMap(Identifiable::getId, branch -> Double.NaN)));
                } else {
                    acReferenceFlowPerVariant.put(variantId, getXnecReferenceFlows(xnecList));
                }
            }
            if (parameters.getLossesCompensationMode() == FlowDecompositionParameters.LossCompensationMode.PER_STATE) {
                compensateLosses(network);
            }
            runDcLoadFlow(network);

            ReferenceNodalInjectionComputer referenceNodalInjectionComputer = new ReferenceNodalInjectionComputer();
            nodalInjectionDcReferencePerVariant.put(variantId, referenceNodalInjectionComputer.run(NetworkUtil.getNodeList(network)));
            nodalInjectionForXNodeFlowPerVariant.put(variantId, referenceNodalInjectionComputer.run(NetworkUtil.getXNodeList(network)));
        });
        networkStateManager.setDefaultNetworkVariant();

        variantIdXnecMap.forEach((variantId, xnecSet) -> {
            networkStateManager.setNetworkVariant(variantId);
            String contingencyId = networkStateManager.getContingencyId(variantId);
            List<Branch> xnecList = new ArrayList<>(xnecSet);
            NetworkMatrixIndexes networkMatrixIndexes = new NetworkMatrixIndexes(network, xnecList);
            List<Injection<?>> nodeList = NetworkUtil.getNodeList(network);
            Map<String, Integer> nodeIndex = NetworkUtil.getIndexFromInjections(nodeList);
            List<String> pstList = networkMatrixIndexes.getPstList();
            Map<String, Integer> pstIndex = networkMatrixIndexes.getPstIndex();

            // DC Sensi
            SensitivityAnalyser sensitivityAnalyser = createSensitivityAnalyser(network, xnecSet);
            SensitivityAnalyser.Result dcSensitivityResult = getPtdfMatrix(sensitivityAnalyser, nodeList, nodeIndex);
            SparseMatrixWithIndexesTriplet ptdfMatrix = dcSensitivityResult.getSensitivityMatrixTriplet();
            SparseMatrixWithIndexesTriplet psdfMatrix = getPsdfMatrix(sensitivityAnalyser, pstList, pstIndex);

            SparseMatrixWithIndexesTriplet nodalInjectionsMatrix = getNodalInjectionsMatrix(network, netPositions, nodeList, nodeIndex, glsks, nodalInjectionDcReferencePerVariant.get(variantId), nodalInjectionForXNodeFlowPerVariant.get(variantId));
            SparseMatrixWithIndexesCSC allocatedLoopFlowsMatrix = computeAllocatedAndLoopFlows(nodalInjectionsMatrix, ptdfMatrix);
            SparseMatrixWithIndexesCSC pstFlowMatrix = computePstFlows(network, pstList, pstIndex, psdfMatrix);

            FlowDecompositionResults.PerStateBuilder flowDecompositionResultsBuilder = flowDecompositionResults.getBuilder(contingencyId, xnecSet);
            flowDecompositionResultsBuilder.saveAcReferenceFlow(acReferenceFlowPerVariant.get(variantId));
            flowDecompositionResultsBuilder.saveDcReferenceFlow(dcSensitivityResult.getReferenceFlow());
            flowDecompositionResultsBuilder.saveAllocatedAndLoopFlowsMatrix(allocatedLoopFlowsMatrix);
            flowDecompositionResultsBuilder.savePstFlowMatrix(pstFlowMatrix);
            flowDecompositionResultsBuilder.build(parameters.isRescaleEnabled());
        });
        networkStateManager.setDefaultNetworkVariant();
        networkStateManager.deleteAllContingencyVariants();
        return flowDecompositionResults;
    }

    private LoadFlowRunningService.Result runAcLoadFlow(Network network) {
        return loadFlowRunningService.runAcLoadflow(network, loadFlowParameters, parameters.isDcFallbackEnabledAfterAcDivergence());
    }

    private Map<Country, Double> getZonesNetPosition(Network network) {
        NetPositionComputer netPositionComputer = new NetPositionComputer();
        return netPositionComputer.run(network);
    }

    private Map<String, Double> getXnecReferenceFlows(Set<Branch> xnecList) {
        ReferenceFlowComputer referenceFlowComputer = new ReferenceFlowComputer();
        return referenceFlowComputer.run(xnecList);
    }

    private void compensateLosses(Network network) {
        if (parameters.getLossesCompensationMode() != FlowDecompositionParameters.LossCompensationMode.NONE) {
            lossesCompensator.run(network);
        }
    }

    private LoadFlowRunningService.Result runDcLoadFlow(Network network) {
        return loadFlowRunningService.runDcLoadflow(network, loadFlowParameters);
    }

    private SparseMatrixWithIndexesTriplet getNodalInjectionsMatrix(Network network,
                                                                    Map<Country, Double> netPositions,
                                                                    List<Injection<?>> nodeList, Map<String, Integer> nodeIndex,
                                                                    Map<Country, Map<String, Double>> glsks,
                                                                    Map<String, Double> nodalInjectionDcReference,
                                                                    Map<String, Double> nodalInjectionForXNodeFlow) {
        NodalInjectionComputer nodalInjectionComputer = new NodalInjectionComputer(nodeList, nodeIndex, nodalInjectionDcReference, nodalInjectionForXNodeFlow);
        return nodalInjectionComputer.run(network, glsks, netPositions);
    }

    private SensitivityAnalyser createSensitivityAnalyser(Network network, Set<Branch> xnecSet) {
        return new SensitivityAnalyser(loadFlowParameters, parameters, sensitivityAnalysisRunner, network, new ArrayList<>(xnecSet), NetworkUtil.getIndexFromBranches(xnecSet));
    }

    private SensitivityAnalyser.Result getPtdfMatrix(SensitivityAnalyser sensitivityAnalyser,
                                                     List<Injection<?>> nodeIdList,
                                                     Map<String, Integer> nodeIndex) {
        return sensitivityAnalyser.run(nodeIdList.stream().map(Identifiable::getId).collect(Collectors.toList()), nodeIndex, SensitivityVariableType.INJECTION_ACTIVE_POWER);
    }

    private SparseMatrixWithIndexesCSC computeAllocatedAndLoopFlows(SparseMatrixWithIndexesTriplet nodalInjectionsMatrix,
                                                                    SparseMatrixWithIndexesTriplet ptdfMatrix) {
        return SparseMatrixWithIndexesCSC.mult(ptdfMatrix.toCSCMatrix(), nodalInjectionsMatrix.toCSCMatrix());
    }

    private SparseMatrixWithIndexesTriplet getPsdfMatrix(SensitivityAnalyser sensitivityAnalyser,
                                                         List<String> pstList,
                                                         Map<String, Integer> pstIndex) {
        return sensitivityAnalyser.run(pstList, pstIndex, SensitivityVariableType.TRANSFORMER_PHASE).getSensitivityMatrixTriplet();
    }

    private SparseMatrixWithIndexesCSC computePstFlows(Network network,
                                                       List<String> pstList,
                                                       Map<String, Integer> pstIndex,
                                                       SparseMatrixWithIndexesTriplet psdfMatrix) {
        PstFlowComputer pstFlowComputer = new PstFlowComputer();
        return pstFlowComputer.run(network, pstList, pstIndex, psdfMatrix);
    }
}
