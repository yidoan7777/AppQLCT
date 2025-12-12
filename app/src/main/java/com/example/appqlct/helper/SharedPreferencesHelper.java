package com.example.appqlct.helper;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Helper class để quản lý SharedPreferences
 * Lưu trữ thông tin session của user
 */
public class SharedPreferencesHelper {
    private static final String PREF_NAME = "AppQLCT_Prefs";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USER_EMAIL = "user_email";
    private static final String KEY_USER_NAME = "user_name";
    private static final String KEY_USER_ROLE = "user_role";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";

    private SharedPreferences prefs;
    private SharedPreferences.Editor editor;

    public SharedPreferencesHelper(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = prefs.edit();
    }

    /**
     * Lưu thông tin user sau khi đăng nhập
     */
    public void saveUserSession(String userId, String email, String name, String role) {
        editor.putString(KEY_USER_ID, userId);
        editor.putString(KEY_USER_EMAIL, email);
        editor.putString(KEY_USER_NAME, name);
        editor.putString(KEY_USER_ROLE, role);
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.apply();
    }

    /**
     * Xóa session khi đăng xuất
     */
    public void clearSession() {
        editor.clear();
        editor.apply();
    }

    /**
     * Kiểm tra user đã đăng nhập chưa
     */
    public boolean isLoggedIn() {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    /**
     * Lấy User ID
     */
    public String getUserId() {
        return prefs.getString(KEY_USER_ID, "");
    }

    /**
     * Lấy User Email
     */
    public String getUserEmail() {
        return prefs.getString(KEY_USER_EMAIL, "");
    }

    /**
     * Lấy User Name
     */
    public String getUserName() {
        return prefs.getString(KEY_USER_NAME, "");
    }

    /**
     * Lấy User Role
     */
    public String getUserRole() {
        return prefs.getString(KEY_USER_ROLE, "user");
    }

    /**
     * Cập nhật tên user
     */
    public void updateUserName(String name) {
        editor.putString(KEY_USER_NAME, name);
        editor.apply();
    }
}

