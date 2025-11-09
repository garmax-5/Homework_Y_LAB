package com.marketplace.out.repository;

import com.marketplace.model.Product;

import java.util.List;
import java.util.Optional;

public interface ProductRepository {
    Product save(Product product); // save or update
    Optional<Product> findById(long id);
    boolean deleteById(long id);
    List<Product> findAll();
    long count();
    boolean existsById(long id);
    List<Product> findByBrand(String brand);
    List<Product> findByCategory(String category);
    List<Product> findByPriceRange(double min, double max);
}