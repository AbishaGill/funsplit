package com.finsplit.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import com.finsplit.app.R;
import com.finsplit.app.utils.SeedManager;
import com.google.firebase.auth.FirebaseAuth;

public class SplashActivity extends BaseActivity {

    private static final long SPLASH_DELAY_MS = 1500L;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Wait for the splash duration, seed admin if needed, then let Firebase
        // authoritatively confirm the session state before routing.
        new Handler(Looper.getMainLooper()).postDelayed(() ->
                SeedManager.seedAdminIfNotExists(this, this::navigateNext),
                SPLASH_DELAY_MS);
    }

    private void navigateNext() {
        // Use a one-time AuthStateListener instead of a bare getCurrentUser() call.
        // getCurrentUser() can return a stale non-null value from a previous session
        // before Firebase has finished restoring (or discarding) the persisted token.
        // AuthStateListener fires only after Firebase has fully settled auth state,
        // so the routing decision is always accurate.
        FirebaseAuth.getInstance().addAuthStateListener(new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(FirebaseAuth auth) {
                FirebaseAuth.getInstance().removeAuthStateListener(this);

                if (isFinishing() || isDestroyed()) return;

                Intent intent = auth.getCurrentUser() != null
                        ? new Intent(SplashActivity.this, MainActivity.class)
                        : new Intent(SplashActivity.this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }
        });
    }

    @Override
    protected boolean requiresAuth() {
        return false;
    }
}
