/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.cne.model;

import com.powsybl.contingency.Contingency;

import java.util.*;

/**
 * @author Thomas Adam <tadam at silicom.fr>
 */
public class ContingencySeries {

    private final String contingencyId;

    private final String contingencyName;

    private final List<RegisteredResource> registeredResourceList = new ArrayList<>();

    public ContingencySeries(Contingency contingency) {
        Objects.requireNonNull(contingency);
        this.contingencyId = contingency.getId();
        this.contingencyName = contingency.getId();
        contingency.getElements().forEach(e -> registeredResourceList.add(new RegisteredResource(e)));
    }

    public String getContingencyId() {
        return contingencyId;
    }

    public String getContingencyName() {
        return contingencyName;
    }

    public List<RegisteredResource> getRegisteredResourceList() {
        return Collections.unmodifiableList(registeredResourceList);
    }
}
