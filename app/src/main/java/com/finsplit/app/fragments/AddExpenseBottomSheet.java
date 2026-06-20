package com.finsplit.app.fragments;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.finsplit.app.R;
import com.finsplit.app.models.Category;
import com.finsplit.app.models.Expense;
import com.finsplit.app.repositories.ExpenseRepository;
import com.finsplit.app.utils.ExpenseCallback;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;

import java.util.Arrays;
import java.util.List;

public class AddExpenseBottomSheet extends BottomSheetDialogFragment {

    public static final String TAG = "AddExpenseBottomSheet";
    private static final String ARG_GROUP_ID      = "group_id";
    private static final String ARG_GROUP_MEMBERS = "group_members";

    private String groupId;
    private List<String> groupMembers;

    private TextInputLayout tilTitle, tilAmount;
    private TextInputEditText etTitle, etAmount;
    private ChipGroup chipGroupCategory;
    private Button btnAddExpense;
    private ProgressBar progressBar;

    private ExpenseRepository expenseRepository;
    private OnExpenseAddedListener listener;

    public interface OnExpenseAddedListener {
        void onExpenseAdded();
    }

    public static AddExpenseBottomSheet newInstance(String groupId, List<String> members) {
        AddExpenseBottomSheet sheet = new AddExpenseBottomSheet();
        Bundle args = new Bundle();
        args.putString(ARG_GROUP_ID, groupId);
        args.putStringArrayList(ARG_GROUP_MEMBERS, new java.util.ArrayList<>(members));
        sheet.setArguments(args);
        return sheet;
    }

    public void setOnExpenseAddedListener(OnExpenseAddedListener listener) {
        this.listener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(STYLE_NORMAL, R.style.Theme_FinSplit_BottomSheet);
        if (getArguments() != null) {
            groupId = getArguments().getString(ARG_GROUP_ID, "");
            groupMembers = getArguments().getStringArrayList(ARG_GROUP_MEMBERS);
        }
        expenseRepository = ExpenseRepository.getInstance();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_add_expense, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tilTitle         = view.findViewById(R.id.til_title);
        tilAmount        = view.findViewById(R.id.til_amount);
        etTitle          = view.findViewById(R.id.et_title);
        etAmount         = view.findViewById(R.id.et_amount);
        chipGroupCategory = view.findViewById(R.id.chip_group_category);
        btnAddExpense    = view.findViewById(R.id.btn_add_expense);
        progressBar      = view.findViewById(R.id.progress_bar);

        btnAddExpense.setOnClickListener(v -> attemptAddExpense());
    }

    private void attemptAddExpense() {
        tilTitle.setError(null);
        tilAmount.setError(null);

        String title  = getText(etTitle);
        String amtStr = getText(etAmount);

        if (TextUtils.isEmpty(title)) {
            tilTitle.setError(getString(R.string.error_title_required));
            return;
        }
        if (TextUtils.isEmpty(amtStr)) {
            tilAmount.setError(getString(R.string.error_amount_required));
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amtStr);
        } catch (NumberFormatException e) {
            tilAmount.setError(getString(R.string.error_amount_invalid));
            return;
        }
        if (amount <= 0) {
            tilAmount.setError(getString(R.string.error_amount_invalid));
            return;
        }

        Category category = getSelectedCategory();
        String paidByUid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : "";

        Expense expense = new Expense(groupId, title, amount, paidByUid, category, null);

        setLoading(true);
        List<String> members = (groupMembers != null && !groupMembers.isEmpty())
                ? groupMembers
                : Arrays.asList(paidByUid);

        expenseRepository.addExpense(groupId, expense, members, new ExpenseCallback() {
            @Override
            public void onExpenseAdded(Expense added) {
                setLoading(false);
                if (listener != null) listener.onExpenseAdded();
                dismiss();
            }

            @Override
            public void onExpensesLoaded(java.util.List<Expense> expenses) { /* unused */ }

            @Override
            public void onError(String error) {
                setLoading(false);
                Toast.makeText(getContext(), error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private Category getSelectedCategory() {
        int checkedId = chipGroupCategory.getCheckedChipId();
        if (checkedId == R.id.chip_food)       return Category.FOOD;
        if (checkedId == R.id.chip_transport)  return Category.TRANSPORT;
        if (checkedId == R.id.chip_rent)       return Category.RENT;
        if (checkedId == R.id.chip_utilities)  return Category.UTILITIES;
        return Category.OTHER;
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnAddExpense.setEnabled(!loading);
    }

    private String getText(TextInputEditText et) {
        return et.getText() != null ? et.getText().toString().trim() : "";
    }
}
