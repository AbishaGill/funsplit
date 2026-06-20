package com.finsplit.app.services;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.finsplit.app.R;
import com.finsplit.app.activities.MainActivity;
import com.finsplit.app.repositories.UserRepository;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

/**
 * FCM service: handles new device tokens and incoming push messages.
 *
 * Registered in AndroidManifest with MESSAGING_EVENT intent-filter.
 * Notifications tap into MainActivity with FLAG_ACTIVITY_CLEAR_TOP.
 */
public class FinSplitMessagingService extends FirebaseMessagingService {

    public static final String CHANNEL_ID = "finsplit_channel";
    private static final int NOTIFICATION_ID_BASE = 1000;

    // ── Token refresh ─────────────────────────────────────────────────────────

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        // Persist the fresh token; only possible when the user is signed in
        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (uid != null && !uid.isEmpty()) {
            UserRepository.getInstance().updateFcmToken(uid, token);
        }
    }

    // ── Message received ──────────────────────────────────────────────────────

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        String title = "FinSplit";
        String body  = "You have a new update.";

        // Prefer notification payload; fall back to data payload
        if (remoteMessage.getNotification() != null) {
            if (remoteMessage.getNotification().getTitle() != null) {
                title = remoteMessage.getNotification().getTitle();
            }
            if (remoteMessage.getNotification().getBody() != null) {
                body = remoteMessage.getNotification().getBody();
            }
        } else if (!remoteMessage.getData().isEmpty()) {
            if (remoteMessage.getData().containsKey("title")) {
                title = remoteMessage.getData().get("title");
            }
            if (remoteMessage.getData().containsKey("body")) {
                body = remoteMessage.getData().get("body");
            }
        }

        showNotification(title, body);
    }

    // ── Notification helpers ──────────────────────────────────────────────────

    private void showNotification(String title, String body) {
        ensureChannelExists();

        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        int flags = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                ? PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
                : PendingIntent.FLAG_UPDATE_CURRENT;

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, intent, flags);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_call_split)
                .setContentTitle(title)
                .setContentText(body)
                .setColor(Color.parseColor("#E07B39"))
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent);

        NotificationManager nm =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) {
            nm.notify(NOTIFICATION_ID_BASE + (int) (System.currentTimeMillis() % 1000), builder.build());
        }
    }

    /**
     * Creates the notification channel on API 26+. Safe to call repeatedly —
     * the system is idempotent for existing channels.
     */
    public static void ensureChannelExists(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "FinSplit Notifications",
                    NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("Expense updates and balance alerts");
            channel.enableLights(true);
            channel.setLightColor(Color.parseColor("#E07B39"));
            NotificationManager nm =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) nm.createNotificationChannel(channel);
        }
    }

    private void ensureChannelExists() {
        ensureChannelExists(this);
    }
}
