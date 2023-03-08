/*
 * Copyright (c) 2019, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.glsk.commons;

import com.powsybl.glsk.commons.chronology.Chronology;
import com.powsybl.glsk.commons.chronology.ChronologyImpl;
import org.junit.jupiter.api.Test;
import org.threeten.extra.Interval;

import java.time.Duration;
import java.time.Instant;
import java.time.Period;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
class ChronologyImplTest {
    @Test
    void testStoringByInstant() {
        Chronology<Integer> dataChronology = ChronologyImpl.create();
        String instantAsString = "2007-12-03T10:15:30.00Z";
        String instantInHourAsString = "2007-12-03T10:45:30.00Z";
        String otherInstantAsString = "2008-12-03T10:15:30.00Z";
        dataChronology.storeDataAtInstant(10, Instant.parse(instantAsString));
        assertNotNull(dataChronology.selectInstant(Instant.parse(instantAsString)));
        assertEquals(10, dataChronology.selectInstant(Instant.parse(instantAsString)).intValue());
        assertNotNull(dataChronology.selectInstant(Instant.parse(instantInHourAsString)));
        assertEquals(10, dataChronology.selectInstant(Instant.parse(instantInHourAsString)).intValue());
        assertNull(dataChronology.selectInstant(Instant.parse(otherInstantAsString)));
    }

    @Test
    void testStoringByInstantAndPeriod() {
        Chronology<Integer> dataChronology = ChronologyImpl.create();
        String instantAsString = "2007-12-03T10:15:30.00Z";
        String otherInstantWithinPeriodAsString = "2008-12-03T10:15:30.00Z";
        String otherInstantOutsidePeriodAsString = "2010-12-03T10:15:30.00Z";
        dataChronology.storeDataAtInstant(10, Instant.parse(instantAsString), Period.ofYears(2));
        assertNotNull(dataChronology.selectInstant(Instant.parse(instantAsString)));
        assertEquals(10, dataChronology.selectInstant(Instant.parse(instantAsString)).intValue());
        assertNotNull(dataChronology.selectInstant(Instant.parse(otherInstantWithinPeriodAsString)));
        assertEquals(10, dataChronology.selectInstant(Instant.parse(otherInstantWithinPeriodAsString)).intValue());
        assertNull(dataChronology.selectInstant(Instant.parse(otherInstantOutsidePeriodAsString)));
    }

    @Test
    void testStoringByInterval() {
        Chronology<Integer> dataChronology = ChronologyImpl.create();
        String intervalAsString = "2009-12-03T10:15:30.00Z/2010-12-03T10:15:30.00Z";
        String instantInsidePeriod = "2010-03-03T10:15:30.00Z";
        String instantOutsidePeriod = "2011-03-03T10:15:30.00Z";
        dataChronology.storeDataOnInterval(0, Interval.parse(intervalAsString));
        assertNotNull(dataChronology.selectInstant(Instant.parse(instantInsidePeriod)));
        assertEquals(0, dataChronology.selectInstant(Instant.parse(instantInsidePeriod)).intValue());
        assertNull(dataChronology.selectInstant(Instant.parse(instantOutsidePeriod)));
    }

    @Test
    void testStoringByBeginningAndEndInstants() {
        Chronology<Integer> dataChronology = ChronologyImpl.create();
        String beginningInstantAsString = "2009-12-03T10:15:30.00Z";
        String endInstantAsString = "2010-12-03T10:15:30.00Z";
        String instantInsidePeriod = "2010-03-03T10:15:30.00Z";
        String instantOutsidePeriod = "2011-03-03T10:15:30.00Z";
        dataChronology.storeDataBetweenInstants(-10, Instant.parse(beginningInstantAsString), Instant.parse(endInstantAsString));
        assertNotNull(dataChronology.selectInstant(Instant.parse(instantInsidePeriod)));
        assertEquals(-10, dataChronology.selectInstant(Instant.parse(instantInsidePeriod)).intValue());
        assertNull(dataChronology.selectInstant(Instant.parse(instantOutsidePeriod)));
    }

    @Test
    void testWithReplacementStrategyGet() {
        Chronology<Integer> dataChronology = ChronologyImpl.create();
        String instant1 = "2010-01-01T10:15:30.00Z";
        String instant2 = "2011-01-01T10:15:30.00Z";
        String instantInside = "2010-03-03T10:15:30.00Z";
        String instantBefore = "2009-03-03T10:15:30.00Z";
        String instantAfter = "2012-03-03T10:15:30.00Z";
        dataChronology.storeDataAtInstant(1, Instant.parse(instant1));
        dataChronology.storeDataAtInstant(2, Instant.parse(instant2));

        // If instant inside interval [instant1, instant2]
        assertNull(dataChronology.selectInstant(Instant.parse(instantInside)));
        assertNotNull(dataChronology.selectInstant(Instant.parse(instantInside), Chronology.ReplacementStrategy.DATA_AT_PREVIOUS_INSTANT));
        assertEquals(1, dataChronology.selectInstant(Instant.parse(instantInside), Chronology.ReplacementStrategy.DATA_AT_PREVIOUS_INSTANT).intValue());
        assertNotNull(dataChronology.selectInstant(Instant.parse(instantInside), Chronology.ReplacementStrategy.DATA_AT_NEXT_INSTANT));
        assertEquals(2, dataChronology.selectInstant(Instant.parse(instantInside), Chronology.ReplacementStrategy.DATA_AT_NEXT_INSTANT).intValue());

        // If instant before interval [instant1, instant2]
        assertNull(dataChronology.selectInstant(Instant.parse(instantBefore)));
        assertNull(dataChronology.selectInstant(Instant.parse(instantBefore), Chronology.ReplacementStrategy.DATA_AT_PREVIOUS_INSTANT));
        assertNotNull(dataChronology.selectInstant(Instant.parse(instantBefore), Chronology.ReplacementStrategy.DATA_AT_NEXT_INSTANT));
        assertEquals(1, dataChronology.selectInstant(Instant.parse(instantBefore), Chronology.ReplacementStrategy.DATA_AT_NEXT_INSTANT).intValue());

        // If instant after interval [instant1, instant2]
        assertNull(dataChronology.selectInstant(Instant.parse(instantAfter)));
        assertNotNull(dataChronology.selectInstant(Instant.parse(instantAfter), Chronology.ReplacementStrategy.DATA_AT_PREVIOUS_INSTANT));
        assertEquals(2, dataChronology.selectInstant(Instant.parse(instantAfter), Chronology.ReplacementStrategy.DATA_AT_PREVIOUS_INSTANT).intValue());
        assertNull(dataChronology.selectInstant(Instant.parse(instantAfter), Chronology.ReplacementStrategy.DATA_AT_NEXT_INSTANT));
    }

    @Test
    void testErrorWhenAddingInstantInAlreadyCreatedInstant() {
        Chronology<Integer> dataChronology = ChronologyImpl.create();
        Instant instant = Instant.parse("2010-03-03T10:15:30.00Z");
        dataChronology.storeDataAtInstant(2, instant);
        GlskException e = assertThrows(GlskException.class, () -> dataChronology.storeDataAtInstant(2, instant));
        assertEquals("A data is already provided for some instant of the interval", e.getMessage());
    }

    @Test
    void testErrorWhenAddingInstantInValidityPeriodOfAnotherInstant() {
        Chronology<Integer> dataChronology = ChronologyImpl.create();
        Instant instant = Instant.parse("2010-03-03T10:15:30.00Z");
        Instant instantPlus59Min = Instant.parse("2010-03-03T10:16:29.00Z");
        dataChronology.storeDataAtInstant(2, instant);
        GlskException e = assertThrows(GlskException.class, () -> dataChronology.storeDataAtInstant(2, instantPlus59Min));
        assertEquals("A data is already provided for some instant of the interval", e.getMessage());
    }

    @Test
    void testErrorWhenAddingInstantInAlreadyCreatedPeriod() {
        Chronology<Integer> dataChronology = ChronologyImpl.create();
        Interval interval = Interval.parse("2010-01-01T10:15:30.00Z/2011-01-01T10:15:30.00Z");
        Instant instantInside = Instant.parse("2010-03-03T10:15:30.00Z");
        dataChronology.storeDataOnInterval(1, interval);
        GlskException e = assertThrows(GlskException.class, () -> dataChronology.storeDataAtInstant(2, instantInside));
        assertEquals("A data is already provided for some instant of the interval", e.getMessage());
    }

    @Test
    void testErrorWhenIntervalsOverlaps() {
        Chronology<Integer> dataChronology = ChronologyImpl.create();
        Interval interval1 = Interval.parse("2010-01-01T10:15:30.00Z/2011-01-01T10:15:30.00Z");
        Interval interval2 = Interval.parse("2010-05-01T10:15:30.00Z/2011-05-01T10:15:30.00Z");
        dataChronology.storeDataOnInterval(1, interval1);
        GlskException e = assertThrows(GlskException.class, () -> dataChronology.storeDataOnInterval(2, interval2));
        assertEquals("A data is already provided for some instant of the interval", e.getMessage());
    }

    @Test
    void testLimitsOfIntervals() {
        Chronology<Integer> dataChronology = ChronologyImpl.create();
        Instant instant = Instant.parse("2010-03-03T10:15:30.00Z");
        Instant instantPlus1Hour = Instant.parse("2010-03-03T11:15:30.00Z");
        Instant instantPlus5Hour = Instant.parse("2010-03-03T15:15:30.00Z");
        Instant instantPlus1Hour1Day = Instant.parse("2010-03-04T11:15:30.00Z");
        dataChronology.storeDataAtInstant(2, instant);
        dataChronology.storeDataAtInstant(2, instantPlus1Hour, Duration.ofDays(1));
        dataChronology.storeDataAtInstant(3, instantPlus1Hour1Day);
        assertEquals(2, dataChronology.selectInstant(instantPlus5Hour).intValue());
        assertEquals(3, dataChronology.selectInstant(instantPlus1Hour1Day).intValue());
    }
}

