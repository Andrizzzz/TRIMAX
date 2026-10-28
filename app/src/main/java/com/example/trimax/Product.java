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
    /** Cases in stock; can end in .5 since half cases (database update 20261028_07). */
    public final double stock;
    /** Path of the product picture in the public product-images bucket, or null (no picture / older server). */
    public final String imagePath;
    /**
     * Whether the Admin lets agents take something off each piece of this item, and the most per piece
     * (the server works this out from the price, puhunan and minimum patong; the phone never sees those).
     * An older server sends neither, so the item simply has no "Less" button.
     */
    public final boolean discountAllowed;
    public final java.math.BigDecimal maxDiscountPerUnit;
    /**
     * Half case (20261028_07): the price of half a case set by the Admin, or null when the product is sold by
     * the whole case only (or the server is older). A half case takes 0.5 off the stock.
     */
    public final Double halfPrice;
    /** The most an agent may take off one half case (0 = no discount on half cases). */
    public final java.math.BigDecimal halfMaxDiscountPerUnit;

    public Product(int id, String name, double price, double stock) {
        this(id, name, price, stock, null);
    }

    public Product(int id, String name, double price, double stock, String imagePath) {
        this(id, name, price, stock, imagePath, false, null);
    }

    public Product(int id, String name, double price, double stock, String imagePath, boolean discountAllowed,
                   java.math.BigDecimal maxDiscountPerUnit) {
        this(id, name, price, stock, imagePath, discountAllowed, maxDiscountPerUnit, null, null);
    }

    public Product(int id, String name, double price, double stock, String imagePath, boolean discountAllowed,
                   java.math.BigDecimal maxDiscountPerUnit, Double halfPrice, java.math.BigDecimal halfMaxDiscountPerUnit) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.stock = stock;
        this.imagePath = imagePath;
        this.maxDiscountPerUnit = ItemDiscounts.scale(maxDiscountPerUnit);
        this.discountAllowed = discountAllowed && this.maxDiscountPerUnit.signum() > 0;
        this.halfPrice = halfPrice != null && halfPrice > 0 ? halfPrice : null;
        this.halfMaxDiscountPerUnit = ItemDiscounts.scale(this.halfPrice == null ? null : halfMaxDiscountPerUnit);
    }

    /** True when the Admin set a half case price, so the agent can order half cases. */
    public boolean sellsHalfCase() {
        return halfPrice != null;
    }

    /** True when an agent may take something off a half case. */
    public boolean halfDiscountAllowed() {
        return sellsHalfCase() && discountAllowed && halfMaxDiscountPerUnit.signum() > 0;
    }

    /** The item as a discount line ("Less" on the item card), for the given quantity. */
    ItemDiscounts.Line discountLine(int quantity) {
        return discountLine(quantity, false);
    }

    /** The whole-case or half-case item as a discount line, for the given quantity. */
    ItemDiscounts.Line discountLine(int quantity, boolean half) {
        if (half) {
            return new ItemDiscounts.Line(id, name, quantity, java.math.BigDecimal.valueOf(halfPrice == null ? 0 : halfPrice),
                    halfMaxDiscountPerUnit, halfDiscountAllowed(), true,
                    halfDiscountAllowed() ? null : "The Admin does not allow a discount on this item.");
        }
        return new ItemDiscounts.Line(id, name, quantity, java.math.BigDecimal.valueOf(price), maxDiscountPerUnit,
                discountAllowed, discountAllowed ? null : "The Admin does not allow a discount on this item.");
    }

    /** "72", "72.5": a stock count as people write it. */
    public static String stockText(double stock) {
        if (Math.abs(stock - Math.rint(stock)) < 1e-9) return String.valueOf((long) Math.rint(stock));
        return java.math.BigDecimal.valueOf(stock).stripTrailingZeros().toPlainString();
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
            Double stock = Json.number(o, "stock");
            Double half = Json.number(o, "half_price");                       // 20261028_07; absent before
            Double halfMax = Json.number(o, "half_max_discount_per_unit");
            list.add(new Product(id, name, price, stock == null ? 0 : stock, Json.text(o, "image_path"),
                    allowed != null && allowed, max == null ? null : java.math.BigDecimal.valueOf(max),
                    half, halfMax == null ? null : java.math.BigDecimal.valueOf(halfMax)));
        }
        return list;
    }
}
