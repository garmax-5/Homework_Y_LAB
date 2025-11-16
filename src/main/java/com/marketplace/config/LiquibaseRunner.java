package com.marketplace.config;

import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.resource.ClassLoaderResourceAccessor;
import liquibase.database.jvm.JdbcConnection;

import java.io.FileInputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Properties;

public class LiquibaseRunner {

    public static void run() {
        try {
            Properties props = new Properties();
            props.load(new FileInputStream("src/main/resources/application.properties"));

            String url = props.getProperty("db.url");
            String user = props.getProperty("db.username");
            String pass = props.getProperty("db.password");
            String changelog = props.getProperty("liquibase.changelog");

            Connection conn = DriverManager.getConnection(url, user, pass);
            Database database = DatabaseFactory.getInstance()
                    .findCorrectDatabaseImplementation(new JdbcConnection(conn));

            Liquibase liquibase = new Liquibase(
                    changelog,
                    new ClassLoaderResourceAccessor(),
                    database
            );

            liquibase.update();
            System.out.println("Liquibase: миграции успешно выполнены.");
            conn.close();
        } catch (Exception e) {
            throw new RuntimeException("Ошибка запуска Liquibase: " + e.getMessage(), e);
        }
    }
}

