package com.airquality;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * DBConnection provides a centralized JDBC Connection factory for MySQL.
 * Works seamlessly in VS Code with Apache Tomcat 10+.
 */
public class DBConnection {

    // JDBC Driver class for MySQL 8+
    private static final String JDBC_DRIVER = "com.mysql.cj.jdbc.Driver";

    // JDBC Connection URL
    private static final String DB_URL = "jdbc:mysql://localhost:3306/air_quality_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=UTF-8";

    // Database Credentials - EDIT YOUR PASSWORD HERE FOR LOCAL SETUP
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "YOUR_MYSQL_PASSWORD"; // <-- Enter your MySQL password here

    static {
        try {
            // Load the MySQL JDBC Driver into the JVM
            Class.forName(JDBC_DRIVER);
        } catch (ClassNotFoundException e) {
            System.err.println("CRITICAL: MySQL JDBC Driver (com.mysql.cj.jdbc.Driver) not found in classpath!");
            e.printStackTrace();
        }
    }

    /**
     * Obtains an active connection to MySQL.
     * Always use inside a try-with-resources statement in Servlets.
     */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }
}
