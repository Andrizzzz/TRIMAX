package com.example.trimax;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

/** Half cases (database update 20261028_07 in the Desktop repository). */
public class HalfCaseTest {

    private static final String LIST = "["
            + "{\"product_id\": 7, \"product_name\": \"Coke Case (24)\", \"price\": 240, \"stock\": 7.5,"
            + " \"discount_allowed\": true, \"max_discount_per_unit\": 30, \"half_price\": 125, \"half_max_discount_per_unit\": 20},"
            + "{\"product_id\": 8, \"product_name\": \"Rice 25 kg\", \"price\": 1250, \"stock\": 4,"
            + " \"discount_allowed\": false, \"max_discount_per_unit\": 0, \"half_price\": null, \"half_max_discount_per_unit\": null},"
            + "{\"product_id\": 9, \"product_name\": \"Old server item\", \"price\": 10, \"stock\": 3}"
            + "]";

    @Test
    public void productListReadsTheHalfCasePriceAndAStockWithAHalf() throws Exception {
        List<Product> list = Product.parseList(LIST);
        Product coke = list.get(0), rice = list.get(1), old = list.get(2);
        assertEquals(7.5, coke.stock, 0.0001);
        assertTrue(coke.sellsHalfCase());
        assertEquals(125.0, coke.halfPrice, 0.0001);
        assertTrue(coke.halfDiscountAllowed());
        assertEquals(new BigDecimal("20.00"), coke.halfMaxDiscountPerUnit);
        assertFalse(rice.sellsHalfCase());
        assertFalse(old.sellsHalfCase());
        assertFalse(rice.halfDiscountAllowed());
        assertEquals("7.5", Product.stockText(coke.stock));
        assertEquals("4", Product.stockText(rice.stock));
        assertEquals("0", Product.stockText(0));
    }

    @Test
    public void halfCaseDiscountLineUsesTheHalfPriceAndLimit() throws Exception {
        Product coke = Product.parseList(LIST).get(0);
        ItemDiscounts.Line half = coke.discountLine(2, true);
        assertTrue(half.half);
        assertEquals("Coke Case (24) (half case)", half.name);
        assertEquals(new BigDecimal("125.00"), half.unitPrice);
        assertEquals(new BigDecimal("20.00"), half.maxPerUnit);
        assertNull(ItemDiscounts.problem(half, new BigDecimal("20")));
        assertEquals("The most you can take off Coke Case (24) (half case) is ₱20.00 per piece.",
                ItemDiscounts.problem(half, new BigDecimal("20.01")));
        ItemDiscounts.Line whole = coke.discountLine(1, false);
        assertFalse(whole.half);
        assertNull(ItemDiscounts.problem(whole, new BigDecimal("30")));
        assertEquals(-7, half.key());
        assertEquals(7, whole.key());
    }

    @Test
    public void lineKeys() {
        assertEquals(12, ItemDiscounts.key(12, false));
        assertEquals(-12, ItemDiscounts.key(12, true));
        assertEquals(12, ItemDiscounts.productOf(-12));
        assertTrue(ItemDiscounts.isHalf(-12));
        assertFalse(ItemDiscounts.isHalf(12));
        CreateBookingRequest.Line l = CreateBookingRequest.Line.ofKey(-12, 3);
        assertEquals(12, l.productId);
        assertTrue(l.half);
        assertEquals(-12, l.key());
    }

    @Test
    public void bookingRequestSendsTheUnitOnlyForHalfCases() throws Exception {
        CreateBookingRequest r = new CreateBookingRequest(null);
        r.customerName = "Aling Nena";
        r.customerContact = "09170001001";
        r.deliveryAddress = "Tungko";
        r.latitude = 14.8;
        r.longitude = 121.0;
        r.lines.add(new CreateBookingRequest.Line(7, 1));
        CreateBookingRequest.Line half = CreateBookingRequest.Line.ofKey(-7, 3);
        half.discountPerUnit = new BigDecimal("5");
        r.lines.add(half);
        assertNull(r.validate());
        JSONArray items = r.toRpcArgs().getJSONArray("p_items");
        JSONObject a = items.getJSONObject(0), b = items.getJSONObject(1);
        assertFalse(a.has("unit"));
        assertEquals("HALF", b.getString("unit"));
        assertEquals(7, b.getInt("product_id"));
        assertEquals(3, b.getInt("quantity"));
        assertEquals("5.00", b.getString("discount_per_unit"));
    }

    @Test
    public void quoteAndTotalsKeepWholeAndHalfLinesApart() throws Exception {
        DiscountQuote q = DiscountQuote.parse("{\"subtotal\": 490, \"max_discount\": 70, \"discount_available\": true, \"items\": ["
                + "{\"product_id\": 7, \"product_name\": \"Coke Case (24)\", \"unit\": \"WHOLE\", \"quantity\": 1, \"unit_price\": 240,"
                + " \"max_discount_per_unit\": 30, \"discount_allowed\": true},"
                + "{\"product_id\": 7, \"product_name\": \"Coke Case (24)\", \"unit\": \"HALF\", \"quantity\": 2, \"unit_price\": 125,"
                + " \"max_discount_per_unit\": 20, \"discount_allowed\": true}]}");
        assertEquals(2, q.items.size());
        assertFalse(q.items.get(0).half);
        assertTrue(q.items.get(1).half);
        assertEquals("Coke Case (24) (half case)", q.items.get(1).name);
        Map<Integer, BigDecimal> typed = new HashMap<>();
        typed.put(7, new BigDecimal("10"));
        typed.put(-7, new BigDecimal("5"));
        ItemDiscounts.Totals t = ItemDiscounts.totals(q.items, typed);
        assertEquals(new BigDecimal("490.00"), t.subtotal);
        assertEquals(new BigDecimal("20.00"), t.discount);   // 10 x 1 + 5 x 2
        typed.put(-7, new BigDecimal("25"));                  // more than 20 off a half case: not counted
        assertEquals(new BigDecimal("10.00"), ItemDiscounts.totals(q.items, typed).discount);
    }

    @Test
    public void bookingRecordShowsHalfCaseLines() throws Exception {
        BookingRecord b = BookingRecord.parse("{\"booking_id\": 42, \"status\": \"Pending\", \"total_amount\": 370, \"items\": ["
                + "{\"product_id\": 2, \"product_name\": \"Mineral water x24\", \"unit\": \"WHOLE\", \"quantity\": 1, \"unit_price\": 180, \"subtotal\": 180},"
                + "{\"product_id\": 2, \"product_name\": \"Mineral water x24\", \"unit\": \"HALF\", \"quantity\": 2, \"unit_price\": 95, \"subtotal\": 190}]}");
        assertFalse(b.items.get(0).half);
        assertTrue(b.items.get(1).half);
        assertEquals("Mineral water x24 (half case)", b.items.get(1).displayName());
        assertEquals("Mineral water x24 x1, Mineral water x24 (half case) x2", b.itemsSummary());
        // an older server sends no unit: whole cases
        BookingRecord old = BookingRecord.parse("{\"booking_id\": 1, \"items\": [{\"product_id\": 2, \"product_name\": \"W\", \"quantity\": 1}]}");
        assertFalse(old.items.get(0).half);
    }
}
