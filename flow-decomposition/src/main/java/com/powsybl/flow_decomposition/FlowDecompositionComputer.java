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
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.sensitivity.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.ObjDoubleConsumer;
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
        Map<String, Contingency> variantContingenciesMap = getVariantContingenciesMap(network, flowDecompositionResults);
        createVariantsWithContingencies(network, variantContingenciesMap);

        runAllAcLoadFlows(network);
        Map<String, Map<Country, Double>> zonalPtdf = getZonalPtdf(network, glsks, flowDecompositionResults);
        List<XnecWithDecomposition> xnecList = getXnecList(network, zonalPtdf, variantContingenciesMap, flowDecompositionResults);
        Map<String, Map<Country, Double>> netPositions = getZonesNetPosition(network, flowDecompositionResults);
        flowDecompositionResults.saveAcReferenceFlow(getXnecReferenceFlows(xnecList, network, DecomposedFlow::setAcReferenceFlow));

        compensateLosses(network);

        LoadFlowRunner dcLoadFlowRunner = new LoadFlowRunner(dcLoadFlowParameters);
        for (String variantId : network.getVariantManager().getVariantIds()) {
            network.getVariantManager().setWorkingVariant(variantId);
            List<XnecWithDecomposition> localXnecList = getLocalXnecList(xnecList, variantId);
            if (!localXnecList.isEmpty()) {
                NetworkMatrixIndexes networkMatrixIndexes = new NetworkMatrixIndexes(network, localXnecList);

                dcLoadFlowRunner.run(network);
                SparseMatrixWithIndexesTriplet nodalInjectionsMatrix = getNodalInjectionsMatrix(network,
                    flowDecompositionResults, netPositions.get(variantId), networkMatrixIndexes, glsks);
                flowDecompositionResults.saveDcReferenceFlow(getXnecReferenceFlows(localXnecList, network, DecomposedFlow::setDcReferenceFlow));

                SensitivityAnalyser sensitivityAnalyser = getSensitivityAnalyser(network, networkMatrixIndexes);
                SparseMatrixWithIndexesTriplet ptdfMatrix = getPtdfMatrix(flowDecompositionResults,
                    networkMatrixIndexes, sensitivityAnalyser);
                SparseMatrixWithIndexesTriplet psdfMatrix = getPsdfMatrix(flowDecompositionResults,
                    networkMatrixIndexes, sensitivityAnalyser);

                computeAllocatedAndLoopFlows(flowDecompositionResults, networkMatrixIndexes, nodalInjectionsMatrix, ptdfMatrix);
                computePstFlows(network, flowDecompositionResults, networkMatrixIndexes, psdfMatrix);
            }
        }

        rescale(xnecList);

        return flowDecompositionResults;
    }

    private List<XnecWithDecomposition> getXnecList(Network network,
                                                    Map<String, Map<Country, Double>> zonalPtdf,
                                                    Map<String, Contingency> variantContingenciesMap,
                                                    FlowDecompositionResults flowDecompositionResults) {
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
        return flowDecompositionResults.saveXnecsWithDecomposition(xnecSelector.run(network, variantContingenciesMap));
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

    private Map<String, Contingency> getVariantContingenciesMap(Network network, FlowDecompositionResults flowDecompositionResults) {
        ContingencyComputer contingencyComputer = new ContingencyComputer(parameters);
        return flowDecompositionResults.saveContingencies(contingencyComputer.run(network));
    }

    private void createVariantsWithContingencies(Network network, Map<String, Contingency> variantcontingencyMap) {
        VariantGenerator variantGenerator = new VariantGenerator();
        variantGenerator.run(network, variantcontingencyMap);
    }

    private void runAllAcLoadFlows(Network network) {
        LoadFlowRunner loadFlowRunner = new LoadFlowRunner(acLoadFlowParameters);
        loadFlowRunner.runAllVariants(network);
    }

    private Map<String, Map<Country, Double>> getZonalPtdf(Network network,
                                                           Map<Country, Map<String, Double>> glsks,
                                                           FlowDecompositionResults flowDecompositionResults) {
        ZonalSensitivityAnalyser zonalSensitivityAnalyser = new ZonalSensitivityAnalyser(dcLoadFlowParameters, parameters);
        Map<String, Map<Country, Double>> zonalPtdf = zonalSensitivityAnalyser.run(network,
            glsks, SensitivityVariableType.INJECTION_ACTIVE_POWER);
        return flowDecompositionResults.saveZonalPtdf(zonalPtdf);
    }

    private Map<String, Map<Country, Double>> getZonesNetPosition(Network network,
                                                                  FlowDecompositionResults flowDecompositionResults) {
        NetPositionComputer netPositionComputer = new NetPositionComputer();
        Map<String, Map<Country, Double>> netPosition = netPositionComputer.run(network);
        return flowDecompositionResults.saveACNetPosition(netPosition);
    }

    private Map<String, Double> getXnecReferenceFlows(List<XnecWithDecomposition> xnecList, Network network, ObjDoubleConsumer<DecomposedFlow> consumer) {
        ReferenceFlowComputer referenceFlowComputer = new ReferenceFlowComputer();
        return referenceFlowComputer.run(xnecList, network, consumer);
    }

    private void compensateLosses(Network network) {
        if (parameters.isLossesCompensationEnabled()) {
            LossesCompensator lossesCompensator = new LossesCompensator(parameters);
            lossesCompensator.run(network);
        }
    }

    private List<XnecWithDecomposition> getLocalXnecList(List<XnecWithDecomposition> xnecList, String variantId) {
        return xnecList.stream().filter(xnec -> Objects.equals(xnec.getVariantId(), variantId)).collect(Collectors.toList());
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

    private Map<String, Double> getDcNodalInjection(Network network, FlowDecompositionResults flowDecompositionResults,
                                                    NetworkMatrixIndexes networkMatrixIndexes) {
        ReferenceNodalInjectionComputer referenceNodalInjectionComputer = new ReferenceNodalInjectionComputer(networkMatrixIndexes);
        return flowDecompositionResults.saveDcNodalInjections(referenceNodalInjectionComputer.run(), network.getVariantManager().getWorkingVariantId());
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
        return flowDecompositionResults.saveNodalInjections(nodalInjectionsMatrix, network.getVariantManager().getWorkingVariantId());
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
        return flowDecompositionResults.savePtdf(ptdfMatrix);
    }

    private void computeAllocatedAndLoopFlows(FlowDecompositionResults flowDecompositionResults,
                                              NetworkMatrixIndexes networkMatrixIndexes,
                                              SparseMatrixWithIndexesTriplet nodalInjectionsMatrix,
                                              SparseMatrixWithIndexesTriplet ptdfMatrix) {
        SparseMatrixWithIndexesCSC allocatedLoopFlowsMatrix =
            SparseMatrixWithIndexesCSC.mult(ptdfMatrix.toCSCMatrix(), nodalInjectionsMatrix.toCSCMatrix());
        flowDecompositionResults.saveAllocatedAndLoopFlows(allocatedLoopFlowsMatrix);
        Map<String, Map<String, Double>> allocatedLoopFlowsMapMap = allocatedLoopFlowsMatrix.toMap();
        networkMatrixIndexes.getXnecList().forEach(xnec -> {
            String xnecId = xnec.getId();
            Map<String, Double> allocatedLoopFlowsMap = allocatedLoopFlowsMapMap.get(xnecId);
            double allocatedFlow = allocatedLoopFlowsMap
                .getOrDefault(DecomposedFlow.ALLOCATED_COLUMN_NAME, DecomposedFlow.DEFAULT_FLOW);
            Map<String, Double> loopFlowsMap = allocatedLoopFlowsMap.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith(NetworkUtil.LOOP_FLOWS_COLUMN_PREFIX))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
            DecomposedFlow decomposedFlow = xnec.getDecomposedFlowBeforeRescaling();
            decomposedFlow.setAllocatedFlow(allocatedFlow);
            decomposedFlow.setLoopFlow(loopFlowsMap);
        });
    }

    private SparseMatrixWithIndexesTriplet getPsdfMatrix(FlowDecompositionResults flowDecompositionResults,
                                                         NetworkMatrixIndexes networkMatrixIndexes,
                                                         SensitivityAnalyser sensitivityAnalyser) {
        SparseMatrixWithIndexesTriplet psdfMatrix =
            sensitivityAnalyser.run(networkMatrixIndexes.getPstList(),
                networkMatrixIndexes.getPstIndex(), SensitivityVariableType.TRANSFORMER_PHASE);
        return flowDecompositionResults.savePsdf(psdfMatrix);
    }

    private void computePstFlows(Network network,
                                 FlowDecompositionResults flowDecompositionResults,
                                 NetworkMatrixIndexes networkMatrixIndexes,
                                 SparseMatrixWithIndexesTriplet psdfMatrix) {
        PstFlowComputer pstFlowComputer = new PstFlowComputer();
        SparseMatrixWithIndexesCSC pstFlowMatrix = pstFlowComputer.run(network, networkMatrixIndexes, psdfMatrix);
        flowDecompositionResults.savePstFlows(pstFlowMatrix);
        Map<String, Map<String, Double>> pstFlowsMapMap = pstFlowMatrix.toMap();
        networkMatrixIndexes.getXnecList().forEach(xnec -> {
            String xnecId = xnec.getId();
            Map<String, Double> pstFlowMap = pstFlowsMapMap.getOrDefault(xnecId, Collections.emptyMap());
            double pstFlow = pstFlowMap.getOrDefault(DecomposedFlow.PST_COLUMN_NAME, DecomposedFlow.DEFAULT_FLOW);
            DecomposedFlow decomposedFlow = xnec.getDecomposedFlowBeforeRescaling();
            decomposedFlow.setPstFlow(pstFlow);
        });
    }

    private void rescale(List<XnecWithDecomposition> xnecList) {
        DecomposedFlowsRescaler decomposedFlowsRescaler;
        if (parameters.isRescaleEnabled()) {
            decomposedFlowsRescaler = new DecomposedFlowsRescalerACER();
        } else {
            decomposedFlowsRescaler = new DecomposedFlowsRescalerIdentity();
        }
        decomposedFlowsRescaler.rescale(xnecList);
    }
}
