/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.glsk.cse;

import com.powsybl.glsk.api.AbstractGlskShiftKey;
import com.powsybl.glsk.api.GlskRegisteredResource;
import com.powsybl.glsk.commons.GlskException;
import org.threeten.extra.Interval;
import xsd.etso_code_lists.BusinessTypeList;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 * @author Vincent BOCHET {@literal <vincent.bochet at rte-france.com>}
 */
public class CseGlskShiftKey extends AbstractGlskShiftKey {
    private int order = 0;

    public CseGlskShiftKey(BlockWrapper blockWrapper, BusinessTypeList businessType, Interval pointInterval, String subjectDomainmRID, BigDecimal sumBlockFactors) {
        initCommonMemberVariables(blockWrapper, businessType, pointInterval, subjectDomainmRID, sumBlockFactors);

        if (blockWrapper.getBlock() instanceof ManualGSKBlockType) {
            this.businessType = "B43";
            handleManualGskBlock(blockWrapper);
        } else if (blockWrapper.getBlock() instanceof PropGSKBlockType) {
            importImplicitProportionalBlock(blockWrapper, "B42");
        } else if (blockWrapper.getBlock() instanceof PropLSKBlockType) {
            this.psrType = "A05"; // Enforce psrType that does not respect "official" format specification
            importImplicitProportionalBlock(blockWrapper, "B42");
        } else if (blockWrapper.getBlock() instanceof ReserveGSKBlockType) {
            importImplicitProportionalBlock(blockWrapper, "B44");
        } else {
            throw new GlskException("Unknown UCTE Block type");
        }
    }

    private void handleManualGskBlock(BlockWrapper blockWrapper) {
        double factorsSum = 0;
        Optional<List<NodeWrapper>> nodeList = blockWrapper.getNodeList();

        if (nodeList.isPresent()) {
            for (NodeWrapper nodeWrapper : nodeList.get()) {
                CseGlskRegisteredResource cseGlskRegisteredResource = new CseGlskRegisteredResource(nodeWrapper);
                registeredResourceArrayList.add(cseGlskRegisteredResource);
                Optional<Double> initialFactor = cseGlskRegisteredResource.getInitialFactor();
                if (initialFactor.isPresent()) {
                    factorsSum += initialFactor.get();
                }
            }
        }

        if (factorsSum == 0) {
            throw new GlskException("Factors sum should not be 0");
        }

        for (GlskRegisteredResource registeredResource : registeredResourceArrayList) {
            CseGlskRegisteredResource cseRegisteredResource = (CseGlskRegisteredResource) registeredResource;
            Optional<Double> initialFactor = cseRegisteredResource.getInitialFactor();
            if (initialFactor.isPresent()) {
                cseRegisteredResource.setParticipationFactor(initialFactor.get() / factorsSum);
            }
        }
    }

    public CseGlskShiftKey(BlockWrapper blockWrapper, NodeWrapper nodeWrapper, BusinessTypeList businessType, Interval pointInterval, String subjectDomainmRID, int position, BigDecimal sumBlockFactors) {
        initCommonMemberVariables(blockWrapper, businessType, pointInterval, subjectDomainmRID, sumBlockFactors);
        this.meritOrderPosition = position;

        this.businessType = "B45";
        CseGlskRegisteredResource cseRegisteredResource = new CseGlskRegisteredResource(nodeWrapper);
        registeredResourceArrayList.add(cseRegisteredResource);
    }

    private void initCommonMemberVariables(BlockWrapper blockWrapper, BusinessTypeList businessType, Interval pointInterval, String subjectDomainmRID, BigDecimal sumBlockFactors) {
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

        if (isPartOfHybridShiftKey(blockWrapper)) {
            blockWrapper.getOrder().map(BigInteger::intValue)
                .ifPresent(o -> this.order = o);
            blockWrapper.getMaximumShift().map(BigDecimal::doubleValue)
                .ifPresent(ms -> this.maximumShift = ms);
        } else {
            blockWrapper.getFactor()
                .ifPresent(q -> {
                    if (sumIsZeroOrOne(sumBlockFactors)) {
                        this.quantity = q.doubleValue();
                    } else {
                        this.quantity = q.divide(sumBlockFactors).doubleValue();
                    }
                });
        }
    }

    private static boolean sumIsZeroOrOne(BigDecimal sumBlockFactors) {
        return BigDecimal.ONE.compareTo(sumBlockFactors) == 0
                || BigDecimal.ZERO.compareTo(sumBlockFactors) == 0;
    }

    private static boolean isPartOfHybridShiftKey(BlockWrapper blockWrapper) {
        return blockWrapper.getOrder().isPresent();
    }

    private void importImplicitProportionalBlock(BlockWrapper blockWrapper, String businessType) {
        this.businessType = businessType;

        blockWrapper.getNodeList().ifPresent(
            nodeList -> nodeList.stream()
                .map(CseGlskRegisteredResource::new)
                .forEach(registeredResourceArrayList::add));
    }

    public int getOrder() {
        return order;
    }
}
