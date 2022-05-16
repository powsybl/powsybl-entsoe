/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.glsk.ucte;

import com.powsybl.glsk.api.AbstractGlskPoint;
import com.powsybl.glsk.api.GlskRegisteredResource;
import com.powsybl.glsk.api.GlskShiftKey;
import com.powsybl.glsk.commons.GlskException;
import org.threeten.extra.Interval;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class UcteGlskPoint extends AbstractGlskPoint {

    /**
     * @param element Dom element
     * @param ucteBlockType Type of block: CountryGSK, ManualGSK, AutoGSK
     * @param area country
     * @param ucteBusinessType generator or load
     * @param shareFactor shareFactor for generator or load
     */
    public UcteGlskPoint(Element element, String ucteBlockType, String area, String ucteBusinessType, Double shareFactor) {
        Objects.requireNonNull(element);
        this.position = 1;
        this.pointInterval = Interval.parse(((Element) element.getElementsByTagName("TimeInterval").item(0)).getAttribute("v"));
        this.subjectDomainmRID = area;
        this.curveType = "A03";

        glskShiftKeys = new ArrayList<>();
        switch (ucteBlockType) {
            case "CountryGSK_Block":
                caseCountryGskBlock(ucteBusinessType, shareFactor);
                break;
            case "ManualGSK_Block":
                caseManualGskBlock(element, ucteBusinessType, shareFactor);
                break;
            case "AutoGSK_Block":
                caseAutoGskBlock(element, ucteBusinessType, shareFactor);
                break;
            default:
                throw new GlskException("Unknown UCTE Block type");
        }
    }

    //build a country GSK B42 empty regitered resources list
    private void caseCountryGskBlock(String ucteBusinessType, Double shareFactor) {
        GlskShiftKey countryGlskShiftKey = new UcteGlskShiftKey("B42", ucteBusinessType, this.subjectDomainmRID, this.pointInterval, shareFactor);
        glskShiftKeys.add(countryGlskShiftKey);
    }

    //build a B43 participation factor
    private void caseManualGskBlock(Element element, String ucteBusinessType, Double shareFactor) {
        GlskShiftKey manuelGlskShiftKey = new UcteGlskShiftKey("B43", ucteBusinessType, this.subjectDomainmRID, this.pointInterval, shareFactor);
        //set registeredResourcesList for manuelGlskShiftKey
        List<GlskRegisteredResource> registerdResourceArrayList = new ArrayList<>();
        NodeList ucteGlskNodesList = element.getElementsByTagName("ManualNodes");

        for (int i = 0; i < ucteGlskNodesList.getLength(); ++i) {
            GlskRegisteredResource ucteGlskNode = new UcteGlskRegisteredResource((Element) ucteGlskNodesList.item(i));
            registerdResourceArrayList.add(ucteGlskNode);
        }
        manuelGlskShiftKey.setRegisteredResourceArrayList(registerdResourceArrayList);
        glskShiftKeys.add(manuelGlskShiftKey);
    }

    /* build a B42 explicit */
    private void caseAutoGskBlock(Element element, String ucteBusinessType, Double shareFactor) {
        GlskShiftKey autoGlskShiftKey = new UcteGlskShiftKey("B42", ucteBusinessType, this.subjectDomainmRID, this.pointInterval, shareFactor);
        List<GlskRegisteredResource> registerdResourceArrayList = new ArrayList<>();
        NodeList ucteGlskNodesList = element.getElementsByTagName("AutoNodes");

        for (int i = 0; i < ucteGlskNodesList.getLength(); ++i) {
            GlskRegisteredResource ucteGlskNode = new UcteGlskRegisteredResource((Element) ucteGlskNodesList.item(i));
            registerdResourceArrayList.add(ucteGlskNode);
        }
        autoGlskShiftKey.setRegisteredResourceArrayList(registerdResourceArrayList);
        glskShiftKeys.add(autoGlskShiftKey);
    }
}
