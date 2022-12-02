/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.flow_decomposition.xnec_provider;

import com.powsybl.flow_decomposition.NetworkUtil;
import com.powsybl.flow_decomposition.XnecProvider;
import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Identifiable;
import com.powsybl.iidm.network.Network;
import java.util.List;
import java.util.LinkedList;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Alexandre LE JEAN
 */
public class XnecProviderHighestLoading implements XnecProvider {
    @Override
    public List<Branch> getNetworkElements(final Network network) {
        // Fetch XNEC and the set of XNE ids
        final List<Branch<?>> xnecList = NetworkUtil.getAllValidBranches(network).stream()
                .filter(XnecProviderInterconnection::isAnInterconnection)
                .collect(Collectors.toList());
        final Set<String> xneIds = xnecList.stream()
                .map(Identifiable::getNameOrId)
                .collect(Collectors.toSet());

        // Select one XNEC by XNE
        final List<Branch> selection = new LinkedList<>();
        for (final String xneId : xneIds) {
            Branch refBranch = getReferenceContingency(xneId, xnecList);
            selection.add(refBranch);
        }
        return selection;
    }

    private Branch getReferenceContingency(final String xneId, final List<Branch<?>> xnecList) {
        // TODO - implements
        // check for materialized remedial action
        // then, compare based on Active Power Flow
        return null;
    }
}
