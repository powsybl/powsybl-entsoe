/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.glsk.cse;

import com.powsybl.glsk.api.AbstractGlskRegisteredResource;
import com.powsybl.glsk.api.AbstractGlskShiftKey;
import com.powsybl.glsk.commons.GlskException;
import org.threeten.extra.Interval;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.Optional;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
public class CseGlskShiftKey extends AbstractGlskShiftKey {
    private static final int DEFAULT_ORDER = 0;

    private int order = DEFAULT_ORDER;

    public CseGlskShiftKey(Element glskBlockElement, String businessType, Interval pointInterval, String subjectDomainmRID) {
        initCommonMemberVariables(glskBlockElement, businessType, pointInterval, subjectDomainmRID);

        switch (glskBlockElement.getTagName()) {
            case "ManualGSKBlock":
                this.businessType = "B43";
                NodeList nodesList = glskBlockElement.getElementsByTagName("Node");
                double currentFactorsSum = 0;
                for (int i = 0; i < nodesList.getLength(); i++) {
                    Element nodeElement = (Element) nodesList.item(i);
                    CseGlskRegisteredResource cseRegisteredResource = new CseGlskRegisteredResource(nodeElement);
                    registeredResourceArrayList.add(cseRegisteredResource);
                    Optional<Double> initialFactor = cseRegisteredResource.getInitialFactor();
                    if (initialFactor.isPresent()) {
                        currentFactorsSum += initialFactor.get();
                    }
                }

                if (currentFactorsSum == 0) {
                    throw new GlskException("Factors sum should not be 0");
                }

                for (AbstractGlskRegisteredResource registeredResource : registeredResourceArrayList) {
                    CseGlskRegisteredResource cseRegisteredResource = (CseGlskRegisteredResource) registeredResource;
                    Optional<Double> intialFactor = cseRegisteredResource.getInitialFactor();
                    if (intialFactor.isPresent()) {
                        cseRegisteredResource.setParticipationFactor(intialFactor.get() / currentFactorsSum);
                    }
                }
                break;
            case "PropGSKBlock":
                importImplicitProportionalBlock(glskBlockElement, "B42");
                break;
            case "PropLSKBlock":
                this.psrType = "A05"; // Enforce psrType that does not respect "official" format specification
                importImplicitProportionalBlock(glskBlockElement, "B42");
                break;
            case "ReserveGSKBlock":
                importImplicitProportionalBlock(glskBlockElement, "B44");
                break;
            default:
                throw new GlskException("Unknown UCTE Block type");
        }
    }

    public CseGlskShiftKey(Element glskBlockElement, String businessType, Interval pointInterval, String subjectDomainmRID, int position) {
        initCommonMemberVariables(glskBlockElement, businessType, pointInterval, subjectDomainmRID);
        this.meritOrderPosition = position;

        if ("MeritOrderGSKBlock".equals(glskBlockElement.getTagName())) {
            this.businessType = "B45";
            Element nodeElement = getNodeElement(glskBlockElement, position);
            CseGlskRegisteredResource cseRegisteredResource = new CseGlskRegisteredResource(nodeElement);
            registeredResourceArrayList.add(cseRegisteredResource);
        } else {
            throw new GlskException("Unknown UCTE Block type");
        }
    }

    private Element getNodeElement(Element glskBlockElement, int position) {
        if (position > 0) {
            // Up scalable element
            // Position is 1 to N for up scalable
            // Though, in XML file, we have to get child position - 1
            Element upBlockElement = (Element) glskBlockElement.getElementsByTagName("Up").item(0);
            return (Element) upBlockElement.getElementsByTagName("Node").item(position - 1);
        } else {
            // Down scalable element
            // Position is -1 to -N for down scalable
            // Though, in XML file, we have to get child -position - 1
            Element downBlockElement = (Element) glskBlockElement.getElementsByTagName("Down").item(0);
            return (Element) downBlockElement.getElementsByTagName("Node").item(-position - 1);
        }
    }

    private void initCommonMemberVariables(Element glskBlockElement, String businessType, Interval pointInterval, String subjectDomainmRID) {
        if (businessType.equals("Z02")) {
            this.psrType = "A04";
        } else if (businessType.equals("Z05")) {
            this.psrType = "A05";
        } else {
            throw new GlskException("in GlskShiftKey UCTE constructor: unknown ucteBusinessType: " + businessType);
        }
        this.glskShiftKeyInterval = pointInterval;
        this.subjectDomainmRID = subjectDomainmRID;
        this.registeredResourceArrayList = new ArrayList<>();
        if (isPartOfHybridShiftKey(glskBlockElement)) {
            this.order = getOrder(glskBlockElement);
            if (hasMaximumShift(glskBlockElement)) {
                this.maximumShift = getMaximumShift(glskBlockElement);
            }
        } else {
            this.quantity = getFactor(glskBlockElement);
        }
    }

    private static boolean isPartOfHybridShiftKey(Element glskBlockElement) {
        return glskBlockElement.getElementsByTagName("Order").getLength() != 0;
    }

    private static int getOrder(Element glskBlockElement) {
        return Integer.parseInt((glskBlockElement.getElementsByTagName("Order").item(0)).getTextContent());
    }

    private static boolean hasMaximumShift(Element glskBlockElement) {
        return glskBlockElement.getElementsByTagName("MaximumShift").getLength() != 0;
    }

    private static double getMaximumShift(Element glskBlockElement) {
        //maximum shift in hybrid cse glsk
        return Double.parseDouble(((Element) glskBlockElement.getElementsByTagName("MaximumShift").item(0)).getAttribute("v"));
    }

    private static double getFactor(Element glskBlockElement) {
        return Double.parseDouble(((Element) glskBlockElement.getElementsByTagName("Factor").item(0)).getAttribute("v"));
    }

    private void importImplicitProportionalBlock(Element glskBlockElement, String businessType) {
        this.businessType = businessType;

        NodeList nodesList = glskBlockElement.getElementsByTagName("Node");
        for (int i = 0; i < nodesList.getLength(); i++) {
            Element nodeElement = (Element) nodesList.item(i);
            CseGlskRegisteredResource cseRegisteredResource = new CseGlskRegisteredResource(nodeElement);
            registeredResourceArrayList.add(cseRegisteredResource);
        }
    }

    public int getOrder() {
        return order;
    }
}
