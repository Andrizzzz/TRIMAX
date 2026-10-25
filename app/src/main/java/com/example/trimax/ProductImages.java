package com.example.trimax;

import java.util.regex.Pattern;

/** Where product pictures live (pure Java, so the rules can be unit-tested without a phone). */
final class ProductImages {

    /** Same rule as products.image_path on the server (sql/20261025_06_product_images.sql). */
    static final Pattern PATH = Pattern.compile("products/[0-9]{1,9}/[A-Za-z0-9_-]{1,80}\\.jpg");

    private ProductImages() {}

    /** Public address of a product picture, or null when the product has none or the path is not valid. */
    static String urlFor(String imagePath) {
        if (imagePath == null || !PATH.matcher(imagePath).matches()) return null;
        return SupabaseConfig.PROJECT_URL.replaceAll("/+$", "") + "/storage/v1/object/public/product-images/" + imagePath;
    }
}
