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

    LossesCompensator(double epsilon) {
        this.epsilon = epsilon;
        networkHash = null;
    }

    LossesCompensator(FlowDecompositionParameters parameters) {
        this(parameters.getLossesCompensationEpsilon());
    }

    static String getLossesId(String id) {
        return String.format("LOSSES %s", id);
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
            .filter(this::hasP0s)
            .forEach(branch -> compensateLossesOnBranch(network, branch));
    }

    private boolean hasBus(Terminal terminal) {
        return terminal.getBusBreakerView().getBus() != null;
    }

    private boolean hasBuses(Branch<?> branch) {
        return hasBus(branch.getTerminal1()) && hasBus(branch.getTerminal2());
    }

    private boolean hasP0(Terminal terminal) {
        return !Double.isNaN(terminal.getP());
    }

    private boolean hasP0s(Branch<?> branch) {
        return hasP0(branch.getTerminal1()) && hasP0(branch.getTerminal2());
    }

    private static void addZeroMWLossesLoad(Network network, String busId) {
        String lossesId = getLossesId(busId);
        Bus bus = network.getBusBreakerView().getBus(busId);
        Country country = NetworkUtil.getBusCountry(bus);
        // nodes without country don't compensate losses
        if (country == null) {
            return;
        }
        switch (bus.getVoltageLevel().getTopologyKind()) {
            case BUS_BREAKER:
                addZeroMWLossesLoadForBusBreakerTopology(bus, lossesId);
                return;
            case NODE_BREAKER:
                addZeroMWLossesLoadForNodeTopology(bus, lossesId);
                return;
            default:
                throw new PowsyblException("This topology is not managed by the loss compensation.");
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
        if (branch instanceof TieLine) {
            compensateLossesOnTieLine(network, (TieLine) branch);
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
        return branch.getTerminal1().getP() > 0 ? branch.getTerminal1() : branch.getTerminal2();
    }

    private Terminal getReceivingTerminal(Branch<?> branch) {
        return branch.getTerminal1().getP() <= 0 ? branch.getTerminal1() : branch.getTerminal2();
    }
}
