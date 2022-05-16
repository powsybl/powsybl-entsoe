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
 * GlskPoint: contain a Generator Shift Key and/or a Load Shift Key
 * for a certain Interval and a certain Country
 *  @author Pengbo Wang {@literal <pengbo.wang@rte-international.com>}
 */
public interface GlskPoint {

    /**
     * @return get point's position
     */
    Integer getPosition();

    /**
     * @param position position setter
     */
    void setPosition(Integer position);

    /**
     * @return get all shift keys in points
     */
    List<GlskShiftKey> getGlskShiftKeys();

    /**
     * @return get interval of point
     */
    Interval getPointInterval();

    boolean containsInstant(Instant instant);

    /**
     * @param pointInterval set interval of point
     */
    void setPointInterval(Interval pointInterval);

    /**
     * @return get country mrid
     */
    String getSubjectDomainmRID();

    /**
     * @return get curvetype
     */
    String getCurveType();
}
