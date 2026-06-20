package com.finsplit.app.utils;

import com.finsplit.app.models.Expense;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * OOP — Polymorphism:
 * Accepts a BaseExpenseSplit strategy at call-time. The caller decides whether expenses
 * were split equally, by percentage, or by exact amount — BalanceCalculator does not
 * care which concrete subclass it receives.
 *
 * OOP — Encapsulation:
 * All internal state is private. The public API is a single pure method that
 * returns a new Map without modifying either argument.
 */
public class BalanceCalculator {

    /**
     * Computes the net balance for every member across a list of expenses.
     *
     * Convention: positive balance → member is owed money;
     *             negative balance → member owes money.
     *
     * @param expenses    list of expenses to process
     * @param memberUids  all member UIDs who share costs
     * @param splitStrategy  how to calculate each member's share (polymorphism)
     * @return map of uid → net balance
     */
    public Map<String, Double> calculateNetBalances(
            List<Expense> expenses,
            List<String> memberUids,
            BaseExpenseSplit splitStrategy) {

        // OOP — Encapsulation: accumulator is private local state
        Map<String, Double> balances = new HashMap<>();
        for (String uid : memberUids) {
            balances.put(uid, 0.0);
        }

        for (Expense expense : expenses) {
            // Skip settlement entries (amountPKR == 0)
            if (expense.getAmountPKR() == 0) continue;

            double total = expense.getAmountPKR();
            String payer = expense.getPaidByUid();
            int count = memberUids.size();

            // OOP — Polymorphism: strategy determines share without an if/else chain
            double share = splitStrategy.calculateShare(total, count);

            for (String uid : memberUids) {
                double current = balances.containsKey(uid) ? balances.get(uid) : 0.0;
                if (uid.equals(payer)) {
                    // Payer fronted the full amount; credit back everyone else's share
                    balances.put(uid, current + (total - share));
                } else {
                    // Non-payer owes their share to the payer
                    balances.put(uid, current - share);
                }
            }
        }

        return balances;
    }

    /**
     * Convenience overload that defaults to EqualSplit — the common case in FinSplit.
     */
    public Map<String, Double> calculateNetBalances(
            List<Expense> expenses,
            List<String> memberUids) {
        return calculateNetBalances(expenses, memberUids, new EqualSplit());
    }
}
