package com.example.trimax;

import org.json.JSONException;
import org.json.JSONObject;

/** The signed-in person as the TRIMAX server sees them (role and status come from the desktop accounts). */
public final class MobileAccount {

    public final String username;
    public final String fullName;
    public final String email;
    public final String contact;
    /** "AGENT" or "DRIVER". */
    public final String role;
    /** Signed in with a temporary password from the administrator: choose a new one first. */
    public boolean mustChangePassword;

    public MobileAccount(String username, String fullName, String email, String contact, String role) {
        this.username = username;
        this.fullName = fullName;
        this.email = email;
        this.contact = contact;
        this.role = role;
    }

    public boolean isDriver() {
        return "DRIVER".equals(role);
    }

    public static MobileAccount parse(String json) throws JSONException {
        JSONObject o = new JSONObject(json);
        String role = Json.text(o, "role");
        String username = Json.text(o, "username");
        if (username == null || role == null || (!"AGENT".equals(role) && !"DRIVER".equals(role))) {
            throw new JSONException("Unexpected account response");
        }
        String name = Json.text(o, "full_name");
        return new MobileAccount(username, name != null ? name : username, Json.text(o, "email"),
                Json.text(o, "contact_num"), role);
    }

    /** For the existing SessionManager, which stores a local User. */
    public User toUser() {
        return new User(0, username, "", role, fullName, email, contact);
    }
}
