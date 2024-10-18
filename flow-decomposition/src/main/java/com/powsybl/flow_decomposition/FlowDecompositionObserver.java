/*
 * Copyright (c) 2024, Coreso SA (https://www.coreso.eu/) and TSCNET Services GmbH (https://www.tscnet.eu/)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.flow_decomposition;

import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import com.powsybl.loadflow.LoadFlowResult;

import java.util.Map;

/**
 * @author Guillaume Verger {@literal <guillaume.verger at artelys.com>}
 * @author Caio Luke {@literal <caio.luke at artelys.com>}
 */
public interface FlowDecompositionObserver {

    /**
     * Called when the computation starts
     */
    void runStart();

    /**
     * Called when the computation is done
     */
    void runDone();

    /**
     * Called when the base case starts to be computed
     */
    void computingBaseCase();

    /**
     * Called when a contingency computation starts
     *
     * @param contingencyId The current contingency id
     */
    void computingContingency(String contingencyId);

    /**
     * Called when Glsk are computed thanks to the GlskProvider (during base case computation)
     *
     * @param glsks the Glsks per country per generator
     */
    void computedGlsk(Map<Country, Map<String, Double>> glsks);

    /**
     * Called when net positions are computed (for base case computation)
     *
     * @param netPositions the net positions per country
     */
    void computedNetPositions(Map<Country, Double> netPositions);

    /**
     * Called when the nodal injection matrix is computed (for base case or contingency)
     *
     * @param nodalInjections the matrix of nodal injections indexed by(node, flow)
     */
    void computedNodalInjectionsMatrix(Map<String, Map<String, Double>> nodalInjections);

    /**
     * Called when the PTDF matrix is computed (for base case or contingency)
     *
     * @param ptdfMatrix the matrix of ptdf indexed by (line, node)
     */
    void computedPtdfMatrix(Map<String, Map<String, Double>> ptdfMatrix);

    /**
     * Called when the PSDF matrix is computed (for base case or contingency)
     *
     * @param psdfMatrix the matrix of psdf indexed by (line, node)
     */
    void computedPsdfMatrix(Map<String, Map<String, Double>> psdfMatrix);

    /**
     * Called after an AC loadflow has been computed
     *
     * @param network the network after AC loadflow
     * @param loadFlowResult loadflow result after AC loadflow
     * @param fallbackHasBeenActivated true if AC loadflow didn't converge
     */
    void computedAcLoadFlowResults(Network network, LoadFlowResult loadFlowResult, boolean fallbackHasBeenActivated);

    /**
     * Called after a DC loadflow has been computed
     *
     * @param network the network after DC loadflow
     * @param loadFlowResult loadflow result after DC loadflow
     */
    void computedDcLoadFlowResults(Network network, LoadFlowResult loadFlowResult);
}
