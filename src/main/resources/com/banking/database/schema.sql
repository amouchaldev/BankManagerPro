-- ============================================================
-- BankManager Pro -- SQLite Schema
-- ============================================================

PRAGMA foreign_keys = ON;
PRAGMA journal_mode = WAL;

-- ----------------------------
-- Table: clients
-- ----------------------------
CREATE TABLE IF NOT EXISTS clients (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    first_name  TEXT    NOT NULL,
    last_name   TEXT    NOT NULL,
    email       TEXT    NOT NULL UNIQUE,
    phone       TEXT,
    address     TEXT,
    birth_date  TEXT,
    created_at  TEXT    NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%S', 'now'))
);

-- ----------------------------
-- Table: accounts
-- ----------------------------
CREATE TABLE IF NOT EXISTS accounts (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    account_number  TEXT    NOT NULL UNIQUE,
    client_id       INTEGER NOT NULL,
    account_type    TEXT    NOT NULL
                    CHECK(account_type IN ('CHECKING','SAVINGS','CREDIT')),
    balance         REAL    NOT NULL DEFAULT 0.0,
    overdraft_limit REAL    NOT NULL DEFAULT 0.0,
    interest_rate   REAL    NOT NULL DEFAULT 0.0,
    is_active       INTEGER NOT NULL DEFAULT 1,
    opened_at       TEXT    NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%S', 'now')),
    FOREIGN KEY (client_id) REFERENCES clients(id) ON DELETE CASCADE
);

-- ----------------------------
-- Table: transactions
-- ----------------------------
CREATE TABLE IF NOT EXISTS transactions (
    id                  INTEGER PRIMARY KEY AUTOINCREMENT,
    source_account_id   INTEGER,
    target_account_id   INTEGER,
    transaction_type    TEXT    NOT NULL
                        CHECK(transaction_type IN ('DEPOSIT','WITHDRAWAL','TRANSFER')),
    amount              REAL    NOT NULL CHECK(amount > 0),
    description         TEXT,
    transaction_date    TEXT    NOT NULL DEFAULT (strftime('%Y-%m-%dT%H:%M:%S', 'now')),
    FOREIGN KEY (source_account_id) REFERENCES accounts(id),
    FOREIGN KEY (target_account_id) REFERENCES accounts(id)
);

-- ----------------------------
-- Indexes
-- ----------------------------
CREATE INDEX IF NOT EXISTS idx_accounts_client   ON accounts(client_id);
CREATE INDEX IF NOT EXISTS idx_transactions_src  ON transactions(source_account_id);
CREATE INDEX IF NOT EXISTS idx_transactions_tgt  ON transactions(target_account_id);
CREATE INDEX IF NOT EXISTS idx_transactions_date ON transactions(transaction_date);

-- ----------------------------
-- Seed data
-- ----------------------------
INSERT OR IGNORE INTO clients (id, first_name, last_name, email, phone, address, birth_date)
VALUES
    (1,  'Alice',       'Martin',      'alice.martin@email.com',       '0612345678', '12 Rue de la Paix, Paris',          '1985-03-22'),
    (2,  'Bob',         'Dupont',      'bob.dupont@email.com',         '0698765432', '5 Avenue Foch, Lyon',               '1990-07-15'),
    (3,  'Claire',      'Bernard',     'claire.bernard@email.com',     '0655443322', '8 Boulevard Haussmann, Paris',      '1978-11-01'),
    (4,  'Mohamed',      'Al-Omari',    'mohammed.alomari@email.com',    '0661234567', '23 Rue Mohammed V, Rabat',          '1988-06-14'),
    (5,  'Fatima',       'Benali',      'fatima.benali@email.com',       '0672345678', '47 Rue des Libertes, Casablanca',   '1993-11-30'),
    (6,  'Youssef',      'Idrissi',     'youssef.idrissi@email.com',     '0683456789', '15 Rue Hassan II, Fes',             '1982-04-05'),
    (7,  'Amina',        'Ziani',       'amina.ziani@email.com',         '0694567890', '9 Derb El Fquih, Marrakech',        '1996-08-19'),
    (8,  'Abderrahman',  'Bensalem',    'abderrahman.bensalem@email.com','0635678901', '31 Route de l Unite, Tanger',       '1975-02-28'),
    (9,  'Khadija',      'Mansouri',    'khadija.mansouri@email.com',    '0626789012', '6 Hay Riad, Agadir',                '1991-09-12'),
    (10, 'Karim',        'Wazzani',     'karim.wazzani@email.com',       '0617890123', '18 Rue Istiqlal, Meknes',           '1987-12-03');

