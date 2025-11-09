package com.marketplace.out.filestore;

import com.marketplace.model.Product;
import com.marketplace.out.repository.ProductRepository;

import java.io.*;
import java.util.*;

public class ProductFileStore implements ProductRepository {
    private final String filePath;

    private final Map<Long, Product> productsById = new HashMap<>();
    private final Map<String, List<Product>> productsByBrand = new HashMap<>();
    private final Map<String, List<Product>> productsByCategory = new HashMap<>();

    public ProductFileStore(String filePath) {
        this.filePath = filePath;
        load();
    }

    private String norm(String s) {
        return s == null ? "" : s.trim().toLowerCase();
    }

    private void load() {
        File file = new File(filePath);
        if (!file.exists()) return;

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",", 5); // id,name,brand,category,price
                if (parts.length == 5) {
                    long id = Long.parseLong(parts[0]);
                    String name = parts[1];
                    String brand = parts[2];
                    String category = parts[3];
                    double price = Double.parseDouble(parts[4]);
                    Product p = new Product(id, name, brand, category, price);
                    productsById.put(id, p);
                    addToIndex(p);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void saveToFile() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            for (Product p : productsById.values()) {
                String line = p.getId() + "," + p.getName() + "," + p.getBrand() + "," + p.getCategory() + "," + p.getPrice();
                writer.write(line);
                writer.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Product save(Product product) {
        Product old = productsById.get(product.getId());
        if (old != null) removeFromIndex(old);

        productsById.put(product.getId(), product);
        addToIndex(product);

        saveToFile();
        return product;
    }

    @Override
    public Optional<Product> findById(long id) {
        return Optional.ofNullable(productsById.get(id));
    }

    @Override
    public boolean deleteById(long id) {
        Product removed = productsById.remove(id);
        if (removed != null) {
            removeFromIndex(removed);
            saveToFile();
            return true;
        }
        return false;
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

    @Override
    public List<Product> findByBrand(String brand) {
        List<Product> list = productsByBrand.get(norm(brand));
        if (list == null) return new ArrayList<>();
        return new ArrayList<>(list);
    }

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
            if (p.getPrice() >= min && p.getPrice() <= max) result.add(p);
        }
        return result;
    }

    private void addToIndex(Product p) {
        String brandKey = norm(p.getBrand());
        String catKey = norm(p.getCategory());

        List<Product> byBrand = productsByBrand.get(brandKey);
        if (byBrand == null) {
            byBrand = new ArrayList<>();
            productsByBrand.put(brandKey, byBrand);
        }
        if (!byBrand.contains(p)) byBrand.add(p);

        List<Product> byCat = productsByCategory.get(catKey);
        if (byCat == null) {
            byCat = new ArrayList<>();
            productsByCategory.put(catKey, byCat);
        }
        if (!byCat.contains(p)) byCat.add(p);
    }

    private void removeFromIndex(Product p) {
        List<Product> byBrand = productsByBrand.get(norm(p.getBrand()));
        if (byBrand != null) {
            byBrand.remove(p);
            if (byBrand.isEmpty()) productsByBrand.remove(norm(p.getBrand()));
        }

        List<Product> byCat = productsByCategory.get(norm(p.getCategory()));
        if (byCat != null) {
            byCat.remove(p);
            if (byCat.isEmpty()) productsByCategory.remove(norm(p.getCategory()));
        }
    }
}
