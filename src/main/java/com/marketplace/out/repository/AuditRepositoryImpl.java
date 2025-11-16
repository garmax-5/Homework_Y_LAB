package com.marketplace.out.repository;

import com.marketplace.model.AuditEvent;

import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class AuditRepositoryImpl implements AuditRepository {
    private final Connection connection;

    public AuditRepositoryImpl(Connection connection) {
        this.connection = connection;
    }

    @Override
    public AuditEvent save(AuditEvent event) {
        String sql = "INSERT INTO audit.audit_events(user_id, action, details, level) " +
                "VALUES (?, ?, ?, ?) RETURNING id, timestamp";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            if (event.getUserId() != null) {
                stmt.setLong(1, event.getUserId());
            } else {
                stmt.setNull(1, Types.BIGINT);
            }
            stmt.setString(2, event.getAction());
            stmt.setString(3, event.getDetails());
            stmt.setString(4, event.getLevel().name());

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Long id = rs.getLong("id");
                    Timestamp ts = rs.getTimestamp("timestamp");
                    Instant timestamp = ts.toInstant();
                    return new AuditEvent(id, timestamp, event.getUserId(),
                            event.getAction(), event.getDetails(), event.getLevel());
                } else {
                    throw new SQLException("INSERT в audit_events не вернул ни одной строки");
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка при сохранении события аудита", e);
        }
    }

    @Override
    public List<AuditEvent> findAll() {
        String sql = "SELECT id, timestamp, user_id, action, details, level " +
                "FROM audit.audit_events ORDER BY timestamp DESC";
        List<AuditEvent> events = new ArrayList<>();
        try (PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                Long userId = rs.getLong("user_id");
                if (rs.wasNull()) userId = null;
                Long id = rs.getLong("id");
                Instant timestamp = rs.getTimestamp("timestamp").toInstant();
                AuditEvent.Level level = AuditEvent.Level.valueOf(rs.getString("level"));
                AuditEvent event = new AuditEvent(
                        id, timestamp, userId,
                        rs.getString("action"),
                        rs.getString("details"),
                        level
                );
                events.add(event);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка получения событий аудита", e);
        }
        return events;
    }
}
