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

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Thomas Bouquet {@literal <thomas.bouquet at rte-france.com>}
 */
public class BiddingZoneTest {
    @Test
    void testGetEIC() {
        assertEquals("10YAL-KESH-----5", BiddingZone.AL.getEIC());
        assertEquals("10YAT-APG------L", BiddingZone.AT.getEIC());
        assertEquals("10YBA-JPCC-----D", BiddingZone.BA.getEIC());
        assertEquals("10YBE----------2", BiddingZone.BE.getEIC());
        assertEquals("10YCA-BULGARIA-R", BiddingZone.BG.getEIC());
        assertEquals("10YCH-SWISSGRIDZ", BiddingZone.CH.getEIC());
        assertEquals("10YCY-1001A0003J", BiddingZone.CY.getEIC());
        assertEquals("10YCZ-CEPS-----N", BiddingZone.CZ.getEIC());
        assertEquals("10Y1001A1001A82H", BiddingZone.DE_LU.getEIC());
        assertEquals("10YDK-1--------W", BiddingZone.DK1.getEIC());
        assertEquals("10YDK-2--------M", BiddingZone.DK2.getEIC());
        assertEquals("10Y1001A1001A39I", BiddingZone.EE.getEIC());
        assertEquals("10YES-REE------0", BiddingZone.ES.getEIC());
        assertEquals("10YFI-1--------U", BiddingZone.FI.getEIC());
        assertEquals("10YFR-RTE------C", BiddingZone.FR.getEIC());
        assertEquals("10YGB----------A", BiddingZone.GB.getEIC());
        assertEquals("10YGR-HTSO-----Y", BiddingZone.GR.getEIC());
        assertEquals("10YHR-HEP------M", BiddingZone.HR.getEIC());
        assertEquals("10YHU-MAVIR----U", BiddingZone.HU.getEIC());
        assertEquals("10Y1001A1001A958", BiddingZone.IS.getEIC());
        assertEquals("10Y1001C--00096J", BiddingZone.IT_CALABRIA.getEIC());
        assertEquals("10Y1001A1001A70O", BiddingZone.IT_CENTRE_NORTH.getEIC());
        assertEquals("10Y1001A1001A71M", BiddingZone.IT_CENTRE_SOUTH.getEIC());
        assertEquals("10Y1001A1001A73I", BiddingZone.IT_NORTH.getEIC());
        assertEquals("10Y1001A1001A74G", BiddingZone.IT_SARDINIA.getEIC());
        assertEquals("10Y1001A1001A75E", BiddingZone.IT_SICILY.getEIC());
        assertEquals("10Y1001A1001A788", BiddingZone.IT_SOUTH.getEIC());
        assertEquals("10YLT-1001A0008Q", BiddingZone.LT.getEIC());
        assertEquals("10YLV-1001A00074", BiddingZone.LV.getEIC());
        assertEquals("10YCS-CG-TSO---S", BiddingZone.ME.getEIC());
        assertEquals("10YMK-MEPSO----8", BiddingZone.MK.getEIC());
        assertEquals("10YNL----------L", BiddingZone.NL.getEIC());
        assertEquals("10YNO-1--------2", BiddingZone.NO1.getEIC());
        assertEquals("10YNO-2--------T", BiddingZone.NO2.getEIC());
        assertEquals("10YNO-3--------J", BiddingZone.NO3.getEIC());
        assertEquals("10YNO-4--------9", BiddingZone.NO4.getEIC());
        assertEquals("10Y1001A1001A48H", BiddingZone.NO5.getEIC());
        assertEquals("10YPL-AREA-----S", BiddingZone.PL.getEIC());
        assertEquals("10YPT-REN------W", BiddingZone.PT.getEIC());
        assertEquals("10YRO-TEL------P", BiddingZone.RO.getEIC());
        assertEquals("10YCS-SERBIATSOV", BiddingZone.RS.getEIC());
        assertEquals("10Y1001A1001A44P", BiddingZone.SE1.getEIC());
        assertEquals("10Y1001A1001A45N", BiddingZone.SE2.getEIC());
        assertEquals("10Y1001A1001A46L", BiddingZone.SE3.getEIC());
        assertEquals("10Y1001A1001A47J", BiddingZone.SE4.getEIC());
        assertEquals("10Y1001A1001A59C", BiddingZone.SEM.getEIC());
        assertEquals("10YSI-ELES-----O", BiddingZone.SI.getEIC());
        assertEquals("10YSK-SEPS-----K", BiddingZone.SK.getEIC());
    }

