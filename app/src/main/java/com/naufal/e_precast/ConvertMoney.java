package com.naufal.e_precast;

import java.text.NumberFormat;
import java.util.Locale;
public class ConvertMoney {
    private static final NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("id", "ID"));

    static {
        currencyFormat.setMaximumFractionDigits(0);
    }

    /**
     * Formats a number to Indonesian Rupiah currency format
     * Example: 1000000 -> Rp 1,000,000
     */
    public static String format(long amount) {
        String formatted = currencyFormat.format(amount);
        // Remove currency symbol and replace with "Rp "
        return "Rp " + formatted.substring(2);
    }

    /**
     * Formats a number to compact Indonesian Rupiah currency format
     * Example: 1000000 -> Rp 1M
     */
    public static String formatCompact(long amount) {
        if (amount < 1000) {
            return "Rp " + amount;
        } else if (amount < 1000000) {
            return "Rp " + (amount / 1000) + "K";
        } else if (amount < 1000000000) {
            return "Rp " + (amount / 1000000) + "M";
        } else {
            return "Rp " + (amount / 1000000000) + "B";
        }
    }
}
