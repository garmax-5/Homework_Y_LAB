package com.marketplace.out.repository;

import com.marketplace.model.Product;

import java.util.*;

// Репозиторий для хранения данных о товаре
public class ProductRepositoryImpl implements ProductRepository {
    private final Map<Long, Product> productsById = new HashMap<>();
    private final Map<String, List<Product>> productsByBrand = new HashMap<>();
    private final Map<String, List<Product>> productsByCategory = new HashMap<>();

    // Нормализация ключа (чтобы поиск был нечувствительным к регистру)
    private String norm(String s) {
        return s == null ? "" : s.trim().toLowerCase();
    }

    @Override
    public Product save(Product product) {
        // Удаляем старую версию из индексов (если есть)
        Product old = productsById.get(product.getId());
        if (old != null) {
            removeFromIndex(old);
        }

        // Добавляем/обновляем основной product
        productsById.put(product.getId(), product);

        // Добавляем в индексы
        addToIndex(product);

        return product;
    }

    @Override
    public Optional<Product> findById(long id) {
        return Optional.ofNullable(productsById.get(id));
    }

    @Override
    public boolean deleteById(long id) {
        Product removed = productsById.remove(id);
        if (removed == null) return false;
        removeFromIndex(removed);
        return true;
    }

    @Override
    public List<Product> findAll() {
        return new ArrayList<>(productsById.values());
    }

    @Override
    public long count() {
        return productsById.size();
    }

    @Override
    public boolean existsById(long id) {
        return productsById.containsKey(id);
    }

    // Быстрый поиск по бренду (вернём копию списка)
    @Override
    public List<Product> findByBrand(String brand) {
        List<Product> list = productsByBrand.get(norm(brand));
        if (list == null) return new ArrayList<>();
        return new ArrayList<>(list);
    }

    // Быстрый поиск по категории (вернём копию списка)
    @Override
    public List<Product> findByCategory(String category) {
        List<Product> list = productsByCategory.get(norm(category));
        if (list == null) return new ArrayList<>();
        return new ArrayList<>(list);
    }

    @Override
    public List<Product> findByPriceRange(double min, double max) {
        List<Product> result = new ArrayList<>();
        for (Product p : productsById.values()) {
            if (p.getPrice() >= min && p.getPrice() <= max) {
                result.add(p);
            }
        }
        return result;
    }

    // Внутренние методы управления индексами
    private void addToIndex(Product p) {
        String brandKey = norm(p.getBrand());
        String catKey = norm(p.getCategory());

        List<Product> byBrand = productsByBrand.get(brandKey);
        if (byBrand == null) {
            byBrand = new ArrayList<>();
            productsByBrand.put(brandKey, byBrand);
        }
        // Чтобы избежать дубликата в списке (на случай повторного save)
        if (!byBrand.contains(p)) byBrand.add(p);

        List<Product> byCat = productsByCategory.get(catKey);
        if (byCat == null) {
            byCat = new ArrayList<>();
            productsByCategory.put(catKey, byCat);
        }
        if (!byCat.contains(p)) byCat.add(p);
    }

    private void removeFromIndex(Product p) {
        String brandKey = norm(p.getBrand());
        String catKey = norm(p.getCategory());

        List<Product> byBrand = productsByBrand.get(brandKey);
        if (byBrand != null) {
            byBrand.remove(p);
            if (byBrand.isEmpty()) productsByBrand.remove(brandKey);
        }

        List<Product> byCat = productsByCategory.get(catKey);
        if (byCat != null) {
            byCat.remove(p);
            if (byCat.isEmpty()) productsByCategory.remove(catKey);
        }
    }

}
