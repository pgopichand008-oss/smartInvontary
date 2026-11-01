package com.smartinventory.config;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Phase 1 Verification: Tests JDBC connection and query execution using HikariCP.
 * Can be run as a JUnit 5 test or executed directly via main().
 */
public class DatabaseConnectionTest {

    @Test
    @DisplayName("Verify MySQL Connectivity and Query Execution")
    public void testDatabaseConnection() throws Exception {
        boolean connected = DatabaseConnection.testConnection();
        assertTrue(connected, "Database connection should succeed with valid credentials.");

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {

            assertNotNull(conn, "Connection must not be null");
            assertFalse(conn.isClosed(), "Connection must be active");

            DatabaseMetaData meta = conn.getMetaData();
            System.out.println("==================================================");
            System.out.println("       DATABASE CONNECTIVITY TEST: SUCCESS        ");
            System.out.println("==================================================");
            System.out.println("Database Product : " + meta.getDatabaseProductName());
            System.out.println("Database Version : " + meta.getDatabaseProductVersion());
            System.out.println("Driver Name      : " + meta.getDriverName());
            System.out.println("Driver Version   : " + meta.getDriverVersion());
            System.out.println("Active Catalog   : " + conn.getCatalog());

            // Check if tables already exist or initialize schema
            try (ResultSet rs = stmt.executeQuery("SHOW TABLES LIKE 'products'")) {
                if (!rs.next()) {
                    System.out.println("\nTables not found. Initializing schema from db/schema.sql...");
                    DatabaseConnection.executeSqlScript("db/schema.sql");
                    System.out.println("Schema successfully initialized.");
                } else {
                    System.out.println("\nExisting project tables verified.");
                }
            }

            // Verify a SELECT query against the users table
            try (ResultSet rs = stmt.executeQuery("SELECT user_id, username, role FROM users LIMIT 1")) {
                if (rs.next()) {
                    System.out.println("Default Seed User Found -> ID: " + rs.getInt("user_id")
                            + ", Username: " + rs.getString("username")
                            + ", Role: " + rs.getString("role"));
                }
            }
            System.out.println("==================================================");
        }
    }

    @AfterAll
    public static void tearDown() {
        DatabaseConnection.closePool();
    }

    /**
     * Standalone main method to run connectivity verification directly.
     */
    public static void main(String[] args) {
        System.out.println("\n>>> Starting Phase 1 Database Connection Test <<<\n");
        DatabaseConnectionTest test = new DatabaseConnectionTest();
        try {
            test.testDatabaseConnection();
            System.out.println("\n>>> Phase 1 Test Result: PASSED <<<\n");
        } catch (Throwable t) {
            System.err.println("\n==================================================");
            System.err.println("       DATABASE CONNECTIVITY TEST: FAILED         ");
            System.err.println("==================================================");
            System.err.println("Error Message: " + t.getMessage());
            System.err.println("\nTroubleshooting Guide:");
            System.err.println("1. Ensure MySQL Server is running on localhost:3306.");
            System.err.println("2. Check 'src/main/resources/db.properties'.");
            System.err.println("3. Verify username and password match your MySQL instance.");
            System.err.println("4. Or set environment variables: DB_URL, DB_USER, DB_PASSWORD.");
            System.err.println("==================================================\n");
            System.exit(1);
        } finally {
            tearDown();
        }
    }
}
