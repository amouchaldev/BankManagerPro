package com.banking.dao;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Singleton that manages the SQLite JDBC connection.
 * Reads {@code schema.sql} from resources on first startup and
 * runs all CREATE TABLE IF NOT EXISTS statements.
 * All DAO classes obtain their connection from {@link #getConnection()}.
 */
public class DatabaseManager {

    private static final String DB_URL = "jdbc:sqlite:banking.db";
    private static DatabaseManager instance;
    private Connection connection;

    /** Injected test connection — when set, getInstance() wraps it instead of opening banking.db. */
    private static Connection testConnection = null;

    private DatabaseManager() throws SQLException {
        connection = DriverManager.getConnection(DB_URL);
        try (Statement st = connection.createStatement()) {
            st.execute("PRAGMA foreign_keys = ON");
            st.execute("PRAGMA journal_mode = WAL");
        }
        initializeSchema();
    }

    /** Constructor used in test mode — wraps an existing connection, skips schema.sql. */
    private DatabaseManager(Connection conn) {
        this.connection = conn;
    }

    /**
     * Returns the singleton instance. Creates it on first call.
     *
     * @return the DatabaseManager singleton
     * @throws SQLException if the connection or schema initialization fails
     */
    public static synchronized DatabaseManager getInstance() throws SQLException {
        if (testConnection != null) {
            // Unit-test mode: wrap the injected connection without touching banking.db
            if (instance == null) {
                instance = new DatabaseManager(testConnection);
            }
            return instance;
        }
        if (instance == null || instance.connection.isClosed()) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    /**
     * Injects an external connection for use in unit tests.
     * Call {@link #clearTestConnection()} in {@code @AfterAll} to clean up.
     *
     * @param conn the test JDBC connection (typically an in-memory SQLite connection)
     */
    public static synchronized void setTestConnection(Connection conn) {
        testConnection = conn;
        instance = null;  // force re-creation with the test connection
    }

    /**
     * Removes the injected test connection and resets the singleton.
     * Should be called in {@code @AfterAll} after each test class.
     */
    public static synchronized void clearTestConnection() {
        testConnection = null;
        instance = null;
    }

    /**
     * Returns the raw JDBC connection for DAO use.
     * The connection is shared — DAOs that use transactions must restore
     * {@code setAutoCommit(true)} in a finally block.
     *
     * @return the active SQLite connection
     */
    public Connection getConnection() {
        return connection;
    }

    /**
     * Reads and executes {@code schema.sql} from the classpath.
     * Safe to run on every startup because all statements use CREATE TABLE IF NOT EXISTS.
     *
     * @throws SQLException if any statement fails
     */
    private void initializeSchema() throws SQLException {
        try (InputStream is = getClass().getResourceAsStream("/com/banking/database/schema.sql")) {
            if (is == null) {
                throw new RuntimeException("schema.sql not found in classpath at /com/banking/database/schema.sql");
            }
            String sql = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            // Split on semicolon and execute each non-empty statement individually.
            // Strip comment lines from each chunk before checking emptiness — chunks
            // starting with "-- comment\nCREATE TABLE..." must not be skipped.
            for (String statement : sql.split(";")) {
                String withoutComments = Arrays.stream(statement.split("\n"))
                    .filter(line -> !line.trim().startsWith("--"))
                    .collect(Collectors.joining("\n"))
                    .trim();
                if (!withoutComments.isEmpty()) {
                    try (Statement st = connection.createStatement()) {
                        st.execute(withoutComments);
                    } catch (SQLException e) {
                        // Ignore duplicate-key errors from seed data on repeated startups
                        if (!e.getMessage().contains("UNIQUE constraint failed")) {
                            throw e;
                        }
                    }
                }
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new SQLException("Schema initialization failed: " + e.getMessage(), e);
        }
    }

    /**
     * Closes the JDBC connection. Should be called on application exit via
     * {@link javafx.application.Application#stop()}.
     */
    public void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException ignored) {}
    }
}
