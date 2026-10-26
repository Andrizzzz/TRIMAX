package com.example.trimax;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

import java.math.BigDecimal;

/** The phone's discount preview; the server repeats every check with its own prices. */
public class DiscountCalcTest {

    private static BigDecimal d(String v) {
        return new BigDecimal(v);
    }

    @Test
    public void parseAcceptsPlainAndFormattedNumbers() {
        assertNull(DiscountCalc.parse(null));
        assertNull(DiscountCalc.parse("   "));
        assertEquals(0, d("50").compareTo(DiscountCalc.parse("50")));
        assertEquals(0, d("1250.50").compareTo(DiscountCalc.parse(" 1,250.50 ")));
        assertEquals(0, d("12.5").compareTo(DiscountCalc.parse("12.5%")));
        assertEquals(0, d("20").compareTo(DiscountCalc.parse("₱20")));
        assertEquals(0, d("3.10").compareTo(DiscountCalc.parse("3.100")));   // trailing zeros are fine
    }

    @Test
    public void parseRefusesBadInput() {
        for (String bad : new String[]{"abc", "-5", "1.234", "1..2"}) {
            try {
                DiscountCalc.parse(bad);
                fail("accepted " + bad);
            } catch (IllegalArgumentException expected) {
                assertNotNull(expected.getMessage());
            }
        }
    }

    @Test
    public void percentIsRoundedHalfUpToTheCentavoLikeTheServer() {
        assertEquals(d("33.34"), DiscountCalc.amount(d("333.35"), DiscountCalc.Type.PERCENT, d("10")));
        assertEquals(d("36.00"), DiscountCalc.amount(d("720.00"), DiscountCalc.Type.PERCENT, d("5")));
        assertEquals(d("720.00"), DiscountCalc.amount(d("720.00"), DiscountCalc.Type.PERCENT, d("100")));
    }

    @Test
    public void amountNeverGoesBelowZeroTotal() {
        assertEquals(d("50.00"), DiscountCalc.amount(d("720.00"), DiscountCalc.Type.AMOUNT, d("50")));
        assertEquals(d("720.00"), DiscountCalc.amount(d("720.00"), DiscountCalc.Type.AMOUNT, d("900")));
        assertEquals(d("0.00"), DiscountCalc.amount(d("720.00"), DiscountCalc.Type.AMOUNT, null));
        assertEquals(d("0.00"), DiscountCalc.amount(d("720.00"), DiscountCalc.Type.PERCENT, BigDecimal.ZERO));
    }

    @Test
    public void problemFollowsTheServerRules() {
        BigDecimal sub = d("720.00"), max = d("120.00");
        // nothing typed / zero: no discount, nothing to fix
        assertNull(DiscountCalc.problem(sub, DiscountCalc.Type.AMOUNT, null, max, true, null));
        assertNull(DiscountCalc.problem(sub, DiscountCalc.Type.AMOUNT, BigDecimal.ZERO, max, false, "no puhunan"));
        // within the limit
        assertNull(DiscountCalc.problem(sub, DiscountCalc.Type.AMOUNT, d("120"), max, true, null));
        assertNull(DiscountCalc.problem(sub, DiscountCalc.Type.PERCENT, d("16.66"), max, true, null));
        // over the limit: the message names the most allowed, never the puhunan
        String tooBig = DiscountCalc.problem(sub, DiscountCalc.Type.AMOUNT, d("120.01"), max, true, null);
        assertNotNull(tooBig);
        assertTrue(tooBig, tooBig.contains("120.00"));
        assertNotNull(DiscountCalc.problem(sub, DiscountCalc.Type.PERCENT, d("17"), max, true, null));
        // impossible values
        assertNotNull(DiscountCalc.problem(sub, DiscountCalc.Type.PERCENT, d("100.01"), null, null, null));
        assertNotNull(DiscountCalc.problem(sub, DiscountCalc.Type.AMOUNT, d("720.01"), null, null, null));
        // no discount allowed for this order: the server's reason is shown
        assertEquals("Ask the Admin to set it.",
                DiscountCalc.problem(sub, DiscountCalc.Type.AMOUNT, d("1"), BigDecimal.ZERO, false, "Ask the Admin to set it."));
        // quote not loaded yet: the server decides on Confirm
        assertNull(DiscountCalc.problem(sub, DiscountCalc.Type.AMOUNT, d("500"), null, null, null));
    }
}
