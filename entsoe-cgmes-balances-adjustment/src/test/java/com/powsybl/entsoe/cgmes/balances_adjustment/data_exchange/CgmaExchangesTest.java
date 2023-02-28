/*
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.entsoe.cgmes.balances_adjustment.data_exchange;

import com.powsybl.timeseries.DoubleTimeSeries;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Common Grid Model Alignment.
 * Check XML parsing.
 *
 * @author Thomas Adam {@literal <tadam at silicom.fr>}
 */
class CgmaExchangesTest {

    private DataExchanges exchanges;

    @BeforeEach
    void setUp() {
        exchanges = DataExchangesXml.parse(getClass().getResourceAsStream("/testCGMAMarketDocument_2-0.xml"));
    }

    @Test
    void baseTests() {
        // Getters
        assertEquals(StandardProcessType.A45, exchanges.getProcessType());
        assertEquals(Optional.of("OptimisationArea"), exchanges.getDomainId());
        assertEquals(Optional.of(StandardCodingSchemeType.A01), exchanges.getDomainCodingScheme());
        assertEquals(StandardRoleType.A39, exchanges.getSenderMarketRole());
    }

    @Test
    void timeSeriesTests() {
        // Time Series
        DoubleTimeSeries timeSeries1 = exchanges.getTimeSeries("TimeSeries1");
        // TimeSeries1 : Check metadata
        assertEquals("B65", timeSeries1.getMetadata().getTags().get("businessType"));
        assertEquals("A32", timeSeries1.getMetadata().getTags().get("marketObjectStatus.status"));
        assertEquals("A02", timeSeries1.getMetadata().getTags().get("curveType"));
    }

    @Test
    void coverageTests() {
        // StandardProcessType
        assertEquals("Two days ahead", StandardProcessType.A45.getDescription());
        // StandardRoleType
        assertEquals("Data provider", StandardRoleType.A39.getDescription());
    }
}
