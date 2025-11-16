package com.marketplace.out.repository;

import com.marketplace.model.User;
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
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
class UserRepositoryImplIT {

    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16")
                    .withDatabaseName("marketplace_test")
                    .withUsername("test_user")
                    .withPassword("test_pass");

    private Connection connection;
    private UserRepositoryImpl userRepository;

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

        db.setDefaultSchemaName("auth");

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

        userRepository = new UserRepositoryImpl(connection);
    }

    @AfterEach
    void tearDown() throws Exception {
        connection.close();
    }

    private void clearTables() throws Exception {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("TRUNCATE TABLE auth.users RESTART IDENTITY CASCADE;");
        }
    }


    @Test
    void save_shouldInsertUser() {
        String username = "user_" + UUID.randomUUID();

        User newUser = new User(username, "1234", User.Role.USER);

        User saved = userRepository.save(newUser);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getUserName()).isEqualTo(username);

        Optional<User> found = userRepository.findByUserName(username);
        assertThat(found).isPresent();
    }

    @Test
    void findByUserName_shouldReturnEmpty_whenUserNotExists() {
        Optional<User> found = userRepository.findByUserName("ghost_" + UUID.randomUUID());
        assertThat(found).isEmpty();
    }

    @Test
    void update_shouldModifyExistingUser() {
        String username = "user_" + UUID.randomUUID();

        User saved = userRepository.save(new User(username, "1234", User.Role.USER));

        String newUsername = "updated_" + UUID.randomUUID();

        User updated = new User(saved.getId(), newUsername, "pass999", User.Role.ADMIN);

        User result = userRepository.update(updated);

        assertThat(result.getUserName()).isEqualTo(newUsername);
        assertThat(result.getRole()).isEqualTo(User.Role.ADMIN);
    }

    @Test
    void existsById_shouldReturnTrue_whenUserExists() {
        String username = "user_" + UUID.randomUUID();

        User saved = userRepository.save(new User(username, "pass", User.Role.USER));

        assertThat(userRepository.existsById(saved.getId())).isTrue();
    }

    @Test
    void existsById_shouldReturnFalse_whenNoUser() {
        assertThat(userRepository.existsById(9999)).isFalse();
    }
}
