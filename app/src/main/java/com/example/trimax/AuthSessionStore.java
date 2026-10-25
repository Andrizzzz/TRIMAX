package com.example.trimax;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.security.KeyStore;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/**
 * Keeps the Supabase session between app starts, encrypted with an AES key that lives in the Android
 * Keystore (it cannot be read out of the phone or restored onto another one). If the stored data cannot
 * be decrypted (e.g. restored from a backup onto a new phone), it is discarded and the user signs in again.
 * The preferences file is also excluded from backups (res/xml/backup_rules.xml, data_extraction_rules.xml).
 */
public final class AuthSessionStore implements AuthSession.Store {

    static final String PREFS = "trimax_auth";
    private static final String KEY_ALIAS = "trimax_auth_session_key";
    private static final String KEY_DATA = "session";
    private static final String KEY_IV = "iv";
    private static final String ANDROID_KEYSTORE = "AndroidKeyStore";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";

    private final SharedPreferences prefs;
    private volatile AuthSession cached;
    private volatile boolean loaded;

    public AuthSessionStore(Context context) {
        this.prefs = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    @Override
    public synchronized AuthSession load() {
        if (loaded) return cached;
        loaded = true;
        String data = prefs.getString(KEY_DATA, null);
        String iv = prefs.getString(KEY_IV, null);
        if (data == null || iv == null) return cached = null;
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(128, Base64.decode(iv, Base64.NO_WRAP)));
            String json = new String(cipher.doFinal(Base64.decode(data, Base64.NO_WRAP)), StandardCharsets.UTF_8);
            JSONObject o = new JSONObject(json);
            cached = new AuthSession(o.getString("a"), o.getString("r"), o.getLong("e"), o.getString("u"), Json.text(o, "m"));
        } catch (Exception unreadable) {
            prefs.edit().clear().apply();
            cached = null;
        }
        return cached;
    }

    @Override
    public synchronized void save(AuthSession s) {
        try {
            JSONObject o = new JSONObject();
            o.put("a", s.accessToken);
            o.put("r", s.refreshToken);
            o.put("e", s.expiresAt);
            o.put("u", s.userId);
            o.put("m", s.email != null ? s.email : JSONObject.NULL);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, key());
            byte[] encrypted = cipher.doFinal(o.toString().getBytes(StandardCharsets.UTF_8));
            prefs.edit()
                    .putString(KEY_DATA, Base64.encodeToString(encrypted, Base64.NO_WRAP))
                    .putString(KEY_IV, Base64.encodeToString(cipher.getIV(), Base64.NO_WRAP))
                    .commit();
        } catch (Exception e) {
            // Could not persist: keep the session for this run only; the user signs in again next time.
            prefs.edit().clear().apply();
        }
        cached = s;
        loaded = true;
    }

    @Override
    public synchronized void clear() {
        prefs.edit().clear().commit();
        cached = null;
        loaded = true;
    }

    private static SecretKey key() throws Exception {
        KeyStore ks = KeyStore.getInstance(ANDROID_KEYSTORE);
        ks.load(null);
        if (ks.containsAlias(KEY_ALIAS)) {
            return ((KeyStore.SecretKeyEntry) ks.getEntry(KEY_ALIAS, null)).getSecretKey();
        }
        KeyGenerator generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE);
        generator.init(new KeyGenParameterSpec.Builder(KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build());
        return generator.generateKey();
    }
}
