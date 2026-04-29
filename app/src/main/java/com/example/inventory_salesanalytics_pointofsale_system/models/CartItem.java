package com.example.inventory_salesanalytics_pointofsale_system.models;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class CartItem {
    private Product product;
    private int quantity;
    private List<Product> selectedAddons;

    public CartItem(Product product, int quantity) {
        this.product = product;
        this.quantity = quantity;
        this.selectedAddons = new ArrayList<>();
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public List<Product> getSelectedAddons() {
        return selectedAddons;
    }

    public void setSelectedAddons(List<Product> selectedAddons) {
        this.selectedAddons = selectedAddons;
    }

    public void addAddon(Product addon) {
        this.selectedAddons.add(addon);
    }

    public double getTotalPrice() {
        double total = product.getPrice();
        for (Product addon : selectedAddons) {
            total += addon.getPrice();
        }
        return total * quantity;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CartItem cartItem = (CartItem) o;
        return Objects.equals(product, cartItem.product) && 
               Objects.equals(selectedAddons, cartItem.selectedAddons);
    }

    @Override
    public int hashCode() {
        return Objects.hash(product, selectedAddons);
    }
}
