package com.finsplit.app.utils;

/**
 * OOP — Inheritance (Abstract Base Class):
 * Defines the split-strategy contract. Subclasses (EqualSplit, PercentageSplit, ExactSplit)
 * inherit this and override calculateShare to implement their own splitting logic.
 * BalanceCalculator uses BaseExpenseSplit polymorphically — it never needs to know
 * which subclass it holds.
 */
public abstract class BaseExpenseSplit {

    /**
     * Returns how much a single member owes for this expense.
     *
     * @param totalAmount  the full expense amount
     * @param memberCount  number of members sharing the expense
     * @return per-member share in the same currency unit
     */
    public abstract double calculateShare(double totalAmount, int memberCount);
}
