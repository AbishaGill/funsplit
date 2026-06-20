package com.finsplit.app.workers;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.finsplit.app.R;
import com.finsplit.app.activities.MainActivity;
import com.finsplit.app.models.Expense;
import com.finsplit.app.models.WeeklyReport;
import com.finsplit.app.services.FinSplitMessagingService;
import com.finsplit.app.utils.AppConstants;
import com.finsplit.app.utils.WeeklyReportGenerator;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * WorkManager Worker that runs once a week to generate a WeeklyReport,
 * persist it to Firestore, and post a local notification.
 *
 * Scheduled via PeriodicWorkRequest with a 7-day interval.
 * Worker is registered implicitly via WorkManager — no Manifest entry needed.
 */
public class WeeklyReportWorker extends Worker {

    private static final int WEEKLY_NOTIF_ID = 2001;

    public WeeklyReportWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return Result.failure();

        String uid     = user.getUid();
        String groupId = "group_" + uid;
        List<String> members = Arrays.asList(uid);

        // Synchronous Firestore fetch using Tasks.await() — Worker runs on a background thread
        try {
            List<Expense> expenses = fetchExpensesSync(groupId);
            WeeklyReportGenerator generator = new WeeklyReportGenerator();
            WeeklyReport report = generator.generate(expenses, members, uid);

            persistReport(uid, report);
            showWeeklyNotification(report);
            return Result.success();
        } catch (Exception e) {
            return Result.retry();
        }
    }

    private List<Expense> fetchExpensesSync(String groupId) throws Exception {
        com.google.android.gms.tasks.Task<com.google.firebase.firestore.QuerySnapshot> task =
                FirebaseFirestore.getInstance()
                        .collection(AppConstants.COLLECTION_GROUPS)
                        .document(groupId)
                        .collection(AppConstants.COLLECTION_EXPENSES)
                        .get();

        com.google.firebase.firestore.QuerySnapshot snapshot =
                com.google.android.gms.tasks.Tasks.await(task);

        List<Expense> list = new ArrayList<>();
        for (QueryDocumentSnapshot doc : snapshot) {
            Expense e = doc.toObject(Expense.class);
            if (e != null) {
                e.setExpenseId(doc.getId());
                list.add(e);
            }
        }
        return list;
    }

    private void persistReport(String uid, WeeklyReport report) throws Exception {
        com.google.android.gms.tasks.Tasks.await(
                FirebaseFirestore.getInstance()
                        .collection(AppConstants.COLLECTION_USERS)
                        .document(uid)
                        .collection("weeklyReports")
                        .add(report));
    }

    private void showWeeklyNotification(WeeklyReport report) {
        Context ctx = getApplicationContext();
        FinSplitMessagingService.ensureChannelExists(ctx);

        String body = String.format(java.util.Locale.getDefault(),
                "Week %s: ₨%,.0f spent", report.getWeekRange(), report.getTotalSpentPKR());

        Intent intent = new Intent(ctx, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        int flags = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                ? PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
                : PendingIntent.FLAG_UPDATE_CURRENT;
        PendingIntent pi = PendingIntent.getActivity(ctx, 0, intent, flags);

        NotificationCompat.Builder nb = new NotificationCompat.Builder(
                ctx, FinSplitMessagingService.CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_call_split)
                .setContentTitle("Your Weekly FinSplit Report")
                .setContentText(body)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pi);

        NotificationManagerCompat.from(ctx).notify(WEEKLY_NOTIF_ID, nb.build());
    }
}
