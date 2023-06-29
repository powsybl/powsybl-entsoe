/*
 * Copyright (c) 2023, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.emf;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import com.powsybl.cgmes.conformity.CgmesConformity1Catalog;
import com.powsybl.cgmes.conversion.export.*;
import com.powsybl.cgmes.model.GridModelReferenceResources;
import com.powsybl.commons.datasource.GenericReadOnlyDataSource;
import com.powsybl.commons.datasource.ResourceSet;
import com.powsybl.commons.xml.XmlUtil;
import com.powsybl.iidm.mergingview.MergingView;
import com.powsybl.iidm.modification.ReplaceTieLinesByLines;
import com.powsybl.iidm.network.DanglingLine;
import com.powsybl.iidm.network.LineCharacteristics;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.TieLine;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Bertrand Rix <bertrand.rix at artelys.com>
 */
class IGMmergeTests {

    private FileSystem fs;
    private Path tmpDir;

    @BeforeEach
    void setUp() throws IOException {
        fs = Jimfs.newFileSystem(Configuration.unix());
        tmpDir = Files.createDirectory(fs.getPath("/tmp"));
    }

    @AfterEach
    void tearDown() throws IOException {
        fs.close();
    }

    @Test
    void igmsDestructiveMerge() throws IOException {

        Set<String> branchIds = new HashSet<>();
        Set<String> generatorsId = new HashSet<>();
        Set<String> voltageLevelIds = new HashSet<>();

        // load two IGMs BE and NL
        Map<String, Network> validNetworks = new HashMap<>();
        GridModelReferenceResources resBE = CgmesConformity1Catalog.microGridBaseCaseBE();
        Network igmBE = Network.read(resBE.dataSource());
        validNetworks.put("BE", igmBE);
        igmBE.getBranches().forEach(b -> branchIds.add(b.getId()));
        igmBE.getGenerators().forEach(g -> generatorsId.add(g.getId()));
        igmBE.getVoltageLevels().forEach(v -> voltageLevelIds.add(v.getId()));

        GridModelReferenceResources resNL = CgmesConformity1Catalog.microGridBaseCaseNL();
        Network igmNL = Network.read(resNL.dataSource());
        validNetworks.put("NL", igmNL);
        igmNL.getBranches().forEach(b -> branchIds.add(b.getId()));
        igmNL.getGenerators().forEach(g -> generatorsId.add(g.getId()));
        igmNL.getVoltageLevels().forEach(v -> voltageLevelIds.add(v.getId()));

        // merge, serialize and deserialize the network
        igmBE.merge(igmNL);
        validNetworks.put("Merged", igmBE);

        // Check that we have subnetworks
        assertEquals(2, igmBE.getSubNetworks().size());

        Path destructiveMergeDir = Files.createDirectories(tmpDir.resolve("destructiveMerge"));
        exportNetwork(igmBE, destructiveMergeDir, "BE_NL", validNetworks, Set.of("EQ", "TP", "SSH", "SV"));

        // copy the boundary set explicitly it is not serialized and is needed for reimport
        ResourceSet boundaries = CgmesConformity1Catalog.microGridBaseCaseBoundaries();
        for (String bFile : boundaries.getFileNames()) {
            Files.copy(boundaries.newInputStream(bFile), destructiveMergeDir.resolve("BE_NL" + bFile), StandardCopyOption.REPLACE_EXISTING);
        }

        // reimport and check
        Network serializedMergedNetwork = Network.read(new GenericReadOnlyDataSource(destructiveMergeDir, "BE_NL"), null);
        validate(serializedMergedNetwork, branchIds, generatorsId, voltageLevelIds);

        // compare
        // FIXME(Luma) CGMES Export: tie lines as two separate equipment instead of a single ACLS
        // Right now in CGMES we are exporting tie lines as regular lines,
        // instead of two separate equipment.
        // Before comparing, perform the same modification on the original network
        // so that both networks are comparable
        // This should be removed when we export tie lines as two separate equipment
        new ReplaceTieLinesByLines().apply(igmBE);
        compareNetwork(serializedMergedNetwork, igmBE);
    }

