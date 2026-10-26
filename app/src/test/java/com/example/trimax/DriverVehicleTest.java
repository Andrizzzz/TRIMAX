package com.example.trimax;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.junit.Test;

/** The Driver profile's vehicle comes only from trimax_driver_my_vehicle(); no sample values. */
public class DriverVehicleTest {

    @Test
    public void oneAssignedVehicle() throws Exception {
        List<DriverVehicle> v = DriverVehicle.parse("{\"vehicles\":[{\"plate_number\":\"NBC-1234\","
                + "\"vehicle_type\":\"Motorcycle\",\"status\":\"Available\"}],\"checked_at\":\"2026-10-27T03:50:00+08:00\"}");
        assertEquals(1, v.size());
        assertEquals("Vehicle type: Motorcycle", v.get(0).typeLine());
        assertEquals("Plate number: NBC-1234 • Available", v.get(0).plateLine());
        assertEquals("Vehicle type: Motorcycle\nPlate number: NBC-1234 • Available", DriverVehicle.describe(v));
    }

    @Test
    public void noVehicleAssignedIsAClearEmptyState() throws Exception {
        List<DriverVehicle> v = DriverVehicle.parse("{\"vehicles\":[],\"checked_at\":\"x\"}");
        assertTrue(v.isEmpty());
        assertTrue(DriverVehicle.describe(v).startsWith("No vehicle assigned"));
        assertTrue(DriverVehicle.describe(DriverVehicle.parse("")).startsWith("No vehicle assigned"));
    }

    @Test
    public void twoVehiclesAndMissingFields() throws Exception {
        List<DriverVehicle> v = DriverVehicle.parse("{\"vehicles\":[{\"plate_number\":\"TRK-001\",\"vehicle_type\":null,"
                + "\"status\":null},{\"plate_number\":\" \"},{\"plate_number\":\"VAN-003\",\"vehicle_type\":\"Van\"}]}");
        assertEquals("rows without a plate number are skipped", 2, v.size());
        assertEquals("Vehicle type: Not set", v.get(0).typeLine());
        assertEquals("Plate number: TRK-001", v.get(0).plateLine());
        assertTrue(DriverVehicle.describe(v).contains("\n\nVehicle type: Van"));
    }

    @Test
    public void neverTheOldSampleText() throws Exception {
        String shown = DriverVehicle.describe(DriverVehicle.parse("{\"vehicles\":[{\"plate_number\":\"XYZ 987\","
                + "\"vehicle_type\":\"Truck\"}]}"));
        assertFalse(shown.contains("ABC 1234"));
        assertFalse(shown.contains("Honda"));
        assertFalse(shown.contains("Motorcycle"));
    }

    @Test(expected = org.json.JSONException.class)
    public void notJsonIsAnError() throws Exception {
        DriverVehicle.parse("<html>gateway</html>");
    }
}
