package com.finsplit.app.utils;

/**
 * OOP — Inheritance: extends BaseExpenseSplit.
 * Calculates a member's share as a fixed percentage of the total.
 * E.g. percentage=30 → share = totalAmount * 0.30.
 */
public class PercentageSplit extends BaseExpenseSplit {

    private final double percentage;  // 0–100

    public PercentageSplit(double percentage) {
        this.percentage = percentage;
    }

    @Override
    public double calculateShare(double totalAmount, int memberCount) {
        return totalAmount * (percentage / 100.0);
    }

    public double getPercentage() { return percentage; }
}