    @Test
    void igmsMergeWithMergingView() throws IOException {

        Set<String> branchIds = new HashSet<>();
        Set<String> generatorsId = new HashSet<>();
        Set<String> voltageLevelIds = new HashSet<>();

        Map<String, Network> validNetworks = new HashMap<>();
        GridModelReferenceResources resBE = CgmesConformity1Catalog.microGridBaseCaseBE();
        Network igmBE = Network.read(resBE.dataSource());
        validNetworks.put("BE", igmBE);
        igmBE.getBranches().forEach(b -> branchIds.add(b.getId()));
        igmBE.getGenerators().forEach(g -> generatorsId.add(g.getId()));
        igmBE.getVoltageLevels().forEach(v -> voltageLevelIds.add(v.getId()));

        GridModelReferenceResources resNL = CgmesConformity1Catalog.microGridBaseCaseNL();
        Network igmNL = Network.read(resNL.dataSource());

        MergingView mergingView = MergingView.create("merged", "validation");
        mergingView.merge(igmBE, igmNL);
        validNetworks.put("NL", igmNL);
        igmNL.getBranches().forEach(b -> branchIds.add(b.getId()));
        igmNL.getGenerators().forEach(g -> generatorsId.add(g.getId()));
        igmNL.getVoltageLevels().forEach(v -> voltageLevelIds.add(v.getId()));
        validNetworks.put("Merged", mergingView);

        Path mergingViewMergeDir = Files.createDirectories(tmpDir.resolve("mergingViewMerge"));
        // export to CGMES only state variable of the merged network, the rest is exported separately for each igms
        exportNetwork(mergingView, mergingViewMergeDir, "BE_NL", validNetworks, Set.of("SV"));
        exportNetwork(igmBE, mergingViewMergeDir, "BE_NL_BE", Map.of("BE", igmBE), Set.of("EQ", "TP", "SSH"));
        exportNetwork(igmNL, mergingViewMergeDir, "BE_NL_NL", Map.of("NL", igmNL), Set.of("EQ", "TP", "SSH"));

        // copy the boundary set explicitly it is not serialized and is needed for reimport
        ResourceSet boundaries = CgmesConformity1Catalog.microGridBaseCaseBoundaries();
        for (String bFile : boundaries.getFileNames()) {
            Files.copy(boundaries.newInputStream(bFile), mergingViewMergeDir.resolve("BE_NL" + bFile), StandardCopyOption.REPLACE_EXISTING);
        }

        Network serializedMergedNetwork = Network.read(new GenericReadOnlyDataSource(mergingViewMergeDir, "BE_NL"), null);
        validate(serializedMergedNetwork, branchIds, generatorsId, voltageLevelIds);

        // compare
        resetDanglineLinesP0Q0(serializedMergedNetwork);
        resetDanglineLinesP0Q0(mergingView);
        compareNetwork(serializedMergedNetwork, mergingView);
    }

    @Test
    void cgmToCgmes() throws IOException {
        // read resources for BE and NL, merge the resources themselves and read a network from this set of resources
        Network networkBENL = createCGM();

        Set<String> branchIds = new HashSet<>();
        Set<String> generatorsId = new HashSet<>();
        Set<String> voltageLevelIds = new HashSet<>();

        networkBENL.getBranches().forEach(b -> branchIds.add(b.getId()));
        networkBENL.getGenerators().forEach(g -> generatorsId.add(g.getId()));
        networkBENL.getVoltageLevels().forEach(v -> voltageLevelIds.add(v.getId()));

        Path mergedResourcesDir = Files.createDirectories(tmpDir.resolve("mergedResourcesExport"));
        exportNetwork(networkBENL, mergedResourcesDir, "BE_NL", Map.of("BENL", networkBENL), Set.of("EQ", "TP", "SSH", "SV"));

        //Copy the boundary set explicitly it is not serialized and is needed for reimport
        ResourceSet boundaries = CgmesConformity1Catalog.microGridBaseCaseBoundaries();
        for (String bFile : boundaries.getFileNames()) {
            Files.copy(boundaries.newInputStream(bFile), mergedResourcesDir.resolve("BE_NL" + bFile), StandardCopyOption.REPLACE_EXISTING);
        }
        Network serializedMergedNetwork = Network.read(new GenericReadOnlyDataSource(mergedResourcesDir, "BE_NL"), null);
        validate(serializedMergedNetwork, branchIds, generatorsId, voltageLevelIds);

        // compare
        // FIXME(Luma) CGMES Export: tie lines as two separate equipment instead of a single ACLS
        // Right now in CGMES we are exporting tie lines as regular lines,
        // instead of two separate equipment.
        // Before comparing, perform the same modification on the original network
        // so that both networks are comparable
        // This should be removed when we export tie lines as two separate equipment
        new ReplaceTieLinesByLines().apply(networkBENL);
        compareNetwork(serializedMergedNetwork, networkBENL);
    }

