package com.powsybl.glsk.cse;

import xsd.etso_core_cmpts.QuantityType;

import java.math.BigDecimal;
import java.util.Optional;

public final class NodeWrapper {
    private final Object node;

    public NodeWrapper(Object node) {
        this.node = node;
    }

    public Object getNode() {
        return node;
    }

    public Optional<String> getName() {
        try {
            NodeNameType name = (NodeNameType) node.getClass().getDeclaredField("name").get(node);
            return Optional.ofNullable(name).map(NodeNameType::getV);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            return Optional.empty();
        }
    }

    public Optional<BigDecimal> getFactor() {
        try {
            QuantityType factor = (QuantityType) node.getClass().getDeclaredField("factor").get(node);
            return Optional.ofNullable(factor).map(QuantityType::getV);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            return Optional.empty();
        }
    }

    public Optional<BigDecimal> getPmin() {
        try {
            QuantityType pmin = (QuantityType) node.getClass().getDeclaredField("pmin").get(node);
            return Optional.ofNullable(pmin).map(QuantityType::getV);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            return Optional.empty();
        }
    }

    public Optional<BigDecimal> getPmax() {
        try {
            QuantityType pmax = (QuantityType) node.getClass().getDeclaredField("pmax").get(node);
            return Optional.ofNullable(pmax).map(QuantityType::getV);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            return Optional.empty();
        }
    }
}
