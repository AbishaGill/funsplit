package com.finsplit.app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.finsplit.app.R;
import com.finsplit.app.models.Expense;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ExpenseAdapter extends RecyclerView.Adapter<ExpenseAdapter.ExpenseViewHolder> {

    private final List<Expense> expenses = new ArrayList<>();
    private final String currentUserId;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("d MMM", Locale.getDefault());

    public ExpenseAdapter(String currentUserId) {
        this.currentUserId = currentUserId;
    }

    public void submitList(List<Expense> newList) {
        expenses.clear();
        expenses.addAll(newList);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ExpenseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_expense, parent, false);
        return new ExpenseViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ExpenseViewHolder holder, int position) {
        holder.bind(expenses.get(position));
    }

    @Override
    public int getItemCount() {
        return expenses.size();
    }

    class ExpenseViewHolder extends RecyclerView.ViewHolder {

        private final TextView tvAvatar;
        private final TextView tvTitle;
        private final TextView tvCategory;
        private final TextView tvDate;
        private final TextView tvAmount;

        ExpenseViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAvatar   = itemView.findViewById(R.id.tv_avatar);
            tvTitle    = itemView.findViewById(R.id.tv_title);
            tvCategory = itemView.findViewById(R.id.tv_category);
            tvDate     = itemView.findViewById(R.id.tv_date);
            tvAmount   = itemView.findViewById(R.id.tv_amount);
        }

        void bind(Expense expense) {
            // Avatar: initials + color by payer
            boolean paidByMe = currentUserId.equals(expense.getPaidByUid());
            String initials = getInitials(expense.getTitle());
            tvAvatar.setText(initials);
            tvAvatar.setBackgroundResource(paidByMe
                    ? R.drawable.bg_avatar_orange
                    : R.drawable.bg_avatar_blue);

            tvTitle.setText(expense.getTitle());
            tvCategory.setText(expense.getCategoryEnum().label());

            if (expense.getCreatedAt() != null) {
                tvDate.setText(dateFormat.format(expense.getCreatedAt()));
            } else {
                tvDate.setText("");
            }

            tvAmount.setText(String.format(Locale.getDefault(),
                    "₨ %,.0f", expense.getAmountPKR()));
        }

        private String getInitials(String title) {
            if (title == null || title.isEmpty()) return "?";
            String[] words = title.trim().split("\\s+");
            if (words.length == 1) {
                return String.valueOf(words[0].charAt(0)).toUpperCase();
            }
            return (String.valueOf(words[0].charAt(0))
                    + String.valueOf(words[1].charAt(0))).toUpperCase();
        }
    }
}
