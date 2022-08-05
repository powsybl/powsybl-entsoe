/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.flow_decomposition;

import com.powsybl.iidm.network.Network;

import java.util.List;
import java.util.Map;
import java.util.function.ObjDoubleConsumer;
import java.util.stream.Collectors;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 */
class ReferenceFlowComputer {
    Map<String, Double> run(List<XnecWithDecomposition> xnecList, Network network, ObjDoubleConsumer<DecomposedFlow> consumer) {
        return xnecList.stream().collect(Collectors.toMap(Xnec::getId, xnec -> {
            network.getVariantManager().setWorkingVariant(xnec.getVariantId());
            double p = xnec.getBranch().getTerminal1().getP();
            consumer.accept(xnec.getDecomposedFlowBeforeRescaling(), p);
            return p;
        }));
    }
}
