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

    /** Exactly trimax_private.delivery_transition_allowed in the server migration. */
    private static final Set<String> SERVER_RULE = new HashSet<>(Arrays.asList(
            "ASSIGNED>STARTED", "STARTED>PICKED_UP", "STARTED>ON_THE_WAY", "PICKED_UP>ON_THE_WAY",
            "ON_THE_WAY>ARRIVED", "ARRIVED>DELIVERED",
            "STARTED>FAILED", "PICKED_UP>FAILED", "ON_THE_WAY>FAILED", "ARRIVED>FAILED"));

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
        assertEquals("PICKED_UP", DeliveryProgress.nextStep("In Progress", "STARTED").target);
        assertEquals("ON_THE_WAY", DeliveryProgress.nextStep("In Progress", "PICKED_UP").target);
        assertEquals("ARRIVED", DeliveryProgress.nextStep("In Progress", "ON_THE_WAY").target);
        DeliveryProgress.NextStep arrived = DeliveryProgress.nextStep("In Progress", "ARRIVED");
        assertTrue(arrived.opensProof);
        assertNull("DELIVERED is only sent from the proof screen", arrived.target);
        for (String done : new String[]{"DELIVERED", "FAILED"}) {
            assertFalse(DeliveryProgress.nextStep("In Progress", done).enabled);
        }
        assertFalse(DeliveryProgress.nextStep("Cancelled", "ON_THE_WAY").enabled);
        assertFalse(DeliveryProgress.nextStep("Completed", "DELIVERED").enabled);
        // Every target the screen can send is a step the server allows.
        for (String code : Arrays.asList("ASSIGNED", "STARTED", "PICKED_UP", "ON_THE_WAY")) {
            String target = DeliveryProgress.nextStep("In Progress", code).target;
            assertTrue(code + " -> " + target, DeliveryProgress.canTransition(code, target));
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
