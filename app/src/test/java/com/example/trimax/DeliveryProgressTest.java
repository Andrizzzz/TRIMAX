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

    /** Exactly trimax_private.delivery_transition_allowed in sql/20261025_01_driver_started_delivered.sql. */
    private static final Set<String> SERVER_RULE = new HashSet<>(Arrays.asList(
            "ASSIGNED>STARTED", "STARTED>DELIVERED", "STARTED>FAILED",
            // rows left by older app builds can only finish or fail
            "PICKED_UP>DELIVERED", "PICKED_UP>FAILED", "ON_THE_WAY>DELIVERED", "ON_THE_WAY>FAILED",
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
        assertEquals("CANCELLED", DeliveryProgress.driverDisplayStatus("Cancelled", "STARTED"));
        assertEquals("STARTED", DeliveryProgress.driverDisplayStatus("In Progress", "STARTED"));
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
    public void assignedShowsStartDeliveryWhichSendsStarted() {
        for (String code : new String[]{"ASSIGNED", null}) {
            DeliveryProgress.NextStep s = DeliveryProgress.nextStep("Confirmed", code);
            assertEquals(DeliveryProgress.START_DELIVERY, s.label);
            assertEquals("STARTED", s.target);
            assertFalse(s.opensProof);
            assertTrue(s.enabled);
        }
        assertTrue(DeliveryProgress.canTransition("ASSIGNED", "STARTED"));
    }

    @Test
    public void startedShowsDropOffWhichOpensTheProofScreen() {
        DeliveryProgress.NextStep s = DeliveryProgress.nextStep("In Progress", "STARTED");
        assertEquals(DeliveryProgress.DROP_OFF, s.label);
        assertTrue(s.opensProof);
        assertTrue(s.enabled);
        assertNull("DELIVERED is only sent from the proof screen (photo + recipient name)", s.target);
        assertTrue(DeliveryProgress.canTransition("STARTED", "DELIVERED"));
        // Rows left by older app builds get the same Drop Off button.
        for (String legacy : new String[]{"PICKED_UP", "ON_THE_WAY", "ARRIVED"}) {
            DeliveryProgress.NextStep l = DeliveryProgress.nextStep("In Progress", legacy);
            assertEquals(legacy, DeliveryProgress.DROP_OFF, l.label);
            assertTrue(legacy, l.opensProof);
            assertTrue(legacy, DeliveryProgress.canTransition(legacy, "DELIVERED"));
        }
    }

    @Test
    public void noIntermediateStepIsEverOffered() {
        for (String booking : new String[]{"Pending", "Confirmed", "In Progress", "Completed", "Cancelled"}) {
            for (String code : CODES) {
                String target = DeliveryProgress.nextStep(booking, code).target;
                assertTrue(booking + "/" + code + " -> " + target, target == null || "STARTED".equals(target));
            }
        }
        for (String from : CODES) {
            for (String intermediate : new String[]{"PICKED_UP", "ON_THE_WAY", "ARRIVED"}) {
                assertFalse(from + " -> " + intermediate, DeliveryProgress.canTransition(from, intermediate));
            }
        }
    }

    @Test
    public void finishedOrCancelledDeliveriesOfferNothing() {
        for (String done : new String[]{"DELIVERED", "FAILED"}) {
            assertFalse(DeliveryProgress.nextStep("In Progress", done).enabled);
        }
        assertFalse(DeliveryProgress.nextStep("Cancelled", "STARTED").enabled);
        assertFalse(DeliveryProgress.nextStep("Completed", "DELIVERED").enabled);
        assertFalse(DeliveryProgress.nextStep("Completed", "STARTED").enabled);
    }

    @Test
    public void refusedStepsGetAnActionableMessage() {
        String old = DeliveryProgress.refusalMessage("A delivery cannot go from ASSIGNED to STARTED.");
        assertTrue(old.startsWith("A delivery cannot go from ASSIGNED to STARTED."));
        assertTrue(old.contains("Tell the office"));
        assertEquals("Enter the name of the person who received the delivery.",
                DeliveryProgress.refusalMessage("Enter the name of the person who received the delivery."));
        assertEquals("The server refused this change.", DeliveryProgress.refusalMessage(null));
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
