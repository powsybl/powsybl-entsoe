/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.flow_decomposition.glsk_provider;

import com.powsybl.flow_decomposition.GlskProvider;
import com.powsybl.glsk.api.GlskDocument;
import com.powsybl.glsk.commons.CountryEICode;
import com.powsybl.glsk.commons.ZonalData;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.Network;
import com.powsybl.sensitivity.SensitivityVariableSet;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
public final class GlskDocumentBasedGlskProvider implements GlskProvider {
    private final GlskDocument glskDocument;
    private final Instant instant;
    private final boolean timeSpecific;

    private GlskDocumentBasedGlskProvider(GlskDocument glskDocument, Instant instant, boolean timeSpecific) {
        this.glskDocument = glskDocument;
        this.instant = instant;
        this.timeSpecific = timeSpecific;
    }

    @Override
    public Map<Country, Map<String, Double>> getGlsk(Network network) {
        Map<Country, Map<String, Double>> perCountryGlsk = getDefaultGlsk(network);
        updateWithGlskFromDocument(network, perCountryGlsk);
        return perCountryGlsk;
    }

    private void updateWithGlskFromDocument(Network network, Map<Country, Map<String, Double>> perCountryGlsk) {
        getSensitivityVariableSetZonalData(network).getDataPerZone().forEach((countryEicCode, glskData) -> perCountryGlsk.put(new CountryEICode(countryEicCode).getCountry(),
            glskData.getVariablesById().entrySet().stream().collect(Collectors.toMap(
                nodeFactorEntry -> nodeFactorEntry.getKey(),
                nodeFactorEntry -> nodeFactorEntry.getValue().getWeight()
            ))));
    }

    private ZonalData<SensitivityVariableSet> getSensitivityVariableSetZonalData(Network network) {
        if (timeSpecific) {
            Instant glskInstant = Optional.ofNullable(instant).orElse(getNetworkInstant(network));
            return glskDocument.getZonalGlsks(network, glskInstant);
        } else {
            return glskDocument.getZonalGlsks(network);
        }
    }

    private Instant getNetworkInstant(Network network) {
        return Instant.ofEpochMilli(network.getCaseDate().getMillis());
    }

    private Map<Country, Map<String, Double>> getDefaultGlsk(Network network) {
        return new AutoGlskProvider().getGlsk(network);
    }

    public static GlskProvider notTimeSpecific(GlskDocument glskDocument) {
        return new GlskDocumentBasedGlskProvider(glskDocument, null, false);
    }

    public static GlskProvider basedOnNetworkInstant(GlskDocument glskDocument) {
        return new GlskDocumentBasedGlskProvider(glskDocument, null, true);
    }

    public static GlskProvider basedOnGivenInstant(GlskDocument glskDocument, Instant instant) {
        return new GlskDocumentBasedGlskProvider(glskDocument, instant, true);
    }
}
