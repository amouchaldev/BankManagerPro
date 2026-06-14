# BankManager Pro

A desktop banking management system built with JavaFX and SQLite for the Java Programming.

---

## What the app does

BankManager Pro lets you manage clients, bank accounts, and financial transactions from a clean desktop interface. Everything runs locally, no internet or external server needed.

The app has four main sections:

- **Dashboard** — statistics overview (total clients, accounts, assets) and an interactive balance history chart
- **Clients** — add, edit, search, and delete clients
- **Accounts** — create and manage bank accounts (Checking, Savings, Credit) linked to clients
- **Transactions** — perform deposits, withdrawals, and transfers with a full history log

---

## Tech stack

| Technology | Purpose | Version |
|---|---|---|
| Java | Main language | 25 (source level 21) |
| JavaFX | Desktop UI framework | 21.0.2 |
| SQLite | Local database (single file) | 3.45.1 |
| Maven | Build tool and dependency manager | 3.9.6 |
| JUnit 5 | Unit tests | 5.10.2 |

---

## Project structure

```
mini-projet-java/
├── pom.xml
├── banking.db                        # created automatically on first run
├── rapport_bankmanager.pdf           # project report
├── screenshots/                      # app screenshots
└── src/
    ├── main/
    │   ├── java/com/banking/
    │   │   ├── BankingApplication.java       # entry point
    │   │   ├── model/
    │   │   │   ├── Client.java
    │   │   │   ├── Account.java
    │   │   │   ├── Transaction.java
    │   │   │   ├── AccountType.java           # enum: CHECKING, SAVINGS, CREDIT
    │   │   │   └── TransactionType.java       # enum: DEPOSIT, WITHDRAWAL, TRANSFER
    │   │   ├── dao/
    │   │   │   ├── DatabaseManager.java       # singleton JDBC connection
    │   │   │   ├── ClientDAO.java
    │   │   │   ├── AccountDAO.java
    │   │   │   └── TransactionDAO.java
    │   │   ├── controller/
    │   │   │   ├── MainController.java        # sidebar navigation
    │   │   │   ├── DashboardController.java
    │   │   │   ├── ClientController.java
    │   │   │   ├── AccountController.java
    │   │   │   └── TransactionController.java
    │   │   └── util/
    │   │       ├── AlertHelper.java
    │   │       ├── FormValidator.java
    │   │       └── CurrencyFormatter.java
    │   └── resources/com/banking/
    │       ├── fxml/
    │       │   ├── main.fxml
    │       │   ├── dashboard.fxml
    │       │   ├── clients.fxml
    │       │   ├── accounts.fxml
    │       │   └── transactions.fxml
    │       ├── css/
    │       │   └── banking.css
    │       └── database/
    │           └── schema.sql                 # tables + seed data
    └── test/
        └── java/com/banking/dao/
```

---

## Requirements

Before running the project, make sure you have the following installed:

- **Java 21 or higher** — [download from oracle.com](https://www.oracle.com/java/technologies/downloads/)
- **Maven 3.6 or higher** — [download from maven.apache.org](https://maven.apache.org/download.cgi)

You do not need to install SQLite separately. The JDBC driver is included as a Maven dependency and the database file is created automatically.

---

## Getting started

### 1. Clone or download the project

If you have Git:

```bash
git clone git@github.com:amouchaldev/BankManagerPro.git
cd mini-projet-java
```

Or just download and extract the ZIP, then open a terminal in the project folder.

### 2. Check your Java version

```bash
java -version
```

You should see Java 21 or higher. If not, install it first.

### 3. Install Maven (if not already installed)

**macOS (Homebrew):**

```bash
brew install maven
```

**macOS (manual):**

```bash
curl -O https://archive.apache.org/dist/maven/maven-3/3.9.6/binaries/apache-maven-3.9.6-bin.tar.gz
tar -xzf apache-maven-3.9.6-bin.tar.gz
```

Then use `./apache-maven-3.9.6/bin/mvn` instead of `mvn` in the commands below.

**Windows:**

Download the zip from [maven.apache.org](https://maven.apache.org/download.cgi), extract it, and add the `bin/` folder to your PATH.

**Linux:**

```bash
sudo apt install maven        # Ubuntu/Debian
sudo dnf install maven        # Fedora
```

### 4. Run the application

```bash
mvn javafx:run
```

That's it. Maven will automatically download all dependencies (JavaFX, SQLite JDBC driver) on the first run. The database file `banking.db` will be created in the project root with sample data already loaded.

---

## Sample data

The app starts with pre-loaded test data so you can explore all features immediately:

- **10 clients** — mix of French and Moroccan names (Alice Martin, Mohamed Al-Omari, Fatima Benali, Youssef Idrissi, etc.)
- **16 bank accounts** — spread across all clients, all three account types
- **30 transactions** — deposits, withdrawals, and transfers with realistic descriptions

To reset the database to this initial state, just delete `banking.db` and restart the app:

```bash
rm banking.db
mvn javafx:run
```

---

## Other useful commands

```bash
# Compile only (check for errors)
mvn compile

# Run tests
mvn test

# Generate Javadoc
mvn javadoc:javadoc
# Output: target/site/apidocs/index.html

# Build a standalone JAR
mvn package
# Output: target/banking-app-1.0.0-shaded.jar
```

---

## Database

The database is a single SQLite file (`banking.db`) with three tables:

```
clients
  id, first_name, last_name, email, phone, address, birth_date, created_at

accounts
  id, account_number, client_id, account_type, balance,
  overdraft_limit, interest_rate, is_active, opened_at

transactions
  id, source_account_id, target_account_id, transaction_type,
  amount, description, transaction_date
```

The schema is defined in `src/main/resources/com/banking/database/schema.sql`. It runs automatically every time the app starts (all statements use `CREATE TABLE IF NOT EXISTS` so existing data is never overwritten).

---

## Architecture

The code follows the **MVC pattern** combined with the **DAO pattern**:

- **Model** — plain Java classes with JavaFX properties for direct TableView binding
- **DAO** — one class per table, handles all SQL using PreparedStatements
- **Controller** — JavaFX controllers that connect the UI to the DAO layer
- **DatabaseManager** — singleton that holds the shared JDBC connection and runs the schema on startup

Financial operations (deposit, withdrawal, transfer) use JDBC transactions (`setAutoCommit(false)`) to make sure the account balance update and the transaction record are always saved together. If anything fails, everything is rolled back.

---

## Common issues

**App does not start / blank window**

Make sure you are using `mvn javafx:run` and not `java -jar`. JavaFX requires specific module-path configuration that the Maven plugin handles automatically.

**"banking.db" permission error**

The database file is created in the current working directory when you run Maven. Make sure you run the command from inside the project folder.

**Maven not found**

If `mvn` is not recognized, use the full path to the Maven binary you downloaded. For example:

```bash
/Users/yourname/apache-maven-3.9.6/bin/mvn javafx:run
```

**Slow first start**

The first run downloads JavaFX and SQLite JARs from Maven Central (~60 MB total). Subsequent runs start instantly from the local cache.

---

## Deliverables (for submission)

The ZIP archive should contain:

```
mini-projet-java.zip
├── src/                    # full source code
├── pom.xml                 # Maven build file
├── schema.sql              # copy of the database schema
├── rapport_bankmanager.pdf # project report with screenshots
└── README.md               # this file
```

To copy the schema to the project root for easy access:

```bash
cp src/main/resources/com/banking/database/schema.sql schema.sql
```

---
