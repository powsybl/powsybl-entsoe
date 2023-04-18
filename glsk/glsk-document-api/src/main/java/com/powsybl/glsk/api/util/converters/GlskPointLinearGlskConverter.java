/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.glsk.api.util.converters;

import com.powsybl.glsk.api.GlskPoint;
import com.powsybl.glsk.api.GlskRegisteredResource;
import com.powsybl.glsk.api.GlskShiftKey;
import com.powsybl.glsk.commons.CountryEICode;
import com.powsybl.glsk.commons.GlskException;
import com.powsybl.iidm.network.*;
import com.powsybl.sensitivity.SensitivityVariableSet;
import com.powsybl.sensitivity.WeightedSensitivityVariable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Convert a single GlskPoint to LinearGlsk
 * @author Pengbo Wang {@literal <pengbo.wang@rte-international.com>}
 * @author Peter Mitri {@literal <peter.mitri@rte-france.com>}
 */
public final class GlskPointLinearGlskConverter {
    private static final Logger LOGGER = LoggerFactory.getLogger(GlskPointLinearGlskConverter.class);

    private GlskPointLinearGlskConverter() {
        throw new AssertionError("Utility class should not be instantiated");
    }

    /**
     * @param network IIDM network
     * @param glskPoint GLSK Point
     * @return farao-core LinearGlsk
     */
    public static SensitivityVariableSet convert(Network network, GlskPoint glskPoint) {

        String sensitivityVariableSetId = glskPoint.getSubjectDomainmRID() + ":" + glskPoint.getPointInterval().toString();

        /* Linear GLSK is used as sensitivityVariable in FlowBasedComputation
         * When it is added into sensivitityFactors, we should be able to find out LinearGlsk's country or NetWorkArea
         * For the moment, LinearGlsk's name is used to trace LinearGlsk's country or NetworkArea.
         * We could also added another attribute in LinearGlsk to mark this information,
         * but this change need to be in Powsybl-core
         */

        Objects.requireNonNull(glskPoint.getGlskShiftKeys());

        List<WeightedSensitivityVariable> weightedSensitivityVariables = new ArrayList<>();

        if (glskPoint.getGlskShiftKeys().size() > 2) {
            throw new GlskException("Multi (GSK+LSK) shift keys not supported yet...");
        }

        for (GlskShiftKey glskShiftKey : glskPoint.getGlskShiftKeys()) {
            if (glskShiftKey.getBusinessType().equals("B42")) {
                if (glskShiftKey.getRegisteredResourceArrayList().isEmpty()) {
                    LOGGER.debug("GLSK Type B42, empty registered resources list --> country (proportional) GLSK");
                    convertCountryProportional(network, glskShiftKey, weightedSensitivityVariables);
                } else {
                    LOGGER.debug("GLSK Type B42, not empty registered resources list --> (explicit/manual) proportional GSK");
                    convertExplicitProportional(network, glskShiftKey, weightedSensitivityVariables);
                }
            } else if (glskShiftKey.getBusinessType().equals("B43")) {
                LOGGER.debug("GLSK Type B43 --> participation factor proportional GSK");
                if (glskShiftKey.getRegisteredResourceArrayList().isEmpty()) {
                    throw new GlskException("Empty Registered Resources List in B43 type shift key.");
                } else {
                    convertParticipationFactor(network, glskShiftKey, weightedSensitivityVariables);
                }
            } else {
                throw new GlskException("convert not supported");
            }
        }

        return new SensitivityVariableSet(sensitivityVariableSetId, weightedSensitivityVariables);
    }

