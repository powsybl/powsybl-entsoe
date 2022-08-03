/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.flow_decomposition;

import com.powsybl.iidm.network.*;
import com.powsybl.loadflow.LoadFlowParameters;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 */
class LossesCompensator extends AbstractAcLoadFlowRunner<Void> {
    private final double epsilon;

    LossesCompensator(LoadFlowParameters initialLoadFlowParameters, double epsilon) {
        super(initialLoadFlowParameters);
        this.epsilon = epsilon;
    }

    LossesCompensator(LoadFlowParameters initialLoadFlowParameters, FlowDecompositionParameters parameters) {
        this(initialLoadFlowParameters, parameters.getLossesCompensationEpsilon());
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

    @Override
    Void run(Network network) {
        addNullLoadsOnBuses(network);
        String originVariant = network.getVariantManager().getWorkingVariantId();
        network.getVariantManager().getVariantIds().forEach(variantId -> compensateVariant(network, variantId));
        network.getVariantManager().setWorkingVariant(originVariant);
        return null;
    }

    private void addNullLoadsOnBuses(Network network) {
        network.getBusBreakerView().getBusStream()
            //.filter(Bus::isInMainSynchronousComponent)
            .forEach(this::addNullLoad);
    }

    private void addNullLoad(Bus bus) {
        bus.getVoltageLevel().newLoad()
            .setId(getLossesId(bus.getId()))
            .setBus(bus.getId())
            .setP0(0)
            .setQ0(0)
            .add();
    }

    private String getLossesId(String id) {
        return String.format("LOSSES %s", id);
    }

    private void compensateVariant(Network network, String variantId) {
        network.getVariantManager().setWorkingVariant(variantId);
        //LoadFlow.run(network, loadFlowParameters);
        network.getBranchStream()
            .filter(this::hasBuses)
            .filter(this::hasP0s)
            .forEach(branch -> this.compensateLossesOnBranch(branch, network));
    }

    private void compensateLossesOnBranch(Branch<?> branch, Network network) {
        if (branch instanceof TieLine) {
            compensateLossesOnTieLine((TieLine) branch, network);
        } else {
            Terminal sendingTerminal = getSendingTerminal(branch);
            double losses = branch.getTerminal1().getP() + branch.getTerminal2().getP();
            updateLoadForLossesOnTerminal(sendingTerminal, losses, network);
        }
    }

    private void compensateLossesOnTieLine(TieLine tieLine, Network network) {
        double r1 = tieLine.getHalf1().getR();
        double r2 = tieLine.getHalf2().getR();
        double r = r1 + r2;
        Terminal terminal1 = tieLine.getTerminal1();
        Terminal terminal2 = tieLine.getTerminal2();
        double losses = terminal1.getP() + terminal2.getP();
        double lossesSide1 = losses * r1 / r;
        double lossesSide2 = losses * r2 / r;

        updateLoadForLossesOnTerminal(terminal1, lossesSide1, network);
        updateLoadForLossesOnTerminal(terminal2, lossesSide2, network);
    }

    private void updateLoadForLossesOnTerminal(Terminal terminal, double losses, Network network) {
        if (Math.abs(losses) > epsilon) {
            Load load = network.getLoad(getLossesId(terminal.getBusBreakerView().getBus().getId()));
            load.setP0(load.getP0() + losses);
        }
    }

    private Terminal getSendingTerminal(Branch<?> branch) {
        return branch.getTerminal1().getP() > 0 ? branch.getTerminal1() : branch.getTerminal2();
    }
}
