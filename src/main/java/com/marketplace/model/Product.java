package com.marketplace.model;

import java.time.Instant;
import java.util.Objects;

// Модель товара
public class Product {
    private final long id;
    private String name;
    private String brand;
    private String category;
    private double price;
    private final Instant createdAt;
    private Instant updatedAt;

    // Конструктор для создания нового товара
    public Product(long id, String name, String brand, String category, double price) {
        this.id = id;
        this.name = name;
        this.brand = brand;
        this.category = category;
        this.price = price;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public long getId() { return id; }
    public String getName() { return name; }
    public String getBrand() { return brand; }
    public String getCategory() { return category; }
    public double getPrice() { return price; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void setName(String name) { this.name = name; touch(); }
    public void setBrand(String brand) { this.brand = brand; touch(); }
    public void setCategory(String category) { this.category = category; touch(); }
    public void setPrice(double price) { this.price = price; touch(); }

    private void touch() {
        this.updatedAt = Instant.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Product product = (Product) o;
        return Objects.equals(id, product.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return String.format("[%s] %s | %s | %s | %.2f", id, name, brand, category, price);
    }
}
