/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.entsoe.cgmes.balances_adjustment.data_exchange;

import com.powsybl.commons.PowsyblException;
import com.powsybl.timeseries.DoubleTimeSeries;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pan European Verification Function.
 * Check XML parsing.
 *
 * @author Thomas Adam {@literal <tadam at silicom.fr>}
 */
class PevfExchangesTest {

    private DataExchanges exchanges;

    @BeforeEach
    void setUp() {
        exchanges = DataExchangesXml.parse(getClass().getResourceAsStream("/testPEVFMarketDocument_2-0.xml"));
    }

    @Test
    void baseTests() {
        // Getters
        assertEquals("MarketDocument_MRID", exchanges.getMRId());
        assertEquals(1, exchanges.getRevisionNumber());
        assertEquals(StandardMessageType.B19, exchanges.getType());
        assertEquals(StandardProcessType.A01, exchanges.getProcessType());
        assertEquals("SenderMarket", exchanges.getSenderId());
        assertEquals(StandardCodingSchemeType.A01, exchanges.getSenderCodingScheme());
        assertEquals(StandardRoleType.A32, exchanges.getSenderMarketRole());
        assertEquals("ReceiverMarket", exchanges.getReceiverId());
        assertEquals(StandardCodingSchemeType.A01, exchanges.getReceiverCodingScheme());
        assertEquals(StandardRoleType.A33, exchanges.getReceiverMarketRole());
        assertEquals(ZonedDateTime.parse("2020-04-05T14:30:00Z"), exchanges.getCreationDate());
        assertEquals(ZonedDateTime.parse("2020-04-05T22:00Z"), exchanges.getPeriod().getLeft());
        assertEquals(ZonedDateTime.parse("2020-04-06T22:00Z"), exchanges.getPeriod().getRight());
        // Optional
        assertEquals(Optional.of("PEVF CGM Export"), exchanges.getDatasetMarketDocumentMRId());
        assertEquals(Optional.of(StandardStatusType.A01), exchanges.getDocStatus());
        assertFalse(exchanges.getDomainId().isPresent());
        assertFalse(exchanges.getDomainCodingScheme().isPresent());
    }

    @Test
    void timeSeriesTests() {
        // Time Series
        DoubleTimeSeries timeSeries1 = exchanges.getTimeSeries("TimeSeries1");
        // TimeSeries1 : Check metadata
        assertEquals("TimeSeries1", timeSeries1.getMetadata().getName());
        assertEquals("B63", timeSeries1.getMetadata().getTags().get("businessType"));
        assertEquals("8716867000016", timeSeries1.getMetadata().getTags().get("product"));
        assertEquals("Sender1", timeSeries1.getMetadata().getTags().get("in_Domain.mRID"));
        assertEquals("A01", timeSeries1.getMetadata().getTags().get("in_Domain.mRID.codingScheme"));
        assertEquals("Receiver1", timeSeries1.getMetadata().getTags().get("out_Domain.mRID"));
        assertEquals("A01", timeSeries1.getMetadata().getTags().get("out_Domain.mRID.codingScheme"));
        assertEquals("Sender1_Receiver1", timeSeries1.getMetadata().getTags().get("connectingLine_RegisteredResource.mRID"));
        assertEquals("MAW", timeSeries1.getMetadata().getTags().get("measurement_Unit.name"));
        assertEquals("A03", timeSeries1.getMetadata().getTags().get("curveType"));
        // TimeSeries1 : values
        // Single step, single value
        assertArrayEquals(new double[]{0.000d, Double.NaN}, timeSeries1.toArray(), 0.0d);

        // TimeSeries2
        // Multi steps, single value
        DoubleTimeSeries timeSeries2 = exchanges.getTimeSeries("TimeSeries2");
        assertArrayEquals(new double[]{0.020d, 0.020d, Double.NaN}, timeSeries2.toArray(), 0.0d);

        // TimeSeries3
        // Each value defined
        DoubleTimeSeries timeSeries3 = exchanges.getTimeSeries("TimeSeries3");
        assertArrayEquals(new double[]{0.000d, 0.250d, 0.500d, 0.750d, Double.NaN}, timeSeries3.toArray(), 0.0d);

        // TimeSeries4
        // Each value not defined
        DoubleTimeSeries timeSeries4 = exchanges.getTimeSeries("TimeSeries4");
        double[] timeSeries4ExpectedValues = new double[]{3939.124, 3939.124, 3939.124, 3939.124, 3939.124, 3939.124, 3926.042, 3926.042, 3926.042, 3926.042, 3926.042, 3924.460, 3924.460, 3924.460, 3924.460, Double.NaN};
        assertArrayEquals(timeSeries4ExpectedValues, timeSeries4.toArray(), 0.0d);
    }

