/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.glsk.api;

import com.powsybl.iidm.network.Network;

import java.util.Optional;

/**
 * Registered Resource: a generator or a load, with its participation factor
 * @author Pengbo Wang {@literal <pengbo.wang@rte-international.com>}
 * @author Peter Mitri {@literal <peter.mitri@rte-france.com>}
 */
public interface GlskRegisteredResource {

    /**
     * @return getter country mrid
     */
    String getmRID();

    /**
     * @param mRID setter mrid
     */
    void setmRID(String mRID);

    /**
     * @return get name
     */
    String getName();

    /**
     * @param name set name
     */
    void setName(String name);

    /**
     * @return get participation factor
     */
    double getParticipationFactor();

    /**
     * @return getter max value
     */
    Optional<Double> getMaximumCapacity();

    /**
     * @return getter min value
     */
    Optional<Double> getMinimumCapacity();

    /**
     * @return the generator Id according to type of Glsk File
     */
    String getGeneratorId();

    /**
     * @return the load Id according to the type of Glsk File
     */
    String getLoadId();

    /**
     * @return the dangling line Id according to the type of Glsk File
     */
    String getDanglingLineId(Network network);
}
