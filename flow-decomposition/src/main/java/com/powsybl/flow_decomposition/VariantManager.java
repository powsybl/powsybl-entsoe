/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.flow_decomposition;

import com.powsybl.contingency.Contingency;
import com.powsybl.iidm.network.Network;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 */
class VariantManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(VariantManager.class);
    private final String defaultVariantId;
    private final List<Contingency> contingencies;

    VariantManager(Network network, XnecProvider xnecProvider) {
        this.defaultVariantId = network.getVariantManager().getWorkingVariantId();
        contingencies = xnecProvider.getContingencies(network);
        LOGGER.info("Found {} contingencies. Default variant is '{}'", contingencies.size(), defaultVariantId);
    }

    void createAVariantPerContingency(Network network) {
        if (!contingencies.isEmpty()) {
            List<String> variantIdList = contingencies.stream().map(Contingency::getId).collect(Collectors.toList());
            network.getVariantManager().cloneVariant(defaultVariantId, variantIdList);
            contingencies.forEach(contingency -> {
                LOGGER.info("Creating variant for contingency: {}", contingency.getId());
                setNetworkVariant(network, contingency.getId());
                LOGGER.info("Applying contingency modification");
                contingency.toModification().apply(network);
            });
            setDefaultNetworkVariant(network);
        }
    }

    void setDefaultNetworkVariant(Network network) {
        LOGGER.info("Setting default network variant ({})", defaultVariantId);
        setNetworkVariant(network, defaultVariantId);
    }

    void setNetworkVariant(Network network, String variantId) {
        LOGGER.info("Setting network variant to: {}", variantId);
        network.getVariantManager().setWorkingVariant(variantId);
    }

    void deleteAllContingencyVariants(Network network) {
        LOGGER.info("Deleting all contingency variants");
        contingencies.forEach(contingency -> network.getVariantManager().removeVariant(contingency.getId()));
        setDefaultNetworkVariant(network);
    }
}
