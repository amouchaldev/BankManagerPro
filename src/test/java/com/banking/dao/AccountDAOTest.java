package com.banking.dao;

import com.banking.model.Account;
import com.banking.model.AccountType;
import com.banking.model.Client;
import org.junit.jupiter.api.*;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link AccountDAO}.
 * Uses an isolated in-memory SQLite database so production data is never touched.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AccountDAOTest {

    private static Connection conn;
    private static ClientDAO  clientDAO;
    private static AccountDAO accountDAO;
    private static int        testClientId;

    /**
     * Sets up an in-memory SQLite database with clients and accounts tables,
     * and inserts one client to use as a foreign key in account tests.
     *
     * @throws Exception if connection or schema setup fails
     */
    @BeforeAll
    static void setup() throws Exception {
        conn = DriverManager.getConnection("jdbc:sqlite::memory:");
        try (Statement st = conn.createStatement()) {
            st.execute("""
                CREATE TABLE IF NOT EXISTS clients (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    first_name TEXT NOT NULL, last_name TEXT NOT NULL,
                    email TEXT NOT NULL UNIQUE, phone TEXT, address TEXT,
                    birth_date TEXT,
                    created_at TEXT NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%S','now'))
                )
                """);
            st.execute("""
                CREATE TABLE IF NOT EXISTS accounts (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    account_number TEXT NOT NULL UNIQUE,
                    client_id INTEGER NOT NULL,
                    account_type TEXT NOT NULL CHECK(account_type IN ('CHECKING','SAVINGS','CREDIT')),
                    balance REAL NOT NULL DEFAULT 0.0,
                    overdraft_limit REAL NOT NULL DEFAULT 0.0,
                    interest_rate REAL NOT NULL DEFAULT 0.0,
                    is_active INTEGER NOT NULL DEFAULT 1,
                    opened_at TEXT NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%S','now')),
                    FOREIGN KEY (client_id) REFERENCES clients(id)
                )
                """);
        }

        DatabaseManager.setTestConnection(conn);
        clientDAO  = new ClientDAO();
        accountDAO = new AccountDAO();

        // Insert one test client that accounts will reference
        Client c = new Client(0, "Test", "Client", "test.client@test.com",
                              "0600000000", "Addr", LocalDate.of(1990, 1, 1), LocalDateTime.now());
        clientDAO.insert(c);
        testClientId = c.getId();
    }

    /**
     * Removes all accounts between tests to keep each test independent.
     *
     * @throws Exception if the DELETE statement fails
     */
    @BeforeEach
    void clearAccounts() throws Exception {
        try (Statement st = conn.createStatement()) {
            st.execute("DELETE FROM accounts");
        }
    }

    /** Closes the in-memory connection after all tests. */
    @AfterAll
    static void teardown() throws Exception {
        if (conn != null && !conn.isClosed()) conn.close();
        DatabaseManager.clearTestConnection();
    }

    // ──────────────────────────── CREATE ────────────────────────────

    /**
     * Tests that insert() persists an account and sets its generated id.
     */
    @Test
    @Order(1)
    void testInsert_setsGeneratedId() throws Exception {
        Account a = buildAccount("FR76-TEST-0001", testClientId, AccountType.CHECKING, 1000.0);
        accountDAO.insert(a);
        assertTrue(a.getId() > 0, "insert() should set a positive generated id");
    }

    /**
     * Tests that insert() with a duplicate account number throws a SQLException.
     */
    @Test
    @Order(2)
    void testInsert_duplicateAccountNumber_throws() throws Exception {
        Account a1 = buildAccount("FR76-DUP-0001", testClientId, AccountType.CHECKING, 500.0);
        Account a2 = buildAccount("FR76-DUP-0001", testClientId, AccountType.SAVINGS,  800.0);
        accountDAO.insert(a1);
        assertThrows(Exception.class, () -> accountDAO.insert(a2),
            "Duplicate account number should throw an exception");
    }

    // ──────────────────────────── READ ──────────────────────────────

    /**
     * Tests that findAll() returns all inserted accounts.
     */
    @Test
    @Order(3)
    void testFindAll_returnsAll() throws Exception {
        accountDAO.insert(buildAccount("FR76-0001-0001", testClientId, AccountType.CHECKING, 1000.0));
        accountDAO.insert(buildAccount("FR76-0001-0002", testClientId, AccountType.SAVINGS,  5000.0));
        List<Account> all = accountDAO.findAll();
        assertEquals(2, all.size(), "findAll() should return 2 accounts");
    }

    /**
     * Tests that findByClientId() returns only accounts belonging to the given client.
     */
    @Test
    @Order(4)
    void testFindByClientId_returnsClientAccounts() throws Exception {
        accountDAO.insert(buildAccount("FR76-0001-0001", testClientId, AccountType.CHECKING, 200.0));
        accountDAO.insert(buildAccount("FR76-0001-0002", testClientId, AccountType.SAVINGS,  300.0));
        List<Account> accounts = accountDAO.findByClientId(testClientId);
        assertEquals(2, accounts.size(), "findByClientId() should return 2 accounts for this client");
    }

    /**
     * Tests that findById() returns the correct account.
     */
    @Test
    @Order(5)
    void testFindById_returnsCorrectAccount() throws Exception {
        Account a = buildAccount("FR76-0001-0001", testClientId, AccountType.CREDIT, 0.0);
        accountDAO.insert(a);
        Optional<Account> found = accountDAO.findById(a.getId());
        assertTrue(found.isPresent(), "findById() should find the inserted account");
        assertEquals(AccountType.CREDIT, found.get().getAccountType());
    }

    // ──────────────────────────── UPDATE ────────────────────────────

    /**
     * Tests that updateBalance() correctly changes the account balance.
     */
    @Test
    @Order(6)
    void testUpdateBalance_changesBalance() throws Exception {
        Account a = buildAccount("FR76-0001-0001", testClientId, AccountType.CHECKING, 100.0);
        accountDAO.insert(a);
        accountDAO.updateBalance(a.getId(), 999.99);
        Account updated = accountDAO.findById(a.getId()).orElseThrow();
        assertEquals(999.99, updated.getBalance(), 0.001, "Balance should be updated to 999.99");
    }

    /**
     * Tests that update() persists changes to account type and overdraft limit.
     */
    @Test
    @Order(7)
    void testUpdate_persistsMetadataChanges() throws Exception {
        Account a = buildAccount("FR76-0001-0001", testClientId, AccountType.CHECKING, 0.0);
        accountDAO.insert(a);

        a.setOverdraftLimit(500.0);
        a.setInterestRate(3.5);
        a.setIsActive(false);
        accountDAO.update(a);

        Account updated = accountDAO.findById(a.getId()).orElseThrow();
        assertEquals(500.0, updated.getOverdraftLimit(), 0.001);
        assertEquals(3.5,   updated.getInterestRate(),   0.001);
        assertFalse(updated.isActive(), "Account should be inactive after update");
    }

    // ──────────────────────────── DELETE ────────────────────────────

    /**
     * Tests that delete() removes the account from the database.
     */
    @Test
    @Order(8)
    void testDelete_removesAccount() throws Exception {
        Account a = buildAccount("FR76-DEL-0001", testClientId, AccountType.SAVINGS, 0.0);
        accountDAO.insert(a);
        accountDAO.delete(a.getId());
        assertFalse(accountDAO.findById(a.getId()).isPresent(),
            "Account should not exist after delete()");
    }

    // ──────────────────────────── STATS ─────────────────────────────

    /**
     * Tests that totalAssetsUnderManagement() sums balances of active accounts only.
     */
    @Test
    @Order(9)
    void testTotalAssets_sumsActiveBalancesOnly() throws Exception {
        Account active   = buildAccount("FR76-ACT-0001", testClientId, AccountType.CHECKING, 1000.0);
        Account inactive = buildAccount("FR76-INA-0001", testClientId, AccountType.SAVINGS,  500.0);
        inactive.setIsActive(false);

        accountDAO.insert(active);
        accountDAO.insert(inactive);
        accountDAO.update(inactive);

        double total = accountDAO.totalAssetsUnderManagement();
        assertEquals(1000.0, total, 0.001,
            "Only the active account balance should be counted");
    }

    /**
     * Tests that generateAccountNumber() produces the correct format.
     */
    @Test
    @Order(10)
    void testGenerateAccountNumber_correctFormat() throws Exception {
        String number = accountDAO.generateAccountNumber(testClientId);
        assertTrue(number.startsWith("FR76-"), "Account number should start with FR76-");
    }

    // ──────────────────────────── HELPER ────────────────────────────

    private Account buildAccount(String number, int clientId, AccountType type, double balance) {
        return new Account(0, number, clientId, "Test Client",
                           type, balance, 0.0, 0.0, true, LocalDateTime.now());
    }
}
