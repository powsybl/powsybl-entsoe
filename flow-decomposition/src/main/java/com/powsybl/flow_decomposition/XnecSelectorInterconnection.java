/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.flow_decomposition;

import com.powsybl.contingency.Contingency;
import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Network;

import java.util.List;
import java.util.Map;

/**
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 */
class XnecSelectorInterconnection implements XnecSelector {
    public List<XnecWithDecomposition> run(Network network, Map<String, Contingency> variantContingenciesMap) {
        List<Branch> branchList = XnecSelector.getBranches(network, XnecSelector::isAnInterconnection);
        return XnecSelector.getXnecList(network, branchList, variantContingenciesMap);
    }
}
