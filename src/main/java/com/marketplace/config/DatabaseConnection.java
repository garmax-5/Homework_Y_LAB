package com.marketplace.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.io.InputStream;

public class DatabaseConnection {

    private static Connection connection;

    private DatabaseConnection() { }

    public static Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            try (InputStream input = DatabaseConnection.class.getClassLoader().getResourceAsStream("application.properties")) {
                Properties prop = new Properties();
                prop.load(input);

                String url = prop.getProperty("db.url");
                String user = prop.getProperty("db.username");
                String password = prop.getProperty("db.password");

                connection = DriverManager.getConnection(url, user, password);
            } catch (Exception e) {
                throw new RuntimeException("Не удалось подключиться к базе данных", e);
            }
        }
        return connection;
    }
}
