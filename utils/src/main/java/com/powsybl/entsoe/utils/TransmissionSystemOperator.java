/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.entsoe.utils;

import com.powsybl.iidm.network.Country;

/**
 * EIC Codes of all ENTSO-E members.
 *
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public enum TransmissionSystemOperator {
    _50HZ("10XDE-VE-TRANSMK", Country.DE),
    ADMIE("10XGR-HTSO-----B", Country.GR),
    AMPIRION("10XDE-RWENET---W", Country.DE),
    APG("10XAT-APG------Z", Country.AT),
    AST("10X1001A1001B54W", Country.LV),
    CEPS("10X1001A1001A524", Country.CZ),
    CGES("10XCS-CG-TSO---5", Country.ME),
    CREOS("21X000000001333E", Country.LU),
    CYPRUS_TSO("10XCZ-CEPS-GRIDE", Country.CY),
    EIRGRID("10X1001A1001A531", Country.IE),
    ELERING("10X1001A1001A39W", Country.EE),
    ELES("10XSI-ELES-----1", Country.SI),
    ELIA("10X1001A1001A612", Country.BE),
    EMS("10XCS-SERBIATSO8", Country.RS),
    ENERGINET("10X1001A1001A248", Country.DK),
    ESO("10XBG-ESO------A", Country.BG),
    FINGRID("10X1001A1001A264", Country.FI),
    HPOS("10XHR-HEP-OPS--A", Country.HR),
    LANDSNET("10X1001A1001A280", Country.IS),
    LITGRID("10X1001A1001A55Y", Country.LT),
    MAVIR("10X1001A1001A329", Country.HU),
    MEPSO("10XMK-MEPSO----M", Country.MK),
    NOSBIH("10XBA-JPCCZEKC-K", Country.BA),
    OST("10XAL-KESH-----J", Country.AL),
    PSE("10XPL-TSO------P", Country.PL),
    REE("10XES-REE------E", Country.ES),
    REN("10XPT-REN------9", Country.PT),
    RTE("10XFR-RTE------Q", Country.FR),
    SEPS("10XSK-SEPS-GRIDB", Country.SK),
    SONI("10XNITSO-12345-O", Country.GB),
    STATNETT("10X1001A1001A38Y", Country.NO),
    SVENSKA_KRAFNET("10X1001A1001A418", Country.SE),
    SWISSGRID("10XCH-SWISSGRIDC", Country.CH),
    TEL("10XRO-TEL------2", Country.RO),
    TENNET("10X1001A1001A361", Country.NL),
    TERNA("10X1001A1001A345", Country.IT),
    TRANSNET("10XDE-ENBW--TNGX", Country.DE),
    TTG("10XDE-EON-NETZ-C", Country.DE),
    UKRENERGO("10X1001C--00001X", Country.UA);

    private final String eic;
    private final Country country;

    TransmissionSystemOperator(String eic, Country country) {
        this.eic = eic;
        this.country = country;
    }

    public String getEIC() {
        return eic;
    }

    public Country getCountry() {
        return country;
    }

    public static TransmissionSystemOperator fromEIC(String eic) {
        for (TransmissionSystemOperator tso : values()) {
            if (tso.getEIC().equals(eic)) {
                return tso;
            }
        }
        throw new IllegalArgumentException("No Transmission System Operator found with EIC %s.".formatted(eic));
    }
}
