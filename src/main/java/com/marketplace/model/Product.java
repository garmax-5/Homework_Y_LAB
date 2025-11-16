package com.marketplace.model;

import java.time.Instant;
import java.util.Objects;

// Модель товара
public class Product {
    private Long id;
    private String name;
    private String brand;
    private String category;
    private double price;
    private final Instant createdAt;
    private Instant updatedAt;

    // Конструктор для нового продукта
    public Product(String name, String brand, String category, double price) {
        this.id = null;
        this.createdAt = null;
        this.updatedAt = null;
        this.name = name;
        this.brand = brand;
        this.category = category;
        this.price = price;
    }

    // Конструктор для загрузки из БД
    public Product(Long id, String name, String brand, String category, double price,
                   Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.name = name;
        this.brand = brand;
        this.category = category;
        this.price = price;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getBrand() { return brand; }
    public String getCategory() { return category; }
    public double getPrice() { return price; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void setId(Long id) {this.id = id;}
    public void setName(String name) { this.name = name; }
    public void setBrand(String brand) { this.brand = brand; }
    public void setCategory(String category) { this.category = category; }
    public void setPrice(double price) { this.price = price; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Product)) return false;
        Product product = (Product) o;
        return Objects.equals(id, product.id);
    }


    @Override
    public int hashCode() {
        return Long.hashCode(id);
    }

    @Override
    public String toString() {
        return String.format("[%s] %s | %s | %s | %.2f", id, name, brand, category, price);
    }
}
