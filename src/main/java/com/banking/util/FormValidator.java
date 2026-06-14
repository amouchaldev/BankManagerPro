package com.banking.util;

/**
 * Static validation helpers used by controllers before calling DAOs.
 * All methods are pure functions with no side effects.
 */
public class FormValidator {

    private FormValidator() {}

    /**
     * Checks that the string is non-null and not blank after trimming.
     *
     * @param text the string to check
     * @return true if non-blank
     */
    public static boolean isNotBlank(String text) {
        return text != null && !text.trim().isEmpty();
    }

    /**
     * Validates an email address format.
     *
     * @param email the email to validate
     * @return true if the format is valid
     */
    public static boolean isValidEmail(String email) {
        if (email == null) return false;
        return email.matches("^[\\w._%+\\-]+@[\\w.\\-]+\\.[a-zA-Z]{2,}$");
    }

    /**
     * Validates a phone number (allows digits, spaces, +, -, parentheses).
     *
     * @param phone the phone number to validate
     * @return true if the format is valid (or the field is empty)
     */
    public static boolean isValidPhone(String phone) {
        if (phone == null || phone.isBlank()) return true; // optional field
        return phone.matches("^[\\d\\s+()\\-]{6,20}$");
    }

    /**
     * Checks that the string parses as a positive double.
     *
     * @param text the text to parse
     * @return true if the text is a number greater than zero
     */
    public static boolean isPositiveAmount(String text) {
        if (text == null || text.isBlank()) return false;
        try {
            double val = Double.parseDouble(text.replace(",", "."));
            return val > 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Checks that the string parses as a non-negative double.
     *
     * @param text the text to parse
     * @return true if the text is a number >= 0
     */
    public static boolean isNonNegativeAmount(String text) {
        if (text == null || text.isBlank()) return false;
        try {
            double val = Double.parseDouble(text.replace(",", "."));
            return val >= 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Validates all required client form fields at once.
     * Returns the first validation error message, or null if all fields are valid.
     *
     * @param firstName the first name input
     * @param lastName  the last name input
     * @param email     the email input
     * @param phone     the phone input (optional)
     * @return an error message, or null if valid
     */
    public static String validateClientForm(String firstName, String lastName,
                                            String email, String phone) {
        if (!isNotBlank(firstName)) return "Le prénom est obligatoire.";
        if (!isNotBlank(lastName))  return "Le nom est obligatoire.";
        if (!isNotBlank(email))     return "L'adresse e-mail est obligatoire.";
        if (!isValidEmail(email))   return "L'adresse e-mail n'est pas valide.";
        if (!isValidPhone(phone))   return "Le numéro de téléphone n'est pas valide.";
        return null;
    }

    /**
     * Validates the amount field for a transaction form.
     *
     * @param amountText the raw amount text from a TextField
     * @return an error message, or null if valid
     */
    public static String validateAmount(String amountText) {
        if (!isNotBlank(amountText)) return "Le montant est obligatoire.";
        if (!isPositiveAmount(amountText)) return "Le montant doit être un nombre positif.";
        return null;
    }
}
