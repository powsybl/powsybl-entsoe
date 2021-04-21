/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.entsoe.balances_adjustment.util;

import com.powsybl.balances_adjustment.util.NetworkArea;
import com.powsybl.cgmes.extensions.CgmesControlArea;
import com.powsybl.cgmes.extensions.CgmesDanglingLineBoundaryNode;
import com.powsybl.cgmes.extensions.CgmesLineBoundaryNode;
import com.powsybl.iidm.network.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Miora Ralambotiana <miora.ralambotiana at rte-france.com>
 */
class CgmesVoltageLevelsArea implements NetworkArea {

    private final List<String> voltageLevelIds = new ArrayList<>();

    private final List<DanglingLine> danglingLineBordersCache;
    private final List<Branch> branchBordersCache;

    private final Set<Bus> busesCache;

    CgmesVoltageLevelsArea(Network network, CgmesControlArea area, List<String> excludedXnodes, List<String> voltageLevelIds) {
        this.voltageLevelIds.addAll(voltageLevelIds);

        danglingLineBordersCache = createDanglingLinesCache(network, area, excludedXnodes);
        branchBordersCache = createBranchesCache(network, area, excludedXnodes);

        busesCache = network.getBusView().getBusStream()
                .filter(bus -> voltageLevelIds.contains(bus.getVoltageLevel().getId()))
                .filter(Bus::isInMainSynchronousComponent)
                .collect(Collectors.toSet());
    }

    private List<DanglingLine> createDanglingLinesCache(Network network, CgmesControlArea area, List<String> excludedXnodes) {
        return network.getDanglingLineStream()
                .filter(this::isAreaBorder)
                .filter(dl -> dl.getTerminal().getBusView().getBus() != null && dl.getTerminal().getBusView().getBus().isInMainSynchronousComponent())
                .filter(dl -> dl.getExtension(CgmesDanglingLineBoundaryNode.class) == null || !dl.getExtension(CgmesDanglingLineBoundaryNode.class).isHvdc())
                .filter(dl -> {
                    if (area != null && (!area.getTerminals().isEmpty() || !area.getBoundaries().isEmpty())) {
                        return area.getTerminals().stream().anyMatch(t -> t.getConnectable().getId().equals(dl.getId())) || area.getBoundaries().stream().anyMatch(bd -> bd.getConnectable().getId().equals(dl.getId()));
                    }
                    return true;
                })
                .filter(dl -> {
                    if (excludedXnodes != null) {
                        return excludedXnodes.stream().noneMatch(xnodeCode -> dl.getUcteXnodeCode().equals(xnodeCode));
                    }
                    return true;
                })
                .collect(Collectors.toList());
    }

    private List<Branch> createBranchesCache(Network network, CgmesControlArea area, List<String> excludedXnodes) {
        return network.getLineStream()
                .filter(this::isAreaBorder)
                .filter(b -> b.getTerminal1().getBusView().getBus() != null && b.getTerminal1().getBusView().getBus().isInMainSynchronousComponent()
                        && b.getTerminal2().getBusView().getBus() != null && b.getTerminal2().getBusView().getBus().isInMainSynchronousComponent())
                .filter(b -> b.getExtension(CgmesLineBoundaryNode.class) == null || !b.getExtension(CgmesLineBoundaryNode.class).isHvdc())
                .filter(b -> !b.hasProperty("isHvdc")) // necessary as extensions of merged lines are not well handled
                .filter(b -> {
                    if (area != null && (!area.getTerminals().isEmpty() || !area.getBoundaries().isEmpty())) {
                        if (b instanceof TieLine) {
                            return area.getTerminals().stream().anyMatch(t -> b.getId().contains(t.getConnectable().getId()))
                                    || area.getBoundaries().stream().anyMatch(bd -> b.getId().contains(bd.getConnectable().getId()));
                        } else {
                            return area.getTerminals().stream().anyMatch(t -> b.getId().contains(t.getConnectable().getId()));
                        }
                    }
                    return true;
                })
                .filter(b -> {
                    if (b instanceof TieLine && excludedXnodes != null) {
                        TieLine tl = (TieLine) b;
                        return excludedXnodes.stream().noneMatch(xnodeCode -> tl.getUcteXnodeCode().equals(xnodeCode));
                    }
                    return true;
                })
                .collect(Collectors.toList());
    }

    @Override
    public double getNetPosition() {
        return danglingLineBordersCache.parallelStream().mapToDouble(this::getLeavingFlow).sum()
                + branchBordersCache.parallelStream().mapToDouble(this::getLeavingFlow).sum();
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

    private double getLeavingFlow(DanglingLine danglingLine) {
        return danglingLine.getTerminal().isConnected() ? -danglingLine.getBoundary().getP() : 0;
    }

    private double getLeavingFlow(Branch<?> branch) {
        if (branch instanceof TieLine) {
            TieLine tl = (TieLine) branch;
            double flowSide1 = branch.getTerminal1().isConnected() ? -tl.getHalf1().getBoundary().getP() : 0;
            double flowSide2 = branch.getTerminal2().isConnected() ? -tl.getHalf2().getBoundary().getP() : 0;
            double directFlow = (flowSide1 - flowSide2) / 2;
            return voltageLevelIds.contains(branch.getTerminal1().getVoltageLevel().getId()) ? directFlow : -directFlow;
        }
        double flowSide1 = branch.getTerminal1().isConnected() ? branch.getTerminal1().getP() : 0;
        double flowSide2 = branch.getTerminal2().isConnected() ? branch.getTerminal2().getP() : 0;
        double directFlow = (flowSide1 - flowSide2) / 2;
        return voltageLevelIds.contains(branch.getTerminal1().getVoltageLevel().getId()) ? directFlow : -directFlow;
    }
}