    @Test
    void testGetName() {
        assertEquals("Albania", BiddingZone.AL.getName());
        assertEquals("Austria", BiddingZone.AT.getName());
        assertEquals("Bosnia and Herzegovina", BiddingZone.BA.getName());
        assertEquals("Belgium", BiddingZone.BE.getName());
        assertEquals("Bulgaria", BiddingZone.BG.getName());
        assertEquals("Switzerland", BiddingZone.CH.getName());
        assertEquals("Cyprus", BiddingZone.CY.getName());
        assertEquals("Czech Republic", BiddingZone.CZ.getName());
        assertEquals("Germany - Luxemburg", BiddingZone.DE_LU.getName());
        assertEquals("Denmark West", BiddingZone.DK1.getName());
        assertEquals("Denmark East", BiddingZone.DK2.getName());
        assertEquals("Estonia", BiddingZone.EE.getName());
        assertEquals("Spain", BiddingZone.ES.getName());
        assertEquals("Finland", BiddingZone.FI.getName());
        assertEquals("France", BiddingZone.FR.getName());
        assertEquals("Great Britain", BiddingZone.GB.getName());
        assertEquals("Greece", BiddingZone.GR.getName());
        assertEquals("Croatia", BiddingZone.HR.getName());
        assertEquals("Hungary", BiddingZone.HU.getName());
        assertEquals("Iceland", BiddingZone.IS.getName());
        assertEquals("Italy Calabria", BiddingZone.IT_CALABRIA.getName());
        assertEquals("Italy Centre-North", BiddingZone.IT_CENTRE_NORTH.getName());
        assertEquals("Italy Centre-South", BiddingZone.IT_CENTRE_SOUTH.getName());
        assertEquals("Italy North", BiddingZone.IT_NORTH.getName());
        assertEquals("Italy Sardinia", BiddingZone.IT_SARDINIA.getName());
        assertEquals("Italy Sicily", BiddingZone.IT_SICILY.getName());
        assertEquals("Italy South", BiddingZone.IT_SOUTH.getName());
        assertEquals("Lithuania", BiddingZone.LT.getName());
        assertEquals("Latvia", BiddingZone.LV.getName());
        assertEquals("Montenegro", BiddingZone.ME.getName());
        assertEquals("Macedonia", BiddingZone.MK.getName());
        assertEquals("Netherlands", BiddingZone.NL.getName());
        assertEquals("Norwegian Area Elspot Area 1", BiddingZone.NO1.getName());
        assertEquals("Norwegian Area Elspot Area 2", BiddingZone.NO2.getName());
        assertEquals("Norwegian Area Elspot Area 3", BiddingZone.NO3.getName());
        assertEquals("Norwegian Area Elspot Area 4", BiddingZone.NO4.getName());
        assertEquals("Norwegian Area Elspot Area 5", BiddingZone.NO5.getName());
        assertEquals("Poland", BiddingZone.PL.getName());
        assertEquals("Portugal", BiddingZone.PT.getName());
        assertEquals("Romania", BiddingZone.RO.getName());
        assertEquals("Serbia", BiddingZone.RS.getName());
        assertEquals("Swedish Elspot Area 1", BiddingZone.SE1.getName());
        assertEquals("Swedish Elspot Area 2", BiddingZone.SE2.getName());
        assertEquals("Swedish Elspot Area 3", BiddingZone.SE3.getName());
        assertEquals("Swedish Elspot Area 4", BiddingZone.SE4.getName());
        assertEquals("Ireland and Northern Ireland", BiddingZone.SEM.getName());
        assertEquals("Slovenia", BiddingZone.SI.getName());
        assertEquals("Slovak Republic", BiddingZone.SK.getName());
    }

