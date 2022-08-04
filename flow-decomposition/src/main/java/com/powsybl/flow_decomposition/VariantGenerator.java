/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.flow_decomposition;

import com.powsybl.contingency.Contingency;
import com.powsybl.iidm.network.Network;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 */
class VariantGenerator {
    public void run(Network network, List<Contingency> contingencies) {
        if (!contingencies.isEmpty()) {
            String originVariantId = network.getVariantManager().getWorkingVariantId();
            network.getVariantManager().cloneVariant(originVariantId, getVariantIds(contingencies));
            contingencies.forEach(contingency -> applyContingencyOnNetwork(network, contingency));
            network.getVariantManager().setWorkingVariant(originVariantId);
        }
    }

    private static List<String> getVariantIds(List<Contingency> contingencies) {
        return contingencies.stream().map(Contingency::getId).collect(Collectors.toList());
    }

    private static void applyContingencyOnNetwork(Network network, Contingency contingency) {
        network.getVariantManager().setWorkingVariant(contingency.getId());
        contingency.toModification().apply(network);
    }
}
