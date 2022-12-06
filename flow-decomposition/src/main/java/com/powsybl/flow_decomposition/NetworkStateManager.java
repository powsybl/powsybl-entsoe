/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.flow_decomposition;

import com.powsybl.iidm.network.Network;

/**
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 */
class NetworkStateManager {
    private final Network network;
    private final VariantManager variantManager;

    public NetworkStateManager(Network network, XnecProvider xnecProvider) {
        this.network = network;
        LossesCompensator.addZeroMWLossesLoadsOnBuses(network);
        this.variantManager = new VariantManager(network, xnecProvider);
        variantManager.createAVariantPerContingency(network);
    }

    public void setNetworkVariant(String contingencyId) {
        variantManager.setNetworkVariant(network, contingencyId);
    }

    public void deleteAllContingencyVariants() {
        variantManager.deleteAllContingencyVariants(network);
    }
}
