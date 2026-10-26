package com.example.trimax;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;

/** Discount per item on the phone: Sprite may be discounted (up to 4.00 a piece), Coke and Candy may not. */
public class ItemDiscountsTest {

    private static DiscountQuote quote() throws Exception {
        return DiscountQuote.parse(DiscountQuoteTest.PER_ITEM);
    }

    @Test
    public void anAllowedItemUpToItsMaximum() throws Exception {
        ItemDiscounts.Line sprite = quote().items.get(0);
        assertNull(ItemDiscounts.problem(sprite, null));
        assertNull(ItemDiscounts.problem(sprite, new BigDecimal("2")));
        assertNull(ItemDiscounts.problem(sprite, new BigDecimal("4.00")));
        assertEquals("The most you can take off Sprite 1L is ₱4.00 per piece.",
                ItemDiscounts.problem(sprite, new BigDecimal("4.01")));
    }

    @Test
    public void anItemTheAdminDidNotAllowShowsWhy() throws Exception {
        ItemDiscounts.Line coke = quote().items.get(1);
        assertNull("no discount typed is fine", ItemDiscounts.problem(coke, BigDecimal.ZERO));
        assertEquals("The Admin does not allow a discount on this item.", ItemDiscounts.problem(coke, BigDecimal.ONE));
    }

    @Test
    public void totalsOnlyCountValidDiscounts() throws Exception {
        DiscountQuote q = quote();
        Map<Integer, BigDecimal> typed = new HashMap<>();
        typed.put(201, new BigDecimal("2"));      // Sprite x2: 4.00 off
        typed.put(202, new BigDecimal("1"));      // Coke: not allowed, not counted
        ItemDiscounts.Totals t = ItemDiscounts.totals(q.items, typed);
        assertEquals(new BigDecimal("78.00"), t.subtotal);
        assertEquals(new BigDecimal("4.00"), t.discount);
        assertEquals(new BigDecimal("74.00"), t.total());

        typed.put(201, new BigDecimal("9"));      // too much: not counted until fixed
        assertEquals(new BigDecimal("0.00"), ItemDiscounts.totals(q.items, typed).discount);
    }

    @Test
    public void summaryNamesTheItemsThatCanBeDiscounted() throws Exception {
        assertEquals("You can take something off: Sprite 1L. Other items keep their price.",
                ItemDiscounts.summary(quote().items));
        DiscountQuote none = DiscountQuote.parse("{\"items\": [{\"product_id\": 5, \"quantity\": 1, \"unit_price\": 10, "
                + "\"discount_allowed\": false, \"max_discount_per_unit\": 0}]}");
        assertEquals("No item in this order can be discounted.", ItemDiscounts.summary(none.items));
    }

    @Test
    public void onlyNonZeroAmountsGoToTheServer() {
        Map<Integer, BigDecimal> typed = new HashMap<>();
        typed.put(1, BigDecimal.ZERO);
        typed.put(2, new BigDecimal("1.5"));
        Map<Integer, BigDecimal> out = ItemDiscounts.nonZero(typed);
        assertEquals(1, out.size());
        assertEquals(new BigDecimal("1.50"), out.get(2));
    }

    @Test
    public void aLineWithMaxZeroIsNeverAllowed() {
        ItemDiscounts.Line l = new ItemDiscounts.Line(9, "Water", 1, new BigDecimal("10"), BigDecimal.ZERO, true, null);
        assertTrue(!l.allowed);
        assertEquals("No discount is allowed on Water.", ItemDiscounts.problem(l, BigDecimal.ONE));
        assertEquals("Water × 1 · ₱10.00 each", l.title());
    }
}
