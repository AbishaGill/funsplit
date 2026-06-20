package com.finsplit.app.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.finsplit.app.R;
import com.finsplit.app.models.User;
import com.finsplit.app.repositories.UserRepository;
import com.finsplit.app.utils.AuthCallback;
import com.finsplit.app.utils.SessionManager;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * Profile screen: shows user info, currency selector, sign-out, and invite partner.
 *
 * OOP — Inheritance: extends BaseActivity (auth guard).
 * OOP — Interface: implements AuthCallback for currency update callbacks.
 * OOP — Encapsulation: all view refs and state are private fields.
 */
public class ProfileActivity extends BaseActivity implements AuthCallback {

    private static final String[] CURRENCIES = {"PKR", "USD", "AED", "GBP"};

    private ImageView ivProfilePic;
    private TextView tvName, tvEmail;
    private Spinner spinnerCurrency;
    private MaterialButton btnSignOut, btnInvitePartner;

    private UserRepository userRepository;
    private SessionManager sessionManager;
    private String currentCurrency = "PKR";
    private boolean spinnerInitialized = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        userRepository = UserRepository.getInstance();
        sessionManager = new SessionManager(this);

        ivProfilePic   = findViewById(R.id.iv_profile_pic);
        tvName         = findViewById(R.id.tv_profile_name);
        tvEmail        = findViewById(R.id.tv_profile_email);
        spinnerCurrency= findViewById(R.id.spinner_currency);
        btnSignOut     = findViewById(R.id.btn_sign_out);
        btnInvitePartner = findViewById(R.id.btn_invite_partner_profile);

        findViewById(R.id.btn_back_profile).setOnClickListener(v -> finish());

        setupCurrencySpinner();
        loadUserData();

        btnSignOut.setOnClickListener(v -> signOut());
        btnInvitePartner.setOnClickListener(v ->
                startActivity(new Intent(this, InviteSetupActivity.class)));
        findViewById(R.id.btn_weekly_report).setOnClickListener(v ->
                startActivity(new Intent(this, WeeklyReportActivity.class)));
    }

    private void loadUserData() {
        FirebaseUser fbUser = FirebaseAuth.getInstance().getCurrentUser();
        if (fbUser == null) return;

        // Show basic info from FirebaseUser immediately for snappy UI
        tvName.setText(fbUser.getDisplayName() != null ? fbUser.getDisplayName() : "");
        tvEmail.setText(fbUser.getEmail() != null ? fbUser.getEmail() : "");

        Uri photoUri = fbUser.getPhotoUrl();
        if (photoUri != null) {
            Glide.with(this)
                    .load(photoUri)
                    .apply(RequestOptions.circleCropTransform())
                    .placeholder(R.drawable.bg_avatar_orange)
                    .into(ivProfilePic);
        } else {
            ivProfilePic.setImageResource(R.drawable.bg_avatar_orange);
        }

        // Fetch full user doc for currency preference
        String userId = fbUser.getUid();
        userRepository.getCurrentUser(userId, new AuthCallback() {
            @Override
            public void onSuccess(User user) {
                currentCurrency = user.getDefaultCurrency() != null
                        ? user.getDefaultCurrency() : "PKR";
                selectCurrencyInSpinner(currentCurrency);
                spinnerInitialized = true;
            }
            @Override
            public void onFailure(String error) {
                spinnerInitialized = true;
            }
        });
    }

    private void setupCurrencySpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, CURRENCIES);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCurrency.setAdapter(adapter);

        spinnerCurrency.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (!spinnerInitialized) return;
                String selected = CURRENCIES[position];
                if (selected.equals(currentCurrency)) return;
                currentCurrency = selected;
                saveCurrency(selected);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void selectCurrencyInSpinner(String currency) {
        for (int i = 0; i < CURRENCIES.length; i++) {
            if (CURRENCIES[i].equals(currency)) {
                spinnerCurrency.setSelection(i, false);
                return;
            }
        }
    }

    private void saveCurrency(String currency) {
        FirebaseUser fbUser = FirebaseAuth.getInstance().getCurrentUser();
        if (fbUser == null) return;
        // OOP — Interface: ProfileActivity implements AuthCallback; passes `this` as callback
        userRepository.updateDefaultCurrency(fbUser.getUid(), currency, this);
    }

    private void signOut() {
        FirebaseAuth.getInstance().signOut();
        sessionManager.clearSession();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    // ── AuthCallback (OOP — Interface implementation) ─────────────────────────

    @Override
    public void onSuccess(User user) {
        Toast.makeText(this,
                getString(R.string.currency_saved, user.getDefaultCurrency()),
                Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onFailure(String error) {
        Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
    }
}
