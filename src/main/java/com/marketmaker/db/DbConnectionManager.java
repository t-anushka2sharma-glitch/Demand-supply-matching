package com.marketmaker.db;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Opens plain JDBC connections to MySQL using settings from
 * {@code db.properties} (found on the classpath, i.e. under
 * {@code src/main/resources}).
 */
public final class DbConnectionManager {

    private static final Properties PROPERTIES = new Properties();

    static {
        try (InputStream in = DbConnectionManager.class.getClassLoader().getResourceAsStream("db.properties")) {
            if (in == null) {
                throw new IllegalStateException("db.properties not found on classpath");
            }
            PROPERTIES.load(in);
            // Explicitly register the driver (usually auto-registered via JDBC 4 SPI,
            // but kept explicit here so the failure mode is obvious if the jar is missing).
            Class.forName(PROPERTIES.getProperty("db.driver", "com.mysql.cj.jdbc.Driver"));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load db.properties", e);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("MySQL JDBC driver not found on classpath", e);
        }
    }

    private DbConnectionManager() {
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(
                PROPERTIES.getProperty("db.url"),
                PROPERTIES.getProperty("db.username"),
                PROPERTIES.getProperty("db.password"));
    }
}
