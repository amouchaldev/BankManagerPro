package com.banking.dao;

import com.banking.model.Account;
import com.banking.model.AccountType;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Data Access Object for {@link Account} entities.
 * Account numbers are auto-generated in the format {@code FR76-XXXX-XXXX}.
 */
public class AccountDAO {

    private final Connection conn;

    /**
     * Creates an AccountDAO using the shared singleton connection.
     *
     * @throws SQLException if the database connection is unavailable
     */
    public AccountDAO() throws SQLException {
        this.conn = DatabaseManager.getInstance().getConnection();
    }

    // ------------------------------------------------------------------ UTILITY

    /**
     * Generates a unique account number for a given client.
     * Format: {@code FR76-[clientId padded to 4]-[sequence padded to 4]}.
     *
     * @param clientId the owning client's id
     * @return a unique account number string
     * @throws SQLException if the query fails
     */
    public String generateAccountNumber(int clientId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM accounts WHERE client_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, clientId);
            try (ResultSet rs = ps.executeQuery()) {
                int seq = rs.next() ? rs.getInt(1) + 1 : 1;
                return String.format("FR76-%04d-%04d", clientId, seq);
            }
        }
    }

    // ------------------------------------------------------------------ CREATE

    /**
     * Inserts a new account. Sets the generated id on the object.
     *
     * @param account the account to insert
     * @throws SQLException if the insert fails (e.g., duplicate account number)
     */
    public void insert(Account account) throws SQLException {
        String sql = """
            INSERT INTO accounts
              (account_number, client_id, account_type, balance,
               overdraft_limit, interest_rate, is_active)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, account.getAccountNumber());
            ps.setInt(2, account.getClientId());
            ps.setString(3, account.getAccountType().name());
            ps.setDouble(4, account.getBalance());
            ps.setDouble(5, account.getOverdraftLimit());
            ps.setDouble(6, account.getInterestRate());
            ps.setInt(7, account.isActive() ? 1 : 0);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) account.setId(keys.getInt(1));
            }
        }
    }

    // ------------------------------------------------------------------ READ

    /**
     * Returns all accounts with the owning client's name (via JOIN), ordered by id.
     *
     * @return list of all accounts
     * @throws SQLException if the query fails
     */
    public List<Account> findAll() throws SQLException {
        List<Account> list = new ArrayList<>();
        String sql = """
            SELECT a.*, c.first_name || ' ' || c.last_name AS client_name
            FROM accounts a
            JOIN clients c ON a.client_id = c.id
            ORDER BY a.id
            """;
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    /**
     * Returns all accounts belonging to a given client.
     *
     * @param clientId the client's id
     * @return list of the client's accounts
     * @throws SQLException if the query fails
     */
    public List<Account> findByClientId(int clientId) throws SQLException {
        List<Account> list = new ArrayList<>();
        String sql = """
            SELECT a.*, c.first_name || ' ' || c.last_name AS client_name
            FROM accounts a
            JOIN clients c ON a.client_id = c.id
            WHERE a.client_id = ?
            ORDER BY a.id
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, clientId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    /**
     * Finds a single account by its primary key.
     *
     * @param id the account id
     * @return an Optional containing the account, or empty if not found
     * @throws SQLException if the query fails
     */
    public Optional<Account> findById(int id) throws SQLException {
        String sql = """
            SELECT a.*, c.first_name || ' ' || c.last_name AS client_name
            FROM accounts a
            JOIN clients c ON a.client_id = c.id
            WHERE a.id = ?
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        }
        return Optional.empty();
    }

    /**
     * Counts how many transactions reference this account (as source or target).
     * Used to prevent deletion of accounts with transaction history.
     *
     * @param accountId the account id to check
     * @return the number of transactions referencing this account
     * @throws SQLException if the query fails
     */
    public int countTransactions(int accountId) throws SQLException {
        String sql = """
            SELECT COUNT(*) FROM transactions
            WHERE source_account_id = ? OR target_account_id = ?
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, accountId);
            ps.setInt(2, accountId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    // ------------------------------------------------------------------ UPDATE

    /**
     * Updates the balance of an account directly.
     * Called atomically inside TransactionDAO after recording the transaction.
     *
     * @param accountId  the account to update
     * @param newBalance the new balance value
     * @throws SQLException if the update fails
     */
    public void updateBalance(int accountId, double newBalance) throws SQLException {
        String sql = "UPDATE accounts SET balance = ? WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, newBalance);
            ps.setInt(2, accountId);
            ps.executeUpdate();
        }
    }

    /**
     * Updates the editable metadata fields of an account.
     * Does NOT update the balance — use {@link #updateBalance} for that.
     *
     * @param account the account with updated values (id must be set)
     * @throws SQLException if the update fails
     */
    public void update(Account account) throws SQLException {
        String sql = """
            UPDATE accounts
            SET account_type=?, overdraft_limit=?, interest_rate=?, is_active=?
            WHERE id=?
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, account.getAccountType().name());
            ps.setDouble(2, account.getOverdraftLimit());
            ps.setDouble(3, account.getInterestRate());
            ps.setInt(4, account.isActive() ? 1 : 0);
            ps.setInt(5, account.getId());
            ps.executeUpdate();
        }
    }

    // ------------------------------------------------------------------ DELETE

    /**
     * Deletes an account by id.
     * Only call this after verifying {@link #countTransactions(int)} returns 0.
     *
     * @param id the account id to delete
     * @throws SQLException if the delete fails
     */
    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM accounts WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    // ------------------------------------------------------------------ STATS

    /**
     * Returns the total sum of all active account balances.
     * Used on the dashboard to show total assets under management.
     *
     * @return total assets, or 0.0 if no active accounts
     * @throws SQLException if the query fails
     */
    public double totalAssetsUnderManagement() throws SQLException {
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(
                "SELECT COALESCE(SUM(balance), 0) FROM accounts WHERE is_active = 1")) {
            return rs.next() ? rs.getDouble(1) : 0.0;
        }
    }

    /**
     * Returns the count of accounts grouped by account type.
     * Used on the dashboard for the account type summary.
     *
     * @return map from AccountType to count
     * @throws SQLException if the query fails
     */
    public Map<AccountType, Integer> countByType() throws SQLException {
        Map<AccountType, Integer> map = new LinkedHashMap<>();
        String sql = "SELECT account_type, COUNT(*) FROM accounts GROUP BY account_type";
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                map.put(AccountType.valueOf(rs.getString(1)), rs.getInt(2));
            }
        }
        return map;
    }

    /**
     * Returns the total number of accounts in the database.
     *
     * @return account count
     * @throws SQLException if the query fails
     */
    public int countAll() throws SQLException {
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM accounts")) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    // ------------------------------------------------------------------ MAPPING

    private Account mapRow(ResultSet rs) throws SQLException {
        return new Account(
            rs.getInt("id"),
            rs.getString("account_number"),
            rs.getInt("client_id"),
            rs.getString("client_name"),
            AccountType.valueOf(rs.getString("account_type")),
            rs.getDouble("balance"),
            rs.getDouble("overdraft_limit"),
            rs.getDouble("interest_rate"),
            rs.getInt("is_active") == 1,
            LocalDateTime.parse(rs.getString("opened_at"))
        );
    }
}
