package com.example.trimax;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.json.JSONObject;
import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;

/** New Booking: how the agent took the order, Visit (photo required) or Text / Call (20261029_02). */
public class OrderViaTest {

    private static final String UID = "0f8fad5b-d9cb-469f-a165-70867728950e";

    private static CreateBookingRequest booking() {
        CreateBookingRequest r = new CreateBookingRequest("11111111-1111-1111-1111-111111111111");
        r.customerName = "Aling Rosa";
        r.customerContact = "09452221111";
        r.deliveryAddress = "Citrus, SJDM";
        r.latitude = 14.79;
        r.longitude = 121.06;
        r.lines.add(new CreateBookingRequest.Line(1, 2));
        return r;
    }

    private static File photo() throws Exception {
        File f = File.createTempFile("visit", ".jpg");
        f.deleteOnExit();
        try (FileOutputStream out = new FileOutputStream(f)) {
            out.write(new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 1, 2, 3});
        }
        return f;
    }

    private static CreateBookingRequest visit() throws Exception {
        CreateBookingRequest r = booking();
        r.orderVia = CreateBookingRequest.VIA_VISIT;
        r.visitPhoto = photo();
        r.visitPhotoName = CreateBookingRequest.visitPhotoName(1793000000000L, "ab12cd34-0000-4000-8000-000000000000");
        r.visitLatitude = 14.8123;
        r.visitLongitude = 121.0456;
        r.visitTakenAtIso = CreateBookingRequest.isoUtc(1793000000000L);
        return r;
    }

    @Test
    public void textOrCallNeedsNoPhoto() throws Exception {
        CreateBookingRequest r = booking();
        r.orderVia = CreateBookingRequest.VIA_TEXT_CALL;
        assertNull(r.validate());
        assertTrue(r.usesOrderVia());
        JSONObject a = r.toRpcArgsV2(null);
        assertEquals("TEXT_CALL", a.getString("p_order_via"));
        assertTrue(a.isNull("p_visit_photo_path"));
        assertTrue(a.isNull("p_visit_latitude"));
        assertTrue(a.isNull("p_visit_taken_at"));
        assertEquals("the booking itself is the same", r.toRpcArgs().getJSONArray("p_items").toString(),
                a.getJSONArray("p_items").toString());
    }

    @Test
    public void visitSendsThePhotoPlaceAndTime() throws Exception {
        CreateBookingRequest r = visit();
        assertNull(r.validate());
        JSONObject a = r.toRpcArgsV2(UID + "/" + r.visitPhotoName);
        assertEquals("VISIT", a.getString("p_order_via"));
        assertEquals(UID + "/visit_1793000000000_ab12cd34.jpg", a.getString("p_visit_photo_path"));
        assertEquals(14.8123, a.getDouble("p_visit_latitude"), 1e-9);
        assertEquals(121.0456, a.getDouble("p_visit_longitude"), 1e-9);
        assertEquals("2026-10-26T07:33:20Z", a.getString("p_visit_taken_at"));
    }

    @Test
    public void visitWithoutAPhotoIsStoppedOnThePhone() throws Exception {
        CreateBookingRequest r = visit();
        r.visitPhoto = null;
        assertEquals("Take a photo of the visit before confirming the booking.", r.validate());

        r = visit();
        r.visitPhoto = new File("/no/such/photo.jpg");
        assertNotNull(r.validate());

        r = visit();
        r.visitPhotoName = "../other_folder.jpg";
        assertNotNull("a bad photo name is refused", r.validate());
    }

    @Test
    public void visitWithoutALocationIsStillAllowed() throws Exception {
        CreateBookingRequest r = visit();
        r.visitLatitude = null;
        r.visitLongitude = null;
        assertNull(r.validate());
        JSONObject a = r.toRpcArgsV2(UID + "/" + r.visitPhotoName);
        assertTrue(a.isNull("p_visit_latitude"));
        assertTrue(a.isNull("p_visit_longitude"));

        r = visit();
        r.visitLatitude = 0.0;
        r.visitLongitude = 0.0;
        assertTrue("0,0 means no GPS fix: sent as no location", r.toRpcArgsV2(UID + "/x").isNull("p_visit_latitude"));

        r = visit();
        r.visitLongitude = null;
        assertNotNull("half a location is refused", r.validate());
    }

    @Test
    public void anUnknownChoiceIsRefused() {
        CreateBookingRequest r = booking();
        r.orderVia = "EMAIL";
        assertEquals("Choose how you took this order: Visit or Text / Call.", r.validate());
    }

    @Test
    public void withoutTheChoiceTheOlderCallIsUsed() throws Exception {
        CreateBookingRequest r = booking();
        assertNull(r.validate());
        assertFalse(r.usesOrderVia());
        assertFalse(r.toRpcArgs().has("p_order_via"));
    }

    @Test
    public void photoNamesFollowTheServerRule() {
        String n = CreateBookingRequest.visitPhotoName(1793000000000L, "ab12cd34-0000-4000-8000-000000000000");
        assertEquals("visit_1793000000000_ab12cd34.jpg", n);
        assertTrue(CreateBookingRequest.VISIT_PHOTO_NAME.matcher(n).matches());
        assertTrue(CreateBookingRequest.VISIT_PHOTO_NAME.matcher(CreateBookingRequest.visitPhotoName(1L, null)).matches());
        String fresh = CreateBookingRequest.newVisitPhotoName();
        assertTrue(CreateBookingRequest.VISIT_PHOTO_NAME.matcher(fresh).matches());
        assertFalse("each new photo gets its own name", fresh.equals(CreateBookingRequest.newVisitPhotoName()));
    }
}