    @Test
    void testGetCountries() {
        assertEquals(Set.of(Country.AL), BiddingZone.AL.getCountries());
        assertEquals(Set.of(Country.AT), BiddingZone.AT.getCountries());
        assertEquals(Set.of(Country.BA), BiddingZone.BA.getCountries());
        assertEquals(Set.of(Country.BE), BiddingZone.BE.getCountries());
        assertEquals(Set.of(Country.BG), BiddingZone.BG.getCountries());
        assertEquals(Set.of(Country.CH), BiddingZone.CH.getCountries());
        assertEquals(Set.of(Country.CY), BiddingZone.CY.getCountries());
        assertEquals(Set.of(Country.CZ), BiddingZone.CZ.getCountries());
        assertEquals(Set.of(Country.DE, Country.LU), BiddingZone.DE_LU.getCountries());
        assertEquals(Set.of(Country.DK), BiddingZone.DK1.getCountries());
        assertEquals(Set.of(Country.DK), BiddingZone.DK2.getCountries());
        assertEquals(Set.of(Country.EE), BiddingZone.EE.getCountries());
        assertEquals(Set.of(Country.ES), BiddingZone.ES.getCountries());
        assertEquals(Set.of(Country.FI), BiddingZone.FI.getCountries());
        assertEquals(Set.of(Country.FR), BiddingZone.FR.getCountries());
        assertEquals(Set.of(Country.GB), BiddingZone.GB.getCountries());
        assertEquals(Set.of(Country.GR), BiddingZone.GR.getCountries());
        assertEquals(Set.of(Country.HR), BiddingZone.HR.getCountries());
        assertEquals(Set.of(Country.HU), BiddingZone.HU.getCountries());
        assertEquals(Set.of(Country.IS), BiddingZone.IS.getCountries());
        assertEquals(Set.of(Country.IT), BiddingZone.IT_CALABRIA.getCountries());
        assertEquals(Set.of(Country.IT), BiddingZone.IT_CENTRE_NORTH.getCountries());
        assertEquals(Set.of(Country.IT), BiddingZone.IT_CENTRE_SOUTH.getCountries());
        assertEquals(Set.of(Country.IT), BiddingZone.IT_NORTH.getCountries());
        assertEquals(Set.of(Country.IT), BiddingZone.IT_SARDINIA.getCountries());
        assertEquals(Set.of(Country.IT), BiddingZone.IT_SICILY.getCountries());
        assertEquals(Set.of(Country.IT), BiddingZone.IT_SOUTH.getCountries());
        assertEquals(Set.of(Country.LT), BiddingZone.LT.getCountries());
        assertEquals(Set.of(Country.LV), BiddingZone.LV.getCountries());
        assertEquals(Set.of(Country.ME), BiddingZone.ME.getCountries());
        assertEquals(Set.of(Country.MK), BiddingZone.MK.getCountries());
        assertEquals(Set.of(Country.NL), BiddingZone.NL.getCountries());
        assertEquals(Set.of(Country.NO), BiddingZone.NO1.getCountries());
        assertEquals(Set.of(Country.NO), BiddingZone.NO2.getCountries());
        assertEquals(Set.of(Country.NO), BiddingZone.NO3.getCountries());
        assertEquals(Set.of(Country.NO), BiddingZone.NO4.getCountries());
        assertEquals(Set.of(Country.NO), BiddingZone.NO5.getCountries());
        assertEquals(Set.of(Country.PL), BiddingZone.PL.getCountries());
        assertEquals(Set.of(Country.PT), BiddingZone.PT.getCountries());
        assertEquals(Set.of(Country.RO), BiddingZone.RO.getCountries());
        assertEquals(Set.of(Country.RS), BiddingZone.RS.getCountries());
        assertEquals(Set.of(Country.SE), BiddingZone.SE1.getCountries());
        assertEquals(Set.of(Country.SE), BiddingZone.SE2.getCountries());
        assertEquals(Set.of(Country.SE), BiddingZone.SE3.getCountries());
        assertEquals(Set.of(Country.SE), BiddingZone.SE4.getCountries());
        assertEquals(Set.of(Country.IE, Country.GB), BiddingZone.SEM.getCountries());
        assertEquals(Set.of(Country.SI), BiddingZone.SI.getCountries());
        assertEquals(Set.of(Country.SK), BiddingZone.SK.getCountries());
    }
}
