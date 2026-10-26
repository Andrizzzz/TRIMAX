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
    /**
     * Whether the Admin lets agents take something off each piece of this item, and the most per piece
     * (the server works this out from the price, puhunan and minimum patong; the phone never sees those).
     * An older server sends neither, so the item simply has no "Less" button.
     */
    public final boolean discountAllowed;
    public final java.math.BigDecimal maxDiscountPerUnit;

    public Product(int id, String name, double price, int stock) {
        this(id, name, price, stock, null);
    }

    public Product(int id, String name, double price, int stock, String imagePath) {
        this(id, name, price, stock, imagePath, false, null);
    }

    public Product(int id, String name, double price, int stock, String imagePath, boolean discountAllowed,
                   java.math.BigDecimal maxDiscountPerUnit) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.stock = stock;
        this.imagePath = imagePath;
        this.maxDiscountPerUnit = ItemDiscounts.scale(maxDiscountPerUnit);
        this.discountAllowed = discountAllowed && this.maxDiscountPerUnit.signum() > 0;
    }

    /** The item as a discount line ("Less" on the item card), for the given quantity. */
    ItemDiscounts.Line discountLine(int quantity) {
        return new ItemDiscounts.Line(id, name, quantity, java.math.BigDecimal.valueOf(price), maxDiscountPerUnit,
                discountAllowed, discountAllowed ? null : "The Admin does not allow a discount on this item.");
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
            Boolean allowed = Json.bool(o, "discount_allowed");
            Double max = Json.number(o, "max_discount_per_unit");
            list.add(new Product(id, name, price, Json.integer(o, "stock", 0), Json.text(o, "image_path"),
                    allowed != null && allowed, max == null ? null : java.math.BigDecimal.valueOf(max)));
        }
        return list;
    }
}
