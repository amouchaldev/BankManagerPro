package com.banking.util;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * Formats monetary values in French locale (e.g., 1 234,56 €).
 */
public class CurrencyFormatter {

    private static final NumberFormat FORMAT = NumberFormat.getCurrencyInstance(Locale.FRANCE);

    private CurrencyFormatter() {}

    /**
     * Formats an amount as French currency.
     *
     * @param amount the value to format
     * @return formatted string, e.g. "1 234,56 €"
     */
    public static String format(double amount) {
        return FORMAT.format(amount);
    }

    /**
     * Formats an amount with an explicit sign (useful for transaction history).
     *
     * @param amount the value to format
     * @return formatted string with "+" prefix for positive values, e.g. "+500,00 €"
     */
    public static String formatSigned(double amount) {
        return (amount >= 0 ? "+" : "") + FORMAT.format(amount);
    }

    /**
     * Parses a currency string back to a double, accepting both "." and "," as decimal separator.
     *
     * @param text the text to parse
     * @return the parsed double value
     * @throws NumberFormatException if the text cannot be parsed
     */
    public static double parse(String text) {
        if (text == null) throw new NumberFormatException("null");
        String cleaned = text.replace(" ", "").replace(",", ".").replaceAll("[^\\d.\\-]", "");
        return Double.parseDouble(cleaned);
    }
}
