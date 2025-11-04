/*
 *  Copyright (c) 2020, RTE (http://www.rte-france.com)
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.glsk.commons;

import com.powsybl.iidm.network.Country;

/**
 * @author Pengbo Wang {@literal <pengbo.wang@rte-international.com>}
 * EIC = Energy Identification Code
 */
public class CountryEICode {
    //EIC = Energy Identification Code

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
        return switch (codeString) {
            case "10YBE----------2" -> Country.BE;
            case "10YSK-SEPS-----K" -> Country.SK;
            case "10YDE-VE-------2", "10YDE-RWENET---I", "10YDE-ENBW-----N", "10YDE-EON------1", "10YCB-GERMANY--8" -> Country.DE;
            case "10YHU-MAVIR----U" -> Country.HU;
            case "10YNL----------L" -> Country.NL;
            case "10YAT-APG------L" -> Country.AT;
            case "10YCZ-CEPS-----N" -> Country.CZ;
            case "10YHR-HEP------M" -> Country.HR;
            case "10YPL-AREA-----S" -> Country.PL;
            case "10YRO-TEL------P" -> Country.RO;
            case "10YSI-ELES-----O" -> Country.SI;
            case "10YFR-RTE------C" -> Country.FR;
            case "10YES-REE------0" -> Country.ES;
            case "10YCS-SERBIATSOV" -> Country.RS;
            case "10YCB-SWITZERL-D", "10YCH-SWISSGRIDZ" -> Country.CH;
            case "10YPT-REN------W" -> Country.PT;
            case "10YCA-BULGARIA-R", "10YCB-BULGARIA-F" -> Country.BG;
            case "10YAL-KESH-----5", "10YCB-ALBANIA--1" -> Country.AL;
            case "10YTR-TEIAS----W", "10YCB-TURKEY---V" -> Country.TR;
            case "10Y1001C--00003F" -> Country.UA;
            case "10YMK-MEPSO----8" -> Country.MK;
            case "10YBA-JPCC-----D" -> Country.BA;
            case "10YCS-CG-TSO---S" -> Country.ME;
            case "10YGR-HTSO-----Y", "10YCB-GREECE---2" -> Country.GR;
            case "10YIT-GRTN-----B", "10YCB-ITALY----1" -> Country.IT;
            case "10YDK-1--------W" -> Country.DK;
            case "10Y1001C--00100H" -> Country.XK;
            case "10YLU-CEGEDEL-NQ" -> Country.LU;
            default -> throw new IllegalArgumentException("Unknown CountryEICode: " + codeString + ".");
        };
    }

    public String getCode() {
        return switch (country) {
            case BE -> "10YBE----------2";
            case SK -> "10YSK-SEPS-----K";
            case DE -> "10YCB-GERMANY--8";
            case HU -> "10YHU-MAVIR----U";
            case NL -> "10YNL----------L";
            case AT -> "10YAT-APG------L";
            case CZ -> "10YCZ-CEPS-----N";
            case HR -> "10YHR-HEP------M";
            case PL -> "10YPL-AREA-----S";
            case RO -> "10YRO-TEL------P";
            case SI -> "10YSI-ELES-----O";
            case FR -> "10YFR-RTE------C";
            case ES -> "10YES-REE------0";
            case RS -> "10YCS-SERBIATSOV";
            case CH -> "10YCH-SWISSGRIDZ";
            case PT -> "10YPT-REN------W";
            case BG -> "10YCA-BULGARIA-R";
            case AL -> "10YAL-KESH-----5";
            case TR -> "10YTR-TEIAS----W";
            case UA -> "10Y1001C--00003F";
            case MK -> "10YMK-MEPSO----8";
            case BA -> "10YBA-JPCC-----D";
            case ME -> "10YCS-CG-TSO---S";
            case GR -> "10YGR-HTSO-----Y";
            case IT -> "10YIT-GRTN-----B";
            case DK -> "10YDK-1--------W";
            case XK -> "10Y1001C--00100H";
            case LU -> "10YLU-CEGEDEL-NQ";
            default -> throw new IllegalArgumentException("Unknown CountryEICode for Country " + country + ".");
        };
    }

}
