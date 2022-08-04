/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.flow_decomposition;

import com.powsybl.contingency.Contingency;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import org.apache.commons.math3.util.Pair;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * This class provides flow decomposition results from a network.
 * Those results are returned by a flowDecompositionComputer when run on a network.
 * By default, the results only contain the flow decomposition of the XNECs.
 * If this runner has its argument {@code saveIntermediates} set to {@code true},
 * then the results will contain supplementary information.
 *
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 * @see FlowDecompositionComputer
 * @see DecomposedFlow
 */
public class FlowDecompositionResults {
    private final boolean saveIntermediates;
    private final String id;
    private final String networkId;
    private final Map<String, Map<String, Double>> allocatedAndLoopFlowsMatrixMap;
    private final Map<String, Map<String, Double>> pstFlowMap;
    private Map<String, Double> acReferenceFlow;
    private final Map<String, Double> dcReferenceFlow;
    private Map<String, Map<Country, Double>> acNetPosition;
    private Map<Country, Map<String, Double>> glsks;
    private Map<String, Map<Country, Double>> zonalPtdf;
    private Map<String, Map<String, Double>> ptdfMatrixMap;
    private Map<String, Map<String, Double>> psdfMatrixMap;
    private Map<String, Map<String, Map<String, Double>>> nodalInjectionsMatrixMap;
    private Map<String, Map<String, Double>> dcNodalInjections;
    private Map<String, DecomposedFlow> decomposedFlowsMapBeforeRescaling;
    private Map<String, DecomposedFlow> decomposedFlowMapAfterRescaling;
    private Map<String, Pair<Country, Country>> xnecToCountryMap;
    private List<Contingency> contingencies;

    FlowDecompositionResults(Network network, FlowDecompositionParameters parameters) {
        this.saveIntermediates = parameters.doesSaveIntermediates();
        this.networkId = network.getNameOrId();
        String date = new SimpleDateFormat("yyyyMMdd-HHmmss").format(Date.from(Instant.now()));
        this.id = "Flow_Decomposition_Results_of_" + date + "_on_network_" + networkId;
        dcReferenceFlow = new HashMap<>();
        int numberOfVariants = network.getVariantManager().getVariantIds().size();
        pstFlowMap = new HashMap<>();
        allocatedAndLoopFlowsMatrixMap = new HashMap<>();
        if (saveIntermediates) {
            dcNodalInjections = new HashMap<>(numberOfVariants);
            nodalInjectionsMatrixMap = new HashMap<>(numberOfVariants);
            ptdfMatrixMap = new HashMap<>();
            psdfMatrixMap = new HashMap<>();
        }
    }

    /**
     * @return Network Id
     */
    public String getNetworkId() {
        return networkId;
    }

    /**
     * @return Id composed of a time format and the network id.
     */
    public String getId() {
        return id;
    }

    /**
     * @return the set of available zones in this result
     */
    public Set<Country> getZoneSet() {
        Set<Country> countries = getZoneSet(Pair::getFirst);
        countries.addAll(getZoneSet(Pair::getSecond));
        return countries;
    }

    private Set<Country> getZoneSet(Function<Pair<Country, Country>, Country> map) {
        return xnecToCountryMap.values().stream().map(map).collect(Collectors.toSet());
    }

    /**
     * @return A flow decomposition map. The keys are the XNEC ids and the values are {@code DecomposedFlow} objects.
     */
    public Map<String, DecomposedFlow> getDecomposedFlowMapBeforeRescaling() {
        if (!isDecomposedFlowMapCacheValid()) {
            initializeDecomposedFlowMapCache();
        }
        return decomposedFlowsMapBeforeRescaling;
    }

    /**
     * @return A rescaled flow decomposition map. The keys are the XNEC ids and the values are {@code DecomposedFlow} objects. This object is dense.
     */
    public Map<String, DecomposedFlow> getDecomposedFlowMap() {
        return decomposedFlowMapAfterRescaling;
    }

    /**
     * GLSKs are an intermediate result.
     * They are represented as a sparse map of map.
     * The first key is a zone, the second key is a node id and the value is the GLSK of the node in the country.
     * They will be saved if this runner has its argument {@code saveIntermediates} set to {@code true}.
     *
     * @return An optional containing GLSKs.
     */
    public Optional<Map<String, Map<Country, Double>>> getAcNetPositions() {
        return Optional.ofNullable(acNetPosition);
    }

