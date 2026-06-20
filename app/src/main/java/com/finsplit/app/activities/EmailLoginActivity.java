package com.finsplit.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.finsplit.app.R;
import com.finsplit.app.models.User;
import com.finsplit.app.repositories.UserRepository;
import com.finsplit.app.utils.AuthCallback;
import com.finsplit.app.utils.SessionManager;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;

public class EmailLoginActivity extends BaseActivity implements AuthCallback {

    private FirebaseAuth firebaseAuth;
    private UserRepository userRepository;
    private SessionManager sessionManager;

    private TextInputLayout tilEmail, tilPassword;
    private TextInputEditText etEmail, etPassword;
    private View btnSignIn;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_email_login);

        firebaseAuth = FirebaseAuth.getInstance();
        userRepository = UserRepository.getInstance();
        sessionManager = new SessionManager(this);

        tilEmail = findViewById(R.id.til_email);
        tilPassword = findViewById(R.id.til_password);
        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        btnSignIn = findViewById(R.id.btn_sign_in);
        progressBar = findViewById(R.id.progress_bar);

        btnSignIn.setOnClickListener(v -> attemptSignIn());
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        findViewById(R.id.btn_create_account).setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class)));
    }

    private void attemptSignIn() {
        tilEmail.setError(null);
        tilPassword.setError(null);

        String email = getText(etEmail);
        String password = getText(etPassword);

        if (TextUtils.isEmpty(email)) {
            tilEmail.setError(getString(R.string.error_email_required));
            return;
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError(getString(R.string.error_email_invalid));
            return;
        }
        if (TextUtils.isEmpty(password)) {
            tilPassword.setError(getString(R.string.error_password_required));
            return;
        }
        if (password.length() < 6) {
            tilPassword.setError(getString(R.string.error_password_short));
            return;
        }

        setLoading(true);
        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful() && firebaseAuth.getCurrentUser() != null) {
                        userRepository.createOrUpdateUser(firebaseAuth.getCurrentUser(), this);
                    } else {
                        setLoading(false);
                        String msg = task.getException() != null
                                ? task.getException().getMessage()
                                : getString(R.string.sign_in_failed);
                        onFailure(msg);
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
        Toast.makeText(this, error, Toast.LENGTH_LONG).show();
    }

    // ─────────────────────────────────────────────────────────────────────────

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnSignIn.setEnabled(!loading);
    }

    private String getText(TextInputEditText editText) {
        return editText.getText() != null ? editText.getText().toString().trim() : "";
    }

    @Override
    protected boolean requiresAuth() {
        return false;
    }
}
