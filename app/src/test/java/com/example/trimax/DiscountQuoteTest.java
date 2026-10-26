package com.example.trimax;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.math.BigDecimal;

/** Replies of trimax_agent_discount_quote (shapes of sql/20261027_01 and 20261027_04 in the desktop repository). */
public class DiscountQuoteTest {

    @Test
    public void parsesAnAllowedDiscount() throws Exception {
        DiscountQuote q = DiscountQuote.parse("{\"reason\": null, \"subtotal\": 720.00, \"max_discount\": 120.00, "
                + "\"discount_available\": true}");
        assertEquals(new BigDecimal("720.00"), q.subtotal);
        assertEquals(new BigDecimal("120.00"), q.maxDiscount);
        assertTrue(q.available);
        assertNull(q.reason);
    }

    @Test
    public void parsesARefusalWithItsReason() throws Exception {
        DiscountQuote q = DiscountQuote.parse("{\"reason\": \"No discount is allowed for this order yet: there is no "
                + "capital cost (puhunan) for Coke Case. Ask the Admin to set it.\", \"subtotal\": 180, "
                + "\"max_discount\": 0, \"discount_available\": false}");
        assertFalse(q.available);
        assertEquals(new BigDecimal("180.00"), q.subtotal);
        assertEquals(new BigDecimal("0.00"), q.maxDiscount);
        assertTrue(q.reason.contains("puhunan"));
    }

    /** The per-item reply of 20261027_04 (as returned by the test database for Sprite x2, Coke x1, Candy x1). */
    static final String PER_ITEM = "{\"items\": [{\"reason\": null, \"quantity\": 2, \"product_id\": 201, \"unit_price\": 25.00, "
            + "\"product_name\": \"Sprite 1L\", \"discount_allowed\": true, \"max_discount_per_unit\": 4.00}, "
            + "{\"reason\": \"The Admin does not allow a discount on this item.\", \"quantity\": 1, \"product_id\": 202, "
            + "\"unit_price\": 25.00, \"product_name\": \"Coke 1L\", \"discount_allowed\": false, \"max_discount_per_unit\": 0}, "
            + "{\"reason\": \"No discount yet: this item has no capital cost (puhunan).\", \"quantity\": 1, \"product_id\": 203, "
            + "\"unit_price\": 3.00, \"product_name\": \"Candy\", \"discount_allowed\": false, \"max_discount_per_unit\": 0}], "
            + "\"reason\": null, \"subtotal\": 78.00, \"max_discount\": 8.00, \"discount_available\": true}";

    @Test
    public void parsesTheItems() throws Exception {
        DiscountQuote q = DiscountQuote.parse(PER_ITEM);
        assertEquals(3, q.items.size());
        ItemDiscounts.Line sprite = q.items.get(0);
        assertEquals(201, sprite.productId);
        assertEquals(2, sprite.quantity);
        assertTrue(sprite.allowed);
        assertEquals(new BigDecimal("4.00"), sprite.maxPerUnit);
        assertEquals(new BigDecimal("25.00"), sprite.unitPrice);
        assertFalse(q.items.get(1).allowed);
        assertTrue(q.items.get(1).reason.contains("Admin"));
        assertFalse(q.items.get(2).allowed);
    }

    @Test
    public void anOlderServerHasNoItems() throws Exception {
        DiscountQuote q = DiscountQuote.parse("{\"subtotal\": 720.00, \"max_discount\": 120.00, \"discount_available\": true}");
        assertTrue(q.items.isEmpty());
    }
}
