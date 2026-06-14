package com.banking.model;

import javafx.beans.property.*;
import java.time.LocalDateTime;

/**
 * Represents a financial transaction.
 * Transactions are immutable once created — never update, only insert.
 *
 * <ul>
 *   <li>DEPOSIT: sourceAccountId = 0 (money from outside), targetAccountId set</li>
 *   <li>WITHDRAWAL: sourceAccountId set, targetAccountId = 0 (money leaves)</li>
 *   <li>TRANSFER: both sourceAccountId and targetAccountId are set</li>
 * </ul>
 */
public class Transaction {

    private final IntegerProperty id;
    private final IntegerProperty sourceAccountId;   // 0 means NULL/external
    private final IntegerProperty targetAccountId;   // 0 means NULL/external
    private final StringProperty sourceAccountNumber; // for display
    private final StringProperty targetAccountNumber; // for display
    private final ObjectProperty<TransactionType> transactionType;
    private final DoubleProperty amount;
    private final StringProperty description;
    private final ObjectProperty<LocalDateTime> transactionDate;

    /** Creates an empty transaction for form initialization. */
    public Transaction() {
        this(0, 0, 0, "", "", TransactionType.DEPOSIT, 0.0, "", LocalDateTime.now());
    }

    /**
     * Creates a transaction with all fields.
     *
     * @param id                    primary key
     * @param sourceAccountId       source account id, or 0 for external
     * @param targetAccountId       target account id, or 0 for external
     * @param sourceAccountNumber   source account number for display
     * @param targetAccountNumber   target account number for display
     * @param transactionType       the transaction type
     * @param amount                positive transaction amount
     * @param description           optional memo / label
     * @param transactionDate       when the transaction occurred
     */
    public Transaction(int id, int sourceAccountId, int targetAccountId,
                       String sourceAccountNumber, String targetAccountNumber,
                       TransactionType transactionType, double amount,
                       String description, LocalDateTime transactionDate) {
        this.id                  = new SimpleIntegerProperty(id);
        this.sourceAccountId     = new SimpleIntegerProperty(sourceAccountId);
        this.targetAccountId     = new SimpleIntegerProperty(targetAccountId);
        this.sourceAccountNumber = new SimpleStringProperty(sourceAccountNumber != null ? sourceAccountNumber : "");
        this.targetAccountNumber = new SimpleStringProperty(targetAccountNumber != null ? targetAccountNumber : "");
        this.transactionType     = new SimpleObjectProperty<>(transactionType);
        this.amount              = new SimpleDoubleProperty(amount);
        this.description         = new SimpleStringProperty(description != null ? description : "");
        this.transactionDate     = new SimpleObjectProperty<>(transactionDate);
    }

    // --- Property accessors ---

    /** @return the id property */
    public IntegerProperty idProperty() { return id; }

    /** @return the sourceAccountId property */
    public IntegerProperty sourceAccountIdProperty() { return sourceAccountId; }

    /** @return the targetAccountId property */
    public IntegerProperty targetAccountIdProperty() { return targetAccountId; }

    /** @return the sourceAccountNumber property */
    public StringProperty sourceAccountNumberProperty() { return sourceAccountNumber; }

    /** @return the targetAccountNumber property */
    public StringProperty targetAccountNumberProperty() { return targetAccountNumber; }

    /** @return the transactionType property */
    public ObjectProperty<TransactionType> transactionTypeProperty() { return transactionType; }

    /** @return the amount property */
    public DoubleProperty amountProperty() { return amount; }

    /** @return the description property */
    public StringProperty descriptionProperty() { return description; }

    /** @return the transactionDate property */
    public ObjectProperty<LocalDateTime> transactionDateProperty() { return transactionDate; }

    // --- Getters ---

    /** @return the transaction database id */
    public int getId() { return id.get(); }

    /** @return the source account id (0 = external) */
    public int getSourceAccountId() { return sourceAccountId.get(); }

    /** @return the target account id (0 = external) */
    public int getTargetAccountId() { return targetAccountId.get(); }

    /** @return the source account number for display */
    public String getSourceAccountNumber() { return sourceAccountNumber.get(); }

    /** @return the target account number for display */
    public String getTargetAccountNumber() { return targetAccountNumber.get(); }

    /** @return the transaction type */
    public TransactionType getTransactionType() { return transactionType.get(); }

    /** @return the transaction amount (always positive) */
    public double getAmount() { return amount.get(); }

    /** @return the optional description/memo */
    public String getDescription() { return description.get(); }

    /** @return the transaction timestamp */
    public LocalDateTime getTransactionDate() { return transactionDate.get(); }

    // --- Setters ---

    /** @param v the new id (set after DB insert) */
    public void setId(int v) { id.set(v); }

    /** @param v the source account id */
    public void setSourceAccountId(int v) { sourceAccountId.set(v); }

    /** @param v the target account id */
    public void setTargetAccountId(int v) { targetAccountId.set(v); }

    /** @param v the source account number */
    public void setSourceAccountNumber(String v) { sourceAccountNumber.set(v); }

    /** @param v the target account number */
    public void setTargetAccountNumber(String v) { targetAccountNumber.set(v); }

    /** @param v the transaction type */
    public void setTransactionType(TransactionType v) { transactionType.set(v); }

    /** @param v the transaction amount */
    public void setAmount(double v) { amount.set(v); }

    /** @param v the description/memo */
    public void setDescription(String v) { description.set(v != null ? v : ""); }

    /** @param v the transaction timestamp */
    public void setTransactionDate(LocalDateTime v) { transactionDate.set(v); }
}
