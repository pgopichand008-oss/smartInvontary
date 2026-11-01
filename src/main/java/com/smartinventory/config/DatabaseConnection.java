package com.smartinventory.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

/**
 * Manages database connectivity and connection pooling using HikariCP.
 * Configuration credentials are loaded securely from external properties or environment variables.
 */
public final class DatabaseConnection {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseConnection.class);
    private static volatile HikariDataSource dataSource;
    private static final Object LOCK = new Object();

    // Prevent direct instantiation (Utility / Singleton pattern)
    private DatabaseConnection() {
    }

    /**
     * Retrieves or lazily initializes the HikariCP DataSource.
     *
     * @return HikariDataSource connection pool
     */
    public static DataSource getDataSource() {
        if (dataSource == null) {
            synchronized (LOCK) {
                if (dataSource == null) {
                    initDataSource();
                }
            }
        }
        return dataSource;
    }

    /**
     * Initializes the HikariCP connection pool with external configuration.
     */
    private static void initDataSource() {
        Properties props = loadDatabaseProperties();

        HikariConfig config = new HikariConfig();

        // 1. Connection URL, User, and Password
        String url = getPropertyOrEnv(props, "db.url", "DB_URL",
                "jdbc:mysql://localhost:3306/smart_inventory?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&createDatabaseIfNotExist=true");
        String user = getPropertyOrEnv(props, "db.user", "DB_USER", "root");
        String password = getPropertyOrEnv(props, "db.password", "DB_PASSWORD", "");

        config.setDriverClassName("com.mysql.cj.jdbc.Driver");
        config.setJdbcUrl(url);
        config.setUsername(user);
        config.setPassword(password);
        config.setPoolName("SmartInventoryHikariPool");

        // 2. Pool Sizing and Timeouts
        config.setMaximumPoolSize(getIntProperty(props, "db.pool.maximumPoolSize", 10));
        config.setMinimumIdle(getIntProperty(props, "db.pool.minimumIdle", 2));
        config.setIdleTimeout(getLongProperty(props, "db.pool.idleTimeout", 30000L));
        config.setConnectionTimeout(getLongProperty(props, "db.pool.connectionTimeout", 20000L));
        config.setMaxLifetime(getLongProperty(props, "db.pool.maxLifetime", 1800000L));

        // 3. MySQL Performance Optimization Properties
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("useServerPrepStmts", "true");
        config.addDataSourceProperty("useLocalSessionState", "true");
        config.addDataSourceProperty("rewriteBatchedStatements", "true");
        config.addDataSourceProperty("cacheResultSetMetadata", "true");
        config.addDataSourceProperty("cacheServerConfiguration", "true");
        config.addDataSourceProperty("elideSetAutoCommits", "true");
        config.addDataSourceProperty("maintainTimeStats", "false");

        logger.info("Initializing HikariCP pool for database: {}", extractDbName(url));
        dataSource = new HikariDataSource(config);
        logger.info("HikariCP pool successfully created.");
    }

    /**
     * Obtains an active connection from the HikariCP connection pool.
     *
     * @return Connection active database connection
     * @throws SQLException if a connection cannot be obtained
     */
    public static Connection getConnection() throws SQLException {
        return getDataSource().getConnection();
    }

    /**
     * Tests connectivity to MySQL and executes a basic validation query.
     *
     * @return true if connection succeeds, false otherwise
     */
    public static boolean testConnection() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT 1 AS connection_test")) {
            if (rs.next()) {
                logger.info("Database connectivity test passed. SELECT 1 returned: {}", rs.getInt("connection_test"));
                return true;
            }
        } catch (SQLException e) {
            logger.error("Database connectivity test failed: {}", e.getMessage(), e);
        }
        return false;
    }

    /**
     * Executes a SQL script from resources (e.g., db/schema.sql) to provision tables.
     *
     * @param resourcePath path to script in resources (e.g., "db/schema.sql")
     * @throws SQLException on execution error
     * @throws IOException  on file reading error
     */
    public static void executeSqlScript(String resourcePath) throws SQLException, IOException {
        InputStream is = DatabaseConnection.class.getClassLoader().getResourceAsStream(resourcePath);
        if (is == null) {
            throw new IOException("SQL script resource not found: " + resourcePath);
        }

        StringBuilder currentStatement = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
             Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                // Skip empty lines and comments
                if (line.isEmpty() || line.startsWith("--") || line.startsWith("//")) {
                    continue;
                }

                currentStatement.append(line).append(" ");
                if (line.endsWith(";")) {
                    String sql = currentStatement.toString().trim();
                    if (!sql.isEmpty()) {
                        stmt.execute(sql);
                    }
                    currentStatement.setLength(0);
                }
            }
            logger.info("Executed SQL script successfully: {}", resourcePath);
        }
    }

    /**
     * Gracefully closes the HikariCP connection pool.
     */
    public static void closePool() {
        synchronized (LOCK) {
            if (dataSource != null && !dataSource.isClosed()) {
                logger.info("Shutting down HikariCP connection pool...");
                dataSource.close();
                dataSource = null;
                logger.info("HikariCP connection pool closed.");
            }
        }
    }

    /**
     * Loads database properties from db.properties in the classpath,
     * falling back to db.properties.example if not present.
     */
    private static Properties loadDatabaseProperties() {
        Properties properties = new Properties();

        // Attempt 1: Load db.properties
        try (InputStream is = DatabaseConnection.class.getClassLoader().getResourceAsStream("db.properties")) {
            if (is != null) {
                properties.load(is);
                logger.info("Loaded database configuration from 'db.properties'.");
                return properties;
            }
        } catch (IOException e) {
            logger.warn("Could not read 'db.properties': {}", e.getMessage());
        }

        // Attempt 2: Fallback to db.properties.example
        try (InputStream is = DatabaseConnection.class.getClassLoader().getResourceAsStream("db.properties.example")) {
            if (is != null) {
                properties.load(is);
                logger.warn("Loaded fallback configuration from 'db.properties.example'. Please create 'db.properties' with actual credentials.");
            }
        } catch (IOException e) {
            logger.warn("Could not read 'db.properties.example': {}", e.getMessage());
        }

        return properties;
    }

    private static String getPropertyOrEnv(Properties props, String propKey, String envKey, String defaultValue) {
        String envValue = System.getenv(envKey);
        if (envValue != null && !envValue.trim().isEmpty()) {
            return envValue.trim();
        }
        String sysValue = System.getProperty(propKey);
        if (sysValue != null && !sysValue.trim().isEmpty()) {
            return sysValue.trim();
        }
        return props.getProperty(propKey, defaultValue).trim();
    }

    private static int getIntProperty(Properties props, String key, int defaultValue) {
        String val = props.getProperty(key);
        if (val != null) {
            try {
                return Integer.parseInt(val.trim());
            } catch (NumberFormatException ignored) {
            }
        }
        return defaultValue;
    }

    private static long getLongProperty(Properties props, String key, long defaultValue) {
        String val = props.getProperty(key);
        if (val != null) {
            try {
                return Long.parseLong(val.trim());
            } catch (NumberFormatException ignored) {
            }
        }
        return defaultValue;
    }

    private static String extractDbName(String jdbcUrl) {
        int qIdx = jdbcUrl.indexOf('?');
        String clean = (qIdx > 0) ? jdbcUrl.substring(0, qIdx) : jdbcUrl;
        int slashIdx = clean.lastIndexOf('/');
        return (slashIdx > 0) ? clean.substring(slashIdx + 1) : "unknown";
    }
}
