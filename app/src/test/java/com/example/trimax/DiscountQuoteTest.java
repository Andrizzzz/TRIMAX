package com.example.trimax;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.math.BigDecimal;

/** Replies of trimax_agent_discount_quote (shape of sql/20261027_01 in the desktop repository). */
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
}
