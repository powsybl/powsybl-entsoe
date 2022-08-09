/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.flow_decomposition;

import com.powsybl.commons.PowsyblException;
import com.powsybl.iidm.network.*;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.sensitivity.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 */
public class FlowDecompositionComputer {
    static final boolean DC_LOAD_FLOW = true;
    private static final Logger LOGGER = LoggerFactory.getLogger(FlowDecompositionComputer.class);
    private final LoadFlowParameters loadFlowParameters;
    private final FlowDecompositionParameters parameters;

    public FlowDecompositionComputer() {
        this(new FlowDecompositionParameters());
    }

    public FlowDecompositionComputer(FlowDecompositionParameters parameters) {
        this.parameters = parameters;
        this.loadFlowParameters = initLoadFlowParameters();
    }

    public FlowDecompositionResults run(Network network) {
        FlowDecompositionResults flowDecompositionResults = new FlowDecompositionResults(network, parameters);

        //AC LF
        Map<Country, Map<String, Double>> glsks = getGlsks(network, flowDecompositionResults);
        Map<String, Map<Country, Double>> zonalPtdf = getZonalPtdf(network, glsks, flowDecompositionResults);
        List<Branch> xnecList = getXnecList(network, zonalPtdf, flowDecompositionResults);
        Map<Country, Double> netPositions = getZonesNetPosition(network, flowDecompositionResults);
        flowDecompositionResults.saveAcReferenceFlow(getXnecReferenceFlows(xnecList));
        compensateLosses(network);

        // None
        NetworkMatrixIndexes networkMatrixIndexes = new NetworkMatrixIndexes(network, xnecList);

        // DC LF
        SparseMatrixWithIndexesTriplet nodalInjectionsMatrix = getNodalInjectionsMatrix(network,
            flowDecompositionResults, netPositions, networkMatrixIndexes, glsks);
        flowDecompositionResults.saveDcReferenceFlow(getXnecReferenceFlows(xnecList));

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

    private List<Branch> getXnecList(Network network, Map<String, Map<Country, Double>> zonalPtdf, FlowDecompositionResults flowDecompositionResults) {
        XnecSelector xnecSelector;
        switch (parameters.getXnecSelectionStrategy()) {
            case ONLY_INTERCONNECTIONS:
                xnecSelector = new XnecSelectorInterconnection();
                break;
            case ZONE_TO_ZONE_PTDF_CRITERIA:
                xnecSelector = new XnecSelector5percPtdf(zonalPtdf);
                break;
            default:
                throw new PowsyblException(String.format("XnecSelectionStrategy %s is not valid",
                    parameters.getXnecSelectionStrategy()));
        }
        List<Branch> xnecList = xnecSelector.run(network);
        flowDecompositionResults.saveXnecToCountry(NetworkUtil.getXnecToCountry(xnecList));
        return xnecList;
    }

    private static LoadFlowParameters initLoadFlowParameters() {
        LoadFlowParameters parameters = LoadFlowParameters.load();
        parameters.setDc(DC_LOAD_FLOW);
        LOGGER.debug("Using following load flow parameters: {}", parameters);
        return parameters;
    }

    private Map<Country, Map<String, Double>> getGlsks(Network network,
                                                       FlowDecompositionResults flowDecompositionResults) {
        GlskComputer glskComputer = new GlskComputer();
        Map<Country, Map<String, Double>> glsks = glskComputer.run(network);
        return flowDecompositionResults.saveGlsks(glsks);
    }

    private Map<String, Map<Country, Double>> getZonalPtdf(Network network,
                                                           Map<Country, Map<String, Double>> glsks,
                                                           FlowDecompositionResults flowDecompositionResults) {
        ZonalSensitivityAnalyser zonalSensitivityAnalyser = new ZonalSensitivityAnalyser(loadFlowParameters, parameters);
        Map<String, Map<Country, Double>> zonalPtdf = zonalSensitivityAnalyser.run(network,
            glsks, SensitivityVariableType.INJECTION_ACTIVE_POWER);
        return flowDecompositionResults.saveZonalPtdf(zonalPtdf);
    }

    private Map<Country, Double> getZonesNetPosition(Network network,
                                                     FlowDecompositionResults flowDecompositionResults) {
        NetPositionComputer netPositionComputer = new NetPositionComputer(loadFlowParameters);
        Map<Country, Double> netPosition = netPositionComputer.run(network);
        return flowDecompositionResults.saveACNetPosition(netPosition);
    }

    private Map<String, Double> getXnecReferenceFlows(List<Branch> xnecList) {
        ReferenceFlowComputer referenceFlowComputer = new ReferenceFlowComputer();
        return referenceFlowComputer.run(xnecList);
    }

    private void compensateLosses(Network network) {
        if (parameters.isLossesCompensationEnabled()) {
            LossesCompensator lossesCompensator = new LossesCompensator(loadFlowParameters, parameters);
            lossesCompensator.run(network);
        }
    }

    private SparseMatrixWithIndexesTriplet getNodalInjectionsMatrix(Network network,
                                                                    FlowDecompositionResults flowDecompositionResults,
                                                                    Map<Country, Double> netPositions,
                                                                    NetworkMatrixIndexes networkMatrixIndexes,
                                                                    Map<Country, Map<String, Double>> glsks) {
        NodalInjectionComputer nodalInjectionComputer = new NodalInjectionComputer(networkMatrixIndexes);
        Map<String, Double> dcNodalInjection = getDcNodalInjection(network, flowDecompositionResults, networkMatrixIndexes);

        return getNodalInjectionsMatrix(network, flowDecompositionResults, netPositions, glsks,
            nodalInjectionComputer, dcNodalInjection);
    }

    private Map<String, Double> getDcNodalInjection(Network network,
                                                    FlowDecompositionResults flowDecompositionResults,
                                                    NetworkMatrixIndexes networkMatrixIndexes) {
        ReferenceNodalInjectionComputer referenceNodalInjectionComputer = new ReferenceNodalInjectionComputer(networkMatrixIndexes);
        Map<String, Double> dcNodalInjection = referenceNodalInjectionComputer.run(network, loadFlowParameters);
        return flowDecompositionResults.saveDcNodalInjections(dcNodalInjection);
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
        return flowDecompositionResults.saveNodalInjectionsMatrix(nodalInjectionsMatrix);
    }

    private SensitivityAnalyser getSensitivityAnalyser(Network network, NetworkMatrixIndexes networkMatrixIndexes) {
        return new SensitivityAnalyser(loadFlowParameters, parameters, network, networkMatrixIndexes);
    }

    private SparseMatrixWithIndexesTriplet getPtdfMatrix(FlowDecompositionResults flowDecompositionResults,
                                                         NetworkMatrixIndexes networkMatrixIndexes,
                                                         SensitivityAnalyser sensitivityAnalyser) {
        SparseMatrixWithIndexesTriplet ptdfMatrix =
            sensitivityAnalyser.run(networkMatrixIndexes.getNodeIdList(),
                networkMatrixIndexes.getNodeIndex(),
                SensitivityVariableType.INJECTION_ACTIVE_POWER);
        return flowDecompositionResults.savePtdfMatrix(ptdfMatrix);
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
        return flowDecompositionResults.savePsdfMatrix(psdfMatrix);
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
