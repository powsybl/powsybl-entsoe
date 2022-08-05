/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.flow_decomposition;

import com.powsybl.contingency.Contingency;
import com.powsybl.iidm.network.Network;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 */
class VariantGenerator {
    public void run(Network network, Map<String, Contingency> variantContingencyMap) {
        if (!variantContingencyMap.isEmpty()) {
            String originVariantId = network.getVariantManager().getWorkingVariantId();
            network.getVariantManager().cloneVariant(originVariantId, getVariantIds(variantContingencyMap));
            variantContingencyMap.forEach((variantId, contingency) -> applyContingencyOnNetwork(network, variantId, contingency));
            network.getVariantManager().setWorkingVariant(originVariantId);
        }
    }

    private static List<String> getVariantIds(Map<String, Contingency> contingencies) {
        return new ArrayList<>(contingencies.keySet());
    }

    private static void applyContingencyOnNetwork(Network network, String variantId, Contingency contingency) {
        network.getVariantManager().setWorkingVariant(variantId);
        contingency.toModification().apply(network);
    }
}
