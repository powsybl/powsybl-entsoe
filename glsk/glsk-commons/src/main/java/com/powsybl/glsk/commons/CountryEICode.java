/*
 *  Copyright (c) 2026, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.glsk.commons;

import com.powsybl.iidm.network.Country;

import java.util.Map;

/**
 * @author Pengbo Wang {@literal <pengbo.wang@rte-international.com>}
 * @author Hugo Schindler{@literal <hugo.schindler at rte-france.com>}
 * EIC = Energy Identification Code
 */
public class CountryEICode {
    //EIC = Energy Identification Code
    private static final Map<String, Country> COUNTRIES_BY_CODE = Map.ofEntries(
        Map.entry("10Y1001A1001A39I", Country.EE),
        Map.entry("10Y1001A1001A990", Country.MD),
        Map.entry("10Y1001C--00003F", Country.UA),
        Map.entry("10Y1001C--00100H", Country.XK),
        Map.entry("10YAL-KESH-----5", Country.AL),
        Map.entry("10YAT-APG------L", Country.AT),
        Map.entry("10YBA-JPCC-----D", Country.BA),
        Map.entry("10YBE----------2", Country.BE),
        Map.entry("10YCA-BULGARIA-R", Country.BG),
        Map.entry("10YCB-ALBANIA--1", Country.AL),
        Map.entry("10YCB-BULGARIA-F", Country.BG),
        Map.entry("10YCB-GERMANY--8", Country.DE),
        Map.entry("10YCB-GREECE---2", Country.GR),
        Map.entry("10YCB-ITALY----1", Country.IT),
        Map.entry("10YCB-SWITZERL-D", Country.CH),
        Map.entry("10YCB-TURKEY---V", Country.TR),
        Map.entry("10YCH-SWISSGRIDZ", Country.CH),
        Map.entry("10YCS-CG-TSO---S", Country.ME),
        Map.entry("10YCS-SERBIATSOV", Country.RS),
        Map.entry("10YCZ-CEPS-----N", Country.CZ),
        Map.entry("10YDE-ENBW-----N", Country.DE),
        Map.entry("10YDE-EON------1", Country.DE),
        Map.entry("10YDE-RWENET---I", Country.DE),
        Map.entry("10YDE-VE-------2", Country.DE),
        Map.entry("10YDK-1--------W", Country.DK),
        Map.entry("10YES-REE------0", Country.ES),
        Map.entry("10YFI-1--------U", Country.FI),
        Map.entry("10YFR-RTE------C", Country.FR),
        Map.entry("10YGR-HTSO-----Y", Country.GR),
        Map.entry("10YHR-HEP------M", Country.HR),
        Map.entry("10YHU-MAVIR----U", Country.HU),
        Map.entry("10YIE-1001A00010", Country.IE),
        Map.entry("10YIT-GRTN-----B", Country.IT),
        Map.entry("10YLT-1001A0008Q", Country.LT),
        Map.entry("10YLU-CEGEDEL-NQ", Country.LU),
        Map.entry("10YLV-1001A00074", Country.LV),
        Map.entry("10YMK-MEPSO----8", Country.MK),
        Map.entry("10YNL----------L", Country.NL),
        Map.entry("10YNO-0--------C", Country.NO),
        Map.entry("10YPL-AREA-----S", Country.PL),
        Map.entry("10YPT-REN------W", Country.PT),
        Map.entry("10YRO-TEL------P", Country.RO),
        Map.entry("10YSE-1--------K", Country.SE),
        Map.entry("10YSI-ELES-----O", Country.SI),
        Map.entry("10YSK-SEPS-----K", Country.SK),
        Map.entry("10YTR-TEIAS----W", Country.TR)
    );

    private static final Map<Country, String> CODES_BY_COUNTRY = Map.ofEntries(
        Map.entry(Country.AL, "10YAL-KESH-----5"),
        Map.entry(Country.AT, "10YAT-APG------L"),
        Map.entry(Country.BA, "10YBA-JPCC-----D"),
        Map.entry(Country.BE, "10YBE----------2"),
        Map.entry(Country.BG, "10YCA-BULGARIA-R"),
        Map.entry(Country.CH, "10YCH-SWISSGRIDZ"),
        Map.entry(Country.CZ, "10YCZ-CEPS-----N"),
        Map.entry(Country.DE, "10YCB-GERMANY--8"),
        Map.entry(Country.DK, "10YDK-1--------W"),
        Map.entry(Country.EE, "10Y1001A1001A39I"),
        Map.entry(Country.ES, "10YES-REE------0"),
        Map.entry(Country.FI, "10YFI-1--------U"),
        Map.entry(Country.FR, "10YFR-RTE------C"),
        Map.entry(Country.GR, "10YGR-HTSO-----Y"),
        Map.entry(Country.HR, "10YHR-HEP------M"),
        Map.entry(Country.HU, "10YHU-MAVIR----U"),
        Map.entry(Country.IE, "10YIE-1001A00010"),
        Map.entry(Country.IT, "10YIT-GRTN-----B"),
        Map.entry(Country.LT, "10YLT-1001A0008Q"),
        Map.entry(Country.LU, "10YLU-CEGEDEL-NQ"),
        Map.entry(Country.LV, "10YLV-1001A00074"),
        Map.entry(Country.MD, "10Y1001A1001A990"),
        Map.entry(Country.ME, "10YCS-CG-TSO---S"),
        Map.entry(Country.MK, "10YMK-MEPSO----8"),
        Map.entry(Country.NL, "10YNL----------L"),
        Map.entry(Country.NO, "10YNO-0--------C"),
        Map.entry(Country.PL, "10YPL-AREA-----S"),
        Map.entry(Country.PT, "10YPT-REN------W"),
        Map.entry(Country.RO, "10YRO-TEL------P"),
        Map.entry(Country.RS, "10YCS-SERBIATSOV"),
        Map.entry(Country.SE, "10YSE-1--------K"),
        Map.entry(Country.SI, "10YSI-ELES-----O"),
        Map.entry(Country.SK, "10YSK-SEPS-----K"),
        Map.entry(Country.TR, "10YTR-TEIAS----W"),
        Map.entry(Country.UA, "10Y1001C--00003F"),
        Map.entry(Country.XK, "10Y1001C--00100H")
    );

    /**
     * code string
     */
    private final String codeString; //find in Market_Areas_v1.0.pdf

    /**
     * country
     */
    private final Country country;

    /**
     * @param codeString default constructor
     */
    public CountryEICode(String codeString) {
        this.codeString = codeString;
        this.country = getCountry();
    }

    /**
     * @param country default constructor
     */
    public CountryEICode(Country country) {
        this.country = country;
        this.codeString = getCode();
    }

    /**
     * @return return Country
     */
    public Country getCountry() {
        Country mappedCountry = COUNTRIES_BY_CODE.get(codeString);
        if (mappedCountry == null) {
            throw new IllegalArgumentException("Unknown CountryEICode: " + codeString + ".");
        }
        return mappedCountry;
    }

    public String getCode() {
        String mappedCode = CODES_BY_COUNTRY.get(country);
        if (mappedCode == null) {
            throw new IllegalArgumentException("Unknown CountryEICode for Country " + country + ".");
        }
        return mappedCode;
    }

}
