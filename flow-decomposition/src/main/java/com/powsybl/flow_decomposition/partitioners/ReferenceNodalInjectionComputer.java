/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.flow_decomposition.partitioners;

import com.powsybl.commons.PowsyblException;
import com.powsybl.iidm.network.Identifiable;
import com.powsybl.iidm.network.Injection;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
public class ReferenceNodalInjectionComputer {
    public ReferenceNodalInjectionComputer() {
    }

    public Map<String, Double> run(List<Injection<?>> nodeList) {
        return nodeList.stream()
            .collect(Collectors.toMap(
                Identifiable::getId,
                this::getReferenceInjection
            ));
    }

    private double getReferenceInjection(Injection<?> node) {
        double p = -node.getTerminal().getP();
        if (Double.isNaN(p)) {
            throw new PowsyblException(String.format("Reference nodal injection cannot be a Nan for node %s", node.getId()));
        }
        return p;
    }
}
