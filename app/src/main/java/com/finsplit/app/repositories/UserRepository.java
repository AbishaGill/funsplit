package com.finsplit.app.repositories;

import com.finsplit.app.models.User;
import com.finsplit.app.utils.AppConstants;
import com.finsplit.app.utils.AuthCallback;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class UserRepository {

    private static UserRepository instance;
    private final FirebaseFirestore db;

    private UserRepository() {
        db = FirebaseFirestore.getInstance();
    }

    public static synchronized UserRepository getInstance() {
        if (instance == null) {
            instance = new UserRepository();
        }
        return instance;
    }

    /**
     * Creates a new user document or updates display name / profile pic if already exists.
     * Uses SetOptions.merge() so existing fields (currency, fcmToken) are never overwritten.
     */
    public void createOrUpdateUser(FirebaseUser firebaseUser, AuthCallback callback) {
        if (firebaseUser == null) {
            callback.onFailure("No authenticated user.");
            return;
        }

        DocumentReference ref = db.collection(AppConstants.COLLECTION_USERS)
                .document(firebaseUser.getUid());

        ref.get().addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                callback.onFailure(getErrorMessage(task.getException()));
                return;
            }

            DocumentSnapshot snapshot = task.getResult();
            Map<String, Object> data = new HashMap<>();

            if (!snapshot.exists()) {
                // First login — write full document
                data.put("userId", firebaseUser.getUid());
                data.put("email", safeString(firebaseUser.getEmail()));
                data.put("displayName", safeString(firebaseUser.getDisplayName()));
                data.put("profilePicUrl", firebaseUser.getPhotoUrl() != null
                        ? firebaseUser.getPhotoUrl().toString() : null);
                data.put("defaultCurrency", AppConstants.CURRENCY_DEFAULT);
                data.put("fcmToken", null);
                // createdAt will be set by @ServerTimestamp when using User POJO, but
                // here we write a map; use server timestamp explicitly:
                data.put("createdAt", com.google.firebase.firestore.FieldValue.serverTimestamp());
            } else {
                // Subsequent login — only refresh mutable identity fields
                data.put("displayName", safeString(firebaseUser.getDisplayName()));
                data.put("profilePicUrl", firebaseUser.getPhotoUrl() != null
                        ? firebaseUser.getPhotoUrl().toString() : null);
            }

            ref.set(data, SetOptions.merge())
                    .addOnSuccessListener(unused -> {
                        // Read back the merged document so the callback gets a full User
                        getCurrentUser(firebaseUser.getUid(), callback);
                    })
                    .addOnFailureListener(e -> callback.onFailure(e.getMessage()));
        });
    }

    public void getCurrentUser(String userId, AuthCallback callback) {
        db.collection(AppConstants.COLLECTION_USERS)
                .document(userId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                        User user = task.getResult().toObject(User.class);
                        if (user != null) {
                            user.setUserId(task.getResult().getId());
                            callback.onSuccess(user);
                        } else {
                            callback.onFailure("Failed to deserialize user.");
                        }
                    } else {
                        callback.onFailure(task.getException() != null
                                ? task.getException().getMessage()
                                : "User not found.");
                    }
                });
    }

    public void updateDefaultCurrency(String userId, String currency, AuthCallback callback) {
        db.collection(AppConstants.COLLECTION_USERS)
                .document(userId)
                .update("defaultCurrency", currency)
                .addOnSuccessListener(unused ->
                        getCurrentUser(userId, callback))
                .addOnFailureListener(e ->
                        callback.onFailure(e.getMessage()));
    }

    public void updateFcmToken(String userId, String token) {
        db.collection(AppConstants.COLLECTION_USERS)
                .document(userId)
                .update("fcmToken", token)
                .addOnFailureListener(e -> { /* best-effort, no callback needed */ });
    }

    private String safeString(String value) {
        return value != null ? value : "";
    }

    private String getErrorMessage(Exception e) {
        return e != null ? e.getMessage() : "Unknown error";
    }
}
