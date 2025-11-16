package com.marketplace.out.repository;

import com.marketplace.model.Product;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.jupiter.api.*;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
class ProductRepositoryImplIT {

    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16")
                    .withDatabaseName("marketplace_test")
                    .withUsername("test_user")
                    .withPassword("test_pass");

    private Connection connection;
    private ProductRepositoryImpl productRepository;

    @BeforeAll
    static void initDb() throws Exception {
        Connection conn = DriverManager.getConnection(
                POSTGRES.getJdbcUrl(),
                POSTGRES.getUsername(),
                POSTGRES.getPassword()
        );

        try (Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE SCHEMA IF NOT EXISTS auth;");
            stmt.execute("CREATE SCHEMA IF NOT EXISTS catalog;");
            stmt.execute("CREATE SCHEMA IF NOT EXISTS audit;");
        }

        Database db = DatabaseFactory.getInstance().openDatabase(
                POSTGRES.getJdbcUrl(),
                POSTGRES.getUsername(),
                POSTGRES.getPassword(),
                null,
                null
        );

        db.setDefaultSchemaName("catalog");

        Liquibase liquibase = new Liquibase(
                "db/changelog/main_changelog.xml",
                new ClassLoaderResourceAccessor(),
                db
        );

        liquibase.update();

        db.close();
        conn.close();
    }

    @BeforeEach
    void setUp() throws Exception {
        connection = DriverManager.getConnection(
                POSTGRES.getJdbcUrl(),
                POSTGRES.getUsername(),
                POSTGRES.getPassword()
        );

        clearTables();

        productRepository = new ProductRepositoryImpl(connection);
    }

    @AfterEach
    void tearDown() throws Exception {
        connection.close();
    }

    private void clearTables() throws Exception {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("TRUNCATE TABLE catalog.products RESTART IDENTITY CASCADE;");
        }
    }

    @Test
    void save_shouldInsertProduct() {
        Product p = new Product("iPhone 15", "Apple", "smartphone", 1200);

        Product saved = productRepository.save(p);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getName()).isEqualTo("iPhone 15");
    }

    @Test
    void update_shouldModifyProduct() {
        Product saved = productRepository.save(new Product("TV", "LG", "electronics", 500));

        Product toUpdate = new Product(
                saved.getId(),
                "TV OLED",
                "LG",
                "electronics",
                900,
                saved.getCreatedAt(),
                null
        );

        boolean updated = productRepository.update(toUpdate);

        assertThat(updated).isTrue();

        Product loaded = productRepository.findById(saved.getId()).get();
        assertThat(loaded.getPrice()).isEqualTo(900);
    }

    @Test
    void findAll_shouldReturnAllProducts() {
        productRepository.save(new Product("A", "BrandA", "cat1", 10));
        productRepository.save(new Product("B", "BrandB", "cat2", 20));

        List<Product> all = productRepository.findAll();

        assertThat(all).hasSize(2);
    }

    @Test
    void deleteById_shouldDeleteProduct() {
        Product saved = productRepository.save(
                new Product("Mouse", "Logi", "peripheral", 25)
        );

        boolean deleted = productRepository.deleteById(saved.getId());

        assertThat(deleted).isTrue();
        assertThat(productRepository.findById(saved.getId())).isEmpty();
    }

    @Test
    void count_shouldReturnCorrectValue() {
        productRepository.save(new Product("P1", "B1", "C1", 1));
        productRepository.save(new Product("P2", "B2", "C2", 2));

        long count = productRepository.count();

        assertThat(count).isEqualTo(2);
    }

    @Test
    void findByBrand_shouldReturnProducts() {
        productRepository.save(new Product("Laptop", "Asus", "notebook", 900));
        productRepository.save(new Product("Monitor", "ASUS", "display", 300));

        List<Product> list = productRepository.findByBrand("asus");

        assertThat(list).hasSize(2);
    }

    @Test
    void findByPriceRange_shouldReturnMatchingProducts() {
        productRepository.save(new Product("Cheap", "B1", "C1", 50));
        productRepository.save(new Product("Mid", "B2", "C2", 200));
        productRepository.save(new Product("Expensive", "B3", "C3", 800));

        List<Product> list = productRepository.findByPriceRange(100, 500);

        assertThat(list).extracting(Product::getName)
                .containsExactly("Mid");
    }
}
