/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.balances_adjustment.util;

import com.powsybl.commons.PowsyblException;
import com.powsybl.iidm.network.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Ameni Walha {@literal <ameni.walha at rte-france.com>}
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 * @author Mathieu Bague {@literal <mathieu.bague at rte-france.com>}
 */
public class CountryArea implements NetworkArea {

    private final List<Country> countries = new ArrayList<>();

    private final List<DanglingLine> danglingLineBordersCache;
    private final List<Line> lineBordersCache;
    private final List<HvdcLine> hvdcLineBordersCache;

    private final Set<Bus> busesCache;

    public CountryArea(Network network, List<Country> countries) {
        this.countries.addAll(countries);

        danglingLineBordersCache = network.getDanglingLineStream()
                .filter(this::isAreaBorder)
                .collect(Collectors.toList());
        lineBordersCache = network.getLineStream()
                .filter(this::isAreaBorder)
                .collect(Collectors.toList());
        hvdcLineBordersCache = network.getHvdcLineStream()
                .filter(this::isAreaBorder)
                .collect(Collectors.toList());

        busesCache = network.getBusView().getBusStream()
                .filter(bus -> bus.getVoltageLevel().getSubstation().flatMap(Substation::getCountry).map(countries::contains).orElse(false))
                .collect(Collectors.toSet());
    }

    public List<Country> getCountries() {
        return countries;
    }

    @Override
    public double getNetPosition() {
        return danglingLineBordersCache.parallelStream().mapToDouble(this::getLeavingFlow).sum()
                + lineBordersCache.parallelStream().mapToDouble(this::getLeavingFlow).sum()
                + hvdcLineBordersCache.parallelStream().mapToDouble(this::getLeavingFlow).sum();
    }

    @Override
    public Collection<Bus> getContainedBusViewBuses() {
        return Collections.unmodifiableCollection(busesCache);
    }

    public double getLeavingFlowToCountry(CountryArea countryArea) {
        countryArea.getCountries().stream().forEach(country -> {
            if (countries.contains(country)) {
                throw new PowsyblException("The leaving flow to the country area cannot be computed. " +
                        "The country " + country.getName() + " is contained in both control areas.");
            }
        });
        double sum = 0;
        for (Line line : lineBordersCache) {
            if (countryArea.isAreaBorder(line)) {
                sum += getLeavingFlow(line);
            }
        }
        for (HvdcLine line : hvdcLineBordersCache) {
            if (countryArea.isAreaBorder(line)) {
                sum += getLeavingFlow(line);
            }
        }
        return sum;
    }

    private boolean isAreaBorder(DanglingLine danglingLine) {
        Country country = danglingLine.getTerminal().getVoltageLevel().getSubstation().map(Substation::getNullableCountry).orElse(null);
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

    private double getLeavingFlow(DanglingLine danglingLine) {
        return danglingLine.getTerminal().isConnected() && !Double.isNaN(danglingLine.getTerminal().getP()) ? danglingLine.getTerminal().getP() : 0;
    }

    private double getLeavingFlow(Line line) {
        double flowSide1 = line.getTerminal1().isConnected() && !Double.isNaN(line.getTerminal1().getP()) ? line.getTerminal1().getP() : 0;
        double flowSide2 = line.getTerminal2().isConnected() && !Double.isNaN(line.getTerminal2().getP()) ? line.getTerminal2().getP() : 0;
        double directFlow = (flowSide1 - flowSide2) / 2;
        return countries.contains(line.getTerminal1().getVoltageLevel().getSubstation().map(Substation::getNullableCountry).orElse(null)) ? directFlow : -directFlow;
    }

    private double getLeavingFlow(HvdcLine hvdcLine) {
        double flowSide1 = hvdcLine.getConverterStation1().getTerminal().isConnected() && !Double.isNaN(hvdcLine.getConverterStation1().getTerminal().getP()) ? hvdcLine.getConverterStation1().getTerminal().getP() : 0;
        double flowSide2 = hvdcLine.getConverterStation2().getTerminal().isConnected() && !Double.isNaN(hvdcLine.getConverterStation2().getTerminal().getP()) ? hvdcLine.getConverterStation2().getTerminal().getP() : 0;
        double directFlow = (flowSide1 - flowSide2) / 2;
        return countries.contains(hvdcLine.getConverterStation1().getTerminal().getVoltageLevel().getSubstation().map(Substation::getNullableCountry).orElse(null)) ? directFlow : -directFlow;
    }
}
