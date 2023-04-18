/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.glsk.cse;

import com.powsybl.glsk.api.AbstractGlskRegisteredResource;
import com.powsybl.iidm.network.Identifiable;
import com.powsybl.iidm.network.Network;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 * @author Peter Mitri {@literal <peter.mitri@rte-france.com>}
 */
public class CseGlskRegisteredResource extends AbstractGlskRegisteredResource {
    private final Double initialFactor;

    public CseGlskRegisteredResource(NodeWrapper nodeWrapper) {
        Objects.requireNonNull(nodeWrapper);
        this.name = nodeWrapper.getName().orElse("");
        this.mRID = this.name;
        this.initialFactor = nodeWrapper.getFactor().map(BigDecimal::doubleValue).orElse(null);
        this.maximumCapacity = nodeWrapper.getPmax().map(CseGlskRegisteredResource::getNegativeDouble).orElse(null);
        this.minimumCapacity = nodeWrapper.getPmin().map(CseGlskRegisteredResource::getNegativeDouble).orElse(null);
    }

    private static Double getNegativeDouble(BigDecimal v) {
        return v.negate().doubleValue();
    }

    void setParticipationFactor(double participationFactor) {
        this.participationFactor = participationFactor;
    }

    @Override
    public String getGeneratorId() {
        return mRID + "_generator";
    }

    @Override
    public String getLoadId() {
        return mRID + "_load";
    }

    @Override
    public String getDanglingLineId(Network network) {
        Set<String> danglingLines = network.getDanglingLineStream()
            .filter(dl -> dl.getUcteXnodeCode().equals(mRID))
            .map(Identifiable::getId)
            .collect(Collectors.toSet());
        if (danglingLines.size() != 1) {
            // No or multiple dangling lines found for Xnode
            return null;
        }
        return danglingLines.iterator().next();
    }

    Optional<Double> getInitialFactor() {
        return Optional.ofNullable(initialFactor);
    }
}
