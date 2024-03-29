/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.flow_decomposition.glsk_provider;

import com.powsybl.flow_decomposition.GlskProvider;
import com.powsybl.flow_decomposition.NetworkUtil;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
public class AutoGlskProvider implements GlskProvider {
    @Override
    public Map<Country, Map<String, Double>> getGlsk(Network network) {
        Map<Country, Map<String, Double>> glsks =
                network.getCountries()
                       .stream()
                       .collect(Collectors.toMap(Function.identity(), country -> new HashMap<>()));

        network.getGeneratorStream().filter(g -> g.getTerminal().isConnected()).forEach(generator -> {
            Country generatorCountry = NetworkUtil.getInjectionCountry(generator);
            glsks.get(generatorCountry).put(generator.getId(), generator.getTargetP());
        });

        glsks.forEach((country, glsk) -> {
            double glskSum = glsk.values().stream().mapToDouble(factor -> factor).sum();
            if (glskSum == 0.0) {
                glsk.forEach((key, value) -> glsk.put(key, 1.0 / glsk.size()));
            } else {
                glsk.forEach((key, value) -> glsk.put(key, value / glskSum));
            }
        });
        return glsks;
    }
}
