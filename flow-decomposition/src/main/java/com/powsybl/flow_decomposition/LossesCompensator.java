/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.flow_decomposition;

import com.powsybl.commons.PowsyblException;
import com.powsybl.iidm.network.*;

import java.util.stream.Stream;

/**
 * This class can
 * 1- add zero MW loss loads to a network
 * 2- set those loads to the correct AC loss
 *
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 */
class LossesCompensator {
    private final double epsilon;
    private Integer networkHash;
    public static final String LOSSES_ID_PREFIX = "LOSSES ";
    private static final double MIN_FLOW = 1E-6; // min flow to be considered the sending terminal in MW

    LossesCompensator(double epsilon) {
        this.epsilon = epsilon;
        networkHash = null;
    }

    LossesCompensator(FlowDecompositionParameters parameters) {
        this(parameters.getLossesCompensationEpsilon());
    }

    static String getLossesId(String id) {
        return String.format("%s%s", LOSSES_ID_PREFIX, id);
    }

    public void run(Network network) {
        if (networkHash == null || networkHash != network.hashCode()) {
            addZeroMWLossesLoadsOnBuses(network);
            networkHash = network.hashCode();
        }
        compensateLossesOnBranches(network);
    }

    private void addZeroMWLossesLoadsOnBuses(Network network) {
        // We want to add a single null load per bus
        // Mapping by bus Id is important as bus are generated on-fly
        // This matters for node breaker topology
        network.getBranchStream()
            .flatMap(branch -> Stream.of(branch.getTerminal1(), branch.getTerminal2()))
            .map(terminal -> terminal.getBusBreakerView().getConnectableBus())
            .map(Identifiable::getId)
            .distinct()
            .forEach(busId -> addZeroMWLossesLoad(network, busId));
    }

    private void compensateLossesOnBranches(Network network) {
        network.getBranchStream()
            .filter(this::hasBuses)
            .forEach(branch -> compensateLossesOnBranch(network, branch));
    }

    private boolean hasBuses(Branch<?> branch) {
        return !(branch.getTerminal1().getBusBreakerView().getBus() == null &&
                branch.getTerminal2().getBusBreakerView().getBus() == null);
    }

    private static void addZeroMWLossesLoad(Network network, String busId) {
        String lossesId = getLossesId(busId);
        Bus bus = network.getBusBreakerView().getBus(busId);
        Country country = NetworkUtil.getBusCountry(bus);
        // node without country will not compensate losses
        if (country == null) {
            return;
        }
        switch (bus.getVoltageLevel().getTopologyKind()) {
            case BUS_BREAKER -> addZeroMWLossesLoadForBusBreakerTopology(bus, lossesId);
            case NODE_BREAKER -> addZeroMWLossesLoadForNodeTopology(bus, lossesId);
            default -> throw new PowsyblException("This topology is not managed by the loss compensation.");
        }
    }

    private static void addZeroMWLossesLoadForBusBreakerTopology(Bus bus, String lossesId) {
        bus.getVoltageLevel().newLoad()
            .setId(lossesId)
            .setBus(bus.getId())
            .setP0(0)
            .setQ0(0)
            .add();
    }

    private static void addZeroMWLossesLoadForNodeTopology(Bus bus, String lossesId) {
        VoltageLevel voltageLevel = bus.getVoltageLevel();
        VoltageLevel.NodeBreakerView nodeBreakerView = voltageLevel.getNodeBreakerView();
        int nodeNum = nodeBreakerView.getMaximumNodeIndex() + 1;
        nodeBreakerView.newInternalConnection()
            .setNode1(nodeNum)
            .setNode2(nodeBreakerView.getNodes()[0])
            .add();
        voltageLevel.newLoad()
            .setId(lossesId)
            .setNode(nodeNum)
            .setP0(0)
            .setQ0(0)
            .add();
    }

    private void compensateLossesOnBranch(Network network, Branch<?> branch) {
        if (branch instanceof TieLine tieLine) {
            compensateLossesOnTieLine(network, tieLine);
        } else {
            Terminal sendingTerminal = getSendingTerminal(branch);
            double losses = branch.getTerminal1().getP() + branch.getTerminal2().getP();
            Country country = NetworkUtil.getTerminalCountry(sendingTerminal);
            if (country != null) {
                updateLoadForLossesOnTerminal(network, sendingTerminal, losses);
                return;
            }
            // sending terminal has no country, attribute loss to receiving terminal
            Terminal receivingTerminal = getReceivingTerminal(branch);
            country = NetworkUtil.getTerminalCountry(receivingTerminal);
            if (country != null) {
                updateLoadForLossesOnTerminal(network, receivingTerminal, losses);
                return;
            }
            throw new PowsyblException(String.format("Branch %s connects two nodes without countries.", branch.getId()));
        }
    }

    private void compensateLossesOnTieLine(Network network, TieLine tieLine) {
        double r1 = tieLine.getDanglingLine1().getR();
        double r2 = tieLine.getDanglingLine2().getR();
        double r = r1 + r2;
        Terminal terminal1 = tieLine.getDanglingLine1().getTerminal();
        Terminal terminal2 = tieLine.getDanglingLine2().getTerminal();
        double losses = terminal1.getP() + terminal2.getP();
        double lossesSide1 = losses * r1 / r;
        double lossesSide2 = losses * r2 / r;

        updateLoadForLossesOnTerminal(network, terminal1, lossesSide1);
        updateLoadForLossesOnTerminal(network, terminal2, lossesSide2);
    }

    private void updateLoadForLossesOnTerminal(Network network, Terminal terminal, double losses) {
        if (Math.abs(losses) > epsilon) {
            Load load = network.getLoad(getLossesId(terminal.getBusBreakerView().getBus().getId()));
            load.setP0(load.getP0() + losses);
        }
    }

    private Terminal getSendingTerminal(Branch<?> branch) {
        // include some tolerance to check if terminal 1 is the sending terminal
        // it avoids the case where the flow is almost zero, but positive
        // example: p1 = 1E-10 and p2 = 1
        return branch.getTerminal1().getP() > MIN_FLOW ? branch.getTerminal1() : branch.getTerminal2();
    }

    private Terminal getReceivingTerminal(Branch<?> branch) {
        return branch.getTerminal1().getP() <= MIN_FLOW ? branch.getTerminal1() : branch.getTerminal2();
    }
}
