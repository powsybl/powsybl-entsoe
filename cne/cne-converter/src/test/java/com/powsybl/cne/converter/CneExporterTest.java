/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.cne.converter;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import com.powsybl.commons.AbstractConverterTest;
import com.powsybl.contingency.Contingency;
import com.powsybl.iidm.network.Branch;
import com.powsybl.security.*;
import com.powsybl.security.converter.SecurityAnalysisResultExporter;
import com.powsybl.security.converter.SecurityAnalysisResultExporters;
import com.powsybl.security.extensions.ActivePowerExtension;
import com.powsybl.security.extensions.CurrentExtension;
import com.powsybl.security.extensions.VoltageExtension;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.*;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.Properties;

/**
 * @author Thomas Adam <tadam at silicom.fr>
 */
public class CneExporterTest extends AbstractConverterTest {

    private FileSystem fileSystem;
    private Path workingDir;

    @Before
    public void createFileSystem() {
        fileSystem = Jimfs.newFileSystem(Configuration.unix());
        workingDir = fileSystem.getPath("/work");
    }

    @After
    public void closeFileSystem() throws IOException {
        fileSystem.close();
    }

    public void exporterTest(SecurityAnalysisResult resultToExport, Properties parameters) throws IOException {
        // Target export file
        Path actualPath = workingDir.resolve("result.xml");
        // Try to export
        SecurityAnalysisResultExporters.export(resultToExport, parameters, actualPath, "CNE-XML");
        // check the exported file and compare it to iidm reference file
        try (InputStream is = Files.newInputStream(actualPath)) {
            compareXml(getClass().getResourceAsStream("/cne.xml"), is);
        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void exportTest() throws IOException {
        Properties parameters = new Properties();
        parameters.put("cne.export.xml." + CneConstants.MRID, "CNE export test");
        exporterTest(create(), parameters);
    }

    @Test
    public void exportTestWithoutMRID() throws IOException {
        // Empty properties
        final Properties parameters = new Properties();
        final SecurityAnalysisResult result = create();
        // Fail to export without mRID
        try {
            exporterTest(result, parameters);
            Assert.fail("Expected an NullPointerException to be thrown");
        } catch (NullPointerException ex) {
            Assert.assertEquals("mRID is missing", ex.getMessage());
        }
    }

    @Test
    public void coverageTest() {
        SecurityAnalysisResultExporter cneExporter = SecurityAnalysisResultExporters.getExporter("CNE-XML");
        Assert.assertNotNull(cneExporter);
        Assert.assertFalse(cneExporter.getComment().isEmpty());
        Assert.assertEquals("CNE-XML", cneExporter.getFormat());
    }

    private static SecurityAnalysisResult create() {
        // Create a LimitViolation(CURRENT)
        LimitViolation violation1 = new LimitViolation("NHV1_NHV2_1", LimitViolationType.CURRENT, null, Integer.MAX_VALUE, 100, 0.95f, 110.0, Branch.Side.ONE);
        violation1.addExtension(ActivePowerExtension.class, new ActivePowerExtension(220.0));

        LimitViolation violation2 = new LimitViolation("NHV1_NHV2_2", LimitViolationType.CURRENT, "20'", 1200, 100, 1.0f, 110.0, Branch.Side.TWO);
        violation2.addExtension(ActivePowerExtension.class, new ActivePowerExtension(220.0, 230.0));
        violation2.addExtension(CurrentExtension.class, new CurrentExtension(95.0));

        LimitViolation violation3 = new LimitViolation("GEN", LimitViolationType.HIGH_VOLTAGE, 100, 0.9f, 110);
        LimitViolation violation4 = new LimitViolation("GEN2", LimitViolationType.LOW_VOLTAGE, 100, 0.7f, 115);
        violation4.addExtension(VoltageExtension.class, new VoltageExtension(400.0));

        Contingency contingency = Contingency.builder("contingency")
                .addBranch("NHV1_NHV2_2", "VLNHV1")
                .addBranch("NHV1_NHV2_1")
                .addGenerator("GEN")
                .addBusbarSection("BBS1")
                .build();

        LimitViolationsResult preContingencyResult = new LimitViolationsResult(true, Collections.singletonList(violation1));
        PostContingencyResult postContingencyResult = new PostContingencyResult(contingency, true, Arrays.asList(violation2, violation3, violation4), Arrays.asList("action1", "action2"));

        return new SecurityAnalysisResult(preContingencyResult, Collections.singletonList(postContingencyResult));
    }
}
