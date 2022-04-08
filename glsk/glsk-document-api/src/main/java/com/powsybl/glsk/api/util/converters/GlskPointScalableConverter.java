/*
 * Copyright (c) 2020, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.glsk.api.util.converters;

import com.powsybl.glsk.commons.CountryEICode;
import com.powsybl.glsk.api.AbstractGlskPoint;
import com.powsybl.glsk.api.AbstractGlskRegisteredResource;
import com.powsybl.glsk.api.AbstractGlskShiftKey;
import com.powsybl.glsk.commons.GlskException;
import com.powsybl.action.util.Scalable;
import com.powsybl.iidm.network.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

/**
 * Convert a single GlskPoint to Scalable
 * @author Pengbo Wang {@literal <pengbo.wang@rte-international.com>}
 */
public final class GlskPointScalableConverter {
    private static final Logger LOGGER = LoggerFactory.getLogger(GlskPointScalableConverter.class);

    private GlskPointScalableConverter() {
        throw new AssertionError("Utility class should not be instantiated");
    }

    /**
     * @param network IIDM network
     * @param glskPoint GLSK Point
     * @return powsybl-core Scalable
     */
    public static Scalable convert(Network network, AbstractGlskPoint glskPoint) {
        Objects.requireNonNull(glskPoint.getGlskShiftKeys());
        if (!glskPoint.getGlskShiftKeys().get(0).getBusinessType().equals("B45")) {
            return convert(network, glskPoint.getGlskShiftKeys());
        } else {
            //B45 merit order
            return convertMeritOrder(network, glskPoint);
        }
    }

    public static Scalable convert(Network network, List<AbstractGlskShiftKey> shiftKeys) {
        Objects.requireNonNull(shiftKeys);
        List<Float> percentages = new ArrayList<>();
        List<Scalable> scalables = new ArrayList<>();

        for (AbstractGlskShiftKey glskShiftKey : shiftKeys) {
            if (glskShiftKey.getBusinessType().equals("B42") && glskShiftKey.getRegisteredResourceArrayList().isEmpty()) {
                //B42 country
                convertCountryProportional(network, glskShiftKey, percentages, scalables);
            } else if (glskShiftKey.getBusinessType().equals("B42") && !glskShiftKey.getRegisteredResourceArrayList().isEmpty()) {
                //B42 explicit
                convertExplicitProportional(network, glskShiftKey, percentages, scalables);
            } else if (glskShiftKey.getBusinessType().equals("B43") && !glskShiftKey.getRegisteredResourceArrayList().isEmpty()) {
                //B43 participation factor
                convertParticipationFactor(network, glskShiftKey, percentages, scalables);
            } else if (glskShiftKey.getBusinessType().equals("B44") && !glskShiftKey.getRegisteredResourceArrayList().isEmpty()) {
                //B44 remaining capacity
                convertRemainingCapacity(network, glskShiftKey, percentages, scalables);
            } else {
                throw new GlskException("In convert glskShiftKey business type not supported");
            }
        }
        return Scalable.proportional(percentages, scalables, true);
    }

    private static void convertRemainingCapacity(Network network, AbstractGlskShiftKey glskShiftKey, List<Float> percentages, List<Scalable> scalables) {
        LOGGER.debug("GLSK Type B44, not empty registered resources list --> remaining capacity proportional GSK");
        // Remaining capacity algorithm is supposed to put all generators at Pmin at the same time when decreasing
        // generation, and to put all generators at Pmax at the same time when increasing generation.
        // Though the scaling is not symmetrical.
        List<AbstractGlskRegisteredResource> generatorResources = glskShiftKey.getRegisteredResourceArrayList().stream()
                .filter(generatorResource -> NetworkUtil.isCorrect(network.getGenerator(generatorResource.getGeneratorId())))
                .collect(Collectors.toList());

        Scalable upScalable = createRemainingCapacityScalable(network, glskShiftKey, generatorResources, GlskPointScalableConverter::getRemainingCapacityUp);
        Scalable downScalable = createRemainingCapacityScalable(network, glskShiftKey, generatorResources, GlskPointScalableConverter::getRemainingCapacityDown);
        percentages.add(100.f);
        scalables.add(Scalable.upDown(upScalable, downScalable));
    }

