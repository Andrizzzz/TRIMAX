package com.example.trimax;

import org.json.JSONObject;

/**
 * Null-safe reads from org.json objects. Android's JSONObject.optString returns the text "null" for a JSON
 * null, which would show up on screen; these helpers return Java null for missing and JSON null instead.
 */
final class Json {

    private Json() {
    }

    static String text(JSONObject o, String key) {
        if (o == null || !o.has(key) || o.isNull(key)) return null;
        Object v = o.opt(key);
        return v == null ? null : String.valueOf(v);
    }

    static Double number(JSONObject o, String key) {
        if (o == null || !o.has(key) || o.isNull(key)) return null;
        Object v = o.opt(key);
        if (v instanceof Number) return ((Number) v).doubleValue();
        try {
            return Double.parseDouble(String.valueOf(v));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    static int integer(JSONObject o, String key, int fallback) {
        Double d = number(o, key);
        return d == null ? fallback : (int) Math.round(d);
    }

    static Boolean bool(JSONObject o, String key) {
        if (o == null || !o.has(key) || o.isNull(key)) return null;
        Object v = o.opt(key);
        if (v instanceof Boolean) return (Boolean) v;
        return Boolean.parseBoolean(String.valueOf(v));
    }

    static JSONObject object(JSONObject o, String key) {
        if (o == null || !o.has(key) || o.isNull(key)) return null;
        return o.optJSONObject(key);
    }
}
