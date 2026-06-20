package com.finsplit.app.models;

import androidx.annotation.NonNull;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;

public class Expense implements Comparable<Expense> {

    @DocumentId
    private String expenseId;
    private String groupId;
    private String title;
    private double amountPKR;
    private String paidByUid;
    private String category;   // stored as string; convert via Category.fromString()
    private String note;

    @ServerTimestamp
    private Date createdAt;

    // Required no-arg constructor for Firestore
    public Expense() {}

    public Expense(String groupId, String title, double amountPKR,
                   String paidByUid, Category category, String note) {
        this.groupId = groupId;
        this.title = title;
        this.amountPKR = amountPKR;
        this.paidByUid = paidByUid;
        this.category = category != null ? category.name() : Category.OTHER.name();
        this.note = note;
    }

    // ── Getters ──────────────────────────────────────────────────────────────

    public String getExpenseId() { return expenseId; }
    public String getGroupId()   { return groupId; }
    public String getTitle()     { return title; }
    public double getAmountPKR() { return amountPKR; }
    public String getPaidByUid() { return paidByUid; }
    public String getCategory()  { return category; }
    public String getNote()      { return note; }
    public Date   getCreatedAt() { return createdAt; }

    // ── Setters ──────────────────────────────────────────────────────────────

    public void setExpenseId(String expenseId) { this.expenseId = expenseId; }
    public void setGroupId(String groupId)     { this.groupId = groupId; }
    public void setTitle(String title)         { this.title = title; }
    public void setAmountPKR(double amountPKR) { this.amountPKR = amountPKR; }
    public void setPaidByUid(String paidByUid) { this.paidByUid = paidByUid; }
    public void setCategory(String category)   { this.category = category; }
    public void setNote(String note)           { this.note = note; }
    public void setCreatedAt(Date createdAt)   { this.createdAt = createdAt; }

    @Exclude
    public Category getCategoryEnum() {
        return Category.fromString(category);
    }

    /** Descending by createdAt — most recent first. Null createdAt sorts to end. */
    @Override
    public int compareTo(@NonNull Expense other) {
        if (this.createdAt == null && other.createdAt == null) return 0;
        if (this.createdAt == null) return 1;
        if (other.createdAt == null) return -1;
        return other.createdAt.compareTo(this.createdAt); // DESC
    }
}
