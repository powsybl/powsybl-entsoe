/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.flow_decomposition;

import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.loadflow.LoadFlowResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 */
class LoadFlowRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoadFlowRunner.class);
    public static final boolean LOAD_FLOW_DC = true;
    private final LoadFlowParameters loadFlowParameters;
    //private final LoadFlowParameters fallbackLoadFlowParameters;

    LoadFlowRunner(LoadFlowParameters loadFlowParameters) {
        this.loadFlowParameters = loadFlowParameters;
        //this.fallbackLoadFlowParameters = loadFlowParameters.copy().setDc(LOAD_FLOW_DC);
    }

    void runAllVariants(Network network) {
        String originVariant = network.getVariantManager().getWorkingVariantId();
        network.getVariantManager().getVariantIds().forEach(variantId -> {
            network.getVariantManager().setWorkingVariant(variantId);
            run(network);
        });
        network.getVariantManager().setWorkingVariant(originVariant);

    }

    void run(Network network) {
        LoadFlowResult loadFlowResult = LoadFlow.run(network, loadFlowParameters);
        if (!loadFlowResult.isOk()) {
            LOGGER.error(String.format("Load Flow diverged ! %s", loadFlowParameters.toString()));
            //loadFlowResult = LoadFlow.run(network, fallbackLoadFlowParameters);
            //if (!loadFlowResult.isOk()) {
            //    throw new PowsyblException("Load Flow has diverged in fallback !");
            //}
        }
    }
}
