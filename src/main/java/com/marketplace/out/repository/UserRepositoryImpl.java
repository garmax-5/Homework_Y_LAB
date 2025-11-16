package com.marketplace.out.repository;

import com.marketplace.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public class UserRepositoryImpl implements UserRepository {
    private final Connection connection;

    public UserRepositoryImpl(Connection connection) {
        this.connection = connection;
    }

    @Override
    public Optional<User> findByUserName(String userName) {
        final String sql = "SELECT id, username, password, role FROM auth.users WHERE username = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, userName);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка поиска пользователя по username", e);
        }
        return Optional.empty();
    }

    @Override
    public User save(User user) {
        final String sql = "INSERT INTO auth.users (username, password, role) " +
                "VALUES (?, ?, ?) RETURNING id, username, password, role";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, user.getUserName());
            stmt.setString(2, user.getPassword());
            stmt.setString(3, user.getRole().name());

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return mapRow(rs);
                else throw new SQLException("INSERT не вернул ни одной строки");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка при сохранении нового пользователя", e);
        }
    }

    @Override
    public User update(User user) {
        final String sql = "UPDATE auth.users SET username = ?, password = ?, role = ? " +
                "WHERE id = ? RETURNING id, username, password, role";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, user.getUserName());
            stmt.setString(2, user.getPassword());
            stmt.setString(3, user.getRole().name());
            stmt.setLong(4, user.getId());

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return mapRow(rs);
                else throw new SQLException("UPDATE не вернул ни одной строки");
            }
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка при обновлении пользователя", e);
        }
    }

    @Override
    public boolean existsById(long id) {
        final String sql = "SELECT 1 FROM auth.users WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка проверки существования пользователя", e);
        }
    }

    private User mapRow(ResultSet rs) throws SQLException {
        long id = rs.getLong("id");
        String username = rs.getString("username");
        String password = rs.getString("password");
        User.Role role = User.Role.valueOf(rs.getString("role"));
        return new User(id, username, password, role);
    }
}