package com.finsplit.app.utils;

/**
 * OOP — Inheritance: extends BaseExpenseSplit.
 * Returns a pre-set exact amount regardless of total or member count.
 * Used when one member owes a specific fixed sum rather than a proportional share.
 */
public class ExactSplit extends BaseExpenseSplit {

    private final double exactAmount;

    public ExactSplit(double exactAmount) {
        this.exactAmount = exactAmount;
    }

    @Override
    public double calculateShare(double totalAmount, int memberCount) {
        return exactAmount;
    }

    public double getExactAmount() { return exactAmount; }
}
