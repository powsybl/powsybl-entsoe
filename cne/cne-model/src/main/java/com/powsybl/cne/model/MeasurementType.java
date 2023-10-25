/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.cne.model;

import com.powsybl.security.LimitViolationType;

/**
 * @author Thomas Adam {@literal <tadam at silicom.fr>}
 */
public enum MeasurementType {
    A01,
    A10,
    A11;

    static MeasurementType from(LimitViolationType type) {
        switch (type) {
            case CURRENT:
            case LOW_SHORT_CIRCUIT_CURRENT:
            case HIGH_SHORT_CIRCUIT_CURRENT:
                return A01;
            case LOW_VOLTAGE:
                return A10;
            case HIGH_VOLTAGE:
                return A11;
            case OTHER:
            default:
                throw new UnsupportedOperationException(type.name() + " is not managed");
        }
    }
}
