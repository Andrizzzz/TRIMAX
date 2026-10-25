package com.example.trimax;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.json.JSONObject;
import org.junit.Test;

public class CreateBookingRequestTest {

    private static CreateBookingRequest valid() {
        CreateBookingRequest r = new CreateBookingRequest("11111111-1111-1111-1111-111111111111");
        r.customerName = "  Aling Rosa ";
        r.customerContact = "0945 222 1111";
        r.deliveryAddress = "Citrus, SJDM";
        r.latitude = 14.79;
        r.longitude = 121.06;
        r.schedule = "ASAP";
        r.instructions = "  ";
        r.lines.add(new CreateBookingRequest.Line(1, 2));
        return r;
    }

    @Test
    public void validRequestBuildsTheServerArguments() throws Exception {
        CreateBookingRequest r = valid();
        assertNull(r.validate());
        JSONObject a = r.toRpcArgs();
        assertEquals("11111111-1111-1111-1111-111111111111", a.getString("p_request_id"));
        assertEquals("Aling Rosa", a.getString("p_customer_name"));
        assertEquals(14.79, a.getDouble("p_delivery_latitude"), 1e-9);
        assertEquals(1, a.getJSONArray("p_items").getJSONObject(0).getInt("product_id"));
        assertEquals(2, a.getJSONArray("p_items").getJSONObject(0).getInt("quantity"));
        assertTrue("blank instructions are sent as null", a.isNull("p_special_instructions"));
        assertFalse("the phone never sends a price", a.toString().contains("price"));
        assertFalse("the phone never names a driver", a.toString().contains("driver"));
    }

    @Test
    public void sameIdIsKeptForRetries() {
        CreateBookingRequest generated = new CreateBookingRequest(null);
        assertNotNull(generated.requestId);
        assertEquals(36, generated.requestId.length());
    }

    @Test
    public void invalidInputIsCaughtBeforeSending() {
        CreateBookingRequest r = valid();
        r.customerName = " ";
        assertNotNull(r.validate());

        r = valid();
        r.customerContact = "call me";
        assertNotNull(r.validate());

        r = valid();
        r.latitude = 0;
        r.longitude = 0;
        assertNotNull(r.validate());

        r = valid();
        r.latitude = Double.NaN;
        assertNotNull(r.validate());

        r = valid();
        r.lines.clear();
        assertNotNull(r.validate());

        r = valid();
        r.lines.add(new CreateBookingRequest.Line(2, 0));
        assertNotNull(r.validate());

        r = new CreateBookingRequest("not-a-uuid");
        r.customerName = "x";
        assertNotNull(r.validate());
    }
}
