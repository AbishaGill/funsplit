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
import com.google.firebase.auth.UserProfileChangeRequest;

public class RegisterActivity extends BaseActivity implements AuthCallback {

    private FirebaseAuth firebaseAuth;
    private UserRepository userRepository;
    private SessionManager sessionManager;

    private TextInputLayout tilName, tilEmail, tilPassword;
    private TextInputEditText etName, etEmail, etPassword;
    private View btnRegister;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        firebaseAuth = FirebaseAuth.getInstance();
        userRepository = UserRepository.getInstance();
        sessionManager = new SessionManager(this);

        tilName = findViewById(R.id.til_name);
        tilEmail = findViewById(R.id.til_email);
        tilPassword = findViewById(R.id.til_password);
        etName = findViewById(R.id.et_name);
        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        btnRegister = findViewById(R.id.btn_register);
        progressBar = findViewById(R.id.progress_bar);

        btnRegister.setOnClickListener(v -> attemptRegister());
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
    }

    private void attemptRegister() {
        tilName.setError(null);
        tilEmail.setError(null);
        tilPassword.setError(null);

        String name = getText(etName);
        String email = getText(etEmail);
        String password = getText(etPassword);

        if (TextUtils.isEmpty(name)) {
            tilName.setError(getString(R.string.error_name_required));
            return;
        }
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
        final String finalName = name;
        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful() && firebaseAuth.getCurrentUser() != null) {
                        // Write display name back to FirebaseAuth profile
                        UserProfileChangeRequest profileUpdate = new UserProfileChangeRequest.Builder()
                                .setDisplayName(finalName)
                                .build();
                        firebaseAuth.getCurrentUser().updateProfile(profileUpdate)
                                .addOnCompleteListener(profileTask ->
                                        userRepository.createOrUpdateUser(
                                                firebaseAuth.getCurrentUser(), this));
                    } else {
                        setLoading(false);
                        String msg = task.getException() != null
                                ? task.getException().getMessage()
                                : getString(R.string.error_generic);
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
        btnRegister.setEnabled(!loading);
    }

    private String getText(TextInputEditText editText) {
        return editText.getText() != null ? editText.getText().toString().trim() : "";
    }

    @Override
    protected boolean requiresAuth() {
        return false;
    }
}
