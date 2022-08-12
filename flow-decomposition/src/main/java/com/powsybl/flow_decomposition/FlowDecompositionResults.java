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

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * This class provides flow decomposition results from a network.
 * Those results are returned by a flowDecompositionComputer when run on a network.
 * By default, the results only contain the flow decomposition of the XNECs.
 * If the flow decomposition parameter has its argument {@code saveIntermediates} set to {@code true},
 * then the results will contain supplementary information.
 *
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 * @see FlowDecompositionComputer
 * @see DecomposedFlow
 */
public class FlowDecompositionResults {
    public static final XnecWithDecomposition DEFAULT_XNEC_WITH_DECOMPOSITION = null;
    private final boolean saveIntermediates;
    private final String id;
    private final String networkId;
    private List<XnecWithDecomposition> xnecsWithDecomposition;
    private Map<String, XnecWithDecomposition> xnecsWithDecompositionMap;
    private Map<String, Contingency> contingencies;
    private Map<String, Double> acReferenceFlows;
    private Map<String, Double> dcReferenceFlows;
    private Map<String, Map<Country, Double>> acNetPositions;
    private Map<Country, Map<String, Double>> glsks;
    private Map<String, Map<Country, Double>> zonalPtdf;
    private Map<String, Map<String, Double>> nodalPtdf;
    private Map<String, Map<String, Double>> psdf;
    private Map<String, Map<String, Map<String, Double>>> nodalInjections;
    private Map<String, Map<String, Double>> dcNodalInjections;
    private Map<String, Map<String, Double>> allocatedAndLoopFlows;
    private Map<String, Map<String, Double>> pstFlows;

    FlowDecompositionResults(Network network, FlowDecompositionParameters parameters) {
        this.saveIntermediates = parameters.doesSaveIntermediates();
        this.networkId = network.getNameOrId();
        String date = new SimpleDateFormat("yyyyMMdd-HHmmss").format(Date.from(Instant.now()));
        this.id = "Flow_Decomposition_Results_of_" + date + "_on_network_" + networkId;
        if (saveIntermediates) {
            contingencies = new HashMap<>();
            acReferenceFlows = new HashMap<>();
            dcReferenceFlows = new HashMap<>();
            acNetPositions = new HashMap<>();
            glsks = new EnumMap<>(Country.class);
            zonalPtdf = new HashMap<>();
            nodalPtdf = new HashMap<>();
            psdf = new HashMap<>();
            nodalInjections = new HashMap<>();
            dcNodalInjections = new HashMap<>();
            allocatedAndLoopFlows = new HashMap<>();
            pstFlows = new HashMap<>();
        }
    }

    /**
     * @return Id composed of a time format and the network id.
     */
    public String getId() {
        return id;
    }

    /**
     * @return Network Id
     */
    public String getNetworkId() {
        return networkId;
    }

    /**
     * @return the set of available zones in this result
     */
    public Set<Country> getZoneSet() {
        Set<Country> countries = getZoneSet(Xnec::getCountryTerminal1);
        countries.addAll(getZoneSet(Xnec::getCountryTerminal2));
        return countries;
    }

    /**
     * @return The Xnecs with their decomposition as a list
     */
    public List<XnecWithDecomposition> getXnecsWithDecomposition() {
        return xnecsWithDecomposition;
    }

    /**
     * Return the selected Xnec with its decomposition
     * @param xnecId a xnec id
     * @return Xnec with its decomposition
     */
    public XnecWithDecomposition get(String xnecId) {
        return xnecsWithDecompositionMap.getOrDefault(xnecId, DEFAULT_XNEC_WITH_DECOMPOSITION);
    }

    /**
     * Contingency list is an intermediate result
     * They will be saved if the flow decomposition parameter has its argument {@code saveIntermediates} set to {@code true}.
     * @return An optional containing the contingencies
     */
    public Optional<Map<String, Contingency>> getContingencies() {
        return Optional.ofNullable(contingencies);
    }

    /**
     * AC reference flow are an intermediate result.
     * They are represented as a sparse map.
     * The first key is a xnec id and the value is the AC reference flow of this xnec.
     * They will be saved if the flow decomposition parameter has its argument {@code saveIntermediates} set to {@code true}.
     *
     * @return An optional containing AC reference flows.
     */
    public Optional<Map<String, Double>> getAcReferenceFlows() {
        return Optional.ofNullable(acReferenceFlows);
    }

