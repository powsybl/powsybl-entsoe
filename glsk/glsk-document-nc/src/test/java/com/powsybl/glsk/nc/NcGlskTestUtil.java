/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.glsk.nc;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.test.EurostagTutorialExample1Factory;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Utilities to test the NC GLSK importer on the test cases (folders of profiles of the test resources), and on networks.
 *
 * @author Víctor Cardozo {@literal <victor.cardozo at artelys.com>}
 */
final class NcGlskTestUtil {

    /**
     * An instant of the day of the test cases (12 August 2029).
     */
    static final Instant INSTANT = Instant.parse("2029-08-12T12:00:00Z");

    private NcGlskTestUtil() {
    }

    /**
     * Runs an action and returns the messages logged as warnings by the given class meanwhile.
     */
    static List<String> warningsLoggedBy(Class<?> loggingClass, Runnable action) {
        Logger logger = (Logger) LoggerFactory.getLogger(loggingClass);
        Level previousLevel = logger.getLevel();
        boolean previousAdditive = logger.isAdditive();
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        logger.setLevel(Level.WARN);
        logger.setAdditive(false);
        try {
            action.run();
        } finally {
            logger.detachAppender(appender);
            logger.setLevel(previousLevel);
            logger.setAdditive(previousAdditive);
        }
        return appender.list.stream().filter(event -> event.getLevel() == Level.WARN).map(ILoggingEvent::getFormattedMessage).toList();
    }

    /**
     * Zips profiles of a test case (a folder of the test resources, profiles/) as an archive of profiles.
     *
     * @param name  name of the folder of the test case
     * @param files names of the profiles to zip; all the profiles of the folder if there is none
     */
    static byte[] zipTestCase(String name, String... files) {
        try {
            Path folder = Path.of(Objects.requireNonNull(NcGlskTestUtil.class.getResource("/profiles/" + name)).toURI());
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (ZipOutputStream zip = new ZipOutputStream(bytes); Stream<Path> folderFiles = Files.list(folder)) {
                List<Path> profiles = files.length == 0 ? folderFiles.sorted().toList()
                    : Arrays.stream(files).map(folder::resolve).toList();
                for (Path file : profiles) {
                    zip.putNextEntry(new ZipEntry(file.getFileName().toString()));
                    zip.write(Files.readAllBytes(file));
                    zip.closeEntry();
                }
            }
            return bytes.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Imports a test case: all the profiles of a folder of the test resources (profiles/), zipped in memory as
     * an archive of profiles.
     *
     * @param name name of the folder of the test case
     */
    static NcGlskDocument importTestCase(String name) {
        return NcGlskDocument.importGlsk(new ByteArrayInputStream(zipTestCase(name)));
    }

    /**
     * Network with two generators: GEN produces 30 MW and GEN2 produces 25 MW, both can reach 1000 MW. It also has a load.
     */
    static Network twoGeneratorsNetwork() {
        Network network = EurostagTutorialExample1Factory.createWithMoreGenerators();
        network.getGenerator("GEN").setTargetP(30.).setMaxP(1000.).setMinP(0.);
        network.getGenerator("GEN2").setTargetP(25.).setMaxP(1000.).setMinP(0.);
        return network;
    }
}
