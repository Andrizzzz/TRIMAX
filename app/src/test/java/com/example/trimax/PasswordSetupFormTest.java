package com.example.trimax;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class PasswordSetupFormTest {

    @Test
    public void aCompleteFormPasses() {
        assertNull(PasswordSetupForm.problem(" tim@example.test ", "ABCD2345", "Sunflower7", "Sunflower7"));
    }

    @Test
    public void theCodeIsTypedAnyWay() {
        assertEquals("ABCD2345", PasswordSetupForm.normalizeCode(" abcd-2345 "));
        assertEquals("ABCD2345", PasswordSetupForm.normalizeCode("ABCD 2345"));
        assertNull(PasswordSetupForm.problem("tim@example.test", "abcd 2345", "Sunflower7", "Sunflower7"));
        assertEquals("", PasswordSetupForm.normalizeCode(null));
    }

    @Test
    public void eachMistakeIsNamed() {
        assertTrue(PasswordSetupForm.problem("tim", "ABCD2345", "Sunflower7", "Sunflower7").contains("email"));
        assertTrue(PasswordSetupForm.problem("tim@example.test", "AB", "Sunflower7", "Sunflower7").contains("code"));
        assertTrue(PasswordSetupForm.problem("tim@example.test", "ABCD!345", "Sunflower7", "Sunflower7").contains("code"));
        assertTrue(PasswordSetupForm.problem("tim@example.test", "ABCD2345", "Sun7", "Sun7").contains("8"));
        assertTrue(PasswordSetupForm.problem("tim@example.test", "ABCD2345", "sunflowers", "sunflowers").contains("number"));
        assertTrue(PasswordSetupForm.problem("tim@example.test", "ABCD2345", "12345678", "12345678").contains("letter"));
        assertTrue(PasswordSetupForm.problem("tim@example.test", "ABCD2345", "Sunflower7", "Sunflower8").contains("not the same"));
        StringBuilder longOne = new StringBuilder("a1");
        for (int i = 0; i < 71; i++) longOne.append('x');
        assertNotNull(PasswordSetupForm.problem("tim@example.test", "ABCD2345", longOne.toString(), longOne.toString()));
        assertNull(PasswordSetupForm.problem("tim@example.test", "ABCD2345", "\u00d1and\u00fa2026", "\u00d1and\u00fa2026"));
    }
}
