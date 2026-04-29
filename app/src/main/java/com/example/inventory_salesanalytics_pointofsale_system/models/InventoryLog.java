package com.example.inventory_salesanalytics_pointofsale_system.models;

public class InventoryLog {
    private int id;
    private String itemName;
    private String type;
    private double quantity;
    private String reason;
    private String date;

    public InventoryLog(int id, String itemName, String type, double quantity, String reason, String date) {
        this.id = id;
        this.itemName = itemName;
        this.type = type;
        this.quantity = quantity;
        this.reason = reason;
        this.date = date;
    }

    public int getId() { return id; }
    public String getItemName() { return itemName; }
    public String getType() { return type; }
    public double getQuantity() { return quantity; }
    public String getReason() { return reason; }
    public String getDate() { return date; }
}
