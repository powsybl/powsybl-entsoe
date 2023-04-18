/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.glsk.ucte;

import com.powsybl.glsk.api.AbstractGlskRegisteredResource;
import com.powsybl.iidm.network.Identifiable;
import com.powsybl.iidm.network.Network;
import org.w3c.dom.Element;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 * @author Peter Mitri {@literal <peter.mitri@rte-france.com>}
 */
public class UcteGlskRegisteredResource extends AbstractGlskRegisteredResource {

    public UcteGlskRegisteredResource(Element element) {
        Objects.requireNonNull(element);
        this.name = ((Element) element.getElementsByTagName("NodeName").item(0)).getAttribute("v");
        this.mRID = this.name;
        this.participationFactor = getContentAsDoubleOrNull(element, "Factor");
    }

    private Double getContentAsDoubleOrNull(Element baseElement, String tag) {
        return baseElement.getElementsByTagName(tag).getLength() == 0 ? null :
                Double.parseDouble(((Element) baseElement.getElementsByTagName(tag).item(0)).getAttribute("v"));
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
}
