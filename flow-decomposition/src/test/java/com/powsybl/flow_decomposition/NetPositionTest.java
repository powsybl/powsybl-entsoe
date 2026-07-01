/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.flow_decomposition;

import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.BoundaryLineFilter;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.TieLine;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowParameters;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static com.powsybl.flow_decomposition.TestUtils.importNetwork;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 * @author Peter Mitri {@literal <peter.mitri at rte-france.com>}
 * @author Hugo Schindler{@literal <hugo.schindler at rte-france.com>}
 */
class NetPositionTest {
    private static final double DOUBLE_TOLERANCE = 1e-3;

    @Test
    void testTieLineWithNonZeroSumOfNetPositions() {
        String networkFileName = "19700101_0000_FO4_UX1.uct";
        Network network = importNetwork(networkFileName);
        network.getBoundaryLineStream().forEach(boundaryLine -> {
            boundaryLine.setR(1E-1);
            boundaryLine.setB(1E-3);
        });

        LoadFlow.run(network, LoadFlowParameters.load().setDc(false));
        assertEquals(800, network.getBoundaryLineStream(BoundaryLineFilter.UNPAIRED).mapToDouble(boundaryLine -> boundaryLine.getBoundary().getP()).sum(), DOUBLE_TOLERANCE);
        assertEquals(0, network.getBoundaryLineStream(BoundaryLineFilter.PAIRED).mapToDouble(boundaryLine -> boundaryLine.getBoundary().getP()).sum(), DOUBLE_TOLERANCE);

        TieLine tieLine = network.getTieLine("XBF00011 BF000011 1 + XBF00011 FB000011 1");
        assertEquals(0.381, tieLine.getBoundaryLine1().getBoundary().getP() + tieLine.getBoundaryLine1().getTerminal().getP(), DOUBLE_TOLERANCE);
        assertEquals(0.381, tieLine.getBoundaryLine2().getBoundary().getP() + tieLine.getBoundaryLine2().getTerminal().getP(), DOUBLE_TOLERANCE);
        assertEquals(0, tieLine.getBoundaryLine1().getBoundary().getP() + tieLine.getBoundaryLine2().getBoundary().getP(), DOUBLE_TOLERANCE);

        Map<Country, Double> netPositions = NetPositionComputer.computeNetPositions(network);
        assertEquals(-800.651, netPositions.get(Country.BE), DOUBLE_TOLERANCE);
        assertEquals(1401.512, netPositions.get(Country.FR), DOUBLE_TOLERANCE);
        assertEquals(-1400.860, netPositions.get(Country.DE), DOUBLE_TOLERANCE);
        assertEquals(-800, netPositions.values().stream().mapToDouble(Double::doubleValue).sum(), DOUBLE_TOLERANCE);
    }
}
