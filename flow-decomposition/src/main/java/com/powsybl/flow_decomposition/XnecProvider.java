/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.flow_decomposition;

import com.powsybl.contingency.ContingenciesProvider;
import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Network;

import java.util.Map;
import java.util.Set;

/**
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 */
public interface XnecProvider extends ContingenciesProvider {
    /**
     * Return basecase NEC
     *
     * @param network network used to find branches.
     * @return a set of branches to monitor
     */
    Set<Branch> getNetworkElements(Network network);

    /**
     * Return XNEC given contingency
     *
     *
     * @param contingencyId mandatory contingency name.
     *                      The contingencies will be provided by the contingency provider
     * @param network network used to find branches.
     * @return a set of branches to monitor
     */
    Set<Branch> getNetworkElements(String contingencyId, Network network);

    /**
     * Return all XNECs mapped by contingency (basecase not included)
     *
     * @param network network used to find branches
     * @return
     */
    Map<String, Set<Branch>> getNetworkElementsPerContingency(Network network);
}
