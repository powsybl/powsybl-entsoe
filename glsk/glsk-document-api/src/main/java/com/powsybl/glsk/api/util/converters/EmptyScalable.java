/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.glsk.api.util.converters;

import com.powsybl.action.util.Scalable;
import com.powsybl.iidm.network.Generator;
import com.powsybl.iidm.network.Injection;
import com.powsybl.iidm.network.Network;

import java.util.Collections;
import java.util.List;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class EmptyScalable implements Scalable {
    @Override
    public double initialValue(Network network) {
        return 0;
    }

    @Override
    public void reset(Network network) {
        // Nothing to do
    }

    @Override
    public double maximumValue(Network network) {
        return 0;
    }

    @Override
    public double minimumValue(Network network) {
        return 0;
    }

    @Override
    public double maximumValue(Network network, ScalingConvention scalingConvention) {
        return 0;
    }

    @Override
    public double minimumValue(Network network, ScalingConvention scalingConvention) {
        return 0;
    }

    @Override
    public void listGenerators(Network network, List<Generator> list, List<String> list1) {
        // Nothing to do
    }

    @Override
    public List<Generator> listGenerators(Network network, List<String> list) {
        return Collections.emptyList();
    }

    @Override
    public List<Generator> listGenerators(Network network) {
        return Collections.emptyList();
    }

    @Override
    public void filterInjections(Network network, List<Injection> list, List<String> list1) {
        // Nothing to do
    }

    @Override
    public List<Injection> filterInjections(Network network, List<String> list) {
        return Collections.emptyList();
    }

    @Override
    public List<Injection> filterInjections(Network network) {
        return Collections.emptyList();
    }

    @Override
    public double scale(Network network, double v) {
        return 0;
    }

    @Override
    public double scale(Network network, double v, ScalingConvention scalingConvention) {
        return 0;
    }
}
