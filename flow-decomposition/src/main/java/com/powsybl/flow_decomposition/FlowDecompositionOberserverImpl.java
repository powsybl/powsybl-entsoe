package com.powsybl.flow_decomposition;

import com.powsybl.iidm.network.Country;
import org.apache.commons.lang3.tuple.Pair;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * FlowDecompositionOberserverImpl gathers all observed events from the flow decomposition. It keeps the events occuring, and the
 * matrices
 */
public class FlowDecompositionOberserverImpl implements FlowDecompositionObserver {

    public enum Event {
        RUN_START,
        RUN_DONE,
        COMPUTING_BASE_CASE,
        COMPUTING_CONTINGENCY,
        COMPUTED_GLSK,
        COMPUTED_NET_POSITIONS,
        COMPUTED_NODAL_INJECTIONS_MATRIX,
        COMPUTED_PTDF_MATRIX,
        COMPUTED_PSDF_MATRIX,
        COMPUTED_AC_NODAL_INJECTIONS,
        COMPUTED_DC_NODAL_INJECTIONS,
        COMPUTED_AC_FLOWS,
        COMPUTED_DC_FLOWS
    }
    public static final String BASE_CASE = "base-case";
    private List<Event> events = new LinkedList<>();
    private String currentContingency = null;
    private ContingencyValue<List<Event>> eventsPerContingency = new ContingencyValue<>();
    private Map<Country, Map<String, Double>> glsks;
    private Map<Country, Double> netPositions;
    private ContingencyValue<Map<String, Map<String, Double>>> nodalInjections = new ContingencyValue<>();
    private ContingencyValue<Map<String, Map<String, Double>>> ptdfs = new ContingencyValue<>();
    private ContingencyValue<Map<String, Map<String, Double>>> psdfs = new ContingencyValue<>();
    private ContingencyValue<Map<String, Double>> acNodalInjections = new ContingencyValue<>();
    private ContingencyValue<Map<String, Double>> dcNodalInjections = new ContingencyValue<>();
    private ContingencyValue<Map<String, Pair<Double, Double>>> acFlows = new ContingencyValue<>();
    private ContingencyValue<Map<String, Pair<Double, Double>>> dcFlows = new ContingencyValue<>();

    public List<Event> allEvents() {
        return events;
    }

    public List<Event> eventsForBaseCase() {
        return eventsPerContingency.forBaseCase();
    }

    public List<Event> eventsForContingency(String contingencyId) {
        return eventsPerContingency.forContingency(contingencyId);
    }

    @Override
    public void runStart() {
        addEvent(Event.RUN_START);
    }

    @Override
    public void runDone() {
        addEvent(Event.RUN_DONE);
    }

    @Override
    public void computingBaseCase() {
        currentContingency = BASE_CASE;
        addEvent(Event.COMPUTING_BASE_CASE);
    }

    @Override
    public void computingContingency(String contingencyId) {
        currentContingency = contingencyId;
        addEvent(Event.COMPUTING_CONTINGENCY);
    }

    @Override
    public void computedGlsk(Map<Country, Map<String, Double>> glsks) {
        addEvent(Event.COMPUTED_GLSK);
        this.glsks = glsks;
    }

    @Override
    public void computedNetPositions(Map<Country, Double> netPositions) {
        addEvent(Event.COMPUTED_NET_POSITIONS);
        this.netPositions = netPositions;
    }

    @Override
    public void computedNodalInjectionsMatrix(Map<String, Map<String, Double>> nodalInjections) {
        addEvent(Event.COMPUTED_NODAL_INJECTIONS_MATRIX);
        this.nodalInjections.put(currentContingency, nodalInjections);
    }

    @Override
    public void computedPtdfMatrix(Map<String, Map<String, Double>> pdtfMatrix) {
        addEvent(Event.COMPUTED_PTDF_MATRIX);
        this.ptdfs.put(currentContingency, pdtfMatrix);
    }

    @Override
    public void computedPsdfMatrix(Map<String, Map<String, Double>> psdfMatrix) {
        addEvent(Event.COMPUTED_PSDF_MATRIX);
        this.psdfs.put(currentContingency, psdfMatrix);
    }

    @Override
    public void computedAcNodalInjections(Map<String, Double> positions, boolean fallbackHasBeenActivated) {
        addEvent(Event.COMPUTED_AC_NODAL_INJECTIONS);
        this.acNodalInjections.put(currentContingency, positions);
    }

    @Override
    public void computedDcNodalInjections(Map<String, Double> positions) {
        addEvent(Event.COMPUTED_DC_NODAL_INJECTIONS);
        this.dcNodalInjections.put(currentContingency, positions);
    }

    @Override
    public void computedAcFlows(Map<String, Pair<Double, Double>> flows) {
        addEvent(Event.COMPUTED_AC_FLOWS);
        this.acFlows.put(currentContingency, flows);
    }

    @Override
    public void computedDcFlows(Map<String, Pair<Double, Double>> flows) {
        addEvent(Event.COMPUTED_DC_FLOWS);
        this.dcFlows.put(currentContingency, flows);
    }

    private void addEvent(Event e) {
        if (currentContingency != null) {
            eventsPerContingency.putIfAbsent(currentContingency, new LinkedList<>());
            eventsPerContingency.forContingency(currentContingency).add(e);
        }
        events.add(e);
    }

    public Map<Country, Map<String, Double>> getGlsks() {
        return glsks;
    }

    public Map<Country, Double> getNetPositions() {
        return netPositions;
    }

    public ContingencyValue<Map<String, Map<String, Double>>> getNodalInjections() {
        return nodalInjections;
    }

    public ContingencyValue<Map<String, Double>> getAcNodalInjections() {
        return acNodalInjections;
    }

    public ContingencyValue<Map<String, Double>> getDcNodalInjections() {
        return dcNodalInjections;
    }

    public ContingencyValue<Map<String, Map<String, Double>>> getPtdfs() {
        return ptdfs;
    }

    public ContingencyValue<Map<String, Map<String, Double>>> getPsdfs() {
        return psdfs;
    }

    public ContingencyValue<Map<String, Pair<Double, Double>>> getAcFlows() {
        return acFlows;
    }

    public ContingencyValue<Map<String, Pair<Double, Double>>> getDcFlows() {
        return dcFlows;
    }

    public static final class ContingencyValue<T> {
        private Map<String, T> values = new HashMap<>();

        public void put(String contingencyId, T value) {
            values.put(contingencyId, value);
        }

        public void putIfAbsent(String contingencyId, T value) {
            values.putIfAbsent(contingencyId, value);
        }

        public T forContingency(String contingencyId) {
            return values.get(contingencyId);
        }

        public T forBaseCase() {
            return values.get(BASE_CASE);
        }
    }
}
