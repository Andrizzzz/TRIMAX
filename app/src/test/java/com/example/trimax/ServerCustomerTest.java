package com.example.trimax;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.json.JSONException;
import org.junit.Test;

import java.util.List;

/** Replies of trimax_agent_save_customer and trimax_agent_list_customers. */
public class ServerCustomerTest {

    static final String SAVED = "{\"name\": \"Aling Nena Store\", \"contact\": \"0917 000 1001\", \"address\": \"Tungko, SJDM\", "
            + "\"created\": true, \"latitude\": 14.7804, \"client_id\": 9, \"longitude\": 121.0632, "
            + "\"photo_path\": \"7d5c2f3e-1111-4222-8333-944455556666/customer_1761490000000_ab12cd34.jpg\", "
            + "\"photo_updated_at\": \"2026-10-26T22:43:11.054586+08:00\"}";

    @Test
    public void parsesASavedCustomer() throws Exception {
        ServerCustomer c = ServerCustomer.parse(SAVED);
        assertEquals(9, c.clientId);
        assertEquals("Aling Nena Store", c.name);
        assertEquals("0917 000 1001", c.contact);
        assertEquals(14.7804, c.latitude, 1e-9);
        assertTrue(c.created);
        assertTrue(c.photoPath.endsWith("/customer_1761490000000_ab12cd34.jpg"));
    }

    @Test
    public void parsesTheListIncludingOldCustomersWithoutPhotos() throws Exception {
        List<ServerCustomer> list = ServerCustomer.parseList("[" + SAVED.replace("\"created\": true, ", "") + ", "
                + "{\"name\": \"Old Store\", \"contact\": \"0918 111 2222\", \"address\": null, \"latitude\": null, "
                + "\"client_id\": 3, \"longitude\": null, \"photo_path\": null, \"photo_updated_at\": null}]");
        assertEquals(2, list.size());
        assertFalse(list.get(0).created);
        assertNull(list.get(1).photoPath);
        assertNull(list.get(1).latitude);
        assertTrue(ServerCustomer.parseList("[]").isEmpty());
        assertTrue(ServerCustomer.parseList("null").isEmpty());
        assertTrue(ServerCustomer.parseList("").isEmpty());
    }

    @Test
    public void aCustomerWithoutAnIdIsRejected() {
        try {
            ServerCustomer.parse("{\"name\": \"x\"}");
            fail();
        } catch (JSONException expected) {
            // the reply is not a customer
        }
    }
}