INSERT OR IGNORE INTO accounts (id, account_number, client_id, account_type, balance, overdraft_limit, interest_rate)
VALUES
    (1,  'FR76-0001-0001', 1,  'CHECKING',   3500.00,   500.00,  0.00),
    (2,  'FR76-0001-0002', 1,  'SAVINGS',   12000.00,     0.00,  2.50),
    (3,  'FR76-0002-0001', 2,  'CHECKING',    850.00,   200.00,  0.00),
    (4,  'FR76-0002-0002', 2,  'CREDIT',        0.00,  3000.00, 15.90),
    (5,  'FR76-0003-0001', 3,  'SAVINGS',   45000.00,     0.00,  3.00),
    (6,  'FR76-0004-0001', 4,  'CHECKING',   8200.00,  1000.00,  0.00),
    (7,  'FR76-0004-0002', 4,  'SAVINGS',   25000.00,     0.00,  3.50),
    (8,  'FR76-0005-0001', 5,  'CHECKING',   1350.00,   300.00,  0.00),
    (9,  'FR76-0006-0001', 6,  'SAVINGS',   62000.00,     0.00,  4.00),
    (10, 'FR76-0006-0002', 6,  'CREDIT',        0.00,  5000.00, 12.50),
    (11, 'FR76-0007-0001', 7,  'CHECKING',   2900.00,   500.00,  0.00),
    (12, 'FR76-0008-0001', 8,  'CHECKING',  15400.00,  2000.00,  0.00),
    (13, 'FR76-0008-0002', 8,  'SAVINGS',   38000.00,     0.00,  3.00),
    (14, 'FR76-0009-0001', 9,  'CHECKING',    670.00,   200.00,  0.00),
    (15, 'FR76-0010-0001', 10, 'SAVINGS',   19500.00,     0.00,  2.75),
    (16, 'FR76-0010-0002', 10, 'CREDIT',        0.00,  4000.00, 14.00);

INSERT OR IGNORE INTO transactions (id, source_account_id, target_account_id, transaction_type, amount, description, transaction_date)
VALUES
    (1,  NULL, 1,  'DEPOSIT',     1000.00, 'Salaire Janvier',          '2026-01-05T09:00:00'),
    (2,  NULL, 1,  'DEPOSIT',     1500.00, 'Salaire Fevrier',          '2026-02-05T09:00:00'),
    (3,  1,    NULL,'WITHDRAWAL',  200.00, 'Retrait DAB',              '2026-02-10T14:30:00'),
    (4,  1,    2,  'TRANSFER',    500.00, 'Virement epargne',          '2026-02-15T11:00:00'),
    (5,  NULL, 3,  'DEPOSIT',    3000.00, 'Salaire Mars',              '2026-03-05T09:00:00'),
    (6,  3,    NULL,'WITHDRAWAL',  150.00, 'Courses',                  '2026-03-12T16:00:00'),
    (7,  NULL, 2,  'DEPOSIT',    5000.00, 'Bonus annuel',              '2026-04-01T10:00:00'),
    (8,  NULL, 6,  'DEPOSIT',    8200.00, 'Salaire Janvier',           '2026-01-03T08:30:00'),
    (9,  6,    7,  'TRANSFER',   2000.00, 'Virement epargne',          '2026-01-20T10:00:00'),
    (10, NULL, 7,  'DEPOSIT',    5000.00, 'Bonus annuel',              '2026-02-01T09:00:00'),
    (11, 7,    NULL,'WITHDRAWAL',  800.00, 'Retrait DAB',              '2026-02-18T15:00:00'),
    (12, NULL, 8,  'DEPOSIT',    1350.00, 'Salaire Fevrier',           '2026-02-05T08:00:00'),
    (13, 8,    NULL,'WITHDRAWAL',  200.00, 'Facture electricite',      '2026-02-20T11:30:00'),
    (14, NULL, 9,  'DEPOSIT',   15000.00, 'Depot initial',             '2026-01-10T09:00:00'),
    (15, NULL, 9,  'DEPOSIT',   10000.00, 'Salaire Mars',              '2026-03-05T08:30:00'),
    (16, 9,    11, 'TRANSFER',   3000.00, 'Aide familiale',            '2026-03-15T12:00:00'),
    (17, NULL, 11, 'DEPOSIT',    2900.00, 'Salaire Avril',             '2026-04-03T08:00:00'),
    (18, 11,   NULL,'WITHDRAWAL', 450.00, 'Loyer',                    '2026-04-10T09:00:00'),
    (19, NULL, 12, 'DEPOSIT',   15400.00, 'Salaire Mars',              '2026-03-04T08:00:00'),
    (20, 12,   13, 'TRANSFER',   5000.00, 'Virement epargne',         '2026-03-20T10:30:00'),
    (21, NULL, 13, 'DEPOSIT',    8000.00, 'Dividendes investissement', '2026-04-05T09:00:00'),
    (22, NULL, 14, 'DEPOSIT',     670.00, 'Salaire Avril',             '2026-04-04T08:00:00'),
    (23, 14,   NULL,'WITHDRAWAL', 100.00, 'Achats divers',             '2026-04-08T14:00:00'),
    (24, NULL, 15, 'DEPOSIT',    4500.00, 'Salaire Mars',              '2026-03-06T08:00:00'),
    (25, 15,   NULL,'WITHDRAWAL',1200.00, 'Remboursement pret',        '2026-03-25T10:00:00'),
    (26, 15,   16, 'TRANSFER',   2000.00, 'Remboursement carte',       '2026-04-15T11:00:00'),
    (27, 6,    NULL,'WITHDRAWAL', 500.00, 'Shopping',                  '2026-04-20T16:00:00'),
    (28, NULL, 6,  'DEPOSIT',    8200.00, 'Salaire Mai',               '2026-05-03T08:30:00'),
    (29, 12,   NULL,'WITHDRAWAL',2000.00, 'Reparation voiture',        '2026-05-10T09:00:00'),
    (30, NULL, 2,  'DEPOSIT',    1200.00, 'Interets epargne',          '2026-05-01T00:00:00')
