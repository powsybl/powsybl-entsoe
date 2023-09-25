/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.glsk.api.util;

import com.powsybl.glsk.commons.GlskException;
import com.powsybl.iidm.network.Identifiable;
import com.powsybl.iidm.network.Network;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 * @author Peter Mitri {@literal <peter.mitri@rte-france.com>}
 */
public final class Util {

    private Util() {
        // Should not be instantiated
    }

    public static Node getUniqueNode(Element glskBlockElement, String tag) {
        return Optional.ofNullable(glskBlockElement.getElementsByTagName(tag).item(0))
            .orElseThrow(() -> new GlskException(String.format("Impossible to import GLSK: <%s> tag is missing", tag)));
    }

    public static String findDanglingLineIdForXndoe(Network network, String xnode) {
        Set<String> danglingLines = network.getDanglingLineStream()
            .filter(dl -> dl.getUcteXnodeCode().equals(xnode))
            .map(Identifiable::getId)
            .collect(Collectors.toSet());
        if (danglingLines.size() != 1) {
            // No / multiple dangling lines found for Xnode
            return null;
        }
        return danglingLines.iterator().next();
    }
}
