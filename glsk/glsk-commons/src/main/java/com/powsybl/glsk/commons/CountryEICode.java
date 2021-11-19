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
    private String codeString; //find in Market_Areas_v1.0.pdf

    /**
     * country
     */
    private Country country;

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
        switch (codeString) {
            case "10YBE----------2" : return Country.BE;
            case "10YSK-SEPS-----K" : return Country.SK;
            case "10YCB-GERMANY--8" : return Country.DE;
            case "10YHU-MAVIR----U" : return Country.HU;
            case "10YNL----------L" : return Country.NL;
            case "10YAT-APG------L" : return Country.AT;
            case "10YCZ-CEPS-----N" : return Country.CZ;
            case "10YHR-HEP------M" : return Country.HR;
            case "10YPL-AREA-----S" : return Country.PL;
            case "10YRO-TEL------P" : return Country.RO;
            case "10YSI-ELES-----O" : return Country.SI;
            case "10YFR-RTE------C" : return Country.FR;
//            case "10YDE-ENBW-----N" : return Country.DE;//a ControlArea
//            case "10YDE-EON------1" : return Country.DE;//b ControlArea
//            case "10YDE-RWENET---I" : return Country.DE;//c ControlArea
//            case "10YDE-VE-------2" : return Country.DE;//d ControlArea
            case "10YES-REE------0" : return Country.ES;
            case "10YCS-SERBIATSOV" : return Country.RS;
            case "10YCB-SWITZERL-D" :
            case "10YCH-SWISSGRIDZ" :
                return Country.CH;
            case "10YPT-REN------W" : return Country.PT;
            case "10YCA-BULGARIA-R" :
            case "10YCB-BULGARIA-F" :
                return Country.BG;
            case "10YAL-KESH-----5" :
            case "10YCB-ALBANIA--1" :
                return Country.AL;
            case "10YCB-TURKEY---V" :
            case "10YTR-TEIAS----W" :
                return Country.TR;
            case "10Y1001C--00003F" : return Country.UA;
            case "10YMK-MEPSO----8" : return Country.MK;
            case "10YBA-JPCC-----D" : return Country.BA;
            case "10YCS-CG-TSO---S" : return Country.ME;
            case "10YCB-GREECE---2" :
            case "10YGR-HTSO-----Y" :
                return Country.GR;
            case "10YCB-ITALY----1" :
            case "10YIT-GRTN-----B" :
                return Country.IT;
            case "10YDK-1--------W" : return Country.DK;
            default: throw new IllegalArgumentException(String.format("Unknown CountryEICode: %s.", codeString));
        }
    }

    public String getCode() {
        switch (country) {
            case BE : return "10YBE----------2";
            case SK : return "10YSK-SEPS-----K";
            case DE : return "10YCB-GERMANY--8";
            case HU : return "10YHU-MAVIR----U";
            case NL : return "10YNL----------L";
            case AT : return "10YAT-APG------L";
            case CZ : return "10YCZ-CEPS-----N";
            case HR : return "10YHR-HEP------M";
            case PL : return "10YPL-AREA-----S";
            case RO : return "10YRO-TEL------P";
            case SI : return "10YSI-ELES-----O";
            case FR : return "10YFR-RTE------C";
            case ES : return "10YES-REE------0";
            case RS : return "10YCS-SERBIATSOV";
            case CH : return "10YCH-SWISSGRIDZ";
            case PT : return "10YPT-REN------W";
            case BG : return "10YCA-BULGARIA-R";
            case AL : return "10YAL-KESH-----5";
            case TR : return "10YTR-TEIAS----W";
            case UA : return "10Y1001C--00003F";
            case MK : return "10YMK-MEPSO----8";
            case BA : return "10YBA-JPCC-----D";
            case ME : return "10YCS-CG-TSO---S";
            case GR : return "10YGR-HTSO-----Y";
            case IT : return "10YIT-GRTN-----B";
            case DK : return "10YDK-1--------W";
            default: throw new IllegalArgumentException(String.format("Unknown CountryEICode for Country %s.", country.toString()));
        }
    }

}
