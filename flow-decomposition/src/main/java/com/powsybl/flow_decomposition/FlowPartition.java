package com.powsybl.flow_decomposition;

import com.powsybl.iidm.network.Country;

import java.util.Map;

public record FlowPartition(double internalFlow, double allocatedFlow, Map<Country, Double> loopFlowPerCountry, double pstFlow, double xNodeFlow) {
}
