package com.finsplit.app.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.finsplit.app.R;
import com.finsplit.app.models.Category;
import com.finsplit.app.models.Expense;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * OOP — Encapsulation: list state is private; mutated only via submitList().
 * OOP — Inheritance: extends RecyclerView.Adapter.
 */
public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder> {

    private List<Expense> expenses = new ArrayList<>();
    private final String currentUserId;
    private final SimpleDateFormat dateFormat =
            new SimpleDateFormat("d MMM yyyy", Locale.getDefault());

    public HistoryAdapter(String currentUserId) {
        this.currentUserId = currentUserId;
    }

    public void submitList(List<Expense> newList) {
        expenses = new ArrayList<>(newList);
        notifyDataSetChanged();
    }

    public Expense getItemAt(int position) {
        if (position < 0 || position >= expenses.size()) return null;
        return expenses.get(position);
    }

    @NonNull
    @Override
    public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_history, parent, false);
        return new HistoryViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position) {
        holder.bind(expenses.get(position));
    }

    @Override
    public int getItemCount() { return expenses.size(); }

    class HistoryViewHolder extends RecyclerView.ViewHolder {
        TextView tvAvatar, tvTitle, tvCategory, tvDate, tvAmount, tvNote;

        HistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAvatar   = itemView.findViewById(R.id.tv_hist_avatar);
            tvTitle    = itemView.findViewById(R.id.tv_hist_title);
            tvCategory = itemView.findViewById(R.id.tv_hist_category);
            tvDate     = itemView.findViewById(R.id.tv_hist_date);
            tvAmount   = itemView.findViewById(R.id.tv_hist_amount);
            tvNote     = itemView.findViewById(R.id.tv_hist_note);
        }

        void bind(Expense expense) {
            boolean paidByMe = currentUserId.equals(expense.getPaidByUid());
            String initials = getInitials(expense.getTitle());
            tvAvatar.setText(initials);
            tvAvatar.setBackgroundResource(paidByMe
                    ? R.drawable.bg_avatar_orange
                    : R.drawable.bg_avatar_blue);

            tvTitle.setText(expense.getTitle());

            Category cat = expense.getCategoryEnum();
            tvCategory.setText(cat.label());
            tvCategory.setCompoundDrawablesWithIntrinsicBounds(
                    getCategoryIcon(cat), 0, 0, 0);

            if (expense.getCreatedAt() != null) {
                tvDate.setText(dateFormat.format(expense.getCreatedAt()));
            } else {
                tvDate.setText("");
            }

            if (expense.getAmountPKR() == 0) {
                tvAmount.setText(R.string.settled_up);
                tvAmount.setTextColor(
                        itemView.getContext().getResources().getColor(
                                R.color.color_tertiary,
                                itemView.getContext().getTheme()));
            } else {
                tvAmount.setText(String.format(Locale.getDefault(),
                        "₨ %,.0f", expense.getAmountPKR()));
                tvAmount.setTextColor(
                        itemView.getContext().getResources().getColor(
                                R.color.text_primary,
                                itemView.getContext().getTheme()));
            }

            String note = expense.getNote();
            if (note != null && !note.isEmpty()) {
                tvNote.setVisibility(View.VISIBLE);
                tvNote.setText(note);
            } else {
                tvNote.setVisibility(View.GONE);
            }
        }

        private String getInitials(String title) {
            if (title == null || title.isEmpty()) return "?";
            String[] words = title.trim().split("\\s+");
            if (words.length == 1) return String.valueOf(words[0].charAt(0)).toUpperCase();
            return (String.valueOf(words[0].charAt(0))
                    + String.valueOf(words[1].charAt(0))).toUpperCase();
        }

        private int getCategoryIcon(Category cat) {
            switch (cat) {
                case FOOD:       return R.drawable.ic_category_food;
                case TRANSPORT:  return R.drawable.ic_category_transport;
                case RENT:       return R.drawable.ic_category_rent;
                case UTILITIES:  return R.drawable.ic_category_utilities;
                default:         return R.drawable.ic_category_other;
            }
        }
    }
}