    @Test
    void utilitiesTests() {
        assertEquals(5, exchanges.getTimeSeries().size());

        assertEquals("TimeSeries1", exchanges.getTimeSeries("TimeSeries1").getMetadata().getName());

        Map<String, Double> timeSeriesById = exchanges.getValuesAt(Instant.parse("2020-04-05T22:14:12.000Z"));
        assertEquals(5, timeSeriesById.keySet().size());
        Iterator<Double> it = timeSeriesById.values().iterator();
        assertTrue(it.hasNext());
        assertEquals(0.02d, it.next(), 0.0d);

        assertEquals(3924.46d, exchanges.getValueAt("TimeSeries4", Instant.parse("2020-04-05T22:14:00.000Z")), 0.0d);
        assertEquals(3924.46d, exchanges.getValueAt("TimeSeries4", Instant.parse("2020-04-05T22:14:59.000Z")), 0.0d);

        assertEquals(35, exchanges.getValueAt("TimeSeries5", Instant.parse("2020-04-05T22:00:00.000Z")), 0.0);
        assertEquals(35, exchanges.getValueAt("TimeSeries5", Instant.parse("2020-04-05T23:00:00.000Z")), 0.0);
        assertEquals(15, exchanges.getValueAt("TimeSeries5", Instant.parse("2020-04-06T01:00:00.000Z")), 0.0);
        assertEquals(15, exchanges.getValueAt("TimeSeries5", Instant.parse("2020-04-06T01:30:00.000Z")), 0.0);

        timeSeriesById = exchanges.getValueAt(new String[]{"TimeSeries1", "TimeSeries5"}, Instant.parse("2020-04-05T22:00:00.000Z"));
        assertEquals(2, timeSeriesById.keySet().size());
        assertEquals(0.0d, timeSeriesById.get("TimeSeries1"), 0.0d);
        assertEquals(35.0d, timeSeriesById.get("TimeSeries5"), 0.0d);
    }

    @Test
    void searchTimeSeriesByDomainIdTest() {
        assertEquals(0, exchanges.getTimeSeries("Sender1", "Invalid").size());
        assertEquals(0, exchanges.getTimeSeries("Invalid", "Receiver1").size());
        assertEquals(1, exchanges.getTimeSeries("Sender1", "Receiver1").size());
        assertThrows(NullPointerException.class, () -> exchanges.getTimeSeries(null, "Receiver1"));
        assertThrows(NullPointerException.class, () -> exchanges.getTimeSeries("Sender1", null));

        assertTrue(exchanges.getValuesAt("Sender1", "Invalid", Instant.parse("2020-04-05T22:00:00.000Z")).isEmpty());
        Map<String, Double> values = exchanges.getValuesAt("Sender1", "Receiver1", Instant.parse("2020-04-05T22:00:00.000Z"));
        assertEquals(1, values.size());
        assertTrue(values.containsKey("TimeSeries1"));
        assertEquals(0.0, values.get("TimeSeries1"), 0.0);

        assertEquals(0.0, exchanges.getNetPosition("Sender1", "Receiver1", Instant.parse("2020-04-05T22:00:00.000Z")), 0.0);
        assertEquals(0.0, exchanges.getNetPosition("Sender1", "Receiver1", Instant.parse("2020-04-05T23:00:00.000Z"), false), 0.0);

        assertEquals(0.0, exchanges.getNetPositionsWithInDomainId("Sender1", Instant.parse("2020-04-05T22:00:00.000Z")).get("Receiver1"), 0.0);
        assertEquals(0.0, exchanges.getNetPositionsWithInDomainId("Sender1", Instant.parse("2020-04-05T23:00:00.000Z"), false).get("Receiver1"), 0.0);

        assertEquals(0.0, exchanges.getNetPositionsWithInDomainId("Receiver1", Instant.parse("2020-04-05T22:00:00.000Z")).get("Sender1"), 0.0);
        assertEquals(0.0, exchanges.getNetPositionsWithInDomainId("Receiver1", Instant.parse("2020-04-05T23:00:00.000Z"), false).get("Sender1"), 0.0);
    }

