package com.finsplit.app.utils;

import com.finsplit.app.models.User;

public interface AuthCallback {
    void onSuccess(User user);
    void onFailure(String error);
}
