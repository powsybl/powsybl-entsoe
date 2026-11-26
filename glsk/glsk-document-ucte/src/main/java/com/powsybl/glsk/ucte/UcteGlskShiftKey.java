/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.glsk.ucte;

import com.powsybl.glsk.api.AbstractGlskShiftKey;
import com.powsybl.glsk.commons.GlskException;
import org.threeten.extra.Interval;

import java.util.ArrayList;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class UcteGlskShiftKey extends AbstractGlskShiftKey {

    /**
     * @param businessType business type for UCTE Glsk shift key constructor
     * @param ucteBusinessType ucte businesstype for load and generator
     * @param subjectDomainmRID country code
     * @param pointInterval interval
     * @param shareFactor shareFactor between load and generator
     */
    public UcteGlskShiftKey(String businessType, String ucteBusinessType, String subjectDomainmRID, Interval pointInterval, Double shareFactor) {
        //for ucte format country gsk
        this.businessType = businessType;
        if ("Z02".equals(ucteBusinessType)) {
            this.psrType = "A04";
        } else if ("Z05".equals(ucteBusinessType)) {
            this.psrType = "A05";
        } else {
            throw new GlskException("in GlskShiftKey UCTE constructor: unknown ucteBusinessType: " + ucteBusinessType);
        }

        this.quantity = shareFactor / 100;
        this.registeredResourceArrayList = new ArrayList<>();
        this.glskShiftKeyInterval = pointInterval;
        this.subjectDomainmRID = subjectDomainmRID;
    }

    private UcteGlskShiftKey() {
        this.registeredResourceArrayList = new ArrayList<>();
    }

    UcteGlskShiftKey copyForInterval(Interval interval) {
        UcteGlskShiftKey copy = new UcteGlskShiftKey();
        copy.businessType = this.businessType;
        copy.psrType = this.psrType;
        copy.quantity = this.quantity;
        copy.glskShiftKeyInterval = interval;
        copy.subjectDomainmRID = this.subjectDomainmRID;
        copy.meritOrderPosition = this.meritOrderPosition;
        copy.flowDirection = this.flowDirection;
        copy.maximumShift = this.maximumShift;
        this.registeredResourceArrayList.forEach(resource ->
                copy.registeredResourceArrayList.add(((UcteGlskRegisteredResource) resource).copy()));
        return copy;
    }
}
