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
import com.powsybl.iidm.modification.scalable.Scalable;
import com.powsybl.iidm.network.Country;
import com.powsybl.iidm.network.DanglingLine;
import com.powsybl.iidm.network.Generator;
import com.powsybl.iidm.network.Load;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.Substation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Convert a single GlskPoint to Scalable
 *
 * @author Pengbo Wang {@literal <pengbo.wang@rte-international.com>}
 */
public final class GlskPointScalableConverter {
    private static final Logger LOGGER = LoggerFactory.getLogger(GlskPointScalableConverter.class);

    private GlskPointScalableConverter() {
        throw new AssertionError("Utility class should not be instantiated");
    }

    /**
     * @param network   IIDM network
     * @param glskPoint GLSK Point
     * @return powsybl-core Scalable
     */
    public static Scalable convert(Network network, GlskPoint glskPoint) {
        Objects.requireNonNull(glskPoint.getGlskShiftKeys());
        if (!glskPoint.getGlskShiftKeys().get(0).getBusinessType().equals("B45")) {
            return convert(network, glskPoint.getGlskShiftKeys());
        } else {
            //B45 merit order
            return convertMeritOrder(network, glskPoint);
        }
    }

    public static Scalable convert(Network network, List<GlskShiftKey> shiftKeys) {
        Objects.requireNonNull(shiftKeys);

        List<Scalable> shiftKeyScalables = new ArrayList<>();
        List<Double> shiftKeyPercentages = new ArrayList<>();

        for (GlskShiftKey glskShiftKey : shiftKeys) {
            List<Double> percentages = new ArrayList<>();
            List<Scalable> scalables = new ArrayList<>();
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
            Double percentageSum = percentages.stream().mapToDouble(Double::doubleValue).sum();
            shiftKeyPercentages.add(percentageSum);

            // each scalable needs to have a sum of percentages equal to 100%
            List<Double> normalizedPercentages = percentages.stream().map(p -> p * 100 / percentageSum).toList();
            // the limit of the scalables is the power they can reach, not how much they can be scaled by
            Double currentPower = scalables.stream().mapToDouble(scalable -> scalable.getSteadyStatePower(network, 1, Scalable.ScalingConvention.GENERATOR)).sum();
            shiftKeyScalables.add(Scalable.proportional(normalizedPercentages, scalables, -Double.MAX_VALUE, currentPower + glskShiftKey.getMaximumShift()));
        }

        return Scalable.proportional(shiftKeyPercentages, shiftKeyScalables); // iterative must be set in scalingParameters during scale
    }

    private static void convertRemainingCapacity(Network network, GlskShiftKey glskShiftKey, List<Double> percentages, List<Scalable> scalables) {
        LOGGER.debug("GLSK Type B44, not empty registered resources list --> remaining capacity proportional GSK");
        // Remaining capacity algorithm is supposed to put all generators at Pmin at the same time when decreasing
        // generation, and to put all generators at Pmax at the same time when increasing generation.
        // Though the scaling is not symmetrical.
        List<GlskRegisteredResource> generatorResources = glskShiftKey.getRegisteredResourceArrayList().stream()
                .filter(generatorResource -> NetworkUtil.isCorrect(network.getGenerator(generatorResource.getGeneratorId())))
                .toList();

        Scalable upScalable = createRemainingCapacityScalable(network, glskShiftKey, generatorResources, GlskPointScalableConverter::getRemainingCapacityUp);
        Scalable downScalable = createRemainingCapacityScalable(network, glskShiftKey, generatorResources, GlskPointScalableConverter::getRemainingCapacityDown);
        percentages.add(100.0);
        scalables.add(Scalable.upDown(upScalable, downScalable));
    }

    private static Scalable createRemainingCapacityScalable(Network network, GlskShiftKey glskShiftKey, List<GlskRegisteredResource> generatorResources, BiFunction<GlskRegisteredResource, Network, Double> remainingCapacityFunction) {
        List<Double> percentages = new ArrayList<>();
        List<Scalable> scalables = new ArrayList<>();
        double totalFactor = generatorResources.stream().mapToDouble(resource -> remainingCapacityFunction.apply(resource, network)).sum();
        generatorResources.forEach(generatorResource -> {
            double generatorPercentage = 100.0 * glskShiftKey.getQuantity() * remainingCapacityFunction.apply(generatorResource, network) / totalFactor;
            if (!Double.isNaN(generatorPercentage)) {
                percentages.add(generatorPercentage);
                scalables.add(getGeneratorScalableWithLimits(network, generatorResource));
            }
        });
        return Scalable.proportional(percentages, scalables); // iterative must be set in scalingParameters during scale
    }