    /**
     * DC reference flow are an intermediate result.
     * They are represented as a sparse map.
     * The first key is a xnec id and the value is the DC reference flow of this xnec.
     * They will be saved if the flow decomposition parameter has its argument {@code saveIntermediates} set to {@code true}.
     *
     * @return An optional containing DC reference flows.
     */
    public Optional<Map<String, Double>> getDcReferenceFlows() {
        return Optional.ofNullable(dcReferenceFlows);
    }

    /**
     * AC Net positions are an intermediate result.
     * They are represented as a sparse map of map.
     * The first key is a variant id, the second key is a country and the value is the net position of the country given the variant.
     * They will be saved if the flow decomposition parameter has its argument {@code saveIntermediates} set to {@code true}.
     *
     * @return An optional containing Net positions.
     */
    public Optional<Map<String, Map<Country, Double>>> getAcNetPositions() {
        return Optional.ofNullable(acNetPositions);
    }

    /**
     * GLSKs are an intermediate result.
     * They are represented as a sparse map of map.
     * The first key is a zone, the second key is a node id and the value is the GLSK of the node in the country.
     * They will be saved if the flow decomposition parameter has its argument {@code saveIntermediates} set to {@code true}.
     * @return An optional containing GLSKs.
     */
    public Optional<Map<Country, Map<String, Double>>> getGlsks() {
        return Optional.ofNullable(glsks);
    }

    /**
     * Zonal PTDFs are an intermediate result.
     * They will be saved if the flow decomposition parameter has its argument {@code saveIntermediates} set to {@code true}.
     * They are represented as a map of map.
     * The first key is a XNEC id, the second key is the country and the value is the PTDF.
     * @return An optional containing PTDFs
     */
    public Optional<Map<String, Map<Country, Double>>> getZonalPtdf() {
        return Optional.ofNullable(zonalPtdf);
    }

    /**
     * PTDFs are an intermediate result.
     * They will be saved if the flow decomposition parameter has its argument {@code saveIntermediates} set to {@code true}.
     * They are represented as a sparse map of map.
     * The first key is a XNEC id, the second key is a node id and the value is the PTDF.
     *
     * @return An optional containing PTDFs
     */
    public Optional<Map<String, Map<String, Double>>> getNodalPtdf() {
        return Optional.ofNullable(nodalPtdf);
    }

    /**
     * PSDFs are an intermediate result.
     * They will be saved if the flow decomposition parameter has its argument {@code saveIntermediates} set to {@code true}.
     * They are represented as a sparse map of map.
     * The first key is a XNEC id, the second key is a node id and the value is the PSDF.
     *
     * @return An optional containing PSDFs
     */
    public Optional<Map<String, Map<String, Double>>> getPsdf() {
        return Optional.ofNullable(psdf);
    }

    /**
     * Nodal injections are an intermediate result.
     * They will be saved if the flow decomposition parameter has its argument {@code saveIntermediates} set to {@code true}.
     * They are represented as a sparse map of map of map.
     * The first key is a variant id, the second key is a node id, the third key is a column identifier and the value is the nodal injection.
     * The one of the column id is the {@code "Allocated Flow"}. It corresponds to the allocated nodal injection.
     * The other column ids are Zone Ids as Strings with a prefix {@code "Loop Flow from XX"}.
     * Each column corresponds to the nodal injection in this zone.
     *
     * @return An optional containing nodal injections
     */
    public Optional<Map<String, Map<String, Map<String, Double>>>> getNodalInjections() {
        return Optional.ofNullable(nodalInjections);
    }

    /**
     * DC Nodal injections are an intermediate result.
     * They will be saved if the flow decomposition parameter has its argument {@code saveIntermediates} set to {@code true}.
     * They are represented as a map of map.
     * The first key is a variant id, the second key is a node id and the value is the DC nodal injection.
     *
     * @return An optional containing DC nodal injections
     */
    public Optional<Map<String, Map<String, Double>>> getDcNodalInjections() {
        return Optional.ofNullable(dcNodalInjections);
    }

