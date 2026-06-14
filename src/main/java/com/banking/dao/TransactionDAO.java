package com.banking.dao;

import com.banking.model.Account;
import com.banking.model.Transaction;
import com.banking.model.TransactionType;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Data Access Object for {@link Transaction} entities.
 *
 * <p>Transactions are immutable once created — there is no update method.
 * The three {@code execute*()} methods wrap both the balance update and
 * the transaction insert inside a single JDBC transaction for atomicity.
 *
 * <p><strong>Connection sharing note:</strong> All DAOs share the same connection.
 * The {@code setAutoCommit(false)} calls here affect the global connection state;
 * the finally blocks always restore {@code setAutoCommit(true)}.
 */
public class TransactionDAO {

    private final Connection conn;
    private final AccountDAO accountDAO;

    /**
     * Creates a TransactionDAO using the shared singleton connection.
     *
     * @throws SQLException if the database connection is unavailable
     */
    public TransactionDAO() throws SQLException {
        this.conn       = DatabaseManager.getInstance().getConnection();
        this.accountDAO = new AccountDAO();
    }

    // ------------------------------------------------------------------ EXECUTE

    /**
     * Atomically records a DEPOSIT and credits the target account balance.
     *
     * @param targetAccountId the account receiving the deposit
     * @param amount          must be positive
     * @param description     optional memo
     * @return the persisted Transaction with its generated id
     * @throws SQLException          if any DB operation fails (rolls back)
     * @throws IllegalArgumentException if targetAccountId is invalid
     */
    public Transaction executeDeposit(int targetAccountId, double amount,
                                      String description) throws SQLException {
        conn.setAutoCommit(false);
        try {
            Account target = accountDAO.findById(targetAccountId)
                .orElseThrow(() -> new SQLException("Compte introuvable: " + targetAccountId));
            accountDAO.updateBalance(targetAccountId, target.getBalance() + amount);

            Transaction tx = new Transaction(0, 0, targetAccountId,
                "Externe", target.getAccountNumber(),
                TransactionType.DEPOSIT, amount, description, LocalDateTime.now());
            insertTransaction(tx);
            conn.commit();
            return tx;
        } catch (Exception e) {
            conn.rollback();
            throw (e instanceof SQLException) ? (SQLException) e : new SQLException(e);
        } finally {
            conn.setAutoCommit(true);
        }
    }

    /**
     * Atomically records a WITHDRAWAL and debits the source account balance.
     * Checks that the resulting balance does not drop below {@code -overdraftLimit}.
     *
     * @param sourceAccountId the account being debited
     * @param amount          must be positive
     * @param description     optional memo
     * @return the persisted Transaction with its generated id
     * @throws SQLException           if any DB operation fails (rolls back)
     * @throws IllegalStateException  if the account has insufficient funds
     */
    public Transaction executeWithdrawal(int sourceAccountId, double amount,
                                         String description) throws SQLException {
        conn.setAutoCommit(false);
        try {
            Account source = accountDAO.findById(sourceAccountId)
                .orElseThrow(() -> new SQLException("Compte introuvable: " + sourceAccountId));
            double available = source.getBalance() + source.getOverdraftLimit();
            if (amount > available) {
                throw new IllegalStateException(
                    String.format("Solde insuffisant. Disponible: %.2f €", available));
            }
            accountDAO.updateBalance(sourceAccountId, source.getBalance() - amount);

            Transaction tx = new Transaction(0, sourceAccountId, 0,
                source.getAccountNumber(), "Externe",
                TransactionType.WITHDRAWAL, amount, description, LocalDateTime.now());
            insertTransaction(tx);
            conn.commit();
            return tx;
        } catch (IllegalStateException e) {
            conn.rollback();
            // Re-wrap as a SQLException so the catch block above handles it uniformly
            throw new SQLException(e.getMessage(), e);
        } catch (Exception e) {
            conn.rollback();
            throw (e instanceof SQLException) ? (SQLException) e : new SQLException(e);
        } finally {
            conn.setAutoCommit(true);
        }
    }

