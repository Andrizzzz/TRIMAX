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
    /** Path of the product picture in the public product-images bucket, or null (no picture / older server). */
    public final String imagePath;

    public Product(int id, String name, double price, int stock) {
        this(id, name, price, stock, null);
    }

    public Product(int id, String name, double price, int stock, String imagePath) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.stock = stock;
        this.imagePath = imagePath;
    }

    /** Public address of the picture, or null when the product has none. */
    public String imageUrl() {
        return ProductImages.urlFor(imagePath);
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
            list.add(new Product(id, name, price, Json.integer(o, "stock", 0), Json.text(o, "image_path")));
        }
        return list;
    }
}
