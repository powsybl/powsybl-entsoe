/*
 * Copyright (c) 2024, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.flow_decomposition;

import com.powsybl.iidm.network.Country;

import java.util.Map;

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
     * Called when starting to compute for the contingency
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
     * Called when net positions are computed (during base case computation)
     *
     * @param netPositions the net positions per country
     */
    void computedNetPositions(Map<Country, Double> netPositions);

    /**
     * Called when the nodal injection matrix is computed (during n-state or contingency)
     *
     * @param nodalInjections the matrix of nodal injections indexed by(node, flow)
     */
    void computedNodalInjectionsMatrix(Map<String, Map<String, Double>> nodalInjections);

    /**
     * Called when the PTDF matrix is computed (during n-state or contingency)
     *
     * @param pdtfMatrix the matrix of ptdf indexed by (line, node)
     */
    void computedPtdfMatrix(Map<String, Map<String, Double>> pdtfMatrix);

    /**
     * Called when the psdf matrix is computed (during n-state or contingency)
     *
     * @param psdfMatrix the matrix of psdf indexed by (line, node)
     */
    void computedPsdfMatrix(Map<String, Map<String, Double>> psdfMatrix);

    /**
     * Called when the AC nodal injections matrix is computed (during n-state or contingency)
     *
     * @param positions the positions after AC loadflow
     * @param fallbackHasBeenActivated true if AC loadflow didn't converge
     */
    void computedAcNodalInjections(Map<String, Double> positions, boolean fallbackHasBeenActivated);

    /**
     * Called when the DC nodal injections matrix is computed (during n-state or contingency)
     *
     * @param positions the positions after DC loadflow
     */
    void computedDcNodalInjections(Map<String, Double> positions);

    /**
     * Called when the AC loadflow is computed (during n-state or contingency)
     *
     * @param flows the flows for all branches
     */
    void computedAcFlows(Map<String, Double> flows);

    /**
     * Called when the DC loadflow is computed (during n-state or contingency)
     *
     * @param flows the flows for all branches
     */
    void computedDcFlows(Map<String, Double> flows);
}
