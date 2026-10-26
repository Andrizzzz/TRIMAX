package com.example.trimax;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Checks the "Set up or reset password" form before anything is sent. Same password rules as the TRIMAX
 * server (at least 8 characters, a letter and a number, at most 72 bytes). Plain Java, so it is unit-tested.
 */
final class PasswordSetupForm {

    private static final Pattern EMAIL = Pattern.compile("[^@\\s]+@[^@\\s]+\\.[^@\\s]+");
    private static final Pattern CODE = Pattern.compile("[A-Z0-9]{6,12}");

    private PasswordSetupForm() {
    }

    /** The code as the server expects it: capitals, without spaces or dashes ("abcd-2345" -> "ABCD2345"). */
    static String normalizeCode(String code) {
        return code == null ? "" : code.replaceAll("[\\s-]", "").toUpperCase(Locale.ROOT);
    }

    /** Null when the form can be sent, otherwise what to fix. */
    static String problem(String email, String code, String password, String confirm) {
        String e = email == null ? "" : email.trim();
        if (e.isEmpty() || e.length() > 254 || !EMAIL.matcher(e).matches()) {
            return "Enter the email address on your TRIMAX account.";
        }
        if (!CODE.matcher(normalizeCode(code)).matches()) {
            return "Enter the code from the TRIMAX email (8 letters and numbers).";
        }
        String p = password == null ? "" : password;
        if (p.length() < 8) return "The password must be at least 8 characters.";
        if (p.getBytes(StandardCharsets.UTF_8).length > 72) return "The password is too long (at most 72 characters).";
        boolean letter = false, digit = false;
        for (int i = 0; i < p.length(); ) {
            int cp = p.codePointAt(i);
            if (Character.isLetter(cp)) letter = true;
            if (Character.isDigit(cp)) digit = true;
            i += Character.charCount(cp);
        }
        if (!letter || !digit) return "The password must contain at least one letter and one number.";
        if (!p.equals(confirm)) return "The two passwords are not the same.";
        return null;
    }
}
