package com.example.trimax;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The vehicle(s) the Admin assigned to the signed-in driver, from {@code trimax_driver_my_vehicle()}
 * (sql/20261027_03_driver_my_vehicle.sql in the Desktop repository). The plate number and type come
 * only from the server: nothing here is a sample value. An empty list means no vehicle is assigned.
 */
public final class DriverVehicle {

    public final String plateNumber;
    public final String vehicleType;
    public final String status;

    DriverVehicle(String plateNumber, String vehicleType, String status) {
        this.plateNumber = plateNumber;
        this.vehicleType = vehicleType;
        this.status = status;
    }

    /** {"vehicles":[{"plate_number","vehicle_type","status"}...]} -> list (never null). */
    public static List<DriverVehicle> parse(String json) throws JSONException {
        if (json == null || json.trim().isEmpty()) return Collections.emptyList();
        JSONObject o = new JSONObject(json);
        JSONArray a = o.optJSONArray("vehicles");
        if (a == null) return Collections.emptyList();
        List<DriverVehicle> out = new ArrayList<>();
        for (int i = 0; i < a.length(); i++) {
            JSONObject v = a.optJSONObject(i);
            if (v == null) continue;
            String plate = text(v, "plate_number");
            if (plate == null) continue;   // a row without a plate number is not shown
            out.add(new DriverVehicle(plate, text(v, "vehicle_type"), text(v, "status")));
        }
        return out;
    }

    private static String text(JSONObject o, String key) {
        if (o.isNull(key)) return null;
        String s = o.optString(key, "").trim();
        return s.isEmpty() ? null : s;
    }

    /** First line on the profile, e.g. "Vehicle type: Motorcycle". */
    public String typeLine() {
        return "Vehicle type: " + (vehicleType == null ? "Not set" : vehicleType);
    }

    /** Second line, e.g. "Plate number: NBC 1234 • Available". */
    public String plateLine() {
        return "Plate number: " + plateNumber + (status == null ? "" : " • " + status);
    }

    /** What the profile card shows for the whole list (one block per vehicle). */
    public static String describe(List<DriverVehicle> vehicles) {
        if (vehicles == null || vehicles.isEmpty()) {
            return "No vehicle assigned to you yet. Ask the administrator to assign one.";
        }
        StringBuilder b = new StringBuilder();
        for (DriverVehicle v : vehicles) {
            if (b.length() > 0) b.append("\n\n");
            b.append(v.typeLine()).append('\n').append(v.plateLine());
        }
        return b.toString();
    }
}
