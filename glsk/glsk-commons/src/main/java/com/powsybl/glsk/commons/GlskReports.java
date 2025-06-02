/**
 * Copyright (c) 2025, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 * SPDX-License-Identifier: MPL-2.0
 */
package com.powsybl.glsk.commons;

import com.powsybl.commons.report.ReportNode;

import static com.powsybl.commons.report.TypedValue.WARN_SEVERITY;

/**
 * @author Olivier Perrin {@literal <olivier.perrin at rte-france.com>}
 */
public final class GlskReports {

    public static final String NODE_ID_KEY = "NodeId";
    public static final String TYPE_KEY = "Type";
    public static final String TSO_KEY = "TSO";

    private GlskReports() {
    }

    public static void reportNodeNotFound(String nodeId, String type, String tso, ReportNode reportNode) {
        reportNode.newReportNode()
                .withMessageTemplate("entsoe.glsk.nodeNotFound")
                .withTypedValue(NODE_ID_KEY, nodeId, "")
                .withTypedValue(TYPE_KEY, type, "")
                .withTypedValue(TSO_KEY, tso, "")
                .withSeverity(WARN_SEVERITY)
                .add();
    }

    public static void reportNoRunningGeneratorOrLoad(String nodeId, String type, String tso, ReportNode reportNode) {
        reportNode.newReportNode()
                .withMessageTemplate("entsoe.glsk.noRunningGeneratorOrLoad")
                .withTypedValue(NODE_ID_KEY, nodeId, "")
                .withTypedValue(TYPE_KEY, type, "")
                .withTypedValue(TSO_KEY, tso, "")
                .withSeverity(WARN_SEVERITY)
                .add();
    }

    public static void reportConnectedToAnIsland(String nodeId, String type, String tso, ReportNode reportNode) {
        reportNode.newReportNode()
                .withMessageTemplate("entsoe.glsk.connectedToAnIsland")
                .withTypedValue(NODE_ID_KEY, nodeId, "")
                .withTypedValue(TYPE_KEY, type, "")
                .withTypedValue(TSO_KEY, tso, "")
                .withSeverity(WARN_SEVERITY)
                .add();
    }
}
