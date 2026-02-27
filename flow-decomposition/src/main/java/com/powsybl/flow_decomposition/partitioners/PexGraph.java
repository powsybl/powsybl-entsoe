/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.flow_decomposition.partitioners;

import com.powsybl.commons.PowsyblException;
import com.powsybl.flow_decomposition.NetworkUtil;
import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Bus;
import org.jgrapht.graph.DirectedMultigraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Vertex business object in PEX graph
 * Stands for network buses
 *
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
class PexGraphVertex {
    private final Bus associatedBus;
    private final double associatedGeneration;
    private final double associatedLoad;

    PexGraphVertex(Bus associatedBus, PexGraph.InjectionStrategy injectionStrategy) {
        this.associatedBus = Objects.requireNonNull(associatedBus);

        double totalGeneration = -NetworkUtil.getInjectionStream(associatedBus)
            .mapToDouble(injection -> injection.getTerminal().getP())
            .filter(d -> !Double.isNaN(d))
            .filter(d -> d < 0)
            .sum();

        double totalLoad = NetworkUtil.getInjectionStream(associatedBus)
            .mapToDouble(injection -> injection.getTerminal().getP())
            .filter(d -> !Double.isNaN(d))
            .filter(d -> d > 0)
            .sum();
        this.associatedGeneration = switch (injectionStrategy) {
            case PexGraph.InjectionStrategy.SUM_INJECTIONS ->
                totalGeneration > totalLoad ? totalGeneration - totalLoad : 0;
            case PexGraph.InjectionStrategy.DECOMPOSE_INJECTIONS -> totalGeneration;
        };
        this.associatedLoad = switch (injectionStrategy) {
            case PexGraph.InjectionStrategy.SUM_INJECTIONS ->
                totalLoad > totalGeneration ? totalLoad - totalGeneration : 0;
            case PexGraph.InjectionStrategy.DECOMPOSE_INJECTIONS -> totalLoad;
        };
    }

    double getAssociatedLoad() {
        return associatedLoad;
    }

    double getAssociatedGeneration() {
        return associatedGeneration;
    }

    Bus getAssociatedBus() {
        return associatedBus;
    }

    @Override
    public String toString() {
        return associatedBus.getId();
    }
}

/**
 * Edge business object in PEX graph
 * Stands for network branches
 *
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
class PexGraphEdge {
    private final Branch<?> associatedBranch;

    PexGraphEdge(Branch<?> associatedBranch) {
        this.associatedBranch = Objects.requireNonNull(associatedBranch);
    }

    double getAssociatedFlow() {
        if (Double.isNaN(associatedBranch.getTerminal1().getP())) {
            return 0.;
        } else {
            return Math.abs(associatedBranch.getTerminal1().getP());
        }
    }

    Branch<?> getAssociatedBranch() {
        return associatedBranch;
    }

    @Override
    public String toString() {
        return associatedBranch.getId();
    }
}

/**
 * Business object for PEX graph
 *
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
public class PexGraph extends DirectedMultigraph<PexGraphVertex, PexGraphEdge> {
    private static final InjectionStrategy DEFAULT_INJECTION_STRATEGY = InjectionStrategy.SUM_INJECTIONS;
    private static final Logger LOGGER = LoggerFactory.getLogger(PexGraph.class);

    private final Map<Bus, PexGraphVertex> vertexPerBus = new HashMap<>();

    public PexGraph(List<Bus> buses, List<Branch<?>> branches) {
        this(buses, branches, DEFAULT_INJECTION_STRATEGY);
    }

    public PexGraph(List<Bus> buses, List<Branch<?>> branches, InjectionStrategy injectionStrategy) {
        super(PexGraphEdge.class);

        buses.forEach(bus -> addBusAsVertex(bus, injectionStrategy));
        branches.forEach(this::addBranchAsEdge);
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

        if (Math.abs(branch.getTerminal1().getP()) < 1e-5) {
            // To avoid possibe cycles, remove 0 transfer lines
            LOGGER.debug("Branch {} filtered because of a flow too low : {} MW", branch.getId(), branch.getTerminal1().getP());
        } else if (branch.getTerminal1().getP() > 0) {
            addEdge(vertexPerBus.get(bus1), vertexPerBus.get(bus2), new PexGraphEdge(branch));
        } else {
            addEdge(vertexPerBus.get(bus2), vertexPerBus.get(bus1), new PexGraphEdge(branch));
        }
    }

    private void checkGraph() {
        for (PexGraphVertex vertex : vertexSet()) {
            double nodalGeneration = vertex.getAssociatedGeneration() + incomingEdgesOf(vertex).stream()
                .mapToDouble(PexGraphEdge::getAssociatedFlow).sum();

            double nodalLoad = vertex.getAssociatedLoad() + outgoingEdgesOf(vertex).stream()
                .mapToDouble(PexGraphEdge::getAssociatedFlow).sum();

            if (Math.abs(nodalGeneration - nodalLoad) > 1e-3) {
                throw new PowsyblException("Nodal generation and load do not match for vertex associated with bus: " + vertex.getAssociatedBus().getId());
            }
        }
    }

    public enum InjectionStrategy {
        SUM_INJECTIONS,
        DECOMPOSE_INJECTIONS
    }
}
