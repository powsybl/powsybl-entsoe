/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.flow_decomposition;

import com.powsybl.iidm.network.*;
import com.powsybl.network.area.BorderBasedCountryArea;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 * @author Hugo Schindler{@literal <hugo.schindler at rte-france.com>}
 */
public class NetPositionComputer {
    Map<Country, Double> run(Network network) {
        return computeNetPositions(network);
    }

    public static Map<Country, Double> computeNetPositions(Network network) {
        return network.getCountries().stream()
                .collect(Collectors.toMap(
                        country -> country,
                        country -> new BorderBasedCountryArea(network, Collections.singletonList(country)).getNetPosition(),
                        (v1, v2) -> v1,
                        () -> new EnumMap<>(Country.class)
                ));
    }
}
