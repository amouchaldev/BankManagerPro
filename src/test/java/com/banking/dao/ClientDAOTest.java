package com.banking.dao;

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
 * Unit tests for {@link ClientDAO}.
 * Uses an isolated in-memory SQLite database so production data is never touched.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ClientDAOTest {

    private static Connection conn;
    private static ClientDAO dao;

    /**
     * Initialises a fresh in-memory SQLite database and creates the clients table before all tests.
     *
     * @throws Exception if connection or schema setup fails
     */
    @BeforeAll
    static void setup() throws Exception {
        conn = DriverManager.getConnection("jdbc:sqlite::memory:");
        try (Statement st = conn.createStatement()) {
            st.execute("""
                CREATE TABLE IF NOT EXISTS clients (
                    id         INTEGER PRIMARY KEY AUTOINCREMENT,
                    first_name TEXT NOT NULL,
                    last_name  TEXT NOT NULL,
                    email      TEXT NOT NULL UNIQUE,
                    phone      TEXT,
                    address    TEXT,
                    birth_date TEXT,
                    created_at TEXT NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%S','now'))
                )
                """);
        }

        // Point DatabaseManager singleton to this test connection
        DatabaseManager.setTestConnection(conn);
        dao = new ClientDAO();
    }

    /**
     * Cleans the clients table between tests to keep them independent.
     *
     * @throws Exception if the DELETE statement fails
     */
    @BeforeEach
    void clearTable() throws Exception {
        try (Statement st = conn.createStatement()) {
            st.execute("DELETE FROM clients");
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
     * Tests that insert() persists a client and sets its generated id.
     */
    @Test
    @Order(1)
    void testInsert_setsGeneratedId() throws Exception {
        Client c = buildClient("Moustapha", "Amouchal", "m.amouchal@test.com");
        dao.insert(c);
        assertTrue(c.getId() > 0, "insert() should set a positive generated id");
    }

    /**
     * Tests that insert() with a duplicate email throws a SQLException.
     */
    @Test
    @Order(2)
    void testInsert_duplicateEmail_throwsSQLException() throws Exception {
        Client c1 = buildClient("Alice", "Martin", "duplicate@test.com");
        Client c2 = buildClient("Bob",   "Dupont", "duplicate@test.com");
        dao.insert(c1);
        assertThrows(Exception.class, () -> dao.insert(c2),
            "Duplicate email should throw an exception");
    }

    // ──────────────────────────── READ ──────────────────────────────

    /**
     * Tests that findAll() returns all inserted clients.
     */
    @Test
    @Order(3)
    void testFindAll_returnsAllClients() throws Exception {
        dao.insert(buildClient("Alice", "Martin", "alice@test.com"));
        dao.insert(buildClient("Bob",   "Dupont", "bob@test.com"));
        List<Client> all = dao.findAll();
        assertEquals(2, all.size(), "findAll() should return 2 clients");
    }

    /**
     * Tests that findById() returns the correct client.
     */
    @Test
    @Order(4)
    void testFindById_returnsCorrectClient() throws Exception {
        Client c = buildClient("Fatima", "Benali", "fatima@test.com");
        dao.insert(c);
        Optional<Client> found = dao.findById(c.getId());
        assertTrue(found.isPresent(), "findById() should find the inserted client");
        assertEquals("Fatima", found.get().getFirstName());
    }

    /**
     * Tests that findById() with an unknown id returns empty.
     */
    @Test
    @Order(5)
    void testFindById_unknownId_returnsEmpty() throws Exception {
        Optional<Client> found = dao.findById(9999);
        assertFalse(found.isPresent(), "findById() with unknown id should return empty Optional");
    }

    /**
     * Tests that search() matches on first name, last name, and email.
     */
    @Test
    @Order(6)
    void testSearch_matchesNameAndEmail() throws Exception {
        dao.insert(buildClient("Youssef", "Idrissi",  "youssef@test.com"));
        dao.insert(buildClient("Karim",   "Wazzani",  "karim@test.com"));
        dao.insert(buildClient("Amina",   "Ziani",    "amina@test.com"));

        List<Client> byName  = dao.search("Youssef");
        List<Client> byEmail = dao.search("karim@test");
        List<Client> noMatch = dao.search("nobody_xyz");

        assertEquals(1, byName.size(),  "search('Youssef') should return 1 result");
        assertEquals(1, byEmail.size(), "search by email should return 1 result");
        assertEquals(0, noMatch.size(), "search with no match should return empty list");
    }

    // ──────────────────────────── UPDATE ────────────────────────────

    /**
     * Tests that update() persists changed fields.
     */
    @Test
    @Order(7)
    void testUpdate_persistsChanges() throws Exception {
        Client c = buildClient("OldFirst", "OldLast", "update@test.com");
        dao.insert(c);

        c.setFirstName("NewFirst");
        c.setLastName("NewLast");
        dao.update(c);

        Client updated = dao.findById(c.getId()).orElseThrow();
        assertEquals("NewFirst", updated.getFirstName(), "First name should be updated");
        assertEquals("NewLast",  updated.getLastName(),  "Last name should be updated");
    }

    // ──────────────────────────── DELETE ────────────────────────────

    /**
     * Tests that delete() removes the client from the database.
     */
    @Test
    @Order(8)
    void testDelete_removesClient() throws Exception {
        Client c = buildClient("ToDelete", "User", "delete@test.com");
        dao.insert(c);
        dao.delete(c.getId());
        assertFalse(dao.findById(c.getId()).isPresent(),
            "Client should not exist after delete()");
    }

    // ──────────────────────────── COUNT ─────────────────────────────

    /**
     * Tests that countAll() returns the correct number of clients.
     */
    @Test
    @Order(9)
    void testCountAll_returnsCorrectCount() throws Exception {
        assertEquals(0, dao.countAll(), "countAll() should be 0 on empty table");
        dao.insert(buildClient("A", "B", "ab@test.com"));
        dao.insert(buildClient("C", "D", "cd@test.com"));
        assertEquals(2, dao.countAll(), "countAll() should be 2 after two inserts");
    }

    // ──────────────────────────── HELPER ────────────────────────────

    private Client buildClient(String first, String last, String email) {
        return new Client(0, first, last, email, "0600000000", "Test Address",
                          LocalDate.of(1995, 1, 1), LocalDateTime.now());
    }
}
