/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.balances_adjustment.util;

import com.powsybl.commons.PowsyblException;
import com.powsybl.iidm.network.*;
import com.powsybl.iidm.network.util.TieLineUtil;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Ameni Walha {@literal <ameni.walha at rte-france.com>}
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 * @author Mathieu Bague {@literal <mathieu.bague at rte-france.com>}
 */
public class BorderBasedCountryArea implements NetworkArea {

    private final List<Country> countries = new ArrayList<>();

    // We should consider all boundary lines now, either paired or unpaired.
    // The computation is more clean because we have the real value at boundary
    // for a tie line now.
    private final List<BoundaryLine> boundaryLineBordersCache;
    private final List<Line> lineBordersCache;
    private final List<HvdcLine> hvdcLineBordersCache;
    private final List<Load> loadsCache;
    private final List<Generator> generatorsCache;

    private final Set<Bus> busesCache;

    public BorderBasedCountryArea(Network network, List<Country> countries) {
        this.countries.addAll(countries);

        boundaryLineBordersCache = network.getBoundaryLineStream()
                .filter(this::isAreaBorder)
                .toList();
        lineBordersCache = network.getLineStream()
                .filter(this::isAreaBorder)
                .toList();
        hvdcLineBordersCache = network.getHvdcLineStream()
                .filter(this::isAreaBorder)
                .toList();
        loadsCache = network.getLoadStream()
            .filter(load -> NetworkAreaUtil.isInCountry(load, countries))
            .toList();
        generatorsCache = network.getGeneratorStream()
            .filter(generator -> NetworkAreaUtil.isInCountry(generator, countries))
            .toList();

        busesCache = network.getBusView().getBusStream()
                .filter(bus -> bus.getVoltageLevel().getSubstation().flatMap(Substation::getCountry).map(countries::contains).orElse(false))
                .collect(Collectors.toSet());
    }

    public List<Country> getCountries() {
        return countries;
    }

    @Override
    public double getNetPosition(boolean subtractLoadFlowBalancing) {
        double netPosition = boundaryLineBordersCache.parallelStream().mapToDouble(this::getLeavingFlow).sum()
                + lineBordersCache.parallelStream().mapToDouble(this::getLeavingFlow).sum()
                + hvdcLineBordersCache.parallelStream().mapToDouble(this::getLeavingFlow).sum();
        if (subtractLoadFlowBalancing) {
            netPosition -= NetworkAreaUtil.getLoadFlowBalance(generatorsCache, loadsCache);
        }
        return netPosition;
    }

    @Override
    public Collection<Bus> getContainedBusViewBuses() {
        return Collections.unmodifiableCollection(busesCache);
    }

    public double getLeavingFlowToCountry(BorderBasedCountryArea otherCountryArea) {
        otherCountryArea.getCountries().forEach(country -> {
            if (countries.contains(country)) {
                throw new PowsyblException("The leaving flow to the country area cannot be computed. " +
                        "The country " + country.getName() + " is contained in both control areas.");
            }
        });
        double sum = 0;
        for (BoundaryLine boundaryLine : boundaryLineBordersCache) {
            if (isOtherSideInArea(boundaryLine, otherCountryArea)) {
                sum += getLeavingFlow(boundaryLine);
            }
        }
        for (Line line : lineBordersCache) {
            if (otherCountryArea.isAreaBorder(line)) {
                sum += getLeavingFlow(line);
            }
        }
        for (HvdcLine line : hvdcLineBordersCache) {
            if (otherCountryArea.isAreaBorder(line)) {
                sum += getLeavingFlow(line);
            }
        }
        return sum;
    }

    private boolean isOtherSideInArea(BoundaryLine boundaryLine, BorderBasedCountryArea countryArea) {
        return TieLineUtil.getPairedBoundaryLine(boundaryLine).filter(countryArea::isAreaBorder).isPresent();
    }

    private boolean isAreaBorder(BoundaryLine boundaryLine) {
        Country country = boundaryLine.getTerminal().getVoltageLevel().getSubstation().map(Substation::getNullableCountry).orElse(null);
        return countries.contains(country);
    }

    private boolean isAreaBorder(Line line) {
        Country countrySide1 = line.getTerminal1().getVoltageLevel().getSubstation().map(Substation::getNullableCountry).orElse(null);
        Country countrySide2 = line.getTerminal2().getVoltageLevel().getSubstation().map(Substation::getNullableCountry).orElse(null);
        if (countrySide1 == null || countrySide2 == null) {
            return false;
        }
        return countries.contains(countrySide1) && !countries.contains(countrySide2) ||
                !countries.contains(countrySide1) && countries.contains(countrySide2);
    }

    private boolean isAreaBorder(HvdcLine hvdcLine) {
        Country countrySide1 = hvdcLine.getConverterStation1().getTerminal().getVoltageLevel().getSubstation().map(Substation::getNullableCountry).orElse(null);
        Country countrySide2 = hvdcLine.getConverterStation2().getTerminal().getVoltageLevel().getSubstation().map(Substation::getNullableCountry).orElse(null);
        if (countrySide1 == null || countrySide2 == null) {
            return false;
        }
        return countries.contains(countrySide1) && !countries.contains(countrySide2) ||
                !countries.contains(countrySide1) && countries.contains(countrySide2);
    }

    private double getLeavingFlow(BoundaryLine boundaryLine) {
        return boundaryLine.getTerminal().isConnected() ? zeroIfNan(-boundaryLine.getBoundary().getP()) : 0;
    }

    private double getLeavingFlow(Line line) {
        double flowSide1 = line.getTerminal1().isConnected() ? zeroIfNan(line.getTerminal1().getP()) : 0;
        double flowSide2 = line.getTerminal2().isConnected() ? zeroIfNan(line.getTerminal2().getP()) : 0;
        double directFlow = (flowSide1 - flowSide2) / 2;
        return countries.contains(line.getTerminal1().getVoltageLevel().getSubstation().map(Substation::getNullableCountry).orElse(null)) ? directFlow : -directFlow;
    }

    private double getLeavingFlow(HvdcLine hvdcLine) {
        double flowSide1 = hvdcLine.getConverterStation1().getTerminal().isConnected() ? zeroIfNan(hvdcLine.getConverterStation1().getTerminal().getP()) : 0;
        double flowSide2 = hvdcLine.getConverterStation2().getTerminal().isConnected() ? zeroIfNan(hvdcLine.getConverterStation2().getTerminal().getP()) : 0;
        double directFlow = (flowSide1 - flowSide2) / 2;
        return countries.contains(hvdcLine.getConverterStation1().getTerminal().getVoltageLevel().getSubstation().map(Substation::getNullableCountry).orElse(null)) ? directFlow : -directFlow;
    }

    private static double zeroIfNan(double aPossiblyNanValue) {
        return Double.isNaN(aPossiblyNanValue) ? 0 : aPossiblyNanValue;
    }
}
