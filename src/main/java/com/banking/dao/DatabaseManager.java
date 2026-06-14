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

    private DatabaseManager() throws SQLException {
        connection = DriverManager.getConnection(DB_URL);
        try (Statement st = connection.createStatement()) {
            st.execute("PRAGMA foreign_keys = ON");
            st.execute("PRAGMA journal_mode = WAL");
        }
        initializeSchema();
    }

    /**
     * Returns the singleton instance. Creates it on first call.
     *
     * @return the DatabaseManager singleton
     * @throws SQLException if the connection or schema initialization fails
     */
    public static synchronized DatabaseManager getInstance() throws SQLException {
        if (instance == null || instance.connection.isClosed()) {
            instance = new DatabaseManager();
        }
        return instance;
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
