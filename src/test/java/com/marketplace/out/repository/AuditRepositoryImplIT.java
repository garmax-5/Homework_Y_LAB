package com.marketplace.out.repository;

import com.marketplace.model.AuditEvent;
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
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
class AuditRepositoryImplIT {

    @Container
    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16")
                    .withDatabaseName("marketplace_test")
                    .withUsername("test_user")
                    .withPassword("test_pass");

    private Connection connection;
    private AuditRepositoryImpl auditRepository;

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

        db.setDefaultSchemaName("audit");

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

        auditRepository = new AuditRepositoryImpl(connection);
    }

    @AfterEach
    void tearDown() throws Exception {
        connection.close();
    }

    private void clearTables() throws Exception {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("TRUNCATE TABLE audit.audit_events RESTART IDENTITY CASCADE;");
        }
    }


    @Test
    void save_shouldInsertAuditEvent() {
        AuditEvent event = new AuditEvent(
                null,
                Instant.now(),
                1L,
                "LOGIN",
                "User logged in",
                AuditEvent.Level.INFO
        );

        AuditEvent saved = auditRepository.save(event);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getAction()).isEqualTo("LOGIN");
        assertThat(saved.getDetails()).isEqualTo("User logged in");
        assertThat(saved.getLevel()).isEqualTo(AuditEvent.Level.INFO);
    }

    @Test
    void findAll_shouldReturnAllSavedEvents() {
        AuditEvent e1 = new AuditEvent(null, Instant.now(), 1L, "LOGIN", "User logged in", AuditEvent.Level.INFO);
        AuditEvent e2 = new AuditEvent(null, Instant.now(), 2L, "LOGOUT", "User logged out", AuditEvent.Level.ERROR);

        auditRepository.save(e1);
        auditRepository.save(e2);

        List<AuditEvent> all = auditRepository.findAll();

        assertThat(all).hasSize(2);
        assertThat(all).extracting(AuditEvent::getAction)
                .containsExactlyInAnyOrder("LOGIN", "LOGOUT");
    }
}
