package com.banking.model;

import javafx.beans.property.*;
import java.time.LocalDateTime;

/**
 * Represents a bank account linked to a client.
 * Uses JavaFX properties for direct {@link javafx.scene.control.TableView} binding.
 * Balance is only modified through {@link com.banking.dao.TransactionDAO} to
 * ensure atomicity with the transaction record.
 */
public class Account {

    private final IntegerProperty id;
    private final StringProperty accountNumber;
    private final IntegerProperty clientId;
    private final StringProperty clientName;   // denormalized from JOIN for display
    private final ObjectProperty<AccountType> accountType;
    private final DoubleProperty balance;
    private final DoubleProperty overdraftLimit;
    private final DoubleProperty interestRate;
    private final BooleanProperty isActive;
    private final ObjectProperty<LocalDateTime> openedAt;

    /** Creates an empty account for use in "new account" forms. */
    public Account() {
        this(0, "", 0, "", AccountType.CHECKING, 0.0, 0.0, 0.0, true, LocalDateTime.now());
    }

    /**
     * Creates an account with all fields.
     *
     * @param id             primary key
     * @param accountNumber  unique account number (e.g. FR76-0001-0001)
     * @param clientId       foreign key to clients table
     * @param clientName     denormalized client full name for display
     * @param accountType    account type enum
     * @param balance        current balance
     * @param overdraftLimit allowed negative balance (0 for no overdraft)
     * @param interestRate   annual interest rate in % (used for SAVINGS)
     * @param isActive       whether the account is open
     * @param openedAt       account opening timestamp
     */
    public Account(int id, String accountNumber, int clientId, String clientName,
                   AccountType accountType, double balance, double overdraftLimit,
                   double interestRate, boolean isActive, LocalDateTime openedAt) {
        this.id             = new SimpleIntegerProperty(id);
        this.accountNumber  = new SimpleStringProperty(accountNumber);
        this.clientId       = new SimpleIntegerProperty(clientId);
        this.clientName     = new SimpleStringProperty(clientName);
        this.accountType    = new SimpleObjectProperty<>(accountType);
        this.balance        = new SimpleDoubleProperty(balance);
        this.overdraftLimit = new SimpleDoubleProperty(overdraftLimit);
        this.interestRate   = new SimpleDoubleProperty(interestRate);
        this.isActive       = new SimpleBooleanProperty(isActive);
        this.openedAt       = new SimpleObjectProperty<>(openedAt);
    }

    // --- Property accessors ---

    /** @return the id property */
    public IntegerProperty idProperty() { return id; }

    /** @return the accountNumber property */
    public StringProperty accountNumberProperty() { return accountNumber; }

    /** @return the clientId property */
    public IntegerProperty clientIdProperty() { return clientId; }

    /** @return the clientName property */
    public StringProperty clientNameProperty() { return clientName; }

    /** @return the accountType property */
    public ObjectProperty<AccountType> accountTypeProperty() { return accountType; }

    /** @return the balance property */
    public DoubleProperty balanceProperty() { return balance; }

    /** @return the overdraftLimit property */
    public DoubleProperty overdraftLimitProperty() { return overdraftLimit; }

    /** @return the interestRate property */
    public DoubleProperty interestRateProperty() { return interestRate; }

    /** @return the isActive property */
    public BooleanProperty isActiveProperty() { return isActive; }

    /** @return the openedAt property */
    public ObjectProperty<LocalDateTime> openedAtProperty() { return openedAt; }

    // --- Getters ---

    /** @return the account database id */
    public int getId() { return id.get(); }

    /** @return the unique account number */
    public String getAccountNumber() { return accountNumber.get(); }

    /** @return the owning client's id */
    public int getClientId() { return clientId.get(); }

    /** @return the owning client's full name */
    public String getClientName() { return clientName.get(); }

    /** @return the account type */
    public AccountType getAccountType() { return accountType.get(); }

    /** @return the current balance */
    public double getBalance() { return balance.get(); }

    /** @return the maximum allowed overdraft (positive number) */
    public double getOverdraftLimit() { return overdraftLimit.get(); }

    /** @return the annual interest rate in % */
    public double getInterestRate() { return interestRate.get(); }

    /** @return true if the account is open */
    public boolean isActive() { return isActive.get(); }

    /** @return the account opening timestamp */
    public LocalDateTime getOpenedAt() { return openedAt.get(); }

    // --- Setters ---

    /** @param v the new id (set after DB insert) */
    public void setId(int v) { id.set(v); }

    /** @param v the new account number */
    public void setAccountNumber(String v) { accountNumber.set(v); }

    /** @param v the new client id */
    public void setClientId(int v) { clientId.set(v); }

    /** @param v the new client name */
    public void setClientName(String v) { clientName.set(v); }

    /** @param v the new account type */
    public void setAccountType(AccountType v) { accountType.set(v); }

    /** @param v the new balance */
    public void setBalance(double v) { balance.set(v); }

    /** @param v the new overdraft limit */
    public void setOverdraftLimit(double v) { overdraftLimit.set(v); }

    /** @param v the new interest rate */
    public void setInterestRate(double v) { interestRate.set(v); }

    /** @param v true to activate, false to deactivate */
    public void setIsActive(boolean v) { isActive.set(v); }

    /** @param v the new opening timestamp */
    public void setOpenedAt(LocalDateTime v) { openedAt.set(v); }

    @Override
    public String toString() {
        return accountNumber.get() + " (" + (accountType.get() != null ? accountType.get().getDisplayName() : "") + ")";
    }
}
