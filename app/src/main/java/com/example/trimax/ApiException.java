package com.example.trimax;

import org.json.JSONObject;

/**
 * A failed call to the TRIMAX server, with a message that is safe to show on screen.
 *
 * Server rules raise SQLSTATE codes TM401..TM422 (see sql/20261023_01_mobile_booking_integration.sql in
 * the desktop repository); their messages are written for users and are shown as they are. Everything
 * else gets a generic message, so internal details never reach the screen.
 */
public final class ApiException extends Exception {

    public enum Kind {
        /** No connection / host unreachable. Safe to retry. */
        NETWORK,
        /** The server did not answer in time. The request may or may not have been applied. */
        TIMEOUT,
        /** The sign-in expired or was revoked. Sign in again. */
        SESSION_EXPIRED,
        /** Signed in, but the sign-in is not linked to an active TRIMAX account (or was disabled). */
        NOT_LINKED,
        /** Wrong role for this action. */
        FORBIDDEN,
        /** Not found, or not yours. */
        NOT_FOUND,
        /** Changed meanwhile, not enough stock, duplicate, already finished. Refresh and look again. */
        CONFLICT,
        /** Invalid input or a step that is not allowed. */
        INVALID,
        /** Wrong email or password. */
        BAD_CREDENTIALS,
        /** Anything else on the server side. */
        SERVER
    }

    private final Kind kind;
    private final String code;
    private final String detail;

    public ApiException(Kind kind, String message) {
        this(kind, message, null, null);
    }

    public ApiException(Kind kind, String message, String code, String detail) {
        super(message);
        this.kind = kind;
        this.code = code;
        this.detail = detail;
    }

    public Kind getKind() {
        return kind;
    }

    /** Server error code (e.g. "TM409", "PGRST301", "invalid_grant"), or null. */
    public String getCode() {
        return code;
    }

    /** Extra machine-readable detail; for TM409 delivery conflicts it is the delivery's current status. */
    public String getDetail() {
        return detail;
    }

    /** The user has to sign in again before anything else will work. */
    public boolean requiresSignIn() {
        return kind == Kind.SESSION_EXPIRED || kind == Kind.NOT_LINKED;
    }

    static ApiException network() {
        return new ApiException(Kind.NETWORK, "No internet connection. Check your connection and try again.");
    }

    static ApiException timeout() {
        return new ApiException(Kind.TIMEOUT, "The server took too long to answer. Check your connection and try again.");
    }

    static ApiException sessionExpired() {
        return new ApiException(Kind.SESSION_EXPIRED, "Your sign-in has expired. Please sign in again.");
    }

    /**
     * Builds the error for a non-2xx HTTP response from Supabase (PostgREST or Auth).
     * @param authEndpoint true for /auth/v1 calls (sign-in, refresh), false for /rest/v1
     */
    static ApiException fromResponse(int httpStatus, String body, boolean authEndpoint) {
        String code = null, message = null, details = null;
        try {
            JSONObject json = new JSONObject(body == null ? "" : body);
            code = firstText(json, "code", "error_code", "error");
            message = firstText(json, "message", "msg", "error_description");
            details = firstText(json, "details");
        } catch (Exception notJson) {
            // fall through to the generic messages below
        }

        if (code != null && code.startsWith("TM") && message != null) {
            switch (code) {
                case "TM401": return new ApiException(Kind.NOT_LINKED, message, code, details);
                case "TM403": return new ApiException(Kind.FORBIDDEN, message, code, details);
                case "TM404": return new ApiException(Kind.NOT_FOUND, message, code, details);
                case "TM409": return new ApiException(Kind.CONFLICT, message, code, details);
                case "TM422": return new ApiException(Kind.INVALID, message, code, details);
                default:      return new ApiException(Kind.SERVER, message, code, details);
            }
        }

        if (authEndpoint) {
            String lower = message == null ? "" : message.toLowerCase(java.util.Locale.ROOT);
            if (lower.contains("invalid login credentials")) {
                return new ApiException(Kind.BAD_CREDENTIALS, "Wrong email or password.", code, null);
            }
            if (lower.contains("email not confirmed")) {
                return new ApiException(Kind.INVALID, "This email address has not been confirmed yet. Ask your administrator.", code, null);
            }
            if (lower.contains("refresh token") || "refresh_token_not_found".equals(code)
                    || "refresh_token_already_used".equals(code) || "session_not_found".equals(code)) {
                return new ApiException(Kind.SESSION_EXPIRED, sessionExpired().getMessage(), code, null);
            }
            if (httpStatus == 429) {
                return new ApiException(Kind.SERVER, "Too many attempts. Wait a minute and try again.", code, null);
            }
            if (httpStatus >= 400 && httpStatus < 500) {
                return new ApiException(Kind.BAD_CREDENTIALS, "Sign-in failed. Check your email and password.", code, null);
            }
        }

        if (httpStatus == 401 || "PGRST301".equals(code) || "PGRST303".equals(code)) {
            return new ApiException(Kind.SESSION_EXPIRED, sessionExpired().getMessage(), code, null);
        }
        if (httpStatus == 403 || "42501".equals(code)) {
            return new ApiException(Kind.FORBIDDEN, "You are not allowed to do this.", code, null);
        }
        if ("PGRST202".equals(code) || (httpStatus == 404 && !authEndpoint)) {
            return new ApiException(Kind.SERVER,
                    "The server is not set up for the mobile app yet. Ask your administrator to apply the TRIMAX database update.",
                    code, null);
        }
        return new ApiException(Kind.SERVER, "The server had a problem (" + httpStatus + "). Please try again.", code, null);
    }

    /** First present, non-null text value among the keys (JSON null and missing are skipped). */
    private static String firstText(JSONObject json, String... keys) {
        for (String k : keys) {
            String v = Json.text(json, k);
            if (v != null && !v.isEmpty()) return v;
        }
        return null;
    }
}
