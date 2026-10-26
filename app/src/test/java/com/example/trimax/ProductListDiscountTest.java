package com.example.trimax;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.util.List;
import org.junit.Test;

/** trimax_list_products after 20261027_04: each item says whether agents may discount it and the most per piece. */
public class ProductListDiscountTest {

    private static final String LIST = "["
            + "{\"product_id\": 201, \"product_name\": \"Sprite 1L\", \"price\": 25, \"stock\": 40,"
            + " \"discount_allowed\": true, \"max_discount_per_unit\": 4},"
            + "{\"product_id\": 202, \"product_name\": \"Coke 1L\", \"price\": 28, \"stock\": 10,"
            + " \"discount_allowed\": false, \"max_discount_per_unit\": 0},"
            + "{\"product_id\": 203, \"product_name\": \"Old server item\", \"price\": 10, \"stock\": 3}"
            + "]";

    @Test
    public void readsWhetherEachItemCanBeDiscounted() throws Exception {
        List<Product> list = Product.parseList(LIST);
        assertEquals(3, list.size());
        assertTrue(list.get(0).discountAllowed);
        assertEquals(new BigDecimal("4.00"), list.get(0).maxDiscountPerUnit);
        assertFalse(list.get(1).discountAllowed);
        assertFalse("an older server sends nothing: no Less button", list.get(2).discountAllowed);
        assertEquals(new BigDecimal("0.00"), list.get(2).maxDiscountPerUnit);
    }

    @Test
    public void allowedButNoRoomIsNotAllowed() throws Exception {
        Product p = Product.parseList("[{\"product_id\": 1, \"product_name\": \"X\", \"price\": 5,"
                + " \"discount_allowed\": true, \"max_discount_per_unit\": 0}]").get(0);
        assertFalse(p.discountAllowed);
    }

    @Test
    public void theCardLineChecksTheSameLimitAsTheReviewStep() throws Exception {
        Product sprite = Product.parseList(LIST).get(0);
        ItemDiscounts.Line line = sprite.discountLine(2);
        assertEquals("Sprite 1L × 2 · ₱25.00 each", line.title());
        assertNull(ItemDiscounts.problem(line, new BigDecimal("4")));
        assertEquals("The most you can take off Sprite 1L is ₱4.00 per piece.",
                ItemDiscounts.problem(line, new BigDecimal("4.25")));
        ItemDiscounts.Line coke = Product.parseList(LIST).get(1).discountLine(1);
        assertEquals("The Admin does not allow a discount on this item.", ItemDiscounts.problem(coke, BigDecimal.ONE));
    }
}
