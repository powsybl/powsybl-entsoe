/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.glsk.cse;

import xsd.etso_core_cmpts.QuantityType;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author Vincent BOCHET {@literal <vincent.bochet at rte-france.com>}
 */
public final class BlockWrapper {
    private final Object block;

    public BlockWrapper(Object block) {
        this.block = block;
    }

    public Object getBlock() {
        return block;
    }

    public Optional<BigInteger> getOrder() {
        try {
            BigInteger order = (BigInteger) block.getClass().getDeclaredField("order").get(block);
            return Optional.ofNullable(order);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            return Optional.empty();
        }
    }

    public Optional<BigDecimal> getMaximumShift() {
        try {
            QuantityType maximumShift = (QuantityType) block.getClass().getDeclaredField("maximumShift").get(block);
            return Optional.ofNullable(maximumShift).map(QuantityType::getV);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            return Optional.empty();
        }
    }

    public Optional<BigDecimal> getFactor() {
        try {
            QuantityType factor = (QuantityType) block.getClass().getDeclaredField("factor").get(block);
            return Optional.ofNullable(factor).map(QuantityType::getV);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            return Optional.empty();
        }
    }

    public Optional<List<NodeWrapper>> getNodeList() {
        try {
            List<Object> objectList = (List<Object>) block.getClass().getDeclaredField("node").get(block);
            return Optional.ofNullable(objectList)
                .map(list -> list.stream().map(NodeWrapper::new).collect(Collectors.toList()));
        } catch (NoSuchFieldException | IllegalAccessException e) {
            return Optional.empty();
        }
    }
}