    @Test
    void timeSeriesNotFoundTest() {
        PowsyblException e = assertThrows(PowsyblException.class, () -> exchanges.getTimeSeries("Unknown"));
        assertTrue(e.getMessage().contains("TimeSeries 'Unknown' not found"));
    }

    @Test
    void timeSeriesEndTest() {
        Instant instant = Instant.parse("2019-06-18T22:00:00.000Z");
        PowsyblException e = assertThrows(PowsyblException.class, () -> exchanges.getValueAt("TimeSeries5", instant));
        assertTrue(e.getMessage().contains("TimeSeries5 '2019-06-18T22:00:00Z' is out of bound [2020-04-05T22:00:00Z, 2020-04-06T22:00:00Z["));
    }

    @Test
    void timeSeriesAfterEndTest() {
        Instant instant = Instant.parse("2019-06-19T00:00:00.000Z");
        PowsyblException e = assertThrows(PowsyblException.class, () -> exchanges.getValueAt("TimeSeries5", instant));
        assertTrue(e.getMessage().contains("TimeSeries5 '2019-06-19T00:00:00Z' is out of bound [2020-04-05T22:00:00Z, 2020-04-06T22:00:00Z["));
    }

    @Test
    void invalidRevisionNumberTest() {
        ZonedDateTime dateTime = ZonedDateTime.now();
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> new DataExchanges("", -1, StandardMessageType.B19, StandardProcessType.A01,
                "", StandardCodingSchemeType.A01, StandardRoleType.A32,
                "", StandardCodingSchemeType.A02, StandardRoleType.A33,
                dateTime, null, "", StandardStatusType.A01, null,
                null, null));
        assertTrue(e.getMessage().contains("Bad revision number value -1"));
    }

    @Test
    void coverageTests() {
        // StandardCodingSchemeType
        assertEquals("EIC", StandardCodingSchemeType.A01.getDescription());
        assertEquals("CGM", StandardCodingSchemeType.A02.getDescription());
        // StandardMessageType
        assertEquals("Reporting information market document", StandardMessageType.B19.getDescription());
        // StandardProcessType
        assertEquals("Day ahead", StandardProcessType.A01.getDescription());
        assertEquals("Total intraday", StandardProcessType.A18.getDescription());
        // StandardRoleType
        assertEquals("Market information aggregator", StandardRoleType.A32.getDescription());
        assertEquals("Information receiver", StandardRoleType.A33.getDescription());
        assertEquals("Capacity Coordinator", StandardRoleType.A36.getDescription());
        assertEquals("Regional Security Coordinator (RSC)", StandardRoleType.A44.getDescription());
        // StandardStatusType
        assertEquals("Intermediate", StandardStatusType.A01.getDescription());
        assertEquals("Final", StandardStatusType.A02.getDescription());
        // StandardBusinessType
        assertEquals("Aggregated netted external schedule", StandardBusinessType.B63.getDescription());
        assertEquals("Netted area AC position", StandardBusinessType.B64.getDescription());
        // StandardCodeType
        assertEquals("Default Time Series applied", StandardReasonCodeType.A26.getDescription());
        assertEquals("Counterpart time series missing", StandardReasonCodeType.A28.getDescription());
        assertEquals("Imposed Time Series from nominated party’s Time Series", StandardReasonCodeType.A30.getDescription());
        assertEquals("Global position not in balance", StandardReasonCodeType.A54.getDescription());
        assertEquals("Time series matched", StandardReasonCodeType.A88.getDescription());
        assertEquals("Data not yet available", StandardReasonCodeType.B08.getDescription());
        assertEquals("Data unverified", StandardReasonCodeType.B30.getDescription());
        assertEquals("Data verified", StandardReasonCodeType.B31.getDescription());
        // StandardCurveType
        assertEquals("Sequential fixed size block", StandardCurveType.A01.getDescription());
        assertEquals("Point", StandardCurveType.A02.getDescription());
        assertEquals("Variable sized Block", StandardCurveType.A03.getDescription());
    }
}
