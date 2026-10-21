/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */

package com.powsybl.entsoe.utils;

import com.powsybl.iidm.network.Country;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public class TransmissionSystemOperatorTest {
    @Test
    void testTSOFromEIC() {
        assertEquals(TransmissionSystemOperator._50HZ, TransmissionSystemOperator.fromEIC("10XDE-VE-TRANSMK"));
        assertEquals(TransmissionSystemOperator.ADMIE, TransmissionSystemOperator.fromEIC("10XGR-HTSO-----B"));
        assertEquals(TransmissionSystemOperator.AMPIRION, TransmissionSystemOperator.fromEIC("10XDE-RWENET---W"));
        assertEquals(TransmissionSystemOperator.APG, TransmissionSystemOperator.fromEIC("10XAT-APG------Z"));
        assertEquals(TransmissionSystemOperator.AST, TransmissionSystemOperator.fromEIC("10X1001A1001B54W"));
        assertEquals(TransmissionSystemOperator.CEPS, TransmissionSystemOperator.fromEIC("10X1001A1001A524"));
        assertEquals(TransmissionSystemOperator.CGES, TransmissionSystemOperator.fromEIC("10XCS-CG-TSO---5"));
        assertEquals(TransmissionSystemOperator.CREOS, TransmissionSystemOperator.fromEIC("21X000000001333E"));
        assertEquals(TransmissionSystemOperator.CYPRUS_TSO, TransmissionSystemOperator.fromEIC("10XCZ-CEPS-GRIDE"));
        assertEquals(TransmissionSystemOperator.EIRGRID, TransmissionSystemOperator.fromEIC("10X1001A1001A531"));
        assertEquals(TransmissionSystemOperator.ELERING, TransmissionSystemOperator.fromEIC("10X1001A1001A39W"));
        assertEquals(TransmissionSystemOperator.ELES, TransmissionSystemOperator.fromEIC("10XSI-ELES-----1"));
        assertEquals(TransmissionSystemOperator.ELIA, TransmissionSystemOperator.fromEIC("10X1001A1001A612"));
        assertEquals(TransmissionSystemOperator.EMS, TransmissionSystemOperator.fromEIC("10XCS-SERBIATSO8"));
        assertEquals(TransmissionSystemOperator.ENERGINET, TransmissionSystemOperator.fromEIC("10X1001A1001A248"));
        assertEquals(TransmissionSystemOperator.ESO, TransmissionSystemOperator.fromEIC("10XBG-ESO------A"));
        assertEquals(TransmissionSystemOperator.FINGRID, TransmissionSystemOperator.fromEIC("10X1001A1001A264"));
        assertEquals(TransmissionSystemOperator.HPOS, TransmissionSystemOperator.fromEIC("10XHR-HEP-OPS--A"));
        assertEquals(TransmissionSystemOperator.LANDSNET, TransmissionSystemOperator.fromEIC("10X1001A1001A280"));
        assertEquals(TransmissionSystemOperator.LITGRID, TransmissionSystemOperator.fromEIC("10X1001A1001A55Y"));
        assertEquals(TransmissionSystemOperator.MAVIR, TransmissionSystemOperator.fromEIC("10X1001A1001A329"));
        assertEquals(TransmissionSystemOperator.MEPSO, TransmissionSystemOperator.fromEIC("10XMK-MEPSO----M"));
        assertEquals(TransmissionSystemOperator.NOSBIH, TransmissionSystemOperator.fromEIC("10XBA-JPCCZEKC-K"));
        assertEquals(TransmissionSystemOperator.OST, TransmissionSystemOperator.fromEIC("10XAL-KESH-----J"));
        assertEquals(TransmissionSystemOperator.PSE, TransmissionSystemOperator.fromEIC("10XPL-TSO------P"));
        assertEquals(TransmissionSystemOperator.REE, TransmissionSystemOperator.fromEIC("10XES-REE------E"));
        assertEquals(TransmissionSystemOperator.REN, TransmissionSystemOperator.fromEIC("10XPT-REN------9"));
        assertEquals(TransmissionSystemOperator.RTE, TransmissionSystemOperator.fromEIC("10XFR-RTE------Q"));
        assertEquals(TransmissionSystemOperator.SEPS, TransmissionSystemOperator.fromEIC("10XSK-SEPS-GRIDB"));
        assertEquals(TransmissionSystemOperator.SONI, TransmissionSystemOperator.fromEIC("10XNITSO-12345-O"));
        assertEquals(TransmissionSystemOperator.STATNETT, TransmissionSystemOperator.fromEIC("10X1001A1001A38Y"));
        assertEquals(TransmissionSystemOperator.SVENSKA_KRAFNET, TransmissionSystemOperator.fromEIC("10X1001A1001A418"));
        assertEquals(TransmissionSystemOperator.SWISSGRID, TransmissionSystemOperator.fromEIC("10XCH-SWISSGRIDC"));
        assertEquals(TransmissionSystemOperator.TEL, TransmissionSystemOperator.fromEIC("10XRO-TEL------2"));
        assertEquals(TransmissionSystemOperator.TENNET, TransmissionSystemOperator.fromEIC("10X1001A1001A361"));
        assertEquals(TransmissionSystemOperator.TERNA, TransmissionSystemOperator.fromEIC("10X1001A1001A345"));
        assertEquals(TransmissionSystemOperator.TRANSNET, TransmissionSystemOperator.fromEIC("10XDE-ENBW--TNGX"));
        assertEquals(TransmissionSystemOperator.TTG, TransmissionSystemOperator.fromEIC("10XDE-EON-NETZ-C"));
        assertEquals(TransmissionSystemOperator.UKRENERGO, TransmissionSystemOperator.fromEIC("10X1001C--00001X"));
    }

    @Test
    void testTSOFromNonExistingEIC() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> TransmissionSystemOperator.fromEIC("10YABCDEFGHIJKLM"));
        assertEquals("No Transmission System Operator found with EIC 10YABCDEFGHIJKLM.", exception.getMessage());
    }

    @Test
    void testGetEIC() {
        assertEquals("10XDE-VE-TRANSMK", TransmissionSystemOperator._50HZ.getEIC());
        assertEquals("10XGR-HTSO-----B", TransmissionSystemOperator.ADMIE.getEIC());
        assertEquals("10XDE-RWENET---W", TransmissionSystemOperator.AMPIRION.getEIC());
        assertEquals("10XAT-APG------Z", TransmissionSystemOperator.APG.getEIC());
        assertEquals("10X1001A1001B54W", TransmissionSystemOperator.AST.getEIC());
        assertEquals("10X1001A1001A524", TransmissionSystemOperator.CEPS.getEIC());
        assertEquals("10XCS-CG-TSO---5", TransmissionSystemOperator.CGES.getEIC());
        assertEquals("21X000000001333E", TransmissionSystemOperator.CREOS.getEIC());
        assertEquals("10XCZ-CEPS-GRIDE", TransmissionSystemOperator.CYPRUS_TSO.getEIC());
        assertEquals("10X1001A1001A531", TransmissionSystemOperator.EIRGRID.getEIC());
        assertEquals("10X1001A1001A39W", TransmissionSystemOperator.ELERING.getEIC());
        assertEquals("10XSI-ELES-----1", TransmissionSystemOperator.ELES.getEIC());
        assertEquals("10X1001A1001A612", TransmissionSystemOperator.ELIA.getEIC());
        assertEquals("10XCS-SERBIATSO8", TransmissionSystemOperator.EMS.getEIC());
        assertEquals("10X1001A1001A248", TransmissionSystemOperator.ENERGINET.getEIC());
        assertEquals("10XBG-ESO------A", TransmissionSystemOperator.ESO.getEIC());
        assertEquals("10X1001A1001A264", TransmissionSystemOperator.FINGRID.getEIC());
        assertEquals("10XHR-HEP-OPS--A", TransmissionSystemOperator.HPOS.getEIC());
        assertEquals("10X1001A1001A280", TransmissionSystemOperator.LANDSNET.getEIC());
        assertEquals("10X1001A1001A55Y", TransmissionSystemOperator.LITGRID.getEIC());
        assertEquals("10X1001A1001A329", TransmissionSystemOperator.MAVIR.getEIC());
        assertEquals("10XMK-MEPSO----M", TransmissionSystemOperator.MEPSO.getEIC());
        assertEquals("10XBA-JPCCZEKC-K", TransmissionSystemOperator.NOSBIH.getEIC());
        assertEquals("10XAL-KESH-----J", TransmissionSystemOperator.OST.getEIC());
        assertEquals("10XPL-TSO------P", TransmissionSystemOperator.PSE.getEIC());
        assertEquals("10XES-REE------E", TransmissionSystemOperator.REE.getEIC());
        assertEquals("10XPT-REN------9", TransmissionSystemOperator.REN.getEIC());
        assertEquals("10XFR-RTE------Q", TransmissionSystemOperator.RTE.getEIC());
        assertEquals("10XSK-SEPS-GRIDB", TransmissionSystemOperator.SEPS.getEIC());
        assertEquals("10XNITSO-12345-O", TransmissionSystemOperator.SONI.getEIC());
        assertEquals("10X1001A1001A38Y", TransmissionSystemOperator.STATNETT.getEIC());
        assertEquals("10X1001A1001A418", TransmissionSystemOperator.SVENSKA_KRAFNET.getEIC());
        assertEquals("10XCH-SWISSGRIDC", TransmissionSystemOperator.SWISSGRID.getEIC());
        assertEquals("10XRO-TEL------2", TransmissionSystemOperator.TEL.getEIC());
        assertEquals("10X1001A1001A361", TransmissionSystemOperator.TENNET.getEIC());
        assertEquals("10X1001A1001A345", TransmissionSystemOperator.TERNA.getEIC());
        assertEquals("10XDE-ENBW--TNGX", TransmissionSystemOperator.TRANSNET.getEIC());
        assertEquals("10XDE-EON-NETZ-C", TransmissionSystemOperator.TTG.getEIC());
        assertEquals("10X1001C--00001X", TransmissionSystemOperator.UKRENERGO.getEIC());
    }

    @Test
    void testGetCountry() {
        assertEquals(Country.DE, TransmissionSystemOperator._50HZ.getCountry());
        assertEquals(Country.GR, TransmissionSystemOperator.ADMIE.getCountry());
        assertEquals(Country.DE, TransmissionSystemOperator.AMPIRION.getCountry());
        assertEquals(Country.AT, TransmissionSystemOperator.APG.getCountry());
        assertEquals(Country.LV, TransmissionSystemOperator.AST.getCountry());
        assertEquals(Country.CZ, TransmissionSystemOperator.CEPS.getCountry());
        assertEquals(Country.ME, TransmissionSystemOperator.CGES.getCountry());
        assertEquals(Country.LU, TransmissionSystemOperator.CREOS.getCountry());
        assertEquals(Country.CY, TransmissionSystemOperator.CYPRUS_TSO.getCountry());
        assertEquals(Country.IE, TransmissionSystemOperator.EIRGRID.getCountry());
        assertEquals(Country.EE, TransmissionSystemOperator.ELERING.getCountry());
        assertEquals(Country.SI, TransmissionSystemOperator.ELES.getCountry());
        assertEquals(Country.BE, TransmissionSystemOperator.ELIA.getCountry());
        assertEquals(Country.RS, TransmissionSystemOperator.EMS.getCountry());
        assertEquals(Country.DK, TransmissionSystemOperator.ENERGINET.getCountry());
        assertEquals(Country.BG, TransmissionSystemOperator.ESO.getCountry());
        assertEquals(Country.FI, TransmissionSystemOperator.FINGRID.getCountry());
        assertEquals(Country.HR, TransmissionSystemOperator.HPOS.getCountry());
        assertEquals(Country.IS, TransmissionSystemOperator.LANDSNET.getCountry());
        assertEquals(Country.LT, TransmissionSystemOperator.LITGRID.getCountry());
        assertEquals(Country.HU, TransmissionSystemOperator.MAVIR.getCountry());
        assertEquals(Country.MK, TransmissionSystemOperator.MEPSO.getCountry());
        assertEquals(Country.BA, TransmissionSystemOperator.NOSBIH.getCountry());
        assertEquals(Country.AL, TransmissionSystemOperator.OST.getCountry());
        assertEquals(Country.PL, TransmissionSystemOperator.PSE.getCountry());
        assertEquals(Country.ES, TransmissionSystemOperator.REE.getCountry());
        assertEquals(Country.PT, TransmissionSystemOperator.REN.getCountry());
        assertEquals(Country.FR, TransmissionSystemOperator.RTE.getCountry());
        assertEquals(Country.SK, TransmissionSystemOperator.SEPS.getCountry());
        assertEquals(Country.GB, TransmissionSystemOperator.SONI.getCountry());
        assertEquals(Country.NO, TransmissionSystemOperator.STATNETT.getCountry());
        assertEquals(Country.SE, TransmissionSystemOperator.SVENSKA_KRAFNET.getCountry());
        assertEquals(Country.CH, TransmissionSystemOperator.SWISSGRID.getCountry());
        assertEquals(Country.RO, TransmissionSystemOperator.TEL.getCountry());
        assertEquals(Country.NL, TransmissionSystemOperator.TENNET.getCountry());
        assertEquals(Country.IT, TransmissionSystemOperator.TERNA.getCountry());
        assertEquals(Country.DE, TransmissionSystemOperator.TRANSNET.getCountry());
        assertEquals(Country.DE, TransmissionSystemOperator.TTG.getCountry());
        assertEquals(Country.UA, TransmissionSystemOperator.UKRENERGO.getCountry());
    }
}