    /**
     * @param network iidm network
     * @param glskShiftKey country type shiftkey
     * @param weightedSensitivityVariables linearGlsk to be filled
     */
    private static void convertCountryProportional(Network network, GlskShiftKey glskShiftKey, List<WeightedSensitivityVariable> weightedSensitivityVariables) {
        Country country = new CountryEICode(glskShiftKey.getSubjectDomainmRID()).getCountry();
        //Generator A04 or Load A05
        if (glskShiftKey.getPsrType().equals("A04")) {
            //Generator A04
            List<Generator> generators = network.getGeneratorStream()
                    .filter(generator -> country.equals(generator.getTerminal().getVoltageLevel().getSubstation().map(Substation::getNullableCountry).orElse(null)))
                    .filter(NetworkUtil::isCorrect)
                    .collect(Collectors.toList());
            //calculate sum P of country's generators
            double totalCountryP = generators.stream().mapToDouble(NetworkUtil::pseudoTargetP).sum();
            //calculate factor of each generator
            generators.forEach(generator -> weightedSensitivityVariables.add(new WeightedSensitivityVariable(generator.getId(),
                    glskShiftKey.getQuantity().floatValue() * (float) NetworkUtil.pseudoTargetP(generator) / (float) totalCountryP)));
        } else if (glskShiftKey.getPsrType().equals("A05")) {
            //Load A05
            List<Load> loads = network.getLoadStream()
                    .filter(load -> country.equals(load.getTerminal().getVoltageLevel().getSubstation().map(Substation::getNullableCountry).orElse(null)))
                    .filter(NetworkUtil::isCorrect)
                    .collect(Collectors.toList());
            double totalCountryLoad = loads.stream().mapToDouble(NetworkUtil::pseudoP0).sum();
            loads.forEach(load -> weightedSensitivityVariables.add(new WeightedSensitivityVariable(load.getId(),
                    glskShiftKey.getQuantity().floatValue() * (float) NetworkUtil.pseudoP0(load) / (float) totalCountryLoad)));
        } else {
            //unknown PsrType
            throw new GlskException("convertCountryProportional PsrType not supported");
        }
    }

    /**
     * @param network iidm network
     * @param glskShiftKey explicit type shiftkey
     * @param weightedSensitivityVariables linearGlsk to be filled
     */
    private static void convertExplicitProportional(Network network, GlskShiftKey glskShiftKey, List<WeightedSensitivityVariable> weightedSensitivityVariables) {
        List<DanglingLine> danglingLines = glskShiftKey.getRegisteredResourceArrayList().stream()
            .map(rr -> rr.getDanglingLineId(network))
            .filter(Objects::nonNull)
            .map(network::getDanglingLine)
            .filter(NetworkUtil::isCorrect)
            .collect(Collectors.toList());
        double totalP = danglingLines.stream().mapToDouble(NetworkUtil::pseudoP0).sum();
        //Generator A04 or Load A05
        if (glskShiftKey.getPsrType().equals("A04")) {
            //Generator A04
            List<Generator> generators = glskShiftKey.getRegisteredResourceArrayList().stream()
                    .map(GlskRegisteredResource::getGeneratorId)
                    .map(network::getGenerator)
                    .filter(NetworkUtil::isCorrect)
                    .collect(Collectors.toList());
            totalP += generators.stream().mapToDouble(NetworkUtil::pseudoTargetP).sum();
            double finalTotalP = totalP;
            generators.forEach(generator -> weightedSensitivityVariables.add(new WeightedSensitivityVariable(generator.getId(),
                    glskShiftKey.getQuantity().floatValue() * (float) NetworkUtil.pseudoTargetP(generator) / (float) finalTotalP)));
        } else if (glskShiftKey.getPsrType().equals("A05")) {
            //Load A05
            List<Load> loads = glskShiftKey.getRegisteredResourceArrayList().stream()
                    .map(GlskRegisteredResource::getLoadId)
                    .map(network::getLoad)
                    .filter(NetworkUtil::isCorrect)
                    .collect(Collectors.toList());
            totalP += loads.stream().mapToDouble(NetworkUtil::pseudoP0).sum();
            double finalTotalP = totalP;
            loads.forEach(load -> weightedSensitivityVariables.add(new WeightedSensitivityVariable(load.getId(),
                    glskShiftKey.getQuantity().floatValue() * (float) NetworkUtil.pseudoP0(load) / (float) finalTotalP)));
        } else {
            //unknown PsrType
            throw new GlskException("convertExplicitProportional PsrType not supported");
        }
        double finalTotalP = totalP;
        danglingLines.forEach(danglingLine -> weightedSensitivityVariables.add(new WeightedSensitivityVariable(danglingLine.getId(),
            glskShiftKey.getQuantity().floatValue() * (float) NetworkUtil.pseudoP0(danglingLine) / (float) finalTotalP)));
    }