    private static Scalable createRemainingCapacityScalable(Network network, AbstractGlskShiftKey glskShiftKey, List<AbstractGlskRegisteredResource> generatorResources, BiFunction<AbstractGlskRegisteredResource, Network, Double> remainingCapacityFunction) {
        List<Float> percentages = new ArrayList<>();
        List<Scalable> scalables = new ArrayList<>();
        double totalFactor = generatorResources.stream().mapToDouble(resource -> remainingCapacityFunction.apply(resource, network)).sum();
        generatorResources.forEach(generatorResource -> {
            float generatorPercentage = (float) (100 * glskShiftKey.getQuantity().floatValue() * remainingCapacityFunction.apply(generatorResource, network) / totalFactor);
            if (!Double.isNaN(generatorPercentage)) {
                percentages.add(generatorPercentage);
                scalables.add(getGeneratorScalableWithLimits(network, generatorResource));
            }
        });
        return Scalable.proportional(percentages, scalables, true);
    }

    private static double getRemainingCapacityUp(AbstractGlskRegisteredResource resource, Network network) {
        Generator generator = network.getGenerator(resource.getGeneratorId());
        double maxP = Math.min(resource.getMaximumCapacity().orElse(generator.getMaxP()), generator.getMaxP());
        return Math.max(0., maxP - NetworkUtil.pseudoTargetP(generator));
    }

    private static double getRemainingCapacityDown(AbstractGlskRegisteredResource resource, Network network) {
        Generator generator = network.getGenerator(resource.getGeneratorId());
        double minP = Math.max(resource.getMinimumCapacity().orElse(generator.getMinP()), generator.getMinP());
        return Math.max(0, NetworkUtil.pseudoTargetP(generator) - minP);
    }

    /**
     * convert merit order glsk point to scalable
     * @param network iidm network
     * @param glskPoint glsk point merit order
     * @return stack scalable
     */
    private static Scalable convertMeritOrder(Network network, AbstractGlskPoint glskPoint) {
        Objects.requireNonNull(network);

        Scalable upScalable = Scalable.stack(glskPoint.getGlskShiftKeys().stream()
                .filter(abstractGlskShiftKey -> abstractGlskShiftKey.getMeritOrderPosition() > 0)
                .sorted(Comparator.comparingInt(AbstractGlskShiftKey::getMeritOrderPosition))
                .map(glskShiftKey -> {
                    AbstractGlskRegisteredResource generatorRegisteredResource = Objects.requireNonNull(glskShiftKey.getRegisteredResourceArrayList()).get(0);
                    return getGeneratorScalableWithLimits(network, generatorRegisteredResource);
                }).toArray(Scalable[]::new));

        Scalable downScalable = Scalable.stack(glskPoint.getGlskShiftKeys().stream()
                .filter(abstractGlskShiftKey -> abstractGlskShiftKey.getMeritOrderPosition() < 0)
                .sorted(Comparator.comparingInt(AbstractGlskShiftKey::getMeritOrderPosition).reversed())
                .map(glskShiftKey -> {
                    AbstractGlskRegisteredResource generatorRegisteredResource = Objects.requireNonNull(glskShiftKey.getRegisteredResourceArrayList()).get(0);
                    return getGeneratorScalableWithLimits(network, generatorRegisteredResource);
                }).toArray(Scalable[]::new));
        return Scalable.upDown(upScalable, downScalable);
    }

