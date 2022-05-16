/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.glsk.api;

import java.util.Optional;

/**
 * @author Pengbo Wang {@literal <pengbo.wang@rte-international.com>}
 */
public abstract class AbstractGlskRegisteredResource implements GlskRegisteredResource {
    /**
     * mRID of registered resource
     */
    protected String mRID;
    /**
     * name
     */
    protected String name;
    /**
     * participation factor between generator and load. default = 0
     */
    protected Double participationFactor;
    /**
     * max value for merit order
     */
    protected Double maximumCapacity;
    /**
     * min value for merit order
     */
    protected Double minimumCapacity;

    @Override
    public String getmRID() {
        return mRID;
    }

    @Override
    public void setmRID(String mRID) {
        this.mRID = mRID;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public double getParticipationFactor() {
        return participationFactor != null ? participationFactor : 0.0;
    }

    @Override
    public Optional<Double> getMaximumCapacity() {
        return Optional.ofNullable(maximumCapacity);
    }

    @Override
    public Optional<Double> getMinimumCapacity() {
        return Optional.ofNullable(minimumCapacity);
    }

    @Override
    public String getGeneratorId() {
        return mRID;
    }

    @Override
    public String getLoadId() {
        return mRID;
    }
}
