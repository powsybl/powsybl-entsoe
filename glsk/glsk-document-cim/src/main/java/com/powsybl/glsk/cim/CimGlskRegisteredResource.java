/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.glsk.cim;

import com.powsybl.glsk.api.AbstractGlskRegisteredResource;
import com.powsybl.iidm.network.Network;
import org.w3c.dom.Element;

import java.util.Objects;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 * @author Peter Mitri {@literal <peter.mitri@rte-france.com>}
 */
public class CimGlskRegisteredResource extends AbstractGlskRegisteredResource {

    public CimGlskRegisteredResource(Element element) {
        Objects.requireNonNull(element);
        this.mRID = element.getElementsByTagName("mRID").item(0).getTextContent();
        this.name = element.getElementsByTagName("name").item(0).getTextContent();
        this.participationFactor = getContentAsDoubleOrNull(element, "sK_ResourceCapacity.defaultCapacity");
        this.maximumCapacity = getContentAsDoubleOrNull(element, "resourceCapacity.maximumCapacity");
        this.minimumCapacity = getContentAsDoubleOrNull(element, "resourceCapacity.minimumCapacity");
    }

    private Double getContentAsDoubleOrNull(Element baseElement, String tag) {
        return baseElement.getElementsByTagName(tag).getLength() == 0 ? null :
                Double.parseDouble(baseElement.getElementsByTagName(tag).item(0).getTextContent());
    }

    @Override
    public String getGeneratorId() {
        return mRID;
    }

    @Override
    public String getLoadId() {
        return mRID;
    }

    @Override
    public String getDanglingLineId(Network network) {
        return mRID;
    }
}
