package com.airquality.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * DBConnection provides a centralized JDBC Connection factory
 * for the Air Quality Monitor application.
 */
public class DBConnection {

    // Database connection credentials
    // Note: In production or local setups, modify URL, USER, and PASSWORD as needed
    private static final String JDBC_DRIVER = "com.mysql.cj.jdbc.Driver";
    private static final String DB_URL = "jdbc:mysql://localhost:3306/air_quality_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=UTF-8";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "YOUR_PASSWORD"; // Replace with your MySQL root password

    static {
        try {
            // Load MySQL JDBC Driver into JVM
            Class.forName(JDBC_DRIVER);
        } catch (ClassNotFoundException e) {
            System.err.println("CRITICAL: MySQL JDBC Driver not found in classpath!");
            System.err.println("Ensure mysql-connector-j-8.x.x.jar is inside WebContent/WEB-INF/lib/");
            e.printStackTrace();
        }
    }

    /**
     * Obtains a new JDBC Connection to MySQL.
     * Always remember to close Connection in a try-with-resources block.
     *
     * @return Connection object
     * @throws SQLException if a database access error occurs
     */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }
}
