package com.example.inventory_salesanalytics_pointofsale_system.models;

public class SaleOrder {
    private int id;
    private String customerName;
    private double totalAmount;
    private String saleDate;
    private String orderSummary; // New field to hold the list of items

    public SaleOrder(int id, String customerName, double totalAmount, String saleDate, String orderSummary) {
        this.id = id;
        this.customerName = customerName;
        this.totalAmount = totalAmount;
        this.saleDate = saleDate;
        this.orderSummary = orderSummary;
    }

    public int getId() { return id; }
    public String getCustomerName() { return customerName; }
    public double getTotalAmount() { return totalAmount; }
    public String getSaleDate() { return saleDate; }
    public String getOrderSummary() { return orderSummary; }
}
