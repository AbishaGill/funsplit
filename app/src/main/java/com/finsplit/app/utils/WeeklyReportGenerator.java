package com.finsplit.app.utils;

import com.finsplit.app.models.Category;
import com.finsplit.app.models.Expense;
import com.finsplit.app.models.WeeklyReport;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Pure-Java utility that converts a list of expenses into a WeeklyReport.
 * No Android framework dependency — fully unit-testable.
 *
 * OOP — Encapsulation: computation is private; callers use generate().
 * OOP — Polymorphism: accepts any BaseExpenseSplit strategy for net balance calculation.
 */
public class WeeklyReportGenerator {

    private static final SimpleDateFormat RANGE_FMT =
            new SimpleDateFormat("MMM d", Locale.getDefault());

    /**
     * Builds a WeeklyReport from expenses that fall in the past 7 days.
     *
     * @param allExpenses  full expense list (will be filtered internally)
     * @param memberUids   group members for balance calculation
     * @param currentUid   the user requesting the report
     * @return populated WeeklyReport (not yet persisted)
     */
    public WeeklyReport generate(List<Expense> allExpenses,
                                 List<String> memberUids,
                                 String currentUid) {

        // ── 1. Filter to last 7 days ──────────────────────────────────────────
        long now = System.currentTimeMillis();
        long weekAgo = now - 7L * 24 * 60 * 60 * 1000;
        java.util.List<Expense> weekly = new java.util.ArrayList<>();
        for (Expense e : allExpenses) {
            if (e.getCreatedAt() != null && e.getCreatedAt().getTime() >= weekAgo) {
                weekly.add(e);
            }
        }

        // ── 2. Total spent ────────────────────────────────────────────────────
        double totalSpent = 0;
        for (Expense e : weekly) totalSpent += e.getAmountPKR();

        // ── 3. Top category ───────────────────────────────────────────────────
        Map<String, Double> byCategory = new HashMap<>();
        for (Expense e : weekly) {
            String cat = e.getCategory() != null ? e.getCategory() : Category.OTHER.name();
            byCategory.put(cat, byCategory.getOrDefault(cat, 0.0) + e.getAmountPKR());
        }
        String topCategory = Category.OTHER.name();
        double maxCat = 0;
        for (Map.Entry<String, Double> entry : byCategory.entrySet()) {
            if (entry.getValue() > maxCat) {
                maxCat = entry.getValue();
                topCategory = entry.getKey();
            }
        }

        // ── 4. Net balances via EqualSplit strategy (Polymorphism) ─────────────
        BalanceCalculator calc = new BalanceCalculator();
        Map<String, Double> balances = calc.calculateNetBalances(weekly, memberUids, new EqualSplit());
        double myBalance = balances.containsKey(currentUid) ? balances.get(currentUid) : 0.0;
        double totalOwed       = myBalance > 0 ? myBalance : 0;    // others owe me
        double totalOwedToUser = myBalance < 0 ? -myBalance : 0;   // I owe others

        // ── 5. Week range string ──────────────────────────────────────────────
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(now);
        String end = RANGE_FMT.format(cal.getTime());
        cal.setTimeInMillis(weekAgo);
        String start = RANGE_FMT.format(cal.getTime());
        String weekRange = start + " – " + end;

        return new WeeklyReport(currentUid, totalSpent, topCategory,
                totalOwed, totalOwedToUser, weekRange);
    }
}
