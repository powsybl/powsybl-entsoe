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
public class VariantManager {
    private final String defaultVariantId;
    private final List<Contingency> contingencies;
    private final List<String> variantIdList;

    VariantManager(Network network, XnecProvider xnecProvider) {
        this.defaultVariantId = network.getVariantManager().getWorkingVariantId();
        contingencies = xnecProvider.getContingencies(network);
        variantIdList = contingencies.stream().map(Contingency::getId).collect(Collectors.toList());
    }

    void createAVariantPerContingency(Network network) {
        if (!variantIdList.isEmpty()) {
            network.getVariantManager().cloneVariant(defaultVariantId, variantIdList); //TODO delete variants
            contingencies.forEach(contingency -> {
                setNetworkVariant(network, contingency.getId());
                contingency.toModification().apply(network);
            });
            setDefaultNetworkVariant(network);
        }
    }

    private void setDefaultNetworkVariant(Network network) {
        setNetworkVariant(network, defaultVariantId);
    }

    void setNetworkVariant(Network network, String variantId) {
        network.getVariantManager().setWorkingVariant(variantId);
    }
}
