package com.example.trimax;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DeliveryProgressTest {

    private static final List<String> CODES = Arrays.asList("ASSIGNED", "STARTED", "PICKED_UP", "ON_THE_WAY",
            "ARRIVED", "DELIVERED", "FAILED");

    /**
     * Exactly trimax_private.delivery_transition_allowed on the live server
     * (TrimaxSystem1 sql/20261025_01_live_baseline.sql): Start, then Drop off / Complete or Failed.
     * PICKED_UP, ON_THE_WAY and ARRIVED appear only on rows saved by older app builds.
     */
    private static final Set<String> SERVER_RULE = new HashSet<>(Arrays.asList(
            "ASSIGNED>STARTED",
            "STARTED>DELIVERED", "STARTED>FAILED",
            "PICKED_UP>DELIVERED", "PICKED_UP>FAILED",
            "ON_THE_WAY>DELIVERED", "ON_THE_WAY>FAILED",
            "ARRIVED>DELIVERED", "ARRIVED>FAILED"));

    @Test
    public void transitionsMatchTheServerExactly() {
        for (String from : CODES) {
            for (String to : CODES) {
                assertEquals(from + ">" + to, SERVER_RULE.contains(from + ">" + to), DeliveryProgress.canTransition(from, to));
            }
        }
    }

    @Test
    public void displayStatusesRoundTrip() {
        for (String code : CODES) {
            String shown = DeliveryProgress.driverDisplayStatus("In Progress", code);
            assertEquals(code, DeliveryProgress.serverCode(shown));
        }
        assertEquals("ARRIVED AT DESTINATION", DeliveryProgress.driverDisplayStatus("In Progress", "ARRIVED"));
        assertEquals("ASSIGNED", DeliveryProgress.driverDisplayStatus("Confirmed", null));
        assertEquals("CANCELLED", DeliveryProgress.driverDisplayStatus("Cancelled", "ON_THE_WAY"));
        assertEquals("COMPLETED", DeliveryProgress.driverDisplayStatus("Completed", "ASSIGNED"));
        assertNull(DeliveryProgress.serverCode("PENDING"));
    }

    @Test
    public void driverCannotStartBeforeTheAdministratorConfirms() {
        DeliveryProgress.NextStep s = DeliveryProgress.nextStep("Pending", "ASSIGNED");
        assertFalse(s.enabled);
        assertNull(s.target);
        assertEquals("WAITING FOR ADMIN CONFIRMATION", s.label);
    }

    @Test
    public void buttonsFollowTheLifecycle() {
        assertEquals("STARTED", DeliveryProgress.nextStep("Confirmed", "ASSIGNED").target);
        assertEquals("STARTED", DeliveryProgress.nextStep("Confirmed", null).target);
        // Once started (or left in an older build's in-between step) the only way on is the proof screen.
        for (String code : Arrays.asList("STARTED", "PICKED_UP", "ON_THE_WAY", "ARRIVED")) {
            DeliveryProgress.NextStep s = DeliveryProgress.nextStep("In Progress", code);
            assertTrue(code, s.opensProof);
            assertTrue(code, s.enabled);
            assertNull("DELIVERED is only sent from the proof screen", s.target);
            assertTrue(code + " can be delivered", DeliveryProgress.canTransition(code, "DELIVERED"));
        }
        for (String done : new String[]{"DELIVERED", "FAILED"}) {
            assertFalse(DeliveryProgress.nextStep("In Progress", done).enabled);
        }
        assertFalse(DeliveryProgress.nextStep("Cancelled", "STARTED").enabled);
        assertFalse(DeliveryProgress.nextStep("Completed", "DELIVERED").enabled);
        // The only status the main button sends directly is Start, and the server allows it.
        assertTrue(DeliveryProgress.canTransition("ASSIGNED", DeliveryProgress.nextStep("Confirmed", "ASSIGNED").target));
    }

    @Test
    public void nothingMovesIntoTheOldInBetweenSteps() {
        for (String from : CODES) {
            for (String old : new String[]{"PICKED_UP", "ON_THE_WAY", "ARRIVED"}) {
                assertFalse(from + ">" + old, DeliveryProgress.canTransition(from, old));
            }
        }
    }

    @Test
    public void underWayMeansStartedButNotFinished() {
        assertFalse(DeliveryProgress.isUnderWay(null));
        assertFalse(DeliveryProgress.isUnderWay("ASSIGNED"));
        assertTrue(DeliveryProgress.isUnderWay("STARTED"));
        assertTrue(DeliveryProgress.isUnderWay("ARRIVED"));
        assertFalse(DeliveryProgress.isUnderWay("DELIVERED"));
        assertFalse(DeliveryProgress.isUnderWay("FAILED"));
    }

    @Test
    public void agentStatusesUseTheBookingStatus() {
        assertEquals("PENDING", DeliveryProgress.agentDisplayStatus("Pending"));
        assertEquals("IN PROGRESS", DeliveryProgress.agentDisplayStatus("In Progress"));
        assertEquals("PENDING", DeliveryProgress.agentDisplayStatus(null));
    }
}
