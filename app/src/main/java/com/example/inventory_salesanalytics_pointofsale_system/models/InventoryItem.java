package com.example.inventory_salesanalytics_pointofsale_system.models;

public class InventoryItem {
    private int id;
    private String name;
    private double quantity; // Changed to double for decimal support
    private String unit;
    private int minimumStock;

    public InventoryItem(int id, String name, double quantity, String unit, int minimumStock) {
        this.id = id;
        this.name = name;
        this.quantity = quantity;
        this.unit = unit;
        this.minimumStock = minimumStock;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public double getQuantity() { return quantity; }
    public String getUnit() { return unit; }
    public int getMinimumStock() { return minimumStock; }
}
