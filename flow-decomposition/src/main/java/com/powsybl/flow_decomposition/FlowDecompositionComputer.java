/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.flow_decomposition;

import com.powsybl.commons.PowsyblException;
import com.powsybl.contingency.Contingency;
import com.powsybl.iidm.network.*;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.loadflow.LoadFlowResult;
import com.powsybl.sensitivity.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 */
public class FlowDecompositionComputer {
    static final boolean DC_LOAD_FLOW = true;
    private static final Logger LOGGER = LoggerFactory.getLogger(FlowDecompositionComputer.class);
    private final LoadFlowParameters dcLoadFlowParameters;
    private final FlowDecompositionParameters parameters;
    private final LoadFlowParameters acLoadFlowParameters;

    public FlowDecompositionComputer() {
        this(new FlowDecompositionParameters());
    }

    public FlowDecompositionComputer(FlowDecompositionParameters parameters) {
        this.parameters = parameters;
        this.dcLoadFlowParameters = initLoadFlowParameters();
        this.acLoadFlowParameters = dcLoadFlowParameters.copy()
            .setDc(false);
    }

    public FlowDecompositionResults run(Network network) {
        FlowDecompositionResults flowDecompositionResults = new FlowDecompositionResults(network, parameters);

        //AC LF
        Map<Country, Map<String, Double>> glsks = getGlsks(network, flowDecompositionResults);
        List<Contingency> contingencies = getContingencies(network, flowDecompositionResults);
        createVariantsWithContingencies(network, contingencies);
        Map<String, Map<Country, Double>> zonalPtdf = getZonalPtdf(network, glsks, flowDecompositionResults);
        Map<Branch, String> xnecList = getXnecList(network, zonalPtdf, flowDecompositionResults);
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

    private Map<Branch, String> getXnecList(Network network, Map<String, Map<Country, Double>> zonalPtdf, FlowDecompositionResults flowDecompositionResults) {
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
        Map<Branch, String> xnecList = xnecSelector.run(network);
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

    private void createVariantsWithContingencies(Network network, List<Contingency> contingencies) {
        String originVariant = network.getVariantManager().getWorkingVariantId();
        network.getVariantManager().cloneVariant(originVariant, contingencies.stream().map(Contingency::getId).collect(Collectors.toList()));
        contingencies.forEach(contingency -> {
            network.getVariantManager().setWorkingVariant(contingency.getId());
            contingency.toModification().apply(network);
            runAcLoadFlow(network);
        });
        network.getVariantManager().setWorkingVariant(originVariant);
        runAcLoadFlow(network);
    }

    private void runAcLoadFlow(Network network) {
        LoadFlowResult loadFlowResult = LoadFlow.run(network, acLoadFlowParameters);
        if (!loadFlowResult.isOk()) {
            LOGGER.error("AC Load Flow diverged !");
            //loadFlowResult = LoadFlow.run(network, dcLoadFlowParameters);
            //if (!loadFlowResult.isOk()) {
            //    throw new PowsyblException("Load Flow has diverged in AC and DC !");
            //}
        }
    }

    private List<Contingency> getContingencies(Network network, FlowDecompositionResults flowDecompositionResults) {
        return flowDecompositionResults.saveContingencies(network.getBranchStream()
            .sorted(Comparator.comparing(branch -> Math.abs(branch.getTerminal1().getP())))
            .limit(100)
            .map(Identifiable::getId)
            .map(branch -> Contingency.builder(branch).addBranch(branch).build())
            .collect(Collectors.toList()));
    }

    private Map<String, Map<Country, Double>> getZonalPtdf(Network network,
                                                           Map<Country, Map<String, Double>> glsks,
                                                           FlowDecompositionResults flowDecompositionResults) {
        ZonalSensitivityAnalyser zonalSensitivityAnalyser = new ZonalSensitivityAnalyser(dcLoadFlowParameters, parameters);
        Map<String, Map<Country, Double>> zonalPtdf = zonalSensitivityAnalyser.run(network,
            glsks, SensitivityVariableType.INJECTION_ACTIVE_POWER);
        return flowDecompositionResults.saveZonalPtdf(zonalPtdf);
    }

    private Map<Country, Double> getZonesNetPosition(Network network,
                                                     FlowDecompositionResults flowDecompositionResults) {
        NetPositionComputer netPositionComputer = new NetPositionComputer(dcLoadFlowParameters);
        Map<Country, Double> netPosition = netPositionComputer.run(network);
        return flowDecompositionResults.saveACNetPosition(netPosition);
    }

    private Map<String, Double> getXnecReferenceFlows(Map<Branch, String> xnecList) {
        ReferenceFlowComputer referenceFlowComputer = new ReferenceFlowComputer();
        return referenceFlowComputer.run(xnecList);
    }

    private void compensateLosses(Network network) {
        if (parameters.isLossesCompensationEnabled()) {
            LossesCompensator lossesCompensator = new LossesCompensator(dcLoadFlowParameters, parameters);
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
        Map<String, Double> dcNodalInjection = referenceNodalInjectionComputer.run(network, dcLoadFlowParameters);
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
        return new SensitivityAnalyser(dcLoadFlowParameters, parameters, network, networkMatrixIndexes);
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
