/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.glsk.api;

import org.threeten.extra.Interval;

import java.time.Instant;
import java.util.List;

/**
 *  @author Pengbo Wang {@literal <pengbo.wang@rte-international.com>}
 */
public abstract class AbstractGlskPoint implements GlskPoint {
    /**
     * position of the point; default value is 1; start from 1;
     */
    protected Integer position;
    /**
     * time interval of point
     */
    protected Interval pointInterval;
    /**
     * list of shift keys of point
     */
    protected List<GlskShiftKey> glskShiftKeys;
    /**
     * country's mrid
     */
    protected String subjectDomainmRID;
    /**
     * curveType A01 or A03
     */
    protected String curveType;

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("\n==== GLSK Point ====\n");
        builder.append("Position = ").append(position).append("\n");
        builder.append("PointInterval = ").append(pointInterval.toString()).append("\n");
        builder.append("subjectDomainmRID = ").append(subjectDomainmRID).append("\n");
        builder.append("CurveType = ").append(curveType).append("\n");
        for (GlskShiftKey key : glskShiftKeys) {
            builder.append(key.glskShiftKeyToString());
        }
        builder.append("\n");
        return builder.toString();
    }

    @Override
    public Integer getPosition() {
        return position;
    }

    @Override
    public void setPosition(Integer position) {
        this.position = position;
    }

    @Override
    public List<GlskShiftKey> getGlskShiftKeys() {
        return glskShiftKeys;
    }

    @Override
    public Interval getPointInterval() {
        return pointInterval;
    }

    public boolean containsInstant(Instant instant) {
        return pointInterval.contains(instant);
    }

    @Override
    public void setPointInterval(Interval pointInterval) {
        this.pointInterval =  pointInterval;
    }

    @Override
    public String getSubjectDomainmRID() {
        return subjectDomainmRID;
    }

    @Override
    public String getCurveType() {
        return curveType;
    }
}
