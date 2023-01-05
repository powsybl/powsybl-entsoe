/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.flow_decomposition;

import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.PhaseTapChanger;
import com.powsybl.iidm.network.PhaseTapChangerStep;

import java.util.Optional;

import static com.powsybl.flow_decomposition.DecomposedFlow.PST_COLUMN_NAME;

/**
 * @author Hugo Schindler {@literal <hugo.schindler at rte-france.com>}
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
class PstFlowComputer {
    SparseMatrixWithIndexesCSC run(Network network,
                                   NetworkMatrixIndexes networkMatrixIndexes,
                                   SparseMatrixWithIndexesTriplet psdfMatrix) {
        SparseMatrixWithIndexesTriplet deltaTapMatrix = getDeltaTapMatrix(network, networkMatrixIndexes);
        return SparseMatrixWithIndexesCSC.mult(psdfMatrix.toCSCMatrix(), deltaTapMatrix.toCSCMatrix());
    }

    private SparseMatrixWithIndexesTriplet getDeltaTapMatrix(Network network, NetworkMatrixIndexes networkMatrixIndexes) {
        SparseMatrixWithIndexesTriplet deltaTapMatrix =
            new SparseMatrixWithIndexesTriplet(networkMatrixIndexes.getPstIndex(),
                PST_COLUMN_NAME, networkMatrixIndexes.getPstCount());
        for (String pst : networkMatrixIndexes.getPstList()) {
            PhaseTapChanger phaseTapChanger = network.getTwoWindingsTransformer(pst).getPhaseTapChanger();
            Optional<PhaseTapChangerStep> neutralStep = phaseTapChanger.getNeutralStep();
            double deltaTap = 0.0;
            if (neutralStep.isPresent()) {
                deltaTap = phaseTapChanger.getCurrentStep().getAlpha() - neutralStep.get().getAlpha();
            }
            deltaTapMatrix.addItem(pst, PST_COLUMN_NAME, deltaTap);
        }
        return deltaTapMatrix;
    }
}
