package com.banking.model;

/**
 * Represents the type of a financial transaction.
 */
public enum TransactionType {
    DEPOSIT("Dépôt"),
    WITHDRAWAL("Retrait"),
    TRANSFER("Virement");

    private final String displayName;

    TransactionType(String displayName) {
        this.displayName = displayName;
    }

    /** @return the French display name for this transaction type */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Returns the French display name, used automatically by JavaFX ComboBox cells.
     *
     * @return the display name string
     */
    @Override
    public String toString() {
        return displayName;
    }
}
