/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.cne.model;

import com.powsybl.security.LimitViolation;

import java.util.*;

/**
 * @author Thomas Adam {@literal <tadam at silicom.fr>}
 */
public class MonitoredRegisteredResource {

    private final String equipmentId;

    private final String equipmentName;

    private final List<Measurement> measurementList = new ArrayList<>();

    public MonitoredRegisteredResource(Collection<LimitViolation> limitViolations) {
        Objects.requireNonNull(limitViolations);
        LimitViolation first = limitViolations.stream().findFirst().orElseThrow(() -> new IllegalArgumentException("LimitViolation list cannot be empty"));
        this.equipmentId = first.getSubjectId();
        this.equipmentName = Optional.ofNullable(first.getSubjectName()).orElse(this.equipmentId);
        limitViolations.forEach(l -> measurementList.add(new Measurement(l)));
    }

    public String getEquipmentId() {
        return equipmentId;
    }

    public String getEquipmentName() {
        return equipmentName;
    }

    public List<Measurement> getMeasurementList() {
        return Collections.unmodifiableList(measurementList);
    }
}
