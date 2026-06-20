package com.finsplit.app.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

/**
 * Seeds a default admin account on the very first app launch.
 * Subsequent launches skip the seed because the SharedPreferences flag is set.
 * All Firestore/Auth work is async; onComplete is invoked on both success and
 * any failure so the splash routing is never blocked.
 */
public final class SeedManager {

    private static final String PREFS_NAME  = AppConstants.PREFS_NAME;
    private static final String KEY_SEEDED  = "admin_seeded";

    private static final String ADMIN_EMAIL    = "admin.funsplit@gmail.com";
    private static final String ADMIN_PASSWORD = "@123Admin";

    private SeedManager() {}

    /**
     * Creates the admin account once per device installation.
     * Safe to call every launch — the SharedPreferences flag ensures the
     * Firebase call is never repeated after the first successful seed.
     *
     * @param context    any Context (Application or Activity)
     * @param onComplete called when seeding is finished (or skipped); always fires
     */
    public static void seedAdminIfNotExists(Context context, Runnable onComplete) {
        SharedPreferences prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        if (prefs.getBoolean(KEY_SEEDED, false)) {
            onComplete.run();
            return;
        }

        // If a user is already signed in, don't call createUserWithEmailAndPassword.
        // That call would sign out the current user and sign in as the new admin
        // account, which is what causes the dashboard to auto-close seconds after
        // login (BaseActivity.onStart sees getCurrentUser() == null during the
        // Firebase session transition and fires redirectToLogin).
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            markSeeded(prefs);
            onComplete.run();
            return;
        }

        FirebaseAuth.getInstance()
                .createUserWithEmailAndPassword(ADMIN_EMAIL, ADMIN_PASSWORD)
                .addOnSuccessListener(result -> {
                    String uid = result.getUser().getUid();
                    writeAdminDoc(uid, prefs, onComplete);
                })
                .addOnFailureListener(e -> {
                    if (e instanceof FirebaseAuthUserCollisionException) {
                        // Account already exists from a previous install or seed run
                        markSeeded(prefs);
                    }
                    // Any other error: skip silently — seed will retry on next launch
                    onComplete.run();
                });
    }

    private static void writeAdminDoc(String uid, SharedPreferences prefs, Runnable onComplete) {
        Map<String, Object> data = new HashMap<>();
        data.put("userId",          uid);
        data.put("displayName",     "Admin");
        data.put("email",           ADMIN_EMAIL);
        data.put("role",            "admin");
        data.put("defaultCurrency", AppConstants.CURRENCY_DEFAULT);
        data.put("fcmToken",        null);
        data.put("createdAt",       FieldValue.serverTimestamp());

        FirebaseFirestore.getInstance()
                .collection(AppConstants.COLLECTION_USERS)
                .document(uid)
                .set(data)
                .addOnCompleteListener(task -> {
                    // Mark seeded regardless of Firestore write success —
                    // the Auth account exists and that's the gating condition.
                    markSeeded(prefs);
                    // signOut() is synchronous — it clears the local session immediately.
                    // Must happen before onComplete so SplashActivity's auth check
                    // does not see the admin account and route to dashboard.
                    FirebaseAuth.getInstance().signOut();
                    onComplete.run();
                });
    }

    private static void markSeeded(SharedPreferences prefs) {
        prefs.edit().putBoolean(KEY_SEEDED, true).apply();
    }
}
