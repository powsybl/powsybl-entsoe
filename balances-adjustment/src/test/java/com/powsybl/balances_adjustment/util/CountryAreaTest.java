/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.balances_adjustment.util;

import com.powsybl.commons.PowsyblException;
import com.powsybl.iidm.network.*;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.*;

/**
 * @author Ameni Walha {@literal <ameni.walha at rte-france.com>}
 */
public class CountryAreaTest {

    private Network testNetwork1;
    private Network testNetwork2;

    private CountryAreaFactory countryAreaFR;
    private CountryAreaFactory countryAreaES;
    private CountryAreaFactory countryAreaBE;

    @Before
    public void setUp() {
        testNetwork1 = Network.read("testCase.xiidm", getClass().getResourceAsStream("/testCase.xiidm"));
        testNetwork2 = NetworkTestFactory.createNetwork();

        countryAreaFR = new CountryAreaFactory(Country.FR);
        countryAreaES = new CountryAreaFactory(Country.ES);
        countryAreaBE = new CountryAreaFactory(Country.BE);
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
    public void testGetNetPosition() {
        //Test network with BranchBorder
        assertEquals(0, countryAreaES.create(testNetwork1).getNetPosition(), 1e-3);

        assertEquals(-getSumFlowCountry(testNetwork1, Country.FR), countryAreaFR.create(testNetwork1).getNetPosition(), 1e-3);

        //Test network with HVDCLines
        assertEquals(testNetwork2.getHvdcLine("hvdcLineFrEs").getConverterStation1().getTerminal().getP(), countryAreaFR.create(testNetwork2).getNetPosition(), 1e-3);
        assertEquals(testNetwork2.getHvdcLine("hvdcLineFrEs").getConverterStation2().getTerminal().getP(), countryAreaES.create(testNetwork2).getNetPosition(), 1e-3);
    }

    @Test
    public void testSpecialDevices() {
        Network network = Network.read("testCaseSpecialDevices.xiidm", getClass().getResourceAsStream("/testCaseSpecialDevices.xiidm"));
        assertEquals(100, countryAreaFR.create(network).getNetPosition(), 1e-3);
        assertEquals(-100, countryAreaES.create(network).getNetPosition(), 1e-3);
    }

    @Test
    public void testGetLeavingFlowToCountry() {
        CountryArea countryAreaFR2 = countryAreaFR.create(testNetwork2);
        CountryArea countryAreaES2 = countryAreaES.create(testNetwork2);
        CountryArea countryAreaFR1 = countryAreaFR.create(testNetwork1);
        CountryArea countryAreaBE1 = countryAreaBE.create(testNetwork1);
        CountryArea countryAreaES1 = countryAreaES.create(testNetwork1);

        assertEquals(100.0, countryAreaFR2.getLeavingFlowToCountry(countryAreaES2), 1e-3);
        assertEquals(-100.0, countryAreaES2.getLeavingFlowToCountry(countryAreaFR2), 1e-3);
        assertEquals(-324.666, countryAreaFR1.getLeavingFlowToCountry(countryAreaBE1), 1e-3);
        assertEquals(324.666, countryAreaBE1.getLeavingFlowToCountry(countryAreaFR1), 1e-3);
        assertEquals(0.0, countryAreaBE1.getLeavingFlowToCountry(countryAreaES1), 1e-3);
        try {
            countryAreaFR.create(testNetwork1).getLeavingFlowToCountry(countryAreaFR1);
            fail();
        } catch (PowsyblException e) {
            assertEquals("The leaving flow to the country area cannot be computed. The country FRANCE is contained in both control areas.", e.getMessage());
        }
    }
}
