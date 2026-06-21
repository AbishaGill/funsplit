package com.finsplit.app.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.finsplit.app.R;
import com.finsplit.app.models.Category;
import com.finsplit.app.models.Expense;
import com.finsplit.app.models.Group;
import com.finsplit.app.models.User;
import com.finsplit.app.repositories.ExpenseRepository;
import com.finsplit.app.repositories.UserRepository;
import com.finsplit.app.utils.AuthCallback;
import com.finsplit.app.utils.BalanceCalculator;
import com.finsplit.app.utils.DebtSimplifier;
import com.finsplit.app.utils.ExpenseCallback;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.ListenerRegistration;

import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class SettleUpActivity extends BaseActivity {

    private RecyclerView rvTransactions;
    private View layoutAllSettled;
    private View layoutNoPartner;
    private TextView tvBalanceAmount;
    private TextView tvBalanceLabel;

    private ExpenseRepository expenseRepository;
    private UserRepository userRepository;
    private BalanceCalculator balanceCalculator;
    private DebtSimplifier debtSimplifier;

    private String groupId;
    private String myUid;
    private List<String> groupMembers;
    private List<DebtSimplifier.Transaction> transactions = new ArrayList<>();
    private Map<String, String> displayNames = new HashMap<>();
    private ListenerRegistration expenseListener;

    private SettleUpAdapter settleUpAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settle_up);

        expenseRepository  = ExpenseRepository.getInstance();
        userRepository     = UserRepository.getInstance();
        balanceCalculator  = new BalanceCalculator();
        debtSimplifier     = new DebtSimplifier();

        myUid   = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid() : "";
        groupId = "group_" + myUid;

        displayNames.put(myUid, "You");

        rvTransactions   = findViewById(R.id.rv_transactions);
        layoutAllSettled = findViewById(R.id.layout_all_settled);
        layoutNoPartner  = findViewById(R.id.layout_no_partner);
        tvBalanceAmount  = findViewById(R.id.tv_balance_amount);
        tvBalanceLabel   = findViewById(R.id.tv_balance_label);

        findViewById(R.id.btn_back_settle).setOnClickListener(v -> finish());

        settleUpAdapter = new SettleUpAdapter();
        rvTransactions.setLayoutManager(new LinearLayoutManager(this));
        rvTransactions.setAdapter(settleUpAdapter);

        loadData();
    }

    private void loadData() {
        expenseRepository.getGroup(groupId, new ExpenseRepository.OnGroupLoadedCallback() {
            @Override
            public void onLoaded(Group group) {
                groupMembers = group.getMembers();
                if (groupMembers == null || groupMembers.isEmpty()) {
                    groupMembers = new ArrayList<>();
                    groupMembers.add(myUid);
                }

                // Update balance card from stored group balances
                updateBalanceSummary(group.getBalances());

                // No partner yet — show guidance instead of empty list
                if (groupMembers.size() < 2) {
                    runOnUiThread(() -> {
                        rvTransactions.setVisibility(View.GONE);
                        layoutAllSettled.setVisibility(View.GONE);
                        layoutNoPartner.setVisibility(View.VISIBLE);
                    });
                    return;
                }

                // Fetch display names for all partners
                for (String uid : groupMembers) {
                    if (!uid.equals(myUid)) {
                        userRepository.getCurrentUser(uid, new AuthCallback() {
                            @Override
                            public void onSuccess(User user) {
                                String name = user.getDisplayName();
                                displayNames.put(uid,
                                        (name != null && !name.isEmpty()) ? name : "Partner");
                            }
                            @Override
                            public void onFailure(String error) {
                                displayNames.put(uid, "Partner");
                            }
                        });
                    }
                }

                expenseListener = expenseRepository.listenToExpenses(groupId, new ExpenseCallback() {
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

    private void updateBalanceSummary(Map<String, Double> balances) {
        if (balances == null) return;
        double myBalance = balances.containsKey(myUid)
                ? (balances.get(myUid) != null ? balances.get(myUid) : 0.0) : 0.0;

        runOnUiThread(() -> {
            tvBalanceAmount.setText(String.format(Locale.getDefault(),
                    "₨ %,.0f", Math.abs(myBalance)));
            if (myBalance > 0.5) {
                tvBalanceLabel.setText("you are owed");
            } else if (myBalance < -0.5) {
                tvBalanceLabel.setText("you owe");
            } else {
                tvBalanceLabel.setText("all settled up");
            }
        });
    }

    private void updateUi() {
        if (transactions.isEmpty()) {
            rvTransactions.setVisibility(View.GONE);
            layoutNoPartner.setVisibility(View.GONE);
            layoutAllSettled.setVisibility(View.VISIBLE);
        } else {
            layoutAllSettled.setVisibility(View.GONE);
            layoutNoPartner.setVisibility(View.GONE);
            rvTransactions.setVisibility(View.VISIBLE);
            settleUpAdapter.setData(transactions);
        }
    }

    private void markSettled(DebtSimplifier.Transaction tx) {
        Expense settlement = new Expense(groupId, "Settled", 0.0,
                myUid, Category.OTHER, "Settled: "
                + nameFor(tx.getFromUid()) + " → " + nameFor(tx.getToUid()));

        List<String> members = groupMembers != null ? groupMembers : new ArrayList<>();
        expenseRepository.addExpense(groupId, settlement, members, new ExpenseCallback() {
            @Override
            public void onExpenseAdded(Expense e) {
                runOnUiThread(() -> {
                    Toast.makeText(SettleUpActivity.this,
                            getString(R.string.settled_up), Toast.LENGTH_SHORT).show();
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

    private String nameFor(String uid) {
        return displayNames.containsKey(uid) ? displayNames.get(uid)
                : uid.substring(0, Math.min(6, uid.length()));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (expenseListener != null) expenseListener.remove();
    }

    // ── Inner adapter ─────────────────────────────────────────────────────────

    private class SettleUpAdapter extends RecyclerView.Adapter<SettleUpAdapter.TxViewHolder> {

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
            holder.bind(data.get(position));
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
                String fromName = nameFor(tx.getFromUid());
                String toName   = nameFor(tx.getToUid());

                tvAvatarFrom.setText(fromName.substring(0, 1).toUpperCase());
                tvAvatarTo.setText(toName.substring(0, 1).toUpperCase());
                tvFrom.setText(fromName);
                tvTo.setText(toName);
                tvAmount.setText(String.format(Locale.getDefault(),
                        "₨ %,.0f", tx.getAmount()));

                btnSettle.setOnClickListener(v -> markSettled(tx));
            }
        }
    }
}
