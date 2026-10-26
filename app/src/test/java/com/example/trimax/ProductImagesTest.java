package com.example.trimax;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.List;

import org.junit.Test;

/** Product pictures: which paths become addresses, and what the product list carries. */
public class ProductImagesTest {

    @Test
    public void validPathBecomesThePublicAddress() {
        assertEquals(SupabaseConfig.PROJECT_URL + "/storage/v1/object/public/product-images/products/12/abc123.jpg",
                ProductImages.urlFor("products/12/abc123.jpg"));
    }

    @Test
    public void anythingElseShowsThePlaceholder() {
        assertNull(ProductImages.urlFor(null));
        assertNull(ProductImages.urlFor(""));
        assertNull(ProductImages.urlFor("products/12/../../secret.jpg"));
        assertNull(ProductImages.urlFor("https://evil.example/x.jpg"));
        assertNull(ProductImages.urlFor("products/12/abc.png"));
        assertNull(ProductImages.urlFor("other/12/abc.jpg"));
    }

    @Test
    public void productListCarriesThePictureWhenThereIsOne() throws Exception {
        List<Product> list = Product.parseList("[{\"product_id\":1,\"product_name\":\"Coke\",\"price\":180,\"stock\":5,"
                + "\"image_path\":\"products/1/a1b2.jpg\"},{\"product_id\":2,\"product_name\":\"Sprite\",\"price\":180,"
                + "\"stock\":0,\"image_path\":null},{\"product_id\":3,\"product_name\":\"Royal\",\"price\":175,\"stock\":1}]");
        assertEquals(3, list.size());
        assertEquals("products/1/a1b2.jpg", list.get(0).imagePath);
        assertNull("no picture", list.get(1).imageUrl());
        assertNull("older server without image_path", list.get(2).imageUrl());
    }
}
