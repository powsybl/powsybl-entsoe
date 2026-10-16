/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.glsk.nc;

import com.powsybl.iidm.modification.scalable.Scalable;
import com.powsybl.iidm.network.Network;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static com.powsybl.glsk.nc.NcGlskTestUtil.INSTANT;
import static com.powsybl.glsk.nc.NcGlskTestUtil.importTestCase;
import static com.powsybl.glsk.nc.NcGlskTestUtil.twoGeneratorsNetwork;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Counter-trading scenarios: the merit order is converted to a scalable and scaled, and the set-points reached by the
 * generators are checked. They cover several steps for one unit, prices interleaved between units, both directions
 * and a disconnected generator. The profiles are the remedial actions "sc-..." of the scaling-and-resolution test case.
 * <p>
 * The network has two generators, GEN producing 30 MW and GEN2 producing 25 MW, both able to reach 1000 MW. The
 * power of a bid is the absolute set-point the unit can reach, not an increment.
 *
 * @author Víctor Cardozo {@literal <victor.cardozo at artelys.com>}
 */
class NcGlskMeritOrderScalingTest {

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

    private static void assertSetPoints(Network network, double expectedGen, double expectedGen2) {
        assertEquals(expectedGen, network.getGenerator("GEN").getTargetP(), EPSILON);
        assertEquals(expectedGen2, network.getGenerator("GEN2").getTargetP(), EPSILON);
    }

    /**
     * Bids by increasing price: GEN up to 50 MW, GEN2 up to 50 MW, GEN up to 100 MW, GEN2 up to 100 MW. For instance,
     * asking 35 MW takes 20 MW from the first bid (GEN from 30 to 50) and 15 MW from the second one (GEN2 from 25 to 40).
     */
    @ParameterizedTest
    @CsvSource({"10, 40, 25", "35, 50, 40", "50, 55, 50", "140, 100, 95"})
    void pricesInterleavedBetweenUnits(double asked, double expectedGen, double expectedGen2) {
        Network network = twoGeneratorsNetwork();
        Scalable scalable = scalable(network, "sc-interleaved");

        assertEquals(asked, scalable.scale(network, asked), EPSILON);

        assertSetPoints(network, expectedGen, expectedGen2);
    }

    @Test
    void sameUnitInSeveralUpSteps() {
        Network network = twoGeneratorsNetwork();
        Scalable scalable = scalable(network, "sc-same-unit");

        // GEN from 30 to 50, then GEN2 from 25 to 50, then GEN from 50 to 55
        assertEquals(50., scalable.scale(network, 50.), EPSILON);

        assertSetPoints(network, 55., 50.);
    }

    @Test
    void upAndDownStacksAreIndependent() {
        Network network = twoGeneratorsNetwork();
        Scalable scalable = scalable(network, "sc-up-down");

        assertEquals(50., scalable.scale(network, 50.), EPSILON);
        assertSetPoints(network, 80., 25.);

        assertEquals(-20., scalable.scale(network, -20.), EPSILON);
        assertSetPoints(network, 80., 5.);
    }

    @Test
    void severalStepsPerUnitInBothDirections() {
        Network network = twoGeneratorsNetwork();
        Scalable scalable = scalable(network, "sc-both-directions");

        // Upwards, GEN is the only unit with bids: from 30 to 55, then to 75. Only 45 MW out of 50 can be reached.
        assertEquals(45., scalable.scale(network, 50.), EPSILON);
        assertSetPoints(network, 75., 25.);

        // Downwards, the cheapest bid is GEN, which goes down from 75 to 0, then GEN2 goes down from 25 to 20
        assertEquals(-80., scalable.scale(network, -80.), EPSILON);
        assertSetPoints(network, 0., 20.);
    }

    @Test
    void disconnectedGeneratorIsSkipped() {
        Network network = twoGeneratorsNetwork();
        network.getGenerator("GEN2").getTerminal().disconnect();
        Scalable scalable = scalable(network, "sc-disconnected");

        // GEN2 cannot be used: GEN goes from 30 to 50, then from 50 to 80
        assertEquals(50., scalable.scale(network, 50.), EPSILON);

        assertSetPoints(network, 80., 25.);
    }
}
