/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.entsoe.cgmes.balances_adjustment.util;

import com.powsybl.balances_adjustment.util.NetworkArea;
import com.powsybl.balances_adjustment.util.NetworkAreaFactory;
import com.powsybl.cgmes.extensions.CgmesControlArea;
import com.powsybl.iidm.network.Network;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * @author Miora Ralambotiana {@literal <miora.ralambotiana at rte-france.com>}
 */
public class CgmesVoltageLevelsAreaFactory implements NetworkAreaFactory {

    private final CgmesControlArea area;
    private final List<String> excludedXnodes;
    private final List<String> voltageLevelIds;

    public CgmesVoltageLevelsAreaFactory(CgmesControlArea area, String... voltageLevelIds) {
        this(area, Arrays.asList(voltageLevelIds));
    }

    public CgmesVoltageLevelsAreaFactory(CgmesControlArea area, List<String> voltageLevelIds) {
        this(area, null, voltageLevelIds);
    }

    public CgmesVoltageLevelsAreaFactory(List<String> excludedXnodes, List<String> voltageLevelIds) {
        this(null, excludedXnodes, voltageLevelIds);
    }

    public CgmesVoltageLevelsAreaFactory(CgmesControlArea area, List<String> excludedXnodes, List<String> voltageLevelIds) {
        this.area = area;
        this.excludedXnodes = excludedXnodes;
        this.voltageLevelIds = Objects.requireNonNull(voltageLevelIds);
    }

    @Override
    public NetworkArea create(Network network) {
        return new CgmesVoltageLevelsArea(network, area, excludedXnodes, voltageLevelIds);
    }
}
