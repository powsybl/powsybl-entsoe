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
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.List;
import java.util.Map;

/**
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 */
public interface XnecProvider extends ContingenciesProvider {
    /**
     * Return basecase NEC
     * @param network network used to find branches.
     * @return a list of branches to monitor
     */
    List<Branch> getNetworkElements(Network network);

    /**
     * Return XNEC given contingency
     * @param network network used to find branches.
     * @return a list of branches to monitor
     */
    List<Branch> getNetworkElements(@NonNull String contingencyId, Network network);

    /**
     * Return all XNECs mapped by contingency (basecase not included)￥
     * @param network network used to find branches
     * @return
     */
    Map<String, List<Branch>> getNetworkElementsPerContingency(Network network);
}
