/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.glsk.api;

import com.powsybl.glsk.commons.ZonalData;
import com.powsybl.glsk.commons.ZonalDataChronology;
import com.powsybl.glsk.api.util.ZonalDataChronologyFromGlskDocument;
import com.powsybl.glsk.api.util.converters.GlskPointLinearGlskConverter;
import com.powsybl.glsk.api.util.converters.GlskPointScalableConverter;
import com.powsybl.glsk.api.util.ZonalDataFromGlskDocument;
import com.powsybl.action.util.Scalable;
import com.powsybl.iidm.network.Network;
import com.powsybl.sensitivity.factors.variables.LinearGlsk;

import java.time.Instant;
import java.util.List;

/**
 * @author Joris Mancini {@literal <joris.mancini at rte-france.com>}
 */
public interface GlskDocument {

    List<String> getZones();

    List<AbstractGlskPoint> getGlskPoints(String zone);

    /**
     * This method will produce a GLSK provider which is not time-specific. It is not guarantee that all GLSK document
     * can handle this method because all time-specific data should be extracted. To implement only if GLSK document
     * is not time-specific.
     *
     * @param network: Network on which to map GLSK document information.
     * @return A {@link ZonalData} of {@link LinearGlsk} extracted from the GLSK document.
     */
    default ZonalData<LinearGlsk> getZonalGlsks(Network network) {
        return new ZonalDataFromGlskDocument<>(this, network, GlskPointLinearGlskConverter::convert);
    }

    /**
     * This method will produce a GLSK provider which is not time-specific. Only data that is available at the
     * specified instant will be extracted.
     *
     * @param network: Network on which to map GLSK document information.
     * @param instant: Instant at which extracted data will be available.
     * @return A {@link ZonalData} of {@link LinearGlsk} extracted from the GLSK document.
     */
    default ZonalData<LinearGlsk> getZonalGlsks(Network network, Instant instant) {
        return new ZonalDataFromGlskDocument<>(this, network, GlskPointLinearGlskConverter::convert, instant);
    }

    /**
     * This method will produce a time-specific GLSK provider. All time-related data will be extracted.
     *
     * @param network: Network on which to map GLSK document information.
     * @return A {@link ZonalDataChronology} of {@link LinearGlsk} extracted from the GLSK document.
     */
    default ZonalDataChronology<LinearGlsk> getZonalGlsksChronology(Network network) {
        return new ZonalDataChronologyFromGlskDocument<>(this, network, GlskPointLinearGlskConverter::convert);
    }

    /**
     * This method will produce a scalable provider which is not time-specific. It is not guarantee that all GLSK
     * document can handle this method because all time-specific data should be extracted. To implement only if GLSK
     * document is not time-specific.
     *
     * @param network: Network on which to map GLSK document information.
     * @return A {@link ZonalData} of {@link Scalable} extracted from the GLSK document.
     */
    default ZonalData<Scalable> getZonalScalable(Network network) {
        return new ZonalDataFromGlskDocument<>(this, network, GlskPointScalableConverter::convert);
    }

    /**
     * This method will produce a scalable provider which is not time-specific. Only data that is available at the
     * specified instant will be extracted.
     *
     * @param network: Network on which to map GLSK document information.
     * @param instant: Instant at which extracted data will be available.
     * @return A {@link ZonalData} of {@link Scalable} extracted from the GLSK document.
     */
    default ZonalData<Scalable> getZonalScalable(Network network, Instant instant) {
        return new ZonalDataFromGlskDocument<>(this, network, GlskPointScalableConverter::convert, instant);
    }

    /**
     * This method will produce a time-specific scalable provider. All time-related data will be extracted.
     *
     * @param network: Network on which to map GLSK document information.
     * @return A {@link ZonalDataChronology} of {@link Scalable} extracted from the GLSK document.
     */
    default ZonalDataChronology<Scalable> getZonalScalableChronology(Network network) {
        return new ZonalDataChronologyFromGlskDocument<>(this, network, GlskPointScalableConverter::convert);
    }
}
