/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.flow_decomposition;

import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * This class provides flow decomposition results from a network.
 * Those results are returned by a flowDecompositionComputer when run on a network.
 *
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 * @see FlowDecompositionComputer
 * @see DecomposedFlow
 */
public class FlowDecompositionResults {
    private final String networkId;
    private final String id;
    private final Set<Country> zoneSet;
    private final Map<String, DecomposedFlow> decomposedFlowMap;

    FlowDecompositionResults(Network network, Date date, Map<String, DecomposedFlow> decomposedFlowMap) {
        String dateString = new SimpleDateFormat("yyyyMMdd-HHmmss").format(date);
        this.networkId = network.getNameOrId();
        this.id = "Flow_Decomposition_Results_of_" + dateString + "_on_network_" + networkId;
        this.zoneSet = network.getCountries();
        this.decomposedFlowMap = decomposedFlowMap;
    }

    /**
     * @return Network Id
     */
    public String getNetworkId() {
        return networkId;
    }

    /**
     * @return Id composed of a time format and the network id.
     */
    public String getId() {
        return id;
    }

    /**
     * @return the set of available zones in this result
     */
    public Set<Country> getZoneSet() {
        return zoneSet;
    }

    /**
     * @return A rescaled flow decomposition map. The keys are the XNEC and the values are {@code DecomposedFlow} objects.
     */
    public Map<String, DecomposedFlow> getDecomposedFlowMap() {
        return decomposedFlowMap;
    }
}
