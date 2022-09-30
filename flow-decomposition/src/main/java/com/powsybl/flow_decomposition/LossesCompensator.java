/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.flow_decomposition;

import com.powsybl.commons.PowsyblException;
import com.powsybl.iidm.network.*;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 */
class LossesCompensator {
    private final double epsilon;

    LossesCompensator(double epsilon) {
        this.epsilon = epsilon;
    }

    LossesCompensator(FlowDecompositionParameters parameters) {
        this(parameters.getLossesCompensationEpsilon());
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

    void run(Network network) {
        addNullLoadsOnBuses(network);
        network.getBranchStream()
            .filter(this::hasBuses)
            .filter(this::hasP0s)
            .forEach(branch -> compensateLossesOnBranch(network, branch));
    }

    private void addNullLoadsOnBuses(Network network) {
        network.getBusBreakerView().getBusStream()
            //.filter(Bus::isInMainSynchronousComponent)
            .forEach(this::addNullLoad);
    }

    private void addNullLoad(Bus bus) {
        switch (bus.getVoltageLevel().getTopologyKind()) {
            case BUS_BREAKER:
                addNullLoadForBusBreakerTopology(bus);
                return;
            case NODE_BREAKER:
                addNullLoadForNodeBreakerTopology(bus);
                return;
            default:
                throw new PowsyblException("This topology is not managed by the loss compensation.");
        }
    }

    private static void addNullLoadForBusBreakerTopology(Bus bus) {
        String busId = bus.getId();
        bus.getVoltageLevel().newLoad()
            .setId(getLossesId(busId))
            .setBus(busId)
            .setP0(0)
            .setQ0(0)
            .add();
    }

    private static void addNullLoadForNodeBreakerTopology(Bus bus) {
        return;
       // String busId = bus.getId();
       // Optional<? extends Terminal> optionalTerminal = bus.getConnectedTerminalStream().findFirst();
       // if (optionalTerminal.isEmpty()) {
       //     throw new PowsyblException(String.format("Bus %s should have at least one terminal", busId));
       // }
       // Terminal terminal = optionalTerminal.get();
       // int nodeNum = terminal.getVoltageLevel().getNodeBreakerView().getMaximumNodeIndex() + 1;
       // terminal.getVoltageLevel().getNodeBreakerView().newInternalConnection()
       //     .setNode1(nodeNum)
       //     .setNode2(terminal.getNodeBreakerView().getNode())
       //     .add();
       // terminal.getVoltageLevel().newLoad()
       //     .setId(busId)
       //     .setNode(nodeNum)
       //     .setP0(0)
       //     .setQ0(0)
       //     .add();
    }

    private static String getLossesId(String id) {
        return String.format("LOSSES %s", id);
    }

    private void compensateLossesOnBranch(Network network, Branch<?> branch) {
        if (branch instanceof TieLine) {
            compensateLossesOnTieLine(network, (TieLine) branch);
        } else {
            Terminal sendingTerminal = getSendingTerminal(branch);
            String lossesId = getLossesId(branch.getId());
            double losses = branch.getTerminal1().getP() + branch.getTerminal2().getP();
            updateLoadForLossesOnTerminal(network, sendingTerminal, lossesId, losses);
        }
    }

    private void compensateLossesOnTieLine(Network network, TieLine tieLine) {
        double r1 = tieLine.getHalf1().getR();
        double r2 = tieLine.getHalf2().getR();
        double r = r1 + r2;
        Terminal terminal1 = tieLine.getTerminal1();
        Terminal terminal2 = tieLine.getTerminal2();
        double losses = terminal1.getP() + terminal2.getP();
        double lossesSide1 = losses * r1 / r;
        double lossesSide2 = losses * r2 / r;
        String lossesIdSide1 = getLossesId(tieLine.getHalf1().getId());
        String lossesIdSide2 = getLossesId(tieLine.getHalf2().getId());

        updateLoadForLossesOnTerminal(network, terminal1, lossesIdSide1, lossesSide1);
        updateLoadForLossesOnTerminal(network, terminal2, lossesIdSide2, lossesSide2);
    }

    private void updateLoadForLossesOnTerminal(Network network, Terminal terminal, String lossesId, double losses) {
        if (Math.abs(losses) > epsilon) {
            switch (terminal.getVoltageLevel().getTopologyKind()) {
                case BUS_BREAKER:
                    updateLoadForBusBreakerTopology(network, terminal, losses);
                    return;
                case NODE_BREAKER:
                    updateLoadForNodeBreakerTopology(terminal, lossesId, losses);
                    return;
                default:
                    throw new PowsyblException("This topology is not managed by the loss compensation.");
            }
        }
    }

    private static void updateLoadForBusBreakerTopology(Network network, Terminal terminal, double losses) {
        Load load = network.getLoad(getLossesId(terminal.getBusBreakerView().getBus().getId()));
        load.setP0(load.getP0() + losses);
    }

    private static void updateLoadForNodeBreakerTopology(Terminal terminal, String lossesId, double losses) {
        int nodeNum = terminal.getVoltageLevel().getNodeBreakerView().getMaximumNodeIndex() + 1;
        terminal.getVoltageLevel().getNodeBreakerView().newInternalConnection()
            .setNode1(nodeNum)
            .setNode2(terminal.getNodeBreakerView().getNode())
            .add();
        terminal.getVoltageLevel().newLoad()
            .setId(lossesId)
            .setNode(nodeNum)
            .setP0(losses)
            .setQ0(0)
            .add();
    }

    private Terminal getSendingTerminal(Branch<?> branch) {
        return branch.getTerminal1().getP() > 0 ? branch.getTerminal1() : branch.getTerminal2();
    }
}
