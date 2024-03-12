/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.glsk.ucte.quality_check;

import com.powsybl.commons.report.ReportNode;
import com.powsybl.glsk.api.GlskPoint;
import com.powsybl.glsk.api.GlskRegisteredResource;
import com.powsybl.glsk.ucte.UcteGlskPoint;
import com.powsybl.iidm.network.Injection;
import com.powsybl.iidm.network.LoadType;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.VoltageLevel;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.powsybl.commons.report.TypedValue.WARN_SEVERITY;

/**
 * @author Marc Erkol {@literal <marc.erkol at rte-france.com>}
 */
class GlskQualityCheck {

    private static final String GENERATOR = "A04";

    private static final String LOAD = "A05";

    public static final String NODE_ID_KEY = "NodeId";

    public static final String TYPE_KEY = "Type";

    public static final String TSO_KEY = "TSO";

    public static void gskQualityCheck(GlskQualityCheckInput input, ReportNode reportNode) {
        new GlskQualityCheck().generateReport(input, reportNode);
    }

    private void generateReport(GlskQualityCheckInput input, ReportNode reportNode) {
        Map<String, UcteGlskPoint> glskPointMap = input.getUcteGlskDocument().getGlskPointsForInstant(input.getInstant());
        glskPointMap.forEach((country, glskPoint) -> checkGlskPoint(glskPoint, input.getNetwork(), country, reportNode));
    }

    private void checkGlskPoint(GlskPoint glskPoint, Network network, String tso, ReportNode reportNode) {
        List<String> manualGskGenerators = glskPoint.getGlskShiftKeys().stream()
                .filter(gskShiftKey -> gskShiftKey.getPsrType().equals(GENERATOR) && gskShiftKey.getBusinessType().equals("B43"))
                .flatMap(gskShiftKey -> gskShiftKey.getRegisteredResourceArrayList().stream())
                .map(GlskRegisteredResource::getGeneratorId).collect(Collectors.toList());
        List<String> manualGskLoads = glskPoint.getGlskShiftKeys().stream()
                .filter(gskShiftKey -> gskShiftKey.getPsrType().equals(LOAD) && gskShiftKey.getBusinessType().equals("B43"))
                .flatMap(gskShiftKey -> gskShiftKey.getRegisteredResourceArrayList().stream())
                .map(GlskRegisteredResource::getLoadId).collect(Collectors.toList());

        network.getVoltageLevelStream().forEach(voltageLevel -> voltageLevel.getBusBreakerView().getBuses().forEach(bus -> {
            if (manualGskGenerators.contains(bus.getId() + "_generator")) {
                createMissingGenerator(network, voltageLevel, bus.getId());
            }
            if (manualGskLoads.contains(bus.getId() + "_load")) {
                createMissingLoad(network, voltageLevel, bus.getId());
            }
        }));

        glskPoint.getGlskShiftKeys().forEach(glskShiftKey -> {
            if (glskShiftKey.getPsrType().equals(GENERATOR)) {
                glskShiftKey.getRegisteredResourceArrayList()
                        .forEach(resource -> checkResource(resource, network.getGenerator(resource.getGeneratorId()), "Generator", network, tso, reportNode));
            } else if (glskShiftKey.getPsrType().equals(LOAD)) {
                glskShiftKey.getRegisteredResourceArrayList()
                        .forEach(resource -> checkResource(resource, network.getLoad(resource.getLoadId()), "Load", network, tso, reportNode));
            }
        });
    }

    private void createMissingGenerator(Network network, VoltageLevel voltageLevel, String busId) {
        String generatorId = busId + "_generator";
        if (network.getGenerator(generatorId) == null) {
            voltageLevel.newGenerator()
                    .setBus(busId)
                    .setEnsureIdUnicity(true)
                    .setId(generatorId)
                    .setMaxP(9999)
                    .setMinP(0)
                    .setTargetP(0)
                    .setTargetQ(0)
                    .setTargetV(voltageLevel.getNominalV())
                    .setVoltageRegulatorOn(false)
                    .setFictitious(true)
                    .add()
                    .newMinMaxReactiveLimits().setMaxQ(99999).setMinQ(99999).add();
        }
    }

    private void createMissingLoad(Network network, VoltageLevel voltageLevel, String busId) {
        String loadId = busId + "_load";
        if (network.getLoad(loadId) == null) {
            voltageLevel.newLoad()
                    .setBus(busId)
                    .setEnsureIdUnicity(true)
                    .setId(loadId)
                    .setP0(0)
                    .setQ0(0)
                    .setLoadType(LoadType.FICTITIOUS)
                    .add();
        }
    }

    private void checkResource(GlskRegisteredResource registeredResource, Injection<?> injection, String type, Network network, String tso, ReportNode reportNode) {
        if (injection == null) {

            if (network.getBusBreakerView().getBus(registeredResource.getmRID()) == null) {
                reportNode.newReportNode().withMessageTemplate("1", "GLSK node is not found in CGM")
                    .withTypedValue(NODE_ID_KEY, registeredResource.getmRID(), "")
                    .withTypedValue(TYPE_KEY, type, "")
                    .withTypedValue(TSO_KEY, tso, "")
                    .withSeverity(WARN_SEVERITY)
                    .add();
            } else {
                reportNode.newReportNode().withMessageTemplate("2", "GLSK node is present but has no running Generator or Load")
                    .withTypedValue(NODE_ID_KEY, registeredResource.getmRID(), "")
                    .withTypedValue(TYPE_KEY, type, "")
                    .withTypedValue(TSO_KEY, tso, "")
                    .withSeverity(WARN_SEVERITY)
                    .add();
            }
        } else {
            if (!injection.getTerminal().isConnected()
                    || !injection.getTerminal().getBusBreakerView().getBus().isInMainSynchronousComponent()) {
                reportNode.newReportNode().withMessageTemplate("3", "GLSK node is connected to an island")
                    .withTypedValue(NODE_ID_KEY, registeredResource.getmRID(), "")
                    .withTypedValue(TYPE_KEY, type, "")
                    .withTypedValue(TSO_KEY, tso, "")
                    .withSeverity(WARN_SEVERITY)
                    .add();
            }
        }
    }
}
