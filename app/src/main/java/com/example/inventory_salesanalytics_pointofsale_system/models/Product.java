package com.example.inventory_salesanalytics_pointofsale_system.models;

import java.util.Objects;

public class Product {
    private int id;
    private String name;
    private String category;
    private double price;
    private String size;
    private String imageUrl; // New field for Glide

    public Product(int id, String name, String category, double price, String size) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.price = price;
        this.size = size;
        this.imageUrl = ""; // Default empty
    }

    public Product(int id, String name, String category, double price, String size, String imageUrl) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.price = price;
        this.size = size;
        this.imageUrl = imageUrl;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public String getCategory() { return category; }
    public double getPrice() { return price; }
    public String getSize() { return size; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Product product = (Product) o;
        // Compare by name, size and category to ensure uniqueness in CartItem comparison, 
        // especially for variants that might share IDs in some logic or addons.
        return Objects.equals(name, product.name) &&
               Objects.equals(size, product.size) &&
               Objects.equals(category, product.category);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, size, category);
    }
}
