/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.cne.converter.model;

import com.powsybl.security.LimitViolationType;

/**
 * @author Thomas Adam <tadam at silicom.fr>
 */
public enum UnitSymbol {
    AMP,
    KVT;

    static UnitSymbol from(LimitViolationType type) {
        switch (type) {
            case CURRENT:
            case LOW_SHORT_CIRCUIT_CURRENT:
            case HIGH_SHORT_CIRCUIT_CURRENT:
                return AMP;
            case LOW_VOLTAGE:
            case HIGH_VOLTAGE:
                return KVT;
            case OTHER:
            default:
                throw new UnsupportedOperationException(type.name() + " is not managed");
        }
    }
}
