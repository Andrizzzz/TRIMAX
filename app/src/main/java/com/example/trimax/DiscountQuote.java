package com.example.trimax;

import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;

/** The server's answer to "how much discount may I give for these products?" (trimax_agent_discount_quote). */
final class DiscountQuote {

    BigDecimal subtotal = BigDecimal.ZERO;
    BigDecimal maxDiscount = BigDecimal.ZERO;
    boolean available;
    /** Why no discount is possible (no puhunan set, or none left at these prices); null otherwise. */
    String reason;

    static DiscountQuote parse(String json) throws JSONException {
        JSONObject o = new JSONObject(json);
        DiscountQuote q = new DiscountQuote();
        q.subtotal = money(o, "subtotal");
        q.maxDiscount = money(o, "max_discount");
        q.available = Boolean.TRUE.equals(Json.bool(o, "discount_available"));
        q.reason = Json.text(o, "reason");
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