    /**
     * Allocated and loop flows are an intermediate result.
     * They will be saved if the flow decomposition parameter has its argument {@code saveIntermediates} set to {@code true}.
     * They are represented as a map of map.
     * The first key is a xnec id, the second key is a column identifier and the value is a decomposed flow.
     * The one of the column id is the {@code "Allocated Flow"}. It corresponds to the allocated flow.
     * The other column ids are Zone Ids as Strings with a prefix {@code "Loop Flow from XX"}. They correspond to loop flows.
     *
     * @return An optional containing the allocated flow and the loop flows.
     */
    public Optional<Map<String, Map<String, Double>>> getAllocatedAndLoopFlows() {
        return Optional.ofNullable(allocatedAndLoopFlows);
    }

    /**
     * Pst flows are an intermediate result.
     * They will be saved if the flow decomposition parameter has its argument {@code saveIntermediates} set to {@code true}.
     * They are represented as a map of map.
     * The first key is a xnec id, the second key is a column identifier and the value is a pst flow.
     * The column identifier is {@code "Pst Flow"}. It corresponds to the PST flow.
     *
     * @return An optional containing the Pst flows.
     */
    public Optional<Map<String, Map<String, Double>>> getPstFlows() {
        return Optional.ofNullable(pstFlows);
    }

    void saveXnecsWithDecomposition(List<XnecWithDecomposition> xnecsWithDecomposition) {
        this.xnecsWithDecomposition = xnecsWithDecomposition;
        this.xnecsWithDecompositionMap = xnecsWithDecomposition.stream().collect(Collectors.toMap(Xnec::getId, Function.identity()));
    }

    void saveContingencies(Map<String, Contingency> contingencies) {
        if (saveIntermediates) {
            this.contingencies = contingencies;
        }
    }

    void saveAcReferenceFlow(Map<String, Double> acReferenceFlows) {
        if (saveIntermediates) {
            this.acReferenceFlows = acReferenceFlows;
        }
    }

    void saveDcReferenceFlow(Map<String, Double> dcReferenceFlows) {
        if (saveIntermediates) {
            this.dcReferenceFlows.putAll(dcReferenceFlows);
        }
    }

    void saveACNetPosition(Map<String, Map<Country, Double>> acNetPositions) {
        if (saveIntermediates) {
            this.acNetPositions = acNetPositions;
        }
    }

    void saveGlsks(Map<Country, Map<String, Double>> glsks) {
        if (saveIntermediates) {
            this.glsks = glsks;
        }
    }

    void saveZonalPtdf(Map<String, Map<Country, Double>> zonalPtdf) {
        if (saveIntermediates) {
            this.zonalPtdf = zonalPtdf;
        }
    }

    void savePtdf(SparseMatrixWithIndexesTriplet ptdfMatrix) {
        if (saveIntermediates) {
            this.nodalPtdf.putAll(ptdfMatrix.toMap());
        }
    }

    void savePsdf(SparseMatrixWithIndexesTriplet psdfMatrix) {
        if (saveIntermediates) {
            this.psdf.putAll(psdfMatrix.toMap());
        }
    }

    void saveNodalInjections(SparseMatrixWithIndexesTriplet nodalInjectionsMatrix, String workingVariantId) {
        if (saveIntermediates) {
            this.nodalInjections.put(workingVariantId, nodalInjectionsMatrix.toMap());
        }
    }

    void saveDcNodalInjections(Map<String, Double> dcNodalInjections, String workingVariantId) {
        if (saveIntermediates) {
            this.dcNodalInjections.put(workingVariantId, dcNodalInjections);
        }
    }

    void saveAllocatedAndLoopFlows(SparseMatrixWithIndexesCSC allocatedAndLoopFlowsMatrix) {
        if (saveIntermediates) {
            this.allocatedAndLoopFlows.putAll(allocatedAndLoopFlowsMatrix.toMap());
        }
    }

    void savePstFlows(SparseMatrixWithIndexesCSC pstFlowMatrix) {
        if (saveIntermediates) {
            this.pstFlows.putAll(pstFlowMatrix.toMap());
        }
    }

    private Set<Country> getZoneSet(Function<Xnec, Country> map) {
        return xnecsWithDecomposition.stream().map(map).collect(Collectors.toSet());
    }
}
