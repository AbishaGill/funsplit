package com.finsplit.app.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

/**
 * Greedy debt-simplification algorithm.
 * Given a map of uid → net balance, produces the minimum number of
 * directed payments that settle all debts.
 *
 * OOP — Encapsulation: Transaction is an inner class whose fields are
 * private with public getters, hiding implementation details from callers.
 */
public class DebtSimplifier {

    /**
     * OOP — Encapsulation (inner class):
     * Represents one directed payment. Caller reads values via getters only.
     */
    public static class Transaction {
        private final String fromUid;   // who pays
        private final String toUid;     // who receives
        private final double amount;

        public Transaction(String fromUid, String toUid, double amount) {
            this.fromUid = fromUid;
            this.toUid = toUid;
            this.amount = amount;
        }

        public String getFromUid() { return fromUid; }
        public String getToUid()   { return toUid; }
        public double getAmount()  { return amount; }
    }

    /**
     * Simplifies a balance map into the minimum set of transactions.
     * Uses two max-heaps (one for creditors, one for debtors).
     *
     * @param balances uid → net balance (positive = owed, negative = owes)
     * @return list of simplified Transaction objects
     */
    public List<Transaction> simplifyDebts(Map<String, Double> balances) {
        List<Transaction> transactions = new ArrayList<>();

        // Max-heap of creditors (largest credit first)
        PriorityQueue<double[]> creditors = new PriorityQueue<>(
                (a, b) -> Double.compare(b[1], a[1]));

        // Max-heap of debtors (largest debt first, stored as positive)
        PriorityQueue<double[]> debtors = new PriorityQueue<>(
                (a, b) -> Double.compare(b[1], a[1]));

        // uid is encoded as its index in the entry set for the heap
        String[] uids = balances.keySet().toArray(new String[0]);
        for (int i = 0; i < uids.length; i++) {
            double bal = balances.get(uids[i]);
            if (bal > 0.001) {
                creditors.offer(new double[]{i, bal});
            } else if (bal < -0.001) {
                debtors.offer(new double[]{i, -bal});   // store as positive
            }
        }

        while (!creditors.isEmpty() && !debtors.isEmpty()) {
            double[] creditor = creditors.poll();
            double[] debtor   = debtors.poll();

            int cIdx = (int) creditor[0];
            int dIdx = (int) debtor[0];
            double credit = creditor[1];
            double debt   = debtor[1];

            double settled = Math.min(credit, debt);
            transactions.add(new Transaction(uids[dIdx], uids[cIdx],
                    Math.round(settled * 100.0) / 100.0));

            double remainingCredit = credit - settled;
            double remainingDebt   = debt   - settled;

            if (remainingCredit > 0.001) creditors.offer(new double[]{cIdx, remainingCredit});
            if (remainingDebt   > 0.001) debtors.offer(new double[]{dIdx, remainingDebt});
        }

        return transactions;
    }
}
