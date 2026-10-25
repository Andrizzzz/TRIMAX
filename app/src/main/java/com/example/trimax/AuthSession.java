package com.example.trimax;

/** A Supabase Auth session. Never log or display the tokens. */
public final class AuthSession {

    public final String accessToken;
    public final String refreshToken;
    /** Seconds since the epoch at which the access token stops working. */
    public final long expiresAt;
    public final String userId;
    public final String email;

    public AuthSession(String accessToken, String refreshToken, long expiresAt, String userId, String email) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.expiresAt = expiresAt;
        this.userId = userId;
        this.email = email;
    }

    /** True when the access token is expired or will expire within marginSeconds. */
    public boolean expiresWithin(long nowSeconds, long marginSeconds) {
        return expiresAt - nowSeconds <= marginSeconds;
    }

    /** Where sessions are kept between app starts. */
    public interface Store {
        AuthSession load();

        void save(AuthSession session);

        void clear();
    }
}
