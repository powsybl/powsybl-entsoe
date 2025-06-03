/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.flow_decomposition;

import com.powsybl.iidm.network.Country;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 */
public class CsvExporter {
    private static final Path DEFAULT_EXPORT_DIR = Path.of(System.getProperty("java.io.tmpdir"));
    private static final Charset CHARSET = StandardCharsets.UTF_8;
    private static final CSVFormat FORMAT = CSVFormat.RFC4180;
    private static final Logger LOGGER = LoggerFactory.getLogger(CsvExporter.class);
    private static final String EMPTY_CELL_VALUE = "";
    private static final double NO_FLOW = 0.;

    public void export(FlowDecompositionResults flowDecompositionResults) {
        export(DEFAULT_EXPORT_DIR, flowDecompositionResults);
    }

    public void export(Path dirPath, FlowDecompositionResults flowDecompositionResults) {
        LOGGER.info("Saving rescaled flow decomposition (id: {}) of network {} in directory {}",
            flowDecompositionResults.getId(), flowDecompositionResults.getNetworkId(), dirPath);
        export(dirPath, flowDecompositionResults.getId(), flowDecompositionResults.getDecomposedFlowMap());
    }

    void export(Path dirPath, String basename, Map<String, DecomposedFlow> decomposedFlowMap) {
        Path path = Paths.get(dirPath.toString(), basename + ".csv");
        try (
            BufferedWriter writer = Files.newBufferedWriter(path, CHARSET);
            CSVPrinter printer = new CSVPrinter(writer, FORMAT)
        ) {
            Set<Country> allLoopFlowKeys = aggregateAllLoopFlowKeys(decomposedFlowMap);
            printHeaderRow(allLoopFlowKeys, printer);
            printContentRows(decomposedFlowMap, allLoopFlowKeys, printer);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private Set<Country> aggregateAllLoopFlowKeys(Map<String, DecomposedFlow> decomposedFlowMap) {
        return decomposedFlowMap.values().stream().flatMap(decomposedFlow -> decomposedFlow.getFlowPartition().loopFlowPerCountry().keySet().stream()).collect(Collectors.toSet());
    }

    private void failSilentlyPrint(CSVPrinter printer, Object valueToPrint) {
        try {
            printer.print(valueToPrint);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void failSilentlyPrintLn(CSVPrinter printer) {
        try {
            printer.println();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void printHeaderRow(Set<Country> loopFlowKeys, CSVPrinter printer) {
        failSilentlyPrint(printer, EMPTY_CELL_VALUE);
        failSilentlyPrint(printer, DecomposedFlow.ALLOCATED_COLUMN_NAME);
        failSilentlyPrint(printer, DecomposedFlow.INTERNAL_COLUMN_NAME);
        failSilentlyPrint(printer, DecomposedFlow.PST_COLUMN_NAME);
        loopFlowKeys.stream().sorted().forEach(loopFlowKey -> failSilentlyPrint(printer, NetworkUtil.getLoopFlowIdFromCountry(loopFlowKey)));
        failSilentlyPrint(printer, DecomposedFlow.AC_REFERENCE_FLOW_1_COLUMN_NAME);
        failSilentlyPrint(printer, DecomposedFlow.AC_REFERENCE_FLOW_2_COLUMN_NAME);
        failSilentlyPrint(printer, DecomposedFlow.DC_REFERENCE_FLOW_COLUMN_NAME);
        failSilentlyPrintLn(printer);
    }

    private void printContentRows(Map<String, DecomposedFlow> decomposedFlowMap, Set<Country> allLoopFlowKeys, CSVPrinter printer) {
        decomposedFlowMap.forEach((xnecId, decomposedFlow) -> printContentRow(xnecId, decomposedFlow, allLoopFlowKeys, printer));
    }

    private void printContentRow(String xnecId, DecomposedFlow decomposedFlow, Set<Country> allLoopFlowKeys, CSVPrinter printer) {
        failSilentlyPrint(printer, xnecId);
        failSilentlyPrint(printer, decomposedFlow.getAllocatedFlow());
        failSilentlyPrint(printer, decomposedFlow.getInternalFlow());
        failSilentlyPrint(printer, decomposedFlow.getPstFlow());
        allLoopFlowKeys.stream().sorted().forEach(loopFlowKey -> failSilentlyPrint(printer, decomposedFlow.getFlowPartition().loopFlowPerCountry().getOrDefault(loopFlowKey, NO_FLOW)));
        failSilentlyPrint(printer, decomposedFlow.getAcTerminal1ReferenceFlow());
        failSilentlyPrint(printer, decomposedFlow.getAcTerminal2ReferenceFlow());
        failSilentlyPrint(printer, decomposedFlow.getDcReferenceFlow());
        failSilentlyPrintLn(printer);
    }
}
