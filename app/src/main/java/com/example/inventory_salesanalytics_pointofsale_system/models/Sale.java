package com.example.inventory_salesanalytics_pointofsale_system.models;

public class Sale {
    private int id;
    private String date;
    private double totalAmount;

    public Sale(int id, String date, double totalAmount) {
        this.id = id;
        this.date = date;
        this.totalAmount = totalAmount;
    }

    public int getId() { return id; }
    public String getDate() { return date; }
    public double getTotalAmount() { return totalAmount; }
}
