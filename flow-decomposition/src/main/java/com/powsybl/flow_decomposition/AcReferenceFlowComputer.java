/*
 * Copyright (c) 2024, Coreso SA (https://www.coreso.eu/) and TSCNET Services GmbH (https://www.tscnet.eu/)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.flow_decomposition;

import com.powsybl.iidm.network.Branch;
import com.powsybl.iidm.network.Identifiable;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Guillaume Verger {@literal <guillaume.verger at artelys.com>}
 */
class AcReferenceFlowComputer {
    private static ReferenceFlowComputer flowComputer = new ReferenceFlowComputer();

    Map<String, Double> run(Collection<Branch> xnecList, LoadFlowRunningService.Result loadFlowServiceAcResult) {
        if (loadFlowServiceAcResult.fallbackHasBeenActivated()) {
            return xnecList.stream().collect(Collectors.toMap(Identifiable::getId, branch -> Double.NaN));
        }

        return flowComputer.run(xnecList);
    }
}
