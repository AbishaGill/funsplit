package com.finsplit.app.models;

import com.finsplit.app.utils.AppConstants;
import com.google.firebase.firestore.Exclude;
import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;

public class User {

    private String userId;
    private String displayName;
    private String email;
    private String profilePicUrl;
    private String defaultCurrency;
    private String fcmToken;

    @ServerTimestamp
    private Date createdAt;

    // Required no-arg constructor for Firestore deserialization
    public User() {
        this.defaultCurrency = "PKR";
    }

    public User(String userId, String displayName, String email, String profilePicUrl) {
        this.userId = userId;
        this.displayName = displayName;
        this.email = email;
        this.profilePicUrl = profilePicUrl;
        this.defaultCurrency = AppConstants.CURRENCY_DEFAULT;
    }

    // Getters
    public String getUserId() { return userId; }
    public String getDisplayName() { return displayName; }
    public String getEmail() { return email; }
    public String getProfilePicUrl() { return profilePicUrl; }
    public String getDefaultCurrency() { return defaultCurrency; }
    public String getFcmToken() { return fcmToken; }
    public Date getCreatedAt() { return createdAt; }

    // Setters
    public void setUserId(String userId) { this.userId = userId; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public void setEmail(String email) { this.email = email; }
    public void setProfilePicUrl(String profilePicUrl) { this.profilePicUrl = profilePicUrl; }
    public void setDefaultCurrency(String defaultCurrency) { this.defaultCurrency = defaultCurrency; }
    public void setFcmToken(String fcmToken) { this.fcmToken = fcmToken; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    @Exclude
    public boolean isValid() {
        return userId != null && !userId.isEmpty() && email != null && !email.isEmpty();
    }
}
