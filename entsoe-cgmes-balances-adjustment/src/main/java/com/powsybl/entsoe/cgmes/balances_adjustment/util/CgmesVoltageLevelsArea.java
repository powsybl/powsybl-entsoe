/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.entsoe.cgmes.balances_adjustment.util;

import com.powsybl.balances_adjustment.util.NetworkArea;
import com.powsybl.cgmes.extensions.CgmesDanglingLineBoundaryNode;
import com.powsybl.iidm.network.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @deprecated Use module powsybl-balances-adjustment instead of this class
 *
 * @author Miora Ralambotiana {@literal <miora.ralambotiana at rte-france.com>}
 */
@Deprecated(since = "2.14", forRemoval = true)
class CgmesVoltageLevelsArea implements NetworkArea {

    private final List<String> voltageLevelIds = new ArrayList<>();

    private final List<DanglingLine> danglingLineBordersCache; // paired and unpaired.
    private final List<? extends Branch<?>> branchBordersCache; // other branches.

    private final Set<Bus> busesCache;

    CgmesVoltageLevelsArea(Network network, Area area, List<String> excludedXnodes, List<String> voltageLevelIds) {
        this.voltageLevelIds.addAll(voltageLevelIds);

        danglingLineBordersCache = createDanglingLinesCache(network, area, excludedXnodes);
        branchBordersCache = createBranchesCache(network, area);

        busesCache = network.getBusView().getBusStream()
                .filter(bus -> voltageLevelIds.contains(bus.getVoltageLevel().getId()))
                .filter(Bus::isInMainSynchronousComponent)
                .collect(Collectors.toSet());
    }

    private List<DanglingLine> createDanglingLinesCache(Network network, Area area, List<String> excludedXnodes) {
        return network.getDanglingLineStream()
                .filter(this::isAreaBorder)
                .filter(dl -> dl.getTerminal().getBusView().getBus() != null && dl.getTerminal().getBusView().getBus().isInMainSynchronousComponent()) // Only consider connected dangling lines in main synchronous component (other synchronous components are not considered)
                .filter(dl -> dl.getExtension(CgmesDanglingLineBoundaryNode.class) == null || !dl.getExtension(CgmesDanglingLineBoundaryNode.class).isHvdc()) // Dangling lines connected to DC boundary points are disregarded
                .filter(dl -> {
                    if (area != null && (CgmesAreaUtils.hasAreaBoundaryTerminals(area) || CgmesAreaUtils.hasAreaBoundaries(area))) { // if CgmesControlArea is defined, dangling lines with no associated tie flows are disregarded
                        return CgmesAreaUtils.isIdInAreaBoundaryTerminals(dl.getId(), area) ||
                                CgmesAreaUtils.isIdInAreaBoundariesDanglingLines(dl.getId(), area);
                    }
                    return true;
                })
                .filter(dl -> {
                    if (excludedXnodes != null) {
                        return excludedXnodes.stream().noneMatch(xnodeCode -> dl.getPairingKey().equals(xnodeCode)); // There is the possibility to exclude dangling lines associated with boundary nodes with given X-node codes
                    }
                    return true;
                })
                .toList();
    }

    private List<? extends Branch<?>> createBranchesCache(Network network, Area area) {
        return network.getLineStream()
                .filter(this::isAreaBorder)
                .filter(b -> b.getTerminal1().getBusView().getBus() != null && b.getTerminal1().getBusView().getBus().isInMainSynchronousComponent()
                        && b.getTerminal2().getBusView().getBus() != null && b.getTerminal2().getBusView().getBus().isInMainSynchronousComponent())  // Only consider branches connected on both sides and in main synchronous component (other synchronous components are not considered)
                .filter(b -> !b.hasProperty("isHvdc")) // necessary as extensions of merged lines are not well handled. FIXME: when it is merged on mergingview, this should be deleted.
                .filter(b -> {
                    if (area != null && CgmesAreaUtils.hasAreaBoundaryTerminals(area)) { // if CgmesControlArea is defined, branches with no associated tie flows are disregarded
                        return CgmesAreaUtils.isIdInAreaBoundaryTerminals(b.getId(), area);
                    }
                    return true;
                })
                .toList();
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
        double flowSide1 = branch.getTerminal1().isConnected() ? branch.getTerminal1().getP() : 0;
        double flowSide2 = branch.getTerminal2().isConnected() ? branch.getTerminal2().getP() : 0;
        double directFlow = (flowSide1 - flowSide2) / 2;
        return voltageLevelIds.contains(branch.getTerminal1().getVoltageLevel().getId()) ? directFlow : -directFlow;
    }
}