    /**
     * Atomically records a TRANSFER between two accounts.
     * Debits the source and credits the target in the same JDBC transaction.
     *
     * @param sourceAccountId the account being debited
     * @param targetAccountId the account being credited
     * @param amount          must be positive
     * @param description     optional memo
     * @return the persisted Transaction with its generated id
     * @throws SQLException           if any DB operation fails (rolls back)
     * @throws IllegalStateException  if the source has insufficient funds
     * @throws IllegalArgumentException if source and target are the same account
     */
    public Transaction executeTransfer(int sourceAccountId, int targetAccountId,
                                       double amount, String description) throws SQLException {
        if (sourceAccountId == targetAccountId) {
            throw new SQLException("Le compte source et cible doivent être différents.");
        }
        conn.setAutoCommit(false);
        try {
            Account source = accountDAO.findById(sourceAccountId)
                .orElseThrow(() -> new SQLException("Compte source introuvable"));
            Account target = accountDAO.findById(targetAccountId)
                .orElseThrow(() -> new SQLException("Compte cible introuvable"));

            double available = source.getBalance() + source.getOverdraftLimit();
            if (amount > available) {
                throw new IllegalStateException(
                    String.format("Solde insuffisant pour le virement. Disponible: %.2f €", available));
            }
            accountDAO.updateBalance(sourceAccountId, source.getBalance() - amount);
            accountDAO.updateBalance(targetAccountId, target.getBalance() + amount);

            Transaction tx = new Transaction(0, sourceAccountId, targetAccountId,
                source.getAccountNumber(), target.getAccountNumber(),
                TransactionType.TRANSFER, amount, description, LocalDateTime.now());
            insertTransaction(tx);
            conn.commit();
            return tx;
        } catch (IllegalStateException e) {
            conn.rollback();
            throw new SQLException(e.getMessage(), e);
        } catch (Exception e) {
            conn.rollback();
            throw (e instanceof SQLException) ? (SQLException) e : new SQLException(e);
        } finally {
            conn.setAutoCommit(true);
        }
    }

    // ------------------------------------------------------------------ READ

