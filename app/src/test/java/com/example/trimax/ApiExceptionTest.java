package com.example.trimax;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ApiExceptionTest {

    private static ApiException rest(int status, String body) {
        return ApiException.fromResponse(status, body, false);
    }

    @Test
    public void serverRulesKeepTheirMessage() {
        ApiException e = rest(400, "{\"code\":\"TM409\",\"details\":\"STARTED\",\"hint\":null,"
                + "\"message\":\"This delivery changed since you opened it (it is now STARTED). Refresh and try again.\"}");
        assertEquals(ApiException.Kind.CONFLICT, e.getKind());
        assertEquals("TM409", e.getCode());
        assertEquals("STARTED", e.getDetail());
        assertTrue(e.getMessage().startsWith("This delivery changed"));

        assertEquals(ApiException.Kind.NOT_LINKED, rest(400, "{\"code\":\"TM401\",\"message\":\"x\"}").getKind());
        assertEquals(ApiException.Kind.FORBIDDEN, rest(400, "{\"code\":\"TM403\",\"message\":\"x\"}").getKind());
        assertEquals(ApiException.Kind.NOT_FOUND, rest(400, "{\"code\":\"TM404\",\"message\":\"x\"}").getKind());
        assertEquals(ApiException.Kind.INVALID, rest(400, "{\"code\":\"TM422\",\"message\":\"x\"}").getKind());
    }

    @Test
    public void signInProblemsRequireANewSignIn() {
        assertTrue(rest(401, "{\"code\":\"PGRST301\",\"message\":\"JWT expired\"}").requiresSignIn());
        assertTrue(rest(400, "{\"code\":\"TM401\",\"message\":\"not linked\"}").requiresSignIn());
        assertFalse(rest(400, "{\"code\":\"TM409\",\"message\":\"x\"}").requiresSignIn());
    }

    @Test
    public void internalDetailsNeverReachTheScreen() {
        ApiException e = rest(500, "{\"code\":\"XX000\",\"message\":\"relation trimax_private.secret does not exist\"}");
        assertEquals(ApiException.Kind.SERVER, e.getKind());
        assertFalse(e.getMessage().contains("trimax_private"));
        assertEquals(ApiException.Kind.SERVER, rest(502, "<html>Bad gateway</html>").getKind());
        assertEquals(ApiException.Kind.FORBIDDEN, rest(403, "{\"code\":\"42501\",\"message\":\"permission denied for table users\"}").getKind());
        assertFalse(rest(403, "{\"code\":\"42501\",\"message\":\"permission denied for table users\"}").getMessage().contains("users"));
    }

    @Test
    public void missingServerUpdateIsExplained() {
        ApiException e = rest(404, "{\"code\":\"PGRST202\",\"message\":\"Could not find the function public.trimax_my_account\"}");
        assertEquals(ApiException.Kind.SERVER, e.getKind());
        assertTrue(e.getMessage().contains("database update"));
    }

    @Test
    public void authEndpointMessages() {
        ApiException bad = ApiException.fromResponse(400,
                "{\"code\":\"invalid_credentials\",\"msg\":\"Invalid login credentials\"}", true);
        assertEquals(ApiException.Kind.BAD_CREDENTIALS, bad.getKind());
        assertTrue(bad.getMessage().startsWith("Wrong email or password."));
        assertTrue("points first-time users to the setup screen", bad.getMessage().contains("Set up or reset password"));
        // Current Supabase Auth shape: numeric "code", text "error_code". Same for an unknown email.
        ApiException noSuchUser = ApiException.fromResponse(400,
                "{\"code\":400,\"error_code\":\"invalid_credentials\",\"msg\":\"Invalid login credentials\"}", true);
        assertEquals(ApiException.Kind.BAD_CREDENTIALS, noSuchUser.getKind());
        assertEquals(bad.getMessage(), noSuchUser.getMessage());
        ApiException unconfirmed = ApiException.fromResponse(400,
                "{\"code\":400,\"error_code\":\"email_not_confirmed\",\"msg\":\"Email not confirmed\"}", true);
        assertEquals(ApiException.Kind.INVALID, unconfirmed.getKind());
        assertTrue(unconfirmed.getMessage().contains("not been confirmed"));
        assertEquals(ApiException.Kind.SESSION_EXPIRED, ApiException.fromResponse(400,
                "{\"code\":\"refresh_token_not_found\",\"msg\":\"Invalid Refresh Token: Refresh Token Not Found\"}", true).getKind());
        assertEquals(ApiException.Kind.SERVER, ApiException.fromResponse(429, "{\"msg\":\"rate limit\"}", true).getKind());
    }
}
