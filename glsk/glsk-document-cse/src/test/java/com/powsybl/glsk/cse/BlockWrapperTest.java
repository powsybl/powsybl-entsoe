/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.glsk.cse;

import org.junit.jupiter.api.Test;
import xsd.etso_core_cmpts.QuantityType;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Vincent Bochet {@literal <vincent.bochet at rte-france.com>}
 */
class BlockWrapperTest {
    @Test
    void checkGetOrderReturnsValue() {
        PropGSKBlockType block = new PropGSKBlockType();
        block.setOrder(BigInteger.TEN);
        assertEquals(Optional.of(BigInteger.TEN), new BlockWrapper(block).getOrder());
    }

    @Test
    void checkGetOrderReturnsEmptyWhenBlockHasNoOrderAttribute() {
        assertEquals(Optional.empty(), new BlockWrapper(new Object()).getOrder());
    }

    @Test
    void checkGetMaximumShiftReturnsValue() {
        PropGSKBlockType block = new PropGSKBlockType();
        QuantityType quantity = new QuantityType();
        quantity.setV(BigDecimal.TEN);
        block.setMaximumShift(quantity);
        assertEquals(Optional.of(BigDecimal.TEN), new BlockWrapper(block).getMaximumShift());
    }

    @Test
    void checkGetMaximumShiftReturnsEmptyWhenBlockHasNoMaximumShiftAttribute() {
        assertEquals(Optional.empty(), new BlockWrapper(new Object()).getMaximumShift());
    }

    @Test
    void checkGetFactorReturnsValue() {
        PropGSKBlockType block = new PropGSKBlockType();
        QuantityType quantity = new QuantityType();
        quantity.setV(BigDecimal.TEN);
        block.setFactor(quantity);
        assertEquals(Optional.of(BigDecimal.TEN), new BlockWrapper(block).getFactor());
    }

    @Test
    void checkGetFactorReturnsEmptyWhenBlockHasNoFactorAttribute() {
        assertEquals(Optional.empty(), new BlockWrapper(new Object()).getFactor());
    }

    @Test
    void checkGetNodeListReturnsList() {
        PropGSKBlockType block = new PropGSKBlockType();
        block.getNode().add(new PropGSKNodeType());
        assertEquals(1, new BlockWrapper(block).getNodeList().get().size());
    }

    @Test
    void checkGetNodeListReturnsEmptyWhenBlockHasNoNodeListAttribute() {
        assertEquals(Optional.empty(), new BlockWrapper(new Object()).getNodeList());
    }
}
