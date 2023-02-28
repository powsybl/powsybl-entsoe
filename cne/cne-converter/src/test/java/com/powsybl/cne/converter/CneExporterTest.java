/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.cne.converter;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import com.powsybl.commons.test.AbstractConverterTest;
import com.powsybl.commons.test.ComparisonUtils;
import com.powsybl.contingency.Contingency;
import com.powsybl.iidm.network.Branch;
import com.powsybl.loadflow.LoadFlowResult;
import com.powsybl.security.*;
import com.powsybl.security.converter.SecurityAnalysisResultExporter;
import com.powsybl.security.converter.SecurityAnalysisResultExporters;
import com.powsybl.security.extensions.ActivePowerExtension;
import com.powsybl.security.extensions.CurrentExtension;
import com.powsybl.security.extensions.VoltageExtension;
import com.powsybl.security.results.PostContingencyResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.Properties;

/**
 * Check CneExporter class
 *
 * @author Thomas Adam <tadam at silicom.fr>
 */
class CneExporterTest extends AbstractConverterTest {

    private FileSystem fileSystem;
    private Path workingDir;

    @BeforeEach
    void createFileSystem() {
        fileSystem = Jimfs.newFileSystem(Configuration.unix());
        workingDir = fileSystem.getPath("/work");
    }

    @AfterEach
    void closeFileSystem() throws IOException {
        fileSystem.close();
    }

    void exporterTest(SecurityAnalysisResult resultToExport, Properties parameters) throws IOException {
        // Target export file
        Path actualPath = workingDir.resolve("result.xml");
        // Try to export
        SecurityAnalysisResultExporters.export(resultToExport, parameters, actualPath, "CNE-XML");
        // check the exported file and compare it to iidm reference file
        try (InputStream is = Files.newInputStream(actualPath)) {
            ComparisonUtils.compareXml(getClass().getResourceAsStream("/cne.xml"), is);
        } catch (IOException e) {
            Assertions.fail(e.getMessage());
        }
    }

    @Test
    void exportTest() throws IOException {
        // mRID key is missing in config.yml file
        // Add mRID property because it is required to export
        Properties parameters = new Properties();
        parameters.put("cne.export.xml." + CneConstants.MRID, "CNE export test");
        exporterTest(create(), parameters);
    }

    @Test
    void exportTestWithoutMRID() throws IOException {
        // Empty properties
        final Properties parameters = new Properties();
        final SecurityAnalysisResult result = create();
        // Fail to export without mRID
        try {
            exporterTest(result, parameters);
            Assertions.fail("Expected an NullPointerException to be thrown");
        } catch (NullPointerException ex) {
            Assertions.assertEquals("mRID is missing", ex.getMessage());
        }
    }

    @Test
    void baseTest() {
        // Check getters / setters
        SecurityAnalysisResultExporter cneExporter = SecurityAnalysisResultExporters.getExporter("CNE-XML");
        Assertions.assertNotNull(cneExporter);
        Assertions.assertFalse(cneExporter.getComment().isEmpty());
        Assertions.assertEquals("CNE-XML", cneExporter.getFormat());
    }

    private static SecurityAnalysisResult create() {
        // Create a many LimitViolations
        LimitViolation violation1 = new LimitViolation("NHV1_NHV2_1", LimitViolationType.CURRENT, null, Integer.MAX_VALUE, 100, 0.95f, 110.0, Branch.Side.ONE);
        violation1.addExtension(ActivePowerExtension.class, new ActivePowerExtension(220.0));

        LimitViolation violation2 = new LimitViolation("NHV1_NHV2_2", LimitViolationType.CURRENT, "20'", 1200, 100, 1.0f, 110.0, Branch.Side.TWO);
        violation2.addExtension(ActivePowerExtension.class, new ActivePowerExtension(220.0, 230.0));
        violation2.addExtension(CurrentExtension.class, new CurrentExtension(95.0));

        LimitViolation violation3 = new LimitViolation("GEN", LimitViolationType.HIGH_VOLTAGE, 100, 0.9f, 110);
        LimitViolation violation4 = new LimitViolation("GEN2", LimitViolationType.LOW_VOLTAGE, 100, 0.7f, 115);
        violation4.addExtension(VoltageExtension.class, new VoltageExtension(400.0));

        // Create a Contingency
        Contingency contingency = Contingency.builder("contingency")
                .addBranch("NHV1_NHV2_2", "VLNHV1")
                .addBranch("NHV1_NHV2_1")
                .addGenerator("GEN")
                .addBusbarSection("BBS1")
                .build();
        // Create a preContingencyResult & postContingencyResult
        LimitViolationsResult preContingencyResult = new LimitViolationsResult(Collections.singletonList(violation1));
        LimitViolationsResult postContingencyLimitViolationResult = new LimitViolationsResult(Arrays.asList(violation2, violation3, violation4), Arrays.asList("action1", "action2"));
        PostContingencyResult postContingencyResult = new PostContingencyResult(contingency, PostContingencyComputationStatus.CONVERGED, postContingencyLimitViolationResult);
        // Create SecurityAnalysisResult
        return new SecurityAnalysisResult(preContingencyResult, LoadFlowResult.ComponentResult.Status.CONVERGED, Collections.singletonList(postContingencyResult));
    }
}
