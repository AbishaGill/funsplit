package com.finsplit.app.utils;

/**
 * OOP — Inheritance: extends BaseExpenseSplit.
 * Divides the total amount equally among all members.
 */
public class EqualSplit extends BaseExpenseSplit {

    @Override
    public double calculateShare(double totalAmount, int memberCount) {
        if (memberCount <= 0) return 0;
        return totalAmount / memberCount;
    }
}