    @Test
    void compareDestructiveMergeAndMergingView() {

        GridModelReferenceResources resBE = CgmesConformity1Catalog.microGridBaseCaseBE();
        Network igmBE = Network.read(resBE.dataSource());
        GridModelReferenceResources resNL = CgmesConformity1Catalog.microGridBaseCaseNL();
        Network igmNL = Network.read(resNL.dataSource());

        MergingView mergingView = MergingView.create("merged", "validation");
        mergingView.merge(igmBE, igmNL);

        Network igmBE2 = Network.read(resBE.dataSource());
        Network igmNL2 = Network.read(resNL.dataSource());

        igmBE2.merge(igmNL2);

        Network cgm = createCGM();

        compareNetwork(igmBE2, mergingView);

        resetDanglineLinesP0Q0(cgm);
        resetDanglineLinesP0Q0(igmBE2);
        compareNetwork(cgm, igmBE2);
    }

    private static void resetDanglineLinesP0Q0(Network network) {
        // FIXME(Luma) CGMES Importer: consider keeping p0, q0 also for assembled (CGM) imports
        // Adaptations to be able to compare assembled and merged networks
        // If a dangling line is paired (is part of a tie line)
        // we should not use p0, q0 anymore,
        // so it doesn't matter what values they have
        // Tie lines from CGM do not have p0, q0 set,
        // and Tie lines from merged networks keep their original p0, q0 values
        network.getTieLineStream().forEach(tl -> {
            tl.getDanglingLine1().setP0(0);
            tl.getDanglingLine1().setQ0(0);
            tl.getDanglingLine2().setP0(0);
            tl.getDanglingLine2().setQ0(0);
        });
    }

    private static void validate(Network n, Set<String> branchIds, Set<String> generatorsId, Set<String> voltageLevelIds) {
        branchIds.forEach(b -> assertNotNull(n.getBranch(b)));
        generatorsId.forEach(g -> assertNotNull(n.getGenerator(g)));
        voltageLevelIds.forEach(v -> assertNotNull(n.getVoltageLevel(v)));
    }

    private static void exportNetwork(Network network, Path outputDir, String baseName, Map<String, Network> validNetworks, Set<String> profilesToExport) {
        Objects.requireNonNull(network);
        Path filenameEq = outputDir.resolve(baseName + "_EQ.xml");
        Path filenameTp = outputDir.resolve(baseName + "_TP.xml");
        Path filenameSsh = outputDir.resolve(baseName + "_SSH.xml");
        Path filenameSv = outputDir.resolve(baseName + "_SV.xml");
        CgmesExportContext context = new CgmesExportContext();
        context.setScenarioTime(network.getCaseDate());
        validNetworks.forEach((name, n) -> context.addIidmMappings(n));

        if (profilesToExport.contains("EQ")) {
            export(filenameEq, writer -> EquipmentExport.write(network, writer, context));
        }
        if (profilesToExport.contains("TP")) {
            export(filenameTp, writer -> TopologyExport.write(network, writer, context));
        }
        if (profilesToExport.contains("SSH")) {
            export(filenameSsh, writer -> SteadyStateHypothesisExport.write(network, writer, context));
        }
        if (profilesToExport.contains("SV")) {
            export(filenameSv, writer -> StateVariablesExport.write(network, writer, context));
        }
    }

    private static void export(Path file, Consumer<XMLStreamWriter> outConsumer) {
        try (OutputStream out = Files.newOutputStream(file)) {
            XMLStreamWriter writer = XmlUtil.initializeWriter(true, "    ", out);
            outConsumer.accept(writer);
        } catch (IOException | XMLStreamException e) {
            throw new RuntimeException(e);
        }
    }

    private static boolean checkDanglingLine(DanglingLine dl1, DanglingLine dl2) {
        return dl1.getG() == dl2.getG() && dl1.getB() == dl1.getB() && dl1.getR() == dl2.getR()
                && dl1.getX() == dl2.getX() && dl1.getP0() == dl2.getP0() && dl1.getQ0() == dl2.getQ0();
    }

