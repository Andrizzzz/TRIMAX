package com.example.trimax;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/** A customer saved on the TRIMAX server for this agent (trimax_agent_save_customer / _list_customers). */
final class ServerCustomer {

    int clientId;
    String name, contact, address, photoPath;
    Double latitude, longitude;
    /** Only after a save: true when the server created the customer (false: an existing one was updated). */
    boolean created;

    static ServerCustomer parse(String json) throws JSONException {
        return from(new JSONObject(json));
    }

    static List<ServerCustomer> parseList(String json) throws JSONException {
        String t = json == null ? "" : json.trim();
        List<ServerCustomer> out = new ArrayList<>();
        if (t.isEmpty() || "null".equals(t)) return out;
        JSONArray a = new JSONArray(t);
        for (int i = 0; i < a.length(); i++) {
            JSONObject o = a.optJSONObject(i);
            if (o != null) out.add(from(o));
        }
        return out;
    }

    static ServerCustomer from(JSONObject o) throws JSONException {
        ServerCustomer c = new ServerCustomer();
        Double id = Json.number(o, "client_id");
        if (id == null) throw new JSONException("client_id missing");
        c.clientId = (int) Math.round(id);
        c.name = Json.text(o, "name");
        c.contact = Json.text(o, "contact");
        c.address = Json.text(o, "address");
        c.photoPath = Json.text(o, "photo_path");
        c.latitude = Json.number(o, "latitude");
        c.longitude = Json.number(o, "longitude");
        c.created = Boolean.TRUE.equals(Json.bool(o, "created"));
        return c;
    }
}
