package com.example.trimax;

/**
 * Which Supabase project the app talks to.
 *
 * Only the project URL and the PUBLISHABLE key belong here. The publishable key is designed to ship in
 * apps; it only lets the app call the TRIMAX functions as a signed-in user, and the database checks every
 * call. Never put a secret (sb_secret_...) or service_role key, a database password or a connection
 * string in the app: anyone can extract them from the APK. SupabaseClient refuses secret keys.
 *
 * To test against a separate (staging) project, change both values to that project's URL and
 * publishable key (Supabase dashboard > Project Settings > API) and rebuild.
 */
public final class SupabaseConfig {

    public static final String PROJECT_URL = "https://dfxjblrletoeukoarxlz.supabase.co";
    public static final String PUBLISHABLE_KEY = "sb_publishable_FwMnGl1O1qmsIfoscL2BPw_JJkf31UQ";

    private SupabaseConfig() {
    }
}