    private static final double TOLERANCE_RX = 1e-10;
    private static final double TOLERANCE_GB = 1e-4;

    private static void checkLineCharacteristics(LineCharacteristics line1, LineCharacteristics line2) {
        boolean halvesHaveSameOrder = true;
        if (line1 instanceof TieLine) {
            String id11 = ((TieLine) line1).getDanglingLine1().getId();
            String id21 = ((TieLine) line2).getDanglingLine1().getId();
            if (!id11.equals(id21)) {
                halvesHaveSameOrder = false;
            }
        }
        assertEquals(line1.getR(), line2.getR(), TOLERANCE_RX);
        assertEquals(line1.getX(), line2.getX(), TOLERANCE_RX);
        if (halvesHaveSameOrder) {
            assertEquals(line1.getB1(), line2.getB1(), TOLERANCE_GB);
            assertEquals(line1.getB2(), line2.getB2(), TOLERANCE_GB);
            assertEquals(line1.getG1(), line2.getG1(), TOLERANCE_GB);
            assertEquals(line1.getG2(), line2.getG2(), TOLERANCE_GB);
        } else {
            assertEquals(line1.getB1(), line2.getB2(), TOLERANCE_GB);
            assertEquals(line1.getB2(), line2.getB1(), TOLERANCE_GB);
            assertEquals(line1.getG1(), line2.getG2(), TOLERANCE_GB);
            assertEquals(line1.getG2(), line2.getG1(), TOLERANCE_GB);
        }
    }

    private static void compareNetwork(Network network1, Network network2) {
        assertEquals(network1.getDanglingLineCount(), network2.getDanglingLineCount());
        assertEquals(network1.getLineCount(), network2.getLineCount());
        network1.getDanglingLineStream().forEach(dl1 -> {
            DanglingLine dl2 = network2.getDanglingLine(dl1.getId());
            if (!checkDanglingLine(dl1, dl2)) {
                System.err.println("error");
            }
            assertTrue(checkDanglingLine(dl1, dl2));
        });
        network1.getLineStream().forEach(line1 -> {
            LineCharacteristics line2 = network2.getLine(line1.getId()); // cgm should be always at network1
            checkLineCharacteristics(line1, line2);
        });
        network1.getTieLineStream().forEach(tieLine1 -> {
            TieLine tieLine2 = network2.getTieLine(tieLine1.getId());
            checkLineCharacteristics(tieLine1, tieLine2);
        });
    }

    private Network createCGM() {
        GridModelReferenceResources mergedResourcesBENL = new GridModelReferenceResources(
                "MicroGrid-BaseCase-BE_NL_MergedResources",
                null,
                new ResourceSet("/conformity/cas-1.1.3-data-4.0.3/MicroGrid/BaseCase/CGMES_v2.4.15_MicroGridTestConfiguration_BC_BE_v2/",
                        "MicroGridTestConfiguration_BC_BE_DL_V2.xml",
                        "MicroGridTestConfiguration_BC_BE_DY_V2.xml",
                        "MicroGridTestConfiguration_BC_BE_EQ_V2.xml",
                        "MicroGridTestConfiguration_BC_BE_GL_V2.xml",
                        "MicroGridTestConfiguration_BC_BE_SSH_V2.xml",
                        "MicroGridTestConfiguration_BC_BE_SV_V2.xml",
                        "MicroGridTestConfiguration_BC_BE_TP_V2.xml"),
                new ResourceSet("/conformity/cas-1.1.3-data-4.0.3/MicroGrid/BaseCase/CGMES_v2.4.15_MicroGridTestConfiguration_BC_NL_v2/",
                        "MicroGridTestConfiguration_BC_NL_DL_V2.xml",
                        "MicroGridTestConfiguration_BC_NL_DY_V2.xml",
                        "MicroGridTestConfiguration_BC_NL_EQ_V2.xml",
                        "MicroGridTestConfiguration_BC_NL_GL_V2.xml",
                        "MicroGridTestConfiguration_BC_NL_SSH_V2.xml",
                        "MicroGridTestConfiguration_BC_NL_SV_V2.xml",
                        "MicroGridTestConfiguration_BC_NL_TP_V2.xml"),
                CgmesConformity1Catalog.microGridBaseCaseBoundaries());
        return Network.read(mergedResourcesBENL.dataSource());
    }
}
