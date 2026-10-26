package com.example.trimax;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

/**
 * The agent's booking discount on the phone: a peso amount or a percent of the order. Only for showing
 * the numbers while typing; the TRIMAX server works the discount out again from its own prices and
 * refuses anything below the minimum price the Admin set (puhunan + minimum patong).
 */
final class DiscountCalc {

    enum Type { AMOUNT, PERCENT }

    private DiscountCalc() {}

    /** The typed value, or null when the field is empty. Throws with a message for anything else. */
    static BigDecimal parse(String text) {
        String t = text == null ? "" : text.trim().replace(",", "").replace("₱", "").replace("%", "").trim();
        if (t.isEmpty()) return null;
        BigDecimal v;
        try {
            v = new BigDecimal(t);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Enter the discount as a number, for example 50 or 12.50.");
        }
        if (v.signum() < 0) throw new IllegalArgumentException("A discount cannot be negative.");
        if (v.stripTrailingZeros().scale() > 2) throw new IllegalArgumentException("Use at most 2 decimals.");
        return v;
    }

    /** Pesos taken off: a percent is rounded to the centavo like the server does (half up). */
    static BigDecimal amount(BigDecimal subtotal, Type type, BigDecimal value) {
        if (subtotal == null || value == null || value.signum() == 0) return BigDecimal.ZERO.setScale(2);
        if (type == Type.PERCENT) {
            return subtotal.multiply(value).divide(new BigDecimal(100), 2, RoundingMode.HALF_UP);
        }
        return value.min(subtotal).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Why this discount cannot be used, or null when it can. maxDiscount and available come from the
     * server's quote (null when the quote is not known yet: the server then decides on Confirm).
     */
    static String problem(BigDecimal subtotal, Type type, BigDecimal value, BigDecimal maxDiscount, Boolean available,
                          String reason) {
        if (value == null || value.signum() == 0) return null;
        if (type == Type.PERCENT && value.compareTo(new BigDecimal(100)) > 0) return "A percent discount must be from 0 to 100.";
        if (type == Type.AMOUNT && subtotal != null && value.compareTo(subtotal) > 0) {
            return "The discount is more than the order total (" + peso(subtotal) + ").";
        }
        if (Boolean.FALSE.equals(available)) {
            return reason != null && !reason.isEmpty() ? reason : "No discount is possible for this order.";
        }
        if (maxDiscount != null && amount(subtotal, type, value).compareTo(maxDiscount) > 0) {
            return "This discount is too big. The most you can give for this order is " + peso(maxDiscount) + ".";
        }
        return null;
    }

    static String peso(BigDecimal v) {
        return String.format(Locale.US, "₱%,.2f", v == null ? BigDecimal.ZERO : v);
    }
}
