/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.glsk.nc;

import com.powsybl.glsk.api.AbstractGlskRegisteredResource;
import com.powsybl.iidm.network.Network;

import java.util.Objects;

/**
 * NC GLSK registered resource implementation.
 * Represents a generating unit or load with a participation factor.
 *
 * @author Víctor Cardozo {@literal <victor.cardozo at artelys.com>}
 */
public class NcGlskRegisteredResource extends AbstractGlskRegisteredResource {

    /**
     * Constructor for NC GLSK registered resource.
     *
     * @param generatingUnitEIC EIC code of the generating unit or load
     * @param participationFactor participation factor (typically 0.0 to 1.0)
     */
    public NcGlskRegisteredResource(String generatingUnitEIC, double participationFactor) {
        this(generatingUnitEIC, participationFactor, null, null);
    }

    /**
     * Constructor for NC GLSK registered resource with capacity limits, as used by a merit order step.
     *
     * @param resourceMrid mRID of the resource (generating unit)
     * @param participationFactor participation factor (typically 0.0 to 1.0)
     * @param minimumCapacity minimum reachable active power of the resource, or null if not limited
     * @param maximumCapacity maximum reachable active power of the resource, or null if not limited
     */
    public NcGlskRegisteredResource(String resourceMrid, double participationFactor, Double minimumCapacity, Double maximumCapacity) {
        Objects.requireNonNull(resourceMrid, "Resource mRID cannot be null");
        this.mRID = resourceMrid;
        this.name = resourceMrid; // Use mRID as name if no explicit name
        this.participationFactor = participationFactor;
        this.maximumCapacity = maximumCapacity;
        this.minimumCapacity = minimumCapacity;
    }

    @Override
    public String getBoundaryLineId(Network network) {
        return mRID;
    }
}