    /**
     * Returns all transactions ordered by date descending (newest first).
     *
     * @return list of all transactions
     * @throws SQLException if the query fails
     */
    public List<Transaction> findAll() throws SQLException {
        List<Transaction> list = new ArrayList<>();
        String sql = """
            SELECT t.*,
                   sa.account_number AS src_num,
                   ta.account_number AS tgt_num
            FROM transactions t
            LEFT JOIN accounts sa ON t.source_account_id = sa.id
            LEFT JOIN accounts ta ON t.target_account_id = ta.id
            ORDER BY t.transaction_date DESC
            """;
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    /**
     * Returns all transactions involving a specific account (as source or target),
     * ordered by date descending.
     *
     * @param accountId the account to filter by
     * @return list of matching transactions
     * @throws SQLException if the query fails
     */
    public List<Transaction> findByAccountId(int accountId) throws SQLException {
        List<Transaction> list = new ArrayList<>();
        String sql = """
            SELECT t.*,
                   sa.account_number AS src_num,
                   ta.account_number AS tgt_num
            FROM transactions t
            LEFT JOIN accounts sa ON t.source_account_id = sa.id
            LEFT JOIN accounts ta ON t.target_account_id = ta.id
            WHERE t.source_account_id = ? OR t.target_account_id = ?
            ORDER BY t.transaction_date DESC
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, accountId);
            ps.setInt(2, accountId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    /**
     * Returns a chronological list of (date, running_balance) pairs for a given account.
     * Reconstructs the balance history by replaying all transactions in order.
     * Consumed by the LineChart in {@link com.banking.controller.DashboardController}.
     *
     * @param accountId the account whose history to reconstruct
     * @return list of Object[]{LocalDateTime date, Double runningBalance}
     * @throws SQLException if the query fails
     */
    public List<Object[]> getBalanceHistory(int accountId) throws SQLException {
        List<Object[]> history = new ArrayList<>();
        String sql = """
            SELECT transaction_date, transaction_type, amount,
                   source_account_id, target_account_id
            FROM transactions
            WHERE source_account_id = ? OR target_account_id = ?
            ORDER BY transaction_date ASC
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, accountId);
            ps.setInt(2, accountId);
            double runningBalance = 0.0;
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    double amount = rs.getDouble("amount");
                    int srcId = rs.getInt("source_account_id");
                    boolean srcNull = rs.wasNull();
                    // outgoing if this account is the source
                    if (!srcNull && srcId == accountId) {
                        runningBalance -= amount;
                    } else {
                        runningBalance += amount;
                    }
                    LocalDateTime date = LocalDateTime.parse(rs.getString("transaction_date"));
                    history.add(new Object[]{date, runningBalance});
                }
            }
        }
        return history;
    }

    /**
     * Returns the total transaction volume (sum of amounts) in the current calendar month.
     * Used on the dashboard summary card.
     *
     * @return total monthly volume, or 0.0 if no transactions this month
     * @throws SQLException if the query fails
     */
    public double totalTransactionsThisMonth() throws SQLException {
        String sql = """
            SELECT COALESCE(SUM(amount), 0) FROM transactions
            WHERE strftime('%Y-%m', transaction_date) = strftime('%Y-%m', 'now')
            """;
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            return rs.next() ? rs.getDouble(1) : 0.0;
        }
    }

    /**
     * Returns the count of transactions grouped by type.
     * Used on the dashboard for the type summary.
     *
     * @return map from TransactionType to count
     * @throws SQLException if the query fails
     */
    public Map<TransactionType, Integer> countByType() throws SQLException {
        Map<TransactionType, Integer> map = new LinkedHashMap<>();
        String sql = "SELECT transaction_type, COUNT(*) FROM transactions GROUP BY transaction_type";
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                map.put(TransactionType.valueOf(rs.getString(1)), rs.getInt(2));
            }
        }
        return map;
    }

    /**
     * Returns the total number of transactions in the database.
     *
     * @return transaction count
     * @throws SQLException if the query fails
     */
    public int countAll() throws SQLException {
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM transactions")) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    // ------------------------------------------------------------------ DELETE

    /**
     * Deletes a transaction record by id.
     * Does NOT reverse the corresponding account balance changes.
     * Should only be used for administrative corrections.
     *
     * @param id the transaction id to delete
     * @throws SQLException if the delete fails
     */
    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM transactions WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    // ------------------------------------------------------------------ INTERNAL

    private void insertTransaction(Transaction tx) throws SQLException {
        String sql = """
            INSERT INTO transactions
              (source_account_id, target_account_id, transaction_type,
               amount, description, transaction_date)
            VALUES (?, ?, ?, ?, ?, ?)
            """;
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            if (tx.getSourceAccountId() == 0) ps.setNull(1, Types.INTEGER);
            else ps.setInt(1, tx.getSourceAccountId());

            if (tx.getTargetAccountId() == 0) ps.setNull(2, Types.INTEGER);
            else ps.setInt(2, tx.getTargetAccountId());

            ps.setString(3, tx.getTransactionType().name());
            ps.setDouble(4, tx.getAmount());
            ps.setString(5, tx.getDescription());
            ps.setString(6, tx.getTransactionDate().toString());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) tx.setId(keys.getInt(1));
            }
        }
    }

    private Transaction mapRow(ResultSet rs) throws SQLException {
        int srcId = rs.getInt("source_account_id");
        boolean srcNull = rs.wasNull();
        int tgtId = rs.getInt("target_account_id");
        boolean tgtNull = rs.wasNull();
        String srcNum = rs.getString("src_num");
        String tgtNum = rs.getString("tgt_num");
        return new Transaction(
            rs.getInt("id"),
            srcNull ? 0 : srcId,
            tgtNull ? 0 : tgtId,
            srcNum != null ? srcNum : "Externe",
            tgtNum != null ? tgtNum : "Externe",
            TransactionType.valueOf(rs.getString("transaction_type")),
            rs.getDouble("amount"),
            rs.getString("description"),
            LocalDateTime.parse(rs.getString("transaction_date"))
        );
    }
}
