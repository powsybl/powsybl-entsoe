/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.entsoe.utils;

import com.powsybl.iidm.network.Country;

import java.util.Set;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public enum BiddingZone {
    AL("10YAL-KESH-----5", "Albania", Country.AL),
    AT("10YAT-APG------L", "Austria", Country.AT),
    BA("10YBA-JPCC-----D", "Bosnia and Herzegovina", Country.BA),
    BE("10YBE----------2", "Belgium", Country.BE),
    BG("10YCA-BULGARIA-R", "Bulgaria", Country.BG),
    CH("10YCH-SWISSGRIDZ", "Switzerland", Country.CH),
    CY("10YCY-1001A0003J", "Cyprus", Country.CY),
    CZ("10YCZ-CEPS-----N", "Czech Republic", Country.CZ),
    DE_LU("10Y1001A1001A82H", "Germany - Luxemburg", Country.DE, Country.LU),
    DK1("10YDK-1--------W", "Denmark West", Country.DK),
    DK2("10YDK-2--------M", "Denmark East", Country.DK),
    EE("10Y1001A1001A39I", "Estonia", Country.EE),
    ES("10YES-REE------0", "Spain", Country.ES),
    FI("10YFI-1--------U", "Finland", Country.FI),
    FR("10YFR-RTE------C", "France", Country.FR),
    GB("10YGB----------A", "Great Britain", Country.GB),
    GR("10YGR-HTSO-----Y", "Greece", Country.GR),
    HR("10YHR-HEP------M", "Croatia", Country.HR),
    HU("10YHU-MAVIR----U", "Hungary", Country.HU),
    IS("10Y1001A1001A958", "Iceland", Country.IS),
    IT_CALABRIA("10Y1001C--00096J", "Italy Calabria", Country.IT),
    IT_CENTRE_NORTH("10Y1001A1001A70O", "Italy Centre-North", Country.IT),
    IT_CENTRE_SOUTH("10Y1001A1001A71M", "Italy Centre-South", Country.IT),
    IT_NORTH("10Y1001A1001A73I", "Italy North", Country.IT),
    IT_SARDINIA("10Y1001A1001A74G", "Italy Sardinia", Country.IT),
    IT_SICILY("10Y1001A1001A75E", "Italy Sicily", Country.IT),
    IT_SOUTH("10Y1001A1001A788", "Italy South", Country.IT),
    LT("10YLT-1001A0008Q", "Lithuania", Country.LT),
    LV("10YLV-1001A00074", "Latvia", Country.LV),
    ME("10YCS-CG-TSO---S", "Montenegro", Country.ME),
    MK("10YMK-MEPSO----8", "Macedonia", Country.MK),
    NL("10YNL----------L", "Netherlands", Country.NL),
    NO1("10YNO-1--------2", "Norwegian Area Elspot Area 1", Country.NO),
    NO2("10YNO-2--------T", "Norwegian Area Elspot Area 2", Country.NO),
    NO3("10YNO-3--------J", "Norwegian Area Elspot Area 3", Country.NO),
    NO4("10YNO-4--------9", "Norwegian Area Elspot Area 4", Country.NO),
    NO5("10Y1001A1001A48H", "Norwegian Area Elspot Area 5", Country.NO),
    PL("10YPL-AREA-----S", "Poland", Country.PL),
    PT("10YPT-REN------W", "Portugal", Country.PT),
    RO("10YRO-TEL------P", "Romania", Country.RO),
    RS("10YCS-SERBIATSOV", "Serbia", Country.RS),
    SE1("10Y1001A1001A44P", "Swedish Elspot Area 1", Country.SE),
    SE2("10Y1001A1001A45N", "Swedish Elspot Area 2", Country.SE),
    SE3("10Y1001A1001A46L", "Swedish Elspot Area 3", Country.SE),
    SE4("10Y1001A1001A47J", "Swedish Elspot Area 4", Country.SE),
    SEM("10Y1001A1001A59C", "Ireland and Northern Ireland", Country.IE, Country.GB),
    SI("10YSI-ELES-----O", "Slovenia", Country.SI),
    SK("10YSK-SEPS-----K", "Slovak Republic", Country.SK);

    private final String eicCode;
    private final String name;
    private final Country[] countries;

    BiddingZone(String eicCode, String name, Country... countries) {
        this.eicCode = eicCode;
        this.name = name;
        this.countries = countries;
    }

    public String getEIC() {
        return eicCode;
    }

    public String getName() {
        return name;
    }

    public Set<Country> getCountries() {
        return Set.of(countries);
    }

}
