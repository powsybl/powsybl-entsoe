package com.powsybl.flow_decomposition;

import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public interface FlowPartitioner {
    Map<String, FlowPartition> computeFlowPartitions(Network network, Set<Branch> xnecs, FlowDecompositionResults.PerStateBuilder flowDecompositionResultsBuilder, Map<Country, Double> netPositions, Map<Country, Map<String, Double>> glsks);
}
