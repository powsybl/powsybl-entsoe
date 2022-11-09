/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.balances_adjustment.util;

import com.powsybl.iidm.network.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Ameni Walha {@literal <ameni.walha at rte-france.com>}
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 * @author Mathieu Bague {@literal <mathieu.bague at rte-france.com>}
 */
public class VoltageLevelsArea implements NetworkArea {

    private final List<String> voltageLevelIds = new ArrayList<>();

    private final List<DanglingLine> danglingLineBordersCache;
    private final List<Branch> branchBordersCache;
    private final List<ThreeWindingsTransformer> threeWindingsTransformerBordersCache;
    private final List<HvdcLine> hvdcLineBordersCache;

    private final Set<Bus> busesCache;

    public VoltageLevelsArea(Network network, List<String> voltageLevelIds) {
        this.voltageLevelIds.addAll(voltageLevelIds);

        danglingLineBordersCache = network.getDanglingLineStream()
                .filter(this::isAreaBorder)
                .collect(Collectors.toList());
        branchBordersCache = network.getLineStream()
                .filter(this::isAreaBorder)
                .collect(Collectors.toList());
        threeWindingsTransformerBordersCache = network.getThreeWindingsTransformerStream()
                .filter(this::isAreaBorder)
                .collect(Collectors.toList());
        hvdcLineBordersCache = network.getHvdcLineStream()
                .filter(this::isAreaBorder)
                .collect(Collectors.toList());

        busesCache = network.getBusView().getBusStream()
                .filter(bus -> voltageLevelIds.contains(bus.getVoltageLevel().getId()))
                .collect(Collectors.toSet());
    }

    @Override
    public double getNetPosition() {
        return danglingLineBordersCache.parallelStream().mapToDouble(this::getLeavingFlow).sum()
                + branchBordersCache.parallelStream().mapToDouble(this::getLeavingFlow).sum()
                + threeWindingsTransformerBordersCache.parallelStream().mapToDouble(this::getLeavingFlow).sum()
                + hvdcLineBordersCache.parallelStream().mapToDouble(this::getLeavingFlow).sum();
    }

    @Override
    public Collection<Bus> getContainedBusViewBuses() {
        return Collections.unmodifiableCollection(busesCache);
    }

    private boolean isAreaBorder(DanglingLine danglingLine) {
        String voltageLevel = danglingLine.getTerminal().getVoltageLevel().getId();
        return voltageLevelIds.contains(voltageLevel);
    }

    private boolean isAreaBorder(Line line) {
        String voltageLevelSide1 = line.getTerminal1().getVoltageLevel().getId();
        String voltageLevelSide2 = line.getTerminal2().getVoltageLevel().getId();
        return voltageLevelIds.contains(voltageLevelSide1) && !voltageLevelIds.contains(voltageLevelSide2) ||
                !voltageLevelIds.contains(voltageLevelSide1) && voltageLevelIds.contains(voltageLevelSide2);
    }

    private boolean isAreaBorder(ThreeWindingsTransformer threeWindingsTransformer) {
        String voltageLevelSide1 = threeWindingsTransformer.getLeg1().getTerminal().getVoltageLevel().getId();
        String voltageLevelSide2 = threeWindingsTransformer.getLeg2().getTerminal().getVoltageLevel().getId();
        String voltageLevelSide3 = threeWindingsTransformer.getLeg3().getTerminal().getVoltageLevel().getId();
        boolean containsOne = voltageLevelIds.contains(voltageLevelSide1) ||
                voltageLevelIds.contains(voltageLevelSide2) ||
                voltageLevelIds.contains(voltageLevelSide3);
        boolean containsAll = voltageLevelIds.contains(voltageLevelSide1) &&
                voltageLevelIds.contains(voltageLevelSide2) &&
                voltageLevelIds.contains(voltageLevelSide3);
        return containsOne && !containsAll;
    }

    private boolean isAreaBorder(HvdcLine hvdcLine) {
        String voltageLevelSide1 = hvdcLine.getConverterStation1().getTerminal().getVoltageLevel().getId();
        String voltageLevelSide2 = hvdcLine.getConverterStation2().getTerminal().getVoltageLevel().getId();
        return voltageLevelIds.contains(voltageLevelSide1) && !voltageLevelIds.contains(voltageLevelSide2) ||
                !voltageLevelIds.contains(voltageLevelSide1) && voltageLevelIds.contains(voltageLevelSide2);
    }

    private double getLeavingFlow(DanglingLine danglingLine) {
        return danglingLine.getTerminal().isConnected() ? danglingLine.getTerminal().getP() : 0;
    }

    private double getLeavingFlow(Branch<?> branch) {
        double flowSide1 = branch.getTerminal1().isConnected() ? branch.getTerminal1().getP() : 0;
        double flowSide2 = branch.getTerminal2().isConnected() ? branch.getTerminal2().getP() : 0;
        double directFlow = (flowSide1 - flowSide2) / 2;
        return voltageLevelIds.contains(branch.getTerminal1().getVoltageLevel().getId()) ? directFlow : -directFlow;
    }

    private double getLeavingFlow(HvdcLine hvdcLine) {
        double flowSide1 = hvdcLine.getConverterStation1().getTerminal().isConnected() ? hvdcLine.getConverterStation1().getTerminal().getP() : 0;
        double flowSide2 = hvdcLine.getConverterStation2().getTerminal().isConnected() ? hvdcLine.getConverterStation2().getTerminal().getP() : 0;
        double directFlow = (flowSide1 - flowSide2) / 2;
        return voltageLevelIds.contains(hvdcLine.getConverterStation1().getTerminal().getVoltageLevel().getId()) ? directFlow : -directFlow;
    }

    private double getLeavingFlow(ThreeWindingsTransformer threeWindingsTransformer) {
        double outsideFlow = 0;
        double insideFlow = 0;
        for (ThreeWindingsTransformer.Side side : ThreeWindingsTransformer.Side.values()) {
            outsideFlow += !voltageLevelIds.contains(threeWindingsTransformer.getTerminal(side).getVoltageLevel().getId()) && threeWindingsTransformer.getTerminal(side).isConnected()
                    ? threeWindingsTransformer.getTerminal(side).getP() : 0;
            insideFlow += voltageLevelIds.contains(threeWindingsTransformer.getTerminal(side).getVoltageLevel().getId()) && threeWindingsTransformer.getTerminal(side).isConnected()
                    ? threeWindingsTransformer.getTerminal(side).getP() : 0;
        }
        return (insideFlow - outsideFlow) / 2;
    }
}
