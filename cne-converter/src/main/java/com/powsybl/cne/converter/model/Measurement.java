/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.cne.converter.model;

import com.powsybl.security.LimitViolation;

/**
 * @author Thomas Adam <tadam at silicom.fr>
 */
public class Measurement {

    private final MeasurementType measurementType;

    private final UnitSymbol unitSymbol;

    private final double analogValue;

    Measurement(LimitViolation limitViolation) {
        this.measurementType = MeasurementType.from(limitViolation.getLimitType());
        this.unitSymbol = UnitSymbol.from(limitViolation.getLimitType());
        this.analogValue = limitViolation.getValue();
    }

    public MeasurementType getMeasurementType() {
        return measurementType;
    }

    public UnitSymbol getUnitSymbol() {
        return unitSymbol;
    }

    public double getAnalogValue() {
        return analogValue;
    }
}
