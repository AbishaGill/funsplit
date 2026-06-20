package com.finsplit.app.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.finsplit.app.R;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class InviteSetupActivity extends BaseActivity {

    private TextInputLayout tilInvite;
    private TextInputEditText etInvite;
    private View btnSendInvite, btnShareLink;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_invite_setup);

        tilInvite    = findViewById(R.id.til_invite);
        etInvite     = findViewById(R.id.et_invite);
        btnSendInvite = findViewById(R.id.btn_send_invite);
        btnShareLink  = findViewById(R.id.btn_share_link);
        progressBar  = findViewById(R.id.progress_bar);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        btnSendInvite.setOnClickListener(v -> attemptSendInvite());
        btnShareLink.setOnClickListener(v -> shareInviteLink());
    }

    private void attemptSendInvite() {
        tilInvite.setError(null);
        String input = getText(etInvite);

        if (TextUtils.isEmpty(input)) {
            tilInvite.setError(getString(R.string.error_invite_empty));
            return;
        }

        setLoading(true);
        // TODO: look up user by email in Firestore, then create/update group with members.
        // For now, show a placeholder success toast.
        Toast.makeText(this, getString(R.string.invite_sent, input), Toast.LENGTH_SHORT).show();
        setLoading(false);
        finish();
    }

    private void shareInviteLink() {
        // Deep-link or dynamic link — placeholder share sheet
        String link = "https://finsplit.app/invite?groupId=demo";
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
