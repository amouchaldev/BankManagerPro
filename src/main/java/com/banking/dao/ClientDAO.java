package com.banking.dao;

import com.banking.model.Client;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object for {@link Client} entities.
 * Provides full CRUD operations using prepared statements to prevent SQL injection.
 */
public class ClientDAO {

    private final Connection conn;

    /**
     * Creates a ClientDAO using the shared singleton connection.
     *
     * @throws SQLException if the database connection is unavailable
     */
    public ClientDAO() throws SQLException {
        this.conn = DatabaseManager.getInstance().getConnection();
    }

    // ------------------------------------------------------------------ CREATE

    /**
     * Inserts a new client into the database.
     * The generated primary key is set on the {@code client} object.
     *
     * @param client the client to insert (id is ignored, will be set by the DB)
     * @throws SQLException if the insert fails (e.g., duplicate email)
     */
    public void insert(Client client) throws SQLException {
        String sql = """
            INSERT INTO clients (first_name, last_name, email, phone, address, birth_date)
            VALUES (?, ?, ?, ?, ?, ?)
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, client.getFirstName());
            ps.setString(2, client.getLastName());
            ps.setString(3, client.getEmail());
            ps.setString(4, client.getPhone());
            ps.setString(5, client.getAddress());
            ps.setString(6, client.getBirthDate() != null ? client.getBirthDate().toString() : null);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) client.setId(keys.getInt(1));
            }
        }
    }

    // ------------------------------------------------------------------ READ

    /**
     * Returns all clients ordered by last name then first name.
     *
     * @return list of all clients, possibly empty
     * @throws SQLException if the query fails
     */
    public List<Client> findAll() throws SQLException {
        List<Client> list = new ArrayList<>();
        String sql = "SELECT * FROM clients ORDER BY last_name, first_name";
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    /**
     * Finds a client by primary key.
     *
     * @param id the client id
     * @return an Optional containing the client, or empty if not found
     * @throws SQLException if the query fails
     */
    public Optional<Client> findById(int id) throws SQLException {
        String sql = "SELECT * FROM clients WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        }
        return Optional.empty();
    }

    /**
     * Searches clients by name or email using a case-insensitive partial match.
     * Used by the search bar in the clients view.
     *
     * @param query the search string
     * @return list of matching clients
     * @throws SQLException if the query fails
     */
    public List<Client> search(String query) throws SQLException {
        List<Client> list = new ArrayList<>();
        String sql = """
            SELECT * FROM clients
            WHERE LOWER(first_name || ' ' || last_name) LIKE LOWER(?)
               OR LOWER(email) LIKE LOWER(?)
            ORDER BY last_name, first_name
            """;
        String pattern = "%" + query + "%";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, pattern);
            ps.setString(2, pattern);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    // ------------------------------------------------------------------ UPDATE

    /**
     * Updates all mutable fields of an existing client.
     *
     * @param client the client with updated values (id must be set)
     * @throws SQLException if the update fails (e.g., duplicate email)
     */
    public void update(Client client) throws SQLException {
        String sql = """
            UPDATE clients
            SET first_name=?, last_name=?, email=?, phone=?, address=?, birth_date=?
            WHERE id=?
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, client.getFirstName());
            ps.setString(2, client.getLastName());
            ps.setString(3, client.getEmail());
            ps.setString(4, client.getPhone());
            ps.setString(5, client.getAddress());
            ps.setString(6, client.getBirthDate() != null ? client.getBirthDate().toString() : null);
            ps.setInt(7, client.getId());
            ps.executeUpdate();
        }
    }

    // ------------------------------------------------------------------ DELETE

    /**
     * Deletes a client by id.
     * Due to ON DELETE CASCADE in the schema, all of the client's accounts are
     * also deleted. Validate no active accounts exist before calling this from the UI.
     *
     * @param id the client id to delete
     * @throws SQLException if the delete fails
     */
    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM clients WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    // ------------------------------------------------------------------ STATS

    /**
     * Returns the total number of clients in the database.
     *
     * @return client count
     * @throws SQLException if the query fails
     */
    public int countAll() throws SQLException {
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM clients")) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    // ------------------------------------------------------------------ MAPPING

    private Client mapRow(ResultSet rs) throws SQLException {
        String bd = rs.getString("birth_date");
        String ca = rs.getString("created_at");
        return new Client(
            rs.getInt("id"),
            rs.getString("first_name"),
            rs.getString("last_name"),
            rs.getString("email"),
            rs.getString("phone"),
            rs.getString("address"),
            bd != null ? LocalDate.parse(bd) : null,
            ca != null ? LocalDateTime.parse(ca) : LocalDateTime.now()
        );
    }
}
