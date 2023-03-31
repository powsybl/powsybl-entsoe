/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.glsk.cse;

import com.powsybl.glsk.api.AbstractGlskPoint;
import com.powsybl.glsk.commons.GlskException;
import org.threeten.extra.Interval;
import xsd.etso_code_lists.BusinessTypeList;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 * @author Vincent BOCHET {@literal <vincent.bochet at rte-france.com>}
 */
public class CseGlskPoint extends AbstractGlskPoint {

    private static final List<Class<?>> STANDARD_BLOCK_CLASSES = List.of(ManualGSKBlockType.class, PropGSKBlockType.class, PropLSKBlockType.class, ReserveGSKBlockType.class);

    public CseGlskPoint(TimeSeriesType timeSeries) {
        Objects.requireNonNull(timeSeries);
        this.position = 1;
        this.pointInterval = Interval.parse(timeSeries.getTimeInterval().getV());
        this.subjectDomainmRID = timeSeries.getArea().getV();
        this.curveType = "A03";
        this.glskShiftKeys = new ArrayList<>();

        BusinessTypeList businessType = timeSeries.getBusinessType().getV();

        try {
            Stream.concat(Stream.ofNullable(timeSeries.getManualLSKBlockOrPropLSKBlock()),
                            Stream.ofNullable(timeSeries.getPropGSKBlockOrReserveGSKBlockOrMeritOrderGSKBlock()))
                    .flatMap(List::stream)
                    .filter(block -> STANDARD_BLOCK_CLASSES.stream().anyMatch(acceptedClass -> acceptedClass.isInstance(block)))
                    .map(BlockWrapper::new)
                    .forEach(block -> importStandardBlock(block, businessType));

            Stream.ofNullable(timeSeries.getPropGSKBlockOrReserveGSKBlockOrMeritOrderGSKBlock())
                    .flatMap(List::stream)
                    .filter(MeritOrderGSKBlockType.class::isInstance)
                    .map(BlockWrapper::new)
                    .forEach(block -> importMeritOrderBlock(block, businessType));

        } catch (GlskException e) {
            throw new GlskException(String.format("Impossible to import GLSK on area %s", subjectDomainmRID), e);
        }
    }

    private void importMeritOrderBlock(BlockWrapper blockWrapper, BusinessTypeList businessType) {
        MeritOrderGSKBlockType block = (MeritOrderGSKBlockType) blockWrapper.getBlock();
        List<MeritOrderUpNodeType> upNodes = block.getUp().getNode();
        for (int j = 0; j < upNodes.size(); j++) {
            // Up nodes have positive merit order position
            // First is 1 last is N to be easily recognized in GLSK point conversion.
            glskShiftKeys.add(new CseGlskShiftKey(blockWrapper, new NodeWrapper(upNodes.get(j)), businessType, pointInterval, subjectDomainmRID, j + 1));
        }
        List<MeritOrderDownNodeType> downNodes = block.getDown().getNode();
        for (int j = 0; j < downNodes.size(); j++) {
            // Down nodes have negative merit order position
            // First is -1 last is -N to be easily recognized in GLSK point conversion.
            glskShiftKeys.add(new CseGlskShiftKey(blockWrapper, new NodeWrapper(downNodes.get(j)), businessType, pointInterval, subjectDomainmRID, -j - 1));
        }
    }

    private void importStandardBlock(BlockWrapper blockWrapper, BusinessTypeList businessType) {
        this.glskShiftKeys.add(new CseGlskShiftKey(blockWrapper, businessType, pointInterval, subjectDomainmRID));
    }
}
