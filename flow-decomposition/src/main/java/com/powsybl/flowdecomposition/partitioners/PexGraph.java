/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.flowdecomposition.partitioners;

import com.powsybl.commons.PowsyblException;
import com.powsybl.flowdecomposition.NetworkUtil;
import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Bus;
import org.jgrapht.graph.DirectedMultigraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Business object for PEX graph
 *
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
public class PexGraph extends DirectedMultigraph<PexGraphVertex, PexGraphEdge> {
    private static final InjectionStrategy DEFAULT_INJECTION_STRATEGY = InjectionStrategy.SUM_INJECTIONS;
    private static final Logger LOGGER = LoggerFactory.getLogger(PexGraph.class);
    public static final double EPSILON_EDGE_POWER = 1e-5;
    public static final double EPSILON_VERTEX_POWER = 1e-3;

    private final Map<Bus, PexGraphVertex> vertexPerBus = new HashMap<>();

    public PexGraph(List<Bus> buses, List<Branch<?>> branches) {
        this(buses, branches, DEFAULT_INJECTION_STRATEGY);
    }

    public PexGraph(List<Bus> buses, List<Branch<?>> branches, InjectionStrategy injectionStrategy) {
        super(PexGraphEdge.class);

        buses.forEach(bus -> addBusAsVertex(bus, injectionStrategy));
        branches.forEach(this::addBranchAsEdge);
        buses.forEach(this::addXNodesAsVertexAndEdges);
        checkGraph();
    }

    private void addBusAsVertex(Bus bus, InjectionStrategy injectionStrategy) {
        assert bus != null;
        PexGraphVertex vertex = new PexGraphVertex(bus, injectionStrategy);
        addVertex(vertex);
        vertexPerBus.put(bus, vertex);
    }

    private void addBranchAsEdge(Branch<?> branch) {
        assert branch != null;

        Bus bus1 = branch.getTerminal1().getBusView().getBus();
        Bus bus2 = branch.getTerminal2().getBusView().getBus();

        if (Double.isNaN(branch.getTerminal1().getP())) {
            LOGGER.debug("Branch {} filtered because of a flow NA", branch.getId());
        } else if (Math.abs(branch.getTerminal1().getP()) < EPSILON_EDGE_POWER) {
            LOGGER.debug("Branch {} filtered because of a flow too low : {} MW", branch.getId(), branch.getTerminal1().getP());
        } else {
            if (branch.getTerminal1().getP() > 0) {
                addEdge(vertexPerBus.get(bus1), vertexPerBus.get(bus2), new PexGraphEdge(branch));
            } else {
                addEdge(vertexPerBus.get(bus2), vertexPerBus.get(bus1), new PexGraphEdge(branch));
            }
        }
    }

    private void addXNodesAsVertexAndEdges(Bus bus) {
        NetworkUtil.getUnpairedXNodeStream(bus).forEach(boundaryLine -> {
            if (Double.isNaN(boundaryLine.getTerminal().getP())) {
                LOGGER.debug("Unpaired boundary line {} filtered because of a flow NA", boundaryLine.getId());
            } else if (Math.abs(boundaryLine.getTerminal().getP()) < EPSILON_EDGE_POWER) {
                LOGGER.debug("Unpaired boundary line {} filtered because of a flow too low : {} MW", boundaryLine.getId(), boundaryLine.getTerminal().getP());
            } else {
                PexGraphVertex v = new PexGraphVertex(boundaryLine);
                addVertex(v);
                PexGraphEdge edge = new PexGraphEdge(boundaryLine);
                if (boundaryLine.getTerminal().getP() > 0) {
                    addEdge(vertexPerBus.get(bus), v, edge);
                } else {
                    addEdge(v, vertexPerBus.get(bus), edge);
                }
            }
        });
    }

    private void checkGraph() {
        for (PexGraphVertex vertex : vertexSet()) {
            double nodalGeneration = vertex.getAssociatedGeneration() + incomingEdgesOf(vertex).stream()
                .mapToDouble(PexGraphEdge::getAssociatedFlow).sum();

            double nodalLoad = vertex.getAssociatedLoad() + outgoingEdgesOf(vertex).stream()
                .mapToDouble(PexGraphEdge::getAssociatedFlow).sum();

            if (Math.abs(nodalGeneration - nodalLoad) > EPSILON_VERTEX_POWER) {
                throw new PowsyblException("Nodal generation and load do not match for vertex associated with bus: " + vertex.getId());
            }
        }
    }

    public enum InjectionStrategy {
        SUM_INJECTIONS,
        DECOMPOSE_INJECTIONS
    }
}
