/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.glsk.cim;

import com.powsybl.glsk.api.AbstractGlskShiftKey;
import com.powsybl.glsk.commons.GlskException;
import org.threeten.extra.Interval;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class CimGlskShiftKey extends AbstractGlskShiftKey {

    /**
     * @param element Dom element of CIM Glsk
     * @param pointInterval interval of point
     * @param subjectDomainmRID country mrid
     */
    public CimGlskShiftKey(Element element, Interval pointInterval, String subjectDomainmRID) {
        Objects.requireNonNull(element);
        this.businessType = element.getElementsByTagName("businessType").item(0).getTextContent();
        List<String> supportedBusinessType = Arrays.asList("B42", "B43", "B45");
        if (!supportedBusinessType.contains(businessType)) {
            throw new GlskException("BusinessType not supported: " + businessType);
        }
        this.psrType = element.getElementsByTagName("mktPSRType.psrType").item(0).getTextContent();
        NodeList quantities = element.getElementsByTagName("quantity.quantity");
        if (quantities.getLength() > 0) {
            this.quantity = Double.valueOf(quantities.item(0).getTextContent());
        } else {
            this.quantity = 1.0;
        }
        this.subjectDomainmRID = subjectDomainmRID;
        NodeList positions = element.getElementsByTagName("attributeInstanceComponent.position");
        this.meritOrderPosition = positions.getLength() == 0 ? 0 :
            Integer.valueOf(positions.item(0).getTextContent());
        NodeList directions = element.getElementsByTagName("flowDirection.direction\"");
        this.flowDirection = directions.getLength() == 0 ? "" :
                directions.item(0).getTextContent();
        //registeredResources
        this.registeredResourceArrayList = new ArrayList<>();
        NodeList glskRegisteredResourcesElements = element.getElementsByTagName("RegisteredResource");
        for (int i = 0; i < glskRegisteredResourcesElements.getLength(); i++) {
            registeredResourceArrayList.add(new CimGlskRegisteredResource((Element) glskRegisteredResourcesElements.item(i)));
        }

        this.glskShiftKeyInterval = pointInterval;
    }
}
