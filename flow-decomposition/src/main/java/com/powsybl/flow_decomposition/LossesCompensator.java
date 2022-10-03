/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.flow_decomposition;

import com.powsybl.commons.PowsyblException;
import com.powsybl.iidm.network.*;

import java.util.Optional;
import java.util.stream.Stream;

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
        compensateLossesOnBranches(network);
    }

    private void addNullLoadsOnBuses(Network network) {
        network.getBranchStream()
            .flatMap(branch -> Stream.of(branch.getTerminal1(), branch.getTerminal2()))
            .map(Terminal::getVoltageLevel)
            .distinct()
            .forEach(this::addNullLoad);
    }

    private void addNullLoad(VoltageLevel voltageLevel) {
        String lossesId = getLossesId(voltageLevel.getId());
        switch (voltageLevel.getTopologyKind()) {
            case BUS_BREAKER:
                addNullLoadForBusBreakerTopology(voltageLevel, lossesId);
                return;
            case NODE_BREAKER:
                addNullLoadForNodeTopology(voltageLevel, lossesId);
                return;
            default:
                throw new PowsyblException("This topology is not managed by the loss compensation.");
        }
    }

    private static void addNullLoadForBusBreakerTopology(VoltageLevel voltageLevel, String lossesId) {
        Optional<Bus> optionalBus = voltageLevel.getBusBreakerView().getBusStream().findFirst();
        if (optionalBus.isEmpty()) {
            throw new PowsyblException("Voltage level has no bus");
        }
        voltageLevel.newLoad()
            .setId(lossesId)
            .setBus(optionalBus.get().getId())
            .setP0(0)
            .setQ0(0)
            .add();
    }

    private static void addNullLoadForNodeTopology(VoltageLevel voltageLevel, String lossesId) {
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

    private static String getLossesId(String id) {
        return String.format("LOSSES %s", id);
    }

    private void compensateLossesOnBranches(Network network) {
        network.getBranchStream()
            .filter(this::hasBuses)
            .filter(this::hasP0s)
            .forEach(branch -> compensateLossesOnBranch(network, branch));
    }

    private void compensateLossesOnBranch(Network network, Branch<?> branch) {
        if (branch instanceof TieLine) {
            compensateLossesOnTieLine(network, (TieLine) branch);
        } else {
            Terminal sendingTerminal = getSendingTerminal(branch);
            double losses = branch.getTerminal1().getP() + branch.getTerminal2().getP();
            updateLoadForLossesOnTerminal(network, sendingTerminal, losses);
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

        updateLoadForLossesOnTerminal(network, terminal1, lossesSide1);
        updateLoadForLossesOnTerminal(network, terminal2, lossesSide2);
    }

    private void updateLoadForLossesOnTerminal(Network network, Terminal terminal, double losses) {
        if (Math.abs(losses) > epsilon) {
            Load load = network.getLoad(getLossesId(terminal.getVoltageLevel().getId()));
            load.setP0(load.getP0() + losses);
        }
    }

    private Terminal getSendingTerminal(Branch<?> branch) {
        return branch.getTerminal1().getP() > 0 ? branch.getTerminal1() : branch.getTerminal2();
    }
}