    private static Scalable getGeneratorScalableWithLimits(Network network, AbstractGlskRegisteredResource generatorRegisteredResource) {
        String generatorId = generatorRegisteredResource.getGeneratorId();
        double incomingMaxP = generatorRegisteredResource.getMaximumCapacity().orElse(Double.MAX_VALUE);
        double incomingMinP = generatorRegisteredResource.getMinimumCapacity().orElse(-Double.MAX_VALUE);
        // Fixes some inconsistencies between GLSK and network that may raise an exception in
        // PowSyBl when actually scaling the network.
        // TODO: Solve this issue in PowSyBl framework.
        Generator generator = network.getGenerator(generatorId);
        if (generator != null) {
            double generatorTargetP = generator.getTargetP();
            if (!Double.isNaN(incomingMaxP) && incomingMaxP < generatorTargetP) {
                LOGGER.warn("Generator '{}' has initial target P that is above GLSK max P. Extending GLSK max P from {} to {}.", generatorId, incomingMaxP, generatorTargetP);
                incomingMaxP = generatorTargetP;
            }
            if (!Double.isNaN(incomingMinP) && incomingMinP > generatorTargetP) {
                LOGGER.warn("Generator '{}' has initial target P that is above GLSK min P. Extending GLSK min P from {} to {}.", generatorId, incomingMinP, generatorTargetP);
                incomingMinP = generatorTargetP;
            }
        }
        return Scalable.onGenerator(generatorId, incomingMinP, incomingMaxP);
    }

    /**
     * convert country proportional glsk point to scalable
     * @param network iidm network
     * @param glskShiftKey shift key
     * @param percentages list of percentage factor of scalable
     * @param scalables list of scalable
     */
    private static void convertCountryProportional(Network network, AbstractGlskShiftKey glskShiftKey, List<Float> percentages, List<Scalable> scalables) {
        Country country = new CountryEICode(glskShiftKey.getSubjectDomainmRID()).getCountry();

        if (glskShiftKey.getPsrType().equals("A04")) {
            LOGGER.debug("GLSK Type B42, empty registered resources list --> country (proportional) GSK");
            List<Generator> generators = network.getGeneratorStream()
                    .filter(generator -> country.equals(getSubstationNullableCountry(generator.getTerminal().getVoltageLevel().getSubstation())))
                    .filter(NetworkUtil::isCorrect)
                    .collect(Collectors.toList());
            //calculate sum P of country's generators
            double totalCountryP = generators.stream().mapToDouble(NetworkUtil::pseudoTargetP).sum();
            //calculate factor of each generator
            generators.forEach(generator -> {
                float generatorPercentage = (float) (100 * glskShiftKey.getQuantity().floatValue() * NetworkUtil.pseudoTargetP(generator) / totalCountryP);
                percentages.add(generatorPercentage);
                scalables.add(Scalable.onGenerator(generator.getId()));
            });
        } else if (glskShiftKey.getPsrType().equals("A05")) {
            LOGGER.debug("GLSK Type B42, empty registered resources list --> country (proportional) LSK");
            List<Load> loads = network.getLoadStream()
                    .filter(load -> country.equals(getSubstationNullableCountry(load.getTerminal().getVoltageLevel().getSubstation())))
                    .filter(NetworkUtil::isCorrect)
                    .collect(Collectors.toList());
            //calculate sum P of country's loads
            double totalCountryP = loads.stream().mapToDouble(NetworkUtil::pseudoP0).sum();
            loads.forEach(load -> {
                float loadPercentage = (float) (100 * glskShiftKey.getQuantity().floatValue() * NetworkUtil.pseudoP0(load) / totalCountryP);
                percentages.add(loadPercentage);
                scalables.add(Scalable.onLoad(load.getId()));
            });
        }
    }

