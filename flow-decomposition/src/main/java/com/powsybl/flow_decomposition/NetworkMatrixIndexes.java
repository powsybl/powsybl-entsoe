/*
 * Copyright (c) 2022, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.flow_decomposition;

import com.powsybl.iidm.network.*;

import java.util.List;
import java.util.Map;

/**
 * @author Hugo Schindler{@literal <hugo.schindler at rte-france.com>}
 * @author Sebastien Murgey{@literal <sebastien.murgey at rte-france.com>}
 */
class NetworkMatrixIndexes {
    private final List<Branch> xnecList;
    private final List<Injection<?>> nodeList;
    private final List<String> nodeIdList;
    private final List<String> pstList;
    private final Map<String, Integer> xnecIndex;
    private final Map<String, Integer> nodeIndex;
    private final Map<String, Integer> pstIndex;
    private final List<Injection<?>> xnodeList;

    NetworkMatrixIndexes(Network network, List<Branch> xnecList) {
        this.xnecList = xnecList;
        nodeList = NetworkUtil.getNodeList(network);
        nodeIdList = getNodeIdList(nodeList);
        pstList = NetworkUtil.getPstIdList(network);
        xnecIndex = NetworkUtil.getIndex(getXnecIdList(this.xnecList));
        nodeIndex = NetworkUtil.getIndex(nodeIdList);
        pstIndex = NetworkUtil.getIndex(pstList);
        xnodeList = NetworkUtil.getXNodeList(network);
    }

    List<Branch> getXnecList() {
        return xnecList;
    }

    List<Injection<?>> getNodeList() {
        return nodeList;
    }

    List<String> getNodeIdList() {
        return nodeIdList;
    }

    List<String> getPstList() {
        return pstList;
    }

    Map<String, Integer> getXnecIndex() {
        return xnecIndex;
    }

    Map<String, Integer> getNodeIndex() {
        return nodeIndex;
    }

    Map<String, Integer> getPstIndex() {
        return pstIndex;
    }

    int getPstCount() {
        return xnecList.size();
    }

    public List<Injection<?>> getUnmergedXNodeList() {
        return xnodeList;
    }

    private List<String> getNodeIdList(List<Injection<?>> nodeList) {
        return nodeList.stream().map(Injection::getId).toList();
    }

    private List<String> getXnecIdList(List<Branch> xnecList) {
        return xnecList.stream().map(Identifiable::getId).toList();
    }
}
