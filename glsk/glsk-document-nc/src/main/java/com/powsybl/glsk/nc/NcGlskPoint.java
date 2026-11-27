/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.glsk.nc;

import com.powsybl.glsk.api.AbstractGlskPoint;
import org.threeten.extra.Interval;

import java.util.List;
import java.util.Objects;

/**
 * NC GLSK point implementation.
 * Represents the GLSK data of a bidding zone over a time interval.
 *
 * @author Víctor Cardozo {@literal <victor.cardozo at artelys.com>}
 */
public class NcGlskPoint extends AbstractGlskPoint {

    /**
     * Constructor for NC GLSK point.
     *
     * @param zoneEIC EIC code of the bidding zone this point applies to
     * @param pointInterval time interval over which this point is valid
     * @param shiftKeys the shift keys of the point (at least one)
     */
    public NcGlskPoint(String zoneEIC, Interval pointInterval, List<NcGlskShiftKey> shiftKeys) {
        Objects.requireNonNull(zoneEIC, "Zone EIC cannot be null");
        Objects.requireNonNull(pointInterval, "Point interval cannot be null");
        Objects.requireNonNull(shiftKeys, "Shift keys cannot be null");
        if (shiftKeys.isEmpty()) {
            throw new IllegalArgumentException("A GLSK point needs at least one shift key");
        }

        this.position = 1; // NC profiles don't use position
        this.pointInterval = pointInterval;
        this.subjectDomainmRID = zoneEIC;
        this.curveType = "A03"; // Variable (explicit time points)
        this.glskShiftKeys = List.copyOf(shiftKeys);
    }

    /**
     * Point of a proportionalForGenerator strategy: a single shift key applying to all the generators of the country of
     * the zone.
     */
    public static NcGlskPoint proportionalForGenerator(String zoneEIC, Interval pointInterval) {
        return new NcGlskPoint(zoneEIC, pointInterval, List.of(NcGlskShiftKey.proportionalForGenerator(pointInterval, zoneEIC)));
    }
}
