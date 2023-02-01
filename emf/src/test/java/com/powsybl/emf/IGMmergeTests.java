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
import com.powsybl.iidm.network.Network;
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
import java.util.*;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Bertrand Rix <bertrand.rix at artelys.com>
 */
class IGMmergeTests {

    private FileSystem fs;

    @BeforeEach
    public void setUp() {
        fs = Jimfs.newFileSystem(Configuration.unix());
    }

    @AfterEach
    public void tearDown() throws IOException {
        fs.close();
    }

    @Test
    void igmsDestructiveMerge() throws IOException {

        Set<String> branchIds = new HashSet<>();
        Set<String> generatorsId = new HashSet<>();
        Set<String> voltageLevelIds = new HashSet<>();

        //Load two IGMs BE and NL
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

        //Merge, Serialize and Deserialize the network
        igmBE.merge(igmNL);
        validNetworks.put("Merged", igmBE);

        Path destructiveMergeDir = Files.createDirectory(fs.getPath("/destructiveMerge"));
        exportNetwork(igmBE, destructiveMergeDir, "BE_NL", validNetworks, Set.of("EQ", "TP", "SSH", "SV"));

        //Copy the boundary set explicitly it is not serialized and is needed for reimport
        ResourceSet boundaries = CgmesConformity1Catalog.microGridBaseCaseBoundaries();
        for (String bFile : boundaries.getFileNames()) {
            Files.copy(boundaries.newInputStream(bFile), destructiveMergeDir.resolve("BE_NL" + bFile));
        }

        //Reimport and check
        Network serializedMergedNetwork = Network.read(new GenericReadOnlyDataSource(destructiveMergeDir, "BE_NL"), null);
        validate(serializedMergedNetwork, branchIds, generatorsId, voltageLevelIds);
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

        Path mergingViewMergeDir = Files.createDirectory(fs.getPath("/mergingViewMerge"));
        //Export to CGMES only state variable of the merged network, the rest is exported separately for each igms
        exportNetwork(mergingView, mergingViewMergeDir, "BE_NL", validNetworks, Set.of("SV"));
        exportNetwork(igmBE, mergingViewMergeDir, "BE_NL_BE", Map.of("BE", igmBE), Set.of("EQ", "TP", "SSH"));
        exportNetwork(igmNL, mergingViewMergeDir, "BE_NL_NL", Map.of("NL", igmNL), Set.of("EQ", "TP", "SSH"));

        //Copy the boundary set explicitly it is not serialized and is needed for reimport
        ResourceSet boundaries = CgmesConformity1Catalog.microGridBaseCaseBoundaries();
        for (String bFile : boundaries.getFileNames()) {
            Files.copy(boundaries.newInputStream(bFile), mergingViewMergeDir.resolve("BE_NL" + bFile));
        }

        Network serializedMergedNetwork = Network.read(new GenericReadOnlyDataSource(mergingViewMergeDir, "BE_NL"), null);
        validate(serializedMergedNetwork, branchIds, generatorsId, voltageLevelIds);
    }

    @Test
    void cgmToCgmes() throws IOException {
        //Read resources for BE and NL, merge the resources themselves and read a network from this set of resources
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
        Network networkBENL = Network.read(mergedResourcesBENL.dataSource());

        Set<String> branchIds = new HashSet<>();
        Set<String> generatorsId = new HashSet<>();
        Set<String> voltageLevelIds = new HashSet<>();

        //networkBENL.getBranches().forEach(b -> branchIds.add(b.getId()));
        networkBENL.getBranches().forEach(b -> branchIds.add(b.getId().replace(" ", "%20"))); // FIXME workaround before fixing CGMES export/import
        networkBENL.getGenerators().forEach(g -> generatorsId.add(g.getId()));
        networkBENL.getVoltageLevels().forEach(v -> voltageLevelIds.add(v.getId()));

        Path mergedResourcesDir = Files.createDirectory(fs.getPath("/mergedResourcesExport"));
        exportNetwork(networkBENL, mergedResourcesDir, "BE_NL", Map.of("BENL", networkBENL), Set.of("EQ", "TP", "SSH", "SV"));

        //Copy the boundary set explicitly it is not serialized and is needed for reimport
        ResourceSet boundaries = CgmesConformity1Catalog.microGridBaseCaseBoundaries();
        for (String bFile : boundaries.getFileNames()) {
            Files.copy(boundaries.newInputStream(bFile), mergedResourcesDir.resolve("BE_NL" + bFile));
        }
        Network serializedMergedNetwork = Network.read(new GenericReadOnlyDataSource(mergedResourcesDir, "BE_NL"), null);
        validate(serializedMergedNetwork, branchIds, generatorsId, voltageLevelIds);
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
        validNetworks.forEach((name, n) -> {
            context.addIidmMappings(n);
        });

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
}
