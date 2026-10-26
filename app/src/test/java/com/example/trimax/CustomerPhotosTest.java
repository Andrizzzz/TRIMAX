package com.example.trimax;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** File names and matching rules of customer photos (the server checks the same name pattern). */
public class CustomerPhotosTest {

    /** The server's rule: '^' || auth.uid() || '/customer_[A-Za-z0-9_-]{1,80}\.jpg$'. */
    private static final String SERVER_NAME_RULE = "customer_[A-Za-z0-9_-]{1,80}\\.jpg";

    @Test
    public void keyMatchesTheSamePhoneInAnyFormat() {
        assertEquals("9171234567", CustomerPhotos.key("0917-123-4567"));
        assertEquals("9171234567", CustomerPhotos.key("+63 917 123 4567"));
        assertEquals(CustomerDirectory.key("x", "0917 123 4567"), "p:" + CustomerPhotos.key("+639171234567"));
        assertNull(CustomerPhotos.key("123"));
        assertNull(CustomerPhotos.key(null));
    }

    @Test
    public void objectNamesPassTheServerRule() {
        assertEquals("customer_1761490000000_ab12cd34.jpg",
                CustomerPhotos.objectName(1761490000000L, "ab12cd34-5678-90ef-aaaa-bbbbccccdddd"));
        assertTrue(CustomerPhotos.objectName(1L, "../../etc").matches(SERVER_NAME_RULE));
        assertTrue(CustomerPhotos.objectName(1L, null).matches(SERVER_NAME_RULE));
        for (int i = 0; i < 20; i++) assertTrue(CustomerPhotos.newObjectName().matches(SERVER_NAME_RULE));
    }

    @Test
    public void onlyPictureTypesArePicked() {
        assertTrue(CustomerPhotos.acceptedPickType("image/jpeg"));
        assertTrue(CustomerPhotos.acceptedPickType("IMAGE/PNG"));
        assertTrue(CustomerPhotos.acceptedPickType("image/heic"));
        assertFalse(CustomerPhotos.acceptedPickType("image/gif"));
        assertFalse(CustomerPhotos.acceptedPickType("application/pdf"));
        assertFalse(CustomerPhotos.acceptedPickType(null));
    }

    @Test
    public void uploadLimitStaysUnderTheBucketLimit() {
        assertTrue(CustomerPhotos.MAX_BYTES < 5 * 1024 * 1024);
        assertTrue(CustomerPhotos.MAX_SIDE <= 1600);
    }
}