    /**
     * @param network iidm network
     * @param glskShiftKey parcitipation factor type shiftkey
     * @param weightedSensitivityVariables linearGlsk to be filled
     */
    private static void convertParticipationFactor(Network network, GlskShiftKey glskShiftKey, List<WeightedSensitivityVariable> weightedSensitivityVariables) {
        //Generator A04 or Load A05
        List<GlskRegisteredResource> danglingLineResources = glskShiftKey.getRegisteredResourceArrayList().stream()
            .filter(danglingLineResource -> danglingLineResource.getDanglingLineId(network) != null &&
                NetworkUtil.isCorrect(network.getDanglingLine(danglingLineResource.getDanglingLineId(network))))
            .collect(Collectors.toList());
        double totalFactor = danglingLineResources.stream().mapToDouble(GlskRegisteredResource::getParticipationFactor).sum();
        if (glskShiftKey.getPsrType().equals("A04")) {
            //Generator A04
            List<GlskRegisteredResource> generatorResources = glskShiftKey.getRegisteredResourceArrayList().stream()
                .filter(generatorResource -> NetworkUtil.isCorrect(network.getGenerator(generatorResource.getGeneratorId())))
                .collect(Collectors.toList());
            totalFactor += generatorResources.stream().mapToDouble(GlskRegisteredResource::getParticipationFactor).sum();
            if (totalFactor < 1e-10) {
                throw new GlskException("total factor is zero");
            }
            double finalTotalFactor2 = totalFactor;
            generatorResources.forEach(generatorResource -> weightedSensitivityVariables.add(new WeightedSensitivityVariable(generatorResource.getGeneratorId(),
                glskShiftKey.getQuantity().floatValue() * (float) generatorResource.getParticipationFactor() / (float) finalTotalFactor2)));
        } else if (glskShiftKey.getPsrType().equals("A05")) {
            //Load A05
            List<GlskRegisteredResource> loadResources = glskShiftKey.getRegisteredResourceArrayList().stream()
                .filter(loadResource ->
                    loadResource.getmRID().contains("XLI_OB1") ||
                        NetworkUtil.isCorrect(network.getLoad(loadResource.getLoadId())))
                .collect(Collectors.toList());
            totalFactor += loadResources.stream().mapToDouble(GlskRegisteredResource::getParticipationFactor).sum();
            if (totalFactor < 1e-10) {
                throw new GlskException("total factor is zero");
            }
            double finalTotalFactor = totalFactor;
            loadResources.forEach(loadResource -> weightedSensitivityVariables.add(new WeightedSensitivityVariable(loadResource.getLoadId(),
                glskShiftKey.getQuantity().floatValue() * (float) loadResource.getParticipationFactor() / (float) finalTotalFactor)));
        } else {
            //unknown PsrType
            throw new GlskException("convertParticipationFactor PsrType not supported");
        }
        double finalTotalFactor = totalFactor;
        danglingLineResources.forEach(danglingLineResource -> weightedSensitivityVariables.add(new WeightedSensitivityVariable(danglingLineResource.getDanglingLineId(network),
            glskShiftKey.getQuantity().floatValue() * (float) danglingLineResource.getParticipationFactor() / (float) finalTotalFactor)));
    }
}
