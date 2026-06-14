package com.banking.model;

/**
 * Represents the type of a bank account.
 */
public enum AccountType {
    CHECKING("Compte Courant"),
    SAVINGS("Compte Épargne"),
    CREDIT("Crédit");

    private final String displayName;

    AccountType(String displayName) {
        this.displayName = displayName;
    }

    /** @return the French display name for this account type */
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
