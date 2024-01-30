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
import com.powsybl.cgmes.conversion.CgmesExport;
import com.powsybl.cgmes.model.GridModelReferenceResources;
import com.powsybl.commons.datasource.GenericReadOnlyDataSource;
import com.powsybl.commons.datasource.ResourceSet;
import com.powsybl.iidm.modification.ReplaceTieLinesByLines;
import com.powsybl.iidm.network.*;
import com.powsybl.loadflow.LoadFlow;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Bertrand Rix {@literal <bertrand.rix at artelys.com>}
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
    void igmsSubnetworksMerge() throws IOException {
        Set<String> branchIds = new HashSet<>();
        Set<String> generatorIds = new HashSet<>();
        Set<String> voltageLevelIds = new HashSet<>();

        // load two IGMs BE and NL
        GridModelReferenceResources resBE = CgmesConformity1Catalog.microGridBaseCaseBE();
        Network igmBE = Network.read(resBE.dataSource());
        igmBE.getBranches().forEach(b -> branchIds.add(b.getId()));
        igmBE.getGenerators().forEach(g -> generatorIds.add(g.getId()));
        igmBE.getVoltageLevels().forEach(v -> voltageLevelIds.add(v.getId()));

        GridModelReferenceResources resNL = CgmesConformity1Catalog.microGridBaseCaseNL();
        Network igmNL = Network.read(resNL.dataSource());
        igmNL.getBranches().forEach(b -> branchIds.add(b.getId()));
        igmNL.getGenerators().forEach(g -> generatorIds.add(g.getId()));
        igmNL.getVoltageLevels().forEach(v -> voltageLevelIds.add(v.getId()));

        // merge, serialize and deserialize the network
        Network merged = Network.merge("Merged", igmBE, igmNL);

        // Check that we have subnetworks
        assertEquals(2, merged.getSubnetworks().size());

        LoadFlow.run(merged);

        Path mergedDir = Files.createDirectories(tmpDir.resolve("subnetworksMerge"));
        exportNetwork(merged, mergedDir, "BE_NL", Set.of("EQ", "TP", "SSH", "SV"));

        // copy the boundary set explicitly it is not serialized and is needed for reimport
        ResourceSet boundaries = CgmesConformity1Catalog.microGridBaseCaseBoundaries();
        for (String bFile : boundaries.getFileNames()) {
            Files.copy(boundaries.newInputStream(bFile), mergedDir.resolve("BE_NL" + bFile), StandardCopyOption.REPLACE_EXISTING);
        }

        // reimport and check
        Network serializedMergedNetwork = Network.read(new GenericReadOnlyDataSource(mergedDir, "BE_NL"), null);
        validate(serializedMergedNetwork, branchIds, generatorIds, voltageLevelIds);

        // compare
        // FIXME(Luma) CGMES Export: tie lines as two separate equipment instead of a single ACLS
        // Right now in CGMES we are exporting tie lines as regular lines,
        // instead of two separate equipment.
        // Before comparing, perform the same modification on the original network
        // so that both networks are comparable
        // This should be removed when we export tie lines as two separate equipment
        new ReplaceTieLinesByLines().apply(merged);
        compareNetwork(serializedMergedNetwork, merged);
    }

    @Test
    void testSubnetworksExports() throws IOException {
        Set<String> branchIdsBE = new HashSet<>();
        Set<String> generatorIdsBE = new HashSet<>();
        Set<String> voltageLevelIdsBE = new HashSet<>();

        // load two IGMs BE and NL
        // BE is loaded twice to keep a reference for further comparison, since "merge" is destructive
        GridModelReferenceResources resBE = CgmesConformity1Catalog.microGridBaseCaseBE();
        Network igmBE = Network.read(resBE.dataSource());
        Network igmRefBE = Network.read(resBE.dataSource());
        String idBE = igmBE.getId();
        igmBE.getBranches().forEach(b -> branchIdsBE.add(b.getId()));
        igmBE.getGenerators().forEach(g -> generatorIdsBE.add(g.getId()));
        igmBE.getVoltageLevels().forEach(v -> voltageLevelIdsBE.add(v.getId()));

        GridModelReferenceResources resNL = CgmesConformity1Catalog.microGridBaseCaseNL();
        Network igmNL = Network.read(resNL.dataSource());

        // merge, serialize and deserialize the network
        Network merged = Network.merge("Merged", igmBE, igmNL);
        Network subnetworkBE = merged.getSubnetwork(idBE);

        // Check that we have subnetworks
        assertEquals(2, merged.getSubnetworks().size());

        Network retrievedFromSubnetwork = exportAndLoad(subnetworkBE, "subnetworksBE", "BE");
        validate(retrievedFromSubnetwork, branchIdsBE, generatorIdsBE, voltageLevelIdsBE);

        // compare with ref (after its export)
        Network retrievedFromRef = exportAndLoad(igmRefBE, "refBE", "BE");

        // compare
        compareNetwork(retrievedFromSubnetwork, retrievedFromRef);
    }

    private Network exportAndLoad(Network network, String dirName, String country) throws IOException {
        Path dir = Files.createDirectories(tmpDir.resolve(dirName));
        exportNetwork(network, dir, country, Set.of("EQ", "TP", "SSH"));

        // copy the boundary set explicitly it is not serialized and is needed for reimport
        ResourceSet boundaries = CgmesConformity1Catalog.microGridBaseCaseBoundaries();
        for (String bFile : boundaries.getFileNames()) {
            Files.copy(boundaries.newInputStream(bFile), dir.resolve(country + bFile), StandardCopyOption.REPLACE_EXISTING);
        }

        // reimport
        return Network.read(new GenericReadOnlyDataSource(dir, country), null);
    }

    @Test
    void cgmToCgmes() throws IOException {
        // read resources for BE and NL, merge the resources themselves and read a network from this set of resources
        Network networkBENL = createCGM();

        Set<String> branchIds = new HashSet<>();
        Set<String> generatorIds = new HashSet<>();
        Set<String> voltageLevelIds = new HashSet<>();

        networkBENL.getBranches().forEach(b -> branchIds.add(b.getId()));
        networkBENL.getGenerators().forEach(g -> generatorIds.add(g.getId()));
        networkBENL.getVoltageLevels().forEach(v -> voltageLevelIds.add(v.getId()));

        LoadFlow.run(networkBENL);

        Path mergedResourcesDir = Files.createDirectories(tmpDir.resolve("mergedResourcesExport"));
        exportNetwork(networkBENL, mergedResourcesDir, "BE_NL", Set.of("EQ", "TP", "SSH", "SV"));

        //Copy the boundary set explicitly it is not serialized and is needed for reimport
        ResourceSet boundaries = CgmesConformity1Catalog.microGridBaseCaseBoundaries();
        for (String bFile : boundaries.getFileNames()) {
            Files.copy(boundaries.newInputStream(bFile), mergedResourcesDir.resolve("BE_NL" + bFile), StandardCopyOption.REPLACE_EXISTING);
        }
        Network serializedMergedNetwork = Network.read(new GenericReadOnlyDataSource(mergedResourcesDir, "BE_NL"), null);
        validate(serializedMergedNetwork, branchIds, generatorIds, voltageLevelIds);

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
    void testCompareSubnetworksMergeAgainstAssembled() {
        Network merged = Network.merge("merged",
                Network.read(CgmesConformity1Catalog.microGridBaseCaseBE().dataSource()),
                Network.read(CgmesConformity1Catalog.microGridBaseCaseNL().dataSource()));
        Network assembled = createCGM();
        compareNetwork(assembled, merged);
    }

    private static void validate(Network n, Set<String> branchIds, Set<String> generatorsId, Set<String> voltageLevelIds) {
        branchIds.forEach(b -> assertNotNull(n.getBranch(b)));
        generatorsId.forEach(g -> assertNotNull(n.getGenerator(g)));
        voltageLevelIds.forEach(v -> assertNotNull(n.getVoltageLevel(v)));
    }

    private static void exportNetwork(Network network, Path outputDir, String baseName, Set<String> profilesToExport) {
        Objects.requireNonNull(network);
        Properties exportParams = new Properties();
        exportParams.put(CgmesExport.PROFILES, String.join(",", profilesToExport));
        network.write("CGMES", exportParams, outputDir.resolve(baseName));
    }

    private static void checkDanglingLine(DanglingLine dl1, DanglingLine dl2) {
        assertEquals(dl1.getG(), dl2.getG(), TOLERANCE_GB);
        assertEquals(dl1.getB(), dl2.getB(), TOLERANCE_GB);
        assertEquals(dl1.getR(), dl2.getR(), TOLERANCE_RX);
        assertEquals(dl1.getX(), dl2.getX(), TOLERANCE_RX);
        assertEquals(dl1.getP0(), dl2.getP0(), TOLERANCE_PQ);
        assertEquals(dl1.getQ0(), dl2.getQ0(), TOLERANCE_PQ);
    }

    private static final double TOLERANCE_RX = 1e-10;
    private static final double TOLERANCE_GB = 1e-4;
    private static final double TOLERANCE_PQ = 1e-4;

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
        assertEquals(network1.getTieLineCount(), network2.getTieLineCount());
        network1.getDanglingLineStream().forEach(dl1 -> {
            DanglingLine dl2 = network2.getDanglingLine(dl1.getId());
            checkDanglingLine(dl1, dl2);
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
