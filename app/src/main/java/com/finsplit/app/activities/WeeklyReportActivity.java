package com.finsplit.app.activities;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.finsplit.app.R;
import com.finsplit.app.models.Category;
import com.finsplit.app.models.WeeklyReport;
import com.finsplit.app.utils.AppConstants;
import com.google.android.material.chip.Chip;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Shows the latest weekly report summary and a scrollable list of past reports.
 *
 * OOP — Inheritance: extends BaseActivity (auth guard).
 * OOP — Encapsulation: adapter state and report list are private.
 */
public class WeeklyReportActivity extends BaseActivity {

    private TextView tvWeekRange, tvTotalSpent, tvOwedLabel;
    private Chip    chipTopCategory;
    private RecyclerView rvPastReports;
    private TextView tvNoReports;

    private final List<WeeklyReport> reports = new ArrayList<>();
    private PastReportsAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weekly_report);

        tvWeekRange     = findViewById(R.id.tv_week_range);
        tvTotalSpent    = findViewById(R.id.tv_total_spent);
        tvOwedLabel     = findViewById(R.id.tv_owed_label);
        chipTopCategory = findViewById(R.id.chip_top_category);
        rvPastReports   = findViewById(R.id.rv_past_reports);
        tvNoReports     = findViewById(R.id.tv_no_reports);

        findViewById(R.id.btn_back_report).setOnClickListener(v -> finish());

        adapter = new PastReportsAdapter();
        rvPastReports.setLayoutManager(new LinearLayoutManager(this));
        rvPastReports.setAdapter(adapter);

        loadReports();
    }

    private void loadReports() {
        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : "";
        if (uid.isEmpty()) return;

        FirebaseFirestore.getInstance()
                .collection(AppConstants.COLLECTION_USERS)
                .document(uid)
                .collection("weeklyReports")
                .orderBy("generatedAt", Query.Direction.DESCENDING)
                .limit(20)
                .get()
                .addOnSuccessListener(snapshots -> {
                    reports.clear();
                    for (QueryDocumentSnapshot doc : snapshots) {
                        WeeklyReport r = doc.toObject(WeeklyReport.class);
                        r.setReportId(doc.getId());
                        reports.add(r);
                    }
                    if (reports.isEmpty()) {
                        tvNoReports.setVisibility(View.VISIBLE);
                        rvPastReports.setVisibility(View.GONE);
                        showPlaceholderCard();
                    } else {
                        tvNoReports.setVisibility(View.GONE);
                        rvPastReports.setVisibility(View.VISIBLE);
                        bindCurrentWeek(reports.get(0));
                        adapter.setData(reports.subList(1, reports.size()));
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, getString(R.string.error_generic),
                                Toast.LENGTH_SHORT).show());
    }

    private void bindCurrentWeek(WeeklyReport r) {
        tvWeekRange.setText(r.getWeekRange());
        tvTotalSpent.setText(String.format(Locale.getDefault(), "₨%,.0f", r.getTotalSpentPKR()));

        Category cat = Category.fromString(r.getTopCategory());
        chipTopCategory.setText(cat.label());
        chipTopCategory.setChipBackgroundColorResource(
                cat == Category.FOOD       ? R.color.color_primary_container :
                cat == Category.TRANSPORT  ? R.color.color_secondary :
                cat == Category.RENT       ? R.color.color_tertiary :
                cat == Category.UTILITIES  ? R.color.color_secondary_container :
                                             R.color.color_outline);

        if (r.getTotalOwed() > 0) {
            tvOwedLabel.setText(String.format(Locale.getDefault(),
                    "You're owed ₨%,.0f", r.getTotalOwed()));
            tvOwedLabel.setTextColor(getResources().getColor(R.color.color_tertiary, getTheme()));
        } else if (r.getTotalOwedToUser() > 0) {
            tvOwedLabel.setText(String.format(Locale.getDefault(),
                    "You owe ₨%,.0f", r.getTotalOwedToUser()));
            tvOwedLabel.setTextColor(getResources().getColor(R.color.color_error, getTheme()));
        } else {
            tvOwedLabel.setText(R.string.all_settled_up);
            tvOwedLabel.setTextColor(getResources().getColor(R.color.color_tertiary, getTheme()));
        }
    }

    private void showPlaceholderCard() {
        tvWeekRange.setText(getString(R.string.this_week));
        tvTotalSpent.setText("₨0");
        chipTopCategory.setText(Category.OTHER.label());
        tvOwedLabel.setText(R.string.all_settled_up);
        tvOwedLabel.setTextColor(getResources().getColor(R.color.color_tertiary, getTheme()));
    }

    // ── Past-reports adapter (private inner class) ────────────────────────────

    private class PastReportsAdapter
            extends RecyclerView.Adapter<PastReportsAdapter.ReportVH> {

        private List<WeeklyReport> data = new ArrayList<>();

        void setData(List<WeeklyReport> list) {
            data = new ArrayList<>(list);
            notifyDataSetChanged();
        }

        @Override
        public ReportVH onCreateViewHolder(android.view.ViewGroup parent, int type) {
            android.view.View v = android.view.LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_weekly_report, parent, false);
            return new ReportVH(v);
        }

        @Override
        public void onBindViewHolder(ReportVH h, int pos) { h.bind(data.get(pos)); }

        @Override
        public int getItemCount() { return data.size(); }

        class ReportVH extends RecyclerView.ViewHolder {
            TextView tvRange, tvSpent, tvOwed;
            ReportVH(android.view.View v) {
                super(v);
                tvRange = v.findViewById(R.id.tv_report_range);
                tvSpent = v.findViewById(R.id.tv_report_spent);
                tvOwed  = v.findViewById(R.id.tv_report_owed);
            }
            void bind(WeeklyReport r) {
                tvRange.setText(r.getWeekRange());
                tvSpent.setText(String.format(Locale.getDefault(),
                        "₨%,.0f", r.getTotalSpentPKR()));
                if (r.getTotalOwed() > 0) {
                    tvOwed.setText(String.format(Locale.getDefault(),
                            "+₨%,.0f", r.getTotalOwed()));
                    tvOwed.setTextColor(Color.parseColor("#006C44"));
                } else if (r.getTotalOwedToUser() > 0) {
                    tvOwed.setText(String.format(Locale.getDefault(),
                            "-₨%,.0f", r.getTotalOwedToUser()));
                    tvOwed.setTextColor(Color.parseColor("#BA1A1A"));
                } else {
                    tvOwed.setText(R.string.settled_up);
                    tvOwed.setTextColor(Color.parseColor("#006C44"));
                }
            }
        }
    }
}
