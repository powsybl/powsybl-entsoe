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
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.sensitivity.SensitivityAnalysis;
import com.powsybl.sensitivity.SensitivityVariableType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 */
public class FlowDecompositionComputer {
    static final String DEFAULT_LOAD_FLOW_PROVIDER = null;
    static final String DEFAULT_SENSITIVITY_ANALYSIS_PROVIDER = null;
    private static final String NO_CONTINGENCY_ID = "";
    private final LoadFlowParameters loadFlowParameters;
    private final FlowDecompositionParameters parameters;
    private final LoadFlowRunningService loadFlowRunningService;
    private final SensitivityAnalysis.Runner sensitivityAnalysisRunner;
    private final LossesCompensator lossesCompensator;

    public FlowDecompositionComputer() {
        this(new FlowDecompositionParameters());
    }

    public FlowDecompositionComputer(FlowDecompositionParameters flowDecompositionParameters, LoadFlowParameters loadFlowParameters, String loadFlowProvider, String sensitivityAnalysisProvider) {
        this.parameters = flowDecompositionParameters;
        this.loadFlowParameters = loadFlowParameters;
        this.loadFlowRunningService = new LoadFlowRunningService(LoadFlow.find(loadFlowProvider));
        this.sensitivityAnalysisRunner = SensitivityAnalysis.find(sensitivityAnalysisProvider);
        this.lossesCompensator = parameters.isLossesCompensationEnabled() ? new LossesCompensator(parameters) : null;
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
        NetworkStateManager networkStateManager = new NetworkStateManager(network, xnecProvider);

        runAcLoadFlow(network);

        Map<Country, Map<String, Double>> glsks = glskProvider.getGlsk(network);
        Map<Country, Double> netPositions = getZonesNetPosition(network);

        FlowDecompositionResults flowDecompositionResults = new FlowDecompositionResults(network);

        Map<String, Map<String, Double>> allNodalInjectionDcReference = new HashMap<>();
        Map<String, Map<String, Double>> allNodalInjectionForXNodeFlow = new HashMap<>();
        Map<String, Map<String, Double>> allACReferenceFlow = new HashMap<>();

        String defaultVariantId = network.getVariantManager().getWorkingVariantId();
        Map<String, Set<Branch>> variantIdXnecMap = new HashMap<>(xnecProvider.getNetworkElementsPerContingency(network));
        variantIdXnecMap.put(defaultVariantId, xnecProvider.getNetworkElements(network));
        variantIdXnecMap.forEach((variantId, xnecList) -> {
            network.getVariantManager().setWorkingVariant(variantId);
            LoadFlowRunningService.Result loadFlowServiceAcResultLocal = runAcLoadFlow(network);
            if (loadFlowServiceAcResultLocal.fallbackHasBeenActivated()) {
                allACReferenceFlow.put(variantId, xnecList.stream().collect(Collectors.toMap(Identifiable::getId, branch -> Double.NaN)));
            } else {
                allACReferenceFlow.put(variantId, getXnecReferenceFlows(xnecList));
            }
            compensateLosses(network);
            runDcLoadFlow(network);
            NetworkMatrixIndexes networkMatrixIndexes = new NetworkMatrixIndexes(network, Collections.emptyList());
            ReferenceNodalInjectionComputer referenceNodalInjectionComputer = new ReferenceNodalInjectionComputer();
            allNodalInjectionDcReference.put(variantId, referenceNodalInjectionComputer.run(networkMatrixIndexes.getNodeList()));
            allNodalInjectionForXNodeFlow.put(variantId, referenceNodalInjectionComputer.run(networkMatrixIndexes.getUnmergedXNodeList()));
        });
        network.getVariantManager().setWorkingVariant(defaultVariantId);

        variantIdXnecMap.forEach((variantId, xnecList) -> {
            networkStateManager.setNetworkVariant(variantId);
            String contingencyId = (Objects.equals(variantId, defaultVariantId)) ? NO_CONTINGENCY_ID : variantId;
            FlowDecompositionResults.PerStateBuilder flowDecompositionResultsBuilder = flowDecompositionResults.getBuilder(contingencyId, xnecList);
            flowDecompositionResultsBuilder.saveAcReferenceFlow(allACReferenceFlow.get(variantId));

            // None
            NetworkMatrixIndexes networkMatrixIndexes = new NetworkMatrixIndexes(network, new ArrayList<>(xnecList));

            // DC Sensi
            SensitivityAnalyser sensitivityAnalyser = createSensitivityAnalyser(network, networkMatrixIndexes);
            SensitivityAnalyser.Result dcSensitivityResult = runDcSensitivity(networkMatrixIndexes, sensitivityAnalyser);
            saveDcReferenceFlow(flowDecompositionResultsBuilder, dcSensitivityResult);
            SparseMatrixWithIndexesTriplet ptdfMatrix = dcSensitivityResult.getSensitivityMatrixTriplet();
            SparseMatrixWithIndexesTriplet psdfMatrix = getPsdfMatrix(networkMatrixIndexes, sensitivityAnalyser);

            SparseMatrixWithIndexesTriplet nodalInjectionsMatrix = getNodalInjectionsMatrix(network, netPositions, networkMatrixIndexes, glsks, allNodalInjectionDcReference, allNodalInjectionForXNodeFlow);

            // None
            computeAllocatedAndLoopFlows(flowDecompositionResultsBuilder, nodalInjectionsMatrix, ptdfMatrix);
            computePstFlows(network, flowDecompositionResultsBuilder, networkMatrixIndexes, psdfMatrix);

            flowDecompositionResultsBuilder.build(parameters.isRescaleEnabled());
        });
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
        if (parameters.isLossesCompensationEnabled()) {
            lossesCompensator.run(network);
        }
    }

