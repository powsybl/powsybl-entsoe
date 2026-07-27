/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.flowdecomposition;

import com.powsybl.iidm.network.Country;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 */
final class FlowDecompositionAssertionsImporter {

    private FlowDecompositionAssertionsImporter() {
    }

    public static List<DecomposedFlowAssert> importFromResource(InputStream inputStream, String resourceName) throws IOException {
        if (inputStream == null) {
            throw new IllegalArgumentException("Resource not found: " + resourceName);
        }
        return parse(inputStream);
    }

    private static List<DecomposedFlowAssert> parse(InputStream is) throws IOException {
        List<DecomposedFlowAssert> assertions = new ArrayList<>();
        CSVFormat format = CSVFormat.DEFAULT.builder()
            .setHeader()
            .setSkipHeaderRecord(true)
            .get();
        for (CSVRecord record : format.parse(new InputStreamReader(is))) {
            DecomposedFlowAssert assertion = DecomposedFlowAssert.newAssert()
                .branchId(record.get("branchId"))
                .contingencyId(record.get("contingencyId"))
                .country1(Country.valueOf(record.get("country1")))
                .country2(Country.valueOf(record.get("country2")))
                .acReferenceFlow(Double.parseDouble(record.get("acReferenceFlow")))
                .dcReferenceFlow(Double.parseDouble(record.get("dcReferenceFlow")))
                .allocatedFlow(Double.parseDouble(record.get("allocatedFlow")))
                .xNodeFlow(Double.parseDouble(record.get("xNodeFlow")))
                .pstFlow(Double.parseDouble(record.get("pstFlow")))
                .internalFlow(Double.parseDouble(record.get("internalFlow")));
            String loopFlowsStr = record.get("loopFlows");
            if (!loopFlowsStr.isEmpty()) {
                for (String part : loopFlowsStr.split(";")) {
                    String[] kv = part.split(":");
                    assertion.loopFlow(Country.valueOf(kv[0]), Double.parseDouble(kv[1]));
                }
            }
            assertions.add(assertion);
        }
        return assertions;
    }
}
