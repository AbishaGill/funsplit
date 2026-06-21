package com.finsplit.app.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.finsplit.app.R;
import com.finsplit.app.models.User;
import com.finsplit.app.repositories.ExpenseRepository;
import com.finsplit.app.repositories.UserRepository;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;

public class InviteSetupActivity extends BaseActivity {

    private TextInputLayout tilInvite;
    private TextInputEditText etInvite;
    private View btnSendInvite, btnShareLink;
    private ProgressBar progressBar;

    private UserRepository userRepository;
    private ExpenseRepository expenseRepository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_invite_setup);

        tilInvite     = findViewById(R.id.til_invite);
        etInvite      = findViewById(R.id.et_invite);
        btnSendInvite = findViewById(R.id.btn_send_invite);
        btnShareLink  = findViewById(R.id.btn_share_link);
        progressBar   = findViewById(R.id.progress_bar);

        userRepository   = UserRepository.getInstance();
        expenseRepository = ExpenseRepository.getInstance();

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        btnSendInvite.setOnClickListener(v -> attemptSendInvite());
        btnShareLink.setOnClickListener(v -> shareInviteLink());
    }

    private void attemptSendInvite() {
        tilInvite.setError(null);
        String email = getText(etInvite);

        if (TextUtils.isEmpty(email)) {
            tilInvite.setError(getString(R.string.error_invite_empty));
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilInvite.setError(getString(R.string.error_email_invalid));
            return;
        }

        String myEmail = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getEmail() : "";
        if (email.equalsIgnoreCase(myEmail)) {
            tilInvite.setError("That's your own email address.");
            return;
        }

        setLoading(true);
        userRepository.findUserByEmail(email, new UserRepository.OnUserFoundCallback() {
            @Override
            public void onFound(User user) {
                // Partner already has an account — add them to the group
                String myUid = FirebaseAuth.getInstance().getCurrentUser() != null
                        ? FirebaseAuth.getInstance().getCurrentUser().getUid() : "";
                String groupId = "group_" + myUid;

                expenseRepository.addMemberToGroup(groupId, user.getUserId(),
                        new ExpenseRepository.OnGroupUpdatedCallback() {
                            @Override
                            public void onUpdated() {
                                runOnUiThread(() -> {
                                    setLoading(false);
                                    Toast.makeText(InviteSetupActivity.this,
                                            user.getDisplayName() + " added to your group.",
                                            Toast.LENGTH_SHORT).show();
                                    finish();
                                });
                            }

                            @Override
                            public void onError(String error) {
                                runOnUiThread(() -> {
                                    setLoading(false);
                                    Toast.makeText(InviteSetupActivity.this,
                                            getString(R.string.error_generic), Toast.LENGTH_SHORT).show();
                                });
                            }
                        });
            }

            @Override
            public void onNotFound() {
                // No account yet — open email client so user can send an invite manually
                runOnUiThread(() -> {
                    setLoading(false);
                    openEmailClient(email);
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    setLoading(false);
                    Toast.makeText(InviteSetupActivity.this,
                            getString(R.string.error_generic), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void openEmailClient(String toEmail) {
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("mailto:"));
        intent.putExtra(Intent.EXTRA_EMAIL, new String[]{toEmail});
        intent.putExtra(Intent.EXTRA_SUBJECT, "Join me on FinSplit");
        intent.putExtra(Intent.EXTRA_TEXT,
                "Hey, I'm using FinSplit to track shared expenses. " +
                "Download the app and sign up with this email address so I can add you to my group.");
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        } else {
            Toast.makeText(this, getString(R.string.invite_sent, toEmail), Toast.LENGTH_LONG).show();
        }
        finish();
    }

    private void shareInviteLink() {
        String link = "https://finsplit.app/invite";
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, getString(R.string.share_link_text, link));
        startActivity(Intent.createChooser(shareIntent, getString(R.string.share_link)));
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnSendInvite.setEnabled(!loading);
        btnShareLink.setEnabled(!loading);
    }

    private String getText(TextInputEditText et) {
        return et.getText() != null ? et.getText().toString().trim() : "";
    }
}
