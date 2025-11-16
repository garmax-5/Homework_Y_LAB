package com.marketplace.out.repository;

import com.marketplace.model.Product;

import java.sql.*;
import java.time.Instant;
import java.util.*;

// Репозиторий для хранения данных о товаре
public class ProductRepositoryImpl implements ProductRepository {
    private final Connection connection;

    public ProductRepositoryImpl(Connection connection) {
        this.connection = connection;
    }

    // Сохраняем новый продукт
    @Override
    public Product save(Product product) {
        final String sql = "INSERT INTO catalog.products (name, brand, category, price) " +
                "VALUES (?, ?, ?, ?) RETURNING id, created_at, updated_at, name, brand, category, price";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, product.getName());
            stmt.setString(2, product.getBrand());
            stmt.setString(3, product.getCategory());
            stmt.setDouble(4, product.getPrice());

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Product saved = mapRow(rs);
                    product.setId(saved.getId());
                    return saved;
                } else {
                    throw new SQLException("INSERT не вернул ни одной строки");
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка при сохранении продукта", e);
        }
    }

    @Override
    public boolean update(Product product) {
        final String sql = "UPDATE catalog.products SET name = ?, brand = ?, category = ?, price = ?, updated_at = now() " +
                "WHERE id = ? RETURNING updated_at";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, product.getName());
            stmt.setString(2, product.getBrand());
            stmt.setString(3, product.getCategory());
            stmt.setDouble(4, product.getPrice());
            stmt.setLong(5, product.getId());

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Timestamp ts = rs.getTimestamp("updated_at");
                    if (ts != null) {
                        product.setUpdatedAt(ts.toInstant());
                    }
                    return true;
                } else {
                    return false;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка при обновлении продукта", e);
        }
    }

    //Находим продукт по ID
    @Override
    public Optional<Product> findById(long id) {
        final String sql = "SELECT id, name, brand, category, price, created_at, updated_at FROM catalog.products WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка при чтении продукта по id", e);
        }
        return Optional.empty();
    }

    // Получаем все продукты
    @Override
    public List<Product> findAll() {
        final String sql = "SELECT id, name, brand, category, price, created_at, updated_at FROM catalog.products ORDER BY id";
        List<Product> list = new ArrayList<>();
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка при загрузке продуктов", e);
        }
    }

    // Удаляем продукт по ID
    @Override
    public boolean deleteById(long id) {
        final String sql = "DELETE FROM catalog.products WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка при удалении продукта", e);
        }
    }

    // Подсчёт всех продуктов
    @Override
    public long count() {
        final String sql = "SELECT COUNT(*) FROM catalog.products";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) return rs.getLong(1);
            return 0L;
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка при подсчёте продуктов", e);
        }
    }

    @Override
    public boolean existsById(long id) {
        final String sql = "SELECT 1 FROM catalog.products WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка проверки существования продукта", e);
        }
    }

    // Быстрый поиск по бренду (вернём копию списка)
    @Override
    public List<Product> findByBrand(String brand) {
        final String sql = "SELECT id, name, brand, category, price, created_at, updated_at FROM catalog.products WHERE LOWER(brand) = LOWER(?)";
        List<Product> list = new ArrayList<>();
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, brand);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка при поиске по бренду", e);
        }
    }

    // Быстрый поиск по категории (вернём копию списка)
    @Override
    public List<Product> findByCategory(String category) {
        final String sql = "SELECT id, name, brand, category, price, created_at, updated_at FROM catalog.products WHERE LOWER(category) = LOWER(?)";
        List<Product> list = new ArrayList<>();
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, category);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка при поиске по категории", e);
        }
    }

    @Override
    public List<Product> findByPriceRange(double min, double max) {
        final String sql = "SELECT id, name, brand, category, price, created_at, updated_at FROM catalog.products WHERE price BETWEEN ? AND ?";
        List<Product> list = new ArrayList<>();
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setDouble(1, min);
            stmt.setDouble(2, max);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка при поиске по диапазону цен", e);
        }
    }

    // Преобразование ResultSet в объект Product
    private Product mapRow(ResultSet rs) throws SQLException {
        long id = rs.getLong("id");
        String name = rs.getString("name");
        String brand = rs.getString("brand");
        String category = rs.getString("category");
        double price = rs.getDouble("price");

        Timestamp createdTs = rs.getTimestamp("created_at");
        Timestamp updatedTs = rs.getTimestamp("updated_at");

        Instant created = createdTs != null ? createdTs.toInstant() : null;
        Instant updated = updatedTs != null ? updatedTs.toInstant() : null;

        return new Product(id, name, brand, category, price, created, updated);
    }
}
