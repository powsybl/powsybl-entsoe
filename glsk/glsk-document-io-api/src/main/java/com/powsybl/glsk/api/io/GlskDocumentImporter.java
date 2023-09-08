/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.glsk.api.io;

import com.powsybl.glsk.api.GlskDocument;
import org.apache.commons.lang3.NotImplementedException;

import java.io.InputStream;

/**
 * Interface for GLSK object import
 *
 * @author Viktor Terrier {@literal <viktor.terrier at rte-france.com>}
 */

public interface GlskDocumentImporter {

    GlskDocument importGlsk(InputStream inputStream);

    default GlskDocument importGlsk(InputStream inputStream, boolean useCalculationDirections) {
        throw new NotImplementedException("This importer does not handle CalculationDirections");
    }

    default GlskDocument importAndValidateGlsk(InputStream inputStream, boolean useCalculationDirections) {
        throw new NotImplementedException("This importer does not handle schema validation");
    }

    boolean canImport(InputStream inputStream);
}
