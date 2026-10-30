/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.glsk.nc;

import com.powsybl.iidm.modification.scalable.Scalable;
import com.powsybl.iidm.network.Identifiable;
import com.powsybl.iidm.network.Network;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.powsybl.glsk.nc.NcGlskTestUtil.INSTANT;
import static com.powsybl.glsk.nc.NcGlskTestUtil.importTestCase;
import static com.powsybl.glsk.nc.NcGlskTestUtil.twoGeneratorsNetwork;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Tests of the resolution of the resources of a merit order against a network. The resources of the SIS profile are
 * generating units, whose mRID is not necessarily the id of the network element that is scaled. The profiles are the
 * remedial actions "rs-..." of the scaling-and-resolution test case, and merit-order-resource-classes for the classes of resources.
 *
 * @author Víctor Cardozo {@literal <victor.cardozo at artelys.com>}
 */
class NcGlskResourceResolutionTest {

    private static final String GENERATING_UNIT_PROPERTY = "CGMES.GeneratingUnit";
    private static final double EPSILON = 1e-6;

    private static NcGlskDocument document;

    @BeforeAll
    static void importTestCaseOnce() {
        document = importTestCase("scaling-and-resolution");
    }

    /**
     * @return the scalable of the merit order of a remedial action of the test case, for the network
     */
    private static Scalable scalable(Network network, String remedialAction) {
        return document.getZonalScalable(network, INSTANT).getData("zone-" + remedialAction);
    }

    /**
     * @return the ids of the network elements the scalable acts on
     */
    private static List<String> injectionIds(Scalable scalable, Network network) {
        return scalable.filterInjections(network).stream().map(Identifiable::getId).sorted().toList();
    }

    private static void assertSetPoints(Network network, double expectedGen, double expectedGen2) {
        assertEquals(expectedGen, network.getGenerator("GEN").getTargetP(), EPSILON);
        assertEquals(expectedGen2, network.getGenerator("GEN2").getTargetP(), EPSILON);
    }

    @Test
    void generatingUnitIsFoundThroughTheGeneratorProperty() {
        Network network = twoGeneratorsNetwork();
        network.getGenerator("GEN").setProperty(GENERATING_UNIT_PROPERTY, "unit-gen");
        network.getGenerator("GEN2").setProperty(GENERATING_UNIT_PROPERTY, "unit-gen2");
        Scalable scalable = scalable(network, "rs-property");

        assertEquals(List.of("GEN", "GEN2"), injectionIds(scalable, network));

        // GEN goes from 30 to 50, then GEN2 goes from 25 to 45
        assertEquals(40., scalable.scale(network, 40.), EPSILON);
        assertSetPoints(network, 50., 45.);
    }

    @Test
    void idTakesPrecedenceOverGeneratingUnitProperty() {
        Network network = twoGeneratorsNetwork();
        network.getGenerator("GEN2").setProperty(GENERATING_UNIT_PROPERTY, "GEN");
        Scalable scalable = scalable(network, "rs-id-precedence");

        assertEquals(List.of("GEN"), injectionIds(scalable, network));
        assertEquals(10., scalable.scale(network, 10.), EPSILON);
        assertSetPoints(network, 40., 25.);
    }

    @Test
    void unknownResourceIsIgnoredAndTheOthersAreKept() {
        Network network = twoGeneratorsNetwork();
        Scalable scalable = scalable(network, "rs-unknown");

        assertEquals(List.of("GEN"), injectionIds(scalable, network));
        assertEquals(20., scalable.scale(network, 20.), EPSILON);
        assertSetPoints(network, 50., 25.);
    }

    @Test
    void generatingUnitOfSeveralGeneratorsIsIgnored() {
        Network network = twoGeneratorsNetwork();
        network.getGenerator("GEN").setProperty(GENERATING_UNIT_PROPERTY, "shared-unit");
        network.getGenerator("GEN2").setProperty(GENERATING_UNIT_PROPERTY, "shared-unit");
        Scalable scalable = scalable(network, "rs-ambiguous");

        assertEquals(List.of("GEN2"), injectionIds(scalable, network));
        assertEquals(20., scalable.scale(network, 20.), EPSILON);
        assertSetPoints(network, 30., 45.);
    }

    @Test
    void disconnectedResourceIsIgnored() {
        Network network = twoGeneratorsNetwork();
        network.getGenerator("GEN2").getTerminal().disconnect();
        Scalable scalable = scalable(network, "rs-disconnected");

        assertEquals(List.of("GEN"), injectionIds(scalable, network));
        assertEquals(10., scalable.scale(network, 10.), EPSILON);
        assertSetPoints(network, 40., 25.);
    }

    @Test
    void generatorsAndLoadsCanBeMixed() {
        Network network = twoGeneratorsNetwork();
        Scalable scalable = scalable(network, "rs-mixed");

        assertEquals(List.of("GEN", "LOAD"), injectionIds(scalable, network));
    }

    @Test
    void nothingToScaleWhenNoResourceCanBeResolved() {
        Network network = twoGeneratorsNetwork();
        Scalable scalable = scalable(network, "rs-nothing");

        assertNotNull(scalable);
        assertEquals(List.of(), injectionIds(scalable, network));
        assertEquals(0., scalable.scale(network, 50.), EPSILON);
        assertSetPoints(network, 30., 25.);
    }

    @Test
    void allTheWaysToGetAScalableResolveTheResources() {
        Network network = twoGeneratorsNetwork();
        network.getGenerator("GEN").setProperty(GENERATING_UNIT_PROPERTY, "unit-gen");
        String zone = "zone-rs-all-ways";

        assertEquals(List.of("GEN"), injectionIds(document.getZonalScalable(network).getData(zone), network));
        assertEquals(List.of("GEN"), injectionIds(document.getZonalScalable(network, INSTANT).getData(zone), network));
        assertEquals(List.of("GEN"), injectionIds(document.getZonalScalableChronology(network).selectInstant(INSTANT).getData(zone), network));
    }

    @Test
    void resourcesOfDifferentClassesAreResolved() {
        // The bids refer to GEN (GeneratingUnit), GEN2 (PowerElectronicsUnit) and LOAD (EnergyConsumer), among other classes
        // of resources that the network does not know
        Network network = twoGeneratorsNetwork();
        NcGlskDocument resourceClasses = importTestCase("merit-order-resource-classes");

        Scalable scalable = resourceClasses.getZonalScalable(network, INSTANT).getData("zone-resource-classes");

        assertEquals(List.of("GEN", "GEN2", "LOAD"), injectionIds(scalable, network));
    }
}
