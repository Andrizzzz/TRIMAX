package com.example.trimax;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Agent discounts per item (sql/20261027_04_per_item_discounts.sql in the Desktop repository): the agent
 * takes a peso amount off each piece of an item the Admin allows (e.g. Sprite 25.00, less 2.00 = 23.00).
 * The server's quote says, per item, whether it may be discounted and the most per piece (never the
 * puhunan). This class only does the arithmetic and the messages on the phone; the server checks again.
 */
final class ItemDiscounts {

    private ItemDiscounts() {
    }

    /** One product of the order as the quote describes it. */
    static final class Line {
        final int productId;
        final String name;
        final int quantity;
        final BigDecimal unitPrice;
        final BigDecimal maxPerUnit;
        final boolean allowed;
        final String reason;

        Line(int productId, String name, int quantity, BigDecimal unitPrice, BigDecimal maxPerUnit, boolean allowed,
             String reason) {
            this.productId = productId;
            this.name = name == null || name.trim().isEmpty() ? "Product " + productId : name.trim();
            this.quantity = quantity;
            this.unitPrice = scale(unitPrice);
            this.maxPerUnit = scale(maxPerUnit);
            this.allowed = allowed && this.maxPerUnit.signum() > 0;
            this.reason = reason;
        }

        /** "Sprite 1L × 2 · ₱25.00 each" */
        String title() {
            return name + " × " + quantity + " · " + DiscountCalc.peso(unitPrice) + " each";
        }

        /** Hint in the field: "Less per piece (up to ₱4.00)". */
        String hint() {
            return "Less per piece (up to " + DiscountCalc.peso(maxPerUnit) + ")";
        }
    }

    /** The typed peso amount per piece, or null when empty. Throws with a message for anything else. */
    static BigDecimal parse(String text) {
        return DiscountCalc.parse(text);
    }

    /** Why this amount per piece cannot be used on this line, or null when it can (or nothing was typed). */
    static String problem(Line line, BigDecimal perUnit) {
        if (perUnit == null || perUnit.signum() == 0) return null;
        if (perUnit.signum() < 0) return "A discount cannot be negative.";
        if (!line.allowed) {
            return line.reason != null && !line.reason.isEmpty() ? line.reason : "No discount is allowed on " + line.name + ".";
        }
        if (perUnit.compareTo(line.maxPerUnit) > 0) {
            return "The most you can take off " + line.name + " is " + DiscountCalc.peso(line.maxPerUnit) + " per piece.";
        }
        return null;
    }

    /** Totals of the order for the amounts typed so far (lines with a problem count as no discount). */
    static final class Totals {
        BigDecimal subtotal = BigDecimal.ZERO.setScale(2);
        BigDecimal discount = BigDecimal.ZERO.setScale(2);

        BigDecimal total() {
            return subtotal.subtract(discount);
        }
    }

    static Totals totals(List<Line> lines, Map<Integer, BigDecimal> perUnit) {
        Totals t = new Totals();
        for (Line l : lines) {
            BigDecimal q = BigDecimal.valueOf(l.quantity);
            t.subtotal = t.subtotal.add(l.unitPrice.multiply(q));
            BigDecimal d = perUnit == null ? null : perUnit.get(l.productId);
            if (d != null && d.signum() > 0 && problem(l, d) == null) t.discount = t.discount.add(d.multiply(q));
        }
        t.subtotal = scale(t.subtotal);
        t.discount = scale(t.discount);
        return t;
    }

    /** What the quote allows, in a short sentence under the item list. */
    static String summary(List<Line> lines) {
        List<String> names = new ArrayList<>();
        for (Line l : lines) if (l.allowed) names.add(l.name);
        if (names.isEmpty()) return "No item in this order can be discounted.";
        return "You can take something off: " + String.join(", ", names) + ". Other items keep their price.";
    }

    /** Only the lines with a discount, for the server ({product id -> amount per piece}). */
    static Map<Integer, BigDecimal> nonZero(Map<Integer, BigDecimal> perUnit) {
        Map<Integer, BigDecimal> out = new LinkedHashMap<>();
        if (perUnit == null) return out;
        for (Map.Entry<Integer, BigDecimal> e : perUnit.entrySet()) {
            if (e.getValue() != null && e.getValue().signum() > 0) out.put(e.getKey(), scale(e.getValue()));
        }
        return out;
    }

    // ------------------------------------------------------------ the "Less" button on an item card

    /** Price per piece after the discount (never below zero). */
    static BigDecimal net(BigDecimal unitPrice, BigDecimal perUnit) {
        BigDecimal n = scale(unitPrice).subtract(scale(perUnit));
        return n.signum() < 0 ? scale(BigDecimal.ZERO) : n;
    }

    /** Text on the small button under the price: "Less" or "Less ₱2.00". */
    static String buttonText(BigDecimal perUnit) {
        return perUnit == null || perUnit.signum() <= 0 ? "Less" : "Less " + DiscountCalc.peso(perUnit);
    }

    /** Slider position to an amount: steps of ₱0.25, never above the maximum, the very end is the maximum. */
    static BigDecimal snap(double raw, BigDecimal max) {
        BigDecimal m = scale(max);
        if (!(raw > 0) || m.signum() <= 0) return scale(BigDecimal.ZERO);
        if (raw >= m.doubleValue() - 0.125) return m;
        BigDecimal quarters = BigDecimal.valueOf(raw).multiply(BigDecimal.valueOf(4)).setScale(0, RoundingMode.HALF_UP);
        BigDecimal v = scale(quarters.divide(BigDecimal.valueOf(4)));
        return v.compareTo(m) > 0 ? m : v;
    }

    /** Quick amounts in the sheet: up to three round amounts below the maximum, then the maximum itself. */
    static List<BigDecimal> quickAmounts(BigDecimal max) {
        List<BigDecimal> out = new ArrayList<>();
        BigDecimal m = scale(max);
        if (m.signum() <= 0) return out;
        int[] steps = {1, 2, 5, 10, 20, 50, 100};
        for (int step : steps) {
            BigDecimal v = scale(BigDecimal.valueOf(step));
            if (v.compareTo(m) < 0 && out.size() < 3) out.add(v);
        }
        out.add(m);
        return out;
    }

    /** Amounts per piece carried from the item list to the review step (same order as the product ids). */
    static Map<Integer, BigDecimal> fromArrays(int[] productIds, double[] perUnit) {
        Map<Integer, BigDecimal> out = new LinkedHashMap<>();
        if (productIds == null || perUnit == null || productIds.length != perUnit.length) return out;
        for (int i = 0; i < productIds.length; i++) {
            if (Double.isFinite(perUnit[i]) && perUnit[i] > 0) out.put(productIds[i], scale(BigDecimal.valueOf(perUnit[i])));
        }
        return out;
    }

    static BigDecimal scale(BigDecimal v) {
        return (v == null ? BigDecimal.ZERO : v).setScale(2, RoundingMode.HALF_UP);
    }
}
