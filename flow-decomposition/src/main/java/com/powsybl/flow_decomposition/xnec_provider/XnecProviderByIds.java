/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.flow_decomposition.xnec_provider;

import com.powsybl.flow_decomposition.XnecProvider;
import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Network;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 */
public class XnecProviderByIds implements XnecProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(XnecProviderByIds.class);
    private final Set<String> xnecList;

    public XnecProviderByIds(List<String> xnecList) {
        this.xnecList = new HashSet<>(xnecList);
    }

    @Override
    public List<Branch> getNetworkElements(Network network) {
        return xnecList.stream()
            .map(xnecId -> {
                Branch branch = network.getBranch(xnecId);
                if (Objects.isNull(branch)) {
                    LOGGER.warn("Branch {} without contingency was not found in network {}", xnecId, network.getId());
                }
                return branch;
            })
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }
}