    /**
     * GLSKs are an intermediate result.
     * They are represented as a sparse map of map.
     * The first key is a zone, the second key is a node id and the value is the GLSK of the node in the country.
     * They will be saved if this runner has its argument {@code saveIntermediates} set to {@code true}.
     * @return An optional containing GLSKs.
     */
    public Optional<Map<Country, Map<String, Double>>> getGlsks() {
        return Optional.ofNullable(glsks);
    }

    /**
     * Zonal PTDFs are an intermediate result.
     * They will be saved if this runner has its argument {@code saveIntermediates} set to {@code true}.
     * They are represented as a map of map.
     * The first key is a XNEC id, the second key is the country and the value is the PTDF.
     * @return An optional containing PTDFs
     */
    public Optional<Map<String, Map<Country, Double>>> getZonalPtdf() {
        return Optional.ofNullable(zonalPtdf);
    }

    /**
     * PTDFs are an intermediate result.
     * They will be saved if this runner has its argument {@code saveIntermediates} set to {@code true}.
     * They are represented as a sparse map of map.
     * The first key is a XNEC id, the second key is a node id and the value is the PTDF.
     *
     * @return An optional containing PTDFs
     */
    public Optional<Map<String, Map<String, Double>>> getPtdfMap() {
        return Optional.ofNullable(ptdfMatrixMap);
    }

    /**
     * PSDFs are an intermediate result.
     * They will be saved if this runner has its argument {@code saveIntermediates} set to {@code true}.
     * They are represented as a sparse map of map.
     * The first key is a XNEC id, the second key is a node id and the value is the PSDF.
     *
     * @return An optional containing PSDFs
     */
    public Optional<Map<String, Map<String, Double>>> getPsdfMap() {
        return Optional.ofNullable(psdfMatrixMap);
    }

    /**
     * Nodal injections are an intermediate result.
     * They will be saved if this runner has its argument {@code saveIntermediates} set to {@code true}.
     * They are represented as a sparse map of map.
     * The first key is a node id, the second key is a column identifier and the value is the nodal injection.
     * The one of the column id is the {@code "Allocated Flow"}. It corresponds to the allocated nodal injection.
     * The other column ids are Zone Ids as Strings with a prefix {@code "Loop Flow from XX"}.
     * Each column corresponds to the nodal injection in this zone.
     *
     * @return An optional containing nodal injections
     */
    public Optional<Map<String, Map<String, Map<String, Double>>>> getAllocatedAndLoopFlowNodalInjectionsMap() {
        return Optional.ofNullable(nodalInjectionsMatrixMap);
    }

    /**
     * DC Nodal injections are an intermediate result.
     * They will be saved if this runner has its argument {@code saveIntermediates} set to {@code true}.
     * They are represented as a map.
     * The key is a node id and the value is the DC nodal injection.
     *
     * @return An optional containing DC nodal injections
     */
    public Optional<Map<String, Map<String, Double>>> getDcNodalInjectionsMap() {
        return Optional.ofNullable(dcNodalInjections);
    }

    /**
     * Contingency list is an intermediate result
     * They will be saved if this runner has its argument {@code saveIntermediates} set to {@code true}.
     * @return An optional containing the contingencies
     */
    public Optional<List<Contingency>> getContingencies() {
        return Optional.ofNullable(contingencies);
    }

    private boolean isDecomposedFlowMapCacheValid() {
        return Objects.nonNull(decomposedFlowsMapBeforeRescaling);
    }

    private void initializeDecomposedFlowMapCache() {
        invalidateDecomposedFlowMapCache();
        decomposedFlowsMapBeforeRescaling = new TreeMap<>();
        allocatedAndLoopFlowsMatrixMap
            .forEach((xnecId, decomposedFlow) -> decomposedFlowsMapBeforeRescaling.put(xnecId, createDecomposedFlow(xnecId, decomposedFlow)));
    }

    private void invalidateDecomposedFlowMapCache() {
        this.decomposedFlowsMapBeforeRescaling = null;
    }

