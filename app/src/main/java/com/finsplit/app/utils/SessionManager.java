package com.finsplit.app.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {

    private final SharedPreferences prefs;

    public SessionManager(Context context) {
        prefs = context.getApplicationContext()
                .getSharedPreferences(AppConstants.PREFS_NAME, Context.MODE_PRIVATE);
    }

    public void saveUserId(String userId) {
        prefs.edit().putString(AppConstants.KEY_USER_ID, userId).apply();
    }

    public String getUserId() {
        return prefs.getString(AppConstants.KEY_USER_ID, null);
    }

    public boolean hasSession() {
        String id = getUserId();
        return id != null && !id.isEmpty();
    }

    public void clearSession() {
        prefs.edit().clear().apply();
    }
}
