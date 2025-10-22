package com.paklog.wes.orchestration.domain.valueobject;

/**
 * Types of workflows that can be orchestrated
 */
public enum WorkflowType {
    ORDER_FULFILLMENT("End-to-end order fulfillment workflow"),
    RECEIVING("Inbound receiving workflow"),
    PUTAWAY("Inventory putaway workflow"),
    PICKING("Order picking workflow"),
    PACKING("Order packing workflow"),
    SHIPPING("Outbound shipping workflow"),
    RETURNS_PROCESSING("Returns processing workflow"),
    INVENTORY_TRANSFER("Inventory transfer workflow"),
    CYCLE_COUNT("Cycle counting workflow"),
    REPLENISHMENT("Inventory replenishment workflow"),
    CROSS_DOCKING("Cross-docking workflow"),
    WAVE_PROCESSING("Wave-based processing workflow"),
    WAVELESS_PROCESSING("Continuous waveless processing workflow"),
    QUALITY_CHECK("Quality inspection workflow"),
    VALUE_ADDED_SERVICE("Value-added service workflow");

    private final String description;

    WorkflowType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public boolean supportsWaveless() {
        return this == ORDER_FULFILLMENT || this == PICKING || this == PACKING;
    }

    public boolean requiresInventory() {
        return this == ORDER_FULFILLMENT || this == PICKING || this == REPLENISHMENT;
    }
}