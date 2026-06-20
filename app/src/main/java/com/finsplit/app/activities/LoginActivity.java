package com.finsplit.app.activities;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.finsplit.app.R;
import com.finsplit.app.models.User;
import com.finsplit.app.repositories.UserRepository;
import com.finsplit.app.utils.AuthCallback;
import com.finsplit.app.utils.SessionManager;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;

public class LoginActivity extends BaseActivity implements AuthCallback {

    private static final int RC_SIGN_IN = 9001;

    private FirebaseAuth firebaseAuth;
    private GoogleSignInClient googleSignInClient;
    private UserRepository userRepository;
    private SessionManager sessionManager;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        firebaseAuth = FirebaseAuth.getInstance();
        userRepository = UserRepository.getInstance();
        sessionManager = new SessionManager(this);

        progressBar = findViewById(R.id.progress_bar);
        animateProgressBar();

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(this, gso);

        findViewById(R.id.btn_google_sign_in).setOnClickListener(v -> startGoogleSignIn());
        findViewById(R.id.btn_email_login).setOnClickListener(v ->
                startActivity(new Intent(this, EmailLoginActivity.class)));
    }

    private void animateProgressBar() {
        ObjectAnimator animator = ObjectAnimator.ofInt(progressBar, "progress", 0, 100);
        animator.setDuration(1400);
        animator.start();
    }

    private void startGoogleSignIn() {
        setLoading(true);
        Intent signInIntent = googleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account.getIdToken());
            } catch (ApiException e) {
                setLoading(false);
                onFailure(getString(R.string.sign_in_failed));
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        firebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful() && firebaseAuth.getCurrentUser() != null) {
                        userRepository.createOrUpdateUser(firebaseAuth.getCurrentUser(), this);
                    } else {
                        setLoading(false);
                        onFailure(getString(R.string.sign_in_failed));
                    }
                });
    }

    // ── AuthCallback ──────────────────────────────────────────────────────────

    @Override
    public void onSuccess(User user) {
        sessionManager.saveUserId(user.getUserId());
        setLoading(false);
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public void onFailure(String error) {
        setLoading(false);
        Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
    }

    // ─────────────────────────────────────────────────────────────────────────

    private void setLoading(boolean loading) {
        // Progress bar is the animated decoration; disable button to prevent double-tap
        findViewById(R.id.btn_google_sign_in).setEnabled(!loading);
        findViewById(R.id.btn_email_login).setEnabled(!loading);
    }

    @Override
    protected boolean requiresAuth() {
        return false;
    }
}