    private static double getRemainingCapacityUp(GlskRegisteredResource resource, Network network) {
        Generator generator = network.getGenerator(resource.getGeneratorId());
        double maxP = Math.min(resource.getMaximumCapacity().orElse(generator.getMaxP()), generator.getMaxP());
        return Math.max(0., maxP - NetworkUtil.pseudoTargetP(generator));
    }

    private static double getRemainingCapacityDown(GlskRegisteredResource resource, Network network) {
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
    private static Scalable convertMeritOrder(Network network, GlskPoint glskPoint) {
        Objects.requireNonNull(network);

        Scalable upScalable = Scalable.stack(glskPoint.getGlskShiftKeys().stream()
                .filter(glskShiftKey -> glskShiftKey.getMeritOrderPosition() > 0)
                .sorted(Comparator.comparingInt(GlskShiftKey::getMeritOrderPosition))
                .map(getGlskShiftKeyScalableFunction(network)).toArray(Scalable[]::new));

        Scalable downScalable = Scalable.stack(glskPoint.getGlskShiftKeys().stream()
                .filter(glskShiftKey -> glskShiftKey.getMeritOrderPosition() < 0)
                .sorted(Comparator.comparingInt(GlskShiftKey::getMeritOrderPosition).reversed())
                .map(getGlskShiftKeyScalableFunction(network)).toArray(Scalable[]::new));
        return Scalable.upDown(upScalable, downScalable);
    }

    private static Function<GlskShiftKey, Scalable> getGlskShiftKeyScalableFunction(Network network) {
        return glskShiftKey -> {
            GlskRegisteredResource resource = Objects.requireNonNull(glskShiftKey.getRegisteredResourceArrayList()).get(0);
            if (isGenerator(network, resource)) {
                return getGeneratorScalableWithLimits(network, resource);
            } else if (isLoad(network, resource)) {
                return getLoadScalableWithLimits(network, resource);
            } else {
                return getDanglingLineScalableWithLimits(network, resource);
            }
        };
    }

    /**
     * convert country proportional glsk point to scalable
     * @param network iidm network
     * @param glskShiftKey shift key
     * @param percentages list of percentage factor of scalable
     * @param scalables list of scalable
     */
    private static void convertCountryProportional(Network network, GlskShiftKey glskShiftKey, List<Double> percentages, List<Scalable> scalables) {
        Country country = new CountryEICode(glskShiftKey.getSubjectDomainmRID()).getCountry();

        if (glskShiftKey.getPsrType().equals("A04")) {
            LOGGER.debug("GLSK Type B42, empty registered resources list --> country (proportional) GSK");
            List<Generator> generators = network.getGeneratorStream()
                    .filter(generator -> country.equals(getSubstationNullableCountry(generator.getTerminal().getVoltageLevel().getSubstation())))
                    .filter(NetworkUtil::isCorrect)
                    .toList();
            //calculate sum P of country's generators
            double totalCountryP = generators.stream().mapToDouble(NetworkUtil::pseudoTargetP).sum();
            //calculate factor of each generator
            generators.forEach(generator -> {
                double generatorPercentage = 100.0 * glskShiftKey.getQuantity() * NetworkUtil.pseudoTargetP(generator) / totalCountryP;
                percentages.add(generatorPercentage);
                scalables.add(Scalable.onGenerator(generator.getId()));
            });
        } else if (glskShiftKey.getPsrType().equals("A05")) {
            LOGGER.debug("GLSK Type B42, empty registered resources list --> country (proportional) LSK");
            List<Load> loads = network.getLoadStream()
                    .filter(load -> country.equals(getSubstationNullableCountry(load.getTerminal().getVoltageLevel().getSubstation())))
                    .filter(NetworkUtil::isCorrect)
                    .toList();

            //calculate sum P of country's loads
            double totalCountryP = loads.stream().mapToDouble(NetworkUtil::pseudoP0).sum();
            loads.forEach(load -> {
                double loadPercentage = 100.0 * glskShiftKey.getQuantity() * NetworkUtil.pseudoP0(load) / totalCountryP;
                percentages.add(loadPercentage);
                scalables.add(Scalable.onLoad(load.getId(), -Double.MAX_VALUE, Double.MAX_VALUE));
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
    private static void convertExplicitProportional(Network network, GlskShiftKey glskShiftKey, List<Double> percentages, List<Scalable> scalables) {
        List<DanglingLine> danglingLines = glskShiftKey.getRegisteredResourceArrayList().stream()
            .map(rr -> rr.getDanglingLineId(network))
            .filter(Objects::nonNull)
            .map(network::getDanglingLine)
            .filter(NetworkUtil::isCorrect)
            .toList();
        double totalP = danglingLines.stream().mapToDouble(NetworkUtil::pseudoP0).sum();

        if (glskShiftKey.getPsrType().equals("A04")) {
            LOGGER.debug("GLSK Type B42, not empty registered resources list --> (explicit/manual) proportional GSK");

            List<Generator> generators = glskShiftKey.getRegisteredResourceArrayList().stream()
                    .map(GlskRegisteredResource::getGeneratorId)
                    .map(network::getGenerator)
                    .filter(NetworkUtil::isCorrect)
                    .toList();
            totalP += generators.stream().mapToDouble(NetworkUtil::pseudoTargetP).sum();

            for (Generator generator : generators) {
                // Calculate factor of each generator
                double factor = glskShiftKey.getQuantity() * NetworkUtil.pseudoTargetP(generator) / totalP;
                percentages.add(100.0 * factor);
                // In case of global shift key limitation we will limit the generator proportionally to
                // its participation in the global proportional scalable
                scalables.add(Scalable.onGenerator(generator.getId()));
            }
        } else if (glskShiftKey.getPsrType().equals("A05")) {
            LOGGER.debug("GLSK Type B42, not empty registered resources list --> (explicit/manual) proportional LSK");
            List<Load> loads = glskShiftKey.getRegisteredResourceArrayList().stream()
                    .map(GlskRegisteredResource::getLoadId)
                    .map(network::getLoad)
                    .filter(NetworkUtil::isCorrect)
                    .toList();
            totalP += loads.stream().mapToDouble(NetworkUtil::pseudoP0).sum();

            for (Load load : loads) {
                double loadPercentage = 100.0 * glskShiftKey.getQuantity() * NetworkUtil.pseudoP0(load) / totalP;
                // For now glsk shift key maximum shift is not handled for loads by lack of specification
                percentages.add(loadPercentage);
                scalables.add(Scalable.onLoad(load.getId(), -Double.MAX_VALUE, Double.MAX_VALUE));
            }
        }
        for (DanglingLine danglingLine : danglingLines) {
            double danglingLinePercentage = 100.0 * glskShiftKey.getQuantity() * NetworkUtil.pseudoP0(danglingLine) / totalP;
            // For now glsk shift key maximum shift is not handled for dangling lines by lack of specification
            percentages.add(danglingLinePercentage);
            scalables.add(Scalable.onDanglingLine(danglingLine.getId(), -Double.MAX_VALUE, Double.MAX_VALUE));
        }
    }

    /**
     * convert participation factor glsk point to scalable
     * @param network iidm network
     * @param glskShiftKey shift key
     * @param percentages list of percentage factor of scalable
     * @param scalables list of scalable
     */
    private static void convertParticipationFactor(Network network, GlskShiftKey glskShiftKey, List<Double> percentages, List<Scalable> scalables) {
        List<GlskRegisteredResource> danglingLineResources = glskShiftKey.getRegisteredResourceArrayList().stream()
            .filter(danglingLineResource -> danglingLineResource.getDanglingLineId(network) != null &&
                NetworkUtil.isCorrect(network.getDanglingLine(danglingLineResource.getDanglingLineId(network))))
            .toList();
        double totalFactor = danglingLineResources.stream().mapToDouble(GlskRegisteredResource::getParticipationFactor).sum();

        if (glskShiftKey.getPsrType().equals("A04")) {
            LOGGER.debug("GLSK Type B43 GSK");

            List<GlskRegisteredResource> generatorResources = glskShiftKey.getRegisteredResourceArrayList().stream()
                    .filter(generatorResource -> NetworkUtil.isCorrect(network.getGenerator(generatorResource.getGeneratorId())))
                    .toList();

            totalFactor += generatorResources.stream().mapToDouble(GlskRegisteredResource::getParticipationFactor).sum();

            for (GlskRegisteredResource generatorResource : generatorResources) {
                double generatorPercentage = 100.0 * glskShiftKey.getQuantity() * generatorResource.getParticipationFactor() / totalFactor;
                percentages.add(generatorPercentage);
                scalables.add(getGeneratorScalableWithLimits(network, generatorResource));
            }
        } else if (glskShiftKey.getPsrType().equals("A05")) {
            LOGGER.debug("GLSK Type B43 LSK");
            List<GlskRegisteredResource> loadResources = glskShiftKey.getRegisteredResourceArrayList().stream()
                    .filter(loadResource -> NetworkUtil.isCorrect(network.getLoad(loadResource.getLoadId())))
                    .toList();

            totalFactor += loadResources.stream().mapToDouble(GlskRegisteredResource::getParticipationFactor).sum();

            for (GlskRegisteredResource loadResource : loadResources) {
                double loadPercentage = 100.0 * glskShiftKey.getQuantity() * loadResource.getParticipationFactor() / totalFactor;
                percentages.add(loadPercentage);
                scalables.add(getLoadScalableWithLimits(network, loadResource));
            }
        }
        for (GlskRegisteredResource danglingLineResource : danglingLineResources) {
            double loadPercentage = 100.0 * glskShiftKey.getQuantity() * danglingLineResource.getParticipationFactor() / totalFactor;
            percentages.add(loadPercentage);
            scalables.add(getDanglingLineScalableWithLimits(network, danglingLineResource));
        }
    }

    private static Country getSubstationNullableCountry(Optional<Substation> substation) {
        return substation.map(Substation::getNullableCountry).orElse(null);
    }

    private static boolean isGenerator(Network network, GlskRegisteredResource glskRegisteredResource) {
        return network.getGenerator(glskRegisteredResource.getGeneratorId()) != null;
    }

    private static boolean isLoad(Network network, GlskRegisteredResource glskRegisteredResource) {
        return network.getLoad(glskRegisteredResource.getLoadId()) != null;
    }

    private static Scalable getGeneratorScalableWithLimits(Network network, GlskRegisteredResource generatorRegisteredResource) {
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

    private static Scalable getLoadScalableWithLimits(Network network, GlskRegisteredResource loadRegisteredResource) {
        String loadId = loadRegisteredResource.getLoadId();
        double incomingMaxP = loadRegisteredResource.getMaximumCapacity().orElse(Double.MAX_VALUE);
        double incomingMinP = loadRegisteredResource.getMinimumCapacity().orElse(-Double.MAX_VALUE);
        // Fixes some inconsistencies between GLSK and network that may raise an exception in
        // PowSyBl when actually scaling the network.
        // TODO: Solve this issue in PowSyBl framework.
        Load load = network.getLoad(loadId);
        if (load != null) {
            double loadP0 = load.getP0();
            if (!Double.isNaN(incomingMaxP) && incomingMaxP < loadP0) {
                LOGGER.warn("Load '{}' has initial P0 that is above GLSK max P. Extending GLSK max P from {} to {}.", loadId, incomingMaxP, loadP0);
                incomingMaxP = loadP0;
            }
            if (!Double.isNaN(incomingMinP) && incomingMinP > loadP0) {
                LOGGER.warn("Load '{}' has initial P0 that is above GLSK min P. Extending GLSK min P from {} to {}.", loadId, incomingMinP, loadP0);
                incomingMinP = loadP0;
            }
        }
        return Scalable.onLoad(loadId, incomingMinP, incomingMaxP);
    }

    private static Scalable getDanglingLineScalableWithLimits(Network network, GlskRegisteredResource danglingLineRegisteredResource) {
        String danglingLineId = danglingLineRegisteredResource.getDanglingLineId(network);
        // We have to invert min and max because dangling lines act like loads but are scaled but in generator convention
        double incomingMinP = -danglingLineRegisteredResource.getMaximumCapacity().orElse(Double.MAX_VALUE);
        double incomingMaxP = -danglingLineRegisteredResource.getMinimumCapacity().orElse(-Double.MAX_VALUE);
        // Fixes some inconsistencies between GLSK and network that may raise an exception in
        // PowSyBl when actually scaling the network.
        // TODO: Solve this issue in PowSyBl framework.
        DanglingLine danglingLine = network.getDanglingLine(danglingLineId);
        if (danglingLine != null) {
            double danglingLineP0 = danglingLine.getP0();
            if (!Double.isNaN(incomingMaxP) && incomingMaxP < danglingLineP0) {
                LOGGER.warn("Dangling line '{}' has initial P0 that is above GLSK max P. Extending GLSK max P from {} to {}.", danglingLineId, incomingMaxP, danglingLineP0);
                incomingMaxP = danglingLineP0;
            }
            if (!Double.isNaN(incomingMinP) && incomingMinP > danglingLineP0) {
                LOGGER.warn("Dangling line '{}' has initial P0 that is above GLSK min P. Extending GLSK min P from {} to {}.", danglingLineId, incomingMinP, danglingLineP0);
                incomingMinP = danglingLineP0;
            }
        }
        return Scalable.onDanglingLine(danglingLineId, incomingMinP, incomingMaxP);
    }
}
