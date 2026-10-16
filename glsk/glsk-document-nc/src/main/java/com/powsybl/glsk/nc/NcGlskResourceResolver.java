/*
 * Copyright (c) 2026, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package com.powsybl.glsk.nc;

import com.powsybl.glsk.api.GlskPoint;
import com.powsybl.glsk.api.GlskRegisteredResource;
import com.powsybl.glsk.api.GlskShiftKey;
import com.powsybl.iidm.network.Generator;
import com.powsybl.iidm.network.Injection;
import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.Terminal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Resolves the resources of a merit order against a network.
 * <p>
 * The resources of the SIS profile are identified by the mRID of their CGMES generating unit, which is not necessarily
 * the id of the network element that has to be scaled: a resource is looked up by id (generator, then load) and, if
 * there is none, as the generating unit of a generator (see {@link NcConstants#PROPERTY_GENERATING_UNIT}).
 * <p>
 * A step of the merit order whose resource cannot be resolved to a unique element that is in service (connected, and in
 * the main synchronous component) is ignored, with a warning, and the other steps are kept.
 *
 * @author Víctor Cardozo {@literal <victor.cardozo at artelys.com>}
 */
final class NcGlskResourceResolver {

    private static final Logger LOGGER = LoggerFactory.getLogger(NcGlskResourceResolver.class);
    private static final String MERIT_ORDER_BUSINESS_TYPE = "B45";

    private NcGlskResourceResolver() {
    }

    /**
     * @param network the network the GLSK point will be applied to
     * @param point   a GLSK point of the NC document
     * @return the point with the resources of its merit order replaced by the id of the network elements they stand for,
     * and without the steps that cannot be resolved. A point that is not a merit order is returned as it is. The result
     * is empty if the point is a merit order none of whose steps can be resolved.
     */
    static Optional<GlskPoint> resolve(Network network, GlskPoint point) {
        if (point.getGlskShiftKeys().stream().noneMatch(shiftKey -> MERIT_ORDER_BUSINESS_TYPE.equals(shiftKey.getBusinessType()))) {
            return Optional.of(point);
        }

        Lookup lookup = new Lookup(network, point.getSubjectDomainmRID());
        List<NcGlskShiftKey> resolvedSteps = new ArrayList<>();
        for (GlskShiftKey shiftKey : point.getGlskShiftKeys()) {
            GlskRegisteredResource resource = shiftKey.getRegisteredResourceArrayList().getFirst();
            lookup.find(resource.getmRID()).ifPresent(injection ->
                resolvedSteps.add(NcGlskShiftKey.meritOrderStep(point.getPointInterval(), point.getSubjectDomainmRID(), shiftKey.getMeritOrderPosition(),
                    new NcGlskRegisteredResource(injection.getId(), resource.getParticipationFactor(),
                        resource.getMinimumCapacity().orElse(null), resource.getMaximumCapacity().orElse(null)))));
        }
        return resolvedSteps.isEmpty() ? Optional.empty()
            : Optional.of(new NcGlskPoint(point.getSubjectDomainmRID(), point.getPointInterval(), resolvedSteps));
    }

    /**
     * Looks the resources up in a network. The generators are indexed by generating unit only if it is needed.
     */
    private static final class Lookup {
        private final Network network;
        private final String zone;
        private Map<String, List<Generator>> generatorsByGeneratingUnit;

        private Lookup(Network network, String zone) {
            this.network = network;
            this.zone = zone;
        }

        private Optional<Injection<?>> find(String resourceMrid) {
            Injection<?> injection = network.getGenerator(resourceMrid);
            if (injection == null) {
                injection = network.getLoad(resourceMrid);
            }
            if (injection == null) {
                List<Generator> generators = generatorsByGeneratingUnit().getOrDefault(resourceMrid, List.of());
                if (generators.size() > 1) {
                    return ignored(resourceMrid, "it is the generating unit of several generators");
                }
                injection = generators.isEmpty() ? null : generators.getFirst();
            }
            if (injection == null) {
                return ignored(resourceMrid, "it was not found in the network");
            }
            if (!isInService(injection)) {
                return ignored(resourceMrid, "it is not connected to the main synchronous component");
            }
            return Optional.of(injection);
        }

        private Map<String, List<Generator>> generatorsByGeneratingUnit() {
            if (generatorsByGeneratingUnit == null) {
                generatorsByGeneratingUnit = network.getGeneratorStream()
                    .filter(generator -> generator.hasProperty(NcConstants.PROPERTY_GENERATING_UNIT))
                    .collect(Collectors.groupingBy(generator -> generator.getProperty(NcConstants.PROPERTY_GENERATING_UNIT)));
            }
            return generatorsByGeneratingUnit;
        }

        private static boolean isInService(Injection<?> injection) {
            Terminal terminal = injection.getTerminal();
            return terminal.isConnected()
                && terminal.getBusView().getBus() != null
                && terminal.getBusView().getBus().isInMainSynchronousComponent();
        }

        private Optional<Injection<?>> ignored(String resourceMrid, String reason) {
            LOGGER.warn("Resource {} of the merit order of zone {} is ignored: {}", resourceMrid, zone, reason);
            return Optional.empty();
        }
    }
}
