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
import org.apache.commons.csv.CSVPrinter;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 */
final class FlowDecompositionAssertionsExporter {

    private FlowDecompositionAssertionsExporter() {
    }

    public static void export(FlowDecompositionResults results, Path path) throws IOException {
        CSVFormat format = CSVFormat.DEFAULT.builder()
            .setHeader("branchId", "contingencyId", "country1", "country2",
                "acReferenceFlow", "dcReferenceFlow", "allocatedFlow", "xNodeFlow",
                "pstFlow", "internalFlow", "loopFlows")
            .get();
        try (BufferedWriter writer = Files.newBufferedWriter(path);
             CSVPrinter printer = new CSVPrinter(writer, format)) {
            for (DecomposedFlow flow : results.getDecomposedFlowMap().values()) {
                printer.printRecord(
                    flow.getBranchId(),
                    flow.getContingencyId(),
                    flow.getCountry1(),
                    flow.getCountry2(),
                    flow.getAcTerminal1ReferenceFlow(),
                    flow.getDcReferenceFlow(),
                    flow.getAllocatedFlow(),
                    flow.getXNodeFlow(),
                    flow.getPstFlow(),
                    flow.getInternalFlow(),
                    formatLoopFlows(flow.getLoopFlows())
                );
            }
        }
    }

    private static String formatLoopFlows(Map<Country, Double> loopFlows) {
        return loopFlows.entrySet().stream()
            .filter(e -> Math.abs(e.getValue()) > 1e-3)
            .map(e -> e.getKey() + ":" + e.getValue())
            .collect(Collectors.joining(";"));
    }
}
