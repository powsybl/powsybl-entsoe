/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.glsk.nc;

import com.powsybl.glsk.api.AbstractGlskShiftKey;
import org.threeten.extra.Interval;

import java.util.List;
import java.util.Objects;

/**
 * NC GLSK shift key implementation.
 * Builds the CIM-like shift keys that the GLSK to Scalable conversion understands:
 * <ul>
 *     <li>B42 without registered resources: all the generators of the country of the zone, proportionally to their
 *     target active power (proportionalForGenerator);</li>
 *     <li>B45: one step of a merit order, with a single resource and its maximum (up) or minimum (down) reachable
 *     active power (explicitSchedule). The sign of the merit order position gives the direction.</li>
 * </ul>
 *
 * @author Víctor Cardozo {@literal <victor.cardozo at artelys.com>}
 */
public final class NcGlskShiftKey extends AbstractGlskShiftKey {

    private static final String GENERATION_PSR_TYPE = "A04";
    private static final String UP_FLOW_DIRECTION = "A01";
    private static final String DOWN_FLOW_DIRECTION = "A02";

    private NcGlskShiftKey(String businessType, Interval interval, String zoneEIC,
                           int meritOrderPosition, String flowDirection, List<NcGlskRegisteredResource> resources) {
        Objects.requireNonNull(interval, "Interval cannot be null");
        Objects.requireNonNull(zoneEIC, "Zone EIC cannot be null");
        this.businessType = businessType;
        this.psrType = GENERATION_PSR_TYPE; // NC profiles reference generating units
        this.quantity = 1.0; // NC profiles don't specify quantity
        this.subjectDomainmRID = zoneEIC;
        this.flowDirection = flowDirection;
        this.meritOrderPosition = meritOrderPosition;
        this.glskShiftKeyInterval = interval;
        this.registeredResourceArrayList = List.copyOf(resources);
    }

    /**
     * Shift key of a proportionalForGenerator strategy: it has no registered resource and applies to all the
     * generators of the country of the zone.
     *
     * @param interval time interval for this shift key
     * @param zoneEIC EIC code of the bidding zone
     */
    public static NcGlskShiftKey proportionalForGenerator(Interval interval, String zoneEIC) {
        return new NcGlskShiftKey("B42", interval, zoneEIC, 0, "", List.of());
    }

    /**
     * Shift key of one step of the merit order of an explicitSchedule strategy.
     *
     * @param interval time interval for this shift key
     * @param zoneEIC EIC code of the bidding zone
     * @param meritOrderPosition position in the merit order: 1..N for the upward direction, -1..-N for the downward
     *                           direction (the cheapest step is the closest to zero)
     * @param resource resource of the step, with its maximum (up) or minimum (down) reachable active power
     */
    public static NcGlskShiftKey meritOrderStep(Interval interval, String zoneEIC, int meritOrderPosition,
                                                NcGlskRegisteredResource resource) {
        if (meritOrderPosition == 0) {
            throw new IllegalArgumentException("The position of a merit order step cannot be 0");
        }
        Objects.requireNonNull(resource, "Resource cannot be null");
        String flowDirection = meritOrderPosition > 0 ? UP_FLOW_DIRECTION : DOWN_FLOW_DIRECTION;
        return new NcGlskShiftKey("B45", interval, zoneEIC, meritOrderPosition, flowDirection, List.of(resource));
    }
}
