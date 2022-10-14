/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.flow_decomposition;

import com.powsybl.commons.PowsyblException;
import com.powsybl.iidm.network.*;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.sensitivity.*;

import java.util.*;
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

    public FlowDecompositionResults run(Network network) {
        FlowDecompositionResultsBuilder flowDecompositionResultsBuilder = new FlowDecompositionResultsBuilder(network);

        LoadFlowRunningService.Result loadFlowServiceAcResult = runAcLoadFlow(network);

        Map<Country, Map<String, Double>> glsks = getGlsks(network);
        List<Branch> xnecList = getXnecList(network, glsks, flowDecompositionResultsBuilder);
        Map<Country, Double> netPositions = getZonesNetPosition(network);
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

        return flowDecompositionResultsBuilder.build(parameters.isRescaleEnabled());
    }

    private LoadFlowRunningService.Result runAcLoadFlow(Network network) {
        return loadFlowRunningService.runAcLoadflow(network, loadFlowParameters, parameters.isDcFallbackEnabledAfterAcDivergence());
    }

    private List<Branch> getXnecList(Network network, Map<Country, Map<String, Double>> glsks, FlowDecompositionResultsBuilder flowDecompositionResultsBuilder) {
        XnecSelector xnecSelector;
        switch (parameters.getXnecSelectionStrategy()) {
            case ONLY_INTERCONNECTIONS:
                xnecSelector = new XnecSelectorInterconnection();
                break;
            case INTERCONNECTION_OR_ZONE_TO_ZONE_PTDF_GT_5PC:
                Map<String, Map<Country, Double>> zonalPtdf = getZonalPtdf(network, glsks);
                xnecSelector = new XnecSelector5percPtdf(zonalPtdf);
                break;
            default:
                throw new PowsyblException(String.format("XnecSelectionStrategy %s is not valid",
                    parameters.getXnecSelectionStrategy()));
        }
        List<Branch> xnecList = xnecSelector.run(network);
        flowDecompositionResultsBuilder.saveXnec(xnecList);
        return xnecList;
    }

    private void saveAcReferenceFlow(FlowDecompositionResultsBuilder flowDecompositionResultsBuilder, List<Branch> xnecList, LoadFlowRunningService.Result loadFlowServiceAcResult) {
        Map<String, Double> acReferenceFlows;
        if (loadFlowServiceAcResult.fallbackHasBeenActivated()) {
            acReferenceFlows = xnecList.stream().collect(Collectors.toMap(Identifiable::getId, branch -> Double.NaN));
        } else {
            acReferenceFlows = getXnecReferenceFlows(xnecList);
        }
        flowDecompositionResultsBuilder.saveAcReferenceFlow(acReferenceFlows);
    }

    private Map<Country, Map<String, Double>> getGlsks(Network network) {
        GlskComputer glskComputer = new GlskComputer();
        return glskComputer.run(network);
    }

    private Map<String, Map<Country, Double>> getZonalPtdf(Network network,
                                                           Map<Country, Map<String, Double>> glsks) {
        ZonalSensitivityAnalyser zonalSensitivityAnalyser = new ZonalSensitivityAnalyser(loadFlowParameters, sensitivityAnalysisRunner);
        return zonalSensitivityAnalyser.run(network,
            glsks, SensitivityVariableType.INJECTION_ACTIVE_POWER);
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
            lossesCompensator.run(network);
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

    private void saveDcReferenceFlow(FlowDecompositionResultsBuilder flowDecompositionResultsBuilder, List<Branch> xnecList) {
        flowDecompositionResultsBuilder.saveDcReferenceFlow(getXnecReferenceFlows(xnecList));
    }

    SensitivityAnalyser getSensitivityAnalyser(Network network, NetworkMatrixIndexes networkMatrixIndexes) {
        return new SensitivityAnalyser(loadFlowParameters, parameters, sensitivityAnalysisRunner, network, networkMatrixIndexes);
    }

    private SparseMatrixWithIndexesTriplet getPtdfMatrix(NetworkMatrixIndexes networkMatrixIndexes,
                                                         SensitivityAnalyser sensitivityAnalyser) {
        return sensitivityAnalyser.run(networkMatrixIndexes.getNodeIdList(),
            networkMatrixIndexes.getNodeIndex(),
            SensitivityVariableType.INJECTION_ACTIVE_POWER);
    }

    private void computeAllocatedAndLoopFlows(FlowDecompositionResultsBuilder flowDecompositionResultsBuilder,
                                              SparseMatrixWithIndexesTriplet nodalInjectionsMatrix,
                                              SparseMatrixWithIndexesTriplet ptdfMatrix) {
        SparseMatrixWithIndexesCSC allocatedLoopFlowsMatrix =
            SparseMatrixWithIndexesCSC.mult(ptdfMatrix.toCSCMatrix(), nodalInjectionsMatrix.toCSCMatrix());
        flowDecompositionResultsBuilder.saveAllocatedAndLoopFlowsMatrix(allocatedLoopFlowsMatrix);
    }

    private SparseMatrixWithIndexesTriplet getPsdfMatrix(NetworkMatrixIndexes networkMatrixIndexes,
                                                         SensitivityAnalyser sensitivityAnalyser) {
        return sensitivityAnalyser.run(networkMatrixIndexes.getPstList(),
            networkMatrixIndexes.getPstIndex(), SensitivityVariableType.TRANSFORMER_PHASE);
    }

    private void computePstFlows(Network network,
                                 FlowDecompositionResultsBuilder flowDecompositionResultsBuilder,
                                 NetworkMatrixIndexes networkMatrixIndexes,
                                 SparseMatrixWithIndexesTriplet psdfMatrix) {
        PstFlowComputer pstFlowComputer = new PstFlowComputer();
        SparseMatrixWithIndexesCSC pstFlowMatrix = pstFlowComputer.run(network, networkMatrixIndexes, psdfMatrix);
        flowDecompositionResultsBuilder.savePstFlowMatrix(pstFlowMatrix);
    }
}
