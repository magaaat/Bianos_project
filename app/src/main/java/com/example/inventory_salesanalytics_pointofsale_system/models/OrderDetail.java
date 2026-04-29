package com.example.inventory_salesanalytics_pointofsale_system.models;

public class OrderDetail {
    private int id;
    private int saleId;
    private int productId;
    private int quantity;
    private double subtotal;

    public OrderDetail(int id, int saleId, int productId, int quantity, double subtotal) {
        this.id = id;
        this.saleId = saleId;
        this.productId = productId;
        this.quantity = quantity;
        this.subtotal = subtotal;
    }

    public int getId() { return id; }
    public int getSaleId() { return saleId; }
    public int getProductId() { return productId; }
    public int getQuantity() { return quantity; }
    public double getSubtotal() { return subtotal; }
}
