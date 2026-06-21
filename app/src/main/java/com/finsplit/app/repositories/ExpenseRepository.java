package com.finsplit.app.repositories;

import com.finsplit.app.models.Expense;
import com.finsplit.app.models.Group;
import com.finsplit.app.utils.AppConstants;
import com.finsplit.app.utils.ExpenseCallback;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExpenseRepository {

    private static ExpenseRepository instance;
    private final FirebaseFirestore db;

    // Held by the caller; caller must call remove() on destroy to avoid leaks
    private ListenerRegistration activeListener;

    private ExpenseRepository() {
        db = FirebaseFirestore.getInstance();
    }

    public static synchronized ExpenseRepository getInstance() {
        if (instance == null) {
            instance = new ExpenseRepository();
        }
        return instance;
    }

    // ── Expense CRUD ─────────────────────────────────────────────────────────

    /**
     * Writes the expense doc and updates the group's balances map atomically.
     * Balance logic (2-member group): payer gains amountPKR/2, other member loses amountPKR/2.
     */
    public void addExpense(String groupId, Expense expense, List<String> groupMembers,
                           ExpenseCallback callback) {
        DocumentReference expenseRef = db
                .collection(AppConstants.COLLECTION_GROUPS)
                .document(groupId)
                .collection(AppConstants.COLLECTION_EXPENSES)
                .document();

        expense.setExpenseId(expenseRef.getId());
        expense.setGroupId(groupId);

        WriteBatch batch = db.batch();
        batch.set(expenseRef, expense);

        // Update group balances: payer's balance goes up by half, partner's goes down by half
        double half = expense.getAmountPKR() / groupMembers.size();
        Map<String, Object> balanceUpdates = new HashMap<>();
        for (String uid : groupMembers) {
            String field = "balances." + uid;
            if (uid.equals(expense.getPaidByUid())) {
                balanceUpdates.put(field,
                        com.google.firebase.firestore.FieldValue.increment(
                                expense.getAmountPKR() - half));
            } else {
                balanceUpdates.put(field,
                        com.google.firebase.firestore.FieldValue.increment(-half));
            }
        }

        DocumentReference groupRef = db
                .collection(AppConstants.COLLECTION_GROUPS)
                .document(groupId);
        batch.set(groupRef, balanceUpdates, SetOptions.merge());

        batch.commit()
                .addOnSuccessListener(unused -> callback.onExpenseAdded(expense))
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    /**
     * Attaches a real-time listener on /groups/{groupId}/expenses ordered by createdAt DESC.
     * Returns the ListenerRegistration so the caller can detach it on destroy.
     */
    public ListenerRegistration listenToExpenses(String groupId, ExpenseCallback callback) {
        activeListener = db
                .collection(AppConstants.COLLECTION_GROUPS)
                .document(groupId)
                .collection(AppConstants.COLLECTION_EXPENSES)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null) {
                        callback.onError(error.getMessage());
                        return;
                    }
                    if (snapshots == null) return;

                    List<Expense> expenses = new ArrayList<>();
                    for (com.google.firebase.firestore.DocumentSnapshot doc : snapshots.getDocuments()) {
                        Expense e = doc.toObject(Expense.class);
                        if (e != null) {
                            e.setExpenseId(doc.getId());
                            expenses.add(e);
                        }
                    }
                    Collections.sort(expenses);  // enforces Comparable<Expense> ordering client-side too
                    callback.onExpensesLoaded(expenses);
                });
        return activeListener;
    }

    // ── Group helpers ─────────────────────────────────────────────────────────

    /** Creates a group document at a specific ID so it can always be looked up by the same key. */
    public void createGroup(String groupId, Group group, OnGroupCreatedCallback callback) {
        db.collection(AppConstants.COLLECTION_GROUPS)
                .document(groupId)
                .set(group)
                .addOnSuccessListener(unused -> callback.onCreated(groupId))
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void getGroup(String groupId, OnGroupLoadedCallback callback) {
        db.collection(AppConstants.COLLECTION_GROUPS)
                .document(groupId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        Group g = doc.toObject(Group.class);
                        if (g != null) {
                            g.setGroupId(doc.getId());
                            callback.onLoaded(g);
                        } else {
                            callback.onError("Failed to parse group.");
                        }
                    } else {
                        callback.onError("Group not found.");
                    }
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void addMemberToGroup(String groupId, String newMemberUid, OnGroupUpdatedCallback callback) {
        DocumentReference groupRef = db.collection(AppConstants.COLLECTION_GROUPS).document(groupId);
        db.runTransaction(transaction -> {
            com.google.firebase.firestore.DocumentSnapshot snap = transaction.get(groupRef);
            Group group = snap.toObject(Group.class);
            if (group == null) throw new RuntimeException("Group not found");
            java.util.List<String> members = group.getMembers();
            if (members == null) members = new ArrayList<>();
            if (!members.contains(newMemberUid)) {
                members.add(newMemberUid);
                transaction.update(groupRef, "members", members);
                transaction.update(groupRef, "balances." + newMemberUid, 0.0);
            }
            return null;
        })
        .addOnSuccessListener(unused -> callback.onUpdated())
        .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    // ── Nested callback interfaces ────────────────────────────────────────────

    public interface OnGroupUpdatedCallback {
        void onUpdated();
        void onError(String error);
    }

    public interface OnGroupCreatedCallback {
        void onCreated(String groupId);
        void onError(String error);
    }

    public interface OnGroupLoadedCallback {
        void onLoaded(Group group);
        void onError(String error);
    }
}
