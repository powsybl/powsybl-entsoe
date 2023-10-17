/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.balances_adjustment.util;

import com.powsybl.commons.PowsyblException;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Injection;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.Substation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Ameni Walha {@literal <ameni.walha at rte-france.com>}
 */
class CountryAreaTest {

    private static final double EPSILON = 1;
    private Network testNetwork1;
    private Network testNetwork2;

    private CountryAreaFactory countryAreaFactoryFR;
    private CountryAreaFactory countryAreaFactoryES;
    private CountryAreaFactory countryAreaFactoryBE;

    @BeforeEach
    void setUp() {
        testNetwork1 = Network.read("testCase.xiidm", getClass().getResourceAsStream("/testCase.xiidm"));
        testNetwork2 = NetworkTestFactory.createNetwork();

        countryAreaFactoryFR = new CountryAreaFactory(Country.FR);
        countryAreaFactoryES = new CountryAreaFactory(Country.ES);
        countryAreaFactoryBE = new CountryAreaFactory(Country.BE);
    }

    private Stream<Injection> getInjectionStream(Network network) {
        Stream returnStream = Stream.empty();
        returnStream = Stream.concat(network.getGeneratorStream(), returnStream);
        returnStream = Stream.concat(network.getLoadStream(), returnStream);
        returnStream = Stream.concat(network.getDanglingLineStream(), returnStream);
        return returnStream;
    }

    private double getSumFlowCountry(Network network, Country country) {
        double sumFlow = 0;
        List<Injection> injections = getInjectionStream(network).filter(i -> country.equals(i.getTerminal().getVoltageLevel().getSubstation().map(Substation::getNullableCountry).orElse(null)))
                .collect(Collectors.toList());
        for (Injection injection : injections) {
            sumFlow += injection.getTerminal().getBusBreakerView().getBus().isInMainConnectedComponent() ? injection.getTerminal().getP() : 0;

        }
        return sumFlow;
    }

    @Test
    void testGetNetPosition() {
        //Test network with BranchBorder
        assertEquals(0, countryAreaFactoryES.create(testNetwork1).getNetPosition(), 1e-3);

        assertEquals(-getSumFlowCountry(testNetwork1, Country.FR), countryAreaFactoryFR.create(testNetwork1).getNetPosition(), 1e-3);

        //Test network with HVDCLines
        assertEquals(testNetwork2.getHvdcLine("hvdcLineFrEs").getConverterStation1().getTerminal().getP(), countryAreaFactoryFR.create(testNetwork2).getNetPosition(), 1e-3);
        assertEquals(testNetwork2.getHvdcLine("hvdcLineFrEs").getConverterStation2().getTerminal().getP(), countryAreaFactoryES.create(testNetwork2).getNetPosition(), 1e-3);
    }

    @Test
    void testSpecialDevices() {
        Network network = Network.read("testCaseSpecialDevices.xiidm", getClass().getResourceAsStream("/testCaseSpecialDevices.xiidm"));
        assertEquals(100, countryAreaFactoryFR.create(network).getNetPosition(), 1e-3);
        assertEquals(-100, countryAreaFactoryES.create(network).getNetPosition(), 1e-3);
    }

    @Test
    void testGetLeavingFlowToCountry() {
        CountryArea countryAreaFR2 = countryAreaFactoryFR.create(testNetwork2);
        CountryArea countryAreaES2 = countryAreaFactoryES.create(testNetwork2);
        CountryArea countryAreaFR1 = countryAreaFactoryFR.create(testNetwork1);
        CountryArea countryAreaBE1 = countryAreaFactoryBE.create(testNetwork1);
        CountryArea countryAreaES1 = countryAreaFactoryES.create(testNetwork1);

        assertEquals(100.0, countryAreaFR2.getLeavingFlowToCountry(countryAreaES2), 1e-3);
        assertEquals(-100.0, countryAreaES2.getLeavingFlowToCountry(countryAreaFR2), 1e-3);
        assertEquals(-324.666, countryAreaFR1.getLeavingFlowToCountry(countryAreaBE1), 1e-3);
        assertEquals(324.666, countryAreaBE1.getLeavingFlowToCountry(countryAreaFR1), 1e-3);
        assertEquals(0.0, countryAreaBE1.getLeavingFlowToCountry(countryAreaES1), 1e-3);
        try {
            countryAreaFR1.getLeavingFlowToCountry(countryAreaFR1);
            fail();
        } catch (PowsyblException e) {
            assertEquals("The leaving flow to the country area cannot be computed. The country FRANCE is contained in both control areas.", e.getMessage());
        }
    }

