package com.powsybl.flow_decomposition;

import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;

import java.util.Map;

public interface GlskProvider {
    Map<Country, Map<String, Double>> getGlsk(Network network);
}
