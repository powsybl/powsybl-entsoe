/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.glsk.commons;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public class GlskException extends RuntimeException {

    public GlskException(String msg) {
        super(msg);
    }

    public GlskException(final Throwable throwable) {
        super(throwable);
    }

    public GlskException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
