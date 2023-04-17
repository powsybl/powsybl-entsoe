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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Vincent Bochet {@literal <vincent.bochet at rte-france.com>}
 */
class NodeWrapperTest {
    @Test
    void checkGetNodeReturnsValue() {
        MeritOrderUpNodeType node = new MeritOrderUpNodeType();
        assertEquals(node, new NodeWrapper(node).getNode());
    }

    @Test
    void checkGetOrderReturnsValue() {
        MeritOrderUpNodeType node = new MeritOrderUpNodeType();
        NodeNameType nodeName = new NodeNameType();
        nodeName.setV("name");
        node.setName(nodeName);
        assertEquals(Optional.of("name"), new NodeWrapper(node).getName());
    }

    @Test
    void checkGetOrderReturnsEmptyWhenNodeHasNoOrderAttribute() {
        assertEquals(Optional.empty(), new NodeWrapper(new Object()).getName());
    }

    @Test
    void checkGetFactorReturnsValue() {
        ManualGSKNodeType node = new ManualGSKNodeType();
        QuantityType quantity = new QuantityType();
        quantity.setV(BigDecimal.TEN);
        node.setFactor(quantity);
        assertEquals(Optional.of(BigDecimal.TEN), new NodeWrapper(node).getFactor());
    }

    @Test
    void checkGetFactorReturnsEmptyWhenNodeHasNoFactorAttribute() {
        assertEquals(Optional.empty(), new NodeWrapper(new Object()).getFactor());
    }

    @Test
    void checkGetPmaxReturnsValue() {
        MeritOrderUpNodeType node = new MeritOrderUpNodeType();
        QuantityType quantity = new QuantityType();
        quantity.setV(BigDecimal.TEN);
        node.setPmax(quantity);
        assertEquals(Optional.of(BigDecimal.TEN), new NodeWrapper(node).getPmax());
    }

    @Test
    void checkGetPmaxReturnsEmptyWhenNodeHasNoPmaxAttribute() {
        assertEquals(Optional.empty(), new NodeWrapper(new Object()).getPmax());
    }

    @Test
    void checkGetPminReturnsValue() {
        MeritOrderUpNodeType node = new MeritOrderUpNodeType();
        QuantityType quantity = new QuantityType();
        quantity.setV(BigDecimal.TEN);
        node.setPmin(quantity);
        assertEquals(Optional.of(BigDecimal.TEN), new NodeWrapper(node).getPmin());
    }

    @Test
    void checkGetPminReturnsEmptyWhenNodeHasNoPminAttribute() {
        assertEquals(Optional.empty(), new NodeWrapper(new Object()).getPmin());
    }
}
