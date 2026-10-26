package com.example.trimax;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * The server's answer to "how much discount may I give for these products?" (trimax_agent_discount_quote).
 * Since 20261027_04 it lists every product with the most the agent may take off one piece; an older
 * server sends no "items" (the list is then empty and no discount can be typed).
 */
final class DiscountQuote {

    BigDecimal subtotal = BigDecimal.ZERO;
    BigDecimal maxDiscount = BigDecimal.ZERO;
    boolean available;
    /** Why no discount is possible (no puhunan set, or none left at these prices); null otherwise. */
    String reason;
    /** Per product: allowed or not, and the most per piece. */
    final List<ItemDiscounts.Line> items = new ArrayList<>();

    static DiscountQuote parse(String json) throws JSONException {
        JSONObject o = new JSONObject(json);
        DiscountQuote q = new DiscountQuote();
        q.subtotal = money(o, "subtotal");
        q.maxDiscount = money(o, "max_discount");
        q.available = Boolean.TRUE.equals(Json.bool(o, "discount_available"));
        q.reason = Json.text(o, "reason");
        JSONArray a = o.optJSONArray("items");
        if (a != null) {
            for (int i = 0; i < a.length(); i++) {
                JSONObject it = a.optJSONObject(i);
                if (it == null || !it.has("product_id")) continue;
                q.items.add(new ItemDiscounts.Line(it.optInt("product_id"), Json.text(it, "product_name"),
                        it.optInt("quantity", 1), money(it, "unit_price"), money(it, "max_discount_per_unit"),
                        Boolean.TRUE.equals(Json.bool(it, "discount_allowed")), Json.text(it, "reason")));
            }
        }
        return q;
    }

    private static BigDecimal money(JSONObject o, String key) {
        String t = o.has(key) && !o.isNull(key) ? o.optString(key, "0") : "0";
        try {
            return new BigDecimal(t).setScale(2, java.math.RoundingMode.HALF_UP);
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO.setScale(2);
        }
    }
}
