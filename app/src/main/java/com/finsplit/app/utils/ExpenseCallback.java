package com.finsplit.app.utils;

import com.finsplit.app.models.Expense;

import java.util.List;

public interface ExpenseCallback {
    void onExpenseAdded(Expense expense);
    void onExpensesLoaded(List<Expense> expenses);
    void onError(String error);
}
