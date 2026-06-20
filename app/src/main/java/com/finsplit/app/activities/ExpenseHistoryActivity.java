package com.finsplit.app.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.finsplit.app.R;
import com.finsplit.app.adapters.HistoryAdapter;
import com.finsplit.app.models.Category;
import com.finsplit.app.models.Expense;
import com.finsplit.app.repositories.ExpenseRepository;
import com.finsplit.app.utils.AppConstants;
import com.finsplit.app.utils.ExpenseCallback;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Shows all expenses (descending) with category filter chips.
 * Swipe left deletes an expense — only if the current user is the payer.
 *
 * OOP — Inheritance: extends BaseActivity (auth guard).
 * OOP — Interface: ExpenseCallback drives real-time updates from repository.
 */
public class ExpenseHistoryActivity extends BaseActivity {

    private HistoryAdapter historyAdapter;
    private ExpenseRepository expenseRepository;
    private ListenerRegistration listener;

    private TextView tvEmpty;
    private RecyclerView rvHistory;
    private ChipGroup chipGroup;

    private String myUid;
    private String groupId;
    private List<Expense> allExpenses = new ArrayList<>();
    private String activeFilter = "ALL";   // ALL, FOOD, TRANSPORT, RENT, UTILITIES, OTHER

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        expenseRepository = ExpenseRepository.getInstance();
        myUid   = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : "";
        groupId = "group_" + myUid;

        tvEmpty   = findViewById(R.id.tv_history_empty);
        rvHistory = findViewById(R.id.rv_history);
        chipGroup = findViewById(R.id.chip_group_filter);

        findViewById(R.id.btn_back_history).setOnClickListener(v -> finish());

        historyAdapter = new HistoryAdapter(myUid);
        rvHistory.setLayoutManager(new LinearLayoutManager(this));
        rvHistory.setAdapter(historyAdapter);

        attachSwipeToDelete();
        setupChipFilters();

        listener = expenseRepository.listenToExpenses(groupId, new ExpenseCallback() {
            @Override
            public void onExpensesLoaded(List<Expense> expenses) {
                allExpenses = expenses;
                applyFilter();
            }
            @Override public void onExpenseAdded(Expense e) {}
            @Override public void onError(String error) {
                Toast.makeText(ExpenseHistoryActivity.this, error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupChipFilters() {
        chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            int id = checkedIds.get(0);
            if      (id == R.id.chip_all)       activeFilter = "ALL";
            else if (id == R.id.chip_food)      activeFilter = "FOOD";
            else if (id == R.id.chip_transport) activeFilter = "TRANSPORT";
            else if (id == R.id.chip_rent)      activeFilter = "RENT";
            else if (id == R.id.chip_utilities) activeFilter = "UTILITIES";
            else if (id == R.id.chip_other)     activeFilter = "OTHER";
            applyFilter();
        });
    }

    private void applyFilter() {
        List<Expense> filtered;
        if ("ALL".equals(activeFilter)) {
            filtered = new ArrayList<>(allExpenses);
        } else {
            filtered = new ArrayList<>();
            for (Expense e : allExpenses) {
                if (activeFilter.equalsIgnoreCase(e.getCategory())) {
                    filtered.add(e);
                }
            }
        }
        Collections.sort(filtered);  // Comparable<Expense> → DESC by createdAt
        historyAdapter.submitList(filtered);
        tvEmpty.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
        rvHistory.setVisibility(filtered.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void attachSwipeToDelete() {
        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(
                0, ItemTouchHelper.LEFT) {

            @Override
            public boolean onMove(@NonNull RecyclerView rv,
                                  @NonNull RecyclerView.ViewHolder vh,
                                  @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder vh, int direction) {
                int pos = vh.getAdapterPosition();
                Expense expense = historyAdapter.getItemAt(pos);

                if (expense == null) return;

                // Only the payer can delete their own expense
                if (!myUid.equals(expense.getPaidByUid())) {
                    Toast.makeText(ExpenseHistoryActivity.this,
                            getString(R.string.error_delete_not_payer),
                            Toast.LENGTH_SHORT).show();
                    historyAdapter.notifyItemChanged(pos);  // snap back
                    return;
                }

                deleteExpense(expense, pos);
            }
        }).attachToRecyclerView(rvHistory);
    }

    private void deleteExpense(Expense expense, int position) {
        FirebaseFirestore.getInstance()
                .collection(AppConstants.COLLECTION_GROUPS)
                .document(groupId)
                .collection(AppConstants.COLLECTION_EXPENSES)
                .document(expense.getExpenseId())
                .delete()
                .addOnSuccessListener(unused -> {
                    // Real-time listener will refresh the list automatically
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
                    historyAdapter.notifyItemChanged(position);
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (listener != null) listener.remove();
    }
}
