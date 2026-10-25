package com.example.trimax;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/** A product the agent can order, as listed by the server (price and stock are the server's). */
public final class Product {

    public final int id;
    public final String name;
    public final double price;
    public final int stock;

    public Product(int id, String name, double price, int stock) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.stock = stock;
    }

    public static List<Product> parseList(String json) throws JSONException {
        JSONArray array = new JSONArray(json == null || json.trim().isEmpty() ? "[]" : json);
        List<Product> list = new ArrayList<>(array.length());
        for (int i = 0; i < array.length(); i++) {
            JSONObject o = array.optJSONObject(i);
            if (o == null) continue;
            int id = Json.integer(o, "product_id", 0);
            String name = Json.text(o, "product_name");
            Double price = Json.number(o, "price");
            if (id <= 0 || name == null || price == null) continue;
            list.add(new Product(id, name, price, Json.integer(o, "stock", 0)));
        }
        return list;
    }
}
