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
        FlowDecompositionResults flowDecompositionResults = new FlowDecompositionResults(network, parameters);

        LoadFlowRunningService.Result loadFlowServiceAcResult = runAcLoadFlow(network);

        Map<Country, Map<String, Double>> glsks = getGlsks(network, flowDecompositionResults);
        List<Branch> xnecList = getXnecList(network, glsks, flowDecompositionResults);
        Map<Country, Double> netPositions = getZonesNetPosition(network, flowDecompositionResults);
        saveAcReferenceFlow(flowDecompositionResults, xnecList, loadFlowServiceAcResult);
        compensateLosses(network);

        // None
        NetworkMatrixIndexes networkMatrixIndexes = new NetworkMatrixIndexes(network, xnecList);

        runDcLoadFlow(network);

        SparseMatrixWithIndexesTriplet nodalInjectionsMatrix = getNodalInjectionsMatrix(network,
            flowDecompositionResults, netPositions, networkMatrixIndexes, glsks);
        saveDcReferenceFlow(flowDecompositionResults, xnecList);

        // DC Sensi
        SensitivityAnalyser sensitivityAnalyser = getSensitivityAnalyser(network, networkMatrixIndexes);
        SparseMatrixWithIndexesTriplet ptdfMatrix = getPtdfMatrix(flowDecompositionResults,
            networkMatrixIndexes, sensitivityAnalyser);
        SparseMatrixWithIndexesTriplet psdfMatrix = getPsdfMatrix(flowDecompositionResults,
            networkMatrixIndexes, sensitivityAnalyser);

        // None
        computeAllocatedAndLoopFlows(flowDecompositionResults, nodalInjectionsMatrix, ptdfMatrix);
        computePstFlows(network, flowDecompositionResults, networkMatrixIndexes, psdfMatrix);

        rescale(flowDecompositionResults);

        return flowDecompositionResults;
    }

    private LoadFlowRunningService.Result runAcLoadFlow(Network network) {
        return loadFlowRunningService.runAcLoadflow(network, loadFlowParameters, parameters.isDcFallbackEnabledAfterAcDivergence());
    }

    private List<Branch> getXnecList(Network network, Map<Country, Map<String, Double>> glsks, FlowDecompositionResults flowDecompositionResults) {
        XnecSelector xnecSelector;
        switch (parameters.getXnecSelectionStrategy()) {
            case ONLY_INTERCONNECTIONS:
                xnecSelector = new XnecSelectorInterconnection();
                break;
            case INTERCONNECTION_OR_ZONE_TO_ZONE_PTDF_GT_5PC:
                Map<String, Map<Country, Double>> zonalPtdf = getZonalPtdf(network, glsks, flowDecompositionResults);
                xnecSelector = new XnecSelector5percPtdf(zonalPtdf);
                break;
            default:
                throw new PowsyblException(String.format("XnecSelectionStrategy %s is not valid",
                    parameters.getXnecSelectionStrategy()));
        }
        List<Branch> xnecList = xnecSelector.run(network);
        flowDecompositionResults.saveXnec(xnecList);
        return xnecList;
    }

    private void saveAcReferenceFlow(FlowDecompositionResults flowDecompositionResults, List<Branch> xnecList, LoadFlowRunningService.Result loadFlowServiceAcResult) {
        Map<String, Double> acReferenceFlows;
        if (loadFlowServiceAcResult.isFallbackHasBeenActivated()) {
            acReferenceFlows = xnecList.stream().collect(Collectors.toMap(Identifiable::getId, branch -> Double.NaN));
        } else {
            acReferenceFlows = getXnecReferenceFlows(xnecList);
        }
        flowDecompositionResults.saveAcReferenceFlow(acReferenceFlows);
    }

    private Map<Country, Map<String, Double>> getGlsks(Network network,
                                                       FlowDecompositionResults flowDecompositionResults) {
        GlskComputer glskComputer = new GlskComputer();
        Map<Country, Map<String, Double>> glsks = glskComputer.run(network);
        flowDecompositionResults.saveGlsks(glsks);
        return glsks;
    }

    private Map<String, Map<Country, Double>> getZonalPtdf(Network network,
                                                           Map<Country, Map<String, Double>> glsks,
                                                           FlowDecompositionResults flowDecompositionResults) {
        ZonalSensitivityAnalyser zonalSensitivityAnalyser = new ZonalSensitivityAnalyser(loadFlowParameters, sensitivityAnalysisRunner);
        Map<String, Map<Country, Double>> zonalPtdf = zonalSensitivityAnalyser.run(network,
            glsks, SensitivityVariableType.INJECTION_ACTIVE_POWER);
        flowDecompositionResults.saveZonalPtdf(zonalPtdf);
        return zonalPtdf;
    }

    private Map<Country, Double> getZonesNetPosition(Network network,
                                                     FlowDecompositionResults flowDecompositionResults) {
        NetPositionComputer netPositionComputer = new NetPositionComputer();
        Map<Country, Double> netPosition = netPositionComputer.run(network);
        flowDecompositionResults.saveACNetPosition(netPosition);
        return netPosition;
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
                                                                    FlowDecompositionResults flowDecompositionResults,
                                                                    Map<Country, Double> netPositions,
                                                                    NetworkMatrixIndexes networkMatrixIndexes,
                                                                    Map<Country, Map<String, Double>> glsks) {
        NodalInjectionComputer nodalInjectionComputer = new NodalInjectionComputer(networkMatrixIndexes);
        Map<String, Double> dcNodalInjection = getDcNodalInjection(flowDecompositionResults, networkMatrixIndexes);

        return getNodalInjectionsMatrix(network, flowDecompositionResults, netPositions, glsks,
            nodalInjectionComputer, dcNodalInjection);
    }

    private Map<String, Double> getDcNodalInjection(FlowDecompositionResults flowDecompositionResults,
                                                    NetworkMatrixIndexes networkMatrixIndexes) {
        ReferenceNodalInjectionComputer referenceNodalInjectionComputer = new ReferenceNodalInjectionComputer(networkMatrixIndexes);
        Map<String, Double> dcNodalInjection = referenceNodalInjectionComputer.run();
        flowDecompositionResults.saveDcNodalInjections(dcNodalInjection);
        return dcNodalInjection;
    }

    private SparseMatrixWithIndexesTriplet getNodalInjectionsMatrix(Network network,
                                                                    FlowDecompositionResults flowDecompositionResults,
                                                                    Map<Country, Double> netPositions,
                                                                    Map<Country, Map<String, Double>> glsks,
                                                                    NodalInjectionComputer nodalInjectionComputer,
                                                                    Map<String, Double> dcNodalInjection) {
        SparseMatrixWithIndexesTriplet nodalInjectionsMatrix =
            nodalInjectionComputer.run(network,
                glsks, netPositions, dcNodalInjection);
        flowDecompositionResults.saveNodalInjectionsMatrix(nodalInjectionsMatrix);
        return nodalInjectionsMatrix;
    }

    private void saveDcReferenceFlow(FlowDecompositionResults flowDecompositionResults, List<Branch> xnecList) {
        flowDecompositionResults.saveDcReferenceFlow(getXnecReferenceFlows(xnecList));
    }

    private SensitivityAnalyser getSensitivityAnalyser(Network network, NetworkMatrixIndexes networkMatrixIndexes) {
        return new SensitivityAnalyser(loadFlowParameters, parameters, sensitivityAnalysisRunner, network, networkMatrixIndexes);
    }

    private SparseMatrixWithIndexesTriplet getPtdfMatrix(FlowDecompositionResults flowDecompositionResults,
                                                         NetworkMatrixIndexes networkMatrixIndexes,
                                                         SensitivityAnalyser sensitivityAnalyser) {
        SparseMatrixWithIndexesTriplet ptdfMatrix =
            sensitivityAnalyser.run(networkMatrixIndexes.getNodeIdList(),
                networkMatrixIndexes.getNodeIndex(),
                SensitivityVariableType.INJECTION_ACTIVE_POWER);
        flowDecompositionResults.savePtdfMatrix(ptdfMatrix);
        return ptdfMatrix;
    }

    private void computeAllocatedAndLoopFlows(FlowDecompositionResults flowDecompositionResults,
                                              SparseMatrixWithIndexesTriplet nodalInjectionsMatrix,
                                              SparseMatrixWithIndexesTriplet ptdfMatrix) {
        SparseMatrixWithIndexesCSC allocatedLoopFlowsMatrix =
            SparseMatrixWithIndexesCSC.mult(ptdfMatrix.toCSCMatrix(), nodalInjectionsMatrix.toCSCMatrix());
        flowDecompositionResults.saveAllocatedAndLoopFlowsMatrix(allocatedLoopFlowsMatrix);
    }

    private SparseMatrixWithIndexesTriplet getPsdfMatrix(FlowDecompositionResults flowDecompositionResults,
                                                         NetworkMatrixIndexes networkMatrixIndexes,
                                                         SensitivityAnalyser sensitivityAnalyser) {
        SparseMatrixWithIndexesTriplet psdfMatrix =
            sensitivityAnalyser.run(networkMatrixIndexes.getPstList(),
                networkMatrixIndexes.getPstIndex(), SensitivityVariableType.TRANSFORMER_PHASE);
        flowDecompositionResults.savePsdfMatrix(psdfMatrix);
        return psdfMatrix;
    }

    private void computePstFlows(Network network,
                                 FlowDecompositionResults flowDecompositionResults,
                                 NetworkMatrixIndexes networkMatrixIndexes,
                                 SparseMatrixWithIndexesTriplet psdfMatrix) {
        PstFlowComputer pstFlowComputer = new PstFlowComputer();
        SparseMatrixWithIndexesCSC pstFlowMatrix = pstFlowComputer.run(network, networkMatrixIndexes, psdfMatrix);
        flowDecompositionResults.savePstFlowMatrix(pstFlowMatrix);
    }

    private void rescale(FlowDecompositionResults flowDecompositionResults) {
        flowDecompositionResults.saveRescaledDecomposedFlowMap(getRescaledDecomposedFlowMap(flowDecompositionResults));
    }

    private Map<String, DecomposedFlow> getRescaledDecomposedFlowMap(FlowDecompositionResults flowDecompositionResults) {
        Map<String, DecomposedFlow> decomposedFlowMap = flowDecompositionResults.getDecomposedFlowMapBeforeRescaling();
        if (parameters.isRescaleEnabled()) {
            DecomposedFlowsRescaler decomposedFlowsRescaler = new DecomposedFlowsRescaler();
            return decomposedFlowsRescaler.rescale(decomposedFlowMap);
        }
        return decomposedFlowMap;
    }
}