    /**
     * convert explicit glsk point to scalable
     * @param network iidm network
     * @param glskShiftKey shift key
     * @param percentages list of percentage factor of scalable
     * @param scalables list of scalable
     */
    private static void convertExplicitProportional(Network network, AbstractGlskShiftKey glskShiftKey, List<Float> percentages, List<Scalable> scalables) {
        if (glskShiftKey.getPsrType().equals("A04")) {
            LOGGER.debug("GLSK Type B42, not empty registered resources list --> (explicit/manual) proportional GSK");

            List<Generator> generators = glskShiftKey.getRegisteredResourceArrayList().stream()
                    .map(AbstractGlskRegisteredResource::getGeneratorId)
                    .map(network::getGenerator)
                    .filter(NetworkUtil::isCorrect)
                    .collect(Collectors.toList());
            double totalP = generators.stream().mapToDouble(NetworkUtil::pseudoTargetP).sum();

            generators.forEach(generator -> {
                // Calculate factor of each generator
                float factor = (float) (glskShiftKey.getQuantity().floatValue() * NetworkUtil.pseudoTargetP(generator) / totalP);
                percentages.add(100 * factor);
                // In case of global shift key limitation we will limit the generator proportionally to
                // its participation in the global proportional scalable
                double maxGeneratorValue = NetworkUtil.pseudoTargetP(generator) + factor * glskShiftKey.getMaximumShift();
                scalables.add(Scalable.onGenerator(generator.getId(), -Double.MAX_VALUE, maxGeneratorValue));
            });
        } else if (glskShiftKey.getPsrType().equals("A05")) {
            LOGGER.debug("GLSK Type B42, not empty registered resources list --> (explicit/manual) proportional LSK");
            List<Load> loads = glskShiftKey.getRegisteredResourceArrayList().stream()
                    .map(AbstractGlskRegisteredResource::getLoadId)
                    .map(network::getLoad)
                    .filter(NetworkUtil::isCorrect)
                    .collect(Collectors.toList());
            double totalP = loads.stream().mapToDouble(NetworkUtil::pseudoP0).sum();

            loads.forEach(load -> {
                float loadPercentage = (float) (100 * glskShiftKey.getQuantity().floatValue() * NetworkUtil.pseudoP0(load) / totalP);
                // For now glsk shift key maximum shift is not handled for loads by lack of specification
                percentages.add(loadPercentage);
                scalables.add(Scalable.onLoad(load.getId()));
            });
        }
    }

    /**
     * convert participation factor glsk point to scalable
     * @param network iidm network
     * @param glskShiftKey shift key
     * @param percentages list of percentage factor of scalable
     * @param scalables list of scalable
     */
    private static void convertParticipationFactor(Network network, AbstractGlskShiftKey glskShiftKey, List<Float> percentages, List<Scalable> scalables) {
        if (glskShiftKey.getPsrType().equals("A04")) {
            LOGGER.debug("GLSK Type B43 GSK");

            List<AbstractGlskRegisteredResource> generatorResources = glskShiftKey.getRegisteredResourceArrayList().stream()
                    .filter(generatorResource -> NetworkUtil.isCorrect(network.getGenerator(generatorResource.getGeneratorId())))
                    .collect(Collectors.toList());

            double totalFactor = generatorResources.stream().mapToDouble(AbstractGlskRegisteredResource::getParticipationFactor).sum();

            generatorResources.forEach(generatorResource -> {
                float generatorPercentage = (float) (100 * glskShiftKey.getQuantity().floatValue() * generatorResource.getParticipationFactor() / totalFactor);
                percentages.add(generatorPercentage);
                scalables.add(Scalable.onGenerator(generatorResource.getGeneratorId()));
            });
        } else if (glskShiftKey.getPsrType().equals("A05")) {
            LOGGER.debug("GLSK Type B43 LSK");
            List<AbstractGlskRegisteredResource> loadResources = glskShiftKey.getRegisteredResourceArrayList().stream()
                    .filter(loadResource -> NetworkUtil.isCorrect(network.getLoad(loadResource.getLoadId())))
                    .collect(Collectors.toList());

            double totalFactor = loadResources.stream().mapToDouble(AbstractGlskRegisteredResource::getParticipationFactor).sum();

            loadResources.forEach(loadResource -> {
                float loadPercentage = (float) (100 * glskShiftKey.getQuantity().floatValue() * loadResource.getParticipationFactor() / totalFactor);

                percentages.add(loadPercentage);
                scalables.add(Scalable.onLoad(loadResource.getLoadId()));
            });
        }
    }

    private static Country getSubstationNullableCountry(Optional<Substation> substation) {
        return substation.map(Substation::getNullableCountry).orElse(null);
    }
}
