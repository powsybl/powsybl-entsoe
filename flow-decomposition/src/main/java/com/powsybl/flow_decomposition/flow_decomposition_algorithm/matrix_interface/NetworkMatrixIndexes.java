/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.flow_decomposition.flow_decomposition_algorithm.matrix_interface;

import com.powsybl.flow_decomposition.NetworkUtil;
import com.powsybl.iidm.network.*;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * @author Hugo Schindler{@literal <hugo.schindler@rte-france.com>}
 * @author Sebastien Murgey{@literal <sebastien.murgey at rte-france.com>}
 */
public class NetworkMatrixIndexes {
    private final List<Branch> xnecList;
    private final List<Injection<?>> nodeList;
    private final List<String> nodeIdList;
    private final List<String> pstList;
    private final Map<String, Integer> xnecIndex;
    private final Map<String, Integer> nodeIndex;
    private final Map<String, Integer> pstIndex;

    public NetworkMatrixIndexes(Network network, List<Branch> xnecList) {
        this.xnecList = xnecList;
        nodeList = getNodeList(network);
        nodeIdList = getNodeIdList(nodeList);
        pstList = getPstIdList(network);
        xnecIndex = getXnecIndex(this.xnecList);
        nodeIndex = NetworkUtil.getIndex(nodeIdList);
        pstIndex = NetworkUtil.getIndex(pstList);
    }

    public List<Branch> getXnecList() {
        return xnecList;
    }

    public List<Injection<?>> getNodeList() {
        return nodeList;
    }

    public List<String> getNodeIdList() {
        return nodeIdList;
    }

    public List<String> getPstList() {
        return pstList;
    }

    public Map<String, Integer> getXnecIndex() {
        return xnecIndex;
    }

    public Map<String, Integer> getNodeIndex() {
        return nodeIndex;
    }

    public Map<String, Integer> getPstIndex() {
        return pstIndex;
    }

    public int getPstCount() {
        return xnecList.size();
    }

    private List<Injection<?>> getNodeList(Network network) {
        return getAllNetworkInjections(network)
            .filter(this::isInjectionConnected)
            .filter(this::isInjectionInMainSynchronousComponent)
            .filter(this::managedInjectionTypes)
            .collect(Collectors.toList());
    }

    private boolean managedInjectionTypes(Injection<?> injection) {
        return !(injection instanceof BusbarSection || injection instanceof ShuntCompensator || injection instanceof StaticVarCompensator); // TODO Remove this fix once the active power computation after a DC load flow is fixed in OLF
    }

    private Stream<Injection<?>> getAllNetworkInjections(Network network) {
        return network.getConnectableStream()
            .filter(Injection.class::isInstance)
            .map(connectable -> (Injection<?>) connectable);
    }

    private boolean isInjectionConnected(Injection<?> injection) {
        return injection.getTerminal().isConnected();
    }

    private boolean isInjectionInMainSynchronousComponent(Injection<?> injection) {
        return NetworkUtil.isTerminalInMainSynchronousComponent(injection.getTerminal());
    }

    private List<String> getNodeIdList(List<Injection<?>> nodeList) {
        return nodeList.stream()
            .map(Injection::getId)
            .collect(Collectors.toList());
    }

    private List<String> getPstIdList(Network network) {
        return network.getTwoWindingsTransformerStream()
            .filter(this::isPst)
            .filter(this::hasNeutralStep)
            .map(Identifiable::getId)
            .collect(Collectors.toList());
    }

    private boolean isPst(TwoWindingsTransformer twt) {
        return twt.getPhaseTapChanger() != null;
    }

    private boolean hasNeutralStep(TwoWindingsTransformer pst) {
        return pst.getPhaseTapChanger().getNeutralStep().isPresent();
    }

    private Map<String, Integer> getXnecIndex(List<Branch> xnecList) {
        return IntStream.range(0, xnecList.size())
            .boxed()
            .collect(Collectors.toMap(
                i -> xnecList.get(i).getId(),
                Function.identity()
            ));
    }
}