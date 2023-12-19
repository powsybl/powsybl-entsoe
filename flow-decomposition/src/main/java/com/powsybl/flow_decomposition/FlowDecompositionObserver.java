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

    void runStart();

    void runDone();

    void computingBaseCase();

    void computingContingency(String contingencyId);

    void computedGlsk(Map<Country, Map<String, Double>> glsks);

    void computedNetPositions(Map<Country, Double> netPositions);

    void computedNodalInjectionsMatrix(Map<String, Map<String, Double>> map);

    void computedPtdfMatrix(Map<String, Map<String, Double>> map);

    /**
     * Called when the psdf matrix is ready
     * @param map the matrix indexed by (line, node)
     */
    void computedPsdfMatrix(Map<String, Map<String, Double>> map);

    void computedAcNodalInjections(Map<String, Double> positions, boolean fallbackHasBeenActivated);

    void computedDcNodalInjections(Map<String, Double> positions);

    void computedAcFlows(Map<String, Double> flows);

    void computedDcFlows(Map<String, Double> flows);
}
