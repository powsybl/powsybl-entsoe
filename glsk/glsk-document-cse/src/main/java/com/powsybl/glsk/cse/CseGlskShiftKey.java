/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.glsk.cse;

import com.powsybl.glsk.api.AbstractGlskShiftKey;
import com.powsybl.glsk.commons.GlskException;
import org.threeten.extra.Interval;
import xsd.etso_code_lists.BusinessTypeList;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
public class CseGlskShiftKey extends AbstractGlskShiftKey {
    private int order = 0;

    public CseGlskShiftKey(BlockWrapper glskBlockElement, BusinessTypeList businessType, Interval pointInterval, String subjectDomainmRID) {
        initCommonMemberVariables(glskBlockElement, businessType, pointInterval, subjectDomainmRID);

        if (glskBlockElement.getBlock() instanceof ManualGSKBlockType) {
            this.businessType = "B43";
            AtomicReference<Double> currentFactorsSumReference = new AtomicReference<>((double) 0);
            glskBlockElement.getNodeList().ifPresent(
                nodeList -> nodeList.stream()
                    .map(CseGlskRegisteredResource::new)
                    .forEach(cseRegisteredResource -> {
                        registeredResourceArrayList.add(cseRegisteredResource);
                        cseRegisteredResource.getInitialFactor().ifPresent(
                            factor -> currentFactorsSumReference.updateAndGet(v -> v + factor));
                    }));

            Double factorsSum = currentFactorsSumReference.get();
            if (factorsSum == 0) {
                throw new GlskException("Factors sum should not be 0");
            }

            registeredResourceArrayList.stream()
                .map(CseGlskRegisteredResource.class::cast)
                .forEach(cseRegisteredResource ->
                    cseRegisteredResource.getInitialFactor().ifPresent(
                        initialFactor -> cseRegisteredResource.setParticipationFactor(initialFactor / factorsSum)));

        } else if (glskBlockElement.getBlock() instanceof PropGSKBlockType) {
            importImplicitProportionalBlock(glskBlockElement, "B42");
        } else if (glskBlockElement.getBlock() instanceof PropLSKBlockType) {
            this.psrType = "A05"; // Enforce psrType that does not respect "official" format specification
            importImplicitProportionalBlock(glskBlockElement, "B42");
        } else if (glskBlockElement.getBlock() instanceof ReserveGSKBlockType) {
            importImplicitProportionalBlock(glskBlockElement, "B44");
        } else {
            throw new GlskException("Unknown UCTE Block type");
        }
    }

    public CseGlskShiftKey(BlockWrapper blockWrapper, NodeWrapper nodeElement, BusinessTypeList businessType, Interval pointInterval, String subjectDomainmRID, int position) {
        initCommonMemberVariables(blockWrapper, businessType, pointInterval, subjectDomainmRID);
        this.meritOrderPosition = position;

        this.businessType = "B45";
        CseGlskRegisteredResource cseRegisteredResource = new CseGlskRegisteredResource(nodeElement);
        registeredResourceArrayList.add(cseRegisteredResource);
    }

    private void initCommonMemberVariables(BlockWrapper glskBlockElement, BusinessTypeList businessType, Interval pointInterval, String subjectDomainmRID) {
        if (businessType.equals(BusinessTypeList.Z_02)) {
            this.psrType = "A04";
        } else if (businessType.equals(BusinessTypeList.Z_05)) {
            this.psrType = "A05";
        } else {
            throw new GlskException("in GlskShiftKey UCTE constructor: unknown ucteBusinessType: " + businessType);
        }

        this.glskShiftKeyInterval = pointInterval;
        this.subjectDomainmRID = subjectDomainmRID;
        this.registeredResourceArrayList = new ArrayList<>();

        if (isPartOfHybridShiftKey(glskBlockElement)) {
            glskBlockElement.getOrder().map(BigInteger::intValue)
                .ifPresent(o -> this.order = o);
            glskBlockElement.getMaximumShift().map(BigDecimal::doubleValue)
                .ifPresent(ms -> this.maximumShift = ms);
        } else {
            glskBlockElement.getFactor().map(BigDecimal::doubleValue)
                .ifPresent(q -> this.quantity = q);
        }
    }

    private static boolean isPartOfHybridShiftKey(BlockWrapper glskBlockElement) {
        return glskBlockElement.getOrder().isPresent();
    }

    private void importImplicitProportionalBlock(BlockWrapper glskBlockElement, String businessType) {
        this.businessType = businessType;

        glskBlockElement.getNodeList().ifPresent(
            nodeList -> nodeList.stream()
                .map(CseGlskRegisteredResource::new)
                .forEach(registeredResourceArrayList::add));
    }

    public int getOrder() {
        return order;
    }
}
