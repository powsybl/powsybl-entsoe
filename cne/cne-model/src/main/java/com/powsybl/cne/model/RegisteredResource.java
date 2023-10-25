/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.cne.model;

import com.powsybl.contingency.ContingencyElement;

/**
 * @author Thomas Adam {@literal <tadam at silicom.fr>}
 */
public class RegisteredResource {

    private final String id;

    private final String name;

    RegisteredResource(ContingencyElement element) {
        this.id = element.getId();
        this.name = element.getId();
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