    private DecomposedFlow createDecomposedFlow(String xnecId, Map<String, Double> allocatedAndLoopFlowMap) {
        Map<String, Double> loopFlowsMap = allocatedAndLoopFlowMap.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith(NetworkUtil.LOOP_FLOWS_COLUMN_PREFIX))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        double allocatedFlow = allocatedAndLoopFlowMap.get(DecomposedFlow.ALLOCATED_COLUMN_NAME);
        Double pstFlow = pstFlowMap.getOrDefault(xnecId, Collections.emptyMap()).getOrDefault(DecomposedFlow.PST_COLUMN_NAME, DecomposedFlow.DEFAULT_FLOW);
        return new DecomposedFlow(loopFlowsMap, allocatedFlow, pstFlow,
            acReferenceFlow.get(xnecId), dcReferenceFlow.get(xnecId), xnecToCountryMap.get(xnecId));
    }

    void saveAllocatedAndLoopFlowsMatrix(SparseMatrixWithIndexesCSC allocatedAndLoopFlowsMatrix) {
        this.allocatedAndLoopFlowsMatrixMap.putAll(allocatedAndLoopFlowsMatrix.toMap());
        invalidateDecomposedFlowMapCache();
    }

    void savePstFlowMatrix(SparseMatrixWithIndexesCSC pstFlowMatrix) {
        this.pstFlowMap.putAll(pstFlowMatrix.toMap());
        invalidateDecomposedFlowMapCache();
    }

    void saveAcReferenceFlow(Map<String, Double> acReferenceFlow) {
        this.acReferenceFlow = acReferenceFlow;
        invalidateDecomposedFlowMapCache();
    }

    void saveDcReferenceFlow(Map<String, Double> dcReferenceFlow) {
        this.dcReferenceFlow.putAll(dcReferenceFlow);
        invalidateDecomposedFlowMapCache();
    }

    void saveRescaledDecomposedFlowMap(Map<String, DecomposedFlow> decomposedFlowMap) {
        this.decomposedFlowMapAfterRescaling = decomposedFlowMap;
    }

    Map<String, Map<Country, Double>> saveACNetPosition(Map<String, Map<Country, Double>> acNetPosition) {
        if (saveIntermediates) {
            this.acNetPosition = acNetPosition;
        }
        return acNetPosition;
    }

    Map<Country, Map<String, Double>> saveGlsks(Map<Country, Map<String, Double>> glsks) {
        if (saveIntermediates) {
            this.glsks = glsks;
        }
        return glsks;
    }

    SparseMatrixWithIndexesTriplet savePtdfMatrix(SparseMatrixWithIndexesTriplet ptdfMatrix) {
        if (saveIntermediates) {
            this.ptdfMatrixMap.putAll(ptdfMatrix.toMap());
        }
        return ptdfMatrix;
    }

    SparseMatrixWithIndexesTriplet savePsdfMatrix(SparseMatrixWithIndexesTriplet psdfMatrix) {
        if (saveIntermediates) {
            this.psdfMatrixMap.putAll(psdfMatrix.toMap());
        }
        return psdfMatrix;
    }

    SparseMatrixWithIndexesTriplet saveNodalInjectionsMatrix(SparseMatrixWithIndexesTriplet nodalInjectionsMatrix, String workingVariantId) {
        if (saveIntermediates) {
            this.nodalInjectionsMatrixMap.put(workingVariantId, nodalInjectionsMatrix.toMap());
        }
        return nodalInjectionsMatrix;
    }

    Map<String, Double> saveDcNodalInjections(Map<String, Double> dcNodalInjections, String workingVariantId) {
        if (saveIntermediates) {
            this.dcNodalInjections.put(workingVariantId, dcNodalInjections);
        }
        return dcNodalInjections;
    }

    Map<String, Map<Country, Double>> saveZonalPtdf(Map<String, Map<Country, Double>> zonalPtdf) {
        if (saveIntermediates) {
            this.zonalPtdf = zonalPtdf;
        }
        return zonalPtdf;
    }

    void saveXnecToCountry(Map<String, Pair<Country, Country>> xnecToCountry) {
        this.xnecToCountryMap = xnecToCountry;
    }

    List<Contingency> saveContingencies(List<Contingency> contingencies) {
        if (saveIntermediates) {
            this.contingencies = contingencies;
        }
        return contingencies;
    }
}
