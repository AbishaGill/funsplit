package com.finsplit.app.activities;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public abstract class BaseActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onStart() {
        super.onStart();
        // isFinishing() guards against the notification-permission dialog
        // onStop→onStart cycle that can briefly return null from getCurrentUser()
        // during any in-progress Firebase session transition.
        if (requiresAuth() && !isFinishing() && !isAuthenticated()) {
            redirectToLogin();
        }
    }

    protected boolean isAuthenticated() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        return user != null;
    }

    /** Override to false in LoginActivity / SplashActivity so they don't self-redirect. */
    protected boolean requiresAuth() {
        return true;
    }

    protected void redirectToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