    @Test
    void testWithTieLine() {
        Network network = Network.read("controlArea.xiidm", getClass().getResourceAsStream("/controlArea.xiidm"));
        CountryArea countryAreaBE = countryAreaFactoryBE.create(network);
        assertEquals(-261.858, countryAreaBE.getNetPosition(), 1e-3);
    }

    @Test
    void testLeavingFlowToCountryWithTieLines() {
        Network network = Network.read("border_exchanges.xiidm", getClass().getResourceAsStream("/border_exchanges.xiidm"));

        CountryArea countryAreaIT = new CountryAreaFactory(Country.IT).create(network);
        CountryArea countryAreaCH = new CountryAreaFactory(Country.CH).create(network);
        CountryArea countryAreaDE = new CountryAreaFactory(Country.DE).create(network);
        CountryArea countryAreaFR = new CountryAreaFactory(Country.FR).create(network);
        CountryArea countryAreaSI = new CountryAreaFactory(Country.SI).create(network);
        CountryArea countryAreaAT = new CountryAreaFactory(Country.AT).create(network);

        assertEquals(0, countryAreaIT.getLeavingFlowToCountry(countryAreaSI), EPSILON);
        assertEquals(-2837, countryAreaIT.getLeavingFlowToCountry(countryAreaCH), EPSILON);
        assertEquals(0, countryAreaFR.getLeavingFlowToCountry(countryAreaDE), EPSILON);
        assertEquals(-2463, countryAreaIT.getLeavingFlowToCountry(countryAreaFR), EPSILON);
        assertEquals(-37, countryAreaCH.getLeavingFlowToCountry(countryAreaFR), EPSILON);
        assertEquals(0, countryAreaCH.getLeavingFlowToCountry(countryAreaDE), EPSILON);
        assertEquals(-699, countryAreaIT.getLeavingFlowToCountry(countryAreaAT), EPSILON);
    }

    @Test
    void testNetPositionIsZeroWhenDanglingLineBorderPIsNaN() {
        Network testNetwork = Network.read("testCaseNanInNetPositionComputation.uct", NetworkAreaTest.class.getResourceAsStream("/testCaseNanInNetPositionComputation.uct"));
        NetworkAreaFactory countryAreaCH = new CountryAreaFactory(Country.CH);
        NetworkAreaFactory countryAreaIT = new CountryAreaFactory(Country.IT);
        assertEquals(0, countryAreaCH.create(testNetwork).getNetPosition(), 1e-3);
        assertEquals(0, countryAreaIT.create(testNetwork).getNetPosition(), 1e-3);
    }

    @Test
    void testNetPositionPartOfLineIsZeroWhenLineTerminal1PIsNaN() {
        Network network = Network.read("testCaseSpecialDevices.xiidm", getClass().getResourceAsStream("/testCaseSpecialDevices.xiidm"));
        network.getLine("LINE_FR_ES").getTerminal1().setP(Double.NaN);
        assertEquals(75, countryAreaFactoryFR.create(network).getNetPosition(), 1e-3);
        assertEquals(-75, countryAreaFactoryES.create(network).getNetPosition(), 1e-3);
    }

    @Test
    void testNetPositionPartOfLineIsZeroWhenLineTerminal2PIsNaN() {
        Network network = Network.read("testCaseSpecialDevices.xiidm", getClass().getResourceAsStream("/testCaseSpecialDevices.xiidm"));
        network.getLine("LINE_FR_ES").getTerminal2().setP(Double.NaN);
        assertEquals(75, countryAreaFactoryFR.create(network).getNetPosition(), 1e-3);
        assertEquals(-75, countryAreaFactoryES.create(network).getNetPosition(), 1e-3);
    }

    @Test
    void testNetPositionPartOfHvdcIsZeroWhenHvdcTerminal1PIsNaN() {
        Network network = Network.read("testCaseSpecialDevices.xiidm", getClass().getResourceAsStream("/testCaseSpecialDevices.xiidm"));
        network.getHvdcLine("HVDC_FR_ES").getConverterStation1().getTerminal().setP(Double.NaN);
        assertEquals(75, countryAreaFactoryFR.create(network).getNetPosition(), 1e-3);
        assertEquals(-75, countryAreaFactoryES.create(network).getNetPosition(), 1e-3);
    }

    @Test
    void testNetPositionPartOfHvdcIsZeroWhenHvdcTerminal2PIsNaN() {
        Network network = Network.read("testCaseSpecialDevices.xiidm", getClass().getResourceAsStream("/testCaseSpecialDevices.xiidm"));
        network.getHvdcLine("HVDC_FR_ES").getConverterStation2().getTerminal().setP(Double.NaN);
        assertEquals(75, countryAreaFactoryFR.create(network).getNetPosition(), 1e-3);
        assertEquals(-75, countryAreaFactoryES.create(network).getNetPosition(), 1e-3);
    }
}
