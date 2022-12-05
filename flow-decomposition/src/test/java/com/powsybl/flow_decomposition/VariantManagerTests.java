/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.flow_decomposition;

import com.powsybl.flow_decomposition.xnec_provider.XnecProviderByIds;
import com.powsybl.iidm.network.Network;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 */
class VariantManagerTests {
    private String contingencyId2;
    private String contingencyId3;
    private Network network;
    private XnecProviderByIds xnecProvider;
    private Collection<String> variantIds;

    @BeforeEach
    public void setup() {
        String networkFileName = "19700101_0000_FO4_UX1.uct";
        String branchId = "DB000011 DF000011 1";
        String contingencyElementId1 = "FB000011 FD000011 1";
        String contingencyElementId2 = "FB000021 FD000021 1";
        contingencyId2 = "DD000011 DF000011 1";
        contingencyId3 = "FB000011 FD000011 1_FB000021 FD000021 1";

        network = TestUtils.importNetwork(networkFileName);
        xnecProvider = XnecProviderByIds.builder()
            .addContingencies(Map.of(contingencyId2, Set.of(contingencyId2), contingencyId3, Set.of(contingencyElementId1, contingencyElementId2)))
            .addNetworkElementsAfterContingencies(Set.of(branchId), Set.of(contingencyId2, contingencyId3))
            .addNetworkElementsOnBasecase(Set.of(branchId))
            .build();
        variantIds = network.getVariantManager().getVariantIds();
    }

    @Test
    void testCreateAVariantPerContingency() {
        VariantManager variantManager = new VariantManager(network, xnecProvider);
        assertEquals(1, variantIds.size());
        assertTrue(variantIds.contains("InitialState"));

        variantManager.createAVariantPerContingency(network);
        assertEquals(3, variantIds.size());
        assertTrue(variantIds.contains("InitialState"));
        assertTrue(variantIds.contains(contingencyId2));
        assertTrue(variantIds.contains(contingencyId3));
    }

    @Test
    void testChangeVariant() {
        VariantManager variantManager = new VariantManager(network, xnecProvider);
        variantManager.createAVariantPerContingency(network);
        assertEquals("InitialState", network.getVariantManager().getWorkingVariantId());

        variantManager.setNetworkVariant(network, contingencyId2);
        assertEquals(contingencyId2, network.getVariantManager().getWorkingVariantId());

        variantManager.setNetworkVariant(network, contingencyId3);
        assertEquals(contingencyId3, network.getVariantManager().getWorkingVariantId());

        variantManager.setDefaultNetworkVariant(network);
        assertEquals("InitialState", network.getVariantManager().getWorkingVariantId());

        variantManager.setNetworkVariant(network, contingencyId2);
        assertEquals(contingencyId2, network.getVariantManager().getWorkingVariantId());
    }

    @Test
    void testDeleteAllContingencyVariants() {
        VariantManager variantManager = new VariantManager(network, xnecProvider);
        variantManager.createAVariantPerContingency(network);
        variantManager.deleteAllContingencyVariants(network);

        assertEquals(1, variantIds.size());
        assertTrue(variantIds.contains("InitialState"));
    }
}
