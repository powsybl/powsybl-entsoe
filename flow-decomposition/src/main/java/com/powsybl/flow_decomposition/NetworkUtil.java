/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.flow_decomposition;

import com.powsybl.commons.PowsyblException;
import com.powsybl.iidm.network.*;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * @author Hugo Schindler{@literal <hugo.schindler at rte-france.com>}
 * @author Sebastien Murgey{@literal <sebastien.murgey at rte-france.com>}
 */
public final class NetworkUtil {
    static final String LOOP_FLOWS_COLUMN_PREFIX = "Loop Flow from";

    private NetworkUtil() {
        // Utility class
    }

    public static String getLoopFlowIdFromCountry(Network network, String identifiableId) {
        Identifiable<?> identifiable = network.getIdentifiable(identifiableId);
        if (identifiable instanceof Injection<?> injection) {
            return getLoopFlowIdFromCountry(getInjectionCountry(injection));
        }
        throw new PowsyblException(String.format("Identifiable %s must be an Injection", identifiableId));
    }

    public static String getLoopFlowIdFromCountry(Country country) {
        return String.format("%s %s", LOOP_FLOWS_COLUMN_PREFIX, country.toString());
    }

    public static Country getInjectionCountry(Injection<?> injection) {
        return getTerminalCountry(injection.getTerminal());
    }

    public static Country getTerminalCountry(Terminal terminal) {
        Optional<Substation> optionalSubstation = terminal.getVoltageLevel().getSubstation();
        if (optionalSubstation.isEmpty()) {
            throw new PowsyblException(String.format("Voltage level %s does not belong to any substation. " +
                    "Cannot retrieve country info needed for the algorithm.", terminal.getVoltageLevel().getId()));
        }
        Substation substation = optionalSubstation.get();
        Optional<Country> optionalCountry = substation.getCountry();
        if (optionalCountry.isEmpty()) {
            throw new PowsyblException(String.format("Substation %s does not have country property " +
                    "needed for the algorithm.", substation.getId()));
        }
        return optionalCountry.get();
    }

    public static Map<String, Integer> getIndex(List<String> idList) {
        return IntStream.range(0, idList.size())
            .boxed()
            .collect(Collectors.toMap(idList::get, Function.identity()));
    }

    public static List<Branch> getAllValidBranches(Network network) {
        return network.getBranchStream()
                .filter(NetworkUtil::isConnected)
                .filter(NetworkUtil::hasABusToEachTerminal)
                .filter(NetworkUtil::isInMainSynchronousComponent)
                .toList();
    }

    private static boolean isConnected(Branch<?> branch) {
        return branch.getTerminal1().isConnected() && branch.getTerminal2().isConnected();
    }

    private static boolean hasABusToEachTerminal(Branch branch) {
        return hasABusInBusBreakerView(branch.getTerminal1()) && hasABusInBusBreakerView(branch.getTerminal2());
    }

    private static boolean hasABusInBusBreakerView(Terminal terminal1) {
        return Objects.nonNull(terminal1.getBusBreakerView().getBus());
    }

    private static boolean isInMainSynchronousComponent(Branch<?> branch) {
        return isTerminalInMainSynchronousComponent(branch.getTerminal1())
            && isTerminalInMainSynchronousComponent(branch.getTerminal2());
    }

    private static boolean isTerminalInMainSynchronousComponent(Terminal terminal) {
        // Sensitivity analysis does not work outside synchronous component...
        return terminal.getBusBreakerView().getBus().isInMainSynchronousComponent();
    }

    public static List<Injection<?>> getNodeList(Network network) {
        return getAllNetworkInjections(network)
            .filter(NetworkUtil::isNotPairedDanglingLine)
            .filter(NetworkUtil::isInjectionConnected)
            .filter(NetworkUtil::isInjectionInMainSynchronousComponent)
            .filter(NetworkUtil::hasReferenceInjections)
            .filter(NetworkUtil::isValidInjectionsForSensitivityComputation)
            .toList();
    }

    public static List<Injection<?>> getXNodeList(Network network) {
        return network.getDanglingLineStream()
            .filter(NetworkUtil::isNotPairedDanglingLine)
            .filter(NetworkUtil::isInjectionConnected)
            .filter(NetworkUtil::isInjectionInMainSynchronousComponent)
            .map(danglingLine -> (Injection<?>) danglingLine)
            .collect(Collectors.toList());
    }

    private static Stream<Injection<?>> getAllNetworkInjections(Network network) {
        return network.getConnectableStream()
            .filter(Injection.class::isInstance)
            .map(connectable -> (Injection<?>) connectable);
    }

    private static boolean isNotPairedDanglingLine(Injection<?> injection) {
        return !(injection instanceof DanglingLine danglingLine && danglingLine.isPaired());
    }

    private static boolean isInjectionConnected(Injection<?> injection) {
        return injection.getTerminal().isConnected();
    }

    private static boolean isInjectionInMainSynchronousComponent(Injection<?> injection) {
        return isTerminalInMainSynchronousComponent(injection.getTerminal());
    }

    private static boolean hasReferenceInjections(Injection<?> injection) {
        return !(injection instanceof BusbarSection);
    }

    private static boolean isValidInjectionsForSensitivityComputation(Injection<?> injection) {
        return !(injection instanceof ShuntCompensator || injection instanceof StaticVarCompensator);
    }

    public static List<String> getPstIdList(Network network) {
        return network.getTwoWindingsTransformerStream()
            .filter(NetworkUtil::isConnected)
            .filter(PhaseTapChangerHolder::hasPhaseTapChanger)
            .filter(NetworkUtil::hasNeutralStep)
            .filter(NetworkUtil::hasABusToEachTerminal)
            .filter(NetworkUtil::isInMainSynchronousComponent)
            .map(Identifiable::getId)
            .toList();
    }

    private static boolean hasNeutralStep(TwoWindingsTransformer pst) {
        return pst.getPhaseTapChanger().getNeutralStep().isPresent();
    }
}