    private LoadFlowRunningService.Result runDcLoadFlow(Network network) {
        return loadFlowRunningService.runDcLoadflow(network, loadFlowParameters);
    }

    private SparseMatrixWithIndexesTriplet getNodalInjectionsMatrix(Network network, Map<Country, Double> netPositions, NetworkMatrixIndexes networkMatrixIndexes, Map<Country, Map<String, Double>> glsks, Map<String, Map<String, Double>> allNodalInjectionDcReference, Map<String, Map<String, Double>> allNodalInjectionForXNodeFlow) {
        NodalInjectionComputer nodalInjectionComputer = new NodalInjectionComputer(networkMatrixIndexes, allNodalInjectionDcReference, allNodalInjectionForXNodeFlow);
        return nodalInjectionComputer.run(network, glsks, netPositions);
    }

    private void saveDcReferenceFlow(FlowDecompositionResults.PerStateBuilder flowDecompositionResultsBuilder, SensitivityAnalyser.Result dcSensitivityResult) {
        flowDecompositionResultsBuilder.saveDcReferenceFlow(dcSensitivityResult.getReferenceFlow());
    }

    private SensitivityAnalyser createSensitivityAnalyser(Network network, NetworkMatrixIndexes networkMatrixIndexes) {
        return new SensitivityAnalyser(loadFlowParameters, parameters, sensitivityAnalysisRunner, network, networkMatrixIndexes);
    }

    private SensitivityAnalyser.Result runDcSensitivity(NetworkMatrixIndexes networkMatrixIndexes, SensitivityAnalyser sensitivityAnalyser) {
        return sensitivityAnalyser.run(networkMatrixIndexes.getNodeIdList(), networkMatrixIndexes.getNodeIndex(), SensitivityVariableType.INJECTION_ACTIVE_POWER);
    }

    private void computeAllocatedAndLoopFlows(FlowDecompositionResults.PerStateBuilder flowDecompositionResultBuilder, SparseMatrixWithIndexesTriplet nodalInjectionsMatrix, SparseMatrixWithIndexesTriplet ptdfMatrix) {
        SparseMatrixWithIndexesCSC allocatedLoopFlowsMatrix = SparseMatrixWithIndexesCSC.mult(ptdfMatrix.toCSCMatrix(), nodalInjectionsMatrix.toCSCMatrix());
        flowDecompositionResultBuilder.saveAllocatedAndLoopFlowsMatrix(allocatedLoopFlowsMatrix);
    }

    private SparseMatrixWithIndexesTriplet getPsdfMatrix(NetworkMatrixIndexes networkMatrixIndexes, SensitivityAnalyser sensitivityAnalyser) {
        return sensitivityAnalyser.run(networkMatrixIndexes.getPstList(), networkMatrixIndexes.getPstIndex(), SensitivityVariableType.TRANSFORMER_PHASE).getSensitivityMatrixTriplet();
    }

    private void computePstFlows(Network network, FlowDecompositionResults.PerStateBuilder flowDecompositionResultBuilder, NetworkMatrixIndexes networkMatrixIndexes, SparseMatrixWithIndexesTriplet psdfMatrix) {
        PstFlowComputer pstFlowComputer = new PstFlowComputer();
        SparseMatrixWithIndexesCSC pstFlowMatrix = pstFlowComputer.run(network, networkMatrixIndexes, psdfMatrix);
        flowDecompositionResultBuilder.savePstFlowMatrix(pstFlowMatrix);
    }
}
