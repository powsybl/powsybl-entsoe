package com.powsybl.flow_decomposition;

import com.powsybl.iidm.network.Country;

import java.util.Map;

public interface FlowDecompositionObserver {

    void computingBaseCase();
    void computingContingency(String contingencyId);

    void computedGlsk(Map<Country, Map<String, Double>> glsks);

    void computedNetPositions(Map<Country, Double> netPositions);
    void computedNodalInjectionsMatrix(SparseMatrixWithIndexesTriplet nodalInjectionsMatrix);
    void computedPtdfMatrix(SparseMatrixWithIndexesTriplet ptdfMatrix);
    void computedPsdfMatrix(SparseMatrixWithIndexesTriplet matrix);
}
