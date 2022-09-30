/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.glsk.api;

import org.threeten.extra.Interval;

import java.util.List;

/**
 * @author Pengbo Wang {@literal <pengbo.wang@rte-international.com>}
 */
public abstract class AbstractGlskShiftKey implements GlskShiftKey {
    private static final double DEFAULT_QUANTITY = 1.0;
    private static final double DEFAULT_MAXIMUM_SHIFT = Double.MAX_VALUE;

    /**
     * business type of shift key. B42, B43, B45
     */
    protected String businessType;
    /**
     * load A05 or generator A04
     */
    protected String psrType;
    /**
     * explicit shift key factor
     */
    protected Double quantity = DEFAULT_QUANTITY;
    /**
     * list of registered resources
     */
    protected List<GlskRegisteredResource> registeredResourceArrayList;

    /**
     * time interval of shift key
     */
    protected Interval glskShiftKeyInterval;
    /**
     * country mrid
     */
    protected String subjectDomainmRID;
    /**
     * merit order position
     */
    protected int meritOrderPosition;
    /**
     * merit order direction
     */
    protected String flowDirection;
    /**
     * Maximum shift
     */
    protected double maximumShift = DEFAULT_MAXIMUM_SHIFT;

    @Override
    public String glskShiftKeyToString() {
        return "\t==== GSK Shift Key ====\n" +
                "\tBusinessType = " + businessType + "\n" +
                "\tPsrType = " + psrType + "\n" +
                "\tQuantity = " + quantity + "\n" +
                "\tGlskShiftKeyInterval = " + glskShiftKeyInterval + "\n" +
                "\tRegisteredResource size = " + registeredResourceArrayList.size() + "\n";
    }

    @Override
    public String getBusinessType() {
        return businessType;
    }

    @Override
    public void setBusinessType(String businessType) {
        this.businessType = businessType;
    }

    @Override
    public String getPsrType() {
        return psrType;
    }

    @Override
    public void setPsrType(String psrType) {
        this.psrType = psrType;
    }

    @Override
    public Double getQuantity() {
        return quantity;
    }

    @Override
    public List<GlskRegisteredResource> getRegisteredResourceArrayList() {
        return registeredResourceArrayList;
    }

    @Override
    public void setRegisteredResourceArrayList(List<GlskRegisteredResource> registeredResourceArrayList) {
        this.registeredResourceArrayList = registeredResourceArrayList;
    }

    @Override
    public String getSubjectDomainmRID() {
        return subjectDomainmRID;
    }

    @Override
    public int getMeritOrderPosition() {
        return meritOrderPosition;
    }

    @Override
    public String getFlowDirection() {
        return flowDirection;
    }

    @Override
    public double getMaximumShift() {
        return maximumShift;
    }

}
