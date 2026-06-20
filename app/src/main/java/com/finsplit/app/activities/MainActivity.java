package com.finsplit.app.activities;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.finsplit.app.R;
import com.finsplit.app.adapters.ExpenseAdapter;
import com.finsplit.app.fragments.AddExpenseBottomSheet;
import com.finsplit.app.models.Expense;
import com.finsplit.app.models.Group;
import com.finsplit.app.repositories.ExpenseRepository;
import com.finsplit.app.services.FinSplitMessagingService;
import com.finsplit.app.utils.ExpenseCallback;
import com.finsplit.app.utils.SessionManager;
import com.finsplit.app.workers.WeeklyReportWorker;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class MainActivity extends BaseActivity {

    private ExpenseAdapter expenseAdapter;
    private ExpenseRepository expenseRepository;
    private ListenerRegistration expenseListener;
    private SessionManager sessionManager;

    private TextView tvMyTotal, tvPartnerTotal, tvNetBalance;
    private ImageView ivTrend;
    private TextView tvEmpty;
    private RecyclerView rvExpenses;
    private TextView bannerOffline;
    private View stubShimmer;
    private boolean shimmerInflated = false;

    private String groupId;
    private List<String> groupMembers;
    private Group currentGroup;

    // Android 13+ POST_NOTIFICATIONS permission launcher
    private final ActivityResultLauncher<String> notifPermLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.RequestPermission(),
                    granted -> { /* user decided; no forced follow-up */ });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Ensure notification channel exists for all Android versions
        FinSplitMessagingService.ensureChannelExists(this);

        sessionManager = new SessionManager(this);
        expenseRepository = ExpenseRepository.getInstance();

        FirebaseUser fbUser = FirebaseAuth.getInstance().getCurrentUser();
        String myUid = fbUser != null ? fbUser.getUid() : "";

        groupId      = "group_" + myUid;
        groupMembers = Arrays.asList(myUid);

        bindViews(myUid);
        setupRecyclerView(myUid);
        setupBottomNav();
        setupFab();
        setAvatarInitials(fbUser);
        checkNotificationPermission();
        checkConnectivity();
        scheduleWeeklyReport();

        loadGroupThenExpenses(myUid);
    }

    // ── View binding ──────────────────────────────────────────────────────────

    private void bindViews(String myUid) {
        tvMyTotal      = findViewById(R.id.tv_my_total);
        tvPartnerTotal = findViewById(R.id.tv_partner_total);
        tvNetBalance   = findViewById(R.id.tv_net_balance);
        ivTrend        = findViewById(R.id.iv_trend);
        tvEmpty        = findViewById(R.id.tv_empty);
        rvExpenses     = findViewById(R.id.rv_expenses);
        bannerOffline  = findViewById(R.id.banner_offline);
        stubShimmer    = findViewById(R.id.stub_shimmer);

        // Settle Up button
        findViewById(R.id.btn_settle_up).setOnClickListener(
                v -> startActivity(new Intent(this, SettleUpActivity.class)));

        // Avatar tap → ProfileActivity
        findViewById(R.id.tv_avatar).setOnClickListener(
                v -> startActivity(new Intent(this, ProfileActivity.class)));

        // "See All" → ExpenseHistoryActivity
        findViewById(R.id.tv_see_all).setOnClickListener(
                v -> startActivity(new Intent(this, ExpenseHistoryActivity.class)));
    }

    private void setupRecyclerView(String myUid) {
        expenseAdapter = new ExpenseAdapter(myUid);
        rvExpenses.setLayoutManager(new LinearLayoutManager(this));
        rvExpenses.setAdapter(expenseAdapter);
        rvExpenses.setNestedScrollingEnabled(false);
    }

    private void setupBottomNav() {
        BottomNavigationView nav = findViewById(R.id.bottom_nav);
        nav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_settings) {
                startActivity(new Intent(this, ProfileActivity.class));
                return true;
            }
            return true;
        });
    }

    private void setupFab() {
        FloatingActionButton fab = findViewById(R.id.fab_add);
        fab.setOnClickListener(v -> openAddExpenseSheet());
    }

    private void setAvatarInitials(FirebaseUser user) {
        TextView tvAvatar = findViewById(R.id.tv_avatar);
        if (user == null) return;
        String name = user.getDisplayName();
        if (name != null && !name.isEmpty()) {
            String[] parts = name.trim().split("\\s+");
            String initials = parts.length >= 2
                    ? String.valueOf(parts[0].charAt(0)) + parts[1].charAt(0)
                    : String.valueOf(parts[0].charAt(0));
            tvAvatar.setText(initials.toUpperCase());
        } else {
            tvAvatar.setText("?");
        }
    }

    // ── Android 13 notification permission ───────────────────────────────────

    private void checkNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return;
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED) return;

        if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle(getString(R.string.notif_rationale_title))
                    .setMessage(getString(R.string.notif_rationale_body))
                    .setPositiveButton(getString(R.string.confirm), (d, w) ->
                            notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS))
                    .setNegativeButton(getString(R.string.cancel), null)
                    .show();
        } else {
            notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
        }
    }

    // ── Connectivity ──────────────────────────────────────────────────────────

    private void checkConnectivity() {
        ConnectivityManager cm =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        boolean online = false;
        if (cm != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                android.net.Network net = cm.getActiveNetwork();
                if (net != null) {
                    NetworkCapabilities caps = cm.getNetworkCapabilities(net);
                    online = caps != null
                            && (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                            || caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
                            || caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET));
                }
            } else {
                android.net.NetworkInfo info = cm.getActiveNetworkInfo();
                online = info != null && info.isConnected();
            }
        }
        bannerOffline.setVisibility(online ? View.GONE : View.VISIBLE);
    }

    // ── WorkManager — weekly report ───────────────────────────────────────────

    private void scheduleWeeklyReport() {
        PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(
                WeeklyReportWorker.class, 7, TimeUnit.DAYS)
                .build();
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "weekly_report",
                ExistingPeriodicWorkPolicy.KEEP,
                request);
    }

    // ── Data loading ──────────────────────────────────────────────────────────

    private void loadGroupThenExpenses(String myUid) {
        expenseRepository.getGroup(groupId, new ExpenseRepository.OnGroupLoadedCallback() {
            @Override
            public void onLoaded(Group group) {
                currentGroup = group;
                if (group.getMembers() != null && !group.getMembers().isEmpty()) {
                    groupMembers = group.getMembers();
                }
                updateBalanceCard(group, myUid);
                startExpenseListener();
            }

            @Override
            public void onError(String error) {
                Group newGroup = new Group("My Group", groupMembers);
                expenseRepository.createGroup(newGroup, new ExpenseRepository.OnGroupCreatedCallback() {
                    @Override
                    public void onCreated(String id) {
                        groupId = id;
                        startExpenseListener();
                    }
                    @Override
                    public void onError(String err) {
                        startExpenseListener();
                    }
                });
            }
        });
    }

    private void startExpenseListener() {
        expenseListener = expenseRepository.listenToExpenses(groupId, new ExpenseCallback() {
            @Override
            public void onExpensesLoaded(List<Expense> expenses) {
                hideShimmer();
                expenseAdapter.submitList(expenses);
                tvEmpty.setVisibility(expenses.isEmpty() ? View.VISIBLE : View.GONE);
                rvExpenses.setVisibility(expenses.isEmpty() ? View.GONE : View.VISIBLE);
                if (currentGroup != null) {
                    updateBalanceCard(currentGroup,
                            FirebaseAuth.getInstance().getCurrentUser() != null
                                    ? FirebaseAuth.getInstance().getCurrentUser().getUid() : "");
                }
            }

            @Override
            public void onExpenseAdded(Expense expense) {}

            @Override
            public void onError(String error) {
                hideShimmer();
                View root = findViewById(android.R.id.content);
                Snackbar.make(root, getString(R.string.error_generic), Snackbar.LENGTH_LONG).show();
            }
        });
    }

    private void hideShimmer() {
        if (stubShimmer != null) {
            stubShimmer.setVisibility(View.GONE);
        }
    }

    private void updateBalanceCard(Group group, String myUid) {
        Map<String, Double> balances = group.getBalances();
        double myBalance = 0, partnerBalance = 0;
        if (balances != null) {
            for (Map.Entry<String, Double> entry : balances.entrySet()) {
                if (entry.getKey().equals(myUid)) {
                    myBalance = entry.getValue() != null ? entry.getValue() : 0;
                } else {
                    partnerBalance += entry.getValue() != null ? entry.getValue() : 0;
                }
            }
        }
        tvMyTotal.setText(String.format(Locale.getDefault(), "Your Total ₨%,.0f", Math.abs(myBalance)));
        tvPartnerTotal.setText(String.format(Locale.getDefault(), "Partner's Total ₨%,.0f", Math.abs(partnerBalance)));

        double net = myBalance;
        if (net >= 0) {
            ivTrend.setImageResource(R.drawable.ic_trending_up);
            tvNetBalance.setText(String.format(Locale.getDefault(), "You're owed ₨%,.0f", net));
            tvNetBalance.setTextColor(getResources().getColor(R.color.color_tertiary, getTheme()));
        } else {
            ivTrend.setImageResource(R.drawable.ic_trending_down);
            tvNetBalance.setText(String.format(Locale.getDefault(), "You owe ₨%,.0f", Math.abs(net)));
            tvNetBalance.setTextColor(getResources().getColor(R.color.color_error, getTheme()));
        }
    }

    // ── Bottom sheet ──────────────────────────────────────────────────────────

    private void openAddExpenseSheet() {
        AddExpenseBottomSheet sheet = AddExpenseBottomSheet.newInstance(groupId, groupMembers);
        sheet.setOnExpenseAddedListener(() -> {});
        sheet.show(getSupportFragmentManager(), AddExpenseBottomSheet.TAG);
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (expenseListener != null) expenseListener.remove();
    }
}
