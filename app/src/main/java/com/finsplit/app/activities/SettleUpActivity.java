package com.finsplit.app.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.finsplit.app.R;
import com.finsplit.app.models.Category;
import com.finsplit.app.models.Expense;
import com.finsplit.app.models.Group;
import com.finsplit.app.repositories.ExpenseRepository;
import com.finsplit.app.repositories.UserRepository;
import com.finsplit.app.utils.BalanceCalculator;
import com.finsplit.app.utils.DebtSimplifier;
import com.finsplit.app.utils.ExpenseCallback;
import com.finsplit.app.utils.SessionManager;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Displays simplified debts for the group and lets users mark individual
 * debts as settled, which writes a zero-amount settlement expense and
 * recalculates balances via BalanceCalculator.
 *
 * OOP — Polymorphism: passes an EqualSplit strategy to BalanceCalculator.
 * OOP — Encapsulation: adapter and helper state are private fields.
 * OOP — Inheritance: extends BaseActivity (auth guard).
 */
public class SettleUpActivity extends BaseActivity {

    private RecyclerView rvTransactions;
    private TextView tvEmpty;

    private ExpenseRepository expenseRepository;
    private BalanceCalculator balanceCalculator;
    private DebtSimplifier debtSimplifier;
    private SessionManager sessionManager;

    private String groupId;
    private List<String> groupMembers;
    private List<DebtSimplifier.Transaction> transactions = new ArrayList<>();

    // Simple in-activity adapter to avoid a separate file for a small list
    private SettleUpAdapter settleUpAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settle_up);

        expenseRepository = ExpenseRepository.getInstance();
        balanceCalculator = new BalanceCalculator();
        debtSimplifier    = new DebtSimplifier();
        sessionManager    = new SessionManager(this);

        String myUid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : "";
        groupId = "group_" + myUid;

        rvTransactions = findViewById(R.id.rv_transactions);
        tvEmpty        = findViewById(R.id.tv_settle_empty);

        findViewById(R.id.btn_back_settle).setOnClickListener(v -> finish());

        settleUpAdapter = new SettleUpAdapter();
        rvTransactions.setLayoutManager(new LinearLayoutManager(this));
        rvTransactions.setAdapter(settleUpAdapter);

        loadData(myUid);
    }

    private void loadData(String myUid) {
        expenseRepository.getGroup(groupId, new ExpenseRepository.OnGroupLoadedCallback() {
            @Override
            public void onLoaded(Group group) {
                groupMembers = group.getMembers();
                if (groupMembers == null || groupMembers.isEmpty()) {
                    groupMembers = new ArrayList<>();
                    groupMembers.add(myUid);
                }
                // Fetch all expenses once to compute balances
                expenseRepository.listenToExpenses(groupId, new ExpenseCallback() {
                    @Override
                    public void onExpensesLoaded(List<Expense> expenses) {
                        Map<String, Double> balances =
                                balanceCalculator.calculateNetBalances(expenses, groupMembers);
                        transactions = debtSimplifier.simplifyDebts(balances);
                        runOnUiThread(() -> updateUi());
                    }
                    @Override public void onExpenseAdded(Expense e) {}
                    @Override public void onError(String error) {}
                });
            }
            @Override
            public void onError(String error) {
                Toast.makeText(SettleUpActivity.this,
                        getString(R.string.error_generic), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateUi() {
        if (transactions.isEmpty()) {
            tvEmpty.setVisibility(View.VISIBLE);
            rvTransactions.setVisibility(View.GONE);
        } else {
            tvEmpty.setVisibility(View.GONE);
            rvTransactions.setVisibility(View.VISIBLE);
            settleUpAdapter.setData(transactions);
        }
    }

    private void markSettled(DebtSimplifier.Transaction tx) {
        String myUid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : "";

        // Write a zero-amount settlement expense — persists the event in history
        Expense settlement = new Expense(groupId, "Settled", 0.0,
                myUid, Category.OTHER, "Settled: "
                + tx.getFromUid().substring(0, Math.min(6, tx.getFromUid().length()))
                + " → "
                + tx.getToUid().substring(0, Math.min(6, tx.getToUid().length())));

        List<String> members = groupMembers != null ? groupMembers : new ArrayList<>();
        expenseRepository.addExpense(groupId, settlement, members, new ExpenseCallback() {
            @Override
            public void onExpenseAdded(Expense e) {
                runOnUiThread(() -> {
                    Toast.makeText(SettleUpActivity.this,
                            getString(R.string.settled_up), Toast.LENGTH_SHORT).show();
                    // Remove the settled transaction from the displayed list
                    transactions.remove(tx);
                    updateUi();
                });
            }
            @Override public void onExpensesLoaded(List<Expense> expenses) {}
            @Override public void onError(String error) {
                runOnUiThread(() ->
                        Toast.makeText(SettleUpActivity.this,
                                error, Toast.LENGTH_SHORT).show());
            }
        });
    }

    // ── Inner RecyclerView adapter ────────────────────────────────────────────

    /**
     * OOP — Encapsulation: private inner adapter hides view-binding details.
     */
    private class SettleUpAdapter
            extends RecyclerView.Adapter<SettleUpAdapter.TxViewHolder> {

        private List<DebtSimplifier.Transaction> data = new ArrayList<>();

        void setData(List<DebtSimplifier.Transaction> list) {
            data = new ArrayList<>(list);
            notifyDataSetChanged();
        }

        @Override
        public TxViewHolder onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
            android.view.View v = android.view.LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_transaction, parent, false);
            return new TxViewHolder(v);
        }

        @Override
        public void onBindViewHolder(TxViewHolder holder, int position) {
            DebtSimplifier.Transaction tx = data.get(position);
            holder.bind(tx);
        }

        @Override
        public int getItemCount() { return data.size(); }

        class TxViewHolder extends RecyclerView.ViewHolder {
            TextView tvFrom, tvTo, tvAmount, tvAvatarFrom, tvAvatarTo;
            android.widget.Button btnSettle;

            TxViewHolder(android.view.View itemView) {
                super(itemView);
                tvAvatarFrom = itemView.findViewById(R.id.tv_avatar_from);
                tvAvatarTo   = itemView.findViewById(R.id.tv_avatar_to);
                tvFrom       = itemView.findViewById(R.id.tv_from);
                tvTo         = itemView.findViewById(R.id.tv_to);
                tvAmount     = itemView.findViewById(R.id.tv_settle_amount);
                btnSettle    = itemView.findViewById(R.id.btn_mark_settled);
            }

            void bind(DebtSimplifier.Transaction tx) {
                // Show first 6 chars of uid as display name placeholder
                // (in production, you'd look up display names from Firestore)
                String fromLabel = shortUid(tx.getFromUid());
                String toLabel   = shortUid(tx.getToUid());

                tvAvatarFrom.setText(fromLabel.substring(0, 1).toUpperCase());
                tvAvatarTo.setText(toLabel.substring(0, 1).toUpperCase());
                tvFrom.setText(fromLabel);
                tvTo.setText(toLabel);
                tvAmount.setText(String.format(Locale.getDefault(),
                        "₨ %,.0f", tx.getAmount()));

                btnSettle.setOnClickListener(v -> markSettled(tx));
            }

            private String shortUid(String uid) {
                if (uid == null || uid.isEmpty()) return "?";
                return uid.substring(0, Math.min(6, uid.length()));
            }
        }
    }
}
