/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.glsk.api;

import java.util.List;

/**
 * Shift Key
 * @author Pengbo Wang {@literal <pengbo.wang@rte-international.com>}
 */
public interface GlskShiftKey {

    /**
     * @return debug to string
     */
    String glskShiftKeyToString();

    /**
     * @return getter businesstype
     */
    String getBusinessType();

    /**
     * @param businessType setter business type
     */
    void setBusinessType(String businessType);

    /**
     * @return getter psrType
     */
    String getPsrType();

    /**
     * @param psrType setter psrType
     */
    void setPsrType(String psrType);

    /**
     * @return getter quantity
     */
    Double getQuantity();

    /**
     * @return get list of registered resources
     */
    List<GlskRegisteredResource> getRegisteredResourceArrayList();

    /**
     * @param registeredResourceArrayList setter registered resources
     */
    void setRegisteredResourceArrayList(List<GlskRegisteredResource> registeredResourceArrayList);

    /**
     * @return getter country mrid
     */
    String getSubjectDomainmRID();

    /**
     * @return getter merit order position
     */
    int getMeritOrderPosition();

    /**
     * @return getter merit order direction
     */
    String getFlowDirection();

    double getMaximumShift();
}
