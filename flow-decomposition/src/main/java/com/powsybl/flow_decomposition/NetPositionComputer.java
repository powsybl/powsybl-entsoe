/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.flow_decomposition;

import com.powsybl.iidm.network.*;

import java.util.EnumMap;
import java.util.Map;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 * @author Hugo Schindler{@literal <hugo.schindler at rte-france.com>}
 */
class NetPositionComputer {
    Map<Country, Double> run(Network network) {
        return computeNetPositions(network);
    }

    static Map<Country, Double> computeNetPositions(Network network) {
        Map<Country, Double> netPositions = new EnumMap<>(Country.class);

        // lines and tielines
        network.getBranchStream().forEach(branch -> {
            Country countrySide1 = NetworkUtil.getTerminalCountry(branch.getTerminal1());
            Country countrySide2 = NetworkUtil.getTerminalCountry(branch.getTerminal2());
            if (countrySide1.equals(countrySide2)) {
                return;
            }
            addLeavingFlow(netPositions, branch, countrySide1);
            addLeavingFlow(netPositions, branch, countrySide2);
        });

        // unpaired dangling lines
        network.getDanglingLineStream(DanglingLineFilter.UNPAIRED).forEach(danglingLine -> {
            Country country = NetworkUtil.getTerminalCountry(danglingLine.getTerminal());
            addLeavingFlow(netPositions, danglingLine, country);
        });

        network.getHvdcLineStream().forEach(hvdcLine -> {
            Country countrySide1 = NetworkUtil.getTerminalCountry(hvdcLine.getConverterStation1().getTerminal());
            Country countrySide2 = NetworkUtil.getTerminalCountry(hvdcLine.getConverterStation2().getTerminal());
            if (countrySide1.equals(countrySide2)) {
                return;
            }
            addLeavingFlow(netPositions, hvdcLine, countrySide1);
            addLeavingFlow(netPositions, hvdcLine, countrySide2);
        });

        return netPositions;
    }

    private static double getPreviousValue(Map<Country, Double> netPositions, Country country) {
        return netPositions.getOrDefault(country, 0.);
    }

    private static void addLeavingFlow(Map<Country, Double> netPositions, Branch<?> branch, Country country) {
        double previousValue = getPreviousValue(netPositions, country);
        netPositions.put(country, previousValue + getLeavingFlow(branch, country));
    }

    private static void addLeavingFlow(Map<Country, Double> netPositions, HvdcLine hvdcLine, Country country) {
        double previousValue = getPreviousValue(netPositions, country);
        netPositions.put(country, previousValue + getLeavingFlow(hvdcLine, country));
    }

    private static void addLeavingFlow(Map<Country, Double> netPositions, DanglingLine danglingLine, Country country) {
        double previousValue = getPreviousValue(netPositions, country);
        netPositions.put(country, previousValue + getLeavingFlow(danglingLine));
    }

    private static double getLeavingFlow(Branch<?> branch, Country country) {
        double flowSide1 = branch.getTerminal1().isConnected() && !Double.isNaN(branch.getTerminal1().getP()) ? branch.getTerminal1().getP() : 0;
        double flowSide2 = branch.getTerminal2().isConnected() && !Double.isNaN(branch.getTerminal2().getP()) ? branch.getTerminal2().getP() : 0;
        double directFlow = (flowSide1 - flowSide2) / 2;
        return country.equals(NetworkUtil.getTerminalCountry(branch.getTerminal1())) ? directFlow : -directFlow;
    }

    private static double getLeavingFlow(HvdcLine hvdcLine, Country country) {
        double flowSide1 = hvdcLine.getConverterStation1().getTerminal().isConnected() && !Double.isNaN(hvdcLine.getConverterStation1().getTerminal().getP()) ? hvdcLine.getConverterStation1().getTerminal().getP() : 0;
        double flowSide2 = hvdcLine.getConverterStation2().getTerminal().isConnected() && !Double.isNaN(hvdcLine.getConverterStation2().getTerminal().getP()) ? hvdcLine.getConverterStation2().getTerminal().getP() : 0;
        double directFlow = (flowSide1 - flowSide2) / 2;
        return country.equals(NetworkUtil.getTerminalCountry(hvdcLine.getConverterStation1().getTerminal())) ? directFlow : -directFlow;
    }

    private static double getLeavingFlow(DanglingLine danglingLine) {
        return danglingLine.getTerminal().isConnected() && !Double.isNaN(danglingLine.getTerminal().getP()) ? danglingLine.getTerminal().getP() : 0;
    }
}
