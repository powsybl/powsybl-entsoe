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
}
