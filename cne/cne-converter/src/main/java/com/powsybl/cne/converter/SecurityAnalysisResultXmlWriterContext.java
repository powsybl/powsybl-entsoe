/**
 * Copyright (c) 2021, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.cne.converter;

import com.powsybl.cne.model.ContingencySeries;
import com.powsybl.cne.model.MonitoredRegisteredResource;
import com.powsybl.commons.xml.XmlWriterContext;
import com.powsybl.security.LimitViolation;
import com.powsybl.security.LimitViolationType;
import com.powsybl.security.PostContingencyResult;
import com.powsybl.security.SecurityAnalysisResult;

import javax.xml.stream.XMLStreamWriter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Thomas Adam <tadam at silicom.fr>
 */
class SecurityAnalysisResultXmlWriterContext implements XmlWriterContext {

    private static final String MISSING_SUFFIX = " is missing";

    private final XMLStreamWriter writer;

    private final Properties parameters;

    private final List<MonitoredRegisteredResource> preMonitoredRegisteredResources = new ArrayList<>();

    private final Map<ContingencySeries, List<MonitoredRegisteredResource>> postMonitoredRegisteredResources = new HashMap<>();

    SecurityAnalysisResultXmlWriterContext(SecurityAnalysisResult result, Properties parameters, XMLStreamWriter writer) {
        this.writer = writer;
        this.parameters = checkRequiredParameters(parameters);
        // PreContingencyResult
        preMonitoredRegisteredResources.addAll(convert(result.getPreContingencyResult().getLimitViolations()));
        // PostContingencyResult
        Collection<PostContingencyResult> postResults = result.getPostContingencyResults();
        postResults.forEach(p -> postMonitoredRegisteredResources.put(new ContingencySeries(p.getContingency()), convert(p.getLimitViolationsResult().getLimitViolations())));
    }

    private List<MonitoredRegisteredResource> convert(List<LimitViolation> limitViolations) {
        final List<MonitoredRegisteredResource> monitoredRegisteredResources = new ArrayList<>();
        // Remove LimitViolationType.OTHER
        Collection<LimitViolation> lvFiltered = limitViolations.stream().filter(l -> l.getLimitType() != LimitViolationType.OTHER).collect(Collectors.toList());
        // List all equipment id involved
        Collection<String> equipmentIds = lvFiltered.stream().map(LimitViolation::getSubjectId).collect(Collectors.toList());
        // Build MonitoredRegisteredResources list
        equipmentIds.forEach(id -> {
            Collection<LimitViolation> lv = lvFiltered.stream().filter(l -> l.getSubjectId().compareTo(id) == 0).collect(Collectors.toList());
            monitoredRegisteredResources.add(new MonitoredRegisteredResource(lv));
        });
        return monitoredRegisteredResources;
    }

    private Properties checkRequiredParameters(Properties parameters) {
        Objects.requireNonNull(parameters.getProperty(CneConstants.MRID), CneConstants.MRID + MISSING_SUFFIX);
        Objects.requireNonNull(parameters.getProperty(CneConstants.SENDER_MARKET_PARTICIPANT_MRID), CneConstants.SENDER_MARKET_PARTICIPANT_MRID + MISSING_SUFFIX);
        Objects.requireNonNull(parameters.getProperty(CneConstants.RECEIVER_MARKET_PARTICIPANT_MRID), CneConstants.RECEIVER_MARKET_PARTICIPANT_MRID + MISSING_SUFFIX);
        Objects.requireNonNull(parameters.getProperty(CneConstants.TIME_SERIES_MRID), CneConstants.TIME_SERIES_MRID + MISSING_SUFFIX);
        Objects.requireNonNull(parameters.getProperty(CneConstants.IN_DOMAIN_MRID), CneConstants.IN_DOMAIN_MRID + MISSING_SUFFIX);
        Objects.requireNonNull(parameters.getProperty(CneConstants.OUT_DOMAIN_MRID), CneConstants.OUT_DOMAIN_MRID + MISSING_SUFFIX);
        return parameters;
    }

    @Override
    public XMLStreamWriter getWriter() {
        return writer;
    }

    Properties getParameters() {
        return parameters;
    }

    List<MonitoredRegisteredResource> getPreMonitoredRegisteredResources() {
        return preMonitoredRegisteredResources;
    }

    Map<ContingencySeries, List<MonitoredRegisteredResource>> getPostMonitoredRegisteredResources() {
        return postMonitoredRegisteredResources;
    }
}
